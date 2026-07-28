import 'package:flutter/material.dart';

import '../theme/design_tokens.dart';

/// The inset, tinted container used inside cards to group a sub-section.
///
/// Only the `Neutral` and `Metric` styles are ported: those are the two the
/// callers here use. The Kotlin composable also carries `Accent`, `Warning` and
/// `Error` styles; add them when a caller needs one rather than guessing at the
/// blend factors now.
enum OpenVitalsSurfaceStyle {
  /// `surfaceContainer` — the default.
  neutral,

  /// `surfaceContainerHighest` — for a metric block nested inside a card.
  metric,
}

class OpenVitalsSurface extends StatelessWidget {
  const OpenVitalsSurface({
    super.key,
    required this.child,
    this.style = OpenVitalsSurfaceStyle.neutral,
    this.containerColor,
    this.contentPadding = EdgeInsets.zero,
    this.border,
    this.borderRadius = Radii.cardBorder,
  });

  final Widget child;
  final OpenVitalsSurfaceStyle style;

  /// Overrides the colour the [style] would pick.
  final Color? containerColor;
  final EdgeInsetsGeometry contentPadding;
  final BoxBorder? border;

  /// The card corner ([Radii.card], 12dp) — the same token [OpenVitalsCard]
  /// draws, so a surface nested in a card cannot have a different corner.
  final BorderRadius borderRadius;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final color = containerColor ??
        switch (style) {
          OpenVitalsSurfaceStyle.metric => scheme.surfaceContainerHighest,
          OpenVitalsSurfaceStyle.neutral => scheme.surfaceContainer,
        };
    return DecoratedBox(
      decoration: BoxDecoration(
        color: color,
        borderRadius: borderRadius,
        border: border,
      ),
      child: Padding(padding: contentPadding, child: child),
    );
  }
}
