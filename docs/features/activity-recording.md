# Recording Of Activity

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/manualentry/activity/` (incl. `activity/recording/`), `lib/features/manualentry/presentation/activity_entry_screen.dart`, `lib/features/activity/`.
> **Navigation:** `/manual_entry/activity?mode=record` (the dashboard's Start action goes straight here), `/manual_entry/activity`, `/manual_entry/activity/edit/:activityEntryId`; `ManualEntryWidgetId.activity`.
> **Related:** [Feature map](feature-map.md), [Activity and training plans](activity-training-plans.md), [Bluetooth LE sensors](ble-sensors.md).

OpenVitals can record activities locally, review the result, and then save the activity to Health Connect.

## How to use it

### Start a recording

1. Tap **Start workout** on the dashboard (or **Log › Activity**). The **Activity** screen offers **Create manually**, **Create from existing plan**, or **Record activity** — choose **Record activity**.
2. Grant **Notifications** if asked (recording runs a foreground service, so the permission is requested before the recorder opens).
3. On the setup screen, pick the **activity type**, then:
   - **GPS activities** show a GPS-fix status and a **Record without GPS** toggle. **Start stays disabled until a precise fix arrives** (or you turn GPS off). Tapping Start requests **precise location** if it isn't granted yet.
   - **Rep activities** (push-ups, pull-ups, rope skipping, treadmill, …) show a **How recording works** panel with per-type sensor guidance and a **Rest seconds** field. Step-counting types request **activity recognition** on Start.
   - If a heart-rate sensor is connected, you can turn on the **Heart rate recovery test** — see [Heart rate recovery](heart-rate-recovery.md).
4. Tap **Start**.

### While recording

- **GPS / timed activities:** **Pause / Resume**, **Finish**, and (GPS) **Lap** and **Add Marker**, over a live map and an editable metric dashboard. **Focus mode** gives a full-screen, glanceable view with one big Pause/Resume; exit with its corner button or the system back gesture. An outdoor high-contrast toggle sits top-right.
- **Rep activities:** a large live count with **− / +** corrections, **End set** / **Start next set**, and **Finish session**.
- To keep the screen on during recording, enable that preference under **Settings › Activities** (it's applied automatically, not a live toggle).

### Review and save (or discard)

Tap **Finish** to end the session and return to the entry form, pre-filled with everything recorded. Review and adjust the details, then tap **Save activity** to write it to Health Connect — or tap the **discard** (✕) button to throw the draft away. Nothing is saved until you choose **Save activity**.

## GPS Activity

GPS recording is for route-backed activities such as walking, running, or cycling. The recording flow can track location points, distance, elevation, moving time, pauses, and route preview data. When the activity is finished, the user reviews the draft before saving it to Health Connect.

The recording flow supports:

- Start, pause, resume, finish, and discard.
- A persistent recording notification.
- A configurable recording dashboard.
- Focus mode: a full-screen, glanceable view with no app bar, exited with its own corner button or the system back gesture.
- A high-contrast outdoor mode for better readability in bright conditions.
- Keep-screen-on support when enabled.
- Offline route maps when map packs have been imported.
- Post-activity speed and cadence charts when compatible samples are available.

## Repetition Activity

Repetition-oriented flows support activities such as strength training, push-ups, pull-ups, rope skipping, treadmill steps, and similar workouts where counts, sets, or repetition stats matter more than a GPS route.

Depending on the activity and available sensors, OpenVitals can show repetition counts, set details, rest timing, heart-rate context, and review data before saving.

## Sensor Support

OpenVitals has experimental Bluetooth LE support for paired heart-rate, cycling cadence, cycling power, and footpod devices during recording. Bluetooth and notification permissions are requested only where Android requires them.
