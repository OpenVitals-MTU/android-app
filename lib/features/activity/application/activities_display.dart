import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/time_range.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/insights/cardio_load.dart';
import '../../../domain/insights/cross_metric_insights.dart';
import '../../../domain/insights/daily_goals.dart';
import '../../../domain/insights/data_confidence.dart';
import '../../../domain/insights/metric_interpretations.dart';
import '../../../domain/insights/period_comparison.dart';
import '../../../domain/insights/personal_baseline.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/usecase/load_activities_use_case.dart';
import '../../../ui/charts/bar_chart.dart';
import '../presentation/exercise_labels.dart';

part 'activities_display.freezed.dart';

/// The workout daily-goal key (Kotlin `MetricDailyGoalKey.WORKOUT_MINUTES`).
const MetricDailyGoalKey activitiesGoalKey = MetricDailyGoalKey.workoutMinutes;

/// Health Connect `Metadata.RECORDING_METHOD_MANUAL_ENTRY`.
const int _recordingMethodManualEntry = 3;

/// The period totals behind the five key-metric cards (Kotlin
/// `ActivityOverviewTotals`).
@freezed
abstract class ActivityOverviewTotals with _$ActivityOverviewTotals {
  const factory ActivityOverviewTotals({
    required int steps,
    required double distanceMeters,
    required double energyBurnedKcal,
    required bool hasEnergyBurned,
    required int cardioLoad,
    required bool hasCardioLoad,
    required CardioLoadConfidence cardioLoadConfidence,
    required double? hrvRmssdMs,
  }) = _ActivityOverviewTotals;
}

/// One marker of the week strip: the day, and the workout that represents it
/// (null on a rest day, which draws the empty ring).
@freezed
abstract class ActivityStripMarker with _$ActivityStripMarker {
  const factory ActivityStripMarker({
    required LocalDate date,
    ExerciseData? workout,
  }) = _ActivityStripMarker;
}

/// The screen-ready derivation of one loaded activities period: the key-metric
/// totals and their bucketed sparkline series, the week strip, the workout-minute
/// bar series, the goal progress, the statistics, the HHS guideline, the
/// resting-HR cross insight and the data confidence.
///
/// Built once per load (and per filter/goal change) by [buildActivitiesDisplay]
/// and stored on the state — the view-model precomputes, the widgets only render.
@freezed
abstract class ActivitiesDisplay with _$ActivitiesDisplay {
  const factory ActivitiesDisplay({
    /// Whether the period has anything at all to show (Kotlin's empty gate).
    required bool hasAnyData,
    required bool hasOverviewDays,

    /// The activity-type dropdown's options, ordered by their label.
    required List<int> filterOptions,

    /// Null when the period has no overview day at all — which is what hides the
    /// key-metrics section.
    required ActivityOverviewTotals? totals,

    /// The representative date of each sparkline bucket, in order. The view turns
    /// these into (localized) labels; it does not bucket.
    required List<LocalDate> bucketDates,

    /// The week strip's markers. Empty for every range but WEEK.
    required List<ActivityStripMarker> stripMarkers,
    required List<double> cardioLoadSeries,
    required List<double> energyBurnedSeries,
    required List<double> stepsSeries,
    required List<double> distanceSeries,
    required List<double> hrvSeries,

    /// True when any day's energy is our own active+BMR estimate rather than a
    /// recorded total — the calories card says so out loud.
    required bool energyEstimated,

    /// The workout-minutes bar series and its summary total.
    required List<PeriodChartValue> chartValues,
    required int totalDurationMs,
    required DailyGoalProgress goalProgress,
    required int workoutCount,
    required int averageDurationMs,
    required int longestDurationMs,
    required PeriodComparison periodComparison,
    required PersonalBaselineInsight? baselineInsight,

    /// The HHS 150-minute guideline. Null when there is nothing to compare.
    required WorkoutGuidelineProgress? guideline,
    required bool guidelineUsesWeeklyAverage,
    required CrossMetricInsight? crossInsight,
    required DataConfidence dataConfidence,

    /// The period's workouts indexed by their local start date — the chart's
    /// selected-day list is a lookup, not a scan.
    required Map<LocalDate, List<ExerciseData>> workoutsByDay,

    /// The planned workouts, in the order the card lists them (earliest first).
    required List<PlannedExerciseData> sortedPlannedWorkouts,
  }) = _ActivitiesDisplay;
}

