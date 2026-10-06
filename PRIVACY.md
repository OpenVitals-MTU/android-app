# OpenVitals Privacy Policy

Last updated: October 6, 2026

OpenVitals is a local-first Android app for viewing health and fitness data from Health Connect.

This policy applies to the OpenVitals Android app published under the package name `tech.mmarca.openvitals`.

Privacy contact: manuel@mmarca.tech

## Summary

OpenVitals is designed to keep health data on your device.

- No account is required.
- No health data is uploaded to OpenVitals servers. There are none.
- The app has no internet permission. Android itself stops it from using the network.
- No ads are shown.
- No analytics SDKs are included.
- No health data is sold or shared with third parties.
- OpenVitals reads Health Connect data only after you grant the relevant Android permissions.
- OpenVitals writes to Health Connect only through something you start or switch on: saving an entry, recording an activity, running an import, adding a medical record, syncing a paired watch, setting up a bathroom scale, syncing from another phone, or the distance backfill setting.
- Data leaves the phone only over Bluetooth, to a watch or a second phone you paired yourself, or in a file you choose to export or share.

## Data OpenVitals Accesses

If you grant permission, OpenVitals may access the following data from Health Connect:

- Activity data, including steps, distance, exercise sessions, exercise routes, floors climbed, elevation gained, speed, power, cadence, active calories burned, and total calories burned.
- Sleep data, including sleep sessions, duration, and sleep stages.
- Heart and recovery data, including heart rate, resting heart rate, and heart rate variability.
- Body data, including weight, height, body fat, lean body mass, bone mass, body water mass, and basal metabolic rate.
- Nutrition and hydration data, including hydration, meals, calories, and macronutrients.
- Mindfulness session data, when supported by Health Connect on your device.
- Vitals data, including blood pressure, oxygen saturation, respiratory rate, body temperature, skin temperature, blood glucose, and VO2 max.
- Optional cycle tracking data, including menstruation, ovulation tests, cervical mucus, basal body temperature, intermenstrual bleeding, and sexual activity. This access is grouped as a sensitive Health Connect permission category and is used only after you grant those permissions.
- Medical records, on Android 14 and newer where Health Connect offers them: vaccines, allergies, conditions, medications, lab results, procedures, visits, vital signs, pregnancy, social history, personal details, and practitioner details. Each category has its own permission. See "Medical Records".

OpenVitals only accesses the categories you choose to grant in Health Connect.

Outside Health Connect, and only for the features you use, OpenVitals may also access:

- Your location, while you record an activity with GPS. See "Activity Recording".
- Bluetooth, to reach a heart rate strap or bike sensor, a paired watch, or a second phone, and to hear a bathroom scale you set up.
- Your phone's notifications, what it is playing, and your calendar, for a paired Garmin watch. Each is off until you switch it on. See "Paired Garmin Watches".
- Files you pick: an Apple Health export, a CSV file, a route or workout file, an offline map pack, an elevation tile, a medical records file.
- The camera, only while you scan the QR code of a health card. See "Medical Records".

## How Data Is Used

OpenVitals uses granted Health Connect data to show dashboards, charts, summaries, lists, details, home screen widgets and reports. It also derives figures on the device from that data, such as readiness, Body Energy, sleep scores and caffeine estimates.

The one-tap onboarding option can request write permissions up front. A granted write permission is used only by the actions listed in the summary. If a needed write permission has not been granted yet, the app asks for it before it writes, so Health Connect remains the source of truth.

If you enable hydration, mindfulness or cycle reminders, OpenVitals uses locally stored reminder preferences and granted Health Connect data to decide when to show an on-device reminder. Cycle reminders use a neutral wording unless you choose otherwise. Reminder notifications are generated locally on your device and are not sent to a server.

The app does not use health data for advertising, profiling, credit decisions, insurance decisions, employment decisions, or any purpose unrelated to showing your health data back to you.

## What OpenVitals Writes To Health Connect

