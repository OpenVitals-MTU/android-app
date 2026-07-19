# Hydration

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/hydration/` (incl. `hydration/reminders/`), `lib/features/manualentry/presentation/hydration_entry_screen.dart`, `lib/data/repository/contract/hydration_repository.dart` (+ `impl/hydration_repository_impl.dart`).
> **Navigation:** `/metric/HYDRATION`; `/manual_entry/hydration` (+ `/edit/:hydrationEntryId`, `/log/:hydrationDrinkId`); `ManualEntryWidgetId.hydration`.
> **Related:** [Feature map](feature-map.md), [Beverage logging and caffeine](beverage-logging-and-caffeine.md), [Reminders](reminders.md).

The hydration feature owns the period-based hydration detail screen and hydration reminder controls.

## How to use it

### Log a drink

1. Tap **Log › Hydration** on the dashboard. The **Beverage entry** screen shows today's total against your goal (for example **"0.8 L / 2 L"**).
2. Pick a drink from the **catalog** — search it, or browse the sections (Frequently consumed, Saved drinks, then Water, Coffees, Teas, Energy drinks, and so on).
3. Tapping a drink opens a small dialog with the **Amount** pre-filled to that drink's serving and a date/time. Adjust if needed and **Save**. The running total updates in place, so you can log several drinks in a row.
4. **Plain water** logs even without nutrition-write permission; a drink that carries nutrients (like caffeine) needs the nutrition write permission granted first.

A quick-log home-screen widget can fire a saved drink's amount dialog directly — see [Home screen widgets](home-widgets.md). For custom drinks, presets, and how one drink also records caffeine and nutrition, see [Beverage logging and caffeine](beverage-logging-and-caffeine.md).

### Review and adjust

- Open the hydration detail from its dashboard tile for daily totals, trends, goal progress, and your entry history (with the **Day / Week / Month / Year** controls — see [Statistics](statistics.md)).
- Editing a hydration entry changes only its **timestamp** (the amount is fixed); delete an entry you logged by swiping its row.
- **Set your daily goal** in **Settings › Nutrition › Hydration goal** — a stepper in ±0.25 L steps (default 2.0 L), shown in your units.

For reminders that nudge you to drink through the day, see the [Reminders](#reminders) section below and [Reminders](reminders.md).

## What It Shows

Hydration can show:

- Daily hydration totals.
- Period totals and trends.
- Goal progress.
- Previous-period comparison and baseline context.
- Hydration entries, including entries created from beverage logging.
- Nutrition-only hydration-related entries when they contribute useful context.
- Data confidence and source labels.
- Hydration reminder configuration.

## Detail Pattern

Hydration follows the canonical metric detail model:

- Day, week, month, and year ranges.
- Selected anchor date.
- Previous/next navigation and calendar selection.
- Pull to refresh.
- Reorderable detail sections.
- Selected-day charts and entry history.

OpenVitals-created hydration entries can be edited or deleted from the relevant entry flow. External Health Connect records remain read-only.

## Reminders

Hydration reminders are local notification reminders. The feature stores reminder preferences locally, checks notification permission on supported Android versions, and does not create Health Connect records when reminders fire.

## Related Features

- [`beverage-logging-and-caffeine.md`](beverage-logging-and-caffeine.md): drink logging, hydration multipliers, caffeine, and nutrition defaults.
- [`reminders.md`](reminders.md): shared reminder behavior for hydration and mindfulness.
