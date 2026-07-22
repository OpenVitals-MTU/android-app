import 'package:flutter/material.dart';

/// A small dashboard stat tile (Distance, Total calories, Sleep, …). Faithful
/// port of the Kotlin `MetricStatCard` / the design-system `MetricStatCard`.
///
/// Layout: a 28dp accent-tinted icon circle (16dp glyph), then a title
/// ([labelMedium], on-surface-variant) over a value ([titleMedium], w600) with
/// an optional unit appended at medium weight, and an optional subtitle
/// ([labelSmall]). An optional 3dp accent progress underline is pinned to the
/// bottom edge and clipped to the rounded corners. Two tiles fit per row.
class MetricStatCard extends StatelessWidget {
  const MetricStatCard({
    super.key,
    required this.title,
    required this.value,
    required this.icon,
    required this.accentColor,
    this.unit,
    this.subtitle,
    this.message,
    this.showTitle = true,
    this.progress,
    this.onTap,
    this.trailing,
  });

  final String title;

  /// The formatted value; ignored when [message] is set.
  final String value;

  /// Optional unit appended after [value] at medium weight (e.g. `km`).
  final String? unit;
  final IconData icon;
  final Color accentColor;
  final String? subtitle;

  /// An optional control in the card's trailing edge, with its own tap target.
  ///
  /// Used by the device tile, whose Sync button acts in place while the rest of
  /// the card opens the device view. Deliberately narrow: it is the only action
  /// allowed outside that view, and only because a watch that has stopped
  /// syncing is noticed here.
  final Widget? trailing;

  /// A no-data / loading message shown in place of the value, in the muted
  /// on-surface-variant colour.
  final String? message;
  final bool showTitle;

  /// Progress fraction in `[0, 1]`; when non-null, draws the accent underline.
  final double? progress;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final iconContainerColor = Color.alphaBlend(
      accentColor.withValues(alpha: 0.16),
      scheme.surfaceContainerHighest,
    );

    return Material(
      color: scheme.surfaceContainer,
      borderRadius: const BorderRadius.all(Radius.circular(12)),
      // Hard-edge clip keeps the progress underline inside the rounded corners
      // without the offscreen saveLayer cost of an antialiased clip.
      clipBehavior: Clip.hardEdge,
      child: InkWell(
        onTap: onTap,
        child: Stack(
          // Ports the Kotlin `Box(Modifier.fillMaxSize())`: when the tile is
          // given a fixed height by its grid cell, the content centres in it
          // rather than pinning to the top-left (the Stack default). With a
          // loose height the card still shrink-wraps to its content.
          alignment: Alignment.centerLeft,
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
              child: Row(
                children: [
                  Container(
                    width: 28,
                    height: 28,
                    decoration: BoxDecoration(
                      color: iconContainerColor,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(icon, size: 16, color: accentColor),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (showTitle)
                          Text(
                            title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.labelMedium?.copyWith(
                              color: scheme.onSurfaceVariant,
                            ),
                          ),
                        _ValueLine(
                          value: value,
                          unit: unit,
                          message: message,
                          theme: theme,
                        ),
                        if (message == null &&
                            subtitle != null &&
                            subtitle!.trim().isNotEmpty)
                          Text(
                            subtitle!,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.labelSmall?.copyWith(
                              color: scheme.onSurfaceVariant,
                            ),
                          ),
                      ],
                    ),
                  ),
                  if (trailing != null) ...[
                    const SizedBox(width: 6),
                    trailing!,
                  ],
                ],
              ),
            ),
            if (progress != null)
              Positioned(
                left: 0,
                right: 0,
                bottom: 0,
                child: FractionallySizedBox(
                  alignment: Alignment.centerLeft,
                  widthFactor: progress!.clamp(0.0, 1.0),
                  child: Container(
                    height: 3,
                    color: accentColor.withValues(alpha: 0.55),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _ValueLine extends StatelessWidget {
  const _ValueLine({
    required this.value,
    required this.unit,
    required this.message,
    required this.theme,
  });

  final String value;
  final String? unit;
  final String? message;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    final scheme = theme.colorScheme;
    if (message != null) {
      return Text(
        message!,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: theme.textTheme.titleMedium?.copyWith(
          fontWeight: FontWeight.w600,
          color: scheme.onSurfaceVariant,
        ),
      );
    }
    final base = theme.textTheme.titleMedium?.copyWith(
      fontWeight: FontWeight.w600,
      color: scheme.onSurface,
    );
    final hasUnit = unit != null && unit!.trim().isNotEmpty;
    return Text.rich(
      TextSpan(
        style: base,
        children: [
          TextSpan(text: value),
          if (hasUnit)
            TextSpan(
              text: ' $unit',
              style: base?.copyWith(fontWeight: FontWeight.w500),
            ),
        ],
      ),
      maxLines: 1,
      overflow: TextOverflow.ellipsis,
    );
  }
}
