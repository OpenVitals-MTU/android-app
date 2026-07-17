import 'dart:ui' show PathMetric;

import 'package:flutter/material.dart';

import 'chart_axis.dart';
import 'chart_viewport.dart';
import 'chart_curve.dart';
import 'chart_decimation.dart';
import 'chart_paint.dart';
import 'chart_reveal.dart';
import 'chart_scrubber.dart';

/// Port of the Kotlin `MetricLinePlot`: a line drawn against a normalized x
/// axis, for series whose points are not evenly spaced in time (the intraday
/// cumulative charts).

/// Kotlin `MetricLinePlotPoint`. [xFraction] is 0 at the start of the window and
/// 1 at its end.
@immutable
class MetricLinePlotPoint {
  const MetricLinePlotPoint({required this.xFraction, required this.value});

  final double xFraction;
  final double value;
}

/// A horizontal line the data is measured AGAINST, rather than data itself — the
/// caffeine sleep threshold, a goal, a clinical limit. Drawn dashed, because a
/// solid line of the same weight reads as another series.
typedef ChartGuideLine = ({double value, Color color});

/// A tick along the bottom edge: something happened at this moment. The caffeine
/// card marks each drink, so the sawtooth in the curve can be read against the
/// act that caused it.
typedef ChartMarker = ({double xFraction, Color color});

/// One span of a min/max band: at [xFraction], the data ranged from [low] to
/// [high]. Drawn as a filled ribbon behind the (average) line in the aggregated
/// chart view. See [MetricLinePlot.band].
typedef ChartBandSpan = ({double xFraction, double low, double high});

/// Above this many points, the per-sample dots ([MetricLinePlot.drawPoints]) are
/// suppressed: at that density they overlap into an illegible band and cost one
/// `drawCircle` each, every frame. The line itself still carries every point.
/// Mirrors [metric_session_chart]'s `_maxVisiblePoints`.
const int _maxDotPoints = 120;

class MetricLinePlot extends StatefulWidget {
  const MetricLinePlot({
    super.key,
    required this.points,
    required this.minValue,
    required this.maxValue,
    required this.accentColor,
    this.chartHeight = 180,
    this.valueFormatter = formatCompactAxisValue,
    this.lineStrokeWidth = 3,
    this.pointRadius = 3.5,
    this.drawPoints = false,
    this.guides = const <ChartGuideLine>[],
    this.markers = const <ChartMarker>[],
    this.band = const <ChartBandSpan>[],
    this.scrubLabelBuilder,
    this.viewport = ChartViewport.full,
  });

  final List<MetricLinePlotPoint> points;
  final double minValue;
  final double maxValue;
  final Color accentColor;
  final double chartHeight;
  final String Function(double value) valueFormatter;
  final double lineStrokeWidth;

  /// Kotlin `pointRadius`: radius of the per-sample dots when [drawPoints].
  final double pointRadius;

  /// Kotlin `drawPoints`: whether to mark each sample with a dot. Off by
  /// default here because the intraday cumulative charts (this widget's
  /// original callers) pass `drawPoints = false` in Kotlin.
  final bool drawPoints;

  /// Reference lines the data is read against. See [ChartGuideLine].
  final List<ChartGuideLine> guides;

  /// Moments worth marking on the baseline. See [ChartMarker].
  final List<ChartMarker> markers;

  /// A min/max ribbon drawn behind the line — the spread the [points] (an average)
  /// summarise. Empty in the raw view; populated in the aggregated view. See
  /// [ChartBandSpan].
  final List<ChartBandSpan> band;

  /// Turns a sample into the two lines of a scrub tooltip: the VALUE, and what it
  /// is a value OF (usually the time it was taken). Null leaves the chart inert —
  /// which is what a chart with nothing to say about a single point should be.
  final (String primary, String? secondary) Function(MetricLinePlotPoint point)?
      scrubLabelBuilder;

