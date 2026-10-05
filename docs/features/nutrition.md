# Nutrition

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/nutrition`, `features/manualentry/nutrition`, `data/repository/NutritionRepository.kt`.
> **Navigation:** `Screen.Nutrition`, `Screen.NutritionEntry`, `Screen.FoodEntry`, `Screen.Metric`; widgets `CALORIES_IN`, `PROTEIN`, `CARBS`, `FAT`; entry widgets `FOOD`, `NUTRITION`.
> **Related:** [Feature map](feature-map.md), [Manual entry of metrics](manual-entry-metrics.md), [Food logging](food-logging.md), [Preloaded beverage nutrition reference](preloaded-beverage-nutrition.md).

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
- Over a week, month or year the nutrient tiles lead with a **daily average**; see below.
- Reorderable detail sections.

Nutrition records remain in Health Connect. OpenVitals writes nutrition records through explicit entry flows: food logging, the nutrition entry form and beverage logging.

The entry list on the nutrition screens (reached from the Calories in, Protein, Carbs and Fat widgets) lets the user act on what OpenVitals wrote:

- Every OpenVitals entry swipes to delete. Records from other apps are read-only.
- A typed entry has an edit button that opens the nutrition form with its values and time. Saving replaces the record in place, so it keeps its id and stays off the beverage screens. Old carbs-only entries edit the same way.
- A drink has an edit button that opens the drink: its nutrients follow the water, so they are not edited apart from it.
- A logged food portion is delete-only, so the food catalog stays the one place its nutrients are set.

## Daily Averages Over A Period

Nobody eats by the month. Over a week, month or year each nutrient tile leads
with the daily average and keeps the period total as a caption underneath, so
the figure the eye lands on is the one that means something. This covers the
vitamin and mineral grids too, not only the four macros.

A single day is left alone: its total already *is* the day, and restating it as
an average says the same thing twice. For the same reason the per-metric
screen's own "Daily average" tile is hidden on a Day period.

### What the average divides by

Settings, Nutrition holds one switch: **Average logged days only**, on by
default.

- **On** — the divisor is the days that carried logged food. What someone who
  logs occasionally means by "my daily calories": the average of the meals they
  actually recorded, undiluted by the days they did not.
- **Off** — the divisor is every elapsed day of the period. What someone who
  logs daily means: a day with nothing recorded is a day they ate little, not a
  day to leave out.

Either way a period still running divides only by the days that have *happened*.
Dividing this month's food by 31 on the 13th would report a third of what was
eaten each day.

The daily values Health Connect returns are sparse — it emits no bucket for a
day with no records — so the "every day" divisor comes from the period's own
calendar, never from the length of the value list. See
`features/nutrition/NutritionAverages.kt`.

## Related Features

- [`manual-entry-metrics.md`](manual-entry-metrics.md): the nutrition entry form.
- [`beverage-logging-and-caffeine.md`](beverage-logging-and-caffeine.md): beverage nutrition defaults.
- [`preloaded-beverage-nutrition.md`](preloaded-beverage-nutrition.md): preset beverage reference data.
