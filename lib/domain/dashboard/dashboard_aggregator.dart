import 'dart:math' as math;

import '../../core/period/period_calculations.dart';
import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';
import '../insights/cardio_load.dart';
import '../insights/intensity_minutes.dart';
import '../model/activity_models.dart';
import '../model/dashboard_data.dart';
import '../model/dashboard_query.dart';
import '../model/nutrition_models.dart';
import '../preferences/activity_week_mode.dart';

/// Pure dashboard combine helpers. Health Connect I/O stays in the data layer's
/// `DashboardDataLoader`.
class DashboardAggregator {
  const DashboardAggregator._();

  static DatePeriod cardioLoadPeriod(
    LocalDate date,
    ActivityWeekMode activityWeekMode,
  ) =>
      periodFor(
        TimeRange.week,
        date,
        today: date,
        weekPeriodMode: activityWeekMode.toWeekPeriodMode(),
      );

  static WeeklyCardioTarget? weeklyCardioTarget({
    required int currentScore,
    required int daysElapsed,
    required List<int> previousWeekScores,
  }) {
    final previousBaseline =
        medianDoubleOrNull(previousWeekScores.where((it) => it > 0).toList());
    if (previousBaseline != null) {
      return WeeklyCardioTarget(
        score: roundCardioTarget(previousBaseline),
        source: DashboardWeeklyCardioLoadTargetSource.recentHistory,
      );
    }

    if (currentScore <= 0 || daysElapsed <= 0) return null;
    return WeeklyCardioTarget(
      score: roundCardioTarget(currentScore * 7.0 / daysElapsed),
      source: DashboardWeeklyCardioLoadTargetSource.currentPace,
    );
  }

  static CardioLoadConfidence weeklyCardioConfidence(
    List<CardioLoadEstimate> estimates,
  ) {
    final tracked = estimates
        .where((it) =>
            it.score > 0 && it.confidence != CardioLoadConfidence.noData)
        .toList();
    if (tracked.isEmpty) return CardioLoadConfidence.noData;
    if (tracked.any((it) => it.confidence == CardioLoadConfidence.high)) {
      return CardioLoadConfidence.high;
    }
    if (tracked.any((it) => it.confidence == CardioLoadConfidence.medium)) {
      return CardioLoadConfidence.medium;
    }
    return CardioLoadConfidence.low;
  }

  static IntensityMinutesConfidence weeklyIntensityConfidence(
    List<IntensityMinutesEstimate> estimates,
  ) {
    final tracked = estimates
        .where((it) =>
            it.moderateEquivalentMinutes > 0 &&
            it.confidence != IntensityMinutesConfidence.noData)
        .toList();
    if (tracked.isEmpty) return IntensityMinutesConfidence.noData;
    if (tracked.any((it) => it.confidence == IntensityMinutesConfidence.high)) {
      return IntensityMinutesConfidence.high;
    }
    if (tracked
        .any((it) => it.confidence == IntensityMinutesConfidence.medium)) {
      return IntensityMinutesConfidence.medium;
    }
    return IntensityMinutesConfidence.low;
  }

  static List<CardioLoadTimeWindow> cardioLoadWindows(
    List<ExerciseData> workouts,
    LocalDate date,
  ) {
    final dayStart = _startOfDay(date);
    final dayEnd = _startOfDay(date.plusDays(1));
    final windows = <CardioLoadTimeWindow>[];
    for (final workout in workouts) {
      if (!workout.endTime.isAfter(dayStart) ||
          !workout.startTime.isBefore(dayEnd)) {
        continue;
      }
      final window = CardioLoadTimeWindow(
        start: _maxInstant(workout.startTime, dayStart),
        end: _minInstant(workout.endTime, dayEnd),
      );
      if (window.durationMinutes > 0.0) windows.add(window);
    }
    return windows;
  }

  static List<IntensityWorkoutInput> intensityWorkoutInputs(
    List<ExerciseData> workouts,
    LocalDate date,
  ) {
    final dayStart = _startOfDay(date);
    final dayEnd = _startOfDay(date.plusDays(1));
    final inputs = <IntensityWorkoutInput>[];
    for (final workout in workouts) {
      if (!workout.endTime.isAfter(dayStart) ||
          !workout.startTime.isBefore(dayEnd)) {
        continue;
      }
      final overlapStart = _maxInstant(workout.startTime, dayStart);
      final overlapEnd = _minInstant(workout.endTime, dayEnd);
      if (!overlapEnd.isAfter(overlapStart)) continue;
      final overlapMinutes =
          overlapEnd.difference(overlapStart).inSeconds.toDouble() / 60.0;
      if (overlapMinutes <= 0.0) continue;
      final totalMinutes =
          math.max(0, workout.durationMs).toDouble() / 60000.0;
      final calories = workout.activeCaloriesKcal;
      final activeCalories = (calories != null && totalMinutes > 0.0)
          ? calories * (overlapMinutes / totalMinutes)
          : null;
      inputs.add(
        IntensityWorkoutInput(
          durationMinutes: overlapMinutes,
          activeCaloriesKcal: activeCalories,
        ),
      );
    }
    return inputs;
  }