- Entries you log or edit: activities, hydration and drinks, nutrition, body measurements, vitals, mindfulness sessions, cycle entries, workout plans.
- Activities you record in the app, with their route and sensor samples.
- Records from an import you run: an Apple Health export, a CSV file, a route or workout file.
- Records from a paired watch, when you sync it or switch on its automatic sync: workouts, sleep, heart rate, heart rate variability, steps, calories and weight. When a watch gives no sleep stages, OpenVitals writes a sleep session whose stages it estimated on the device.
- Weigh-ins from a bathroom scale you set up: weight, heart rate, and the body fat, lean body mass and body water mass OpenVitals estimates from them. See "Bathroom Scales".
- Records received from another phone, when you run a phone-to-phone sync on both phones.
- Medical records you import or type in. See "Medical Records".
- One distance record per day derived from your steps and stride length, if you switch on distance backfill. Switching it off removes them.

Records OpenVitals wrote can be edited or deleted in the app. Records written by other apps are read-only.

## Medical Records

On Android 14 and newer, where Health Connect offers medical records, OpenVitals can show, import and export them. Health Connect stores them in the FHIR format. OpenVitals asks for these permissions only inside its Medical records area, never together with other permissions, and each category has its own permission.

- **Viewing.** Records are shown as Health Connect holds them. OpenVitals does not interpret them. It flags a lab value as high or low only when the lab did.
- **Importing.** You pick a file: a FHIR file, a CDA document, an Apple Health or Estonian health portal export, or a SMART Health Card. The file is read on the device, and its records are written to Health Connect. Records in an older format are converted on the device first. A health card's signature is not checked, because that needs the internet, and its records say so.
- **Scanning a health card.** You can scan a card's QR code with the camera. OpenVitals asks for the camera only when you tap Scan. Each picture is read on the device and then dropped. None is saved or sent. Before it writes, OpenVitals compares the person named in the file with the records already on the phone, and asks you to confirm when they differ. The import report includes record content, so review it before you share it.
- **Entering.** Vaccines, allergies, medications and conditions you type in are written to Health Connect.
- **Exporting.** An export is a FHIR file created on the device. It leaves the phone only if you save or share it.
- **Saved documents.** When you import a file, you can choose to keep a copy of it. The choice starts off every time. A kept copy stays in the app's private storage and is not backed up. It can hold more than the records taken from it, such as names, identifiers and notes. Opening it hands it to a viewer app on your phone. Deleting a document does not delete its records, and deleting records does not delete the document.
- **Phone-to-phone sync.** When you select medical records in Sync with another phone, all your medical records are copied to the other phone, whatever range you choose. Saved documents are not copied.

OpenVitals can delete only the medical records it wrote. Records from other apps stay read-only.

## Activity Recording

While you record an activity, OpenVitals uses location to build the route, and motion sensors and Bluetooth sensors you added to measure steps, heart rate, power, speed and cadence. A notification shows that a recording is running. The recording is kept in the app's private storage until you save it to Health Connect or discard it. If you switch on the CoMaps integration, OpenVitals reads turn guidance from the CoMaps app on your phone to show it during the recording and, if you choose, keeps it with the activity on the device.

## Paired Garmin Watches

A paired watch is reached over Bluetooth only. Nothing about a watch is sent off the phone.

**Sync.** The watch's recorded files are read over Bluetooth and imported on the device. Series Health Connect has no type for, such as stress, Body Battery, and the watch's sleep and readiness scores, are stored in the app's own local database. The rest is written to Health Connect. A copy of each downloaded file is kept in the app's private storage for about 30 days. The copies, and the per-minute sleep data, age out when the app starts or a watch syncs, whichever comes first.

**Pairing.** Pairing uses Android's companion device manager, so Android keeps OpenVitals running while the watch is in range and a long sync can finish.

**Phone location.** When the watch asks for it, to speed up its own GPS fix, OpenVitals sends the phone's last known position to the watch. It does this only if you granted the location permission. It does not request a new fix and does not store the position.

