# Elevation Correction

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/activity/elevation`, `features/manualentry/activity/routeimport`, `core/geo`, `features/settings`.
> **Navigation:** Settings, Activities, Elevation correction.
> **Related:** [Feature map](feature-map.md), [Add elevation tiles](../how-to/elevation-tiles.md), [GPX/KML/KMZ/TCX route import](route-file-import.md), [FIT files import](fit-files-import.md), [Watches](watches.md), [Offline maps support](offline-maps-support.md).

A barometric altimeter drifts with the weather, sweat, and age; a watch or phone without one reports noisy GPS altitude. Both inflate elevation gain. Elevation correction replaces the altitude of every point in an imported route with the height from a digital elevation model (DEM) tile stored on the phone, then recomputes the gain from that profile.

For where to get tiles and how to import them, see [Add elevation tiles](../how-to/elevation-tiles.md).

## What Gets Corrected

Correction runs when a file is imported: GPX, TCX, KML, KMZ, and FIT files from Settings, Data Importers and Activity Entry, a folder of FIT files, and the activity files a paired watch hands over during sync. All of these go through one importer, so they cannot differ.

Activities recorded on the phone are not changed. They already filter their own barometer and GPS altitude; see [Recording of activity](activity-recording.md).

Activities already saved in Health Connect are never rewritten. Only new imports are corrected.

## Coverage And Skip Rule

A route is corrected only when every one of its points falls inside an imported tile and none of them lands on a void sample. If a single point is uncovered, the whole file keeps the altitudes it came with, and the import log says so. A profile half from the DEM and half from a drifting sensor would be worse than either.

When correction applies, the file's own ascent total is replaced too, since it came from the sensor that drifted. Distance, timing, heart rate, cadence, and speed are untouched. Elevation gain is computed from the corrected profile with the same smoothing filter a recording uses.

## Tiles

Tiles are SRTM-style `.hgt` files, one per one-degree cell, named by the cell's south-west corner:

| Grid | Samples | File size |
| --- | --- | --- |
| 3 arc-second (about 90 m) | 1201 x 1201 | 2,884,802 bytes |
| 1 arc-second (about 30 m) | 3601 x 3601 | 25,934,402 bytes |

`N45E007.hgt` covers 45 to 46 N and 7 to 8 E. A `.zip` holding one `.hgt` can be imported as is. A file that is neither size is refused.

Altitude at a position is interpolated from the four nearest grid samples.

## Nothing Is Downloaded

The app declares no internet permission. Tiles are obtained on a computer and imported with the system file picker, the same way offline map packs are.

## Local Storage

Imported tiles stay on the device as app support data, not Health Connect records. Deleting a tile removes only that file; activities imported while it was present keep their corrected values. The toggle in Settings switches correction off without deleting tiles.
