import 'package:flutter/foundation.dart';

/// The slice of a chart's horizontal axis that is actually on screen.
///
/// Every chart in this app maps its data onto the same normalised axis first — a
/// fraction in `0..1`, where 0 is the left edge of what the chart is ABOUT (midnight,
/// the start of the session, the first bucket of the week) and 1 is the right edge. See
/// [DayAxis] and [SessionAxis]; the bar chart does it too, implicitly, by cutting the
/// width into one slot per bucket.
///
/// That shared step is the whole reason zooming can live in one place. A viewport is a
/// sub-range of that fraction, and [visibleFraction] is the ONE function that turns a
/// data fraction into the fraction of the plot it should be drawn at. A chart that goes
/// through it zooms; a chart that does not, does not. Nothing else about a painter has to
/// know that zooming exists.
@immutable
class ChartViewport {
  const ChartViewport({this.start = 0.0, this.end = 1.0})
      : assert(start >= 0.0 && start < end && end <= 1.0);

  /// The whole chart, which is what every chart starts as.
  static const ChartViewport full = ChartViewport();

  final double start;
  final double end;

  /// How far you may zoom in: 2% of the axis. On a one-day chart that is under half an
  /// hour across the plot, which is as fine as any of this data is worth reading — the
  /// samples underneath are minutes apart at best.
  static const double minimumSpan = 0.02;

  double get span => end - start;

  bool get isZoomed => span < 1.0 - 1e-9;

  /// Where a point at [fraction] of the DATA should be drawn, as a fraction of the PLOT.
  ///
  /// Outside `0..1` when the point is off-screen — deliberately not clamped, because a
  /// line leaving the left edge has to keep going to its real position or it would bend
  /// upwards into the corner. Painters clip; they do not clamp.
  double visibleFraction(double fraction) => (fraction - start) / span;

  /// The inverse: which data fraction is under a point [fraction] of the way across the
  /// plot. The scrubber needs it, and so does a pinch, which zooms around the point
  /// between the fingers.
  double dataFraction(double fraction) => start + fraction * span;

  /// Zooms by [scale] about [focus], a fraction of the PLOT (0 = left edge, 1 = right).
  ///
  /// Zooming about the fingers rather than the centre is what makes it feel like the
  /// chart is being stretched under them rather than replaced.
  ChartViewport zoomed(double scale, double focus) {
    if (!scale.isFinite || scale <= 0) return this;
    final anchor = dataFraction(focus.clamp(0.0, 1.0));
    final newSpan = (span / scale).clamp(minimumSpan, 1.0);
    // Keep the anchor under the fingers: it stays the same fraction across the plot.
    return _around(anchor, newSpan, focus.clamp(0.0, 1.0));
  }

  /// Slides the window by [delta], a fraction of the PLOT — so a drag moves the data
  /// under the finger by the same distance whatever the zoom.
  ChartViewport panned(double delta) {
    final shift = -delta * span;
    return _shifted(shift);
  }

  ChartViewport _around(double anchor, double newSpan, double focus) {
    var newStart = anchor - focus * newSpan;
    // Never past the ends. Panning off the edge of a chart shows nothing and is only
    // ever a mistake, so the window stops rather than emptying.
    newStart = newStart.clamp(0.0, 1.0 - newSpan);
    return ChartViewport(start: newStart, end: newStart + newSpan);
  }

  ChartViewport _shifted(double shift) {
    final newStart = (start + shift).clamp(0.0, 1.0 - span);
    return ChartViewport(start: newStart, end: newStart + span);
  }

  @override
  bool operator ==(Object other) =>
      other is ChartViewport && other.start == start && other.end == end;

  @override
  int get hashCode => Object.hash(start, end);

  @override
  String toString() =>
      'ChartViewport(${start.toStringAsFixed(3)}..${end.toStringAsFixed(3)})';
}