  /// The slice of the axis on show. [ChartViewport.full] -- the whole chart -- unless a
  /// [ChartZoom] above has been pinched.
  final ChartViewport viewport;

  @override
  State<MetricLinePlot> createState() => _MetricLinePlotState();
}

class _MetricLinePlotState extends State<MetricLinePlot> {
  // Lives on the State so it survives the per-frame rebuilds of the entry
  // animation (ChartReveal's AnimatedBuilder) and of a pinch gesture: those
  // rebuild the painter, not this widget, so the cache — keyed by everything the
  // geometry depends on EXCEPT progress — turns the 550ms reveal from ~33 full
  // path rebuilds into one build plus a cheap per-frame `extractPath`.
  final _PlotGeometryCache _cache = _PlotGeometryCache();

  /// Whether a point at [xFraction] of the data is inside the slice on show.
  bool _isVisible(double xFraction) {
    final visible = widget.viewport.visibleFraction(xFraction);
    return visible >= 0.0 && visible <= 1.0;
  }

  @override
  Widget build(BuildContext context) {
    // Guard a flat series: a zero span would divide by zero when normalizing.
    final span = widget.maxValue - widget.minValue;
    final safeMax = span.abs() < 1e-9 ? widget.minValue + 1.0 : widget.maxValue;

    return YAxisChart(
      chartHeight: widget.chartHeight,
      // `chartYAxisLabels`, not three hand-rolled calls to the formatter — which
      // is what this was, and it dropped the one thing that function is for.
      //
      // The compact formatter rounds anything over 10 to a whole number, so a
      // narrow range collides: a weight chart across 74.06–74.64 kg asked for
      // three ticks and got "75", "74", "74". Two of them the same number, at
      // different heights, on an axis whose whole job is to say how high things
      // are. `chartYAxisLabels` notices the collision and steps up to a precision
      // that separates them — and `MetricLineChart` has been calling it all along,
      // which is exactly why the heart charts never showed this and the day charts
      // always could.
      labels: chartYAxisLabels(
        widget.minValue,
        safeMax,
        valueFormatter: widget.valueFormatter,
      ),
      chart: _maybeScrubbable(
        ChartReveal(
          builder: (context, t) => CustomPaint(
            size: Size.infinite,
            painter: _MetricLinePlotPainter(
              cache: _cache,
              points: widget.points,
              minValue: widget.minValue,
              maxValue: safeMax,
              accentColor: widget.accentColor,
              guides: widget.guides,
              markers: widget.markers,
              band: widget.band,
              strokeWidth: widget.lineStrokeWidth,
              pointRadius: widget.drawPoints ? widget.pointRadius : 0,
              progress: t,
              viewport: widget.viewport,
            ),
          ),
        ),
      ),
    );
  }

  /// Wraps the plot in a [ChartScrubber] when the caller said how to label a
  /// sample, and returns it untouched otherwise.
  Widget _maybeScrubbable(Widget plot) {
    final builder = widget.scrubLabelBuilder;
    if (builder == null || widget.points.length < 2) return plot;

    final span = widget.maxValue - widget.minValue;
    final safeSpan = span.abs() < 1e-9 ? 1.0 : span;
    return ChartScrubber(
      accentColor: widget.accentColor,
      targets: [
        for (final point in widget.points)
          if (_isVisible(point.xFraction))
            () {
            final (primary, secondary) = builder(point);
            return (
              xFraction: widget.viewport.visibleFraction(point.xFraction),
              yFraction:
                  ((point.value - widget.minValue) / safeSpan).clamp(0.0, 1.0),
              primary: primary,
              secondary: secondary,
            );
          }(),
      ],
      child: plot,
    );
  }
}

/// The geometry of a plotted line: the pixel positions, the smoothed [Path], and
/// its first [PathMetric] (for the reveal's `extractPath`) and drawn end. All of
/// it depends only on the points, size, viewport and value range — never on the
/// reveal progress — so it is computed once per those and reused across frames.
class _PlotGeometry {
  _PlotGeometry({
    required this.offsets,
    required this.path,
    required this.metric,
    required this.end,
  });

