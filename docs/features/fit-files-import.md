# FIT Files Import

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/manualentry/activity/routeimport`.
> **Navigation:** `Screen.ActivityEntry`, `ManualEntryWidgetId.ACTIVITY`.
> **Related:** [Feature map](feature-map.md), [GPX/KML/KMZ/FIT import](route-file-import.md), [Recording of activity](activity-recording.md).

FIT import lets users bring activity files into OpenVitals for review before saving to Health Connect.

## What FIT Import Is For

FIT files commonly come from fitness devices and activity platforms. OpenVitals uses supported FIT data to infer workout details such as activity type, timing, route points, distance, elevation, and other available activity metrics.

## Review Before Save

Imported FIT activities are not written immediately. The user reviews detected details, adjusts supported fields where needed, and then chooses whether to save the activity to Health Connect.

## Relationship To Route Import

FIT is also part of the broader route/activity file import workflow alongside GPX, KML, and KMZ. FIT files can contain richer workout metadata than plain route files when the source device included it.

## Limits

FIT files vary by device and exporter. OpenVitals imports supported fields and leaves unsupported or missing fields out of the saved Health Connect record.
