import 'dart:math' as math;

import 'package:flutter/foundation.dart';

import '../../../core/period/time_range.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/insights/daily_goals.dart';
import '../../../domain/insights/period_comparison.dart';
import '../../../domain/insights/personal_baseline.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/query/activity_period_data.dart';
import 'activity_metric.dart';

/// Port of the Kotlin `ActivityDisplayState.kt` + the per-metric `*Display`
/// functions in `ActivityPresentationMapper.kt`.
///
/// Pure: everything the Steps (and sibling) detail screens render is derived
/// here from a loaded [ActivityPeriodData], so the sections stay dumb.

/// Kotlin `ActivityIntradayPoint`.
@immutable
class ActivityIntradayPoint {
  const ActivityIntradayPoint({required this.time, required this.value});

  final DateTime time;
  final double value;
}

/// Kotlin `ActivityMetricDisplay`.
@immutable
class ActivityMetricDisplay {
  const ActivityMetricDisplay({
    this.hasData = false,
    this.values = const [],
    this.goalValues = const [],
    this.trackedDates = const [],
    this.sampleCount = 0,
    this.previousTotal = 0.0,
    this.baselineValues = const [],
    this.activeDays = 0,
    this.goalProgress,
    this.periodComparison,
    this.baselineCurrentValue = 0.0,
    this.intradayPoints = const [],
    this.dayTotal = 0.0,
  });

  final bool hasData;
  final List<double> values;
  final List<DailyGoalValue> goalValues;
  final List<LocalDate> trackedDates;
  final int sampleCount;
  final double previousTotal;
  final List<BaselineValue> baselineValues;
  final int activeDays;
  final DailyGoalProgress? goalProgress;
  final PeriodComparison? periodComparison;
  final double baselineCurrentValue;
  final List<ActivityIntradayPoint> intradayPoints;
  final double dayTotal;

  double get total => values.fold(0.0, (sum, value) => sum + value);

  double get best => values.isEmpty ? 0.0 : values.reduce(math.max);
}

/// Kotlin `averageOrZero`.
double averageOrZero(double total, int activeDays) =>
    activeDays > 0 ? total / activeDays : 0.0;

/// Kotlin `ActivityMetric.dailyGoalKey`.
MetricDailyGoalKey activityMetricGoalKey(ActivityMetric metric) =>
    switch (metric) {
      ActivityMetric.steps => MetricDailyGoalKey.steps,
      ActivityMetric.distance => MetricDailyGoalKey.distanceMeters,
      ActivityMetric.caloriesOut => MetricDailyGoalKey.caloriesOutKcal,
      ActivityMetric.activeCalories => MetricDailyGoalKey.activeCaloriesKcal,
      ActivityMetric.floors => MetricDailyGoalKey.floors,
      ActivityMetric.elevation => MetricDailyGoalKey.elevationMeters,
      ActivityMetric.wheelchair => MetricDailyGoalKey.wheelchairPushes,
    };

/// The daily value of [metric] on a [DailySteps] row.
double _dailyValue(ActivityMetric metric, DailySteps entry) => switch (metric) {
      ActivityMetric.steps => entry.steps.toDouble(),
      ActivityMetric.distance => entry.distanceMeters,
      ActivityMetric.activeCalories => entry.activeCaloriesKcal ?? 0.0,
      ActivityMetric.floors => (entry.floorsClimbed ?? 0).toDouble(),
      ActivityMetric.elevation => entry.elevationGainedMeters ?? 0.0,
      ActivityMetric.wheelchair => (entry.wheelchairPushes ?? 0).toDouble(),
      // Calories burned comes from the nutrition slice, never from daily steps.
      ActivityMetric.caloriesOut => 0.0,
    };

/// The cumulative intraday value of [metric] on a progress point, or null when
/// that metric was not sampled — Kotlin's `mapNotNull` drops those points.
double? _intradayValue(ActivityMetric metric, ActivityProgressPoint point) =>
    switch (metric) {
      ActivityMetric.steps => point.totalSteps.toDouble(),
      ActivityMetric.distance => point.totalDistanceMeters,
      ActivityMetric.caloriesOut => point.totalCaloriesBurnedKcal,
      ActivityMetric.activeCalories => point.totalActiveCaloriesKcal,
      ActivityMetric.floors => point.totalFloorsClimbed?.toDouble(),
      ActivityMetric.elevation => point.totalElevationGainedMeters,
      ActivityMetric.wheelchair => point.totalWheelchairPushes?.toDouble(),
    };

