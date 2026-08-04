# Watches

> **Status:** Current implemented behavior. Experimental.
> **Audience:** Users and contributors.
> **Implementation:** `devices/core`, `devices/garmin`, `devices/wearos`, `devices/notifications`, `features/watches`.
> **Navigation:** `Screen.SettingsWatches`, `Screen.WatchDevice`, `Screen.WatchData`, `Screen.WatchNotifications`, `Screen.WatchSettings`; settings section `WATCHES`.
> **Related:** [Feature map](feature-map.md), [Bluetooth LE sensors](ble-sensors.md), [FIT files import](fit-files-import.md), [Body Energy](body-energy.md), [Permissions](../app/permissions.md), [Privacy](../app/privacy.md).

OpenVitals has experimental support for wrist devices. Settings, Watches pairs a watch and copies what it recorded onto the phone over Bluetooth. There is no watch-vendor account and no network step; the app declares no internet permission.

Support differs sharply by make:

- **Garmin** watches are read over Garmin's own Bluetooth protocol. Sync, the watch-only data screen, notification forwarding, the watch's settings tree, and find-my-watch are all Garmin features.
- **WearOS** watches (and other smartwatches recognized by name) can be registered so OpenVitals knows they exist, but nothing above applies to them. Their recorded data reaches the app through Health Connect, and their live heart rate through standard Bluetooth LE, exactly as before.

## Experimental Status

Watch support is developed and verified against a single Garmin model. It is offered in the same spirit as [Bluetooth LE sensors](ble-sensors.md): useful, honest about its limits, and not a substitute for the vendor's own app.

- The protocol is not model-specific, and OpenVitals asks each watch what it can do rather than assuming. A watch that does not report find-my-watch gets no Find button; one without a settings tree gets no settings row.
- **Watches using the older single-link Garmin transport are not supported.** Pairing probes the watch and warns when it cannot recognize the connection at all. A watch that pairs but turns out to use the older transport reports an error the first time it is asked to sync.
- A file type OpenVitals does not understand is skipped rather than failing the sync.
- Sync is always something the user asked for. There is no background or scheduled sync.

## Pairing A Watch

Settings, Watches shows the paired watches, or "No watch paired" with a Pair a watch button.

1. OpenVitals asks for nearby-device Bluetooth permission where Android requires it, then scans. The watch must be awake and close to the phone. Watches already bonded with the phone appear in the list even when they advertise nothing useful.
2. Picking a Garmin watch runs a short checklist: pairing with the watch, asking Android for access, and checking what the watch supports.
3. Android shows its own pairing dialog. The code shown on the watch has to be confirmed.
4. Android then asks separately whether OpenVitals may access this watch. This step is optional. Allowing it lets Android keep OpenVitals alive while the watch is nearby, so a sync that takes minutes is not interrupted. Declining still pairs the watch, and syncing then works while OpenVitals is open.

A watch can be renamed, switched off without unpairing, or removed. Removing it unpairs the watch and forgets which files were already copied, so a future pairing starts fresh. Data already written to Health Connect is kept.

A Garmin Edge bike computer is recognized as a bike computer rather than a watch. It gets a Live sensor section instead of the watch data screen, because broadcast mode is normally only on during a ride.

## Syncing

Tapping a paired Garmin watch opens its device screen, which has a Sync action. The watches list itself shows each watch's last sync time.

The watch hands over the files it recorded since last time and OpenVitals imports them. Each file is saved on the phone before the watch is told it may archive it, and a file is only marked as synced once its import succeeded, so a run that fails partway re-fetches rather than skipping data that never landed.

The device screen shows whether the watch has ever been synced and when it last was. A link dropped mid-sync is not treated as a failure: whatever arrived is kept.

### Recorded Activities

Activity files go through the same importer a hand-picked folder of FIT files uses, so imports are batched and a single bad file does not stop the rest. See [FIT files import](fit-files-import.md).

Activities are written to Health Connect as exercise sessions with their routes and series, and appear on the normal activity screens alongside data from any other source. A file is skipped when the Health Connect write permission it needs is missing.

### Wellness Data

Wellness files are split by where the data belongs.

Written to Health Connect, and therefore visible on the usual dashboard and detail screens:

- Sleep sessions with stages, and naps.
- Heart rate series and resting heart rate.
- Heart rate variability.
- Respiratory rate.
- VO2 max.
- Blood oxygen and respiratory rate from a Health Snapshot recording.
- Basal metabolic rate.
- Steps, distance, and active calories through the day.

Kept in OpenVitals' own storage, because Health Connect has no record type for them: stress, Body Battery, intensity minutes, recovery time, training readiness, training load, and the watch's own verdict on a night's sleep.

Syncing the same day twice does not double anything. Files already imported are not downloaded again, Health Connect records carry a stable identifier so a record that does arrive twice updates in place, and watch-only measurements are keyed on the measurement and its instant.

## Watch Data Screen

