# Nutrition

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/nutrition/`, `lib/features/manualentry/presentation/carbs_entry_screen.dart`, `lib/data/repository/contract/nutrition_repository.dart` (+ `impl/nutrition_repository_impl.dart`).
> **Navigation:** `/nutrition` (overview); `/metric/:metricId` for `CALORIES_IN`, `PROTEIN`, `CARBS`, `FAT` (all → the parametric `NutritionMetricScreen`); `/manual_entry/carbs`.
> **Related:** [Feature map](feature-map.md), [Manual entry of metrics](manual-entry-metrics.md), [Preloaded beverage nutrition reference](preloaded-beverage-nutrition.md).

The nutrition feature owns period-based nutrition detail screens for intake metrics read from Health Connect.

## How to use it

1. **Open the overview.** Tap a nutrient tile on the dashboard to open the **Nutrition** overview. Its **Statistics** section groups totals under headers (Carbohydrates, Fats, Vitamins, Minerals, Other), **Nutrition trends** charts each tracked nutrient, and **Meals** lists every logged meal newest-first.
2. **Open one nutrient.** Tap through to a per-nutrient detail (**Calories in**, **Protein**, **Carbohydrates**, **Fat**) for its hero total, trend chart, and full statistics with previous-period comparison and baseline.
3. **Set a goal.** On a nutrient detail, use the **− / +** on the daily-goal card to adjust its target.
4. **Move through time.** The shared **Day / Week / Month / Year** controls, calendar, and pull-to-refresh apply here too (see [Statistics](statistics.md)).
5. **Log intake.** Of the four metrics, only **Carbs** has its own entry tile — use **Log › Carbs** on the dashboard. Calories-in, protein, and fat come from meals or beverages logged elsewhere (see [Beverage logging and caffeine](beverage-logging-and-caffeine.md)) or from other apps writing into Health Connect.
6. **Remove a meal.** Swipe a meal you logged in OpenVitals to delete it from the **Meals** list.

## Implemented Metrics

Nutrition metric detail screens currently cover:

- Calories in.
- Protein.
- Carbohydrates.
- Fat.

Caffeine is intentionally separate. It is a caffeine-specific analytics and setup experience described in [`beverage-logging-and-caffeine.md`](beverage-logging-and-caffeine.md); planned direct sleep integration is tracked in [`caffeine-aware-sleep-insights.md`](../proposals/caffeine-aware-sleep-insights.md).

## Detail Pattern

Nutrition metrics follow the canonical period-detail pattern:

- Day, week, month, and year ranges.
- Selected anchor date.
- Previous/next navigation and calendar selection.
- Pull to refresh.
- Goal progress for supported intake metrics.
- Period charts, selected-day breakdowns, entries, statistics, comparisons, confidence, and source labels.
- Reorderable detail sections.

Nutrition records remain in Health Connect. OpenVitals writes nutrition records through explicit entry flows such as carbohydrate entry and beverage logging; the nutrition detail screens remain read-oriented.

## Related Features

- [`manual-entry-metrics.md`](manual-entry-metrics.md): carbohydrate entry.
- [`beverage-logging-and-caffeine.md`](beverage-logging-and-caffeine.md): beverage nutrition defaults.
- [`preloaded-beverage-nutrition.md`](preloaded-beverage-nutrition.md): preset beverage reference data.
