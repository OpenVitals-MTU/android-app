/// The isolate-agnostic core of the background Apple Health import, ported from
/// the Kotlin `AppleHealthImportWorker.doWork()`.
///
/// The Kotlin worker *is* the orchestration: WorkManager hands it a `Context`,
/// it resolves the service through a Hilt entry point, loads the checkpoint,
/// imports, writes the report and clears its staged state. Its Flutter analogue
/// is a foreground-service isolate (see `apple_health_import_task_handler.dart`),
/// which cannot be unit-tested at all — no plugin channels, no `TaskHandler`
/// lifecycle. So everything the worker does *apart* from the notification and
/// the isolate plumbing lives here, as a plain async function over injected
/// collaborators: [runAppleHealthImportJob].
///
/// The one invariant this function exists to protect: Health Connect access is
/// resolved **before** the import runs. `HealthDataSource.cachedAvailability`
/// starts at `notSupported` and every repository gates its writes on it, so an
/// import that skips [AppleHealthImportJobInputs.resolveHealthAccess] writes
/// nothing at all while cheerfully reporting success.
///
/// This file also owns the wire format between the two isolates: the inputs are
/// handed over as `FlutterForegroundTask.saveData` primitives, and progress /
/// result / error come back as flat `Map`s, exactly as the Kotlin worker
/// flattens them into WorkManager `Data`.
library;

import 'dart:async';
import 'dart:io';

import 'apple_health_import_checkpoint_store.dart';
import 'apple_health_import_error_formatter.dart';
import 'apple_health_import_models.dart';
import 'apple_health_import_report_store.dart';
import 'apple_health_import_service.dart';
import 'apple_health_import_staging_store.dart';

// ── Isolate inputs (FlutterForegroundTask.saveData / getData) ────────────────
//
// `saveData` only persists int/double/String/bool (it silently returns `false`
// for anything else), so the selected-category set travels as a comma-joined
// list of enum names — the Kotlin worker's `putStringArray(KeySelectedCategories)`.

/// Absolute path of the staged export the task must import.
const String kAppleHealthImportKeyStagedPath = 'apple_health_import_staged_path';

/// `uri|displayName|size` — the identity a checkpoint is keyed by.
const String kAppleHealthImportKeySourceKey = 'apple_health_import_source_key';

/// Comma-joined [AppleHealthImportCategory] names.
const String kAppleHealthImportKeyCategories = 'apple_health_import_categories';

/// The percent denominator (Kotlin `KeyExpectedSelectedRecords`).
const String kAppleHealthImportKeyExpectedSelectedRecords =
    'apple_health_import_expected_selected_records';

/// The scan denominator (Kotlin `KeyExpectedParsedElements`).
const String kAppleHealthImportKeyExpectedParsedElements =
    'apple_health_import_expected_parsed_elements';

/// `true` while the foreground service belongs to an Apple Health import.
///
/// There is exactly one `ForegroundService` in the app, so this flag is what
/// tells a running service owned by *us* apart from one owned by an activity
/// recording — see `apple_health_import_foreground_controller.dart`.
const String kAppleHealthImportKeyActive = 'apple_health_import_active';

// ── Isolate outputs (FlutterForegroundTask.sendDataToMain) ───────────────────

/// Discriminator of every payload the task sends back to the main isolate.
const String kAppleHealthImportEventKey = 'appleHealthImportEvent';
const String kAppleHealthImportEventProgress = 'progress';
const String kAppleHealthImportEventResult = 'result';
const String kAppleHealthImportEventError = 'error';

/// Encodes a category set for [kAppleHealthImportKeyCategories].
String encodeAppleHealthImportCategories(
  Set<AppleHealthImportCategory> categories,
) =>
    (categories.map((category) => category.name).toList()..sort()).join(',');

/// Decodes [kAppleHealthImportKeyCategories], falling back to every category
/// (the Kotlin `selectedCategoriesFromData` default).
Set<AppleHealthImportCategory> decodeAppleHealthImportCategories(String? value) {
  if (value == null || value.isEmpty) return allAppleHealthImportCategories;
  final decoded = <AppleHealthImportCategory>{};
  for (final name in value.split(',')) {
    for (final category in AppleHealthImportCategory.values) {
      if (category.name == name) decoded.add(category);
    }
  }
  return decoded.isEmpty ? allAppleHealthImportCategories : decoded;
}

/// Flattens [progress] for the port (Kotlin `AppleHealthImportProgress.toData`).
Map<String, Object> encodeAppleHealthImportProgress(
  AppleHealthImportProgress progress, {
  required String event,
  bool workoutRoutesIncomplete = false,
}) =>
    <String, Object>{
      kAppleHealthImportEventKey: event,
      'phase': progress.phase.name,
      'parsedRecords': progress.parsedRecords,
      'parsedWorkouts': progress.parsedWorkouts,
      'parsedCorrelations': progress.parsedCorrelations,
      'parsedActivitySummaries': progress.parsedActivitySummaries,
      'convertedRecords': progress.convertedRecords,
      'importedRecords': progress.importedRecords,
      'duplicateSkippedRecords': progress.duplicateSkippedRecords,
      'notSelectedRecords': progress.notSelectedRecords,
      'unsupportedElements': progress.unsupportedElements,
      'skippedRecords': progress.skippedRecords,
      'failedRecords': progress.failedRecords,
      'expectedSelectedRecords': progress.expectedSelectedRecords,
      'expectedParsedElements': progress.expectedParsedElements,
      'workoutRoutesIncomplete': workoutRoutesIncomplete,
    };