/// Pure derivation from one (already filtered) loaded period to its display
/// model. No clock, no `ref`, no I/O — unit-testable with a fixture result.
///
/// [result] is the slice the selected activity-type filter left behind;
/// [availableActivityTypes] is the unfiltered set of types, in label order.
ActivitiesDisplay buildActivitiesDisplay({
  required ActivitiesLoadResult result,
  required List<int> availableActivityTypes,
  required int? selectedActivityType,
  required TimeRange range,
  required DatePeriod period,
  required double dailyGoalMinutes,
}) {
  final workouts = result.workouts;
  final sortedDays = [...result.overviewDays]
    ..sort((a, b) => a.date.compareTo(b.date));
  final buckets = _buckets(sortedDays, range);

  final goalValues = workoutDailyGoalValues(workouts);
  final goalProgress = dailyGoalProgress(
    goalValues,
    period,
    dailyGoalMinutes,
    activitiesGoalKey.direction,
  );

  final totalMs =
      workouts.fold<int>(0, (sum, w) => sum + math.max(0, w.durationMs));
  final previousTotalMs = result.previousWorkouts
      .fold<int>(0, (sum, w) => sum + math.max(0, w.durationMs));
  final dailyMinutes = [for (final v in goalValues) v.value];
  final currentAverage = dailyMinutes.isEmpty
      ? 0.0
      : dailyMinutes.reduce((a, b) => a + b) / dailyMinutes.length;
  final baselineValues = [
    for (final v in workoutDailyGoalValues(result.baselineWorkouts))
      BaselineValue(date: v.date, value: v.value),
  ];

  final workoutsByDay = <LocalDate, List<ExerciseData>>{};
  for (final w in workouts) {
    workoutsByDay
        .putIfAbsent(instantToLocalDate(w.startTime), () => <ExerciseData>[])
        .add(w);
  }

  final useWeeklyAverage =
      range == TimeRange.month || range == TimeRange.year;
  final guidelineMinutes = useWeeklyAverage
      ? (totalMs / 60000.0) / _weekCount(period)
      : totalMs / 60000.0;

  return ActivitiesDisplay(
    hasAnyData: workouts.isNotEmpty ||
        result.plannedWorkouts.isNotEmpty ||
        result.overviewDays.any((d) =>
            d.steps > 0 || d.distanceMeters > 0 || d.energyBurnedKcal > 0),
    hasOverviewDays: sortedDays.isNotEmpty,
    filterOptions: <int>{
      ...availableActivityTypes,
      ?selectedActivityType,
    }.toList()
      ..sort((a, b) => exerciseTypeLabel(a).compareTo(exerciseTypeLabel(b))),
    totals: sortedDays.isEmpty ? null : _overviewTotals(sortedDays),
    bucketDates: [for (final bucket in buckets) bucket.date],
    stripMarkers: range == TimeRange.week
        ? [
            for (final bucket in buckets)
              ActivityStripMarker(
                date: bucket.date,
                workout: bucket.markerWorkout,
              ),
          ]
        : const <ActivityStripMarker>[],
    cardioLoadSeries: _series(
      buckets,
      (d) => d.cardioLoadConfidence == CardioLoadConfidence.noData
          ? null
          : d.cardioLoad.toDouble(),
    ),
    energyBurnedSeries: _series(
      buckets,
      (d) => d.energyBurnedSource == CaloriesBurnedSource.noData
          ? null
          : d.energyBurnedKcal,
    ),
    stepsSeries: _series(buckets, (d) => d.steps.toDouble()),
    distanceSeries: _series(buckets, (d) => d.distanceMeters),
    hrvSeries: _series(buckets, (d) => d.hrvRmssdMs, average: true),
    energyEstimated: sortedDays.any(
      (d) => d.energyBurnedSource == CaloriesBurnedSource.estimatedActiveAndBmr,
    ),
    // The bar series is the goal series: workout minutes per day, same map, same
    // insertion order (the screen used to build the identical fold twice).
    chartValues: [
      for (final value in goalValues) PeriodChartValue(value.date, value.value),
    ],
    totalDurationMs: totalMs,
    goalProgress: goalProgress,
    workoutCount: workouts.length,
    averageDurationMs: workouts.isEmpty ? 0 : totalMs ~/ workouts.length,
    longestDurationMs: workouts.isEmpty
        ? 0
        : workouts
            .map((w) => math.max(0, w.durationMs))
            .reduce((a, b) => a > b ? a : b),
    periodComparison: periodComparison(
      totalMs.toDouble(),
      previousTotalMs.toDouble(),
    ),
    baselineInsight: personalBaselineInsight(
      currentAverage,
      baselineValues,
      period.start.minusDays(1),
    ),
    guideline: workoutGuidelineProgress(guidelineMinutes),
    guidelineUsesWeeklyAverage: useWeeklyAverage,
    crossInsight: crossMetricInsight(
      [for (final v in goalValues) CrossMetricValue(date: v.date, value: v.value)],
      [
        for (final r in result.crossDailyRestingHR)
          CrossMetricValue(date: r.date, value: r.bpm.toDouble()),
      ],
    ),
    dataConfidence: dataConfidence(
      period,
      [for (final w in workouts) instantToLocalDate(w.startTime)],
      workouts.length,
      sources: [for (final w in workouts) w.source],
      manualEntryCount: workouts
          .where((w) => w.recordingMethod == _recordingMethodManualEntry)
          .length,
    ),
    workoutsByDay: workoutsByDay,
    sortedPlannedWorkouts: [...result.plannedWorkouts]
      ..sort((a, b) => a.startTime.compareTo(b.startTime)),
  );
}

