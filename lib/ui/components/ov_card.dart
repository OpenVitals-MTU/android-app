import 'package:flutter/material.dart';

/// The canonical flat card used across the shell and charts, ported from the
/// Kotlin `OpenVitalsCard` (in `DetailCards.kt`): zero elevation, a
/// `surfaceContainer` background, and the Material medium (12dp) shape.
class OpenVitalsCard extends StatelessWidget {
  const OpenVitalsCard({
    super.key,
    required this.child,
    this.onTap,
    this.color,
  });

  final Widget child;
  final VoidCallback? onTap;

  /// Overrides the default `surfaceContainer` background.
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      color: color ?? scheme.surfaceContainer,
      // Avoid the antialiased clip (an offscreen saveLayer re-rasterized every
      // frame while scrolling — a major scroll-jank source since this card is
      // used app-wide). The Card already paints its own rounded, filled shape;
      // non-interactive cards need no child clip at all, and interactive ones
      // only need a cheap hard-edge clip to keep the ink ripple in the corners.
      clipBehavior: onTap == null ? Clip.none : Clip.hardEdge,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(12)),
      ),
      child: onTap == null ? child : InkWell(onTap: onTap, child: child),
    );
  }
}