The Data action on a Garmin watch opens the watch-only measurements: the things the watch makes that Health Connect has no place for. Everything else goes to Health Connect and is not repeated here.

Today:

- Stress, with today's average.
- Body Battery, with today's highest value. This also feeds the app's own Body Energy calibration; see [Body Energy](body-energy.md).
- Intensity minutes, counting vigorous minutes double, against the 150-minute weekly target. The week starts on Monday.

Last night:

- Sleep score.
- Time awake.
- Times woken.
- Sleep need, compared with the usual nightly need when the watch reported one.

Training:

- Recovery time.
- Training readiness.
- Training load, showing the acute value with the chronic value beside it.

A footer names the measurements this watch did not send, so an absent row reads as "your watch does not report this" rather than "the app lost it". Until a first sync the screen is empty and says so.

## Notifications On The Watch

Phone notifications can be forwarded to a paired Garmin watch that supports it. The feature is off by default and does nothing at all until it is switched on: no reading, no Bluetooth link, no background work.

1. Open the watch, then Notifications, and turn on "Send notifications to the watch".
2. OpenVitals shows a disclosure explaining exactly what it will read and where it goes, and asks for confirmation.
3. It then opens Android's notification access screen. Access can only be granted there; OpenVitals cannot request it from inside the app.
4. Coming back to OpenVitals, the switch turns itself on once access has been granted.

### The App Blocklist

"Apps to silence" lists the apps installed on the phone. Everything sends to the watch until an app is switched off, so the list is a blocklist rather than an allow-list.

Some notifications never reach the watch regardless: ongoing and foreground-service notifications, group summaries, notifications an app marks as local to the phone, notifications with neither a title nor a body, minimum-importance channels, anything at all while the phone's own Do Not Disturb is on, and OpenVitals' own notifications.

### Acting From The Wrist

Dismissing on the watch clears the notification from the phone. Where the posting app publishes them, a reply action and up to five of the app's own buttons are offered. Actions that would merely open a screen on the phone are not offered, because Android does not let a background app launch them; the button would report success and do nothing.

### The Link

While forwarding is on, the link to the watch is held open for as long as the watch is in range, which is how Garmin watches expect a phone to behave. If the watch goes out of range the link is re-established with a backoff, and anything that arrived while it was away is delivered when it returns.

## Settings On The Watch

A Garmin watch that reports a settings tree gets a "Settings on the watch" row, plus a direct Alarms action.

None of these menus are built into OpenVitals. The watch sends its own menu — screens, rows, choices, and current values, already in the language the watch is set to — and the app renders what arrives. A screen OpenVitals has never seen still works, and nothing needs updating when the watch's firmware changes.

Switches, option lists, times, and sub-screens can be changed, and every change applies to the watch itself and is read back to confirm it. Rows the phone cannot act on are shown rather than hidden, because seeing a greyed row says the watch has the setting and the app cannot reach it, which is true.

When the watch cannot be reached, refuses a change, or does not answer, the screen says which of those happened instead of claiming the change worked.

## Find My Watch

A watch that reports the capability gets a Find action. It makes the watch alert so it can be located, for about a minute by default, and the same button stops it early. If the watch never answers, the screen says so and suggests bringing it closer.

## One Radio At A Time

Sync, find, settings on the watch, and notification forwarding all speak to the same watch over the same Bluetooth link, and only one of them can hold it.

- A user-initiated action asks for the link and waits a few seconds for whatever holds it to let go. Notification forwarding, the usual holder, gives it up on its next check and resumes afterwards.
- If the link cannot be taken in time, the action reports that the watch is busy and suggests trying again in a moment.
- Sync, Alarms, and Find are disabled while a sync or a find is already running.
- A live activity recording blocks a watch sync outright. The recording has to be finished or discarded first.

Different devices do not contend with each other, so a Bluetooth LE sensor is unaffected by what a watch is doing.

## Privacy

Nothing leaves the phone. The watch is read over Bluetooth, the files are parsed on the device, and the results go to Health Connect or to OpenVitals' own local database.

Notification text is read on the device, held in memory only while it is needed, and sent only to the paired watch. It is never written to a file or a database. Turning the feature off, or revoking notification access in Android settings, stops it immediately.

See [Privacy](../app/privacy.md) and [Permissions](../app/permissions.md) for the full boundary, including why the companion association is asked for and why it is optional.

## Known Limitations

- Verified end to end against one Garmin model. Other recent models are expected to work, but are untested.
- Older single-link transport watches cannot sync.
- There is no background sync. Every sync is one the user asked for.
- The Connected and Not connected labels reflect whether the watch is switched on in OpenVitals, not whether a Bluetooth link is open right now.
- WearOS watches are registered only. Sync, watch data, notification forwarding, watch settings, and find are Garmin-only.
- Health Snapshot values only exist if a Health Snapshot has been recorded on the watch.
- Battery level and charging state are not read from a Garmin watch.
