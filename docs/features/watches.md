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
- Sync happens by hand, or on a schedule the user chose per watch. Nothing syncs until one of those two says so.

## Pairing A Watch

Settings, Watches shows the paired watches, or "No watch paired" with a Pair a watch button.

1. OpenVitals asks for nearby-device Bluetooth permission where Android requires it, then scans. The watch must be awake and close to the phone. Watches already bonded with the phone appear in the list even when they advertise nothing useful.
2. Picking a Garmin watch runs a short checklist: pairing with the watch, asking Android for access, and checking what the watch supports.
3. Android shows its own pairing dialog. The code shown on the watch has to be confirmed.
4. Android then asks separately whether OpenVitals may access this watch. This step is optional. Allowing it lets Android keep OpenVitals alive while the watch is nearby, so a sync that takes minutes is not interrupted. Declining still pairs the watch, and syncing then works while OpenVitals is open.

The path taken decides what the device becomes. A device added through Settings, Watches is a watch; one added through the Bluetooth LE sensors flow is a live sensor, even when it is physically a smartwatch. The name no longer decides: a Garmin watch added as a sensor behaves exactly like a heart-rate strap.

After pairing, a checklist offers the OS-level permissions a watch benefits from: notification access for forwarding, and exemption from battery optimization so a held link survives the night. Each row explains itself and opens the right system screen; all of them can be declined and granted later.

A watch can be renamed, switched off without unpairing, or removed. Removing it unpairs the watch and forgets which files were already copied, so a future pairing starts fresh. Data already written to Health Connect is kept.

A Garmin Edge bike computer is recognized as a bike computer rather than a watch. It gets a Live sensor section instead of the watch data screen, because broadcast mode is normally only on during a ride.

## Syncing

Tapping a paired Garmin watch opens its device screen, which has a Sync action. The watches list itself shows each watch's last sync time.

The watch hands over the files it recorded since last time and OpenVitals imports them. Each file is saved on the phone before the watch is told it may archive it, and a file is only marked as synced once its import succeeded, so a run that fails partway re-fetches rather than skipping data that never landed.

The device screen shows whether the watch has ever been synced and when it last was. A link dropped mid-sync is not treated as a failure: whatever arrived is kept.

The dashboard carries a watch tile showing the most recently synced watch with its battery, last sync time, and a sync button, so a sync does not require a trip through Settings. While live readings are streaming (see below), the tile shows the current heart rate and step count instead of the last sync time.

### Automatic Sync

"Automatic sync", on the watch's device screen, syncs the watch on its own every 30 minutes, hour, or two hours. Off by default, and set per watch, so two paired watches can be on different schedules or only one of them on a schedule at all.

A scheduled run does exactly what tapping Sync does, with three differences:

- It is quiet. A run that could not reach the watch leaves no error on screen; the last sync time on the device screen and the watches list is what says whether the schedule is working. A watch out of range at 3am is not a fault to report.
- It does not linger on the link afterwards. A manual sync holds the connection open a few extra seconds so the watch can run its own errands, notably fetching weather; a scheduled one hangs up as soon as the files are in, because that time costs radio on both sides.
- It refuses rather than queues. If the radio is busy, an activity recording is in progress, or a sync is already running, the run steps aside and waits for the next one. It retries a couple of times on a short backoff first, since a watch that just walked out of range is usually back within minutes.

The exact moment is Android's to choose, not the app's. The schedule is a floor, not an alarm: a run can arrive late, and the phone being in Doze, below its low-battery mark, or out of range of the watch all delay it. Granting the battery-optimization exemption the pairing checklist offers is what keeps overnight runs close to their schedule. The schedule survives a reboot and an app update without being re-armed.

One thing does wait for the app to be opened. Body Energy is recomputed from Health Connect after a sync, and reading Health Connect in the background needs its own grant, so on a phone where that grant is missing a scheduled sync lands the data and leaves the recomputation to the next time the app is open. Nothing is lost either way.

Nothing about this changes what is written or where. The same importer runs, the same watermarks apply, and syncing the same day twice still does not double anything.

If "Stay connected" is also on, the held link is given up for the duration of the run and re-established afterwards, the same handover a manual sync uses.

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

#### The step, distance and calorie counters

These three are the awkward ones. The watch reports them as running daily
totals, so a file only says where the counter stood, never what happened. They
are accumulated across every file of a sync and differenced once at the end
against a stored watermark, which is what lets an interval record say *when* the
walking happened rather than drawing the day as one straight ramp from midnight.

Two properties are load-bearing, and both were learned the hard way:

- **Records never overlap** — not within a sync and not across two. A record's
  end follows the data, so a gap in the counters makes it end where the gap
  ends, past later slots on the 15-minute grid. A sync resuming inside one of
  those slots therefore starts its first record at the point it resumed, not at
  the slot's edge. This matters more than it sounds: Health Connect **discards
  the overlapping span when it aggregates**, so two records sharing a minute
  report less between them than either claims. A real day read 889 steps while
  its own records summed to 1,007.
- **A day differences from where the day before it ended**, per activity type,
  not from zero — the watch does not roll its counters over at local midnight.

A gap between the live step count and the synced total is normal and is not
either of the above: the live reading is the wrist's current number, and the
minutes since the watch last closed a monitoring file have not been handed over
yet. They arrive on the next sync.

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

## Staying Connected