  final List<Offset> offsets;
  final Path path;
  final PathMetric? metric;
  final Offset end;
}

/// A single-slot memo for [_PlotGeometry]. Rebuilds only when an input the
/// geometry actually depends on changes; a progress-only change (every reveal
/// frame) is a hit.
class _PlotGeometryCache {
  _PlotGeometry? _cached;
  List<MetricLinePlotPoint>? _points;
  Size? _size;
  ChartViewport? _viewport;
  double? _minValue;
  double? _maxValue;

  _PlotGeometry build({
    required List<MetricLinePlotPoint> points,
    required Size size,
    required ChartViewport viewport,
    required double minValue,
    required double maxValue,
    required Offset Function(MetricLinePlotPoint point) offsetFor,
  }) {
    final cached = _cached;
    if (cached != null &&
        identical(_points, points) &&
        _size == size &&
        _viewport == viewport &&
        _minValue == minValue &&
        _maxValue == maxValue) {
      return cached;
    }

    // Cull to the visible window (keeping one point past each edge so the line
    // reaches the border), then decimate to roughly one vertex per pixel — no
    // point drawing more cubics than the chart is wide. Both depend on the
    // viewport and size, which is exactly why they live behind this cache and not
    // in the per-frame paint.
    final visible = _cull(points, viewport);
    var offsets = [for (final point in visible) offsetFor(point)];
    offsets = decimateOffsets(offsets, size.width.ceil());
    final path = smoothPath(offsets);
    final metrics = path.computeMetrics().toList();
    final metric = metrics.isEmpty ? null : metrics.first;
    final end = offsets.isEmpty
        ? Offset.zero
        : (metric == null
            ? offsets.last
            : (metrics.last.getTangentForOffset(metrics.last.length)?.position ??
                offsets.last));

    final geometry = _PlotGeometry(
      offsets: offsets,
      path: path,
      metric: metric,
      end: end,
    );
    _cached = geometry;
    _points = points;
    _size = size;
    _viewport = viewport;
    _minValue = minValue;
    _maxValue = maxValue;
    return geometry;
  }

  /// The points inside the viewport, plus one on each side so the line runs to the
  /// edges instead of stopping short. Points are sorted ascending by xFraction.
  /// The full (unzoomed) viewport shows everything, so there is nothing to cull.
  static List<MetricLinePlotPoint> _cull(
    List<MetricLinePlotPoint> points,
    ChartViewport viewport,
  ) {
    if (!viewport.isZoomed) return points;
    final n = points.length;
    var lo = 0;
    while (lo < n && viewport.visibleFraction(points[lo].xFraction) < 0) {
      lo++;
    }
    var hi = n - 1;
    while (hi >= 0 && viewport.visibleFraction(points[hi].xFraction) > 1) {
      hi--;
    }
    if (lo > hi) return const [];
    if (lo > 0) lo--;
    if (hi < n - 1) hi++;
    if (lo == 0 && hi == n - 1) return points;
    return points.sublist(lo, hi + 1);
  }
}

class _MetricLinePlotPainter extends CustomPainter {
  const _MetricLinePlotPainter({
    required this.cache,
    required this.points,
    required this.minValue,
    required this.maxValue,
    required this.accentColor,
    required this.guides,
    required this.markers,
    required this.band,
    required this.strokeWidth,
    required this.pointRadius,
    required this.progress,
    required this.viewport,
  });

  final _PlotGeometryCache cache;
  final ChartViewport viewport;
  final List<MetricLinePlotPoint> points;
  final double minValue;
  final double maxValue;
  final Color accentColor;
  final List<ChartGuideLine> guides;
  final List<ChartMarker> markers;
  final List<ChartBandSpan> band;
  final double strokeWidth;

