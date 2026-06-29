# Recording Of Activity

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
