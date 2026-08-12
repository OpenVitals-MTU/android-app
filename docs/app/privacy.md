# Privacy

OpenVitals is designed as a local-first app. The local Android app is intentionally separate from any connected app work.

The local app:

- Does not ship app-level internet permission.
- Does not create OpenVitals cloud accounts.
- Does not upload health data to an OpenVitals server.
- Does not include ads.
- Does not include an analytics SDK.
- Reads supported records through Health Connect.
- Writes supported records only after an explicit save, import, record, edit, or delete action.

Local app preferences may include onboarding state, acknowledged permission prompts, unit settings, language, theme, widget order, goals, reminders, Body Energy calibration, timer settings, paired device settings, and display choices.

## No Internet Permission

The manifest removes `INTERNET`, `ACCESS_NETWORK_STATE`, and `ACCESS_WIFI_STATE` from the merged manifest, so no library can quietly reintroduce network access. Android itself then makes network use impossible, which is stronger than a promise not to use it.

Every device feature is built around keeping that true:

- Phone-to-phone sync uses Bluetooth Classic (RFCOMM), not Wi-Fi. A Wi-Fi or TCP transfer would require `INTERNET`; Bluetooth keeps it a direct link between the two phones.
- Watch sync and notification forwarding talk to the watch over Bluetooth only.
- Offline maps are imported as local map packs; nothing is downloaded.
- Diagnostics and import reports are generated on device and shared only if the user chooses to.

## Paired Devices

### Garmin Watches

A paired watch's recorded files are read over Bluetooth and imported on the device. Wellness series a watch records but Health Connect has no type for, such as stress and Body Battery, are stored in the app's own local database; the rest is written to Health Connect. Nothing about a watch is sent off the phone.

Pairing uses Android's companion device manager. The association exists so Android keeps OpenVitals running while the watch is in range and a multi-minute sync can finish.

### Notification Forwarding

If, and only if, the user grants Android notification access, OpenVitals reads incoming phone notifications so they can be shown on a paired watch. OpenVitals shows a prominent disclosure explaining this before sending the user to the Android settings screen that grants it.

- The content is used for one purpose: displaying the notification on the paired watch.
- It is held in a bounded in-memory buffer. It is not written to a file, a database, or Health Connect.
- It is sent to nothing but the paired watch, over Bluetooth. The app has no internet permission, so there is no other route out.
- Individual apps can be blocked from reaching the watch, the feature can be switched off in OpenVitals, and access can be revoked from Android settings at any time.

### Calendar On The Watch

If, and only if, the per-watch "Calendar on watch" toggle is switched on (off by default; enabling it asks for Android's calendar permission), OpenVitals reads the phone's calendar to answer the watch's calendar glance.

- Events are read only while answering a watch that asked, and only within the window the watch named.
- They are held in memory only; nothing is written to a file, a database, or Health Connect.
- They are sent to nothing but the paired watch, over Bluetooth. The app has no internet permission, so there is no other route out.
- Declined and cancelled meetings are not sent.

### Weather And Ephemeris

Weather shown on the watch comes from a weather app on the phone that broadcasts it (such as Breezy Weather); OpenVitals itself never contacts a weather service. GPS ephemeris files are imported by hand and handed to the watch when it asks; nothing is downloaded. Both go to the watch and nowhere else.

### Phone-To-Phone Sync

Sync copies Health Connect records from one phone to another over a paired, encrypted Bluetooth Classic link. One phone is made discoverable and shows a six-digit code, the other scans for it and types that code in, so the connection is confirmed as the intended device. The user then chooses how far back to sync and which data categories to accept. No server or account is involved at any point.

## Health Records

Health Connect is the source of truth. OpenVitals reads Health Connect records to show dashboard summaries, detail screens, readiness, statistics, achievements, and insights.

Manual entries are saved back to Health Connect only when the user chooses to save them. Supported Apple Health export records are written to Health Connect only when the user imports an export file from Settings. OpenVitals-created records can be edited later; records created by other apps remain read-only.

Apple Health exports are copied into app-private local storage and analyzed there before the user chooses which categories to import. The staged compressed copy is reused for the pending background import, cleared after a successful import or analysis failure, replaced by a newly selected export, and removed with the app's private data. Import reports are generated locally when the user runs an import and chooses to copy or download the report. These troubleshooting reports intentionally include full importer logs, selected categories, diagnostics, and exception details, so users should review them before sharing outside their device.

## Sensitive Data

Cycle tracking uses sensitive Health Connect records and is shown only after cycle permissions are granted explicitly.

Workout route previews require manual Health Connect approval in some cases. GPS recording requires location permission because OpenVitals needs location points to build the route. Imported PMTiles or Mapsforge map packs stay local on the device.

## Permission Details

See [Permissions](permissions.md) for the current permission list and why each group is requested.

See [Local And Connected Editions](local-and-connected-editions.md) for the boundary between the local app and planned connected work.
