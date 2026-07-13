import 'package:flutter/material.dart';

import '../../../domain/insights/body_energy_timeline.dart';
import '../../../ui/charts/day_axis.dart';
import '../../../ui/theme/chart_colors.dart';
import '../application/body_energy_display.dart';

/// The Body Energy day timeline: a smoothed 0-100 score line drawn by a
/// [CustomPainter], with a charge/drain influence-bar strip beneath. A trimmed
/// port of the Kotlin `BodyEnergyTimelineChart`.
///
/// Everything it draws arrives precomputed on the [BodyEnergyDisplay] — the
/// bucket fractions, the bar magnitudes and the strip's scale. This card only
/// paints them.
class BodyEnergyTimelineChart extends StatelessWidget {
  const BodyEnergyTimelineChart({
    super.key,
    required this.points,
    required this.influenceBars,
    required this.maxMagnitude,
  });

  final List<BodyEnergyChartPoint> points;
  final List<BodyEnergyInfluenceBar> influenceBars;

  /// The tallest charge/drain in [influenceBars], precomputed by the view-model.
  final double maxMagnitude;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SizedBox(
          height: 172,
          child: CustomPaint(
            painter: _LinePainter(
              points: points,
              lineColor: scheme.primary,
              gridColor: scheme.primary.withValues(alpha: 0.12),
              axisColor: scheme.outlineVariant.withValues(alpha: 0.8),
            ),
          ),
        ),
        const SizedBox(height: 6),
        SizedBox(
          height: 44,
          child: CustomPaint(
            painter: _InfluenceBarsPainter(
              bars: influenceBars,
              maxMagnitude: maxMagnitude,
              axisColor: scheme.outlineVariant.withValues(alpha: 0.8),
              noDataColor: scheme.outline.withValues(alpha: 0.36),
              colorFor: (influence) => influenceColor(influence, scheme),
            ),
          ),
        ),
        const SizedBox(height: 6),
        // inset: 0 — these painters draw no y-axis label column, so the plot starts
        // at the card's edge and the hour row must too.
        const DayAxisLabels(inset: 0),
      ],
    );
  }
}

/// The accent colour for a Body Energy influence (port of the Kotlin
/// `bodyEnergyInfluenceColor`).
class _LinePainter extends CustomPainter {
  _LinePainter({
    required this.points,
    required this.lineColor,
    required this.gridColor,
    required this.axisColor,
  });

  final List<BodyEnergyChartPoint> points;
  final Color lineColor;
  final Color gridColor;
  final Color axisColor;

  @override
  void paint(Canvas canvas, Size size) {
    // Horizontal guide lines at 0/25/50/75/100.
    final gridPaint = Paint()
      ..color = gridColor
      ..strokeWidth = 1;
    for (var i = 0; i <= 4; i++) {
      final y = size.height * i / 4;
      canvas.drawLine(Offset(0, y), Offset(size.width, y), gridPaint);
    }
    canvas.drawLine(
      Offset(0, size.height),
      Offset(size.width, size.height),
      Paint()
        ..color = axisColor
        ..strokeWidth = 1,
    );
    if (points.isEmpty) return;

    final rawPoints = [
      for (final point in points)
        Offset(
          size.width * point.xFraction.clamp(0.0, 1.0),
          size.height * (1.0 - (point.score / 100.0).clamp(0.0, 1.0)),
        ),
    ];
    // Scores are integers (0..100) sampled per bucket, so the raw series is a
    // staircase. Damp that quantization with a small moving average before
    // splining, otherwise the curve just traces the steps and reads as jagged.
    final positioned = _movingAverageY(rawPoints);

    final linePaint = Paint()
      ..color = lineColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.5
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;
    if (positioned.length >= 3) {
      canvas.drawPath(
        _smoothLinePath(positioned, size.height),
        linePaint,
      );
    } else {
      final path = Path()..moveTo(positioned.first.dx, positioned.first.dy);
      for (var i = 1; i < positioned.length; i++) {
        path.lineTo(positioned[i].dx, positioned[i].dy);
      }
      canvas.drawPath(path, linePaint);
    }
    if (positioned.length <= 40) {
      final pointPaint = Paint()..color = lineColor;
      for (final offset in positioned) {
        canvas.drawCircle(offset, 3, pointPaint);
      }
    }
  }