/// The workout minutes per day (Kotlin `workoutDailyGoalValues`).
List<DailyGoalValue> workoutDailyGoalValues(List<ExerciseData> workouts) {
  final byDate = <LocalDate, double>{};
  for (final w in workouts) {
    final date = instantToLocalDate(w.startTime);
    byDate[date] =
        (byDate[date] ?? 0.0) + math.max(0, w.durationMs).toDouble() / 60000.0;
  }
  return [
    for (final entry in byDate.entries)
      DailyGoalValue(date: entry.key, value: entry.value),
  ];
}

double _weekCount(DatePeriod period) {
  final days = period.end.epochDay - period.start.epochDay + 1;
  return math.max(days / 7.0, 1.0 / 7.0);
}

// ── Overview buckets / series / totals ─────────────────────────────────

class _Bucket {
  const _Bucket(this.date, this.days);
  // The bucket's representative date — its first (earliest) day (Kotlin
  // `ActivityOverviewBucket.date`).
  final LocalDate date;
  final List<ActivityOverviewDay> days;

  /// The workout that gets to represent this bucket — the first one, as Kotlin's
  /// `activityOverviewMarkerWorkout` did. A day with two rides shows one icon;
  /// the strip answers "did you train?", not "how often".
  ExerciseData? get markerWorkout {
    for (final day in days) {
      if (day.workouts.isNotEmpty) return day.workouts.first;
    }
    return null;
  }
}

List<_Bucket> _buckets(List<ActivityOverviewDay> days, TimeRange range) {
  final sorted = [...days]..sort((a, b) => a.date.compareTo(b.date));
  final maxBuckets = range == TimeRange.year ? 12 : 7;
  final List<_Bucket> raw;
  if (range == TimeRange.year) {
    final byMonth = <String, List<ActivityOverviewDay>>{};
    for (final day in sorted) {
      final key = '${day.date.year}-${day.date.month}';
      byMonth.putIfAbsent(key, () => <ActivityOverviewDay>[]).add(day);
    }
    raw = [
      for (final group in byMonth.values) _Bucket(group.first.date, group),
    ];
  } else {
    raw = [
      for (final day in sorted) _Bucket(day.date, [day]),
    ];
  }
  if (raw.isEmpty || maxBuckets <= 0) return const <_Bucket>[];
  if (raw.length <= maxBuckets) return raw;
  final chunkSize = math.max(1, (raw.length / maxBuckets).ceil());
  final chunked = <_Bucket>[];
  for (var i = 0; i < raw.length; i += chunkSize) {
    final slice = raw.sublist(i, math.min(i + chunkSize, raw.length));
    chunked.add(_Bucket(
      slice.first.date,
      [for (final b in slice) ...b.days],
    ));
  }
  return chunked;
}

List<double> _series(
  List<_Bucket> buckets,
  double? Function(ActivityOverviewDay) selector, {
  bool average = false,
}) =>
    [
      for (final bucket in buckets)
        () {
          final values = [
            for (final day in bucket.days)
              if (selector(day) != null) selector(day)!,
          ];
          if (values.isEmpty) return 0.0;
          final sum = values.reduce((a, b) => a + b);
          return average ? sum / values.length : sum;
        }(),
    ];

ActivityOverviewTotals _overviewTotals(List<ActivityOverviewDay> days) {
  final hrvValues = [for (final d in days) if (d.hrvRmssdMs != null) d.hrvRmssdMs!];
  final cardioDays = [
    for (final d in days)
      if (d.cardioLoadConfidence != CardioLoadConfidence.noData) d,
  ];
  return ActivityOverviewTotals(
    steps: days.fold<int>(0, (sum, d) => sum + d.steps),
    distanceMeters: days.fold<double>(0, (sum, d) => sum + d.distanceMeters),
    energyBurnedKcal: days.fold<double>(0, (sum, d) => sum + d.energyBurnedKcal),
    hasEnergyBurned:
        days.any((d) => d.energyBurnedSource != CaloriesBurnedSource.noData),
    cardioLoad: cardioDays.fold<int>(0, (sum, d) => sum + d.cardioLoad),
    hasCardioLoad: cardioDays.isNotEmpty,
    cardioLoadConfidence: _aggregateCardioConfidence(cardioDays),
    hrvRmssdMs: hrvValues.isEmpty
        ? null
        : hrvValues.reduce((a, b) => a + b) / hrvValues.length,
  );
}

CardioLoadConfidence _aggregateCardioConfidence(List<ActivityOverviewDay> days) {
  if (days.isEmpty) return CardioLoadConfidence.noData;
  if (days.any((d) => d.cardioLoadConfidence == CardioLoadConfidence.low)) {
    return CardioLoadConfidence.low;
  }
  if (days.any((d) => d.cardioLoadConfidence == CardioLoadConfidence.medium)) {
    return CardioLoadConfidence.medium;
  }
  return CardioLoadConfidence.high;
}
