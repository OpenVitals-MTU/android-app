# Permissions

OpenVitals asks for permissions by purpose. The local app removes inherited network permissions during manifest merge and should not ship app-level `INTERNET`, network-state, or Wi-Fi-state permissions.

This page mirrors the current permission surface declared in `app/src/main/AndroidManifest.xml`.

## Health Connect Read Permissions

Used to show records in the dashboard, metric detail screens, readiness, statistics, achievements, and insights:

- `android.permission.health.READ_STEPS`
- `android.permission.health.READ_DISTANCE`
- `android.permission.health.READ_EXERCISE`
- `android.permission.health.READ_EXERCISE_ROUTES`
- `android.permission.health.READ_SLEEP`
- `android.permission.health.READ_HEART_RATE`
- `android.permission.health.READ_RESTING_HEART_RATE`
- `android.permission.health.READ_HEART_RATE_VARIABILITY`
- `android.permission.health.READ_WEIGHT`
- `android.permission.health.READ_HEIGHT`
- `android.permission.health.READ_BODY_FAT`
- `android.permission.health.READ_LEAN_BODY_MASS`
- `android.permission.health.READ_BASAL_METABOLIC_RATE`
- `android.permission.health.READ_BONE_MASS`
- `android.permission.health.READ_BODY_WATER_MASS`
- `android.permission.health.READ_FLOORS_CLIMBED`
- `android.permission.health.READ_ACTIVE_CALORIES_BURNED`
- `android.permission.health.READ_ELEVATION_GAINED`
- `android.permission.health.READ_WHEELCHAIR_PUSHES`
- `android.permission.health.READ_TOTAL_CALORIES_BURNED`
- `android.permission.health.READ_SPEED`
- `android.permission.health.READ_POWER`
- `android.permission.health.READ_STEPS_CADENCE`
- `android.permission.health.READ_CYCLING_PEDALING_CADENCE`
- `android.permission.health.READ_PLANNED_EXERCISE`
- `android.permission.health.READ_HYDRATION`
- `android.permission.health.READ_NUTRITION`
- `android.permission.health.READ_MINDFULNESS`
- `android.permission.health.READ_BLOOD_PRESSURE`
- `android.permission.health.READ_OXYGEN_SATURATION`
- `android.permission.health.READ_RESPIRATORY_RATE`
- `android.permission.health.READ_BODY_TEMPERATURE`
- `android.permission.health.READ_VO2_MAX`
- `android.permission.health.READ_BLOOD_GLUCOSE`
- `android.permission.health.READ_SKIN_TEMPERATURE`

## Cycle Tracking Read Permissions

Cycle data is sensitive. These permissions are grouped separately in onboarding and Settings so users can grant or skip them explicitly:

- `android.permission.health.READ_MENSTRUATION`
- `android.permission.health.READ_OVULATION_TEST`
- `android.permission.health.READ_CERVICAL_MUCUS`
- `android.permission.health.READ_BASAL_BODY_TEMPERATURE`
- `android.permission.health.READ_INTERMENSTRUAL_BLEEDING`
- `android.permission.health.READ_SEXUAL_ACTIVITY`

## Health Connect Write Permissions

Declared for explicit save, edit/delete, recording, and supported import workflows. Write permissions should be requested only when a workflow needs them:

- `android.permission.health.WRITE_STEPS`
- `android.permission.health.WRITE_EXERCISE`
- `android.permission.health.WRITE_SLEEP`
- `android.permission.health.WRITE_EXERCISE_ROUTE`
- `android.permission.health.WRITE_DISTANCE`
- `android.permission.health.WRITE_ELEVATION_GAINED`
- `android.permission.health.WRITE_ACTIVE_CALORIES_BURNED`
- `android.permission.health.WRITE_TOTAL_CALORIES_BURNED`
- `android.permission.health.WRITE_HEART_RATE`
- `android.permission.health.WRITE_RESTING_HEART_RATE`
- `android.permission.health.WRITE_HEART_RATE_VARIABILITY`
- `android.permission.health.WRITE_WEIGHT`
- `android.permission.health.WRITE_HEIGHT`
- `android.permission.health.WRITE_BODY_FAT`
- `android.permission.health.WRITE_LEAN_BODY_MASS`
- `android.permission.health.WRITE_BASAL_METABOLIC_RATE`
- `android.permission.health.WRITE_BONE_MASS`
- `android.permission.health.WRITE_BODY_WATER_MASS`
- `android.permission.health.WRITE_FLOORS_CLIMBED`
- `android.permission.health.WRITE_WHEELCHAIR_PUSHES`
- `android.permission.health.WRITE_SPEED`
- `android.permission.health.WRITE_POWER`
- `android.permission.health.WRITE_STEPS_CADENCE`
- `android.permission.health.WRITE_CYCLING_PEDALING_CADENCE`
- `android.permission.health.WRITE_PLANNED_EXERCISE`
- `android.permission.health.WRITE_HYDRATION`
- `android.permission.health.WRITE_NUTRITION`
- `android.permission.health.WRITE_MINDFULNESS`
- `android.permission.health.WRITE_BLOOD_PRESSURE`
- `android.permission.health.WRITE_OXYGEN_SATURATION`
- `android.permission.health.WRITE_RESPIRATORY_RATE`
- `android.permission.health.WRITE_BODY_TEMPERATURE`
- `android.permission.health.WRITE_VO2_MAX`
- `android.permission.health.WRITE_BLOOD_GLUCOSE`
- `android.permission.health.WRITE_MENSTRUATION`
- `android.permission.health.WRITE_OVULATION_TEST`
- `android.permission.health.WRITE_CERVICAL_MUCUS`
- `android.permission.health.WRITE_BASAL_BODY_TEMPERATURE`
- `android.permission.health.WRITE_INTERMENSTRUAL_BLEEDING`
- `android.permission.health.WRITE_SEXUAL_ACTIVITY`

