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
    // `now` is noon on the period's first day: all three nights are still
    // ahead, so none count as lived — but every day still has its stat row
    // for the charts.
    expect(insights.totalNights, 0);
    expect(insights.dailyStats, hasLength(3));
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

  test('a midnight bedtime minutes away projects tonight, not last night',
      () {
    // 23:56 with 61-ish mg active and bedtime 00:00: the upcoming midnight is
    // four minutes away, so the projection must be ~the current level. The old
    // anchor ("today at 00:00") was 24h in the past and read 0 mg.
    const prefs = CaffeinePreferences(bedtime: LocalTime(0, 0));
    final drink = CaffeineEntry(
      id: 'evening-tea',
      startTime: LocalDate(2026, 7, 23).atTimeInstant(20),
      endTime: LocalDate(2026, 7, 23).atTimeInstant(20, 10),
      caffeineMg: 100.0,
      name: 'Tea',
      source: 'test.source',
      mealType: 0,
    );
    final now = LocalDate(2026, 7, 23).atTimeInstant(23, 56);

    final insights = CaffeineInsightCalculator.build(
      entries: [drink],
      period: DatePeriod(LocalDate(2026, 7, 23), LocalDate(2026, 7, 23)),
      preferences: prefs,
      now: now,
    );

    expect(insights.currentMg, greaterThan(0.0));
    expect(
      insights.bedtimeMg,
      closeTo(
        CaffeineInsightCalculator.activeCaffeineMg(
          entries: [drink],
          at: LocalDate(2026, 7, 24).atTimeInstant(0),
          preferences: prefs.normalized(),
        ),
        0.001,
      ),
    );
    expect(insights.bedtimeMg, greaterThan(insights.currentMg * 0.9));
  });

  test('the morning after, a midnight bedtime projects the coming night',
      () {
    // 09:00 the next day with no drinks today: the projection is tonight's
    // midnight (~28h after the drink), near zero. The old anchor was THIS
    // morning's 00:00 — nine hours in the past — and resurfaced last night's
    // leftover (~60 mg) as the forecast.
    const prefs = CaffeinePreferences(bedtime: LocalTime(0, 0));
    final drink = CaffeineEntry(
      id: 'evening-tea',
      startTime: LocalDate(2026, 7, 23).atTimeInstant(20),
      endTime: LocalDate(2026, 7, 23).atTimeInstant(20, 10),
      caffeineMg: 200.0,
      name: 'Tea',
      source: 'test.source',
      mealType: 0,
    );
    final now = LocalDate(2026, 7, 24).atTimeInstant(9);

    final insights = CaffeineInsightCalculator.build(
      entries: [drink],
      period: DatePeriod(LocalDate(2026, 7, 23), LocalDate(2026, 7, 24)),
      preferences: prefs,
      now: now,
    );

    expect(insights.currentMg, greaterThan(0.0));
    expect(insights.bedtimeMg, lessThan(insights.currentMg));
    expect(insights.bedtimeMg, lessThan(10.0));
  });

  test("a day's safe-for-sleep stat uses the midnight that ENDS the day", () {
    // Drink at 20:00 with bedtime 00:00: that day's night starts at the NEXT
    // midnight, four hours after the drink — decidedly not safe. The old
    // anchor (the midnight that started the day) predated the drink and
    // counted every such night as safe.
    const prefs = CaffeinePreferences(bedtime: LocalTime(0, 0));
    final drink = CaffeineEntry(
      id: 'evening-espresso',
      startTime: LocalDate(2026, 7, 1).atTimeInstant(20),
      endTime: LocalDate(2026, 7, 1).atTimeInstant(20, 10),
      caffeineMg: 200.0,
      name: 'Espresso',
      source: 'test.source',
      mealType: 0,
    );

    final insights = CaffeineInsightCalculator.build(
      entries: [drink],
      period: DatePeriod(LocalDate(2026, 7, 1), LocalDate(2026, 7, 1)),
      preferences: prefs,
      now: LocalDate(2026, 7, 2).atTimeInstant(12),
    );

    final stat = insights.dailyStats.single;
    expect(stat.bedtimeMg, greaterThan(prefs.sleepThresholdMg.toDouble()));
    expect(stat.safeForSleep, isFalse);
  });

  test('an un-lived night is neither safe nor unsafe until it happens', () {
    // A big espresso at 20:00 today projects over the threshold at tonight's
    // (future) midnight bedtime. That projection must not count as a lived
    // night: it neither joins totalNights/safeNights nor zeroes the streak
    // that yesterday's real, safe night established.
    const prefs = CaffeinePreferences(bedtime: LocalTime(0, 0));
    final yesterdayDrink = CaffeineEntry(
      id: 'yesterday-tea',
      startTime: LocalDate(2026, 7, 22).atTimeInstant(9),
      endTime: LocalDate(2026, 7, 22).atTimeInstant(9, 10),
      caffeineMg: 50.0,
      name: 'Tea',
      source: 'test.source',
      mealType: 0,
    );
    final tonightEspresso = CaffeineEntry(
      id: 'tonight-espresso',
      startTime: LocalDate(2026, 7, 23).atTimeInstant(20),
      endTime: LocalDate(2026, 7, 23).atTimeInstant(20, 10),
      caffeineMg: 200.0,
      name: 'Espresso',
      source: 'test.source',
      mealType: 0,
    );

    final insights = CaffeineInsightCalculator.build(
      entries: [yesterdayDrink, tonightEspresso],
      period: DatePeriod(LocalDate(2026, 7, 22), LocalDate(2026, 7, 23)),
      preferences: prefs,
      now: LocalDate(2026, 7, 23).atTimeInstant(21),
    );

    // July 22's night (bedtime 00:00 on the 23rd) has passed and was safe;
    // July 23's night is still ahead.
    final lived = insights.dailyStats
        .singleWhere((stat) => stat.date == LocalDate(2026, 7, 22));
    final pending = insights.dailyStats
        .singleWhere((stat) => stat.date == LocalDate(2026, 7, 23));
    expect(lived.nightCompleted, isTrue);
    expect(pending.nightCompleted, isFalse);
    expect(pending.safeForSleep, isFalse); // the projection itself is unsafe
    expect(insights.totalNights, 1);
    expect(insights.safeNights, 1);
    expect(insights.safeSleepStreak, 1);
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