  /// Smooths the Y series with a centered moving average, keeping each point's
  /// X. The window radius scales with point count (wider for denser series) so
  /// the integer-quantized score staircase becomes a flowing line before
  /// splining. Port of the Kotlin `movingAverageY`.
  List<Offset> _movingAverageY(List<Offset> pts) {
    if (pts.length < 3) return pts;
    final radius = (pts.length ~/ 16).clamp(1, 4);
    final last = pts.length - 1;
    return [
      for (var index = 0; index < pts.length; index++)
        () {
          final from = (index - radius).clamp(0, last);
          final to = (index + radius).clamp(0, last);
          var sum = 0.0;
          for (var i = from; i <= to; i++) {
            sum += pts[i].dy;
          }
          return Offset(pts[index].dx, sum / (to - from + 1));
        }(),
    ];
  }

  /// Builds a smooth curve through [pts] using a Catmull-Rom spline converted to
  /// cubic Bézier segments. Control-point Y is clamped to [0, maxY] so the eased
  /// curve can never overshoot past the chart's 0/100 bounds. Port of the Kotlin
  /// `smoothLinePath`.
  Path _smoothLinePath(List<Offset> pts, double maxY) {
    final last = pts.length - 1;
    final path = Path()..moveTo(pts[0].dx, pts[0].dy);
    for (var i = 0; i < last; i++) {
      final p0 = pts[i == 0 ? 0 : i - 1];
      final p1 = pts[i];
      final p2 = pts[i + 1];
      final p3 = pts[i + 2 <= last ? i + 2 : last];
      final control1X = p1.dx + (p2.dx - p0.dx) / 6.0;
      final control1Y = (p1.dy + (p2.dy - p0.dy) / 6.0).clamp(0.0, maxY);
      final control2X = p2.dx - (p3.dx - p1.dx) / 6.0;
      final control2Y = (p2.dy - (p3.dy - p1.dy) / 6.0).clamp(0.0, maxY);
      path.cubicTo(control1X, control1Y, control2X, control2Y, p2.dx, p2.dy);
    }
    return path;
  }

  @override
  bool shouldRepaint(covariant _LinePainter oldDelegate) => true;
}

class _InfluenceBarsPainter extends CustomPainter {
  _InfluenceBarsPainter({
    required this.bars,
    required this.maxMagnitude,
    required this.axisColor,
    required this.noDataColor,
    required this.colorFor,
  });

  final List<BodyEnergyInfluenceBar> bars;
  final double maxMagnitude;
  final Color axisColor;
  final Color noDataColor;
  final Color Function(BodyEnergyPrimaryInfluence) colorFor;

  @override
  void paint(Canvas canvas, Size size) {
    final centerY = size.height / 2;
    canvas.drawLine(
      Offset(0, centerY),
      Offset(size.width, centerY),
      Paint()
        ..color = axisColor
        ..strokeWidth = 1,
    );
    if (bars.isEmpty) return;

    const minBarWidth = 2.0;
    const radius = Radius.circular(2);

    for (final bar in bars) {
      final x = size.width * bar.xFraction.clamp(0.0, 1.0);
      final width = (size.width * bar.widthFraction * 0.82)
          .clamp(minBarWidth, size.width);
      final left =
          (x - width / 2).clamp(0.0, (size.width - width).clamp(0.0, size.width));
      final color = colorFor(bar.influence);
      final barPaint = Paint()..color = color;
      if (bar.charge > 0.0) {
        final height =
            ((bar.charge / maxMagnitude) * centerY).clamp(1.0, centerY);
        canvas.drawRRect(
          RRect.fromRectAndCorners(
            Rect.fromLTWH(left, centerY - height, width, height),
            topLeft: radius,
            topRight: radius,
            bottomLeft: radius,
            bottomRight: radius,
          ),
          barPaint,
        );
      }
      if (bar.drain > 0.0) {
        final height =
            ((bar.drain / maxMagnitude) * centerY).clamp(1.0, centerY);
        canvas.drawRRect(
          RRect.fromRectAndCorners(
            Rect.fromLTWH(left, centerY, width, height),
            topLeft: radius,
            topRight: radius,
            bottomLeft: radius,
            bottomRight: radius,
          ),
          barPaint,
        );
      }
      // NO_DATA buckets with no charge or drain read as a low-emphasis vertical
      // tick spanning the strip (Kotlin `BodyEnergyInfluenceBars`).
      if (bar.charge <= 0.0 &&
          bar.drain <= 0.0 &&
          bar.influence == BodyEnergyPrimaryInfluence.noData) {
        canvas.drawLine(
          Offset(x, 0),
          Offset(x, size.height),
          Paint()
            ..color = noDataColor
            ..strokeWidth = minBarWidth
            ..strokeCap = StrokeCap.round,
        );
      }
    }
  }

  @override
  bool shouldRepaint(covariant _InfluenceBarsPainter oldDelegate) => true;
}
