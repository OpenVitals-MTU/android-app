import 'package:flutter/material.dart';

import 'chart_axis.dart';
import 'chart_curve.dart';

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
      chart: CustomPaint(
        size: Size.infinite,
        painter: _MetricLinePlotPainter(
          points: points,
          minValue: minValue,
          maxValue: safeMax,
          accentColor: accentColor,
          strokeWidth: lineStrokeWidth,
          pointRadius: drawPoints ? pointRadius : 0,
        ),
      ),
    );
  }
}

class _MetricLinePlotPainter extends CustomPainter {
  const _MetricLinePlotPainter({
    required this.points,
    required this.minValue,
    required this.maxValue,
    required this.accentColor,
    required this.strokeWidth,
    required this.pointRadius,
  });

  final List<MetricLinePlotPoint> points;
  final double minValue;
  final double maxValue;
  final Color accentColor;
  final double strokeWidth;

  /// Dot radius for each sample; 0 draws no dots (Kotlin `drawPoints=false`).
  final double pointRadius;

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

    final offsets = [for (final point in points) _offsetFor(point, size)];
    final first = offsets.first;
    final path = smoothPath(offsets);

    // Fill under the line, then stroke it, so the stroke stays crisp on top.
    //
    // Closed under the LAST POINT, not at the plot's right edge. Closing at the
    // edge fills a region the line never went to: on `today` — the commonest
    // state of the commonest chart in the app — the trace stops at the current
    // hour and the fill went on sweeping down to the bottom-right corner, shading
    // a triangle across the hours that have not happened yet. A weight chart did
    // the same past its final reading of the day. The fill is meant to say "under
    // the line"; it was saying "and also over here".
    final fill = Path.from(path)
      ..lineTo(offsets.last.dx, size.height)
      ..lineTo(first.dx, size.height)
      ..close();
    canvas.drawPath(
      fill,
      Paint()..color = accentColor.withValues(alpha: 0.12),
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

    // Kotlin `drawMetricLinePlot`: a dot per sample when requested.
    if (pointRadius > 0) {
      final dotPaint = Paint()..color = accentColor;
      for (final point in points) {
        canvas.drawCircle(_offsetFor(point, size), pointRadius, dotPaint);
      }
    }
  }

  @override
  bool shouldRepaint(_MetricLinePlotPainter oldDelegate) =>
      oldDelegate.points != points ||
      oldDelegate.minValue != minValue ||
      oldDelegate.maxValue != maxValue ||
      oldDelegate.accentColor != accentColor ||
      oldDelegate.pointRadius != pointRadius;
}
