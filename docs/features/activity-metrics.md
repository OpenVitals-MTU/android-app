# Activity Metrics

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/activity`, `data/repository/ActivityRepository.kt`.
> **Navigation:** `Screen.Metric`; widgets `STEPS`, `DISTANCE`, `CALORIES_OUT`, `ACTIVE_CALORIES`, `FLOORS`, `ELEVATION`, `WHEELCHAIR_PUSHES`.
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

The feature uses `ActivityRepository` through activity ViewModels. New activity metric work should keep metric-specific charts and rows in `features/activity` and use the shared period scaffold instead of adding a new screen shell.

## Related Features

- [`activity-recording.md`](activity-recording.md): GPS/repetition recording before saving to Health Connect.
- [`activity-training-plans.md`](activity-training-plans.md): planned workouts and activity setup defaults.
- [`route-file-import.md`](route-file-import.md): GPX/KML/KMZ route import review.
- [`fit-files-import.md`](fit-files-import.md): FIT activity, course, and workout import from Settings Data Importers.
- [`offline-maps-support.md`](offline-maps-support.md): local map packs for route display.
- [`non-health-connect-metrics-dashboard.md`](non-health-connect-metrics-dashboard.md): cardio load and other derived activity context.
