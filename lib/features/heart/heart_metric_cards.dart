import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../core/period/time_range.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../core/time/local_date.dart';
import '../../domain/model/heart_models.dart';
import '../../domain/model/vitals_models.dart';
import '../../l10n/app_localizations.dart';
import '../../ui/charts/metric_line_plot.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/theme/app_colors.dart';

/// Cards used across the heart + vitals detail screens, ported from the Kotlin
/// `HeartCards.kt`, `HeartMetricContent.kt` (threshold checks) and
/// `HeartVitalsRows.kt`.

/// A label → value statistics card used by the body detail screens, a trimmed
/// port of the various Kotlin `*StatisticsContent` grids.
class HeartStatisticsCard extends StatelessWidget {
  const HeartStatisticsCard({
    super.key,
    required this.rows,
    required this.accentColor,
    this.title = 'Statistics',
  });

  final List<(String, String)> rows;
  final Color accentColor;
  final String title;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.insights, color: accentColor, size: 20),
                const SizedBox(width: 8),
                Text(
                  title,
                  style: theme.textTheme.labelMedium
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
            ),
            const SizedBox(height: 12),
            for (final row in rows)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(row.$1, style: theme.textTheme.bodyMedium),
                    Text(
                      row.$2,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: theme.colorScheme.onSurface,
                      ),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}

// ── Heart-rate threshold checks (Kotlin `HeartMetricContent.kt`) ─────────────

/// Kotlin `HeartRateThresholdStepBpm` / `HeartRateThresholdMinimumGapBpm`.
const int heartRateThresholdStepBpm = 5;
const int heartRateThresholdMinimumGapBpm = 5;

enum HeartRateThresholdCheckType { high, low }

/// Kotlin `HeartRateThresholdCheck`.
@immutable
class HeartRateThresholdCheck {
  const HeartRateThresholdCheck({
    required this.type,
    required this.thresholdBpm,
    this.count = 0,
    this.hasData = false,
  });

  final HeartRateThresholdCheckType type;
  final int thresholdBpm;
  final int count;
  final bool hasData;
}

/// Kotlin `HeartPeriodLoadResult.heartRateThresholdCheck`
/// (`HeartPresentationMapper.kt`): samples at/above|below the threshold for a
/// day, days whose max/min crosses it otherwise.
HeartRateThresholdCheck heartRateThresholdCheck({
  required TimeRange selectedRange,
  required HeartRateThresholdCheckType type,
  required int thresholdBpm,
  required List<HeartRateSample> daySamples,
  required List<HeartRateSummary> dailySummaries,
}) {
  final isDay = selectedRange == TimeRange.day;
  final hasData = isDay ? daySamples.isNotEmpty : dailySummaries.isNotEmpty;
  final int count;
  switch (type) {
    case HeartRateThresholdCheckType.high:
      count = isDay
          ? daySamples.where((s) => s.beatsPerMinute >= thresholdBpm).length
          : dailySummaries.where((s) => s.maxBpm >= thresholdBpm).length;
    case HeartRateThresholdCheckType.low:
      count = isDay
          ? daySamples.where((s) => s.beatsPerMinute <= thresholdBpm).length
          : dailySummaries.where((s) => s.minBpm <= thresholdBpm).length;
  }
  return HeartRateThresholdCheck(
    type: type,
    thresholdBpm: thresholdBpm,
    count: count,
    hasData: hasData,
  );
}

String _thresholdSubtitle(
  HeartRateThresholdCheck check,
  TimeRange selectedRange,
  AppLocalizations l10n,
) {
  final isDay = selectedRange == TimeRange.day;
  return switch (check.type) {
    HeartRateThresholdCheckType.high => isDay
        ? l10n.heartRateSamplesAtOrAbove(check.thresholdBpm)
        : l10n.heartRateDaysAtOrAbove(check.thresholdBpm),
    HeartRateThresholdCheckType.low => isDay
        ? l10n.heartRateSamplesAtOrBelow(check.thresholdBpm)
        : l10n.heartRateDaysAtOrBelow(check.thresholdBpm),
  };
}

/// Kotlin `HeartRateThresholdChecksContent`: the "Heart rate checks" section —
/// side-by-side high/low threshold cards with +/- steppers.
class HeartRateThresholdChecksContent extends StatelessWidget {
  const HeartRateThresholdChecksContent({
    super.key,
    required this.highCheck,
    required this.lowCheck,
    required this.selectedRange,
    required this.unitFormatter,
    required this.onDecreaseHighThreshold,
    required this.onIncreaseHighThreshold,
    required this.onDecreaseLowThreshold,
    required this.onIncreaseLowThreshold,
  });

