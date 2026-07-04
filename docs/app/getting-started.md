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
- Import GPX, KML, and KMZ route files from Activity Entry, or FIT activity, course, and workout files from Settings, Data Importers, then review detected details before saving.
- Record a GPS activity from OpenVitals, then review and save it to Health Connect.
- Import PMTiles or Mapsforge map packs from Settings for offline route maps.
- Configure the recording dashboard, use Focus mode, keep the screen awake, and connect supported Bluetooth LE sensors while recording.

GPS recording needs precise location permission. Bluetooth LE sensor recording needs nearby-device Bluetooth permission on Android versions that require it. Finished GPS drafts can be discarded before saving.

Recording notifications, Apple Health import progress, and reminders use notification permission on Android versions that require it.

## Home Screen Widgets

After setup, long-press the Android home screen and add an OpenVitals widget for a selected metric, Daily Readiness, Body Energy, Today Vitals, or quick beverage logging.

Widgets use the same on-device Health Connect data and local derived calculations as the app.

## Importing Apple Health Exports

Settings includes Data Importers for supported Apple Health `export.xml` or `export.zip` records and FIT activity/course/workout files.

Imported records are written into Health Connect after required write permissions are granted. Large imports can continue in the background and show progress while OpenVitals scans and writes records.
