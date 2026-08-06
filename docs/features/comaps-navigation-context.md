# CoMaps Navigation Context

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `comaps`, `data/repository`, `domain/model`, `features/manualentry/activity/recording`, `features/activity`, `features/settings`.
> **Navigation:** GPS activity recording screen; activity detail; Settings > Activity recording.
> **Related:** [Feature map](feature-map.md), [Recording of activity](activity-recording.md), [Offline maps support](offline-maps-support.md).

While recording a GPS activity, OpenVitals can show the turn-by-turn guidance CoMaps is giving at that moment: the next street, the distance to the turn, route progress, and the planned route drawn under the recorded track. CoMaps plans and navigates; OpenVitals records. The integration reads what CoMaps is already doing and cannot start, stop, or steer a route.

The integration is off by default and switched on in Settings > Activity recording.

## What Appears While Recording

- **Turn strip on the map tab**: a green overlay with a rotated turn arrow (flag on arrival), the distance to the turn, and the next street. It floats over the live map, above the recorded track and the dashed planned route.
- **Dashboard tile on the stats tab**: `CoMaps guidance` joins the layout editor's field list while the integration is on — the distance to the turn as the value, the street it turns onto as the label.
- **State card**: when CoMaps is reachable but not guiding, a small card explains why — permission missing (with an allow button), CoMaps not installed, provider not available, or simply not navigating (with a "Plan in CoMaps" button that opens CoMaps on the current position).

Distances and times arrive pre-formatted from CoMaps and are shown as sent, so both apps always agree on units.

## How It Reads CoMaps

CoMaps (upstream PR #4588) exposes a navigation `ContentProvider` at `<package>.provider.navigation`, guarded by the runtime permission `<package>.permission.READ_NAVIGATION_DATA`. The permission name is flavour-specific, so the manifest declares it for every known CoMaps package (app.comaps and its `.fdroid`, `.google`, `.huawei`, `.test`, and debug variants).

The feed is observed, not polled: CoMaps notifies the provider URI on every location fix while it guides, so a phone that is navigating nowhere is never queried. Two subtleties keep the state honest:

- Querying the provider starts the CoMaps process if it is not running, so nothing is queried unless a GPS recording is active and the integration is on.
- CoMaps serves its last route from a cache it never clears, so a row only counts as guidance while change notifications are fresh (a 15-second liveness window backed by a 10-second safety poll that runs only while a route is being followed). Without this, a finished route would haunt the next recording.

## Saved Guidance

With the separate "Save CoMaps guidance with activity" toggle on, the readings shown during the recording are banked (at most one per 15 seconds, only when the guidance changed) and saved beside the activity when it is saved. The activity detail screen then shows a CoMaps navigation card listing each reading: the street, the distances, the direction, the time, and route progress.

Saved guidance is app-local support data, like recording markers. It is never written to Health Connect, and deleting the activity deletes it.

## Route Line

While CoMaps guides, the route polyline it is following is fetched once per route revision and drawn as a solid blue line under the recorded track on both the MapLibre and Mapsforge renderers — every point CoMaps serves, so the drawn line matches CoMaps' own 1:1. The polyline is display-only and never persisted, and both renderers rebuild it only when the route revision changes, never per frame; the point-buffer conversion runs on a worker thread so even a cross-country route never blocks the UI.