/// The inverse of [encodeAppleHealthImportProgress]; `null` when the payload is
/// not one of ours (Kotlin `progressFromData`).
AppleHealthImportProgress? decodeAppleHealthImportProgress(Object? payload) {
  if (payload is! Map) return null;
  final phaseName = payload['phase'];
  AppleHealthImportPhase? phase;
  for (final candidate in AppleHealthImportPhase.values) {
    if (candidate.name == phaseName) phase = candidate;
  }
  if (phase == null) return null;
  int at(String key) {
    final value = payload[key];
    final parsed = value is int ? value : int.tryParse('$value') ?? 0;
    return parsed < 0 ? 0 : parsed;
  }

  return AppleHealthImportProgress(
    phase: phase,
    parsedRecords: at('parsedRecords'),
    parsedWorkouts: at('parsedWorkouts'),
    parsedCorrelations: at('parsedCorrelations'),
    parsedActivitySummaries: at('parsedActivitySummaries'),
    convertedRecords: at('convertedRecords'),
    importedRecords: at('importedRecords'),
    duplicateSkippedRecords: at('duplicateSkippedRecords'),
    notSelectedRecords: at('notSelectedRecords'),
    unsupportedElements: at('unsupportedElements'),
    skippedRecords: at('skippedRecords'),
    failedRecords: at('failedRecords'),
    expectedSelectedRecords: at('expectedSelectedRecords'),
    expectedParsedElements: at('expectedParsedElements'),
  );
}

/// Rebuilds the (counter-only) result the card renders from a `result` payload.
///
/// The per-type summaries, diagnostics and the report text are deliberately not
/// sent across the port — the report can be several megabytes. It is persisted
/// by the task through [AppleHealthImportReportStore] and read back in the main
/// isolate, exactly as Kotlin hands back a `reportPath` instead of the text.
AppleHealthImportResult? decodeAppleHealthImportResult(
  Object? payload,
  String reportText,
) {
  final progress = decodeAppleHealthImportProgress(payload);
  if (progress == null || payload is! Map) return null;
  return AppleHealthImportResult(
    parsedRecords: progress.parsedRecords,
    parsedWorkouts: progress.parsedWorkouts,
    parsedCorrelations: progress.parsedCorrelations,
    parsedActivitySummaries: progress.parsedActivitySummaries,
    convertedRecords: progress.convertedRecords,
    importedRecords: progress.importedRecords,
    duplicateSkippedRecords: progress.duplicateSkippedRecords,
    notSelectedRecords: progress.notSelectedRecords,
    unsupportedElements: progress.unsupportedElements,
    skippedRecords: progress.skippedRecords,
    failedRecords: progress.failedRecords,
    workoutRoutesIncomplete: payload['workoutRoutesIncomplete'] == true,
    typeSummaries: const [],
    diagnostics: const [],
    shareableReportText: reportText,
  );
}

/// The `error` payload (Kotlin `AppleHealthImportWorker.errorData`).
Map<String, Object> encodeAppleHealthImportError(Object error) =>
    <String, Object>{
      kAppleHealthImportEventKey: kAppleHealthImportEventError,
      'error': AppleHealthImportErrorFormatter.details(error),
      'permissionDenied': AppleHealthImportErrorFormatter.isPermissionDenied(error),
    };

/// A progress snapshot built from a finished import (Kotlin
/// `AppleHealthImportResult.toProgress`).
AppleHealthImportProgress appleHealthImportProgressOf(
  AppleHealthImportResult result, {
  required AppleHealthImportPhase phase,
  required int expectedSelectedRecords,
  int expectedParsedElements = 0,
}) =>
    AppleHealthImportProgress(
      phase: phase,
      parsedRecords: result.parsedRecords,
      parsedWorkouts: result.parsedWorkouts,
      parsedCorrelations: result.parsedCorrelations,
      parsedActivitySummaries: result.parsedActivitySummaries,
      convertedRecords: result.convertedRecords,
      importedRecords: result.importedRecords,
      duplicateSkippedRecords: result.duplicateSkippedRecords,
      notSelectedRecords: result.notSelectedRecords,
      unsupportedElements: result.unsupportedElements,
      skippedRecords: result.skippedRecords,
      failedRecords: result.failedRecords,
      expectedSelectedRecords: expectedSelectedRecords,
      expectedParsedElements: expectedParsedElements,
    );

// ── The job ──────────────────────────────────────────────────────────────────