## Medical Records Permissions

Health Connect medical records are FHIR resources: vaccines, allergies, conditions, lab results, and more. These permissions exist only on Android 14 and newer with the Health Connect medical records feature. Elsewhere OpenVitals never asks for them.

They are asked for in one request, only inside the medical records area, and never together with fitness permissions. Health Connect then shows its own medical records permission screen. A screen with some categories declined still works: it lists the records OpenVitals wrote, which write access lets it read. Health Connect stops asking for a permission the user refused twice. The records home then leaves it out of the re-ask, and offers Health Connect's settings when nothing else is left.

- `android.permission.health.READ_MEDICAL_DATA_VACCINES`
- `android.permission.health.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES`
- `android.permission.health.READ_MEDICAL_DATA_CONDITIONS`
- `android.permission.health.READ_MEDICAL_DATA_MEDICATIONS`
- `android.permission.health.READ_MEDICAL_DATA_LABORATORY_RESULTS`
- `android.permission.health.READ_MEDICAL_DATA_PROCEDURES`
- `android.permission.health.READ_MEDICAL_DATA_VISITS`
- `android.permission.health.READ_MEDICAL_DATA_VITAL_SIGNS`
- `android.permission.health.READ_MEDICAL_DATA_PREGNANCY`
- `android.permission.health.READ_MEDICAL_DATA_SOCIAL_HISTORY`
- `android.permission.health.READ_MEDICAL_DATA_PERSONAL_DETAILS`
- `android.permission.health.READ_MEDICAL_DATA_PRACTITIONER_DETAILS`
- `android.permission.health.WRITE_MEDICAL_DATA`: used only for imports, manual entries, and phone-to-phone sync the user starts.

## Health Connect Access Modes

- `android.permission.health.READ_HEALTH_DATA_HISTORY`: used when the user grants access to older records.
- `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`: used where supported for background Health Connect reads.

## Android Runtime Permissions

- `android.permission.ACCESS_FINE_LOCATION`: required for reliable GPS activity recording.
- `android.permission.ACCESS_COARSE_LOCATION`: declared with location access for Android permission compatibility.
- `android.permission.ACTIVITY_RECOGNITION`: used where Android requires activity-recognition access for recorded activity workflows.
- `android.permission.HIGH_SAMPLING_RATE_SENSORS`: supports higher-rate sensor access for activity recording on devices that expose it.
- `android.permission.POST_NOTIFICATIONS`: used for activity recording, Apple Health import progress, watch sync progress, and reminder notifications.
- `android.permission.RECEIVE_BOOT_COMPLETED`: used to reschedule reminders after reboot or app update.
- `android.permission.CAMERA`: used only to scan the QR code of a SMART Health Card in the medical records import. Requested when the user taps Scan a QR code. Each frame is read on the device and dropped; none is saved or sent. A photo of the code can be picked instead.
- `android.permission.READ_CALENDAR`: used only to feed a paired Garmin watch's calendar glance. Requested when the per-watch "Calendar on watch" toggle (off by default) is switched on, read only while answering a watch that asked, and the events go to the watch over Bluetooth and nowhere else. There is no `INTERNET` permission to send them anywhere further.

## Bluetooth Permissions

OpenVitals uses Bluetooth for three separate things: Bluetooth LE sensors during activity recording, Garmin watches, and phone-to-phone sync. They share the same nearby-device permissions:

- `android.permission.BLUETOOTH_SCAN`: used to find Bluetooth LE sensors, to find a Garmin watch during pairing, to discover a nearby phone for sync, and to hear a bathroom scale's broadcasts. It is declared with `neverForLocation`, so OpenVitals does not derive location from Bluetooth scan results.
- `android.permission.BLUETOOTH_CONNECT`: used to connect to a Bluetooth LE sensor, to talk to a paired watch, and to open the sync connection to another phone.
- `android.permission.BLUETOOTH_ADVERTISE`: used only by phone-to-phone sync on Android 12 and newer, so this phone can be made discoverable while the other phone looks for it.
- `android.permission.BLUETOOTH` and `android.permission.BLUETOOTH_ADMIN`: declared for Android 11 and older only (`maxSdkVersion="30"`). Those versions need them to scan, to connect and to list bonded devices. There, a Bluetooth LE scan also needs the location permission, so the add-sensor and add-watch flows ask for it.

