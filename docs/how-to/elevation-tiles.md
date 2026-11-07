# Add Elevation Tiles

OpenVitals can replace the altitude in imported activity files and in activities recorded on the phone with values from elevation tiles stored on the phone. This fixes routes from a drifting barometric altimeter or from noisy GPS altitude.

For the short feature overview, see [Elevation Correction](../features/elevation-correction.md).

## Supported Tile Formats

| Format | File name | Size | Notes |
| --- | --- | --- | --- |
| SRTM 3 arc-second | `N45E007.hgt` | 2,884,802 bytes | 1201 x 1201 samples, about 90 m apart. Enough for gain and loss totals. |
| SRTM 1 arc-second | `N45E007.hgt` | 25,934,402 bytes | 3601 x 3601 samples, about 30 m apart. Better profiles in steep terrain. |
| Zip | `N45E007.hgt.zip`, `N45E007.SRTMGL1.hgt.zip` | as above | One `.hgt` per zip. Other files inside the zip are ignored. |

GeoTIFF, `.tif`, `.dem`, and other DEM formats are not supported.

## Tile Naming Rule

A tile is named by the latitude and longitude of its south-west corner: `N` or `S`, two digits, then `E` or `W`, three digits.

- `N45E007.hgt` covers 45 to 46 N and 7 to 8 E.
- `S34W071.hgt` covers 34 to 33 S and 71 to 70 W.
- `N00E000.hgt` covers 0 to 1 N and 0 to 1 E.

OpenVitals matches tiles by name, so do not rename them. Extra words after the cell, as in `N45E007.SRTMGL1.hgt`, are fine.

## Which Tiles Do I Need

One tile per one-degree cell your activities cross. A city needs one tile, a mountain range two to four. An activity that leaves the imported tiles keeps its original altitude.

## Where To Get Tiles

The app downloads nothing. Get the tiles on a computer, then copy them to the phone.

### Option A: Viewfinder Panoramas

Void-filled SRTM tiles for the whole world at 3 arc-second, and 1 arc-second for Europe, North America, and a few other regions. Free, no account.

1. Open the 3 arc-second coverage map: https://viewfinderpanoramas.org/Coverage%20map%20viewfinderpanoramas_org3.htm. For 1 arc-second tiles use https://viewfinderpanoramas.org/dem1d.html instead.
2. Click the map square that holds your area. Each square downloads a `.zip` with the `.hgt` tiles it covers.
3. Unzip it on the computer. Keep only the tiles for the one-degree cells you need, for example `N45E007.hgt`.
4. Copy those `.hgt` files to the phone.

About the data: https://viewfinderpanoramas.org/dem3.html.

### Option B: USGS EarthExplorer

The original SRTM 1 arc-second data, worldwide. Free account required.

1. Open https://earthexplorer.usgs.gov/ and sign in.
2. Under Search Criteria, pick your area on the map or by coordinates.
3. Under Data Sets, choose Digital Elevation, SRTM, SRTM 1 Arc-Second Global.
4. In Results, download each cell in the HGT format. The file is named by its cell, for example `N45E007.SRTMGL1.hgt.zip`; import it as is or unzipped.
5. Copy the files to the phone.

Raw SRTM has voids in steep and coastal terrain. A route that touches one keeps its original altitude, so prefer Option A there.

### Option C: Sonny's LiDAR DTMs of Europe

1 arc-second `.hgt` tiles resampled from national LiDAR surveys, more accurate than SRTM where they exist. Free, no account, Creative Commons Attribution 4.0.

1. Open https://sonny.4lima.de/.
2. Pick your country and the 1 arc-second `.hgt` download. Files are hosted on Google Drive.
3. Unzip, keep the tiles you need, and copy them to the phone.

Do not pick the 0.5 arc-second or GeoTIFF downloads: OpenVitals reads only 1 and 3 arc-second `.hgt` tiles.

Licence and attribution are the user's responsibility.

## Import Tiles

1. Copy the `.hgt` or `.zip` files to the phone.
2. Open OpenVitals.
3. Go to Settings, Activities, Elevation correction.
4. Tap Import elevation tile and pick a file. Repeat for each tile.
5. Leave Correct altitude from tiles switched on.
6. Import a route file, sync a watch, or finish a GPS recording. The review screen and the activity's elevation profile now come from the tile.

The import copies the file into app storage and validates its size. It takes about a second.

## Coverage And Skipped Routes

A route is corrected only when every point is inside an imported tile. A route with one point outside, or on a void sample, keeps the altitudes it came with, from the file or from the phone's sensors. If a route's gain did not change after import or recording, check that tiles for every cell it crosses are listed.

## Manage Tiles

From Settings, Activities, Elevation correction, users can:

- Import more tiles.
- Replace a tile by importing one with the same name.
- Delete a tile.
- Switch correction off without deleting tiles.

Deleting a tile removes only local support data. It does not change Health Connect activities.

## Troubleshooting

If import fails:

- Confirm the name starts with the cell, for example `N45E007`.
- Confirm the file is exactly 2,884,802 or 25,934,402 bytes. Any other size means a truncated download or a different format.
- A zip must hold exactly one `.hgt`. Unzip a regional archive first and import the tiles one by one.

If altitude did not change after an import or a recording:

- Confirm the toggle is on.
- Confirm a tile is listed for every cell the route crosses.
- Coastal and mountain-shadow areas of raw SRTM contain voids. Prefer void-filled tiles.
