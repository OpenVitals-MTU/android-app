# FAQ

## Does OpenVitals Upload My Health Data?

No. The local OpenVitals app does not ship app-level internet permission and does not upload health data to an OpenVitals server.

Health data is read from Health Connect on device. Entries explicitly saved by the user are written back to Health Connect.

## Why Does OpenVitals Ask For Many Health Connect Permissions?

OpenVitals has dashboard and detail screens for many health areas: activity, sleep, heart, body, hydration, nutrition, mindfulness, vitals, and cycle tracking.

Users do not need to grant everything. The dashboard can work with partial permissions, and cycle tracking is grouped separately so it can be granted or skipped explicitly.

## Why Is Cycle Tracking Separate?

Cycle data is sensitive. OpenVitals requests cycle permissions as their own Health Connect category in onboarding and Settings, so those records are shown only after the user grants that category.

## Why Can I Not See Old Data?

Health Connect restricts access to older records unless Health history access is granted. Open Health Connect permissions and grant history access if older data should be included.

Long reads can also hit Health Connect rate limits. When that happens, wait and try again later.

## Can OpenVitals Edit Records From Other Apps?

No. OpenVitals keeps third-party records read-only.

OpenVitals-created hydration, activity, mindfulness, body, and vitals entries can be edited later, but ownership is checked before updating Health Connect records.

## Why Do Routes Need Separate Approval?

Workout route data is treated as sensitive Health Connect data. Some route access must be approved manually from Health Connect settings.

## Why Does GPS Recording Need Location Permission?

OpenVitals needs precise location to record route-backed activities. Without it, the app cannot record reliable GPS tracks.

## Why Does OpenVitals Need Bluetooth Permission?

Bluetooth is used for three separate things, all of them local:

- Connecting to Bluetooth LE sensors during activity recording.
- Pairing with and syncing a Garmin watch.
- Copying Health Connect records to another nearby phone.

Nearby-device Bluetooth permissions do not add internet access.

## Why Does Phone-To-Phone Sync Use Bluetooth Instead Of Wi-Fi?

Because any Wi-Fi or TCP transfer on Android needs the `INTERNET` permission, and OpenVitals does not declare it. Bluetooth Classic keeps the transfer a direct link between the two phones and lets the app stay provably network-free.

## Why Does OpenVitals Need Notification Permission?

Notification permission is used for:

- Persistent activity recording notifications.
- User-started Apple Health import progress.
- Phone-to-phone sync progress.
- Optional hydration and mindfulness reminders.

Reminders are local and optional.

## Why Does OpenVitals Ask For Notification Access?

Notification access is a different, optional permission from notification permission, and it is used for one thing only: showing your phone's notifications on a paired watch.

OpenVitals reads the notification, sends it to the watch over Bluetooth, and keeps it in memory only. Nothing is written to a file, a database, or Health Connect, and the app has no internet permission, so there is nowhere else for it to go. Android grants this from its own settings screen; OpenVitals shows a disclosure first, and the feature does nothing until access is granted. Individual apps can be silenced, the feature can be turned off, and access can be revoked at any time.

## Why Does Android Ask To Let OpenVitals Access My Watch?

That is Android's companion device dialog. Allowing it lets Android keep OpenVitals running while the watch is nearby, so a sync that takes several minutes is not killed halfway through. Declining is fine: the watch is still paired and still syncs, just without the background priority.

## Where Does Watch Data Go?

Data from a synced watch is written to Health Connect wherever a matching record type exists. Watch-only series such as stress, Body Battery, and watch sleep scores have no Health Connect type, so OpenVitals keeps those in its own local database on the device.

## Is OpenVitals A Medical App?

No. OpenVitals shows wellness context from Health Connect records. Sleep score, cardio load, vital context, Body Energy, readiness, and metric interpretation cards are not medical diagnosis or treatment advice.

## Does OpenVitals Work Without Google Play Services?

OpenVitals does not depend on Google Play Services for core app logic.

Health Connect availability depends on Android version and device setup. Android 14 and newer include Health Connect as part of the system. Android 13 and older normally use the separate Health Connect app.

## What Is The Connected App?

The connected app is a separate app and repository for planned online features such as accounts and sharing. It is separate so the local app can remain internet-free.
