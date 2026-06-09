# Manual Activity Entry Plan

This tracks the OpenTracks-like manual activity entry work.

The goal is to support Health Connect activity logging for GPS-capable activities such as running, cycling, walking, and hiking, while keeping OpenVitals local-first and Health Connect as the source of truth.

## Phase 1: Health Connect Workout Write

Status: Mostly done

Implemented:

- Added an activity write model for Health Connect activity sessions.
- Added `WRITE_EXERCISE`, `WRITE_EXERCISE_ROUTE`, `WRITE_DISTANCE`, `WRITE_ELEVATION_GAINED`, `WRITE_ACTIVE_CALORIES_BURNED`, and `WRITE_TOTAL_CALORIES_BURNED` manifest permissions.
- Extended `ActivityRepository` with activity write permission checks and `writeActivityEntry`.
- Activity Entry asks for the full activity write permission set up front so route, distance, elevation, active calories, and total calories do not trigger later permission requests.
- Added Health Connect write support for:
  - `ExerciseSessionRecord`
  - optional `ExerciseRoute`
  - optional `DistanceRecord`
  - optional `ElevationGainedRecord`
  - optional active calories
  - optional total calories

Still needed:

- Real-device validation for Health Connect route-write behavior.
- Confirm written routes appear correctly in activity detail after Health Connect grants route access.
- Confirm each optional write permission behaves consistently across Android and Health Connect provider versions.

## Phase 2: Manual Entry UI

Status: Done for MVP

Implemented:

- Added an Activity tile to Add Entry.
- Added an Activity Entry route and top bar title.
- Added an entry source choice for:
  - creating an activity manually without a route file
  - importing GPX/KML/KMZ/FIT first, then reviewing and completing missing details before saving
- Added an Activity Entry screen with:
  - activity type selector
  - title
  - start date
  - start time
  - duration
  - optional distance
  - optional elevation
  - optional active calories
  - optional total calories
  - notes
  - Health Connect write permission handling
- Replaced typed start date/time fields with picker controls backed by Material date/time dialogs.
- Added field-specific validation state and inline messages for activity type, start date/time, duration, distance, elevation, active calories, and total calories.
- Added localized Activity Entry exercise type labels for English and Spanish.
- Added GPS-capable activity type definitions for running, biking, walking, hiking, wheelchair, rowing, paddling, skiing, snowboarding, snowshoeing, skating, sailing, surfing, open-water swimming, and golf.
- Wired the dashboard Workout add action to Activity Entry.

Still needed:

- Real-device review of the picker layout on compact screens.

## Phase 3: Route File Import

Status: Mostly done

Implemented:

- Added GPX/KML/KMZ/FIT import from Android document picker.
- Parses `trkpt` and `rtept` points with latitude, longitude, elevation, and timestamp.
- Parses timestamped KML `gx:Track` points from KMZ files.
- Parses KML `LineString` coordinates from KML/KMZ files even when the file has no per-point timestamps.
- Retimes untimestamped imported routes from the user-selected start date, start time, and duration before writing to Health Connect.
- Reads basic route metadata when present: name, description, and type where the format provides it.
- Reads FIT activity route points and sport metadata from supported FIT files.
- Computes route distance and elevation gain.
- Applies imported route start time, duration, title, notes, inferred activity type, distance, and elevation to the entry form.
- Estimates active and total calories for imported routes while preserving user-entered calorie values.
- Shows a route preview using the existing route preview style.
- Writes imported route points into the Health Connect exercise session.
- Simplifies very large imported routes to a bounded point count for Health Connect writes while keeping distance and elevation summaries based on the full imported route.
- Added more specific route import error messages for unsupported or malformed GPX/KML/KMZ/FIT files.
- Added tests for GPX parsing, KMZ parsing, FIT parsing, untimestamped KML `LineString` parsing, route retiming, large-route simplification, and route summary behavior.
- Decided for MVP: laps, segments, speed series, and other route extensions are ignored during import. The manual entry writes one Health Connect exercise session plus optional route and summary records.

Still needed:

- Test with real GPX/KML/KMZ/FIT exports from OpenTracks and other apps.
- Real-device validation that simplified and untimestamped KML/KMZ routes write and display correctly through Health Connect.

