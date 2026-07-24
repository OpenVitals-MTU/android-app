import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/time/local_date.dart';
import '../../../core/period/time_range.dart';
import '../../../domain/hydration/hydration_entry_merge.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../ui/charts/bar_chart.dart';
import '../../../ui/charts/metric_day_chart.dart';

part 'hydration_display.freezed.dart';

/// The period hydration summary, a trimmed port of the Kotlin
/// `HydrationPeriodSummary` computed by `HydrationPresentationMapper`.
@freezed
abstract class HydrationSummary with _$HydrationSummary {
  const factory HydrationSummary({
    @Default(0.0) double totalLiters,
    @Default(0) int trackedDays,
    @Default(0) int loggedDays,

    /// Days of the period that have actually happened — the whole period, or
    /// the part of it up to today. The denominator a person means when they ask
    /// "how did I do this week".
    @Default(0) int elapsedDays,
    @Default(0.0) double averageLiters,
    @Default(0.0) double bestDayLiters,
    @Default(0) int goalMetDays,
    @Default(0) int goalSuccessRatePercent,
    @Default(0) int currentGoalStreakDays,
    @Default(0) int longestGoalStreakDays,
  }) = _HydrationSummary;
}

/// A single drink-type breakdown slice (drink name + summed litres over the
/// period).
///
/// [label] is null when the drink has no name at all — a bare `HydrationRecord`
/// from another app, which Health Connect gives us as a volume and a package
/// name and nothing else. The screen names those slices; a package name
/// ("tech.mmarca.openvitals") is never a drink name.
@freezed
abstract class HydrationDrinkSlice with _$HydrationDrinkSlice {
  const factory HydrationDrinkSlice({
    required String? label,
    required double liters,
  }) = _HydrationDrinkSlice;
}

/// The screen-ready derivation of one loaded hydration period: the summary, the
/// daily bar series, the cumulative day curve, the drink breakdown (with the
/// bars' shared scale) and the beverage history in display order.
///
/// Built once per load by [buildHydrationDisplay] and stored on the state — the
/// view-model precomputes, the screen only renders (the Kotlin
/// `HydrationPresentationMapper` discipline, restored).
@freezed
abstract class HydrationDisplay with _$HydrationDisplay {
  const factory HydrationDisplay({
    @Default(false) bool hasData,
    @Default(HydrationSummary()) HydrationSummary summary,
    @Default(<PeriodChartValue>[]) List<PeriodChartValue> chartValues,
    @Default(<DaySample>[]) List<DaySample> cumulativeSamples,
    @Default(<HydrationDrinkSlice>[]) List<HydrationDrinkSlice> drinkBreakdown,

    /// The six biggest slices — all the breakdown card has room for.
    @Default(<HydrationDrinkSlice>[]) List<HydrationDrinkSlice> topDrinkSlices,

    /// The scale [topDrinkSlices] are drawn against, floored at 1.0.
    @Default(1.0) double maxDrinkLiters,

    /// The daily average as a fraction of the goal, clamped to 0..1.
    @Default(0.0) double goalProgress,
    @Default(<HydrationEntry>[]) List<HydrationEntry> entriesNewestFirst,
  }) = _HydrationDisplay;
}

/// Pure derivation from the loaded period to its display model. No clock, no
/// I/O — unit-testable with fixture lists.
HydrationDisplay buildHydrationDisplay(
  List<DailyHydration> dailyHydration,
  List<HydrationEntry> entries, {
  required double dailyGoalLiters,
  required DatePeriod period,
  required LocalDate today,
}) {
  final summary = _summarize(
    today: today,
    dailyHydration,
    dailyGoalLiters,
    elapsedDays: _elapsedDays(period, today),
  );
  final breakdown = _drinkBreakdown(entries);
  final topSlices = breakdown.take(6).toList();
  var maxLiters = 0.0;
  for (final slice in topSlices) {
    if (slice.liters > maxLiters) maxLiters = slice.liters;
  }
  if (maxLiters <= 0.0) maxLiters = 1.0;

  return HydrationDisplay(
    hasData: dailyHydration.any((day) => day.liters > 0.0),
    summary: summary,
    chartValues: [
      for (final day in dailyHydration) PeriodChartValue(day.date, day.liters),
    ],
    cumulativeSamples: cumulativeHydration(entries),
    drinkBreakdown: breakdown,
    topDrinkSlices: topSlices,
    maxDrinkLiters: maxLiters,
    // How much of the period you actually met the goal on — NOT the average of
    // the days you happened to log. Dividing by tracked days meant that logging
    // one day and hitting the goal filled the bar completely, so the bar
    // rewarded you for logging less. See docs/engineering/refactor-tracker.md.
    goalProgress: summary.elapsedDays > 0
        ? (summary.goalMetDays / summary.elapsedDays).clamp(0.0, 1.0)
        : 0.0,
    entriesNewestFirst: [...entries]
      ..sort((a, b) => b.startTime.compareTo(a.startTime)),
  );
}

