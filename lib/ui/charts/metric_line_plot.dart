import 'package:flutter/material.dart';

import 'chart_axis.dart';
import 'chart_curve.dart';
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

class MetricLinePlot extends StatelessWidget {
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
    this.scrubLabelBuilder,
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

  /// Turns a sample into the two lines of a scrub tooltip: the VALUE, and what it
  /// is a value OF (usually the time it was taken). Null leaves the chart inert —
  /// which is what a chart with nothing to say about a single point should be.
  final (String primary, String? secondary) Function(MetricLinePlotPoint point)?
      scrubLabelBuilder;

  @override
  Widget build(BuildContext context) {
    // Guard a flat series: a zero span would divide by zero when normalizing.
    final span = maxValue - minValue;
    final safeMax = span.abs() < 1e-9 ? minValue + 1.0 : maxValue;

    return YAxisChart(
      chartHeight: chartHeight,
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
        minValue,
        safeMax,
        valueFormatter: valueFormatter,
      ),
      chart: _maybeScrubbable(
        ChartReveal(
          builder: (context, t) => CustomPaint(
            size: Size.infinite,
            painter: _MetricLinePlotPainter(
              points: points,
              minValue: minValue,
              maxValue: safeMax,
              accentColor: accentColor,
              guides: guides,
              markers: markers,
              strokeWidth: lineStrokeWidth,
              pointRadius: drawPoints ? pointRadius : 0,
              progress: t,
            ),
          ),
        ),
      ),
    );
  }

  /// Wraps the plot in a [ChartScrubber] when the caller said how to label a
  /// sample, and returns it untouched otherwise.
  Widget _maybeScrubbable(Widget plot) {
    final builder = scrubLabelBuilder;
    if (builder == null || points.length < 2) return plot;

    final span = maxValue - minValue;
    final safeSpan = span.abs() < 1e-9 ? 1.0 : span;
    return ChartScrubber(
      accentColor: accentColor,
      targets: [
        for (final point in points)
          () {
            final (primary, secondary) = builder(point);
            return (
              xFraction: point.xFraction.clamp(0.0, 1.0),
              yFraction: ((point.value - minValue) / safeSpan).clamp(0.0, 1.0),
              primary: primary,
              secondary: secondary,
            );
          }(),
      ],
      child: plot,
    );
  }
}

class _MetricLinePlotPainter extends CustomPainter {
  const _MetricLinePlotPainter({
    required this.points,
    required this.minValue,
    required this.maxValue,
    required this.accentColor,
    required this.guides,
    required this.markers,
    required this.strokeWidth,
    required this.pointRadius,
    required this.progress,
  });

  final List<MetricLinePlotPoint> points;
  final double minValue;
  final double maxValue;
  final Color accentColor;
  final List<ChartGuideLine> guides;
  final List<ChartMarker> markers;
  final double strokeWidth;

  /// Dot radius for each sample; 0 draws no dots (Kotlin `drawPoints=false`).
  final double pointRadius;

  /// 0 → 1: how much of the line has been drawn. See [ChartReveal].
  final double progress;

  Offset _offsetFor(MetricLinePlotPoint point, Size size) {
    final x = point.xFraction.clamp(0.0, 1.0) * size.width;
    final normalized =
        ((point.value - minValue) / (maxValue - minValue)).clamp(0.0, 1.0);
    return Offset(x, size.height - normalized * size.height);
  }

  @override
  void paint(Canvas canvas, Size size) {
    if (points.length < 2 || size.width <= 0 || size.height <= 0) return;

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

    final offsets = [for (final point in points) _offsetFor(point, size)];
    final first = offsets.first;
    final full = smoothPath(offsets);

    // The line draws itself in, left to right — `extractPath` walks the real curve
    // rather than clipping a rectangle over it, so the leading end is the line's
    // own end and not a cut.
    final path = progress >= 1.0 ? full : _partial(full, progress);
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
    final drawnEnd = _endOf(path) ?? offsets.last;
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
        Offset(marker.xFraction.clamp(0.0, 1.0) * size.width, size.height - 3),
        3,
        Paint()..color = marker.color,
      );
    }

    // Kotlin `drawMetricLinePlot`: a dot per sample when requested. Only the dots
    // the line has actually reached — a dot ahead of the trace is a sample the
    // chart is claiming to have drawn and has not.
    if (pointRadius > 0) {
      final dotPaint = Paint()..color = accentColor;
      for (final point in points) {
        final offset = _offsetFor(point, size);
        if (offset.dx <= drawnEnd.dx + 0.5) {
          canvas.drawCircle(offset, pointRadius, dotPaint);
        }
      }
    }
  }

  /// The first [fraction] of [path], by length.
  Path? _partial(Path path, double fraction) {
    if (fraction <= 0) return null;
    final metrics = path.computeMetrics().toList();
    if (metrics.isEmpty) return null;
    final metric = metrics.first;
    return metric.extractPath(0, metric.length * fraction.clamp(0.0, 1.0));
  }

  Offset? _endOf(Path path) {
    final metrics = path.computeMetrics().toList();
    if (metrics.isEmpty) return null;
    final metric = metrics.last;
    return metric.getTangentForOffset(metric.length)?.position;
  }

  @override
  bool shouldRepaint(_MetricLinePlotPainter oldDelegate) =>
      // `progress` is in here, and everything else is too — the old answer was a
      // bare `true` in several of these painters, which is free until something
      // animates and then costs a repaint every frame, forever, on every chart in
      // a scrolling list.
      oldDelegate.progress != progress ||
      oldDelegate.points != points ||
      oldDelegate.minValue != minValue ||
      oldDelegate.maxValue != maxValue ||
      oldDelegate.accentColor != accentColor ||
      oldDelegate.guides != guides ||
      oldDelegate.markers != markers ||
      oldDelegate.pointRadius != pointRadius;
}
