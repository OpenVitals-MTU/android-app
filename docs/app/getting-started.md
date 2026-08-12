# Getting Started

## First Launch

OpenVitals starts with onboarding so users can connect the app to Health Connect and decide which data categories it may read.

The dashboard can work with partial permissions. Activity and Sleep are a small useful starting point. Heart, Body, Nutrition, Hydration, Mindfulness, Vitals, and Cycle can be granted later when those areas are needed.

## Basic Setup

1. Install or enable Health Connect.
2. Open OpenVitals.
3. Review the Health Connect permission categories.
4. Use one-tap setup to grant all requestable permissions, or choose categories individually.
5. Return to the Summary screen and wait for Health Connect data to load.

## If No Data Appears

- Check that another app or device is writing data into Health Connect.
- Open Health Connect settings and confirm OpenVitals has the relevant read permissions.
- Grant Health history access if older records outside the recent access window should be included.
- For route previews, open Health Connect permissions manually and approve workout route access.
- If Health Connect reports rate limiting, wait and try again later.

## Adding Entries

The Summary dashboard is read-only. Use Log or Add entry when saving a new record to Health Connect.

Supported entry areas include:

- Hydration and beverages.
- Carbohydrates.
- Activity sessions.
- Mindfulness sessions.
- Weight, height, and body fat.
- Blood pressure, SpO2, respiratory rate, and body temperature.

OpenVitals-created entries can be edited or deleted later. Records created by other apps stay read-only in OpenVitals.

## Importing Or Recording Activities

Activity entry supports several workflows:

- Create a manual activity.
- Import GPX, KML, KMZ, TCX, and FIT activity, course, and workout files from Settings, Data Importers, then review detected details before saving.
- Bulk import multiple GPX, KML, or KMZ route files from Settings, Data Importers when you want to save them directly.
- Record a GPS activity from OpenVitals, then review and save it to Health Connect.
- Import PMTiles or Mapsforge map packs from Settings for offline route maps.
- Configure the recording dashboard, use Focus mode, keep the screen awake, and connect supported Bluetooth LE sensors while recording.

GPS recording needs precise location permission. Bluetooth LE sensor recording needs nearby-device Bluetooth permission on Android versions that require it. Finished GPS drafts can be discarded before saving.

Recording notifications, Apple Health import progress, and reminders use notification permission on Android versions that require it.

## Pairing A Watch

Settings, Watches pairs a Garmin watch over Bluetooth and copies what the watch recorded into OpenVitals.

- Keep the watch awake and close to the phone while scanning.
- Confirm the pairing code shown on the watch.
- Android asks separately whether OpenVitals may access the watch. Allowing it lets sync keep running in the background; declining still works, the watch is just more likely to be interrupted mid-sync.
- Synced data goes into Health Connect where a matching record type exists. Watch-only series such as stress and Body Battery are stored locally in OpenVitals instead, because Health Connect has no type for them.

Forwarding phone notifications to the watch is optional and off until it is turned on. It needs Android's notification access, which is granted from an Android settings screen rather than an in-app prompt. Notifications are read on the phone and sent only to the paired watch. Individual apps can be silenced from Settings, Watches, Notifications.

## Syncing With Another Phone

Settings, Sync with another phone copies Health Connect records between two nearby phones over Bluetooth, with no cloud involved. The exchange is two-way: each phone sends what the other does not already have, and duplicates are skipped.

1. On one phone, choose Make this phone discoverable and note the six-digit code.
2. On the other phone, choose Find a phone, pick the first phone from the scan, and type that code.
3. Choose how far back to sync and which data categories to include.

Bluetooth must be on, and an activity recording must be finished or discarded first, because recording and sync cannot hold the foreground at the same time.

## Home Screen Widgets

After setup, long-press the Android home screen and add an OpenVitals widget for a selected metric, Daily Readiness, Body Energy, Today Vitals, or quick beverage logging.

Widgets use the same on-device Health Connect data and local derived calculations as the app.

## Importing Apple Health Exports

Settings includes Data Importers for supported Apple Health `export.xml` or `export.zip` records, GPX/KML/KMZ route files, TCX and FIT activity/course/workout files, and CSV files of point-in-time measurements. GPX/KML/KMZ can be imported one at a time for review or in bulk for direct save.

The CSV importer covers measurements taken at a moment in time, such as weight, body composition, heart rate, HRV, SpO2, respiratory rate, temperature, blood glucose, and VO2 max. You choose which column is the timestamp, which is the value, and what unit it is in. Records that span a period, such as steps, sleep, and workouts, are not part of the CSV importer.

Imported records are written into Health Connect after required write permissions are granted. Large imports can continue in the background and show progress while OpenVitals scans and writes records.