/// Whether [metric] recorded anything on this daily row. Distinguishes "the
/// sensor reported zero" from "the metric was never sampled", which is why the
/// nullable columns test for null rather than for `> 0`.
bool _hasDailyValue(ActivityMetric metric, DailySteps entry) =>
    switch (metric) {
      ActivityMetric.steps || ActivityMetric.distance => true,
      ActivityMetric.activeCalories => entry.activeCaloriesKcal != null,
      ActivityMetric.floors => entry.floorsClimbed != null,
      ActivityMetric.elevation => entry.elevationGainedMeters != null,
      ActivityMetric.wheelchair => entry.wheelchairPushes != null,
      ActivityMetric.caloriesOut => true,
    };

/// Kotlin's per-metric `*Display` + shared `metricDisplay`.
ActivityMetricDisplay activityMetricDisplay({
  required ActivityMetric metric,
  required ActivityPeriodData data,
  required TimeRange range,
  required DatePeriod period,
  required double dailyGoal,
}) {
  final bool isCalories = metric == ActivityMetric.caloriesOut;

  final List<LocalDate> dates;
  final List<double> values;
  final double previousTotal;
  final List<BaselineValue> baselineValues;
  final double dayTotal;
  final bool hasDailyData;

  if (isCalories) {
    dates = [for (final entry in data.nutrition) entry.date];
    values = [for (final entry in data.nutrition) entry.caloriesBurnedKcal];
    previousTotal = data.previousNutrition
        .fold(0.0, (sum, entry) => sum + entry.caloriesBurnedKcal);
    baselineValues = [
      for (final entry in data.baselineNutrition)
        BaselineValue(date: entry.date, value: entry.caloriesBurnedKcal),
    ];
    dayTotal = data.nutrition.isEmpty ? 0.0 : data.nutrition.first.caloriesBurnedKcal;
    hasDailyData = data.nutrition.isNotEmpty;
  } else {
    dates = [for (final entry in data.dailySteps) entry.date];
    values = [for (final entry in data.dailySteps) _dailyValue(metric, entry)];
    previousTotal = data.previousDailySteps
        .fold(0.0, (sum, entry) => sum + _dailyValue(metric, entry));
    baselineValues = [
      for (final entry in data.baselineDailySteps)
        BaselineValue(date: entry.date, value: _dailyValue(metric, entry)),
    ];
    dayTotal = data.dailySteps.isEmpty
        ? 0.0
        : _dailyValue(metric, data.dailySteps.first);
    hasDailyData = metric == ActivityMetric.steps ||
            metric == ActivityMetric.distance
        ? data.dailySteps.isNotEmpty
        : data.dailySteps.any((entry) => _hasDailyValue(metric, entry));
  }

  final goalValues = [
    for (var i = 0; i < values.length; i++)
      DailyGoalValue(date: dates[i], value: values[i]),
  ];
  final trackedDates = [
    for (var i = 0; i < values.length; i++)
      if (values[i] > 0.0) dates[i],
  ];
  final activeDays = values.where((value) => value > 0.0).length;

  final intradayPoints = <ActivityIntradayPoint>[
    for (final point in data.activityProgress)
      if (_intradayValue(metric, point) case final value?)
        ActivityIntradayPoint(time: point.time, value: value),
  ];

  // A single day is described by its intraday samples, longer periods by their
  // active days.
  final sampleCount = range == TimeRange.day
      ? intradayPoints.where((point) => point.value > 0.0).length
      : activeDays;

  final total = values.fold(0.0, (sum, value) => sum + value);
  final goalKey = activityMetricGoalKey(metric);
  final goalProgress = dailyGoalProgress(
    goalValues,
    period,
    dailyGoal,
    goalKey.direction,
  );

  return ActivityMetricDisplay(
    // On a day view the screen has something to draw even with no daily row.
    hasData: range == TimeRange.day || hasDailyData,
    values: values,
    goalValues: goalValues,
    trackedDates: trackedDates,
    sampleCount: sampleCount,
    previousTotal: previousTotal,
    baselineValues: baselineValues,
    activeDays: activeDays,
    goalProgress: goalProgress,
    periodComparison: periodComparison(total, previousTotal),
    baselineCurrentValue: averageOrZero(total, activeDays),
    intradayPoints: intradayPoints,
    dayTotal: dayTotal,
  );
}
