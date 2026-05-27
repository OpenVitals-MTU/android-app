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
  - importing GPX/KMZ first, then reviewing and completing missing details before saving
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

- Added GPX/KMZ import from Android document picker.
- Parses `trkpt` and `rtept` points with latitude, longitude, elevation, and timestamp.
- Parses timestamped KML `gx:Track` points from KMZ files.
- Parses KML `LineString` coordinates from KML/KMZ files even when the file has no per-point timestamps.
- Retimes untimestamped imported routes from the user-selected start date, start time, and duration before writing to Health Connect.
- Reads basic route metadata when present: name, description, and type where the format provides it.
- Computes route distance and elevation gain.
- Applies imported route start time, duration, title, notes, inferred activity type, distance, and elevation to the entry form.
- Shows a route preview using the existing route preview style.
- Writes imported route points into the Health Connect exercise session.
- Simplifies very large imported routes to a bounded point count for Health Connect writes while keeping distance and elevation summaries based on the full imported route.
- Added more specific route import error messages for unsupported or malformed GPX/KML/KMZ files.
- Added tests for GPX parsing, KMZ parsing, untimestamped KML `LineString` parsing, route retiming, large-route simplification, and route summary behavior.
- Decided for MVP: laps, segments, speed series, and other route extensions are ignored during import. The manual entry writes one Health Connect exercise session plus optional route and summary records.

Still needed:

- Test with real GPX/KMZ exports from OpenTracks and other apps.
- Real-device validation that simplified and untimestamped KML/KMZ routes write and display correctly through Health Connect.

## Phase 4: Live GPS Recorder

Status: Not started

Still needed:

- Android runtime location permissions.
- Foreground location service.
- Persistent recording notification.
- Start, pause, resume, finish, and discard recording flow.
- Temporary in-progress recording state for process death or accidental navigation.
- Live route preview while recording.
- Location accuracy handling.
- Battery behavior and sampling interval decisions.
- Final write to Health Connect using the same activity write path created in Phase 1.

This phase is intentionally separate because the app currently has no location stack, no foreground service, and no runtime location permission flow.

## Verification So Far

Completed:

- `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --no-daemon`
- `.\gradlew.bat :app:assembleDebug --no-daemon`
- `.\gradlew.bat :app:lintDebug --no-daemon` completed successfully once
- `git diff --check`

Known verification gap:

- A later lint rerun hung without diagnostics after a layout-only tweak. No lint failure was reported, and the leftover Gradle daemon was stopped.
- Real-device Health Connect route-write testing is still required.
