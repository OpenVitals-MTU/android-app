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
- `android.permission.POST_NOTIFICATIONS`: used for activity recording, Apple Health import progress, phone-to-phone sync progress, and reminder notifications.
- `android.permission.RECEIVE_BOOT_COMPLETED`: used to reschedule reminders after reboot or app update.
- `android.permission.CAMERA`: used only to scan the QR code of a SMART Health Card in the medical records import. Requested when the user taps Scan a QR code. Each frame is read on the device and dropped; none is saved or sent. A photo of the code can be picked instead.

## Bluetooth Permissions

OpenVitals uses Bluetooth for two separate things: Bluetooth LE sensors during activity recording and phone-to-phone sync. They share the same nearby-device permissions:

- `android.permission.BLUETOOTH_SCAN`: used to find Bluetooth LE sensors and to discover a nearby phone for sync. It is declared with `neverForLocation`, so OpenVitals does not derive location from Bluetooth scan results.
- `android.permission.BLUETOOTH_CONNECT`: used to connect to a Bluetooth LE sensor and to open the sync connection to another phone.
- `android.permission.BLUETOOTH_ADVERTISE`: used only by phone-to-phone sync on Android 12 and newer, so this phone can be made discoverable while the other phone looks for it.
- `android.permission.BLUETOOTH` and `android.permission.BLUETOOTH_ADMIN`: declared for Android 11 and older only (`maxSdkVersion="30"`). Those versions need them to scan, to connect and to list bonded devices. There, a Bluetooth LE scan also needs the location permission, so the add-sensor flow asks for it.

Nearby-device Bluetooth permissions never add internet access. Phone-to-phone sync uses Bluetooth Classic (RFCOMM) rather than Wi-Fi precisely because any Wi-Fi or TCP socket on Android would require the `INTERNET` permission, which OpenVitals does not declare.

## Foreground Service Permissions

- `android.permission.FOREGROUND_SERVICE`: base permission for foreground work.
- `android.permission.WAKE_LOCK`: keeps the CPU running during an activity recording while the screen is off, so timers and sensors keep time. Held while recording and during a timed rest, released on pause and at the end. Android grants it without a prompt.
- `android.permission.FOREGROUND_SERVICE_LOCATION`: marks the recording service as location-based.
- `android.permission.FOREGROUND_SERVICE_HEALTH`: marks the recording service as health-related where Android supports it.
- `android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE`: used by activity recording with connected Bluetooth LE sensors and by the keep-alive service that runs during a phone-to-phone sync transfer.
- `android.permission.FOREGROUND_SERVICE_DATA_SYNC`: marks long-running Apple Health imports as user-started data sync work.

OpenVitals treats the foreground slot as effectively single. Activity recording, an Apple Health import, and a phone-to-phone sync contend for it, so the app does not run them at the same time. No service starts without a tap.

## Removed Network Permissions

The manifest explicitly removes inherited network permissions from dependencies:

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.ACCESS_WIFI_STATE`

The `android.hardware.wifi` feature is removed the same way.

These removals preserve the local app's internet-free boundary, and every device feature added since is built to keep it: phone-to-phone sync runs over Bluetooth Classic. Watches and scales are handled by the separate [OpenWatch](https://github.com/OpenVitals-MTU/OpenWatch) app, so OpenVitals declares no companion-device, notification-access or calendar permission. It deliberately does not request `QUERY_ALL_PACKAGES`.

## File And Route Intents

OpenVitals can receive GPX, KML, KMZ, FIT, and TCX files through Android open/share intents so imported activities can be reviewed and saved to Health Connect. It can also import PMTiles and Mapsforge map packs from Settings for offline activity maps.

The app uses a local file provider to export route files, such as GPX or KMZ, to other apps.
