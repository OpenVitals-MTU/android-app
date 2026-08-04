# Recording Of Activity

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/manualentry/activity`, `features/manualentry/activity/recording`, `features/activity`.
> **Navigation:** `Screen.ActivityEntry`, `Screen.ActivityEntryEdit`, `ManualEntryWidgetId.ACTIVITY`.
> **Related:** [Feature map](feature-map.md), [Activity and training plans](activity-training-plans.md), [Bluetooth LE sensors](ble-sensors.md), [GPX/KML/KMZ/TCX route import](route-file-import.md).

OpenVitals can record activities locally, review the result, and then save the activity to Health Connect.

## GPS Activity

GPS recording is for route-backed activities such as walking, running, or cycling. The recording flow can track location points, distance, elevation, moving time, pauses, and route preview data. When the activity is finished, the user reviews the draft before saving it to Health Connect.

The recording flow supports:

- Start, pause, resume, finish, and discard.
- A persistent recording notification.
- A configurable recording dashboard.
- Focus mode for a cleaner in-recording view.
- A high-contrast outdoor mode for better readability in bright conditions.
- Keep-screen-on support when enabled.
- Offline route maps when map packs have been imported.
- Post-activity speed and cadence charts when compatible samples are available.

## Repetition Activity

Repetition-oriented flows support activities such as strength training, push-ups, pull-ups, rope skipping, treadmill steps, and similar workouts where counts, sets, or repetition stats matter more than a GPS route.

Depending on the activity and available sensors, OpenVitals can show repetition counts, set details, rest timing, heart-rate context, and review data before saving.

## Sensor Support

OpenVitals has experimental Bluetooth LE support for paired heart-rate, cycling cadence, cycling power, and footpod devices during recording. Bluetooth and notification permissions are requested only where Android requires them.

## Recording Without GPS

A GPS-capable activity such as a run or a ride can be recorded with GPS switched off from the setup screen. No location permission is requested and no fix is waited for. The session becomes a timed recording with duration and heart rate.

The barometer and the step detector keep running, because neither needs a position, so elevation gain and steps are still recorded. Distance can be typed into the form when saving. What is lost is only what genuinely comes from location: the map, automatic distance, pace, and splits.

## Guided Heart-Rate Recovery Test

With a connected Bluetooth LE heart-rate sensor, a timed recording can run as a guided heart-rate recovery test:

1. Warm up, for a configurable time that defaults to three minutes.
2. Go hard, until an optional target heart rate is reached or the effort is ended by hand.
3. Stop, and stay still while the app measures how quickly the heart rate falls.

Each phase change is announced with a bell, a spoken cue, and a vibration. The cues are part of the protocol, so they are not optional.

The moment the effort stopped is saved to Health Connect as a rest segment running to the end of the session, which is how the measurement is found again later. That mark is not kept in the crash-recovery draft: the heart-rate samples exist only in memory, so a recording restored after a crash comes back as an ordinary one rather than claiming a measurement with no data behind it.

## Elevation Noise Filtering

GPS vertical readings are noisy, and adding up every small rise between points inflates elevation gain badly. OpenVitals instead smooths the route and requires a real change before it counts, so the reported gain matches the climbing that actually happened.

Barometer readings were already filtered and are preferred when the device has one. The same filter is applied to imported route files, so a recorded and an imported version of the same outing agree.
