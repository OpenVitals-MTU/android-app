import 'package:flutter/material.dart';

import '../theme/design_tokens.dart';

/// The canonical flat card used across the shell and charts: zero elevation, a
/// `surfaceContainer` background, and the [Radii.card] (12dp) shape.
///
/// The corner comes from the token rather than a literal because this card and
/// [OpenVitalsSurface] have to agree, and for a long time they only agreed by
/// coincidence — the same `12` typed into two files, either of which could have
/// been retuned alone.
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
      shape: const RoundedRectangleBorder(borderRadius: Radii.cardBorder),
      child: onTap == null ? child : InkWell(onTap: onTap, child: child),
    );
  }
}
