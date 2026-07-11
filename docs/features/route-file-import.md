# GPX/KML/KMZ Route Import

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/manualentry/activity/routeimport/`, `lib/features/imports/` (`route_import_intent.dart`, `pending_route_import.dart`), `lib/features/activity/`.
> **Navigation:** `/manual_entry/activity` (review step); `ManualEntryWidgetId.activity`.
> **Related:** [Feature map](feature-map.md), [FIT files import](fit-files-import.md), [Offline maps support](offline-maps-support.md).

OpenVitals can import GPX, KML, and KMZ route files from Activity Entry, preview the result, and save supported activities to Health Connect.

## Supported Formats

- GPX for route tracks and timestamps where present.
- KML and KMZ for route geometry.

FIT activity, course, and workout files are imported from Settings, Data Importers. See [FIT files import](fit-files-import.md).

## Import Flow

The user opens Activity Entry and chooses route import, or shares a supported route file with OpenVitals. OpenVitals then shows the detected activity details before the user decides whether to save.

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
