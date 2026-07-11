# Activity Metrics

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/activity/`, `lib/data/repository/contract/activity_repository.dart` (+ `impl/activity_repository_impl.dart`).
> **Navigation:** `/metric/:metricId` for `STEPS`, `DISTANCE`, `FLOORS`, `ELEVATION`, `WHEELCHAIR_PUSHES` (→ `ActivityMetricScreen`); `/calories` and `/metric/{CALORIES_OUT,ACTIVE_CALORIES,BMR}` (→ `CaloriesScreen`).
> **Related:** [Feature map](feature-map.md), [Statistics](statistics.md), [Recording of activity](activity-recording.md).

The activity feature owns period-based detail screens for movement metrics and workout sessions. It is separate from activity recording: recording and manual activity entry create records, while these screens read and explain existing Health Connect data.

## Implemented Metrics

Activity metric detail screens currently cover:

- Steps.
- Distance.
- Calories burned.
- Active calories.
- Floors.
- Elevation.
- Wheelchair pushes.

The workout/session area also covers activity lists, activity detail, route preview/export, heart-rate charts for workouts, activity summaries, and cardio-load context.

## Detail Pattern

Activity metric screens follow the shared period-detail model:

- Day, week, month, and year ranges.
- Selected anchor date.
- Previous/next period navigation.
- Calendar selection.
- Pull to refresh.
- Period charts and selected-day entry views where available.
- Statistics, comparisons, baselines, confidence, and source context.
- Reorderable metric detail sections.

The five movement metrics share **one parametric screen**: `ActivityMetricScreen` (`lib/features/activity/activity_metric_screen.dart`) is configured by the `ActivityMetric` enum (`activity_metric.dart`), which carries each metric's title, accent, required Health Connect permission, and value extraction. There is no `StepsScreen` or `FloorsScreen`. Calories, active calories and BMR are intercepted before that branch and render the `CaloriesScreen` aggregate.

State lives in `ActivityMetricNotifier` (a Riverpod `Notifier` over a `freezed` `ActivityMetricState`), which loads through `ActivityRepository`. Period selection is **not** owned by the notifier — `MetricDetailScaffold` owns it and hands a `PeriodSelection` down. New activity metric work should keep metric-specific charts and rows in `lib/features/activity/` and use the shared scaffold instead of adding a new screen shell.

## Related Features

- [`activity-recording.md`](activity-recording.md): GPS/repetition recording before saving to Health Connect.
- [`activity-training-plans.md`](activity-training-plans.md): planned workouts and activity setup defaults.
- [`route-file-import.md`](route-file-import.md): GPX/KML/KMZ route import review.
- [`fit-files-import.md`](fit-files-import.md): FIT activity, course, and workout import from Settings Data Importers.
- [`offline-maps-support.md`](offline-maps-support.md): local map packs for route display.
- [`non-health-connect-metrics-dashboard.md`](non-health-connect-metrics-dashboard.md): cardio load and other derived activity context.
