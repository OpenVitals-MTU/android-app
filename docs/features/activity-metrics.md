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

The workout/session area also covers activity lists, activity detail, route preview/export, routeless workout export (TCX/CSV/FIT), heart-rate charts for workouts, activity summaries, and cardio-load context. The CSV export imports back through the CSV importer, one workout a row; see [CSV import](csv-import.md#openvitals-workout-export).

## One Workout, Two Writers

Health Connect does not merge workouts. Two watches worn on one walk, or a watch and its vendor app, each file their own session, and the activity statistics would add both. OpenVitals folds such sessions into one before anything counts them: two sessions whose spans overlap by at least 85 percent and whose starts and ends lie within 15 minutes of each other are the same workout, and the one carrying more (a route, a distance, a heart rate, an OpenVitals entry over another app's) is kept. Two different apps may call the same outing different things, a walk on one wrist and a hike on the other, so between sources the exercise type is not compared. Within one source two overlapping sessions of different types are two deliberate records and are both kept.

Steps and distance through the day are not touched by this: Health Connect itself keeps one app's data where several apps overlap, in the order its Data sources and priority setting lists them. That order ranks apps, not devices, so two devices syncing through the same app still add up there.

## Workout Distance

A workout's distance is the sum of the Health Connect distance records inside the session. When there are none, the route's length fills in.

Some apps save a GPS route but a distance estimated from steps, which can be half the real one. The **Distance from GPS routes** setting under Activities, off by default, makes a workout with a route take the route's length instead. Pace, speed, splits, the activity statistics, exports and the PDF report follow it. Workouts without a route keep their recorded distance. Routes written by other apps are readable only with the Exercise routes permission, which Health Connect grants in its own settings. The edit form still shows only the distance OpenVitals wrote.

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
