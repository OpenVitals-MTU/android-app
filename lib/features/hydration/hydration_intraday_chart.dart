import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../core/presentation/unit_formatter.dart';
import '../../core/time/local_date.dart';
import '../../domain/model/nutrition_models.dart';
import '../../l10n/app_localizations.dart';
import '../../ui/charts/metric_line_plot.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/theme/app_colors.dart';

/// The day's hydration, as it actually accumulated — port of the Kotlin
/// `HydrationIntradayChartCard`.
///
/// The Day view was drawing the WEEK chart with a single day in it: one fat bar
/// labelled "Sun 12", which says nothing a day view does not already say twice on
/// the cards above it. A day is not a bar. A day is a line that climbs from nothing
/// at midnight to whatever you have drunk by now, and its shape is the whole point:
/// it shows you that you drank everything at 9am and nothing since.
///
/// Cumulative, not per-entry: each point is the running TOTAL at the time of that
/// entry, so the line only ever goes up and the last point is the day's total. It is
/// anchored at (0, 0) so the climb starts at midnight, and closed at (1, total) so
/// the line runs to the edge of the chart rather than stopping at the last drink.
class HydrationIntradayChartCard extends StatelessWidget {
  const HydrationIntradayChartCard({
    super.key,
    required this.selectedDate,
    required this.entries,
    required this.dailyGoalLiters,
    required this.formatter,
  });

  final LocalDate selectedDate;
  final List<HydrationEntry> entries;
  final double dailyGoalLiters;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toString();

    final isToday = selectedDate == LocalDate.now();
    final dayStart = DateTime(
      selectedDate.year,
      selectedDate.month,
      selectedDate.day,
    );
    // Today's chart ends NOW, not at midnight: a line drawn across the whole 24
    // hours at 2pm would show a flat run into a future that has not happened.
    final chartEnd =
        isToday ? DateTime.now() : dayStart.add(const Duration(days: 1));
    final elapsedMs = chartEnd
        .difference(dayStart)
        .inMilliseconds
        .clamp(1, 1 << 62);

    final points = _cumulative(entries);
    final total = points.isEmpty ? 0.0 : points.last.$2;
    final maxValue = [total, dailyGoalLiters, 0.5].reduce((a, b) => a > b ? a : b);

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              formatter.hydration(total).text,
              style: theme.textTheme.headlineMedium
                  ?.copyWith(color: AppColors.hydration),
            ),
            Text(
              isToday
                  ? l10n.summaryToday(l10n.metricHydration)
                  : l10n.summaryOnDate(
                      l10n.metricHydration,
                      DateFormat.yMMMd(locale).format(dayStart),
                    ),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 16),
            if (points.isEmpty)
              Text(
                isToday
                    ? l10n.summaryEmptyToday(l10n.metricHydration)
                    : l10n.summaryEmptyDay(l10n.metricHydration),
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              )
            else ...[
              MetricLinePlot(
                points: [
                  // Anchored at midnight, so the climb starts where the day does.
                  const MetricLinePlotPoint(xFraction: 0, value: 0),
                  for (final point in points)
                    MetricLinePlotPoint(
                      xFraction: point.$1
                              .difference(dayStart)
                              .inMilliseconds
                              .clamp(0, elapsedMs) /
                          elapsedMs,
                      value: point.$2,
                    ),
                  // Closed at the right edge: the total holds until now.
                  MetricLinePlotPoint(xFraction: 1, value: total),
                ],
                minValue: 0,
                maxValue: maxValue,
                accentColor: AppColors.hydration,
                valueFormatter: (value) => formatter.hydration(value).text,
              ),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  for (final label in [
                    '00:00',
                    '06:00',
                    '12:00',
                    '18:00',
                    if (isToday) l10n.summaryNow else '24:00',
                  ])
                    Text(
                      label,
                      style: theme.textTheme.labelSmall
                          ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                    ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                l10n.summaryLastUpdate(
                  DateFormat.jm(locale).format(points.last.$1.toLocal()),
                ),
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ],
        ),
      ),
    );
  }

  /// `(time, running total)` for each entry, in order. Kotlin
  /// `cumulativeHydrationPoints()`.
  List<(DateTime, double)> _cumulative(List<HydrationEntry> entries) {
    final ordered = [...entries]
      ..sort((a, b) => a.startTime.compareTo(b.startTime));
    var running = 0.0;
    return [
      for (final entry in ordered)
        if (entry.liters > 0) (entry.startTime, running += entry.liters),
    ];
  }
}
