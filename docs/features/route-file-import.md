# GPX/KML/KMZ/TCX Route Import

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/manualentry/activity/routeimport`, `features/activity`.
> **Navigation:** `Screen.SettingsDataImport`, `Screen.ActivityEntry`.
> **Related:** [Feature map](feature-map.md), [FIT files import](fit-files-import.md), [Offline maps support](offline-maps-support.md), [Recording of activity](activity-recording.md).

OpenVitals can import GPX, KML, KMZ, and TCX activity files from Settings, Data Importers, preview one file in Activity Entry, and save supported activities to Health Connect. It can also bulk import multiple route files directly into Health Connect.

## Supported Formats

- GPX for route tracks and timestamps where present.
- KML and KMZ for route geometry.
- TCX for recorded activities, including indoor ones.

FIT activity, course, and workout files are also imported from Settings, Data Importers. See [FIT files import](fit-files-import.md).

The file's own content decides which parser is used, so a file with the wrong extension still imports correctly.

## Indoor And Routeless Files

A route file does not have to contain a route. A treadmill run or an indoor ride is a complete activity that simply has no positions in it, and OpenVitals imports it as one.

- TCX is the format that expresses this best: its laps carry total time, distance, and calories, and its track points carry heart rate, cadence, and speed with the position optional.
- A GPX whose track points carry timestamps but no coordinates is also accepted. It gives the start, the end, and the duration, plus whatever series the file recorded. Distance stays at zero because the file did not state one, and calories are left for the entry form to estimate.
- A file with neither positions nor timestamps is refused, because there is nothing in it to import.

## Imported Series

Imported activities carry the per-second series the file recorded, not just its geometry:

- Heart rate.
- Cadence.
- Speed.

That is what gives an imported activity the same charts a recorded one has, and it is the only content an indoor file has to offer besides its timing.

## Import Flow

The user opens Settings, Data Importers and chooses route file import, or shares a supported file with OpenVitals. OpenVitals then shows the detected activity details before the user decides whether to save.

For mass import, the user chooses the bulk action from Settings, Data Importers, selects multiple files, grants route import write permissions if needed, and OpenVitals writes each valid activity directly. The card shows progress plus imported and failed counts.

The review can include:

- Activity type.
- Title and notes.
- Start and end time.
- Route preview when usable route points are available.
- Distance and elevation.
- Estimated calories where available.

## Saved Data

When saved, OpenVitals writes supported Health Connect exercise session data and related records such as route, distance, elevation, heart rate, cadence, speed, active calories, and total calories where permissions and data allow.

Elevation gain from route points is smoothed before it is saved, using the same filter activity recording uses, so GPS noise does not inflate the figure. See [Recording of activity](activity-recording.md).

## Export

Saved route data can also be exported as GPX or KMZ when the activity has route points available.
