import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/daily_goals.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/features/nutrition/application/nutrition_display.dart';

/// The derivations both nutrition screens used to do in their build paths — the
/// per-nutrient series and their statistics, the goal progress, the macro split,
/// the day curve and the meal list — now a pure function the view-model calls
/// once per load.
DailyMacros _macros(
  LocalDate date, {
  double energy = 0,
  double protein = 0,
  double carbs = 0,
  double fat = 0,
  Map<NutritionNutrient, double> nutrients = const {},
}) =>
    DailyMacros(
      date: date,
      energyKcal: energy,
      proteinGrams: protein,
      carbsGrams: carbs,
      fatGrams: fat,
      nutrientValues: {
        NutritionNutrient.energy: energy,
        NutritionNutrient.protein: protein,
        NutritionNutrient.totalCarbohydrate: carbs,
        NutritionNutrient.totalFat: fat,
        ...nutrients,
      },
    );

NutritionEntry _entry(DateTime time, {required double energyKcal}) =>
    NutritionEntry(
      time: time,
      mealType: 0,
      name: 'Meal',
      energyKcal: energyKcal,
      proteinGrams: null,
      carbsGrams: null,
      fatGrams: null,
      fiberGrams: null,
      sugarGrams: null,
      source: 'test',
    );

void main() {
  const monday = LocalDate(2026, 3, 2);
  final period = DatePeriod(monday, monday.plusDays(2));

  NutritionDisplay build({
    List<DailyMacros> dailyMacros = const [],
    List<DailyMacros> previous = const [],
    List<DailyMacros> baseline = const [],
    List<NutritionEntry> entries = const [],
    NutritionNutrient nutrient = NutritionNutrient.energy,
    double goal = 2000,
  }) =>
      buildNutritionDisplay(
        nutrient: nutrient,
        goalDirection: DailyGoalDirection.atLeast,
        dailyGoal: goal,
        period: period,
        dailyMacros: dailyMacros,
        previousDailyMacros: previous,
        baselineDailyMacros: baseline,
        entries: entries,
      );

  test('an empty period has no data, and every series is empty', () {
    final display = build();

    expect(display.hasData, isFalse);
    expect(display.hasMacros, isFalse);
    expect(display.metricSeries.total, 0.0);
    expect(display.metricSeries.average, 0.0);
    expect(display.metricSeries.best, 0.0);
    expect(display.metricSeries.loggedDays, 0);
    expect(display.trackedSeries, isEmpty);
    expect(display.macroSplit, isNull);
    expect(display.entriesNewestFirst, isEmpty);
    // The primary four always exist, even with nothing logged.
    expect(display.primarySeries.length, 4);
  });

  test('a series folds total, average, best and logged days', () {
    final display = build(dailyMacros: [
      _macros(monday, energy: 1800),
      // A day with nothing logged is not a logged day, and the average is over
      // the logged ones.
      _macros(monday.plusDays(1)),
      _macros(monday.plusDays(2), energy: 2200),
    ]);

    final series = display.metricSeries;
    expect(display.hasData, isTrue);
    expect(display.hasMacros, isTrue);
    expect(series.total, 4000.0);
    expect(series.loggedDays, 2);
    expect(series.average, 2000.0);
    expect(series.best, 2200.0);
    expect(series.values.length, 3);
  });

  test('tracked nutrients split into primary and grouped additional ones', () {
    final display = build(dailyMacros: [
      _macros(
        monday,
        energy: 1900,
        protein: 85,
        nutrients: const {NutritionNutrient.sodium: 2.1},
      ),
    ]);

    expect(
      display.trackedSeries.map((s) => s.nutrient),
      containsAll([
        NutritionNutrient.energy,
        NutritionNutrient.protein,
        NutritionNutrient.sodium,
      ]),
    );
    // Sodium is not a primary macro, so it lands in its own group.
    expect(display.additionalSeries.map((s) => s.nutrient),
        [NutritionNutrient.sodium]);
    expect(
      display.additionalSeriesByGroup[NutritionNutrientGroup.minerals]!
          .map((s) => s.nutrient),
      [NutritionNutrient.sodium],
    );
    expect(display.additionalSeriesByGroup[NutritionNutrientGroup.vitamins],
        isNull);
  });

  test('the macro split needs macros, and the comparison needs a previous '
      'period', () {
    final display = build(
      dailyMacros: [_macros(monday, energy: 2000, protein: 100, carbs: 250, fat: 60)],
      previous: [_macros(monday.minusDays(3), energy: 1000)],
    );

    expect(display.macroSplit, isNotNull);
    // 2000 against 1000: the period doubled.
    expect(display.comparison.previousValue, 1000.0);
    expect(display.comparison.currentValue, 2000.0);
  });

  test('the day curve accumulates in time order, skipping absent readings', () {
    final day = DateTime(2026, 3, 2);
    final display = build(
      dailyMacros: [_macros(monday, energy: 1000)],
      entries: [
        _entry(day.add(const Duration(hours: 13)), energyKcal: 700),
        _entry(day.add(const Duration(hours: 8)), energyKcal: 300),
        _entry(day.add(const Duration(hours: 10)), energyKcal: 0),
      ],
    );

    final samples = display.metricSeries.cumulativeSamples;
    expect(samples.map((s) => s.value).toList(), [300.0, 1000.0]);
    expect(samples.first.time.hour, 8);

    // A nutrient the entries carry nothing for gets no curve at all.
    final protein = display.allSeries
        .firstWhere((s) => s.nutrient == NutritionNutrient.protein);
    expect(protein.cumulativeSamples, isEmpty);
  });

  test('meals are listed newest first, and indexed by their day', () {
    final display = build(
      entries: [
        _entry(DateTime(2026, 3, 2, 8), energyKcal: 300),
        _entry(DateTime(2026, 3, 3, 13), energyKcal: 700),
        _entry(DateTime(2026, 3, 2, 20), energyKcal: 500),
      ],
    );

    expect(display.hasData, isTrue, reason: 'entries alone are data');
    expect(display.entriesNewestFirst.first.time, DateTime(2026, 3, 3, 13));
    expect(display.entriesByDay[monday]!.length, 2);
    // Each day's list is newest first too.
    expect(display.entriesByDay[monday]!.first.time, DateTime(2026, 3, 2, 20));
    expect(display.entriesByDay[monday.plusDays(1)]!.length, 1);
  });

  test('goal progress counts the days that met the goal', () {
    final display = build(
      dailyMacros: [
        _macros(monday, energy: 2100),
        _macros(monday.plusDays(1), energy: 1200),
        _macros(monday.plusDays(2), energy: 2500),
      ],
      goal: 2000,
    );

    expect(display.goalProgress.goalMetDays, 2);
    expect(display.goalProgress.target, 2000);
  });
}
