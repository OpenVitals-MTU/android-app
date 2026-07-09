# GPX/KML/KMZ Route Import

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/manualentry/activity/routeimport`, `features/activity`.
> **Navigation:** `Screen.SettingsDataImport`, `Screen.ActivityEntry`.
> **Related:** [Feature map](feature-map.md), [FIT files import](fit-files-import.md), [Offline maps support](offline-maps-support.md).

OpenVitals can import GPX, KML, and KMZ route files from Settings, Data Importers, preview one file in Activity Entry, and save supported activities to Health Connect. It can also bulk import multiple GPX/KML/KMZ files directly into Health Connect.

## Supported Formats

- GPX for route tracks and timestamps where present.
- KML and KMZ for route geometry.

FIT activity, course, and workout files are also imported from Settings, Data Importers. See [FIT files import](fit-files-import.md).

## Import Flow

The user opens Settings, Data Importers and chooses GPX/KML/KMZ import, or shares a supported route file with OpenVitals. OpenVitals then shows the detected activity details before the user decides whether to save.

For mass import, the user chooses the bulk GPX/KML/KMZ action from Settings, Data Importers, selects multiple files, grants route import write permissions if needed, and OpenVitals writes each valid activity directly. The card shows progress plus imported and failed counts.

The review can include:

- Activity type.
- Title and notes.
- Start and end time.
- Route preview when usable route points are available.
- Distance and elevation.
- Estimated calories where available.

## Saved Data

When saved, OpenVitals writes supported Health Connect exercise session data and related records such as route, distance, elevation, active calories, and total calories where permissions and data allow.

## Export

Saved route data can also be exported as GPX or KMZ when the activity has route points available.