**Notifications (optional).** If you turn on **Send notifications to the watch**, OpenVitals reads the notifications your phone receives so they can be shown on your wrist. This is off by default and does nothing until you turn it on.

- OpenVitals asks you to confirm what will be read before Android's notification-access screen is opened.
- Notification text is read on your device and sent only to the watch you paired, over Bluetooth.
- Notification content is never written to a file or a database. It is held in memory only while it is needed: a short buffer on the phone, and the recent notifications your watch may still ask about. It is discarded after that.
- Notifications marked by their app as ongoing, as group summaries, or as local to this device are not forwarded. Neither are notifications from apps you silence in **Apps to silence**.
- Your phone's Do Not Disturb setting is respected: while it is on, notifications are not forwarded.
- Acting on a notification from the watch, by dismissing it, replying, or using one of the app's own buttons, performs that action on the phone as if you had tapped it there. OpenVitals does not send SMS or messages itself.
- You can turn the feature off at any time, and revoke notification access from Android settings. Revoking it stops the feature immediately.

**Music controls (optional).** With **Music controls on watch** switched on, OpenVitals reads what the phone is playing so the watch can show and control it: the player app's name, the track, artist and album, the track length and the playback position. Android shows media players only to an app with notification access, so the same grant is used, behind its own disclosure. This is read only while the switch is on, sent only to the watch, and never stored. Granting access for music does not switch notification forwarding on.

**Calendar (optional).** If you switch on **Calendar on watch** for a watch, OpenVitals asks for Android's calendar permission and reads your calendar to answer the watch's calendar glance. Events are read only while answering a watch that asked, within the window the watch named. They are held in memory only and sent only to the watch. Cancelled events, and meetings you declined, are not sent.

**Weather.** Weather shown on the watch comes from a weather app on your phone that broadcasts it, such as Breezy Weather. OpenVitals never contacts a weather service. It passes the forecast on to the watch and keeps the latest one locally.

## Bathroom Scales

If you set up a Xiaomi Body Composition Scale S400, OpenVitals listens for the Bluetooth broadcasts the scale sends when someone steps on it. It never connects to the scale and sends nothing to it.

**Listening.** From the moment you set a scale up until you remove it, Android hands that scale's broadcasts to OpenVitals, whether the app is open or closed. Broadcasts from other devices are not delivered. This uses the Nearby devices permission, which OpenVitals declares as never used to work out your location. On Android 11 and older it uses the location permission instead, and works only while the app is open.

**Waking up for a weigh-in.** The scale sends its result for only about two seconds, which a closed app would usually miss. If you allow it, OpenVitals registers the scale with Android's companion device manager, so Android wakes OpenVitals when someone steps on the scale. OpenVitals then listens for up to 45 seconds and shows a silent notification while it does. Android's own dialog asks for this, and you can undo it by removing the scale in OpenVitals or in Android's settings.

**The key.** The scale encrypts what it broadcasts. You get its key from your own Xiaomi account, outside OpenVitals, and paste it in. OpenVitals never contacts Xiaomi. The key is kept in the app's private storage, is not part of any export or phone-to-phone sync, and is erased when you remove the scale.

**What is kept.** Each weigh-in's readings, which are the weight, the heart rate and the two body impedance values the scale measures, are stored in the app's local database, because Health Connect has no record type for impedance. The weight, the heart rate, and the body fat, lean body mass and body water mass OpenVitals estimates on the device are written to Health Connect. The estimates use the height, sex and age in your body profile. If Health Connect cannot take a weigh-in at that moment, it waits in the local database and is written later.

**Other people.** A scale tells its users apart. A weigh-in the scale assigns to someone else is not stored. OpenVitals keeps only the fact that one was ignored.

## Phone-To-Phone Sync

