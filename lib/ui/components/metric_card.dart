import 'package:flutter/material.dart';

import '../../core/period/time_range.dart';
import 'ov_card.dart';

/// A single-metric summary card: icon + title, a large value/unit row, and an
/// optional subtitle and source chip. Port of Kotlin `MetricCard`.
class MetricCard extends StatelessWidget {
  const MetricCard({
    super.key,
    required this.title,
    required this.value,
    required this.unit,
    required this.icon,
    required this.accentColor,
    this.subtitle,
    this.source,
    this.onTap,
  });

  final String title;
  final String value;
  final String unit;
  final IconData icon;
  final Color accentColor;
  final String? subtitle;
  final String? source;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, color: accentColor, size: 20),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    title,
                    style: theme.textTheme.labelMedium
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            MetricValueRow(value: value, unit: unit),
            if (subtitle != null) ...[
              const SizedBox(height: 4),
              Text(
                subtitle!,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
            if (source != null) ...[
              const SizedBox(height: 8),
              SourceChip(source: source!),
            ],
          ],
        ),
      ),
    );
  }
}

/// The empty/placeholder variant of [MetricCard]. Port of Kotlin
/// `MetricCardPlaceholder`.
class MetricCardPlaceholder extends StatelessWidget {
  const MetricCardPlaceholder({
    super.key,
    required this.title,
    required this.icon,
    required this.accentColor,
    required this.message,
    this.showHeader = true,
    this.onTap,
  });

  final String title;
  final IconData icon;
  final Color accentColor;
  final String message;
  final bool showHeader;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (showHeader) ...[
              Row(
                children: [
                  Icon(icon, color: accentColor.withValues(alpha: 0.5), size: 20),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      title,
                      style: theme.textTheme.labelMedium
                          ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
            ],
            Text(
              message,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant.withValues(alpha: 0.6),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// The large value + unit row, baseline aligned. Port of Kotlin
/// `MetricValueRow`.
class MetricValueRow extends StatelessWidget {
  const MetricValueRow({super.key, required this.value, required this.unit});

  final String value;
  final String unit;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.baseline,
      textBaseline: TextBaseline.alphabetic,
      children: [
        Text(
          value,
          style: theme.textTheme.headlineMedium?.copyWith(
            fontWeight: FontWeight.bold,
            color: theme.colorScheme.onSurface,
          ),
        ),
        if (unit.trim().isNotEmpty) ...[
          const SizedBox(width: 4),
          Text(
            unit,
            style: theme.textTheme.bodyMedium
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
        ],
      ],
    );
  }
}

const int _sourceLabelMaxCharacters = 24;
const String _sourceLabelOverflow = '...';

String _truncatedSourceLabel(String label) {
  final trimmed = label.trim();
  if (trimmed.length <= _sourceLabelMaxCharacters) return trimmed;
  return '${trimmed.substring(0, _sourceLabelMaxCharacters - _sourceLabelOverflow.length).trimRight()}$_sourceLabelOverflow';
}

/// A small data-source attribution pill. The Kotlin `SourceChip` resolves the
/// package name to an installed-app label + icon via a platform resolver; the
/// Flutter port has no such resolver, so it shows the (truncated) source string
/// directly. Port of Kotlin `SourceChip` / `DataSourceAttribution`.
class SourceChip extends StatelessWidget {
  const SourceChip({super.key, required this.source});

  final String source;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest,
        borderRadius: const BorderRadius.all(Radius.circular(8)),
      ),
      child: Text(
        _truncatedSourceLabel(source),
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: theme.textTheme.labelSmall
            ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
      ),
    );
  }
}

/// A subdued section header. Port of Kotlin `SectionHeader`.
class SectionHeader extends StatelessWidget {
  const SectionHeader(this.text, {super.key});

  final String text;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Text(
        text,
        style: theme.textTheme.titleSmall
            ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
      ),
    );
  }
}

/// The Day/Week/Month/Year segmented control. Port of Kotlin
/// `TimeRangeSelector`.
class TimeRangeSelector extends StatelessWidget {
  const TimeRangeSelector({
    super.key,
    required this.selected,
    required this.onSelect,
  });

  final TimeRange selected;
  final ValueChanged<TimeRange> onSelect;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        children: [
          for (var index = 0; index < TimeRange.values.length; index++) ...[
            if (index > 0) const SizedBox(width: 4),
            Expanded(
              child: _TimeRangeSegment(
                range: TimeRange.values[index],
                selected: TimeRange.values[index] == selected,
                onSelect: onSelect,
                scheme: scheme,
                textStyle: theme.textTheme.labelLarge,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _TimeRangeSegment extends StatelessWidget {
  const _TimeRangeSegment({
    required this.range,
    required this.selected,
    required this.onSelect,
    required this.scheme,
    required this.textStyle,
  });

  final TimeRange range;
  final bool selected;
  final ValueChanged<TimeRange> onSelect;
  final ColorScheme scheme;
  final TextStyle? textStyle;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: selected ? scheme.primaryContainer : scheme.surfaceContainer,
      borderRadius: const BorderRadius.all(Radius.circular(16)),
      clipBehavior: Clip.hardEdge,
      child: InkWell(
        onTap: () => onSelect(range),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 12),
          child: Text(
            range.label,
            textAlign: TextAlign.center,
            style: textStyle?.copyWith(
              color: selected
                  ? scheme.onPrimaryContainer
                  : scheme.onSurfaceVariant,
              fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
            ),
          ),
        ),
      ),
    );
  }
}
