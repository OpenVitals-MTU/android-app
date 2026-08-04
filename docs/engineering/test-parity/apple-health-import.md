## test/features/imports/applehealth/apple_health_import_background_test.dart
Kotlin counterpart: app/src/main/kotlin/.../AppleHealthImportWorker.kt is the port target (WorkManager `doWork`); the `Data` payload helpers are covered by app/src/test/kotlin/tech/mmarca/openvitals/features/imports/applehealth/AppleHealthImportWorkerDataTest.kt; further coverage in AppleHealthImportServiceTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| resolves Health Connect access BEFORE the import runs | N/A-FRAMEWORK | — | Pins a Flutter health-plugin `cachedAvailability` bug; Kotlin repository talks to Health Connect directly, no resolve step exists |
| resumes from a checkpoint stored for the same source and selection | N/A-FRAMEWORK | — | `doWork` orchestration; service-level resume is ported (`service resumes from selected record checkpoint and writes remaining batches`), store-level load untested (see checkpoint-store file) |
| starts clean when no checkpoint is stored | N/A-FRAMEWORK | — | Zero-checkpoint construction lives inside `doWork` (WorkManager, not plain-JVM testable) |
| saves a checkpoint for every batch the import commits | N/A-FRAMEWORK | — | `doWork` wiring; per-batch checkpoint emission asserted at service level in AppleHealthImportServiceTest |
| a successful import writes the report and clears staging + checkpoint | N/A-FRAMEWORK | — | `doWork` sequence; staging clear itself covered by `staging cleanup deletes private export files but preserves selected source` |
| a failed import KEEPS staging + checkpoint so the next run resumes | N/A-FRAMEWORK | — | `doWork`; failure-report content ported via `worker failure report includes summary logs and full exception stack` |
| a failure while resolving Health Connect access never imports | N/A-FRAMEWORK | — | No resolve-access step in Kotlin |
| isolate payloads > category set survives the saveData round trip | PORTED | AppleHealthImportWorkerDataTest.kt: `category set survives the input data round trip` | Kotlin `AppleHealthImportWorker.inputData`/`selectedCategoriesFromData` untested (decode side private, but Data helpers are JVM-testable — `errorData` already is) |
| isolate payloads > progress survives the port round trip | PORTED | AppleHealthImportWorkerDataTest.kt: `progress survives the work data round trip` | `AppleHealthImportWorker.progressFromData` (incl. `expectedParsedElements` survival) untested |
| isolate payloads > the job re-seeds both expected totals onto every progress it emits | N/A-FRAMEWORK | — | Re-seeding happens inside `doWork`'s progress lambda |
| isolate payloads > the result payload carries the counters, the store carries the report | PORTED | AppleHealthImportWorkerDataTest.kt: `the result payload carries the counters and the store carries the report` | `AppleHealthImportWorker.resultFromData(data, reportText)` untested |
| isolate payloads > the error payload carries the details and the permission flag | PORTED | AppleHealthImportWorkerDataTest.kt: `the error payload carries the details and the permission flag` | permission_denied=false key now asserted |
| isolate payloads > a permission denial raises the flag the card acts on | PORTED | AppleHealthImportWorkerDataTest.kt: `a permission denial raises the flag the card acts on` | `errorData` with SecurityException → `KeyPermissionDenied=true` untested; plainly JVM-portable |

## test/features/imports/applehealth/apple_health_import_batch_channel_test.dart
Kotlin counterpart: none (Kotlin uses `kotlinx.coroutines.channels.Channel(capacity=2)` directly in AppleHealthImportService.kt)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| delivers batches to the consumer in FIFO order, then null on close | N/A-FRAMEWORK | — | Dart re-implementation of kotlinx Channel; ordering covered end-to-end by `service pipelines multiple batches in order and imports all records` |
| the consumer waits for a batch that is added later | N/A-FRAMEWORK | — | Library-provided semantics in Kotlin |
| awaitCapacity suspends the producer until the writer drains below cap | N/A-FRAMEWORK | — | Backpressure = `trySendBlocking` on a capacity-2 Channel (library behavior) |
| a writer failure releases a parked producer and re-throws on its turn | N/A-FRAMEWORK | — | Kotlin cancels the channel on writer failure; library behavior |

