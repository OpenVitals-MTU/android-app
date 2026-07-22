# Garmin Watch Sync

> **Status:** Current implemented behavior. Experimental — developed and verified against a vívoactive 5.
> **Audience:** Users and contributors.
> **Implementation:** `lib/data/source/sensors/garmin/` (the whole GFDI stack), `lib/features/settings/presentation/watch_device_screen.dart` + `watch_data_screen.dart` + `watch_settings_screen.dart` + `watch_common.dart`, `lib/features/settings/application/garmin_sync_view_model.dart` + `watch_settings_view_model.dart` + `watch_metrics_view_model.dart`, `lib/domain/usecase/onboard_garmin_watch_use_case.dart`, `lib/features/dashboard/presentation/watch_summary_tile.dart`, `lib/features/imports/fit/fit_wellness_import.dart`, `packages/bluetooth_sync_native/` (`CompanionDevices.kt`).
> **Navigation:** `/settings/watches`, `/watch/:watchDeviceId`, `/watch/:watchDeviceId/data`, `/watch/:watchDeviceId/settings/:watchScreenId`.
> **Related:** [Feature map](feature-map.md), [Bluetooth LE sensors](ble-sensors.md), [FIT files import](fit-files-import.md), [Permissions](../app/permissions.md), [Garmin FIT files](../reference/garmin-fit-files.md), [Garmin settings service](../reference/garmin-settings-service.md).

OpenVitals copies what a Garmin watch recorded directly off the watch, over Bluetooth. There is no Garmin account, no Garmin Connect, and no network step: the app ships no `INTERNET` permission at all, and the watch is read over GFDI — Garmin's own Bluetooth protocol — using the same FIT files the watch would otherwise hand to Garmin's app.

Alarms and the watch's other settings can also be read and changed from the phone.

## How to use it

### Pair a watch

1. Go to **Settings › Watches**. With none paired you'll see **"No watch paired"** and a **Pair a watch** button.
2. Tap **Pair a watch**. OpenVitals asks for **Nearby devices** (Bluetooth) permission, then scans. Wake the watch and keep it close.
3. Pick your watch. Android shows its own **pairing** dialog, then a **companion device** dialog asking to let OpenVitals access the watch.
4. Name the watch if you like, and save.

Declining the companion dialog is fine — the watch still pairs and still syncs. What the association buys is a priority boost from Android while the watch is nearby, which makes a long sync less likely to be killed. See [Permissions](../app/permissions.md).

### Sync

Tap the watch to open its **device view**, then **Sync**. The watch hands over the files it has recorded since last time, and OpenVitals imports them.

A sync usually takes a second or two. The same **Sync** button is on the watch's tile on the summary screen, so a sync never needs more than one tap from the home screen.

Nothing is deleted from the watch until the phone has safely stored a copy.

### See what came across

**Data**, in the device view, shows everything the watch recorded that Health Connect has no place for — stress, Body Battery, intensity minutes, training readiness, recovery time, training load, and the watch's own verdict on last night's sleep. Everything else goes to Health Connect and shows up on the normal dashboard and metric screens alongside data from any other source.

### Alarms and settings

- **Alarms** opens the watch's alarm list. Alarms can be switched on and off, retimed, relabelled, added and deleted, and every change applies to the watch itself.
- **Settings on the watch** opens the watch's own settings menu — Clocks, Notifications, Sensors, Music, System and the rest — read live from the wrist.
- **Find** makes the watch alert so you can locate it. It rings for a minute; the same button stops it early.

None of these menus are built into OpenVitals. The watch sends its menu — screens, rows, choices and current values, already in the language the watch is set to — and the app renders what arrives. That means a screen OpenVitals has never seen still works, and nothing needs updating when the watch's firmware changes.

Rows the phone cannot act on are shown greyed rather than hidden — a setting that opens something on the watch (Garmin Pay), or that the watch marks as unavailable to a phone. Seeing it greyed says the watch has it and the app cannot reach it, which is true; hiding it would suggest the watch does not have it.

## What Gets Imported

### Into Health Connect

Sleep sessions and stages, heart rate, resting heart rate, heart rate variability, VO2 max, SpO2, respiratory rate, steps, distance, active calories, and basal metabolic rate. Recorded activities import as workouts with their routes.

