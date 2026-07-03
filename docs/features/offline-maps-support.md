# Offline Maps Support

Offline maps let activity routes render without app-level internet access.

For detailed download, import, and troubleshooting steps, see [Add offline maps](../how-to/offline-maps.md).

## Map Pack Import

Map packs are imported from Settings. OpenVitals supports:

- PMTiles packs compatible with the app's offline map renderer.
- Mapsforge `.map` or `.maps` packs.

Large imports can continue in the background and show progress through a notification.

## Where Maps Are Used

Imported offline maps can be used for:

- Activity recording map views.
- Saved activity route previews.
- Imported route previews before saving.

The map view can recenter on the route or current recording position when available.

## Local Storage

Imported map packs stay local on the device. They are app support data, not Health Connect records. Removing an imported pack removes the local map source without deleting Health Connect activities.