## test/features/imports/applehealth/apple_health_import_checkpoint_store_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/applehealth/AppleHealthImportCheckpointStoreTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| AppleHealthImportCheckpointStore > round-trips a checkpoint for the same source and categories | PORTED | AppleHealthImportCheckpointStoreTest.kt: `round-trips a checkpoint for the same source and categories` | Store is context+filesDir based, testable like the staging tests in AppleHealthImportServiceTest |
| AppleHealthImportCheckpointStore > is not reused when the source key differs | PORTED | AppleHealthImportCheckpointStoreTest.kt: `is not reused when the source key differs` |  |
| AppleHealthImportCheckpointStore > is not reused when the selected categories differ | PORTED | AppleHealthImportCheckpointStoreTest.kt: `is not reused when the selected categories differ` |  |
| AppleHealthImportCheckpointStore > load returns null when nothing was ever written | PORTED | AppleHealthImportCheckpointStoreTest.kt: `load returns null when nothing was ever written` |  |
| AppleHealthImportCheckpointStore > clear removes the checkpoint | PORTED | AppleHealthImportCheckpointStoreTest.kt: `clear removes the checkpoint` |  |

## test/features/imports/applehealth/apple_health_import_conversion_support_test.dart
Kotlin counterpart: none needed (helpers guard the Dart port against native Kotlin behavior)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| appleInstantToStableString... > drops the fractional part for a whole-second UTC instant | N/A-FRAMEWORK | — | Guards Dart emulation of `java.time.Instant.toString()`; Kotlin uses the JDK natively |
| appleInstantToStableString... > keeps milliseconds when they are non-zero | N/A-FRAMEWORK | — | Same |
| appleInstantToStableString... > normalises a non-UTC instant to UTC before formatting | N/A-FRAMEWORK | — | Same |
| stableSort > keeps input order among elements that tie on the sort key | N/A-FRAMEWORK | — | Dart `List.sort` is unstable; Kotlin's sort is stable by spec |
| stableSort > leaves a list of fewer than two elements untouched | N/A-FRAMEWORK | — | Same |

## test/features/imports/applehealth/apple_health_import_error_formatter_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/applehealth/AppleHealthImportErrorFormatterTest.kt (+ AppleHealthImportServiceTest.kt for the report case)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| AppleHealthImportErrorFormatter > summary includes the exception type when a message is missing | PORTED | AppleHealthImportErrorFormatterTest.kt: `summary includes exception type when message is missing` | |
| AppleHealthImportErrorFormatter > details includes the exception message and its cause chain | PORTED | AppleHealthImportErrorFormatterTest.kt: `details includes exception stack trace and cause` | |
| AppleHealthImportErrorFormatter > isPermissionDenied is true for a direct permission exception | PORTED | AppleHealthImportErrorFormatterTest.kt: `isPermissionDenied is true for a direct SecurityException` | SecurityException is the platform-appropriate analogue |
| AppleHealthImportErrorFormatter > isPermissionDenied is true when the permission exception is a wrapped cause | PORTED | AppleHealthImportErrorFormatterTest.kt: `isPermissionDenied is true when SecurityException is a wrapped cause` | |
| AppleHealthImportErrorFormatter > isPermissionDenied is false for unrelated errors | PORTED | AppleHealthImportErrorFormatterTest.kt: `isPermissionDenied is false for unrelated errors` | |
| AppleHealthImportReportStore > failure report includes summary, logs, and the full exception chain | PORTED | AppleHealthImportServiceTest.kt: `worker failure report includes summary logs and full exception stack` | Same Summary/Logs/Exception/Caused-by assertions |
| AppleHealthImportReportStore > round-trips the last report and failure via its file store | PORTED | AppleHealthImportErrorFormatterTest.kt: `report store round-trips the last report and failure via its file store` | Kotlin AppleHealthImportReportStore write/read (and empty-default read) has no direct test |

## test/features/imports/applehealth/apple_health_import_notification_test.dart
Kotlin counterpart: none testable (logic lives in private `AppleHealthImportWorker.buildNotification` using Android resources/NotificationCompat; no Robolectric in project)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| percent + a known export size shows scan progress | N/A-FRAMEWORK | — | Android notification/resource rendering; scan-denominator choice covered by AppleHealthImportProgressTest |
| the printed percent is the scan percent, not the selected one | N/A-FRAMEWORK | — | Scan-wins-over-selected percent rule covered by `percent uses raw scan progress when analyzed element total is known` |
| percent without a known export size shows selected-record progress | N/A-FRAMEWORK | — | Selected-denominator percent covered by `percent uses selected record total as denominator` |
| no percent falls back to phase + scanned/imported counters | N/A-FRAMEWORK | — | Null-percent case covered by `percent is unavailable until selected record total is known` |
| the phase label is the one the card shows | N/A-FRAMEWORK | — | `AppleHealthImportPhase.labelRes` is a pure resource-id map, string resolution needs Android |

