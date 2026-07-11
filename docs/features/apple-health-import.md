# Apple Health Import

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/imports/applehealth/`, `lib/features/settings/cards/apple_health_import_card.dart`, `lib/data/repository/contract/apple_health_import_repository.dart` (+ `impl/apple_health_import_repository_impl.dart`).
> **Navigation:** `/settings/data_import` (`SettingsSection.dataImport`).
> **Related:** [Feature map](feature-map.md), [Settings and preferences](settings-and-preferences.md), [Permissions](../app/permissions.md).

OpenVitals can analyze supported records from Apple Health exports, let the user choose which categories to import, and then write the selected Health Connect-compatible records with duplicate checks, route handling, and a downloadable full report.

## Input Files

The import starts from Settings and accepts Apple Health `export.xml` or `export.zip` files. Zipped exports may also contain Apple workout route files under `workout-routes/*.gpx`; these are matched to `WorkoutRoute` file references in `export.xml`.

Large exports run as user-started background work so the import can continue after leaving the Settings screen. The picked file is first staged into app-private storage with a verified byte count (`apple_health_import_staging_store.dart`); nothing downstream ever holds the export as a byte list.

The background mechanism is a **foreground service** (`flutter_foreground_task`), driven by `apple_health_import_task_handler.dart`, not a WorkManager worker. That has one user-visible consequence the Kotlin app did not have: the app declares exactly one foreground service, so an Apple Health import **cannot start while a GPS activity recording is running** and is refused with an explicit message (`AppleHealthImportLaunch.serviceBusy` in `apple_health_import_foreground_controller.dart`). Where no foreground service exists (tests, desktop, a missing plugin) the import falls back to running in-process through the same job function the service isolate runs.

## Analyze And Import Flow

The app consumes the export as a stream of XML events (`package:xml`'s `parseEvents`, in `apple_health_import_parser.dart`) rather than materializing a DOM. A leading `<!DOCTYPE ...>` declaration is stripped before parsing, so Apple export doctypes never force entity or grammar resolution (`apple_health_import_xml_support.dart`), and Apple date strings keep their timezone offsets when records are mapped to Health Connect. A zipped export is read **sequentially** from its ZIP local file headers, so an archive whose tail is truncated is still usable: `export.xml` is extracted to a temp file, and damage inside a later `workout-routes/*.gpx` entry costs only the routes after it.

The first pass is analysis-only. It streams the selected export, maps Apple types to import categories, and shows detected import categories such as workouts and routes, activity metrics, heart, sleep, body, vitals, nutrition, hydration, mindfulness, and cycle tracking. Nothing is written to Health Connect during this analysis pass, and the analyzer does not parse GPX route geometry or construct Health Connect records just to build the checkbox menu: it runs with `parseRecordDetails: false` and `parseRouteFiles: false`, so it skips timestamps, metadata and numeric values and only needs each record's Apple type.

After analysis, the user can select or unselect categories with checkboxes. The import pass then re-parses the same staged export, parses GPX route geometry when route files are present (and skips the route entries entirely when Workouts is deselected), and writes only converted records whose Health Connect target category is selected. A known record type whose category is *not* selected is never materialized at all — which matters here, because this port buffers the parsed export in memory before converting it, so on a multi-gigabyte heart-rate export with only Body selected that filter is the difference between completing and running out of memory. The export **file** is never held as bytes; the parsed record list is.

The importer requests required Health Connect write permissions before writing accepted records into Health Connect. Large exports are processed with targeted lookups and time-window chunking to reduce memory pressure. Progress and result counts are shown while the import runs.

Import progress is split into explicit stages: scanning the export, converting records, checking duplicates, writing records, and building the report. During the import pass, the determinate percentage is based on the compatible records in the categories the user selected, not the full export. The downloadable report logs start and finish entries for these stages, including batch counts where available, so long-running imports can show whether time is being spent parsing, deduplicating, writing to Health Connect, or preparing diagnostics.

The import pipeline is **sequential**, not a producer/consumer pipeline: the export is parsed and converted in one pass, and the converted records are then deduplicated and written in 300-record batches, one batch at a time (`apple_health_import_service.dart`). Existing-record duplicate lookups are chunked by time window per record type to keep each Health Connect query bounded. (The Kotlin app overlapped parse/convert with the write coroutine; this port deliberately does not, and the file says so.)

Every successfully written batch writes a **checkpoint** (`apple_health_import_checkpoint_store.dart`). An import that is killed part-way can be resumed: the user re-picks the same export, the batch writer drops the records it already committed, and the imported/duplicate/failed and per-type totals carry over. A checkpoint is only reused when both the source key (uri, name, size) and the selected-category set match; anything else starts clean. This matters more than it did in Kotlin, because a foreground service is more likely to be killed than a WorkManager worker.

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

After an import finishes, Settings can download a full text report. The report starts with a short summary, then includes selected categories, importer logs, per-category counts, per-type counts, grouped diagnostics, raw diagnostics, and worker logs. If the import fails before completion, the failure report includes the summary, worker logs, and full exception stack trace.

These import reports are intentionally not sanitized. They are explicit user exports for troubleshooting and may include file names, timestamps, record values, failure details, and stack traces. Grouped diagnostic counts (for example, how many records were skipped for a given reason) are always complete; the raw per-record diagnostic log underneath is capped at 1,000 entries per source so that re-importing an already-imported export cannot grow the report to an unbounded size.

## Local Smoke Test

**There is no local smoke test in the Flutter app.** The retired Kotlin app had a desktop-JVM Gradle smoke test that could run the importer against a real `export.zip` (`-PappleHealthExport=...`) without building or installing the app. It has no Flutter equivalent, and the gap is tracked in [development.md](../engineering/development.md#known-gaps).

What does cover the importer today is unit tests over synthetic exports in `test/features/imports/applehealth/` — parser, converter, progress, staging, checkpointing, notification and error formatting. Nothing exercises a real multi-gigabyte export off-device.

## Data Ownership

Imported records are written to Health Connect. OpenVitals does not upload the export to an OpenVitals server and does not provide a bulk rollback after records are written.