  final HeartRateThresholdCheck highCheck;
  final HeartRateThresholdCheck lowCheck;
  final TimeRange selectedRange;
  final UnitFormatter unitFormatter;
  final VoidCallback onDecreaseHighThreshold;
  final VoidCallback onIncreaseHighThreshold;
  final VoidCallback onDecreaseLowThreshold;
  final VoidCallback onIncreaseLowThreshold;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 8),
          child: Text(
            l10n.heartRateHealthChecksTitle,
            style: theme.textTheme.titleSmall
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
        ),
        const SizedBox(height: 8),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: HeartRateThresholdCheckCard(
                check: highCheck,
                title: l10n.heartRateHighTitle,
                selectedRange: selectedRange,
                unitFormatter: unitFormatter,
                onDecreaseThreshold: onDecreaseHighThreshold,
                onIncreaseThreshold: onIncreaseHighThreshold,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: HeartRateThresholdCheckCard(
                check: lowCheck,
                title: l10n.heartRateLowTitle,
                selectedRange: selectedRange,
                unitFormatter: unitFormatter,
                onDecreaseThreshold: onDecreaseLowThreshold,
                onIncreaseThreshold: onIncreaseLowThreshold,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

/// Kotlin `HeartRateThresholdCheckCard`.
class HeartRateThresholdCheckCard extends StatelessWidget {
  const HeartRateThresholdCheckCard({
    super.key,
    required this.check,
    required this.title,
    required this.selectedRange,
    required this.unitFormatter,
    required this.onDecreaseThreshold,
    required this.onIncreaseThreshold,
  });

  final HeartRateThresholdCheck check;
  final String title;
  final TimeRange selectedRange;
  final UnitFormatter unitFormatter;
  final VoidCallback onDecreaseThreshold;
  final VoidCallback onIncreaseThreshold;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  check.type == HeartRateThresholdCheckType.high
                      ? Icons.favorite
                      : Icons.favorite_border,
                  size: 20,
                  color: AppColors.heart,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    title,
                    style: theme.textTheme.titleSmall
                        ?.copyWith(color: theme.colorScheme.onSurface),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              check.hasData ? unitFormatter.count(check.count) : l10n.noData,
              style: theme.textTheme.headlineSmall?.copyWith(
                color: AppColors.heart,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 10),
            Text(
              _thresholdSubtitle(check, selectedRange, l10n),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                IconButton(
                  onPressed: onDecreaseThreshold,
                  tooltip: l10n.cdDecreaseHrThreshold,
                  iconSize: 18,
                  visualDensity: VisualDensity.compact,
                  icon: const Icon(Icons.remove),
                ),
                IconButton(
                  onPressed: onIncreaseThreshold,
                  tooltip: l10n.cdIncreaseHrThreshold,
                  iconSize: 18,
                  visualDensity: VisualDensity.compact,
                  icon: const Icon(Icons.add),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

// ── Day cards (Kotlin `HeartCards.kt`) ───────────────────────────────────────

/// Kotlin `HeartRateEmptyDayCard`.
class HeartRateEmptyDayCard extends StatelessWidget {
  const HeartRateEmptyDayCard({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              l10n.messageNoHeartSamplesDay,
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: theme.colorScheme.onSurface),
            ),
            const SizedBox(height: 6),
            Text(
              l10n.messageHeartEmptyHint,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

/// Kotlin `RestingHRDayCard` / `HRVDayCard`: the single-value hero card of a
/// day's resting HR or HRV.
class HeartDayValueCard extends StatelessWidget {
  const HeartDayValueCard({
    super.key,
    required this.title,
    required this.value,
  });

  final String title;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: theme.textTheme.labelMedium
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 4),
            Text(
              value,
              style: theme.textTheme.headlineSmall
                  ?.copyWith(color: AppColors.heart),
            ),
          ],
        ),
      ),
    );
  }
}

/// Kotlin `HeartRateTimelineCard` / `HrvTimelineCard`: the intraday sample
/// timeline — avg/range/samples header, a normalized-x line plot over the day,
/// hour marks and a recorded-window footnote.
class HeartTimelineCard extends StatelessWidget {
  const HeartTimelineCard({
    super.key,
    required this.date,
    required this.points,
    required this.averageText,
    required this.rangeText,
    required this.valueFormatter,
    this.minValue,
    this.maxValue,
  });

  final LocalDate date;

  /// Instant → value samples, in any order.
  final List<(DateTime, double)> points;
  final String averageText;
  final String rangeText;
  final String Function(double) valueFormatter;

  /// Padded axis bounds; default to min-5/max+5 like Kotlin.
  final double? minValue;
  final double? maxValue;

  @override
  Widget build(BuildContext context) {
    if (points.isEmpty) return const SizedBox.shrink();
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();

    final sorted = [...points]..sort((a, b) => a.$1.compareTo(b.$1));
    final values = sorted.map((p) => p.$2).toList();
    final min = values.reduce((a, b) => a < b ? a : b);
    final max = values.reduce((a, b) => a > b ? a : b);
    final dayStart = DateTime(date.year, date.month, date.day);
    final dayEnd = dayStart.add(const Duration(days: 1));
    final dayDurationMs = (dayEnd.millisecondsSinceEpoch -
            dayStart.millisecondsSinceEpoch)
        .clamp(1, 1 << 62);
    final timeFormat = DateFormat.jm(locale);

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child:
                      _TimelineStat(label: l10n.summaryAverage, value: averageText),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: _TimelineStat(label: l10n.summaryRange, value: rangeText),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: _TimelineStat(
                    label: l10n.summarySamples,
                    value: '${sorted.length}',
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            MetricLinePlot(
              points: [
                for (final (time, value) in sorted)
                  MetricLinePlotPoint(
                    xFraction: ((time.toLocal().millisecondsSinceEpoch -
                                dayStart.millisecondsSinceEpoch) /
                            dayDurationMs)
                        .clamp(0.0, 1.0),
                    value: value,
                  ),
              ],
              minValue: minValue ?? (min - 5),
              maxValue: maxValue ?? (max + 5),
              accentColor: AppColors.heart,
              valueFormatter: valueFormatter,
              drawPoints: true,
              pointRadius: 3,
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                for (final label in const ['00:00', '06:00', '12:00', '18:00', '24:00'])
                  Text(
                    label,
                    style: theme.textTheme.labelSmall
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              l10n.summaryRecorded(
                timeFormat.format(sorted.first.$1.toLocal()),
                timeFormat.format(sorted.last.$1.toLocal()),
              ),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

class _TimelineStat extends StatelessWidget {
  const _TimelineStat({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: theme.textTheme.labelSmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: theme.textTheme.titleSmall?.copyWith(
            fontWeight: FontWeight.w600,
            color: theme.colorScheme.onSurface,
          ),
        ),
      ],
    );
  }
}

// ── Day rows (Kotlin `HeartCards.kt` / `HeartVitalsRows.kt`) ─────────────────

/// Kotlin `HeartRateDayRow`: a day's avg + min-max range.
class HeartRateDayRow extends StatelessWidget {
  const HeartRateDayRow({
    super.key,
    required this.summary,
    required this.unitFormatter,
  });

  final HeartRateSummary summary;
  final UnitFormatter unitFormatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Expanded(
              child: Text(
                DateFormat.MMMEd(locale).format(DateTime(
                  summary.date.year,
                  summary.date.month,
                  summary.date.day,
                )),
                style: theme.textTheme.bodyMedium,
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  l10n.summaryValueAvg(
                      unitFormatter.heartRate(summary.avgBpm).text),
                  style: theme.textTheme.titleSmall
                      ?.copyWith(color: AppColors.heart),
                ),
                Text(
                  '${unitFormatter.heartRate(summary.minBpm).text}-${unitFormatter.heartRate(summary.maxBpm).text}',
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// Kotlin `RespiratoryRateDaySummary` (`HeartVitalsSummaries.kt`).
@immutable
class RespiratoryRateDaySummary {
  const RespiratoryRateDaySummary({
    required this.date,
    required this.average,
    required this.min,
    required this.max,
    required this.readings,
  });

  final LocalDate date;
  final double average;
  final double min;
  final double max;
  final int readings;
}

/// Kotlin `respiratoryRateDaySummaries`.
List<RespiratoryRateDaySummary> respiratoryRateDaySummaries(
  List<RespiratoryRateEntry> entries,
) {
  final byDate = <LocalDate, List<double>>{};
  for (final entry in entries) {
    byDate
        .putIfAbsent(instantToLocalDate(entry.time), () => <double>[])
        .add(entry.breathsPerMinute);
  }
  return [
    for (final MapEntry(key: date, value: values) in byDate.entries)
      RespiratoryRateDaySummary(
        date: date,
        average: values.reduce((a, b) => a + b) / values.length,
        min: values.reduce((a, b) => a < b ? a : b),
        max: values.reduce((a, b) => a > b ? a : b),
        readings: values.length,
      ),
  ];
}

/// Kotlin `RespiratoryRateDayRow`.
class RespiratoryRateDayRow extends StatelessWidget {
  const RespiratoryRateDayRow({
    super.key,
    required this.summary,
    required this.unitFormatter,
    required this.accentColor,
  });

  final RespiratoryRateDaySummary summary;
  final UnitFormatter unitFormatter;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Expanded(
              child: Text(
                DateFormat.MMMEd(locale).format(DateTime(
                  summary.date.year,
                  summary.date.month,
                  summary.date.day,
                )),
                style: theme.textTheme.bodyMedium,
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  l10n.summaryValueAvg(
                      unitFormatter.respiratoryRate(summary.average).text),
                  style:
                      theme.textTheme.titleSmall?.copyWith(color: accentColor),
                ),
                Text(
                  '${unitFormatter.respiratoryRate(summary.min).text}-${unitFormatter.respiratoryRate(summary.max).text}',
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
                Text(
                  l10n.summaryReadings(unitFormatter.count(summary.readings)),
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
