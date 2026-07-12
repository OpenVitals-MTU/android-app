import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/query/activity_period_data.dart';
import 'package:openvitals/features/activity/application/calories_display.dart';

/// The two folds the calories screen ran per card, per rebuild.
void main() {
  const day3 = LocalDate(2026, 7, 3);
  const day4 = LocalDate(2026, 7, 4);

  DailyNutrition burned(LocalDate date, double kcal) => DailyNutrition(
        date: date,
        hydrationLiters: 0,
        caloriesBurnedKcal: kcal,
      );

  DailySteps active(LocalDate date, double kcal) => DailySteps(
        date: date,
        steps: 0,
        distanceMeters: 0,
        activeCaloriesKcal: kcal,
      );

  test('each series totals its own slice', () {
    final display = buildCaloriesDisplay(ActivityPeriodData(
      nutrition: [burned(day3, 2100), burned(day4, 2300)],
      dailySteps: [active(day3, 400), active(day4, 500)],
    ));

    expect(display.caloriesOut.total, 4400);
    expect(display.caloriesOut.hasData, isTrue);
    expect(display.caloriesOut.values.map((v) => v.value), [2100.0, 2300.0]);

    expect(display.activeCalories.total, 900);
    expect(display.activeCalories.hasData, isTrue);
  });

  test('all-zero readings are no data — that is what shows the placeholder', () {
    final display = buildCaloriesDisplay(ActivityPeriodData(
      nutrition: [burned(day3, 0)],
      dailySteps: [active(day3, 0)],
    ));

    expect(display.caloriesOut.hasData, isFalse);
    expect(display.activeCalories.hasData, isFalse);
    expect(display.caloriesOut.total, 0);
  });

  test('an empty period derives empty series, not nulls', () {
    final display = buildCaloriesDisplay(const ActivityPeriodData());

    expect(display.caloriesOut.values, isEmpty);
    expect(display.caloriesOut.hasData, isFalse);
    expect(display.activeCalories.values, isEmpty);
    expect(display.activeCalories.total, 0);
  });
}
