import 'dart:math' as math;

import 'package:flutter/material.dart';

/// The app's one radial chart: an open-bottom arc gauge.
///
/// It lived inside `summary_ring_card.dart` — a chart hiding in the components
/// folder, sharing no colour, no stroke and no token with any of the other
/// sixteen. Pulled out here so it is one of the charts, which is what lets a
/// restyle reach it: "the ring sweeps in with the bars" is now a change to this
/// file, and not an argument about whether a card counts as a chart.
///
/// Geometry unchanged: start at 130°, sweep 280° clockwise, round caps. The gap
/// at the bottom is the point — a closed ring reads as a pie, and this is a
/// gauge.
class RingGauge extends StatelessWidget {
  const RingGauge({
    super.key,
    required this.progress,
    required this.accentColor,
    required this.trackColor,
    this.child,
  });

  /// `[0, 1]`. Clamped: you can walk 12,000 steps against a 10,000 goal, and the
  /// arc should stop at the top rather than lap itself.
  final double progress;

  final Color accentColor;
  final Color trackColor;

  /// Whatever sits in the middle of the ring — a value, usually.
  final Widget? child;

  /// The stroke a gauge of [side] gets. Kept as a function rather than a
  /// constant because the ring is laid out square inside whatever space the
  /// dashboard's `Expanded` gives it, so its thickness has to scale with it.
  static double strokeFor(double side) => (side * 0.09).clamp(5.0, 10.0);

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final side = math.min(constraints.maxWidth, constraints.maxHeight);
        final stroke = strokeFor(side);
        return Center(
          child: SizedBox.square(
            dimension: side,
            child: Stack(
              children: [
                Positioned.fill(
                  child: CustomPaint(
                    painter: RingGaugePainter(
                      progress: progress.clamp(0.0, 1.0),
                      accentColor: accentColor,
                      trackColor: trackColor,
                    ),
                  ),
                ),
                if (child case final child?)
                  Positioned.fill(
                    child: Padding(
                      // Clear of the arc, plus a hair — text that touches the
                      // ring reads as part of it.
                      padding: EdgeInsets.all(stroke + 6),
                      child: Center(child: child),
                    ),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class RingGaugePainter extends CustomPainter {
  RingGaugePainter({
    required this.progress,
    required this.accentColor,
    required this.trackColor,
  });

  /// Open-bottom gauge: start at 130°, sweep 280° clockwise (degrees → radians).
  static const double startAngle = 130 * math.pi / 180;
  static const double sweepAngle = 280 * math.pi / 180;

  final double progress;
  final Color accentColor;
  final Color trackColor;

  @override
  void paint(Canvas canvas, Size size) {
    final stroke = RingGauge.strokeFor(size.shortestSide);
    final radius = (size.shortestSide - stroke) / 2 - 2;
    if (radius <= 0) return;
    final rect =
        Rect.fromCircle(center: size.center(Offset.zero), radius: radius);

    final trackPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = stroke
      ..strokeCap = StrokeCap.round
      ..color = trackColor;
    canvas.drawArc(rect, startAngle, sweepAngle, false, trackPaint);

    if (progress > 0) {
      final fillPaint = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = stroke
        ..strokeCap = StrokeCap.round
        ..color = accentColor.withValues(alpha: 0.72);
      canvas.drawArc(rect, startAngle, sweepAngle * progress, false, fillPaint);
    }
  }

  @override
  bool shouldRepaint(RingGaugePainter oldDelegate) =>
      oldDelegate.progress != progress ||
      oldDelegate.accentColor != accentColor ||
      oldDelegate.trackColor != trackColor;
}