## test/features/imports/applehealth/apple_health_import_parser_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/applehealth/AppleHealthImportServiceTest.kt (parser/converter cases live here)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| AppleHealthImportParser streaming zip > reads entries whose local header has no sizes (data descriptors) | PORTED | AppleHealthImportServiceTest.kt: `parser reads streaming zip entries with data descriptors and no central directory` | Exercised implicitly (ZipOutputStream fixtures write flag-bit-3 entries, java.util.zip reads them); no dedicated no-central-directory case |
| AppleHealthImportParser + converter > imports sleep category values as sleep stages | PORTED | AppleHealthImportServiceTest.kt: `parser and converter import sleep category values as sleep stages` | |
| ... > handles an apple export DOCTYPE without loading DTD grammar | PORTED | AppleHealthImportServiceTest.kt: `parser handles apple export doctype without loading dtd grammar` | |
| ... > repairs raw control characters and unescaped ampersands | PORTED | AppleHealthImportServiceTest.kt: `parser repairs raw control characters and unescaped ampersands in attribute values` | |
| ... > wraps a genuine well-formedness failure with surrounding text | PORTED | AppleHealthImportServiceTest.kt: `parser wraps a genuine well-formedness failure with the surrounding text` | OR-clause gone; cause AND message both asserted |
| ... > preserves timezone offsets on apple date strings | PORTED | AppleHealthImportServiceTest.kt: `parser preserves timezone offsets on apple date strings` | |
| ... > imports walking speed as speed samples (km/hr → m/s) | PORTED | AppleHealthImportServiceTest.kt: `parser and converter import walking speed as speed samples` | |
| ... > prefers blood pressure correlations | PORTED | AppleHealthImportServiceTest.kt: `parser and converter prefer blood pressure correlations` | |
| ... > reads workout statistics as workout distance and energy | PORTED | AppleHealthImportServiceTest.kt: `parser and converter read workout statistics as workout distance and energy` | |
| ... > drops workout energy totals when overlapping records exist from another source | PORTED | AppleHealthImportServiceTest.kt: `converter does not import workout energy totals when overlapping records exist from another source` | |
| ... > skips lower priority additive records mostly covered by another source | PORTED | AppleHealthImportServiceTest.kt: `converter skips lower priority additive records mostly covered by another source` | |
| ... > synthetic export fixture covers supported converter targets | PORTED | AppleHealthImportServiceTest.kt: `synthetic export fixture covers supported converter targets` | Same fixture in app/src/test/resources/apple_health |
| ... > reads a zipped apple export | PORTED | AppleHealthImportServiceTest.kt: `parser reads zipped apple export` | |
| ... > imports an apple workout route with synthesized times | PORTED | AppleHealthImportServiceTest.kt: `parser and converter import apple workout route with synthesized times` | |
| ... > synthesized route times stay strictly increasing at millisecond precision | PORTED | AppleHealthImportServiceTest.kt: `synthesized route times stay strictly increasing at millisecond precision` | |
| ... > light mode keeps counts but skips dates, metadata and numeric values | PORTED | AppleHealthImportServiceTest.kt: `parser light mode keeps counts but skips dates metadata and numeric values` | |

## test/features/imports/applehealth/apple_health_import_progress_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/applehealth/AppleHealthImportProgressTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| AppleHealthImportProgress.percent > is unavailable until selected record total is known | PORTED | AppleHealthImportProgressTest.kt: `percent is unavailable until selected record total is known` | |
| ... > uses selected record total as denominator | PORTED | AppleHealthImportProgressTest.kt: `percent uses selected record total as denominator` | |
| ... > does not count unselected records or generic skips as progress | PORTED | AppleHealthImportProgressTest.kt: `percent does not count unselected records or generic skips as selected progress` | |
| ... > uses raw scan progress when the analyzed element total is known | PORTED | AppleHealthImportProgressTest.kt: `percent uses raw scan progress when analyzed element total is known` | |
| ... > raw scan progress advances across unselected record sections | PORTED | AppleHealthImportProgressTest.kt: `raw scan progress advances across unselected record sections` | selectedPreparedRecords premise assertions now present |
| ... > a complete phase is 100 even with no totals at all | PORTED | AppleHealthImportProgressTest.kt: `a complete phase is 100 even with no totals at all` | Kotlin `percent` implements COMPLETE-before-denominator (AppleHealthImportModels.kt line 55) but no test pins the no-totals case |
| ... > reserves final steps for duplicate checks, writing and report | PORTED | AppleHealthImportProgressTest.kt: `percent reserves final steps for duplicate checks writing and report` | |

