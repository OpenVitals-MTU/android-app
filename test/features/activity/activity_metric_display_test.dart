import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/period_comparison.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/query/activity_period_data.dart';
import 'package:openvitals/features/activity/activity_metric.dart';
import 'package:openvitals/features/activity/activity_metric_display.dart';

/// Unit coverage for the Kotlin `ActivityPresentationMapper` port.
void main() {
  final day3 = LocalDate(2026, 7, 3);
  final day4 = LocalDate(2026, 7, 4);
  final day5 = LocalDate(2026, 7, 5);
  final week = DatePeriod(day3, day5);

  DailySteps steps(
    LocalDate date, {
    int steps = 0,
    double distance = 0,
    int? floors,
    double? activeCalories,
    double? elevation,
    int? wheelchair,
  }) =>
      DailySteps(
        date: date,
        steps: steps,
        distanceMeters: distance,
        floorsClimbed: floors,
        activeCaloriesKcal: activeCalories,
        elevationGainedMeters: elevation,
        wheelchairPushes: wheelchair,
      );

  DailyNutrition nutrition(LocalDate date, double caloriesBurnedKcal) =>
      DailyNutrition(
        date: date,
        hydrationLiters: 0,
        caloriesBurnedKcal: caloriesBurnedKcal,
      );

  ActivityProgressPoint point(int hour, {int totalSteps = 0, int? floors}) =>
      ActivityProgressPoint(
        time: DateTime(2026, 7, 5, hour),
        totalSteps: totalSteps,
        totalDistanceMeters: null,
        totalCaloriesBurnedKcal: null,
        totalFloorsClimbed: floors,
      );

  ActivityMetricDisplay displayFor(
    ActivityMetric metric,
    ActivityPeriodData data, {
    TimeRange range = TimeRange.week,
    DatePeriod? period,
    double dailyGoal = 8000,
  }) =>
      activityMetricDisplay(
        metric: metric,
        data: data,
        range: range,
        period: period ?? week,
        dailyGoal: dailyGoal,
      );

  group('steps', () {
    test('sums values and counts only the days with movement', () {
      final display = displayFor(
        ActivityMetric.steps,
        ActivityPeriodData(dailySteps: [
          steps(day3, steps: 9000),
          steps(day4, steps: 0),
          steps(day5, steps: 7000),
        ]),
      );

      expect(display.values, [9000, 0, 7000]);
      expect(display.total, 16000);
      expect(display.best, 9000);
      expect(display.activeDays, 2);
      // The zero day is not "tracked".
      expect(display.trackedDates, [day3, day5]);
    });

    test('the daily average divides by active days, not calendar days', () {
      final display = displayFor(
        ActivityMetric.steps,
        ActivityPeriodData(dailySteps: [
          steps(day3, steps: 9000),
          steps(day4, steps: 0),
          steps(day5, steps: 7000),
        ]),
      );
      expect(averageOrZero(display.total, display.activeDays), 8000);
      expect(display.baselineCurrentValue, 8000);
    });

    test('compares against the previous period total', () {
      final display = displayFor(
        ActivityMetric.steps,
        ActivityPeriodData(
          dailySteps: [steps(day5, steps: 10000)],
          previousDailySteps: [steps(day3, steps: 8000)],
        ),
      );
      expect(display.previousTotal, 8000);
      expect(display.periodComparison!.currentValue, 10000);
      expect(display.periodComparison!.direction, PeriodComparisonDirection.up);
    });

    test('goal progress counts the days that reached the target', () {
      final display = displayFor(
        ActivityMetric.steps,
        ActivityPeriodData(dailySteps: [
          steps(day3, steps: 9000),
          steps(day4, steps: 100),
          steps(day5, steps: 8000),
        ]),
        dailyGoal: 8000,
      );
      // 9000 and 8000 meet an at-least goal of 8000; 100 does not.
      expect(display.goalProgress!.goalMetDays, 2);
      expect(display.goalProgress!.trackedDays, 3);
    });

    test('a week with no rows has no data; a day always does', () {
      expect(
        displayFor(ActivityMetric.steps, const ActivityPeriodData()).hasData,
        isFalse,
      );
      expect(
        displayFor(
          ActivityMetric.steps,
          const ActivityPeriodData(),
          range: TimeRange.day,
          period: DatePeriod(LocalDate(2026, 7, 5), LocalDate(2026, 7, 5)),
        ).hasData,
        isTrue,
      );
    });
  });

  group('sample count', () {
    test('a day is described by its intraday samples', () {
      final display = displayFor(
        ActivityMetric.steps,
        ActivityPeriodData(
          dailySteps: [steps(day5, steps: 5000)],
          activityProgress: [
            point(8, totalSteps: 0),
            point(9, totalSteps: 1200),
            point(10, totalSteps: 5000),
          ],
        ),
        range: TimeRange.day,
        period: DatePeriod(day5, day5),
      );
      // The zero-valued sample does not count.
      expect(display.sampleCount, 2);
      expect(display.intradayPoints, hasLength(3));
      expect(display.dayTotal, 5000);
    });

    test('a longer period is described by its active days', () {
      final display = displayFor(
        ActivityMetric.steps,
        ActivityPeriodData(dailySteps: [
          steps(day3, steps: 9000),
          steps(day4, steps: 0),
        ]),
      );
      expect(display.sampleCount, 1);
    });

    test('intraday points are dropped for a metric the device never sampled',
        () {
      final display = displayFor(
        ActivityMetric.floors,
        ActivityPeriodData(
          dailySteps: [steps(day5, floors: 4)],
          activityProgress: [point(9, totalSteps: 100), point(10, floors: 4)],
        ),
        range: TimeRange.day,
        period: DatePeriod(day5, day5),
        dailyGoal: 10,
      );
      // Only the point that carries a floors reading survives.
      expect(display.intradayPoints, hasLength(1));
      expect(display.intradayPoints.single.value, 4);
    });
  });

  group('per-metric slices', () {
    test('calories burned reads the nutrition slice, not daily steps', () {
      final display = displayFor(
        ActivityMetric.caloriesOut,
        ActivityPeriodData(
          dailySteps: [steps(day5, steps: 9999)],
          nutrition: [
            nutrition(day3, 2100),
            nutrition(day5, 2300),
          ],
          previousNutrition: [
            nutrition(day3, 2000),
          ],
        ),
        dailyGoal: 2000,
      );

      expect(display.values, [2100, 2300]);
      expect(display.previousTotal, 2000);
      expect(display.hasData, isTrue);
    });

    test('a nullable metric has no data until a row actually carries it', () {
      final never = displayFor(
        ActivityMetric.floors,
        ActivityPeriodData(dailySteps: [steps(day3), steps(day4)]),
        dailyGoal: 10,
      );
      expect(never.hasData, isFalse);

      // A recorded zero is data; an absent column is not.
      final zero = displayFor(
        ActivityMetric.floors,
        ActivityPeriodData(dailySteps: [steps(day3, floors: 0)]),
        dailyGoal: 10,
      );
      expect(zero.hasData, isTrue);
      expect(zero.activeDays, 0);
    });

    test('steps and distance always have data when rows exist', () {
      final display = displayFor(
        ActivityMetric.distance,
        ActivityPeriodData(dailySteps: [steps(day3)]),
        dailyGoal: 5000,
      );
      expect(display.hasData, isTrue);
    });

    test('each metric reads its own column', () {
      final data = ActivityPeriodData(dailySteps: [
        steps(
          day5,
          steps: 9000,
          distance: 6500,
          floors: 12,
          activeCalories: 480,
          elevation: 95,
          wheelchair: 1500,
        ),
      ]);

      expect(displayFor(ActivityMetric.steps, data).values, [9000]);
      expect(displayFor(ActivityMetric.distance, data).values, [6500]);
      expect(displayFor(ActivityMetric.floors, data).values, [12]);
      expect(displayFor(ActivityMetric.activeCalories, data).values, [480]);
      expect(displayFor(ActivityMetric.elevation, data).values, [95]);
      expect(displayFor(ActivityMetric.wheelchair, data).values, [1500]);
    });
  });

  test('every metric maps to its own goal key', () {
    final keys = {
      for (final metric in ActivityMetric.values) activityMetricGoalKey(metric),
    };
    expect(keys, hasLength(ActivityMetric.values.length));
  });
}
