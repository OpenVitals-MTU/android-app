import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/caffeine_health_drink_catalog.dart';
import 'package:openvitals/domain/insights/caffeine_insight_calculator.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/preferences/caffeine_preferences.dart';

void main() {
  const preferences = CaffeinePreferences();
  final entry = CaffeineEntry(
    id: 'coffee-1',
    startTime: LocalDate(2026, 7, 1).atTimeInstant(8),
    endTime: LocalDate(2026, 7, 1).atTimeInstant(8).add(const Duration(minutes: 10)),
    caffeineMg: 100.0,
    name: 'Coffee',
    source: 'test.source',
    mealType: 0,
  );

  test('contribution is zero before intake and positive after absorption', () {
    expect(
      CaffeineInsightCalculator.contributionMg(
        entry: entry,
        at: entry.startTime.subtract(const Duration(minutes: 1)),
        preferences: preferences,
      ),
      closeTo(0.0, 0.001),
    );

    final contribution = CaffeineInsightCalculator.contributionMg(
      entry: entry,
      at: entry.startTime.add(const Duration(hours: 1)),
      preferences: preferences,
    );

    expect(contribution > 0.0, isTrue);
  });

  test('active caffeine decays over time', () {
    final early = CaffeineInsightCalculator.activeCaffeineMg(
      entries: [entry],
      at: entry.startTime.add(const Duration(hours: 1)),
      preferences: preferences,
    );
    final late = CaffeineInsightCalculator.activeCaffeineMg(
      entries: [entry],
      at: entry.startTime.add(const Duration(hours: 12)),
      preferences: preferences,
    );

    expect(early > late, isTrue);
  });

  test('build returns bedtime safety source and time bucket insights', () {
    final insights = CaffeineInsightCalculator.build(
      entries: [entry],
      period: DatePeriod(LocalDate(2026, 7, 1), LocalDate(2026, 7, 3)),
      preferences: preferences,
      now: LocalDate(2026, 7, 1).atTimeInstant(12),
    );

    expect(insights.periodTotalMg, closeTo(100.0, 0.001));
    expect(insights.periodAverageMg, closeTo(100.0 / 3.0, 0.001));
    expect(insights.loggedDays, 1);
    expect(insights.totalNights, 3);
    expect(insights.sourceTotals.single.label, 'test.source');
    expect(insights.categoryTotals.single.label, 'Coffee');
    expect(insights.timeToThresholdMinutes, isNotNull);
    expect(insights.curvePoints.isNotEmpty, isTrue);
    expect(
      insights.entryInsights.single.inferredCategory,
      CaffeineSourceCategory.coffee,
    );
    expect(
      insights.entryInsights.single.catalogMatch?.item.name,
      'Drip coffee',
    );
  });

  test('caffeine health catalog matches health connect names without local entries',
      () {
    expect(CaffeineHealthDrinkCatalog.items.length, 224);

    final redBull = CaffeineHealthDrinkCatalog.matchName('Red Bull 250 ml');
    final cokeZero = CaffeineHealthDrinkCatalog.matchName('Coke Zero');
    final matcha = CaffeineHealthDrinkCatalog.matchName('Matcha latte');

    expect(redBull?.item.name, 'Red Bull');
    expect(redBull?.item.category, CaffeineSourceCategory.energyDrink);
    expect(cokeZero?.item.category, CaffeineSourceCategory.soda);
    expect(matcha?.item.category, CaffeineSourceCategory.tea);

    final coffeePreset = CaffeineHealthDrinkCatalog.beveragePresets()
        .firstWhere((drink) => drink.id == 'caffeinehealth-drip-coffee');
    expect(coffeePreset.volumeMilliliters, closeTo(240.0, 0.001));
    expect(coffeePreset.isPreloaded, true);
    expect(coffeePreset.category, CaffeineSourceCategory.coffee);
    expect(
      coffeePreset.nutrientValues[NutritionNutrient.energy] ?? 0.0,
      closeTo(2.0, 0.001),
    );
    expect(
      coffeePreset.nutrientValues[NutritionNutrient.caffeine] ?? 0.0,
      closeTo(0.095, 0.001),
    );

    final redBullPreset = CaffeineHealthDrinkCatalog.beveragePresets()
        .firstWhere((drink) => drink.id == 'caffeinehealth-red-bull');
    expect(
      redBullPreset.nutrientValues[NutritionNutrient.energy] ?? 0.0,
      closeTo(110.0, 0.001),
    );
    expect(
      redBullPreset.nutrientValues[NutritionNutrient.sugar] ?? 0.0,
      closeTo(27.0, 0.001),
    );
    expect(
      redBullPreset.nutrientValues[NutritionNutrient.caffeine] ?? 0.0,
      closeTo(0.08, 0.001),
    );
    expect(
      CaffeineHealthDrinkCatalog.beveragePresetItem(coffeePreset.id)?.category,
      CaffeineSourceCategory.coffee,
    );
  });
}
