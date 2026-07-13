import 'package:flutter/material.dart';

import '../charts/ring_gauge.dart';

/// A large hero stat rendered inside an open-bottom progress gauge (the two
/// dashboard rings: Steps and Weekly cardio). Faithful port of the Kotlin
/// `DashboardSummaryCard` / the design-system `SummaryRingCard`.
///
/// The gauge itself is [RingGauge], which lives with the other charts — because
/// that is what it is. This file is the CARD: the tile, the tap target, and the
/// title/value/subtitle stack in the middle of the ring.
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
            child: RingGauge(
            progress: progress,
            accentColor: accentColor,
            trackColor: scheme.outlineVariant,
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
                  style: theme.textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: scheme.onSurface,
                    fontFeatures: const [FontFeature.tabularFigures()],
                  ),
                ),
                if (subtitle != null && subtitle!.trim().isNotEmpty)
                  _AutoText(
                    subtitle!,
                    maxLines: 2,
                    style: theme.textTheme.labelSmall?.copyWith(
                      color: scheme.onSurfaceVariant,
                    ),
                  ),
              ],
            ),
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
