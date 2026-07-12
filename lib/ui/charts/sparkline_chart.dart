import 'package:flutter/material.dart';
import 'chart_curve.dart';

/// A compact sparkline (mini trend line with baseline + point dots), ported
/// from the Kotlin `MetricSparklineChart`. Draws a baseline, a poly-line
/// through the values, and a filled dot at each point via [CustomPainter].
class SparklineChart extends StatelessWidget {
  const SparklineChart({
    super.key,
    required this.values,
    required this.accentColor,
    this.minValue,
    this.baselineFraction = 0.75,
    this.baselineAlpha = 0.22,
    this.verticalScaleFraction = 0.72,
    this.topPaddingFraction = 0.14,
    this.lineStrokeWidth = 4,
    this.pointRadius = 3,
    this.pointStrokeWidth,
    this.pointFillRadius,
    this.singlePointLine = false,
  });

  final List<double> values;
  final Color accentColor;
  final double? minValue;
  final double baselineFraction;
  final double baselineAlpha;
  final double verticalScaleFraction;
  final double topPaddingFraction;
  final double lineStrokeWidth;
  final double pointRadius;
  final double? pointStrokeWidth;
  final double? pointFillRadius;
  final bool singlePointLine;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: _SparklinePainter(this),
      size: Size.infinite,
    );
  }
}

class _SparklinePainter extends CustomPainter {
  _SparklinePainter(this.spec);

  final SparklineChart spec;

  @override
  void paint(Canvas canvas, Size size) {
    final values = spec.values;
    if (values.isEmpty) return;

    final maxCandidate = values.reduce((a, b) => a > b ? a : b);
    final maxValue = maxCandidate > 0.0 ? maxCandidate : 1.0;
    final resolvedMin = spec.minValue ?? 0.0;
    final rawRange = maxValue - resolvedMin;
    final range = rawRange > 0.0 ? rawRange : 1.0;
    final stepX = values.length > 1
        ? size.width / (values.length - 1)
        : size.width / 2.0;

    final points = <Offset>[];
    for (var index = 0; index < values.length; index++) {
      final yFraction =
          ((values[index] - resolvedMin) / range).clamp(0.0, 1.0);
      points.add(
        Offset(
          values.length > 1 ? index * stepX : stepX,
          size.height -
              (yFraction * (size.height * spec.verticalScaleFraction)) -
              (size.height * spec.topPaddingFraction),
        ),
      );
    }

    final baselinePaint = Paint()
      ..color = spec.accentColor.withValues(alpha: spec.baselineAlpha)
      ..strokeWidth = 2;
    canvas.drawLine(
      Offset(0, size.height * spec.baselineFraction),
      Offset(size.width, size.height * spec.baselineFraction),
      baselinePaint,
    );

    final linePaint = Paint()
      ..color = spec.accentColor
      ..strokeWidth = spec.lineStrokeWidth
      ..strokeCap = StrokeCap.round;

    if (spec.singlePointLine && points.length == 1) {
      final half = spec.lineStrokeWidth / 2.0;
      canvas.drawLine(
        Offset(half, points.first.dy),
        Offset(size.width - half, points.first.dy),
        linePaint,
      );
    } else {
      canvas.drawPath(
        smoothPath(points),
        Paint()
          ..color = spec.accentColor
          ..style = PaintingStyle.stroke
          ..strokeWidth = spec.lineStrokeWidth
          ..strokeCap = StrokeCap.round
          ..strokeJoin = StrokeJoin.round,
      );
    }

    for (final point in points) {
      final stroke = spec.pointStrokeWidth;
      if (stroke != null) {
        canvas.drawCircle(
          point,
          spec.pointRadius,
          Paint()
            ..color = spec.accentColor
            ..style = PaintingStyle.stroke
            ..strokeWidth = stroke,
        );
      }
      canvas.drawCircle(
        point,
        spec.pointFillRadius ?? spec.pointRadius,
        Paint()..color = spec.accentColor,
      );
    }
  }

  @override
  bool shouldRepaint(covariant _SparklinePainter oldDelegate) =>
      oldDelegate.spec != spec;
}
