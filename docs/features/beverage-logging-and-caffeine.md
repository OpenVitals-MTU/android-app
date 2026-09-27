# Beverage Logging And Caffeine

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/manualentry/hydration`, `features/hydration`, `features/caffeine`, `features/nutrition`.
> **Navigation:** `Screen.HydrationEntry`, `Screen.HydrationEntryLogDrink`, widgets `HYDRATION`, `CAFFEINE`.
> **Related:** [Feature map](feature-map.md), [Hydration](hydration.md), [Caffeine sleep proposal](../proposals/caffeine-aware-sleep-insights.md).

OpenVitals supports beverage logging as an explicit Health Connect write flow for hydration, caffeine, and selected nutrition values.

## Beverage Catalog

The beverage entry flow supports preset drinks, categories, custom drinks, and frequently consumed drinks. Common options include water, coffee, tea, soft drinks, energy drinks, sports drinks, oral rehydration solution, milk, fruit juice, and custom beverages.

Users can choose drink type, container size, custom amount, and saved beverage defaults.

## Hydration Values

Beverages can contribute an effective hydration amount. Some drinks can use hydration multipliers so the logged hydration value better matches the selected beverage.

Hydration entries created by OpenVitals can be edited or deleted when the app has the required Health Connect write permission. A logged drink with nutrients is two Health Connect records: a hydration record, and a nutrition record found through the hydration record's client id. They are written, edited and deleted together. An edit shifts the nutrition record by the same time and scales its nutrients with the volume, so a coffee moved an hour later takes its caffeine with it. The hydration record is rewritten under its client id, not by record id, so the link survives the edit.

## Caffeine Values

Caffeine-aware drinks can write caffeine nutrition values and feed the caffeine detail screen.

The caffeine screen works like the other metric screens: Day, Week, Month and Year, a period you can step through, and sections you can reorder. It opens on Day. It remembers the last range you picked.

- **Day, today:** active caffeine now, the sleep verdict, the day's total and the time until you are under your threshold. Then the day's caffeine curve with the threshold line and a mark for each drink. Tap near a mark to see that drink.
- **Day, past:** that day's total, whether that night was over your threshold, and the curve for that day.
- **Week, Month, Year:** the period total with daily average, safe nights, top source and threshold. A chart of daily totals; tap a day in Week or Month to list its drinks. The daily and bedtime impact card with the safe-night calendar.
- **Every range:** the bedtime level, statistics with a comparison to the previous period, where the caffeine came from (apps, items, categories, time of day), the drinks in the period, and how the estimate works.

The curve covers the whole day, from midnight to midnight. If your bedtime is after midnight, it runs on to your bedtime.

## Where The Caffeine Settings Live

The caffeine model itself stays in Settings, Nutrition: half-life, absorption time, the sleep threshold, and bedtime, with the resulting effective half-life shown underneath.

The physiological factors that change how fast caffeine is cleared moved to Settings, Body profile, under Metabolism. They are facts about the person rather than parameters of the model, so they sit with age, weight, and heart rate. Both cards edit the same stored preferences, and leaving the metabolism factors alone uses population averages. See [Settings and preferences](settings-and-preferences.md).

## Nutrition Defaults

Selected beverages can include nutrition defaults that map to supported Health Connect nutrition fields. Health Connect remains the source of truth after the entry is saved.

## Relationship To Sleep

Existing caffeine records feed the standalone caffeine detail experience, where users can review active caffeine, timing, intake distribution, daily limits, sensitivity settings, and bedtime guidance. Direct sleep-detail integration is planned separately; current sleep scores are not adjusted by caffeine records.
