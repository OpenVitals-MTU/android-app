# Heart And Vitals

> **Status:** Current implemented behavior. One parametric screen serves all ten metrics.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/heart/`, `lib/features/vitals/`, `lib/features/manualentry/vitals_measurement_entry_screen.dart`, `lib/data/repository/contract/heart_repository.dart` + `vitals_repository.dart`, `lib/domain/usecase/load_heart_period_use_case.dart`.
> **Navigation:** `/heart_vitals` (overview); `/metric/:metricId` for all ten heart/vitals ids; `/manual_entry/vitals/:vitalsMeasurementType` (+ `/edit/:vitalsEntryId`).
> **Related:** [Feature map](feature-map.md), [Manual entry of metrics](manual-entry-metrics.md), [Statistics](statistics.md).

Heart and vitals are related but distinct feature areas, and the split is in the data layer, not the screen layer.

- `lib/features/heart/` owns the **parametric detail screen** used by all ten metrics: `HeartMetricScreen`, configured by the `HeartMetric` enum (`heart_metric.dart`), backed by `HeartMetricNotifier`. There is no `RestingHeartRateScreen` or `BloodPressureScreen` in the route table.
- `lib/features/vitals/` owns the Today Vitals overview (`heart_vitals_overview_screen.dart`). It also holds `vitals_screens.dart`, thin fixed-metric wrappers over `HeartMetricScreen` — a port artifact the router does not use; they are only exercised by `test/features/vitals/vitals_screens_test.dart`.
- The read split is real: `HeartRepository` serves heart rate / resting heart rate / HRV, `VitalsRepository` serves the rest, and `LoadHeartPeriodUseCase` combines them so one screen can load either family.

`HeartMetricNotifier` (a Riverpod `Notifier` over a `freezed` `HeartMetricState`) is shared across every heart and vitals route. That is an intentional shared loader, not a leftover: each `HeartMetric` declares whether it needs the heart-only or vitals-only load path. Unlike the retired Kotlin view model, the notifier does **not** precompute a display state — the per-metric derivations are cheap and are computed by the screen on demand.

## Implemented Metrics

Heart metrics:

- Average heart rate.
- Resting heart rate.
- HRV.

Vitals metrics:

- Blood pressure.
- SpO2.
- VO2 max.
- Respiratory rate.
- Body temperature.
- Blood glucose.
- Skin temperature.

## Detail Pattern

Heart and vitals detail screens use the shared metric detail scaffold:

- Day, week, month, and year ranges.
- Selected anchor date.
- Previous/next period navigation.
- Calendar date picking.
- Pull to refresh.
- Intraday charts where sample data is available.
- Period charts, statistics, thresholds, comparisons, data confidence, source labels, and entry lists.
- Reorderable sections.

OpenVitals-created vitals entries can be edited or deleted when the app has write permission and ownership can be verified. External records stay read-only.

## Today Vitals

The vitals overview groups related metrics into heart, cardiovascular, and respiratory/body-temperature sections. It uses the same period shell as other metric details and links into focused metric screens.

## Data Boundaries

Heart-rate and vitals records are read through feature-facing repository/use-case APIs rather than a global browser. Manual vitals entry is `lib/features/manualentry/vitals_measurement_entry_screen.dart` (there is no `manualentry/vitals/` subdirectory). Dashboard heart and vitals tiles open the `/heart_vitals` overview, which links on into the per-metric `/metric/:metricId` routes.
