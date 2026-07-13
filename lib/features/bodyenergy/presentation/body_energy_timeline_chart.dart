import 'package:flutter/material.dart';

import '../../../domain/insights/body_energy_timeline.dart';
import '../../../ui/charts/chart_axis.dart';
import '../../../ui/charts/chart_curve.dart';
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
    // Five guides — 0/25/50/75/100 — and the axis along the BOTTOM: a score out
    // of a hundred wants a floor, not a scale down its side. Both of those used
    // to be a hand-rolled loop here, beside an identical one in the library.
    drawYAxisGuides(
      canvas,
      size,
      gridColor: gridColor,
      axisColor: axisColor,
      lineCount: 5,
      axisLine: ChartAxisLine.baseline,
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
    // staircase. Damp that quantization before splining, or the curve just traces
    // the steps and reads as jagged.
    final positioned = movingAverageY(rawPoints);

    final linePaint = Paint()
      ..color = lineColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.5
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;
    if (positioned.length >= 3) {
      // `smoothPath` — the library's monotone cubic — and not the Catmull-Rom
      // this used to carry.
      //
      // That spline clamped its control points to [0, maxY], which is a confession:
      // it overshoots. And clamping a CONTROL point does not even keep the rendered
      // cubic inside the bounds — a Bézier can still bulge past a clamped handle,
      // which is why this chart's own golden showed the trace leaving the top of
      // the plot. On a score that is DEFINED as 0 to 100, a curve that climbs above
      // 100, or dips below the lowest reading of the day, is not a smoothing
      // artefact. It is a false statement about your body.
      //
      // Fritsch–Carlson cannot do it: where the data rises the curve rises, and it
      // never leaves the interval its samples live in. The line becomes less swoopy.
      // It also becomes true.
      canvas.drawPath(smoothPath(positioned), linePaint);
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
