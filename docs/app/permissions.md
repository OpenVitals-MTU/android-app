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

## Bluetooth Permissions

OpenVitals uses Bluetooth for two separate things: Bluetooth LE sensors during activity recording, and phone-to-phone sync. They share the same nearby-device permissions:

- `android.permission.BLUETOOTH_SCAN`: used to find Bluetooth LE sensors and to discover a nearby phone for sync. It is declared with `neverForLocation`, so OpenVitals does not derive location from Bluetooth scan results.
- `android.permission.BLUETOOTH_CONNECT`: used to connect to a Bluetooth LE sensor and to open the sync connection to another phone.
- `android.permission.BLUETOOTH_ADVERTISE`: used only by phone-to-phone sync on Android 12 and newer, so this phone can be made discoverable while the other phone looks for it.

Nearby-device Bluetooth permissions never add internet access. Phone-to-phone sync uses Bluetooth Classic (RFCOMM) rather than Wi-Fi precisely because any Wi-Fi or TCP socket on Android would require the `INTERNET` permission, which OpenVitals does not declare.

## Watches

OpenVitals requests no watch-related permissions. The app does not link to watches directly: watch data arrives through Health Connect, written by a companion app such as Gadgetbridge, and OpenVitals reads it with its ordinary Health Connect permissions. Earlier releases declared companion-device and notification-access machinery for a direct Garmin integration; both were removed with it.

## Foreground Service Permissions

- `android.permission.FOREGROUND_SERVICE`: base permission for foreground work.
- `android.permission.FOREGROUND_SERVICE_LOCATION`: marks the recording service as location-based.
- `android.permission.FOREGROUND_SERVICE_HEALTH`: marks the recording service as health-related where Android supports it.
- `android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE`: used by activity recording with connected Bluetooth LE sensors and by the keep-alive service that runs during a phone-to-phone sync transfer.
- `android.permission.FOREGROUND_SERVICE_DATA_SYNC`: marks long-running Apple Health imports as user-started data sync work.

OpenVitals treats the foreground slot as effectively single. Activity recording, an Apple Health import, and a phone-to-phone sync contend for it, so the app does not run them at the same time.

## Removed Network Permissions

The manifest explicitly removes inherited network permissions from dependencies:

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.ACCESS_WIFI_STATE`

The `android.hardware.wifi` feature is removed the same way.

These removals preserve the local app's internet-free boundary, and every device feature added since is built to keep it: live BLE sensors and phone-to-phone sync run over Bluetooth.

## File And Route Intents

OpenVitals can receive GPX, KML, KMZ, FIT, and TCX files through Android open/share intents so imported activities can be reviewed and saved to Health Connect. It can also import PMTiles and Mapsforge map packs from Settings for offline activity maps.

The app uses a local file provider to export route files, such as GPX or KMZ, to other apps.