Files go through the *same* importer a hand-picked folder of FIT files uses ([FIT files import](fit-files-import.md)), so imports are batched, tolerate a single bad file, and respect Health Connect's write quota.

### Into the app's own storage

Health Connect has no record type for these, so OpenVitals stores them itself:

| Measure | Notes |
|---|---|
| Stress | Garmin's 0–100 scale, through the day. |
| Body Battery | 0–100. Also teaches the app's own Body Energy calibration — see [Body Energy](body-energy.md). |
| Intensity minutes | Moderate and vigorous, as running daily totals. |
| Recovery time | Minutes until the watch considers you recovered. |
| Training readiness | 0–100. |
| Training load | Acute and chronic, on Garmin's own unitless scale. |
| Sleep score, awakenings, time awake, sleep pressure | The watch's own verdict on a night. |
| Sleep need | The usual nightly need, and what the night's strain called for. |
| Health Snapshot | SpO2, stress, respiration and Body Battery from a snapshot recording. Only present if a Health Snapshot has been run on the watch. |

## Repeat Syncs Are Safe

Syncing the same day twice does not double anything.

- Files already imported are skipped, so a re-sync does not re-download them.
- Every Health Connect record carries a stable `clientRecordId`, so a record that does arrive twice updates in place instead of duplicating.
- Watch-only measures are keyed on `(measure, instant)` in the app's database, which has the same effect.

A file is only marked as synced *after* its import succeeded, so a run that fails partway re-fetches rather than skipping data that never landed.

## Supported Watches

Developed against a **vívoactive 5** on firmware 17.05, which is the only model verified end to end.

The protocol is not model-specific, and the app asks each watch what it can do rather than assuming: the capability list the watch sends decides which actions appear. A watch that does not report **Find my watch** does not get a Find button; one without a settings tree does not get a settings row. Other recent Garmin models are expected to work, and the file sync degrades gracefully — a file type OpenVitals does not understand is skipped, not failed.

Watches using the older single-link GFDI transport are **not** supported. The app detects the transport during pairing and says so rather than pairing something it cannot read.

## Privacy

Nothing leaves the phone. The watch is read over Bluetooth, the files are parsed on-device, and the results go to Health Connect or the app's own database. OpenVitals has no `INTERNET` permission, so it could not send this anywhere even if it tried. See [Privacy](../app/privacy.md).

Neither the watch's Bluetooth address nor the contents of what it recorded are written to Android's system log in a release build.

## Known Limitations

- **Sleep stage durations disagree with the watch.** The stages OpenVitals derives from the file do not add up to the watch's own figure for time awake — a night where the watch reported 3 minutes came out as 59. The sleep session and its stages still import; treat the *awake* total as unreliable for now. The watch's own figure is stored separately and shown on the **Data** screen.
- **Number settings are read-only.** The watch sends a number's current value but not its allowed range, so OpenVitals shows the value rather than offering a picker that might propose a number the watch refuses.
- **Health Snapshot data only exists if you record one** on the watch.
- **Delete is verified on a vívoactive 5.** Deleting an alarm uses an encoding confirmed on that model. A watch that disagrees refuses the change, and OpenVitals reports the refusal rather than claiming it worked.
- **Background sync is not implemented.** Every sync is one the user asked for.

## For Contributors

The protocol stack lives in `lib/data/source/sensors/garmin/` and is **transport-free by construction**: `GarminSession` consumes decoded GFDI frames and emits frames to send, so the entire protocol — handshake, directory listing, file download, protobuf, the settings service — is exercised over an in-memory pipe with no Bluetooth. Only `garmin_ble_transport.dart` touches `flutter_blue_plus`.

Two reference documents record what the watch actually does, as opposed to what the schema claims:

- [Garmin FIT files](../reference/garmin-fit-files.md) — file types and how each maps to Health Connect.
- [Garmin settings service](../reference/garmin-settings-service.md) — the protobuf settings protocol, the tree as measured, and the traps met getting there.

Both were written from bytes a real watch sent. Where Gadgetbridge's schema and the watch disagreed, the watch was right every time — including against Garmin's own FIT profile, which documents `awake_duration` in minutes where the watch sends seconds. Test fixtures are rebuilt from captured replies for the same reason.

Ported files from [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge) (AGPLv3, the same licence as this app) name their origin in the file header.
