import 'package:flutter/material.dart';

import '../../../domain/insights/body_energy_timeline.dart';
import '../../../ui/charts/chart_curve.dart';
import '../../../ui/charts/day_axis.dart';
import '../../../ui/charts/metric_line_plot.dart';
import '../../../ui/theme/chart_colors.dart';
import '../../../ui/theme/chart_tokens.dart';
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
        // The shared plot, at last. This chart was drawing its own line, with its
        // own spline, its own grid and no y axis at all — a 0-to-100 score with no
        // scale beside it, which is a number you cannot read. It now gets what
        // every other line in the app gets: a scale, a gradient, a reveal, and a
        // finger you can drag along it.
        MetricLinePlot(
          points: [
            for (final point in _dampened(points))
              MetricLinePlotPoint(xFraction: point.dx, value: point.dy),
          ],
          // A score DEFINED as 0 to 100. Not `ChartRange.padded`: padding it would
          // invent headroom above a ceiling and depth below a floor.
          minValue: 0,
          maxValue: 100,
          accentColor: scheme.primary,
          chartHeight: kChartHeightBodyEnergy,
          lineStrokeWidth: 2.5,
          valueFormatter: (value) => value.round().toString(),
          scrubLabelBuilder: (point) => (
            point.value.round().toString(),
            _clockAt(point.xFraction, context),
          ),
        ),
        const SizedBox(height: 6),
        // Inset to match the plot above. The line's x, the bar's x and the hour
        // row's 12:00 all have to come from the same fraction of the same day — if
        // one drifts, the card says the workout happened at a time it did not.
        Padding(
          padding: const EdgeInsets.only(left: kChartPlotInset),
          child: SizedBox(
            height: kChartHeightInfluenceStrip,
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
        ),
        const SizedBox(height: 6),
        // The plot NOW has a y-axis label column, so the hour row insets to match
        // it — it used to be 0 precisely because the old painters had no gutter.
        const DayAxisLabels(),
      ],
    );
  }
}

/// The accent colour for a Body Energy influence (port of the Kotlin
/// `bodyEnergyInfluenceColor`).
/// The score is an integer sampled per bucket, so the raw series is a staircase.
/// [movingAverageY] damps it before the curve is drawn — a DATA decision, and one
/// that has to survive the move onto the shared plot, or the line goes back to
/// tracing the steps.
List<Offset> _dampened(List<BodyEnergyChartPoint> points) => movingAverageY(
      [for (final point in points) Offset(point.xFraction, point.score)],
    );

/// The clock time a fraction of the way through the day. The score points carry no
/// timestamp of their own — only where they sit across the day — which is all the
/// tooltip needs.
String _clockAt(double fraction, BuildContext context) {
  final minutes = (fraction.clamp(0.0, 1.0) * Duration.minutesPerDay).round();
  return TimeOfDay(
    hour: (minutes ~/ 60).clamp(0, 23),
    minute: minutes % 60,
  ).format(context);
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

    for (final bar in bars) {
      final x = size.width * bar.xFraction.clamp(0.0, 1.0);
      final width = (size.width * bar.widthFraction * 0.82)
          .clamp(minBarWidth, size.width);
      final left =
          (x - width / 2).clamp(0.0, (size.width - width).clamp(0.0, size.width));
      // The one bar-corner rule, instead of this strip's own flat 2px — which made
      // it the only bar in the app with square-ish shoulders.
      final radius = Radius.circular(chartBarRadius(width));
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
