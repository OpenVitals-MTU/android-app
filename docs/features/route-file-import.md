# GPX/KML/KMZ/TCX Route Import

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/manualentry/activity/routeimport/`, `lib/features/imports/` (`route_import_intent.dart`, `pending_route_import.dart`), `lib/features/activity/`.
> **Navigation:** started from `/settings/data_import` (the **GPX/KML/KMZ/TCX Importer** card); single-file import stages into `/manual_entry/activity` for review.
> **Related:** [Feature map](feature-map.md), [FIT files import](fit-files-import.md), [Offline maps support](offline-maps-support.md).

OpenVitals can import GPX, KML, KMZ, and TCX activity files, preview the result, and save supported activities to Health Connect.

## How to use it

1. Go to **Settings › Data Importers** and open the **GPX/KML/KMZ/TCX Importer** card.
2. Choose one of:
   - **Import GPX/KML/KMZ/TCX file** — pick a single file. OpenVitals parses it, then opens the **Activity Entry** review screen with an **Imported route** section (map preview, distance, elevation, point count, and a pace/speed line when the file has timestamps). The route's distance, elevation, duration, and calories pre-fill the editable fields. Tap **Save activity** to write it to Health Connect.
   - **Bulk import GPX/KML/KMZ/TCX files** — pick several files and write them straight to Health Connect with **no per-file review**. Grant route-import permissions if prompted; a progress and result count are shown.
3. To remove a bad route before saving a single import, use the clear/trash button in the **Imported route** section.

Use single-file import when you want to check and tweak a route before saving; use bulk import to load a backlog quickly.

## Supported Formats

- **GPX** for route tracks and timestamps where present.
- **KML** and **KMZ** for route geometry.
- **TCX** for activities including indoor ones that have no GPS route.

FIT activity, course, and workout files are imported from the separate **FIT Importer** card in the same Settings section. See [FIT files import](fit-files-import.md).

## Import Flow

Single-file import stages the parsed file and opens the **Activity Entry** review screen so the user sees the detected activity details before deciding whether to save. Bulk import skips the review and writes each file directly.

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
