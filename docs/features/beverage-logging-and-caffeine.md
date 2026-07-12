# Beverage Logging And Caffeine

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/manualentry/presentation/hydration_entry_screen.dart` + `hydration_catalog*.dart`, `lib/features/hydration/`, `lib/features/caffeine/`, `lib/features/nutrition/`, `lib/data/local/beverage/beverage_store.dart` (the drift `beverages` table), `lib/domain/insights/caffeine_health_drink_catalog.dart`.
> **Navigation:** `/manual_entry/hydration`, `/manual_entry/hydration/log/:hydrationDrinkId`, `/metric/HYDRATION`, `/metric/CAFFEINE`.
> **Related:** [Feature map](feature-map.md), [Hydration](hydration.md), [Caffeine sleep proposal](../proposals/caffeine-aware-sleep-insights.md).

OpenVitals supports beverage logging as an explicit Health Connect write flow for hydration, caffeine, and selected nutrition values.

## Beverage Catalog

The beverage entry flow supports preset drinks, categories, custom drinks, and frequently consumed drinks. Common options include water, coffee, tea, soft drinks, energy drinks, sports drinks, oral rehydration solution, milk, fruit juice, and custom beverages.

Users can choose drink type, container size, custom amount, and saved beverage defaults.

## Hydration Values

Beverages can contribute an effective hydration amount. Some drinks can use hydration multipliers so the logged hydration value better matches the selected beverage.

Hydration entries created by OpenVitals can be edited or deleted when the app has the required Health Connect write permission.

## Caffeine Values

Caffeine-aware drinks can write caffeine nutrition values and feed the caffeine detail screen. The caffeine experience can show:

- Total caffeine intake.
- Active caffeine estimates.
- Source and time-of-day context.
- Daily limit and sensitivity settings.
- Bedtime guidance.

## Nutrition Defaults

Selected beverages can include nutrition defaults that map to supported Health Connect nutrition fields. Health Connect remains the source of truth after the entry is saved.

## Relationship To Sleep

Existing caffeine records feed the standalone caffeine detail experience, where users can review active caffeine, timing, intake distribution, daily limits, sensitivity settings, and bedtime guidance. Direct sleep-detail integration is planned separately; current sleep scores are not adjusted by caffeine records.
