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

    // The x axis is the WHOLE DAY, always — 00:00 to 24:00 — and a drink is placed
    // at its real hour.
    //
    // Kotlin scaled x by the time ELAPSED so far, so on a chart opened at 12:49 a
    // drink at 09:29 landed at 74% of the width... under an axis whose labels read
    // 00:00 / 06:00 / 12:00 / 18:00. The chart said quarter-past-five. That is not a
    // rendering nit: the only thing this chart exists to tell you is WHEN, and it
    // was telling you the wrong hour. Porting it faithfully reproduced the lie.
    //
    // So the day is the day. Today's line simply stops at `now` instead of running
    // to the right edge, which is honest — the rest of the day has not happened.
    const dayMs = Duration.millisecondsPerDay;
    double fractionOf(DateTime time) =>
        (time.difference(dayStart).inMilliseconds.clamp(0, dayMs)) / dayMs;
    final endFraction = isToday ? fractionOf(DateTime.now()) : 1.0;

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
                // A STEP, not a ramp. You do not sip continuously from midnight
                // onwards: you drink nothing, then a glass, then nothing. A plain
                // line from (0,0) to the first drink draws a diagonal that says you
                // were drinking all morning, and the flat stretches between glasses
                // — the part that tells you you have had nothing since nine — vanish
                // into the slope.
                //
                // So each drink contributes TWO points at the same instant: the
                // total before it, and the total after.
                points: [
                  const MetricLinePlotPoint(xFraction: 0, value: 0),
                  for (final (index, point) in points.indexed) ...[
                    MetricLinePlotPoint(
                      xFraction: fractionOf(point.$1),
                      value: index == 0 ? 0.0 : points[index - 1].$2,
                    ),
                    MetricLinePlotPoint(
                      xFraction: fractionOf(point.$1),
                      value: point.$2,
                    ),
                  ],
                  // Holds the total to now (today) or to midnight (a past day).
                  MetricLinePlotPoint(xFraction: endFraction, value: total),
                ],
                minValue: 0,
                maxValue: maxValue,
                accentColor: AppColors.hydration,
                valueFormatter: (value) => formatter.hydration(value).text,
              ),
              const SizedBox(height: 8),
              // Evenly spaced, and now TRUE: the axis spans the whole day, so the
              // 12:00 tick really is halfway across it.
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  for (final label in ['00:00', '06:00', '12:00', '18:00', '24:00'])
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