/// What [runAppleHealthImportJob] produced: exactly one of [result] / [error].
class AppleHealthImportJobOutcome {
  const AppleHealthImportJobOutcome.success(AppleHealthImportResult this.result)
      : error = null;

  const AppleHealthImportJobOutcome.failure(Object this.error) : result = null;

  final AppleHealthImportResult? result;
  final Object? error;

  bool get isSuccess => result != null;
}

/// Everything [runAppleHealthImportJob] needs; the isolate builds these by hand
/// (no Riverpod, no drift), tests pass fakes.
class AppleHealthImportJobInputs {
  const AppleHealthImportJobInputs({
    required this.service,
    required this.stagingStore,
    required this.checkpointStore,
    required this.reportStore,
    required this.resolveHealthAccess,
    required this.stagedFile,
    required this.sourceKey,
    required this.selectedCategories,
    this.expectedSelectedRecords = 0,
    this.expectedParsedElements = 0,
  });

  final AppleHealthImportService service;
  final AppleHealthImportStagingStore stagingStore;
  final AppleHealthImportCheckpointStore checkpointStore;
  final AppleHealthImportReportStore reportStore;

  /// Resolves Health Connect availability + granted permissions before a single
  /// record is written. See the library comment: this is not optional.
  final Future<void> Function() resolveHealthAccess;

  final File stagedFile;
  final String sourceKey;
  final Set<AppleHealthImportCategory> selectedCategories;
  final int expectedSelectedRecords;

  /// The analysis pass's element total — the scan percent's denominator.
  final int expectedParsedElements;
}

/// Runs one Apple Health import end to end: resolve Health Connect access, load
/// the resume checkpoint, import, persist the report, and clear the staged state
/// on success — the body of Kotlin's `AppleHealthImportWorker.doWork()`.
///
/// Never throws. On failure the staged export **and** the checkpoint are
/// deliberately kept, so the next run resumes from the last committed batch
/// instead of re-importing everything.
Future<AppleHealthImportJobOutcome> runAppleHealthImportJob(
  AppleHealthImportJobInputs inputs, {
  void Function(AppleHealthImportProgress progress)? onProgress,
}) async {
  try {
    // MUST come before the import. `cachedAvailability` starts at
    // `notSupported`, and the import repository gates every write on it, so an
    // import that skips this silently writes nothing and still reports success.
    await inputs.resolveHealthAccess();

    // Only a checkpoint written for this exact export *and* this exact category
    // selection may be resumed; anything else starts clean.
    final stored = await inputs.checkpointStore.load(
      inputs.sourceKey,
      inputs.selectedCategories,
    );
    final resumeCheckpoint = stored ??
        AppleHealthImportCheckpoint(
          sourceKey: inputs.sourceKey,
          selectedCategories: inputs.selectedCategories,
        );

    // The service hands checkpoints to a *synchronous* callback, so the writes
    // are chained: an earlier save must never land after a later one and wind
    // the checkpoint backwards.
    var checkpointWrites = Future<void>.value();
    final result = await inputs.service.importAppleHealthExport(
      inputs.stagedFile,
      selectedCategories: inputs.selectedCategories,
      resumeCheckpoint: resumeCheckpoint,
      onCheckpoint: (checkpoint) {
        checkpointWrites = checkpointWrites
            .then((_) => inputs.checkpointStore.save(checkpoint))
            .catchError((_) {
          // A checkpoint we could not persist only costs a re-import of the
          // batch; it must never fail the import itself.
        });
      },
      onProgress: (progress) {
        // The service emits progress without the expected totals, so re-seed
        // them here to keep the percent getter meaningful (Kotlin re-seeds them
        // in the worker's progress callback for the same reason).
        onProgress?.call(progress.copyWith(
          expectedSelectedRecords: inputs.expectedSelectedRecords,
          expectedParsedElements: inputs.expectedParsedElements,
        ));
      },
    );
    await checkpointWrites;

    onProgress?.call(appleHealthImportProgressOf(
      result,
      phase: AppleHealthImportPhase.buildingReport,
      expectedSelectedRecords: inputs.expectedSelectedRecords,
      expectedParsedElements: inputs.expectedParsedElements,
    ));
    await inputs.reportStore.writeReport(result.shareableReportText);

    // Success: the staged copy, its metadata, any leftover `.tmp` and the
    // checkpoint all go away.
    await inputs.checkpointStore.clear();
    await inputs.stagingStore.clear();

    onProgress?.call(appleHealthImportProgressOf(
      result,
      phase: AppleHealthImportPhase.complete,
      expectedSelectedRecords: inputs.expectedSelectedRecords,
      expectedParsedElements: inputs.expectedParsedElements,
    ));
    return AppleHealthImportJobOutcome.success(result);
  } catch (error, stack) {
    // The staged copy and the checkpoint are deliberately kept so the next run
    // resumes from the last committed batch.
    try {
      await inputs.reportStore.writeFailure(
        buildAppleHealthFailureReportText(error, stackTrace: stack),
      );
    } catch (_) {
      // A report we could not persist must not mask the import failure.
    }
    return AppleHealthImportJobOutcome.failure(error);
  }
}
