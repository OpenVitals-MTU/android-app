# FIT Files Import

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/manualentry/activity/routeimport/`, `lib/features/imports/`.
> **Navigation:** `/settings/data_import`, then `/manual_entry/activity` for review.
> **Related:** [Feature map](feature-map.md), [GPX/KML/KMZ route import](route-file-import.md), [Recording of activity](activity-recording.md), [Apple Health import](apple-health-import.md).

FIT import lives in Settings, Data Importers. It lets users bring supported activity, course, and workout files into OpenVitals for review before saving to Health Connect.

## How to use it

1. Go to **Settings › Data Importers** and open the **FIT Importer** card.
2. Choose one of:
   - **Import FIT file** — pick a single `.fit` file. OpenVitals reads it and opens the **Activity Entry** review screen (the same one route import uses), carrying any per-second heart-rate, cadence, and speed data. Review the detected details and tap **Save activity** to write it to Health Connect.
   - **Import a folder of FIT files** — pick a folder; OpenVitals finds every FIT file inside and writes them **straight to Health Connect with no review**, showing a result count.
3. Use the single-file path when you want to check timing, calories, or route before saving; use the folder path to import a device's whole history at once.

## What FIT Import Is For

FIT files commonly come from fitness devices and activity platforms. OpenVitals reads the Garmin FIT File Id type and handles Activity, Course, and Workout files differently: completed activities can provide timing, calories, distance, elevation, and optional GPS samples; courses provide route geometry and optional estimated duration; workouts provide structured workout metadata such as name, sport, and supported timed step duration.

## Review Before Save

Imported FIT files are not written immediately. The user reviews detected details, adjusts supported fields where needed, and then chooses whether to save the activity to Health Connect.

## Relationship To Route Import

FIT uses the same activity review screen as route import after the file is selected from Settings. Unlike plain route files, FIT files do not need GPS route points; OpenVitals imports supported activity, course, or workout details and attaches a route only when usable route samples are present.

## Limits

FIT files vary by device and exporter. OpenVitals imports supported fields and leaves unsupported or missing fields out of the saved Health Connect record.
