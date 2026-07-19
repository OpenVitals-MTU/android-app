# Beverage Logging And Caffeine

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/manualentry/presentation/hydration_entry_screen.dart` + `hydration_catalog*.dart`, `lib/features/hydration/`, `lib/features/caffeine/`, `lib/features/nutrition/`, `lib/data/local/beverage/beverage_store.dart` (the drift `beverages` table), `lib/domain/insights/caffeine_health_drink_catalog.dart`.
> **Navigation:** `/manual_entry/hydration`, `/manual_entry/hydration/log/:hydrationDrinkId`, `/metric/HYDRATION`, `/metric/CAFFEINE`.
> **Related:** [Feature map](feature-map.md), [Hydration](hydration.md), [Caffeine sleep proposal](../proposals/caffeine-aware-sleep-insights.md).

OpenVitals supports beverage logging as an explicit Health Connect write flow for hydration, caffeine, and selected nutrition values.

## How to use it

### Log a beverage

1. Tap **Log › Hydration** on the dashboard to open **Beverage entry**, then pick a drink from the catalog. See [Hydration](hydration.md) for the step-by-step.
2. **One drink writes three things at once:** a hydration record (the amount scaled by the drink's hydration impact), a caffeine value, and any other nutrients, all scaled to the amount you poured. A zero-hydration drink logs nutrition only and tells you **"Saved as nutrition only. No hydration was added."**

### Make a custom drink

1. In the catalog, tap **New drink**.
2. Fill in **Name**, **Amount**, and **Category**, then choose a **Hydration impact** — **Counts fully**, **Counts partially** (reveals a "Counts as hydration (%)" field), or **Does not count**.
3. Add **Nutrients** with **Add nutrient** (caffeine, calories, sugar, and so on). Save it, and it appears in your catalog to reuse, edit, or delete like any preset.

### Track caffeine

1. Open the **Caffeine** detail from its dashboard tile. It's read-only: **Active caffeine now**, a sleep-impact banner, a caffeine-decay curve, and a **Drinks** list (tap a drink for its own curve; swipe to delete one you logged).
2. There is **no caffeine "+" button** — you add caffeine by logging a caffeinated beverage.
3. **Personalize the model** in **Settings › Nutrition › Caffeine model**. This is a pharmacokinetic model, not a simple daily cap: set **half-life**, **absorption**, **sleep threshold (mg)**, **bedtime**, plus sensitivity and habituation options. A live "Effective half-life" readout updates as you change them; tap **Save**. These values drive the active-caffeine estimate and the bedtime/safe-sleep guidance.

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
