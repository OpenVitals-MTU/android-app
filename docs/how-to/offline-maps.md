# Add Offline Maps

OpenVitals can render activity routes without app-level internet access after offline map packs are imported.

For the short feature overview, see [Offline Maps Support](../features/offline-maps-support.md).

## Supported Formats

| Format | File extensions | Best for |
| --- | --- | --- |
| PMTiles | `.pmtiles` | General vector basemaps, especially Protomaps-compatible extracts. |
| Mapsforge | `.map`, `.maps` | Prebuilt regional vector maps, often useful for roads, trails, and outdoor detail. |

OpenVitals can store multiple packs, but it renders one map format at a time. Choose PMTiles or Mapsforge in Settings after import when both formats are present.

Prefer regional map files over full-world downloads. Smaller files import faster and use less storage.

## Option A: Prepare PMTiles

PMTiles is a single-file archive format. OpenVitals expects PMTiles data compatible with its offline route renderer.

Recommended PMTiles workflow:

1. Install the `pmtiles` CLI on a computer.
2. Choose a current Protomaps-compatible source file.
3. Choose a bounding box or region polygon for the area where activities are recorded.
4. Extract a regional `.pmtiles` file.
5. Copy the finished file to the phone.
6. Open OpenVitals.
7. Go to Settings, Activities, Offline maps.
8. Import the `.pmtiles` file.

Example extract command:

```bash
pmtiles extract https://build.protomaps.com/20260518.pmtiles my_area.pmtiles --bbox=MIN_LON,MIN_LAT,MAX_LON,MAX_LAT --maxzoom=14
```

Replace the input URL and bounding box with the current source and region. Lower maximum zoom levels can make files much smaller.

## Option B: Prepare Mapsforge

Mapsforge maps use `.map` or `.maps` files. They are commonly distributed as prebuilt regional maps.

Recommended Mapsforge workflow:

1. Download a country or regional `.map` file from a trusted Mapsforge source.
2. If the file is inside an archive, extract it first.
3. Copy the `.map` file to the phone if it was downloaded on a computer.
4. Open OpenVitals.
5. Go to Settings, Activities, Offline maps.
6. Import the `.map` or `.maps` file.
7. Choose Mapsforge as the render format if PMTiles packs are also imported.

## Choose Which Format To Render

If only one format has imported packs, OpenVitals uses that format automatically.

If both PMTiles and Mapsforge packs are present:

1. Open Settings, Activities, Offline maps.
2. Choose the active render format.
3. Return to recording or route preview.

Imported files stay on device until deleted.

## Where Maps Are Used

Imported maps are used automatically when map data is available for:

- GPS activity recording.
- Saved activity route previews.
- Imported route previews before saving.

No separate map selection is required during recording. If the route is outside the downloaded region, the route line may still appear without surrounding map detail.

## Manage Imported Maps

From Settings, Activities, Offline maps, users can:

- Import more packs.
- Switch render format.
- Delete unused map packs.

Deleting a map pack removes only local map support data. It does not delete Health Connect activities or routes.

## Troubleshooting

If import fails:

- Confirm the file extension is `.pmtiles`, `.map`, or `.maps`.
- For PMTiles, confirm the file is a compatible vector basemap.
- For Mapsforge, confirm the file finished downloading and is not corrupted.

If the map looks empty:

- Confirm the route is inside the downloaded region.
- Confirm the active render format matches the imported pack.
- Try zooming or recentering the route view.

If the file is too large:

- For PMTiles, extract a smaller bounding box or lower `--maxzoom`.
- For Mapsforge, download a smaller regional file.

If import is slow:

- Leave the import running or allow the background notification to finish.
- Prefer city, region, or country extracts instead of very large map files.
