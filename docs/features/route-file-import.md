# GPX/KML/KMZ/FIT Import

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/manualentry/activity/routeimport`, `features/activity`.
> **Navigation:** `Screen.ActivityEntry`, `ManualEntryWidgetId.ACTIVITY`.
> **Related:** [Feature map](feature-map.md), [FIT files import](fit-files-import.md), [Offline maps support](offline-maps-support.md).

OpenVitals can import route and activity files, preview the result, and save supported activities to Health Connect.

## Supported Formats

- GPX for route tracks and timestamps where present.
- KML and KMZ for route geometry.
- FIT for route-backed activities and richer workout metadata where available.

## Import Flow

The user opens or shares a supported file with OpenVitals, reviews the detected activity details, and then decides whether to save.

The review can include:

- Activity type.
- Title and notes.
- Start and end time.
- Route preview.
- Distance and elevation.
- Estimated calories where available.

## Saved Data

When saved, OpenVitals writes supported Health Connect exercise session data and related records such as route, distance, elevation, active calories, and total calories where permissions and data allow.

## Export

Saved route data can also be exported as GPX or KMZ when the activity has route points available.
