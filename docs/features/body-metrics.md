# Body Metrics

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/body/`, `lib/features/manualentry/body_measurement_entry_screen.dart`, `lib/data/repository/contract/body_repository.dart` (+ `impl/body_repository_impl.dart`).
> **Navigation:** `/body`; `/metric/:metricId` for `WEIGHT`, `HEIGHT`, `BMI`, `FFMI`, `BODY_FAT`, `LEAN_MASS`, `BONE_MASS`, `BODY_WATER_MASS` — all of which render the same `BodyScreen` aggregate. `/manual_entry/body/:bodyMeasurementType` (+ `/edit/:bodyEntryId`) for entry. (`BMR` is a body metric conceptually, but `/metric/BMR` is claimed earlier by `CaloriesScreen`.)
> **Related:** [Feature map](feature-map.md), [Manual entry of metrics](manual-entry-metrics.md), [Statistics](statistics.md).

The body feature owns period-based detail screens for body measurement and composition metrics read from Health Connect.

## Implemented Metrics

Body metric detail screens currently cover:

- Weight.
- Height.
- BMI.
- Body fat.
- Lean mass.
- Basal metabolic rate.
- Bone mass.
- Body water mass.

BMI and FFMI-style context are derived from available measurements. Derived values should explain missing prerequisites instead of pretending the calculation is complete.

## Detail Pattern

Body metrics follow the canonical period-detail pattern:

- Day, week, month, and year ranges.
- Previous/next period navigation capped at the current period.
- Calendar selection and pull to refresh.
- Period charts, selected-day entries, statistics, comparisons, personal baselines, and data confidence.
- Entry lists that allow edit/delete only for OpenVitals-created records.
- Reorderable metric detail sections.

Manual body entry is `lib/features/manualentry/body_measurement_entry_screen.dart` (there is no `manualentry/body/` subdirectory) and writes explicit user-entered records to Health Connect. The dashboard and body detail screens remain read-oriented.

## Data Boundaries

The body feature reads through `BodyRepository` (contract in `lib/data/repository/contract/`, implementation in `impl/`), with state in `BodyMetricNotifier`. Unlike the other metric families there is no parametric per-metric screen: every body id renders the one `BodyScreen`, which shows the composition metrics inline. New body metric work should keep feature-specific formatting, cards, charts, and rows in `lib/features/body/`; shared components should only move out when another feature really reuses them.