## test/features/imports/applehealth/apple_health_import_service_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/applehealth/AppleHealthImportServiceTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| AppleHealthImportService > skips duplicate records inside same export and includes report | PORTED | AppleHealthImportServiceTest.kt: `service skips duplicate records inside same export and includes report` | |
| ... > analysis detects import categories without writing | PORTED | AppleHealthImportServiceTest.kt: `service analysis detects import categories without writing` | |
| ... > analysis detects route categories without parsing gpx geometry | PORTED | AppleHealthImportServiceTest.kt: `service analysis detects route categories without parsing gpx geometry` | |
| ... > imports only selected categories after analysis | PORTED | AppleHealthImportServiceTest.kt: `service imports only selected categories after analysis` | |
| ... > skips large unselected sections before record materialization | PORTED | AppleHealthImportServiceTest.kt: `service skips large unselected sections before record materialization` | per-type heart-rate typeSummaries now asserted |
| ... > the scan percent climbs while the parser streams the export | PORTED | AppleHealthImportServiceTest.kt: `the scan percent climbs while the parser streams the export` | Kotlin service emits parse ticks (`ProgressReportElementInterval`) but no test asserts tick cadence or monotonic scan percent |
| ... > the analysis scan reports its running element count | PORTED | AppleHealthImportServiceTest.kt: `the analysis scan reports its running element count` | No Kotlin test of analysis-pass element ticks / null percent during analysis |
| ... > early-skipped records leave the element stack and correlations intact | PORTED | AppleHealthImportServiceTest.kt: `early-skipped records leave the element stack and correlations intact` | No Kotlin test that early-skips preserve correlation children and following records |
| ... > workout selection retains unselected samples needed for overlap checks | PORTED | AppleHealthImportServiceTest.kt: `workout selection retains unselected activity samples needed for overlap checks` | |
| ... > report aggregates repeated diagnostics and keeps later distinct groups | PORTED | AppleHealthImportServiceTest.kt: `service report aggregates repeated diagnostics and keeps later distinct groups` | |
| ... > pipelines multiple batches in order and imports all records | PORTED | AppleHealthImportServiceTest.kt: `service pipelines multiple batches in order and imports all records` | |
| ... > skips duplicates that appear in a later batch of the same export | PORTED | AppleHealthImportServiceTest.kt: `service skips duplicates that appear in a later batch of the same export` | |
| ... > unions parallel duplicate check chunks across types and time spans | PORTED | AppleHealthImportServiceTest.kt: `service unions parallel duplicate check chunks across types and time spans` | |
| ... > analysis streams zip when route files precede export xml | PORTED | AppleHealthImportServiceTest.kt: `service analysis streams zip when route files precede export xml` | |
| ... > a ZIP truncated inside a workout-route entry still imports the health records and flags workoutRoutesIncomplete | PORTED | AppleHealthImportServiceTest.kt: `service imports intact health records when zip ends during workout route` | Kotlin asserts more (diagnostic detail, warn log) |
| ... > a ZIP truncated before export.xml is read still hard-fails | PORTED | AppleHealthImportServiceTest.kt: `parser reports truncated zip exports with actionable apple health context` + `parser does not recover a truncated route before export xml` | Parser-level in Kotlin; no-insert assertion implicit |
| ... > an unreadable route is not reported when workouts are deselected | PORTED | AppleHealthImportServiceTest.kt: `service skips damaged workout routes when workouts are not selected` | |
| ... > saves a checkpoint after every batch and resumes without rewriting committed records | PORTED | AppleHealthImportServiceTest.kt: `service saves a checkpoint after every batch on a clean run` + `service resumes from selected record checkpoint and writes remaining batches` | clean-run cadence now asserted |
| ... > a checkpoint that skips everything writes nothing at all | PORTED | AppleHealthImportServiceTest.kt: `a checkpoint that skips everything writes nothing at all` | No Kotlin test that a fully-committed checkpoint produces zero inserts while totals stay 400 |