Notification forwarding holds the link only while it has work. "Stay connected", on the watch's device screen, holds it always: whenever the watch is in range, the phone keeps the connection open, the way Garmin's own app behaves. Android's companion-device presence wakes OpenVitals when the watch comes back into range, so the link returns promptly rather than on a retry timer.

Off by default. A held link is what the features below ride on.

### Live Readings

With Stay connected on, a second switch streams the watch's live heart rate and step count to the phone. The current value appears on the watch's device screen and on the dashboard watch tile ("86 bpm now"). The values live in memory only and disappear when the link drops. Nothing live is ever stored; the same measurements arrive later through the normal sync, with the watch's own timestamps.

Off by default, because an open stream spends the watch's battery.

## Weather On The Watch

The watch's weather glance asks the phone for weather, and OpenVitals answers from a weather app on the phone, not from the internet. Any app that broadcasts the Gadgetbridge generic-weather format works; [Breezy Weather](https://github.com/breezy-weather/breezy-weather) is the tested one (enable its Gadgetbridge broadcast and add OpenVitals to its recipients). The snapshot is considered fresh for six hours.

The location the watch shows is the weather app's location. OpenVitals also answers the watch's own position asks from the phone's last known location, which is what arms the glance in the first place.

Known limitation: on the model verified against, the glance arms (it stops saying "Reconnect to phone") but does not always fetch. Gadgetbridge is at the same wall with the same watch generation.

## Find My Phone

Works in both directions. The Find action on the device screen makes the watch alert; the watch's own find-my-phone feature makes the phone ring at alarm volume, with a notification to stop it. The phone rings even when silenced, because a phone lost in a couch cushion is the whole point.

## CoMaps Guidance On The Watch

"CoMaps guidance on watch", on the watch's device screen, puts the turn-by-turn guidance CoMaps is giving (see [CoMaps navigation context](comaps-navigation-context.md)) on the wrist. Off by default, and complete in itself: no recording has to be running, and the activity-recording CoMaps integration does not have to be on. Switching it on asks for CoMaps' own permission, and the card says so if that grant is declined or later revoked. Ride with a route set in CoMaps and nothing else switched on, and the turns still reach the watch; record a GPS activity at the same time and the wrist and the phone's turn strip show the same guidance.

Garmin watches have no turn-by-turn channel a phone can drive, and Gadgetbridge's Garmin support has none either, so the guidance travels as a notification: the next manoeuvre as the title ("Turn left", "Roundabout, exit 3", "Arrive at destination"), the distance to it as the subtitle, and the street, the distance left and the time left as the body. One notification is added when guidance starts and updated in place from then on, so the watch does not buzz at every fix; it is withdrawn the moment guidance stops — the route ended, the toggle went off, the watch was forgotten — so a finished route never lingers on the wrist.

A new manoeuvre or street reaches the watch at once. A countdown that merely ticked down is refreshed at most every five seconds, and a reading that says nothing new is not sent at all. Distances and times are shown as CoMaps formatted them, so the wrist and the phone always agree on units. The notification rides the same link forwarded phone notifications use and needs nothing more than a paired Garmin watch: no notification access, no Stay connected.

## Calendar On The Watch

"Calendar on watch", on the watch's device screen, feeds the watch's calendar glance from the phone's calendar. Off by default; switching it on asks for Android's calendar permission.

The watch asks for a window of events and names its own limits (how many events, how long each field may be), and the phone answers within them. Recurring events arrive as their occurrences, declined and cancelled meetings stay off the wrist, and all-day events land on the wearer's midnight. Events go to the watch over Bluetooth and nowhere else; they are never stored and there is no network to send them over.

If the calendar permission is later revoked in system settings, the row says so, and the watch's asks are answered with an empty calendar rather than ignored.

## GPS Ephemeris

Ephemeris, a few days of predicted satellite orbits, is what turns a minutes-long cold GPS fix into a seconds-long one. Garmin's own app downloads it from Garmin silently; OpenVitals has no internet access and does not grow any for this. Instead, the same arrangement Gadgetbridge offers: the user downloads the file, imports it on the watch's device screen, and the phone hands it over when the watch asks.

The imported file is recognized by its contents (a constellation archive, an rxNetworks blob, or a Sony CPE blob; which one a watch wants is decided by its GPS chipset), and the URL the watch asked for is shown on the screen, since that URL is the only way to know which format to fetch. A stale file is refused rather than served: an out-of-date orbit prediction is worse for the watch than the almanac it already has.

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

Calendar events follow the same rule: read only while answering a watch that asked, held in memory only, sent only to the watch, never stored. Weather comes from a weather app on the phone and goes only to the watch; OpenVitals itself never talks to a weather service.

See [Privacy](../app/privacy.md) and [Permissions](../app/permissions.md) for the full boundary, including why the companion association is asked for and why it is optional.

## Known Limitations

- Verified end to end against one Garmin model. Other recent models are expected to work, but are untested.
- Older single-link transport watches cannot sync.
- There is no background sync. Every sync is one the user asked for.
- The Connected and Not connected labels reflect whether the watch is switched on in OpenVitals, not whether a Bluetooth link is open right now.
- WearOS watches are registered only. Sync, watch data, notification forwarding, watch settings, and find are Garmin-only.
- Health Snapshot values only exist if a Health Snapshot has been recorded on the watch.
- Battery percentage is read during a sync and shown on the device screen and the dashboard tile; charging state is not read.
- The weather glance on the verified model arms but does not always fetch; see Weather On The Watch.