## Phase 4: Live GPS Recorder

Status: Mostly done

Implemented:

- Added Android runtime permission flow for GPS recording:
  - Health Connect activity write permissions are requested first.
  - Location permissions are requested before starting the recorder.
  - Android 13+ notification permission is requested with the recording permission set.
- Added location and foreground-service manifest permissions.
- Added an optional GPS hardware feature declaration so devices without GPS can still install the app.
- Added a foreground `ActivityRecordingService` with `foregroundServiceType="location"`.
- Added a persistent activity recording notification with pause/resume and discard actions.
- Added a singleton activity recording controller that:
  - starts recording from Activity Entry
  - pauses and resumes location updates
  - finishes a recording into the existing Activity Entry review form
  - discards an in-progress recording
  - keeps in-progress state across screen recreation and process recreation using app-private preferences
- Persists pause intervals during recording and writes them to Health Connect as pause exercise segments.
- Added a focused live recording screen with route preview, distance, total time, moving time, speed, elevation gain, point count, accuracy, pause/resume, finish, and discard controls.
- `Record activity` now opens a ready screen first; recording starts only after the user taps `Start`.
- The ready screen now acquires GPS before start and disables `Start` until there is a fresh precise GPS fix.
- Finishing a recording opens the existing activity edit/confirm form so title, notes, calories, type, distance, and elevation can be reviewed before saving.
- Added location accuracy filtering:
  - recording now requires precise location, not approximate/coarse location
  - live route samples now come from the GPS provider only
  - non-GPS samples, samples without accuracy, samples above 30 m accuracy, stale samples, and samples timestamped before the recording start are dropped
  - implausible GPS jumps are dropped when the implied speed is unrealistic
  - last accuracy and dropped-point count are tracked
- Added OpenTracks/FitoTrack-inspired sampling:
  - 1 second GPS provider polling
  - 0 meter provider distance threshold so the app can apply its own filtering
  - 500 ms minimum sample spacing
  - activity-aware minimum accepted sample distance: lower for open-water swimming, moderate for walking/running/hiking, higher for cycling/skiing/sailing
- Computes live distance and elevation gain from accepted GPS points.
- Finishing a recording creates the same route-backed entry shape used by GPX/KML/KMZ/FIT import, so final save still uses the Phase 1 Health Connect activity write path.
- If a recording finishes without enough GPS points, the screen falls back to a manual activity entry with the recorded start time, duration, and calorie estimates.
- Fully manual activity entries do not estimate calories automatically.
- Checked OpenTracks and FitoTrack GPS recording behavior before tuning:
  - OpenTracks records against `LocationManager.GPS_PROVIDER`, requests high accuracy updates, and filters poor-accuracy fixes.
  - FitoTrack records against `LocationManager.GPS_PROVIDER`, samples every second, treats fixes above 30 m as bad signal, treats signal as lost after 10 seconds, and skips samples too close in time or distance.

Still needed:

- Real-device validation of foreground service startup on Android 14+ and Android 15/16 target behavior.
- Real-device validation that the persistent notification appears as expected when notification permission is granted or denied.
- Real-device validation of GPS quality, distance, elevation gain, pause/resume behavior, and Health Connect pause segment display.
- Writes active exercise segments around pause segments so Health Connect receives active, pause, active intervals for paused recordings.
- Tune the 30 m accuracy threshold, stale-fix threshold, and activity-aware sample distances after outdoor testing.
- Add richer recovery UI for restored in-progress recordings after process death.
- Nearby Devices permission is not requested for this MVP because it does not improve phone GPS precision by itself. It is only useful if OpenVitals later supports Bluetooth sensors or external GNSS devices.

## Verification So Far

Completed:

- `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --no-daemon`
- `.\gradlew.bat :app:assembleDebug --no-daemon`
- `.\gradlew.bat :app:lintDebug --no-daemon` completed successfully once
- `.\gradlew.bat --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
- `.\gradlew.bat --no-daemon :app:bundleRelease`
- `git diff --check`

Known verification gap:

- Real-device Health Connect route-write testing is still required.