  static Iterable<LocalDate> datesInRange(LocalDate start, LocalDate end) sync* {
    if (start.isAfter(end)) return;
    var date = start;
    while (!date.isAfter(end)) {
      yield date;
      date = date.plusDays(1);
    }
  }

  static int? medianLongOrNull(List<int> values) {
    if (values.isEmpty) return null;
    final sorted = [...values]..sort();
    return sorted[(sorted.length - 1) ~/ 2];
  }

  static double? medianDoubleOrNull(List<int> values) {
    if (values.isEmpty) return null;
    final sorted = [...values]..sort();
    final middle = sorted.length ~/ 2;
    if (sorted.length.isEven) {
      return (sorted[middle - 1] + sorted[middle]) / 2.0;
    }
    return sorted[middle].toDouble();
  }

  static double? medianDoubleValuesOrNull(List<double> values) {
    if (values.isEmpty) return null;
    final sorted = [...values]..sort();
    final middle = sorted.length ~/ 2;
    if (sorted.length.isEven) {
      return (sorted[middle - 1] + sorted[middle]) / 2.0;
    }
    return sorted[middle];
  }

  static DashboardData mergeDerivedDashboardProjection(
    DashboardData base,
    DashboardData projection,
  ) {
    final estimatedCaloriesLoaded =
        projection.loadedMetrics.contains(DashboardMetric.caloriesOut) &&
            projection.caloriesKcalSource ==
                CaloriesBurnedSource.estimatedActiveAndBmr;
    return base.copyWith(
      caloriesKcal:
          estimatedCaloriesLoaded ? projection.caloriesKcal : base.caloriesKcal,
      caloriesKcalSource: estimatedCaloriesLoaded
          ? projection.caloriesKcalSource
          : base.caloriesKcalSource,
      bmi: projection.loadedMetrics.contains(DashboardMetric.bmi)
          ? projection.bmi
          : base.bmi,
      ffmi: projection.loadedMetrics.contains(DashboardMetric.ffmi)
          ? projection.ffmi
          : base.ffmi,
      sleepScore: projection.loadedMetrics.contains(DashboardMetric.sleep)
          ? projection.sleepScore
          : base.sleepScore,
      restingHeartRateBaselineBpm:
          projection.loadedMetrics.contains(DashboardMetric.restingHeartRate)
              ? projection.restingHeartRateBaselineBpm
              : base.restingHeartRateBaselineBpm,
      hrvRmssdMs: projection.loadedMetrics.contains(DashboardMetric.hrv)
          ? projection.hrvRmssdMs
          : base.hrvRmssdMs,
      hrvBaselineRmssdMs: projection.loadedMetrics.contains(DashboardMetric.hrv)
          ? projection.hrvBaselineRmssdMs
          : base.hrvBaselineRmssdMs,
      weeklyCardioLoad:
          projection.loadedMetrics.contains(DashboardMetric.weeklyCardioLoad)
              ? projection.weeklyCardioLoad
              : base.weeklyCardioLoad,
      weeklyIntensityMinutes:
          projection.loadedMetrics.contains(DashboardMetric.intensityMinutes)
              ? projection.weeklyIntensityMinutes
              : base.weeklyIntensityMinutes,
      loadedMetrics: {...base.loadedMetrics, ...projection.loadedMetrics},
    );
  }

  static int roundCardioTarget(double value) =>
      math.max((value / 5.0).round() * 5, 5);

  static DateTime _startOfDay(LocalDate date) =>
      DateTime(date.year, date.month, date.day);

  static DateTime _maxInstant(DateTime a, DateTime b) => a.isAfter(b) ? a : b;

  static DateTime _minInstant(DateTime a, DateTime b) => a.isBefore(b) ? a : b;
}

class WeeklyCardioTarget {
  const WeeklyCardioTarget({required this.score, required this.source});

  final int score;
  final DashboardWeeklyCardioLoadTargetSource source;

  @override
  bool operator ==(Object other) =>
      other is WeeklyCardioTarget &&
      other.score == score &&
      other.source == source;

  @override
  int get hashCode => Object.hash(score, source);
}