Nearby-device Bluetooth permissions never add internet access. Phone-to-phone sync uses Bluetooth Classic (RFCOMM) rather than Wi-Fi precisely because any Wi-Fi or TCP socket on Android would require the `INTERNET` permission, which OpenVitals does not declare.

## Companion Device Permissions

Garmin watch pairing uses Android's companion device manager. The association is what lets Android keep OpenVitals alive while the watch is in range, so a file sync that takes minutes is not killed halfway through. A bathroom scale can be associated too, so that Android wakes OpenVitals when someone steps on it:

- `android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND`
- `android.permission.REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE`
- `android.permission.REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND`: lets the app start the scale's short listening service when Android reports the associated scale awake. It does nothing without the association.

None shows a permission prompt of its own. The consent is the system dialog that asks whether OpenVitals may access the selected watch or scale. Declining it is supported: the watch is still bonded and still syncs, only without the background priority boost; the scale is heard while the app is open.

The manifest also declares the `android.software.companion_device_setup` feature as not required, so the app stays installable on devices without companion support. `android.permission.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND` is deliberately not declared, because it governs background network use and OpenVitals has no network access at all.

The app declares one companion service, `.devices.core.pairing.OpenVitalsCompanionDeviceService`, which Android binds while an associated device is in range. The binding raises the app's process priority during a sync, and the service passes "device appeared" on to the parts that act on it: with "Stay connected" switched on the held link to a watch returns promptly instead of waiting out a retry timer, and a bathroom scale waking up starts the short listening service described under Foreground Service Permissions.

## Notification Access

Notification access is optional and used for two things: forwarding phone notifications to a paired Garmin watch, and showing the phone's music player on that watch. Each is its own switch, off by default.

- The app declares `.devices.notifications.OpenVitalsNotificationListenerService`, protected by the system-only `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`.
- There is no runtime prompt. Android grants notification access from its own settings screen, and OpenVitals shows a prominent disclosure before sending you there. Each feature's disclosure says what that feature reads.
- The feature stays dormant until the user grants access, and it can be turned off in OpenVitals or revoked in Android settings at any time.
- Notification content is read on the device, held in a bounded in-memory buffer, and sent only to the paired watch over Bluetooth. It is not written to a file or a database, and the app has no internet permission.
- Settings, Watches, Notifications includes a per-app list so individual apps can be stopped from reaching the watch.
- Music controls read the phone's media sessions, which Android shows only to an app with this grant: the player's name, the track, artist and album, and the playback position. They go only to the paired watch and are never stored. Granting access for music alone forwards no notifications: forwarding stays off until its own switch is on.

## Foreground Service Permissions

- `android.permission.FOREGROUND_SERVICE`: base permission for foreground work.
- `android.permission.WAKE_LOCK`: keeps the CPU running during an activity recording while the screen is off, so timers and sensors keep time. Held while recording and during a timed rest, released on pause and at the end. Android grants it without a prompt.
- `android.permission.FOREGROUND_SERVICE_LOCATION`: marks the recording service as location-based.
- `android.permission.FOREGROUND_SERVICE_HEALTH`: marks the recording service as health-related where Android supports it.
- `android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE`: used by activity recording with connected Bluetooth LE sensors, by the keep-alive service that runs during a phone-to-phone sync transfer, and by the scale's listening service, which runs for at most 45 seconds after Android reports the associated scale awake, because the scale's result is on the air for two seconds and a background scan would miss it.
- `android.permission.FOREGROUND_SERVICE_DATA_SYNC`: marks long-running Apple Health imports as user-started data sync work.

OpenVitals treats the foreground slot as effectively single. Activity recording, an Apple Health import, and a phone-to-phone sync contend for it, so the app does not run them at the same time. The scale's listening service is the one the app starts without a tap, and it starts nothing while another already holds the slot.

## Removed Network Permissions

The manifest explicitly removes inherited network permissions from dependencies:

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.ACCESS_WIFI_STATE`

The `android.hardware.wifi` feature is removed the same way.

These removals preserve the local app's internet-free boundary, and every device feature added since is built to keep it: watch sync and notification forwarding run over Bluetooth to the watch, and phone-to-phone sync runs over Bluetooth Classic.

The app also queries the launcher for installed apps, which is what the watch-notification per-app list uses. It deliberately does not request `QUERY_ALL_PACKAGES`.

## File And Route Intents

OpenVitals can receive GPX, KML, KMZ, FIT, and TCX files through Android open/share intents so imported activities can be reviewed and saved to Health Connect. It can also import PMTiles and Mapsforge map packs from Settings for offline activity maps.

The app uses a local file provider to export route files, such as GPX or KMZ, to other apps.
