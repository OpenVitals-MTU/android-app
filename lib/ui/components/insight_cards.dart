import 'package:flutter/material.dart';

import 'ov_card.dart';

/// Port of the Kotlin `InsightCards.kt`: the small stat cards every metric
/// detail screen lays out in a two-column grid.

/// Kotlin `InsightStat`.
@immutable
class InsightStat {
  const InsightStat({
    required this.title,
    required this.value,
    required this.unit,
    required this.icon,
    required this.accentColor,
  });

  final String title;
  final String value;
  final String unit;
  final IconData icon;
  final Color accentColor;
}

/// Kotlin `InsightStatGrid`: stats chunked into rows of [columns].
///
/// A trailing odd stat occupies one column and leaves the rest of its row empty,
/// as Compose's `Row` + `weight(1f)` does — it does not stretch to full width.
class InsightStatGrid extends StatelessWidget {
  const InsightStatGrid({
    super.key,
    required this.stats,
    this.columns = 2,
  });

  final List<InsightStat> stats;
  final int columns;

  @override
  Widget build(BuildContext context) {
    if (stats.isEmpty) return const SizedBox.shrink();
    final perRow = columns < 1 ? 1 : columns;

    final rows = <List<InsightStat>>[];
    for (var i = 0; i < stats.length; i += perRow) {
      rows.add(stats.sublist(i, (i + perRow).clamp(0, stats.length)));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 12,
      children: [
        for (final row in rows)
          IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                for (var i = 0; i < perRow; i++) ...[
                  if (i > 0) const SizedBox(width: 12),
                  Expanded(
                    child: i < row.length
                        ? _InsightStatCard(stat: row[i])
                        // Keeps the last row's cards the same width as the rest.
                        : const SizedBox.shrink(),
                  ),
                ],
              ],
            ),
          ),
      ],
    );
  }
}

class _InsightStatCard extends StatelessWidget {
  const _InsightStatCard({required this.stat});

  final InsightStat stat;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Icon(stat.icon, size: 24, color: stat.accentColor),
            const SizedBox(height: 16),
            // scaleDown instead of ellipsis: a shrunk numeral stays true where
            // a truncated one misreads, and the unit could still overflow on
            // its own at large font scales.
            FittedBox(
              fit: BoxFit.scaleDown,
              child: Row(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.baseline,
                textBaseline: TextBaseline.alphabetic,
                children: [
                  Text(
                    stat.value,
                    style: theme.textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: theme.colorScheme.onSurface,
                    ),
                  ),
                  if (stat.unit.trim().isNotEmpty) ...[
                    const SizedBox(width: 4),
                    Text(
                      stat.unit,
                      style: theme.textTheme.bodyMedium
                          ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                    ),
                  ],
                ],
              ),
            ),
            const SizedBox(height: 8),
            Text(
              stat.title,
              textAlign: TextAlign.center,
              style: theme.textTheme.labelMedium?.copyWith(
                fontWeight: FontWeight.w600,
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