/// Port of the Kotlin `List<DailyHydration>.summaryForGoal`.
/// Days of [period] that have happened. A goal you have not had the chance to
/// miss yet must not count against you, so a period running past today is cut
/// at today.
int _elapsedDays(DatePeriod period, LocalDate today) {
  final end = period.end.isAfter(today) ? today : period.end;
  if (end.isBefore(period.start)) return 0;
  return (end.epochDay - period.start.epochDay) + 1;
}

HydrationSummary _summarize(
  List<DailyHydration> days,
  double goalLiters, {
  required int elapsedDays,
  required LocalDate today,
}) {
  final sorted = [...days]..sort((a, b) => a.date.compareTo(b.date));
  final totalLiters = sorted.fold<double>(0.0, (sum, day) => sum + day.liters);
  final trackedDays = sorted.where((day) => day.liters > 0.0).length;
  bool meetsGoal(DailyHydration day) => goalLiters > 0.0 && day.liters >= goalLiters;
  final goalMetDays = sorted.where(meetsGoal).length;

  var currentGoalStreak = 0;
  var longestGoalStreak = 0;
  for (final day in sorted) {
    if (meetsGoal(day)) {
      currentGoalStreak += 1;
      if (currentGoalStreak > longestGoalStreak) {
        longestGoalStreak = currentGoalStreak;
      }
    } else {
      currentGoalStreak = 0;
    }
  }

  final reversed = sorted.reversed.toList();
  var trailingGoalStreak = 0;
  for (final day in reversed) {
    if (!meetsGoal(day)) {
      // A day that has not finished yet cannot break the streak — the same
      // protection _elapsedDays gives goalProgress. Without it the current
      // streak collapsed to 0 at midnight until today's goal was met.
      if (day.date.isBefore(today)) break;
      continue;
    }
    trailingGoalStreak += 1;
  }

  final bestDay = sorted.isEmpty
      ? 0.0
      : sorted.map((day) => day.liters).reduce((a, b) => a > b ? a : b);

  return HydrationSummary(
    totalLiters: totalLiters,
    trackedDays: trackedDays,
    loggedDays: sorted.length,
    elapsedDays: elapsedDays,
    averageLiters: trackedDays > 0 ? totalLiters / trackedDays : 0.0,
    bestDayLiters: bestDay,
    goalMetDays: goalMetDays,
    goalSuccessRatePercent:
        trackedDays > 0 ? (goalMetDays * 100 ~/ trackedDays) : 0,
    currentGoalStreakDays: trailingGoalStreak,
    longestGoalStreakDays: longestGoalStreak,
  );
}

/// Groups hydration entries by drink name, summing litres. Only
/// hydration-bearing entries are counted (nutrition-only entries carry no
/// volume).
///
/// The name comes from the paired nutrition record (see
/// [mergeHydrationAndNutrition]); drinks with no name group together under a
/// single null-labelled slice, which the screen titles. It must never fall back
/// to [HydrationEntry.source] — that is the originating *package*, so an entry
/// this very app wrote rendered as "tech.mmarca.openvitals".
List<HydrationDrinkSlice> _drinkBreakdown(List<HydrationEntry> entries) {
  final byLabel = <String?, double>{};
  for (final entry in entries) {
    if (entry.liters <= 0.0) continue;
    final displayName = entry.displayName?.trim();
    final label =
        (displayName != null && displayName.isNotEmpty) ? displayName : null;
    byLabel[label] = (byLabel[label] ?? 0.0) + entry.liters;
  }
  final slices = byLabel.entries
      .map((e) => HydrationDrinkSlice(label: e.key, liters: e.value))
      .toList()
    ..sort((a, b) => b.liters.compareTo(a.liters));
  return slices;
}

/// `(time, running total)` for each entry, in order. Kotlin
/// `cumulativeHydrationPoints()`.
List<DaySample> cumulativeHydration(List<HydrationEntry> entries) {
  final ordered = [...entries]
    ..sort((a, b) => a.startTime.compareTo(b.startTime));
  var running = 0.0;
  return [
    for (final entry in ordered)
      if (entry.liters > 0)
        (time: entry.startTime, value: running += entry.liters),
  ];
}
