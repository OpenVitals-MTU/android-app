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
- Offline maps are imported as local map packs; nothing is downloaded.
- Diagnostics and import reports are generated on device and shared only if the user chooses to.

## Paired Devices

### Watches

OpenVitals does not link to watches directly and reads no notifications. Watch data arrives through Health Connect, written by a companion app such as Gadgetbridge, and OpenVitals reads it with its ordinary Health Connect permissions. A watch broadcasting standard Bluetooth LE heart rate can be added as a live sensor for activity recording; that connection is local and streams only the sensor values.

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
