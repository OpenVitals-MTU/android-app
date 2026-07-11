import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';

part 'daily_goals.freezed.dart';

enum DailyGoalDirection {
  atLeast,
  atMost,
}

enum MetricDailyGoalKey {
  steps('goal_steps', 8000.0, 500.0, 50000.0, 500.0),
  distanceMeters('goal_distance_meters', 5000.0, 250.0, 50000.0, 250.0),
  caloriesOutKcal('goal_calories_out_kcal', 2000.0, 250.0, 6000.0, 50.0),
  activeCaloriesKcal('goal_active_calories_kcal', 400.0, 25.0, 3000.0, 25.0),
  floors('goal_floors', 10.0, 1.0, 200.0, 1.0),
  elevationMeters('goal_elevation_meters', 100.0, 5.0, 3000.0, 5.0),
  wheelchairPushes('goal_wheelchair_pushes', 1000.0, 50.0, 50000.0, 50.0),
  sleepHours('goal_sleep_hours', 8.0, 1.0, 14.0, 0.25),
  workoutMinutes('goal_workout_minutes', 30.0, 5.0, 240.0, 5.0),
  mindfulnessMinutes('goal_mindfulness_minutes', 10.0, 1.0, 120.0, 1.0),
  caloriesInKcal('goal_calories_in_kcal', 2000.0, 500.0, 6000.0, 50.0,
      DailyGoalDirection.atMost),
  proteinGrams('goal_protein_grams', 50.0, 5.0, 300.0, 5.0),
  carbsGrams('goal_carbs_grams', 275.0, 25.0, 800.0, 25.0,
      DailyGoalDirection.atMost),
  fatGrams('goal_fat_grams', 70.0, 5.0, 300.0, 5.0, DailyGoalDirection.atMost);

  const MetricDailyGoalKey(
    this.storageKey,
    this.defaultValue,
    this.minValue,
    this.maxValue,
    this.step, [
    this.direction = DailyGoalDirection.atLeast,
  ]);

  final String storageKey;
  final double defaultValue;
  final double minValue;
  final double maxValue;
  final double step;
  final DailyGoalDirection direction;

  double normalize(double value) => value.clamp(minValue, maxValue).toDouble();
}

@freezed
abstract class DailyGoalValue with _$DailyGoalValue {
  const factory DailyGoalValue({
    required LocalDate date,
    required double value,
  }) = _DailyGoalValue;
}

@freezed
abstract class DailyGoalDay with _$DailyGoalDay {
  const factory DailyGoalDay({
    required LocalDate date,
    required double value,
    required bool isTracked,
    required bool isMet,
  }) = _DailyGoalDay;
}

@freezed
abstract class DailyGoalProgress with _$DailyGoalProgress {
  const DailyGoalProgress._();

  const factory DailyGoalProgress({
    required double target,
    required DailyGoalDirection direction,
    required List<DailyGoalDay> days,
  }) = _DailyGoalProgress;

  int get trackedDays => days.where((day) => day.isTracked).length;

  int get goalMetDays => days.where((day) => day.isMet).length;

  int get successRatePercent =>
      trackedDays > 0 ? (goalMetDays * 100.0 / trackedDays).round() : 0;

  int get currentStreakDays {
    final sorted = [...days]..sort((a, b) => a.date.compareTo(b.date));
    var count = 0;
    for (final day in sorted.reversed) {
      if (!day.isMet) break;
      count += 1;
    }
    return count;
  }

  int get longestStreakDays {
    final sorted = [...days]..sort((a, b) => a.date.compareTo(b.date));
    var current = 0;
    var longest = 0;
    for (final day in sorted) {
      if (day.isMet) {
        current += 1;
        longest = current > longest ? current : longest;
      } else {
        current = 0;
      }
    }
    return longest;
  }

  double get averageGapToGoal {
    final gaps = days.where((day) => day.isTracked).map((day) {
      switch (direction) {
        case DailyGoalDirection.atLeast:
          return (target - day.value) < 0.0 ? 0.0 : target - day.value;
        case DailyGoalDirection.atMost:
          return (day.value - target) < 0.0 ? 0.0 : day.value - target;
      }
    }).toList();
    if (gaps.isEmpty) return 0.0;
    return gaps.fold<double>(0.0, (sum, gap) => sum + gap) / gaps.length;
  }
}

DailyGoalProgress dailyGoalProgress(
  List<DailyGoalValue> values,
  DatePeriod period,
  double target,
  DailyGoalDirection direction,
) {
  final valuesByDate = <LocalDate, double>{};
  for (final value in values) {
    valuesByDate[value.date] = (valuesByDate[value.date] ?? 0.0) + value.value;
  }

  final days = <DailyGoalDay>[];
  var date = period.start;
  while (!date.isAfter(period.end)) {
    final value = valuesByDate[date] ?? 0.0;
    final isTracked = value > 0.0;
    final bool isMet;
    switch (direction) {
      case DailyGoalDirection.atLeast:
        isMet = isTracked && value >= target;
      case DailyGoalDirection.atMost:
        isMet = isTracked && value <= target;
    }
    days.add(DailyGoalDay(
      date: date,
      value: value,
      isTracked: isTracked,
      isMet: isMet,
    ));
    date = date.plusDays(1);
  }

  return DailyGoalProgress(
    target: target,
    direction: direction,
    days: days,
  );
}
