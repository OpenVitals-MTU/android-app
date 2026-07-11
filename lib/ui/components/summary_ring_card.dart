import 'dart:math' as math;

import 'package:flutter/material.dart';

/// A large hero stat rendered inside an open-bottom progress gauge (the two
/// dashboard rings: Steps and Weekly cardio). Faithful port of the Kotlin
/// `DashboardSummaryCard` / the design-system `SummaryRingCard`.
///
/// The gauge is a 280°-sweep arc that starts at 130° (open at the bottom) drawn
/// clockwise with round caps: an [colorScheme.outlineVariant] track under an
/// accent fill at 0.72 opacity. Title / value / subtitle are stacked in the
/// centre. The card is a flat `surfaceContainer` tile with a 12dp radius and is
/// intended to be laid out square (two per row in an [Expanded]).
class SummaryRingCard extends StatelessWidget {
  const SummaryRingCard({
    super.key,
    required this.title,
    required this.value,
    required this.accentColor,
    this.subtitle,
    this.progress = 0,
    this.onTap,
  });

  final String title;
  final String value;
  final String? subtitle;

  /// Progress fraction in `[0, 1]`; clamped before use.
  final double progress;
  final Color accentColor;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return AspectRatio(
      aspectRatio: 1,
      child: Material(
        color: scheme.surfaceContainer,
        borderRadius: const BorderRadius.all(Radius.circular(12)),
        clipBehavior: onTap == null ? Clip.none : Clip.hardEdge,
        child: InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(6),
            child: LayoutBuilder(
              builder: (context, constraints) {
                final side = math.min(
                  constraints.maxWidth,
                  constraints.maxHeight,
                );
                final stroke = (side * 0.09).clamp(5.0, 10.0);
                return Center(
                  child: SizedBox.square(
                    dimension: side,
                    child: Stack(
                      children: [
                        Positioned.fill(
                          child: CustomPaint(
                            painter: _RingGaugePainter(
                              progress: progress.clamp(0.0, 1.0),
                              accentColor: accentColor,
                              trackColor: scheme.outlineVariant,
                            ),
                          ),
                        ),
                        Positioned.fill(
                          child: Padding(
                            padding: EdgeInsets.all(stroke + 6),
                            child: Center(
                              child: Column(
                                mainAxisSize: MainAxisSize.min,
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  _AutoText(
                                    title,
                                    style: theme.textTheme.labelSmall?.copyWith(
                                      color: scheme.onSurfaceVariant,
                                    ),
                                  ),
                                  _AutoText(
                                    value,
                                    style:
                                        theme.textTheme.headlineSmall?.copyWith(
                                      fontWeight: FontWeight.bold,
                                      color: scheme.onSurface,
                                      fontFeatures: const [
                                        FontFeature.tabularFigures(),
                                      ],
                                    ),
                                  ),
                                  if (subtitle != null &&
                                      subtitle!.trim().isNotEmpty)
                                    _AutoText(
                                      subtitle!,
                                      maxLines: 2,
                                      style:
                                          theme.textTheme.labelSmall?.copyWith(
                                        color: scheme.onSurfaceVariant,
                                      ),
                                    ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        ),
      ),
    );
  }
}

/// Shrinks its text to fit the available width, approximating the Kotlin
/// `AutoResizeText` used inside the ring.
class _AutoText extends StatelessWidget {
  const _AutoText(this.text, {this.style, this.maxLines = 1});

  final String text;
  final TextStyle? style;
  final int maxLines;

  @override
  Widget build(BuildContext context) {
    return FittedBox(
      fit: BoxFit.scaleDown,
      child: Text(
        text,
        maxLines: maxLines,
        textAlign: TextAlign.center,
        style: style,
      ),
    );
  }
}

class _RingGaugePainter extends CustomPainter {
  _RingGaugePainter({
    required this.progress,
    required this.accentColor,
    required this.trackColor,
  });

  // Open-bottom gauge: start at 130°, sweep 280° clockwise (degrees → radians).
  static const double _startAngle = 130 * math.pi / 180;
  static const double _sweepAngle = 280 * math.pi / 180;

  final double progress;
  final Color accentColor;
  final Color trackColor;

  @override
  void paint(Canvas canvas, Size size) {
    final stroke = (size.shortestSide * 0.09).clamp(5.0, 10.0);
    final radius = (size.shortestSide - stroke) / 2 - 2;
    if (radius <= 0) return;
    final rect = Rect.fromCircle(center: size.center(Offset.zero), radius: radius);

    final trackPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = stroke
      ..strokeCap = StrokeCap.round
      ..color = trackColor;
    canvas.drawArc(rect, _startAngle, _sweepAngle, false, trackPaint);

    if (progress > 0) {
      final fillPaint = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = stroke
        ..strokeCap = StrokeCap.round
        ..color = accentColor.withValues(alpha: 0.72);
      canvas.drawArc(rect, _startAngle, _sweepAngle * progress, false, fillPaint);
    }
  }

  @override
  bool shouldRepaint(_RingGaugePainter oldDelegate) =>
      oldDelegate.progress != progress ||
      oldDelegate.accentColor != accentColor ||
      oldDelegate.trackColor != trackColor;
}
