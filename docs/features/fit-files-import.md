# FIT Files Import

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/manualentry/activity/routeimport`.
> **Navigation:** `Screen.SettingsDataImport`, then `Screen.ActivityEntry` for review.
> **Related:** [Feature map](feature-map.md), [GPX/KML/KMZ/TCX route import](route-file-import.md), [Recording of activity](activity-recording.md), [Watches](watches.md), [Apple Health import](apple-health-import.md).

FIT import lives in Settings, Data Importers. It lets users bring supported activity, course, and workout files into OpenVitals for review before saving to Health Connect.

## What FIT Import Is For

FIT files commonly come from fitness devices and activity platforms. OpenVitals reads the Garmin FIT File Id type and handles Activity, Course, and Workout files differently: completed activities can provide timing, calories, distance, elevation, and optional GPS samples; courses provide route geometry and optional estimated duration; workouts provide structured workout metadata such as name, sport, and supported timed step duration.

## Imported Series

An imported activity carries the per-second series the file recorded, not only its route: heart rate, cadence, and speed. This is what gives an imported FIT activity the same charts a recorded one has.

It also means an indoor session, which has no positions at all, still imports as a complete activity with usable data rather than as an empty one.

## Review Before Save

Imported FIT files are not written immediately. The user reviews detected details, adjusts supported fields where needed, and then chooses whether to save the activity to Health Connect.

## Import A Folder Of FIT Files

Reviewing one file is the right thing for one file and the wrong thing for two hundred. The FIT importer card therefore has a second action, "Import a folder of FIT files", which writes every FIT file under the picked folder straight to Health Connect without the review screen.

- The folder is picked with the system document tree picker and walked as a SAF tree (`RouteFolderScanner`), sub-folders included, up to a depth of 8. No storage permission is declared or needed; the pick itself grants access.
- Files are matched by the `.fit` extension, sorted by name (a watch names files by timestamp, so this is ride order), and handed to the same bulk importer the route card uses, which opens each file only when it reaches it.
- Activity FIT files become activities; Garmin wellness FIT files that carry nightly HRV are imported as HRV instead of failing. A file that will not read fails on its own and the rest of the folder carries on.
- A very large folder is capped at 50,000 files and the card says how many were taken. A folder with no FIT files in it is reported as such, not as an error.
- Because the folder import writes directly, it needs the same Health Connect write permissions as the route bulk import; the card offers to grant them.

Progress, imported and failed counts, and any warning show on the FIT card for a folder run, and on the route card for a multi-select route run.

## Relationship To Route Import

FIT uses the same activity review screen as route import after the file is selected from Settings. Like TCX, FIT files do not need GPS route points; OpenVitals imports supported activity, course, or workout details and attaches a route only when usable route samples are present.

Files a paired Garmin watch hands over go through this same importer. See [Watches](watches.md).

## Limits

FIT files vary by device and exporter. OpenVitals imports supported fields and leaves unsupported or missing fields out of the saved Health Connect record.