  /// Dot radius for each sample; 0 draws no dots (Kotlin `drawPoints=false`).
  final double pointRadius;

  /// 0 → 1: how much of the line has been drawn. See [ChartReveal].
  final double progress;

  Offset _offsetFor(MetricLinePlotPoint point, Size size) {
    final x = viewport.visibleFraction(point.xFraction) * size.width;
    final normalized =
        ((point.value - minValue) / (maxValue - minValue)).clamp(0.0, 1.0);
    return Offset(x, size.height - normalized * size.height);
  }

  @override
  void paint(Canvas canvas, Size size) {
    if (points.length < 2 || size.width <= 0 || size.height <= 0) return;

    // Painters CLIP; they do not clamp. A point scrolled off the left edge keeps its real
    // position, so the line running off the plot carries on to where it actually is —
    // clamping it to the edge instead would bend it into the corner and draw a value
    // nobody ever recorded. The clip is what keeps that honest line inside the card.
    if (viewport.isZoomed) {
      canvas.save();
      canvas.clipRect(Offset.zero & size);
    }

    final baseline = Paint()
      ..color = accentColor.withValues(alpha: 0.22)
      ..strokeWidth = 1;
    canvas.drawLine(
      Offset(0, size.height),
      Offset(size.width, size.height),
      baseline,
    );

    // Guides first, so the data is drawn ON them and not under them.
    for (final guide in guides) {
      final normalized =
          ((guide.value - minValue) / (maxValue - minValue)).clamp(0.0, 1.0);
      final y = size.height - normalized * size.height;
      drawDashedLine(
        canvas,
        Offset(0, y),
        Offset(size.width, y),
        Paint()
          ..color = guide.color
          ..strokeWidth = 2,
        dash: 6,
        gap: 6,
      );
    }

    // Built once per (points, size, viewport, range) and reused across the reveal
    // and pinch frames — see [_PlotGeometryCache].
    final geometry = cache.build(
      points: points,
      size: size,
      viewport: viewport,
      minValue: minValue,
      maxValue: maxValue,
      offsetFor: (point) => _offsetFor(point, size),
    );
    final offsets = geometry.offsets;
    // The visible window can fall entirely in a gap between samples (deep zoom on
    // a stretch with no readings) — nothing to draw then, but the clip is open.
    if (offsets.length < 2) {
      if (viewport.isZoomed) canvas.restore();
      return;
    }
    final first = offsets.first;
    final full = geometry.path;

    // The line draws itself in, left to right — `extractPath` walks the real curve
    // rather than clipping a rectangle over it, so the leading end is the line's
    // own end and not a cut. It extracts from the CACHED metric, so no path is
    // remeasured per frame.
    final path = progress >= 1.0 ? full : _partial(geometry.metric, progress);
    if (path == null) return;

    // Fill under the line, then stroke it, so the stroke stays crisp on top.
    //
    // Closed under the LAST POINT, not at the plot's right edge. Closing at the
    // edge fills a region the line never went to: on `today` — the commonest
    // state of the commonest chart in the app — the trace stops at the current
    // hour and the fill went on sweeping down to the bottom-right corner, shading
    // a triangle across the hours that have not happened yet. A weight chart did
    // the same past its final reading of the day. The fill is meant to say "under
    // the line"; it was saying "and also over here".
    // Where the trace has drawn to: the cached path end at full, or the point the
    // cached metric reaches at `progress` along it — no per-frame remeasuring.
    final drawnEnd = progress >= 1.0
        ? geometry.end
        : (geometry.metric
                ?.getTangentForOffset(geometry.metric!.length * progress.clamp(0.0, 1.0))
                ?.position ??
            offsets.last);
    // Aggregated view: the min/max ribbon behind the average line takes the place
    // of the under-line gradient — the spread IS the fill, and drawing both would
    // double up.
    if (band.isNotEmpty) {
      _drawBand(canvas, size);
    } else {
      final fill = Path.from(path)
        ..lineTo(drawnEnd.dx, size.height)
        ..lineTo(first.dx, size.height)
        ..close();
      // Gradient, not a flat block: a solid wash under a line reads as a second
      // object — a coloured rectangle with a curved lid — where a fade reads as the
      // line and the space it encloses. It is the honest shape too, because the fill
      // says "under here", and the further under you go the less it is saying.
      canvas.drawPath(
        fill,
        chartFillPaint(accentColor, Offset.zero & size),
      );
    }
    canvas.drawPath(
      path,
      Paint()
        ..color = accentColor
        ..style = PaintingStyle.stroke
        ..strokeWidth = strokeWidth
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round,
    );

    // Moments, on the baseline: each one is a thing that HAPPENED, sitting under
    // the consequence it had.
    for (final marker in markers) {
      canvas.drawCircle(
        Offset(viewport.visibleFraction(marker.xFraction) * size.width,
            size.height - 3),
        3,
        Paint()..color = marker.color,
      );
    }

    // Kotlin `drawMetricLinePlot`: a dot per sample when requested. Only the dots
    // the line has actually reached — a dot ahead of the trace is a sample the
    // chart is claiming to have drawn and has not.
    // At most [_maxDotPoints]: past that the dots merge into a solid band, so the
    // loop is pure cost. Reuse the cached offsets rather than recomputing them.
    if (pointRadius > 0 && offsets.length <= _maxDotPoints) {
      final dotPaint = Paint()..color = accentColor;
      for (final offset in offsets) {
        if (offset.dx <= drawnEnd.dx + 0.5) {
          canvas.drawCircle(offset, pointRadius, dotPaint);
        }
      }
    }

    if (viewport.isZoomed) canvas.restore();
  }