## test/features/imports/applehealth/apple_health_import_staging_store_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/applehealth/AppleHealthImportServiceTest.kt (staging cases)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| AppleHealthImportStagingStore > stages the picked export and verifies the copied byte count | PORTED | AppleHealthImportServiceTest.kt: `staging store reuses matching local export copy` | staged content bytes and no leftover .tmp now asserted |
| ... > throws AppleHealthExportCopyException on a short provider copy | PORTED | AppleHealthImportServiceTest.kt: `staging store rejects a short provider copy` | .tmp cleanup and the download-fully hint now asserted |
| ... > reuses an existing staged copy when the fingerprint matches | PORTED | AppleHealthImportServiceTest.kt: `staging store reuses matching local export copy` | |
| ... > re-stages when the fingerprint no longer matches | PORTED | AppleHealthImportServiceTest.kt: `staging store re-stages when the fingerprint no longer matches` | Only failure-triggered re-stage is tested (`failed staged analysis clears the local copy before retry`); fingerprint-mismatch re-stage is not |
| ... > clear removes the staged export, metadata, leftover tmp and the dir | PORTED | AppleHealthImportServiceTest.kt: `staging cleanup deletes private export files but preserves selected source` | Properties-file removal subsumed by directory-gone assertion |
| ... > source key is uri\|displayName\|size | PORTED | AppleHealthImportCheckpointStoreTest.kt: `source key is uri displayName and size joined with pipes` | Kotlin `AppleHealthImportCheckpointStore.sourceKey(uri, fingerprint)` untested |

## test/features/imports/applehealth/apple_health_import_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/settings/SettingsViewModelTest.kt (Apple import lives in SettingsViewModel; import runs via WorkManager, not in-process)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| analyze summarises the export and pre-selects what it found | DIVERGED | SettingsViewModelTest.kt: `re-selecting the same file reuses the previous analysis` | Analysis + pre-selection asserted only as a precondition of the reuse test; no error/isBusy assertions |
| a failed analysis reports the error and forgets the staged pick | PORTED | SettingsViewModelTest.kt: `a failed analysis reports the error and forgets the staged pick` | error text, permissionDenied flag, busy flag cleared, and the follow-up import enqueues nothing |
| importing the selected categories keeps the whole report | DIVERGED | SettingsViewModelTest.kt: `apple import observer uses current import work over older failures` | Enqueue with selected categories + expected counts verified; result/report retention via WorkInfo not asserted |
| a failed import reports the failure and offers it for saving | DIVERGED | SettingsViewModelTest.kt: `apple import observer uses current import work over older failures` | Error surfaced from current work asserted; report-for-save and staged-export retention not |
| closing the card mid-load does not throw out of the unawaited read | N/A-FRAMEWORK | — | Riverpod ref-after-dispose bug, no Kotlin analogue |
| importing without an analysis does nothing | PORTED | SettingsViewModelTest.kt: `importing without an analysis does nothing` | asserts the settled state is unchanged and nothing is enqueued |

## test/features/imports/applehealth/apple_health_import_xml_support_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/applehealth/AppleHealthImportXmlSupportTest.kt (direct `XmlCharacterSanitizingReader` unit tests) plus indirect coverage via AppleHealthImportServiceTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| escapes a bare & split across a chunk boundary | PORTED | AppleHealthImportXmlSupportTest.kt: `escapes a bare ampersand mid-text` | direct Reader test drained through a 3-char buffer |
| does not re-escape a real entity split across a chunk boundary | PORTED | AppleHealthImportXmlSupportTest.kt: `does not re-escape a real entity` | `isEntityReferenceAhead` (entity preservation) has no direct or indirect test |
| escapes a bare & at the very end of the stream | PORTED | AppleHealthImportXmlSupportTest.kt: `escapes a bare ampersand at the very end of the stream` | End-of-stream pending-`&` flush untested |
| numeric and hex character references split across chunks stay intact | PORTED | AppleHealthImportXmlSupportTest.kt: `numeric and hex character references stay intact` | `&#65;` / `&#x41;` preservation untested |
| strips a disallowed control character mid-text | PORTED | AppleHealthImportXmlSupportTest.kt: `strips a disallowed control character mid-text` | direct Reader test |
| recentContext reports the trailing emitted text | PORTED | AppleHealthImportXmlSupportTest.kt: `recentContext reports the trailing emitted text` | direct Reader test |

### Counts
PORTED: 72, DIVERGED: 3, MISSING: 0, N/A-WIDGET: 0, N/A-FRAMEWORK: 23

### Missing list

None — both remaining cases were ported into `SettingsViewModelTest.kt` (the Kotlin home of
the Apple-import card).
