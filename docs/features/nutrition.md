# Nutrition

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/nutrition/`, `lib/features/manualentry/presentation/carbs_entry_screen.dart`, `lib/data/repository/contract/nutrition_repository.dart` (+ `impl/nutrition_repository_impl.dart`).
> **Navigation:** `/nutrition` (overview); `/metric/:metricId` for `CALORIES_IN`, `PROTEIN`, `CARBS`, `FAT` (all → the parametric `NutritionMetricScreen`); `/manual_entry/carbs`.
> **Related:** [Feature map](feature-map.md), [Manual entry of metrics](manual-entry-metrics.md), [Preloaded beverage nutrition reference](preloaded-beverage-nutrition.md).

The nutrition feature owns period-based nutrition detail screens for intake metrics read from Health Connect.

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
