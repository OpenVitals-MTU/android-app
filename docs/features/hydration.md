# Hydration

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/hydration/` (incl. `hydration/reminders/`), `lib/features/manualentry/hydration_entry_screen.dart`, `lib/data/repository/contract/hydration_repository.dart` (+ `impl/hydration_repository_impl.dart`).
> **Navigation:** `/metric/HYDRATION`; `/manual_entry/hydration` (+ `/edit/:hydrationEntryId`, `/log/:hydrationDrinkId`); `ManualEntryWidgetId.hydration`.
> **Related:** [Feature map](feature-map.md), [Beverage logging and caffeine](beverage-logging-and-caffeine.md), [Reminders](reminders.md).

The hydration feature owns the period-based hydration detail screen and hydration reminder controls.

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