Sync copies Health Connect records from one phone to another over a paired Bluetooth link. Before any record moves, both phones show the same six-digit code, and you confirm on both that they match. That tells you no other device sits in between. The records are then encrypted between the two phones with a key made for that one sync, on top of Bluetooth's own encryption. You choose how far back to sync and which categories to send. Medical records always sync in full. No server or account is involved. Each phone keeps a plain-text report of its last sync in the app's private storage.

## Imports, Exports And Reports

An Apple Health export you pick is copied into the app's private storage and analysed there before you choose what to import. The copy is removed after a successful import, when the analysis fails, or when you pick another export. A copy you never came back to is removed when the app next starts, once it is a day old.

Exports (routes, workouts, reports, CSV files) and import reports are created on the device and leave it only if you save or share them. An import report includes the importer's full log, so review it before you share it.

## Data Sharing

OpenVitals does not sell, rent, transfer, or share your health data with third parties.

OpenVitals does not include advertising SDKs or analytics SDKs.

## Local Storage

Health Connect remains the source of truth for the health data it has a record type for. OpenVitals also keeps, in its private storage on your device:

- App preferences, such as units, language, theme, widget order, goals, reminder settings, timer settings, your body profile, paired device settings, acknowledged permission prompts, and the cycle tracking contexts and age band you declare.
- A local database with your drink catalog, cached daily summaries that make the charts fast (up to two years), the Body Energy timeline, the watch-only wellness series described above, up to 45 days of per-minute watch data used to estimate sleep stages, the readings of each bathroom scale weigh-in, and the cycle journal: the pain, mood, energy, symptoms, notes, pregnancy-test results, temperature disturbances and cervical sensation you log per day, and the cycles you exclude from estimates. Health Connect has no record type for these. It also holds the list of medical documents you chose to keep. The journal stays until you delete a day's entry, delete the journal in Settings, or clear the app's data. It moves to another phone only through Sync with another phone, when the cycle category is selected on both phones.
- Files: downloaded watch files, a recording in progress or not yet saved, a staged Apple Health export, offline map packs and elevation tiles you imported, medical documents you chose to keep, and the last sync report.

If reminders are enabled, Android may restore the local reminder schedule after a device restart. OpenVitals uses the notification permission for reminders, active recording notifications, sync notifications, the notification shown while a scale's weigh-in is read, and the find-my-phone alert a watch can start.

OpenVitals opts out of Android's cloud backup. The shipping app manifest removes inherited Android network permissions, including `INTERNET`.

## Diagnostics

OpenVitals sends no crash reports by itself. After a crash it can open an email draft with the device model, the app version and the error trace. Nothing is sent unless you press send. Debug and nightly builds can also export the app's log, unfiltered, when you ask for it. Release builds cannot.

## Data Retention And Deletion

Health data in Health Connect is controlled by your device and Health Connect settings. Records OpenVitals wrote there stay after you uninstall the app. You can delete them in the app before that, or from Health Connect's own settings at any time.

You can revoke OpenVitals permissions at any time from Android Settings or Health Connect settings. After permissions are revoked, OpenVitals can no longer access the revoked data. Summaries it cached earlier stay on the device until you clear them.

Removing a watch from OpenVitals does not delete the data it already synced. When you remove your last Garmin watch, its file copies and per-minute sleep data are deleted, and you can choose to delete the stress, Body Battery and score history that only this phone holds. Removing a scale erases its key and stops the listening. The weigh-ins already saved stay.

You can delete all local OpenVitals data by clearing the app's storage in Android settings or uninstalling the app.

## Security

OpenVitals relies on Android and Health Connect permission controls to protect access to health data. Its local data sits in app-private storage, which other apps cannot read. You can also require your device unlock to open the app.

Because OpenVitals works locally and does not upload health data, there is no OpenVitals cloud account or cloud health-data store.

## Children

OpenVitals is not directed to children and is not designed for child-directed use.

## Changes To This Policy

This privacy policy may be updated when OpenVitals changes how it accesses, uses, stores, or shares data. The "Last updated" date above will be changed when this policy is updated.

## Contact

For privacy questions, contact:

manuel@mmarca.tech
