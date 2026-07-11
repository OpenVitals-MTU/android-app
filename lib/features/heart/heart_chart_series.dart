import 'package:flutter/material.dart';

import '../../core/period/time_range.dart';
import '../../core/stats/stats.dart';
import '../../core/time/local_date.dart';
import '../../domain/model/heart_models.dart';
import '../../domain/model/vitals_models.dart';
import '../../l10n/app_localizations.dart';
import '../../ui/charts/line_chart.dart';
import '../../ui/theme/app_colors.dart';

/// The line series behind the heart and vitals charts.
///
/// Port of Kotlin `HeartVitalsChartData.kt`. Both the heart **metric** screen and
/// the heart-vitals **overview** draw the same charts from the same records, and
/// each had written out its own copy of these four builders — ~170 lines that
/// differed only in how the [TimeRange] reached them (a parameter in one, a field
/// read in the other). They now take it as a parameter, which is what let the two
/// copies become one.
///
/// The shape shared by all of them: **within a single day, plot the raw readings;
/// over any longer range, plot one point per day.** A month of raw blood-pressure
/// dots is noise, not a trend.

/// The average line, plus min/max lines when any day actually has a spread.
///
/// A day whose min equals its max has no range to draw, and drawing three
/// coincident lines just thickens the average.
List<MetricLineSeries> heartRateSeries(
  List<HeartRateSummary> summaries,
  AppLocalizations l10n,
) {
  final hasRange = summaries.any((s) => s.minBpm != s.maxBpm);
  return [
    MetricLineSeries(
      points: [
        for (final s in summaries)
          MetricLinePoint(date: s.date, value: s.avgBpm.toDouble()),
      ],
      color: AppColors.heart,
      label: l10n.summaryAverage,
    ),
    if (hasRange) ...[
      MetricLineSeries(
        points: [
          for (final s in summaries)
            MetricLinePoint(date: s.date, value: s.minBpm.toDouble()),
        ],
        color: AppColors.heart.withValues(alpha: 0.55),
        label: l10n.statLowest,
      ),
      MetricLineSeries(
        points: [
          for (final s in summaries)
            MetricLinePoint(date: s.date, value: s.maxBpm.toDouble()),
        ],
        color: AppColors.heart.withValues(alpha: 0.9),
        label: l10n.statHighest,
      ),
    ],
  ];
}

/// Systolic and diastolic as two lines — raw within a day, daily averages beyond.
List<MetricLineSeries> bloodPressureSeries(
  List<BloodPressureEntry> sorted,
  AppLocalizations l10n,
  TimeRange range,
) {
  final isDay = range == TimeRange.day;
  final systolic = [
    for (final e in sorted)
      MetricLinePoint(
        date: instantToLocalDate(e.time),
        value: e.systolicMmHg.toDouble(),
        time: e.time,
      ),
  ];
  final diastolic = [
    for (final e in sorted)
      MetricLinePoint(
        date: instantToLocalDate(e.time),
        value: e.diastolicMmHg.toDouble(),
        time: e.time,
      ),
  ];
  return [
    MetricLineSeries(
      points: isDay ? systolic : dailyAverageLinePoints(systolic),
      color: AppColors.vitals,
      label: l10n.vitalsEntrySystolicLabel,
    ),
    MetricLineSeries(
      points: isDay ? diastolic : dailyAverageLinePoints(diastolic),
      color: AppColors.heart,
      label: l10n.vitalsEntryDiastolicLabel,
    ),
  ];
}

/// Raw readings within a day; otherwise a daily average line plus min/max lines.
///
/// [color] and [dayLabel] are the two things the two callers genuinely disagree
/// about — the metric screen tints by its own accent, the overview by the
/// respiratory blue — so they are parameters rather than a difference smuggled
/// into two copies of the body.
List<MetricLineSeries> respiratoryRateSeries(
  List<RespiratoryRateEntry> entries,
  AppLocalizations l10n,
  TimeRange range, {
  required Color color,
  required String dayLabel,
}) {
  final sorted = [...entries]..sort((a, b) => a.time.compareTo(b.time));
  if (range == TimeRange.day) {
    return singleSeries(
      [for (final e in sorted) (e.time, e.breathsPerMinute)],
      color,
      range,
      label: dayLabel,
    );
  }

  final byDate = <LocalDate, List<double>>{};
  for (final e in sorted) {
    byDate
        .putIfAbsent(instantToLocalDate(e.time), () => <double>[])
        .add(e.breathsPerMinute);
  }
  final dates = byDate.keys.toList()..sort((a, b) => a.compareTo(b));
  final average = <MetricLinePoint>[];
  final min = <MetricLinePoint>[];
  final max = <MetricLinePoint>[];
  for (final date in dates) {
    // Non-empty by construction: a date is only a key because a reading put it
    // there.
    final values = byDate[date]!;
    average.add(MetricLinePoint(date: date, value: averageOrZero(values)));
    min.add(MetricLinePoint(date: date, value: minOf(values)!));
    max.add(MetricLinePoint(date: date, value: maxOf(values)!));
  }
  final hasRange = [
    for (var i = 0; i < min.length; i++) min[i].value != max[i].value,
  ].any((different) => different);
  return [
    MetricLineSeries(
      points: average,
      color: color,
      label: l10n.summaryAverage,
    ),
    if (hasRange) ...[
      MetricLineSeries(
        points: min,
        color: color.withValues(alpha: 0.55),
        label: l10n.statLowest,
      ),
      MetricLineSeries(
        points: max,
        color: AppColors.vitals.withValues(alpha: 0.75),
        label: l10n.statHighest,
      ),
    ],
  ];
}

/// One line: raw points within a day, daily averages over any longer range.
List<MetricLineSeries> singleSeries(
  List<(DateTime, double)> raw,
  Color color,
  TimeRange range, {
  String? label,
}) {
  final base = [
    for (final (time, value) in raw)
      MetricLinePoint(
        date: instantToLocalDate(time),
        value: value,
        time: time,
      ),
  ];
  final points = range == TimeRange.day ? base : dailyAverageLinePoints(base);
  return [MetricLineSeries(points: points, color: color, label: label)];
}
