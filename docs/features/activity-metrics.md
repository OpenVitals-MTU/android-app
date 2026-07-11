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

## Activity Detail: Where a Session's Numbers Actually Live

**An `ExerciseSessionRecord` carries almost nothing.** A watch writes the activity itself as a session — little more than a type, a title and a duration — and writes its steps, distance, calories and elevation as *separate* records covering the same window. Reading the session by id therefore yields a duration and not much else, which is why a recorded walk once reported "Steps: Not available" directly above a chart of its own step cadence.

The detail screen reassembles a session from three sources, in this order (`ActivityDetailNotifier`):

1. **The session record** — whatever it does state is authoritative and is never overwritten.
2. **Sibling records over the session's window** — `ActivityRepository.loadWorkoutMetrics` → the native `readExerciseSessionMetrics`, which *aggregates* `StepsRecord`, `DistanceRecord`, `TotalCaloriesBurnedRecord`, `ActiveCaloriesBurnedRecord`, `ElevationGainedRecord`, `FloorsClimbedRecord`, `WheelchairPushesRecord` and `SpeedRecord` between the session's start and end. Each metric is gated on its own read permission, so an ungranted metric costs that one number rather than failing the whole read (`ExerciseSessionMetrics`, `withSessionMetricsBackfilled`).
3. **The samples** — heart rate, speed and cadence fill the averages the session omits, and a device that records speed but writes no `DistanceRecord` at all (a treadmill, some watches) gets its distance integrated from those speed samples, by the same trapezoidal rule the splits are cut with, so the header and the splits card cannot disagree (`withSampleBackfilledMetrics`).

A **zero total is treated as an empty summary, not a measurement** (`_backfilledByDouble` / `_backfilledByInt` in `activity_backfill.dart`), the same rule the route backfill uses — nobody wants "Wheelchair pushes: 0" on a walk.

### Which Rows the Metrics Card Shows

The card does not render a fixed row list. A metric appears when it **has a value**, or when **its absence is worth reporting for that kind of activity**:

- A recorded value is never hidden. A device that somehow counts steps on a bike ride still gets a Steps row; no relevance table may suppress real data.
- Relevance only decides which *absences* inform. "Distance: Not available" tells a cyclist the GPS did not record; "Wheelchair pushes: Not available" tells them nothing.
- Metrics needing hardware most people do not own (power meter, footpod, a bike computer's speed average) are value-only for every type.
- Pace is a *rendering* of distance over duration rather than a datum, so it always has a value; whether pace or speed is shown is gated on the activity type outright — a cyclist reads km/h, a runner reads min/km.

## Related Features

- [`activity-recording.md`](activity-recording.md): GPS/repetition recording before saving to Health Connect.
- [`activity-training-plans.md`](activity-training-plans.md): planned workouts and activity setup defaults.
- [`route-file-import.md`](route-file-import.md): GPX/KML/KMZ route import review.
- [`fit-files-import.md`](fit-files-import.md): FIT activity, course, and workout import from Settings Data Importers.
- [`offline-maps-support.md`](offline-maps-support.md): local map packs for route display.
- [`non-health-connect-metrics-dashboard.md`](non-health-connect-metrics-dashboard.md): cardio load and other derived activity context.