  /// The min/max ribbon: across the top by the maxima, back along the bottom by
  /// the minima, closed and filled. Buckets are few (≤ a few hundred), so a
  /// straight-segment ribbon behind the smoothed average line is cheap and reads
  /// cleanly under a translucent fill.
  void _drawBand(Canvas canvas, Size size) {
    if (band.length < 2) return;
    double x(double xFraction) => viewport.visibleFraction(xFraction) * size.width;
    double y(double value) {
      final normalized =
          ((value - minValue) / (maxValue - minValue)).clamp(0.0, 1.0);
      return size.height - normalized * size.height;
    }

    final ribbon = Path()..moveTo(x(band.first.xFraction), y(band.first.high));
    for (final span in band.skip(1)) {
      ribbon.lineTo(x(span.xFraction), y(span.high));
    }
    for (final span in band.reversed) {
      ribbon.lineTo(x(span.xFraction), y(span.low));
    }
    ribbon.close();
    canvas.drawPath(
      ribbon,
      Paint()
        ..color = accentColor.withValues(alpha: 0.16)
        ..style = PaintingStyle.fill,
    );
  }

  /// The first [fraction] of the line, by length, extracted from the CACHED
  /// [metric] — the path is measured once (in [_PlotGeometryCache]), not per frame.
  Path? _partial(PathMetric? metric, double fraction) {
    if (fraction <= 0 || metric == null) return null;
    return metric.extractPath(0, metric.length * fraction.clamp(0.0, 1.0));
  }

  @override
  bool shouldRepaint(_MetricLinePlotPainter oldDelegate) =>
      // `progress` is in here, and everything else is too — the old answer was a
      // bare `true` in several of these painters, which is free until something
      // animates and then costs a repaint every frame, forever, on every chart in
      // a scrolling list.
      oldDelegate.progress != progress ||
      oldDelegate.viewport != viewport ||
      oldDelegate.points != points ||
      oldDelegate.minValue != minValue ||
      oldDelegate.maxValue != maxValue ||
      oldDelegate.accentColor != accentColor ||
      oldDelegate.guides != guides ||
      oldDelegate.markers != markers ||
      oldDelegate.band != band ||
      oldDelegate.pointRadius != pointRadius;
}
