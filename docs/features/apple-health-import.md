# Apple Health Import

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/imports/applehealth`.
> **Navigation:** `Screen.SettingsDataImport`, settings section `DATA_IMPORT`.
> **Related:** [Feature map](feature-map.md), [Settings and preferences](settings-and-preferences.md), [Permissions](../app/permissions.md).

OpenVitals can analyze supported records from Apple Health exports, let the user choose which categories to import, and then write the selected Health Connect-compatible records with duplicate checks, route handling, and a downloadable full report.

## Input Files

The import starts from Settings and accepts Apple Health `export.xml` or `export.zip` files. Zipped exports may also contain Apple workout route files under `workout-routes/*.gpx`; these are matched to `WorkoutRoute` file references in `export.xml`.

Large exports run as user-started background work so the import can continue after leaving the Settings screen.

## Analyze And Import Flow

The app scans the export with a streaming SAX parser rather than loading the whole XML document into memory. External DTD/entity loading is disabled so Apple export doctypes do not force network or grammar resolution, and Apple date strings keep their timezone offsets when records are mapped to Health Connect.

The first pass is analysis-only. It streams the selected export, maps Apple types to import categories, and shows detected import categories such as workouts and routes, activity metrics, heart, sleep, body, vitals, nutrition, hydration, mindfulness, and cycle tracking. Nothing is written to Health Connect during this analysis pass, and the analyzer does not parse GPX route geometry or construct Health Connect records just to build the checkbox menu. Analysis only needs each record's Apple type, so it skips parsing timestamps, metadata, and numeric values, and it streams `export.xml` directly out of a zipped export instead of extracting a temporary copy first; both cut analysis time on large exports.

After analysis, the user can select or unselect categories with checkboxes. The MVP import pass then re-parses the same export, parses GPX route geometry when route files are present, and writes only converted records whose Health Connect target category is selected. This avoids holding very large exports or converted record lists in UI memory.

If a ZIP ends unexpectedly while the full import is reading a `workout-routes/*.gpx` entry, OpenVitals can continue only when `export.xml` was already copied intact. It imports the health records and any workout routes parsed before the damaged entry, marks workout routes as incomplete in the result, and records a `route_archive_truncated` diagnostic in the report. Workout sessions whose referenced GPX files were unavailable are imported without route geometry and listed under **Activities Requiring Manual Route Import** with their Apple activity type, time range, and missing file path so the user can identify them for manual recovery. The damaged route and remaining ZIP entries are unavailable. Damage before `export.xml` is complete still fails the import rather than accepting partial health data.

The importer requests required Health Connect write permissions before writing accepted records into Health Connect. Large exports are processed with targeted lookups and time-window chunking to reduce memory pressure. Progress and result counts are shown while the import runs.

Import progress is split into explicit stages: scanning the export, converting records, checking duplicates, writing records, and building the report. During the import pass, the determinate percentage is based on the compatible records in the categories the user selected, not the full export. The downloadable report logs start and finish entries for these stages, including batch counts where available, so long-running imports can show whether time is being spent parsing, deduplicating, writing to Health Connect, or preparing diagnostics.

Parsing/converting and the Health Connect duplicate-check/write steps run concurrently on a producer/consumer pipeline instead of blocking each other: while one batch of converted records is being checked and written, the parser keeps reading and converting the next batch. Health Connect duplicate lookups for a batch also run as several bounded concurrent queries instead of one at a time. Both changes shorten wall-clock import time without changing which records get imported, deduplicated, or skipped.

Result summaries can include parsed, imported, duplicate, not selected, unsupported, skipped, and failed counts.

## Supported Areas

Supported imports cover activity, heart, body, hydration, nutrition, sleep, mindfulness, vitals, and cycle records where Health Connect has compatible record types and write permissions are granted.

Unsupported or incompatible records are skipped with diagnostics rather than forcing partial data into the wrong Health Connect type.

Workout imports include `WorkoutStatistics` totals when available. Walking speed records map to Health Connect speed samples. Apple workout GPX route geometry is written as a Health Connect exercise route when a workout references a route file and the route has at least two valid points.

Apple GPX route timestamps are not trusted for Health Connect writes because they can be duplicated or outside the workout interval. OpenVitals synthesizes strictly increasing route point times inside the workout start/end window, weighted by cumulative route distance, at the millisecond precision Health Connect stores route locations with; this keeps paused or stationary GPS segments (repeated identical coordinates) from collapsing onto the same timestamp, which would otherwise break route reads and re-import duplicate detection. Placeholder all-zero altitude values are omitted.

## Duplicate And Overlap Handling

The importer skips exact duplicates inside the same export and checks existing imported Health Connect records by deterministic `clientRecordId` before writing selected records.

For additive metrics where double-counting is common, such as steps, active calories, and distance, overlapping records from multiple Apple Health sources are merged conservatively. Lower-priority samples that are already mostly covered by another source are skipped with an `overlap_cross_source` diagnostic. Workout distance and active calorie totals are not written when overlapping raw records already cover the same workout interval.

## Reports And Diagnostics

After an import finishes, Settings can download a full text report. The report starts with a short summary, then includes selected categories, importer logs, per-category counts, per-type counts, grouped diagnostics, raw diagnostics, and worker logs. A recovered truncated workout-route section is shown as a partial-route warning and `route_archive_truncated` diagnostic rather than failing the health-record import. If the import fails before completion, the failure report includes the summary, worker logs, and full exception stack trace.

These import reports are intentionally not sanitized beyond Android WorkManager output limits for inline status. They are explicit user exports for troubleshooting and may include file names, timestamps, record values, failure details, and stack traces. Grouped diagnostic counts (for example, how many records were skipped for a given reason) are always complete; the raw per-record diagnostic log underneath is capped at 1,000 entries per source so that re-importing an already-imported export cannot grow the report to an unbounded size.

## Local Smoke Test

Contributors can run the importer parser/converter on a desktop JVM without building or installing the whole app:

```bash
./gradlew app:testCiUnitTest \
  --tests tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportSmokeTest \
  -PappleHealthExport=/path/to/export.zip \
  --console=plain
```

The `appleHealthExport` value may point to `export.zip`, `export.xml`, or an unzipped Apple Health export directory. The smoke test parses XML, parses GPX route files when present, converts supported records, and prints counts including parsed records, unsupported entries, skipped records, failures, route-backed sessions, and GPX files.

## Data Ownership

Imported records are written to Health Connect. OpenVitals does not upload the export to an OpenVitals server and does not provide a bulk rollback after records are written.
