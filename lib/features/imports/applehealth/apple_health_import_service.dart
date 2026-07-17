/// Two-pass Apple Health import orchestration, ported from the Kotlin
/// `AppleHealthImportService.kt`.
///
/// The Kotlin service streams the export through a SAX consumer and overlaps
/// parse/convert with a duplicate-check + insert writer coroutine. This Dart
/// port keeps the same observable contract — analyze (count/summarize by
/// category without writing) and import (convert, in-file + existing dedup,
/// batched insert, shareable report) — but converts the whole parsed export in
/// one pass (`AppleHealthImportConverter.convert`) and processes the converted
/// records in sequential 300-record batches, which is simpler and matches the
/// tests. A real WorkManager-style background worker is intentionally NOT
/// ported; callers run this async service directly (e.g. from an import screen).
///
/// Both entry points take the **staged** export [File] produced by
/// `AppleHealthImportStagingStore` — never a `List<int>`. A multi-gigabyte
/// export must never be held in RAM, and only a real file can be read
/// sequentially when the archive turns out to be truncated.
///
/// [importAppleHealthExport] is resumable (Kotlin 1.9.0 `3d6b8dd`): pass the
/// checkpoint the previous run left behind and the batch writer skips the
/// records it already committed, carrying the running totals forward.
library;

import 'dart:async';
import 'dart:collection';
import 'dart:io';

import '../../../core/result/app_failure.dart';
import '../../../core/result/result.dart';
import '../../../data/repository/contract/apple_health_import_repository.dart';
import 'apple_health_import_categories.dart';
import 'apple_health_import_checkpoint_store.dart';
import 'apple_health_import_converter.dart';
import 'apple_health_import_conversion_support.dart';
import 'apple_health_import_error_formatter.dart';
import 'apple_health_import_models.dart';
import 'apple_health_import_parser.dart';
import 'apple_health_import_report_store.dart';
import 'apple_health_import_types.dart';

typedef AppleHealthImportProgressCallback = void Function(
    AppleHealthImportProgress progress);

/// Invoked after every successfully written batch so the caller can persist the
/// checkpoint (Kotlin's `onCheckpoint`).
typedef AppleHealthImportCheckpointCallback = void Function(
    AppleHealthImportCheckpoint checkpoint);

const int _convertedBatchSize = 300;
const int _bufferedRecordBatchSize = 2000;
const int _maxRawDiagnostics = 1000;
const int _maxDuplicateCheckSpanSeconds = 6 * 60 * 60;

/// Unwraps an import write, raising a *typed* permission exception when that is
/// what went wrong.
///
/// The card can offer to fix a missing permission; it cannot do anything about
/// a malformed record. Everything else is rethrown as the original throwable,
/// because the duplicate classifier and the error formatter are written against
/// it.
T _orThrowImport<T>(Result<T> result) {
  switch (result) {
    case Ok(:final value):
      return value;
    case Err(:final PermissionFailure failure):
      throw AppleHealthImportPermissionException(
        failure.message,
        cause: failure.cause,
      );
    case Err(:final failure):
      Error.throwWithStackTrace(
        failure.cause ?? StateError(failure.toString()),
        failure.stackTrace ?? StackTrace.current,
      );
  }
}

class AppleHealthImportService {
  AppleHealthImportService(this._repository);

  final AppleHealthImportRepository _repository;

  Future<AppleHealthImportAnalysisResult> analyzeAppleHealthExport(
    File file, {
    AppleHealthImportProgressCallback? onProgress,
  }) async {
    final importLogs = <String>[];
    void log(String message) => importLogs.add(_infoLog(message));
    final mindfulnessAvailable = _repository.isMindfulnessAvailable();
    log('Apple Health analysis requested');

    // Stream the export through a consumer that categorises and counts each
    // element then immediately drops it, so a multi-gigabyte export is never
    // accumulated in RAM. This pass is the one the user hit an OOM on: the old
    // code materialised every record into `parsed.records` and looped over it.
    final consumer = _AnalyzeConsumer(mindfulnessAvailable);
    final parsed = await _parseExport(
      file,
      importLogs,
      onProgress,
      AppleHealthParseOptions(
        parseRouteFiles: false,
        parseRecordDetails: false,
        // The analysis pass is the one that *measures* the element total, so it
        // has no denominator and its bar stays indeterminate — but it is just as
        // long as the import pass, and without these ticks its "Scanned N items"
        // line would read 0 for the whole scan.
        onElementsParsed: onProgress == null
            ? null
            : (parsedElements) => onProgress(_scanProgress(parsedElements)),
      ),
      consumer: consumer,
    );

    final typeStats = consumer.typeStats;
    final categoryStats = consumer.categoryStats;
    final diagnostics = consumer.diagnostics;
    final diagnosticSummaries = consumer.diagnosticSummaries;

    // Parsed totals still come from the handler's aggregate type counts (it tracks
    // them even when the consumer drops the records), so seed `.parsed` as before.
    parsed.parsedTypeCounts.forEach((type, count) {
      _stat(typeStats, type).parsed += count;
    });

    final summaries = _toTypeSummaries(typeStats);
    final totals = _totals(summaries);
    final categorySummaries = _categorySummaries(categoryStats);
    onProgress?.call(_progress(
      AppleHealthImportPhase.buildingReport,
      parsed: parsed,
      converted: totals.converted,
      unsupported: totals.unsupported,
    ));
    final reportText = _buildReportText(
      parsed: parsed,
      imported: 0,
      selectedCategories: null,
      summaries: summaries,
      categorySummaries: categorySummaries,
      diagnostics: diagnostics,
      diagnosticSummaries: diagnosticSummaries.values.toList(),
      importLogs: importLogs,
    );
    return AppleHealthImportAnalysisResult(
      parsedRecords: parsed.parsedRecords,
      parsedWorkouts: parsed.parsedWorkouts,
      parsedCorrelations: parsed.parsedCorrelations,
      parsedActivitySummaries: parsed.parsedActivitySummaries,
      convertedRecords: totals.converted,
      unsupportedElements: totals.unsupported,
      skippedRecords: totals.skipped,
      failedRecords: totals.failed,
      categorySummaries: categorySummaries,
      typeSummaries: summaries,
      diagnostics: diagnostics,
      shareableReportText: reportText,
    );
  }

  Future<AppleHealthImportResult> importAppleHealthExport(
    File file, {
    Set<AppleHealthImportCategory> selectedCategories =
        allAppleHealthImportCategories,
    AppleHealthImportProgressCallback? onProgress,
    AppleHealthImportCheckpoint? resumeCheckpoint,
    AppleHealthImportCheckpointCallback? onCheckpoint,
  }) async {
    final importLogs = <String>[];
    void log(String message) => importLogs.add(_infoLog(message));
    log('Apple Health import requested stagedFile=${file.path} '
        'selectedCategories=${selectedCategories.map((c) => c.name).join(', ')}');
    if (resumeCheckpoint != null &&
        resumeCheckpoint.committedSelectedRecords > 0) {
      log('Resuming Apple Health import checkpoint committedSelectedRecords='
          '${resumeCheckpoint.committedSelectedRecords} '
          'imported=${resumeCheckpoint.importedRecords} '
          'duplicates=${resumeCheckpoint.duplicateSkippedRecords} '
          'failed=${resumeCheckpoint.failedRecords}');
    }

    final workoutsSelected =
        selectedCategories.contains(AppleHealthImportCategory.workouts);
    final converter = AppleHealthImportConverter(
      mindfulnessAvailable: _repository.isMindfulnessAvailable(),
      diagnosticLimit: _maxRawDiagnostics,
      reportUnavailableWorkoutRoutes: workoutsSelected,
    );
    final typeStats = converter.typeStats;

    // Category stats include not-selected and early-skipped records (Kotlin), so
    // the streaming consumer that books them owns this map.
    final categoryStats = <AppleHealthImportCategory, _MutableCategorySummary>{};

    final serviceDiagnostics = <AppleHealthImportDiagnostic>[];
    final serviceDiagnosticSummaries =
        <String, AppleHealthImportDiagnosticSummary>{};

    // Resume state (Kotlin `ConvertedBatchWriter`): the previous run's write totals
    // are carried forward, and the first `committedSelectedRecords` converted+
    // selected records are dropped instead of being written twice. Seeded *before*
    // the parse because batches are now written during it (streamed off disk).
    resumeCheckpoint?.typeStats.forEach((appleType, stats) {
      final stat = _stat(typeStats, appleType);
      stat.imported = stats.imported;
      stat.duplicateSkipped = stats.duplicateSkipped;
      stat.failed = stats.failed;
    });
    var imported = resumeCheckpoint?.importedRecords ?? 0;
    var duplicate = resumeCheckpoint?.duplicateSkippedRecords ?? 0;
    var failed = resumeCheckpoint?.failedRecords ?? 0;
    final recordsToSkip = resumeCheckpoint?.committedSelectedRecords ?? 0;
    var skipRemaining = recordsToSkip;
    var committedSelectedRecords = recordsToSkip;

    void saveCheckpoint() {
      final checkpoint = resumeCheckpoint;
      if (checkpoint == null || onCheckpoint == null) return;
      onCheckpoint(AppleHealthImportCheckpoint(
        sourceKey: checkpoint.sourceKey,
        selectedCategories: checkpoint.selectedCategories,
        committedSelectedRecords: committedSelectedRecords,
        importedRecords: imported,
        duplicateSkippedRecords: duplicate,
        failedRecords: failed,
        typeStats: {
          for (final entry in typeStats.entries)
            if (entry.value.imported > 0 ||
                entry.value.duplicateSkipped > 0 ||
                entry.value.failed > 0)
              entry.key: AppleHealthImportCheckpointTypeStats(
                imported: entry.value.imported,
                duplicateSkipped: entry.value.duplicateSkipped,
                failed: entry.value.failed,
              ),
        },
      ));
    }

    // The streaming import pipeline: the consumer converts + selects records as
    // they are parsed and hands full 300-record batches to [readyBatches]; the
    // drain (run at every XML chunk boundary and after the final flush) writes them
    // to Health Connect. Nothing accumulates the whole export — memory stays
    // bounded to roughly one parse chunk of records plus one batch in flight. This
    // replaces `converter.convert(parsed)` + a `selected` list, both of which
    // materialised the entire export before writing.
    final readyBatches = Queue<List<ConvertedAppleRecord>>();
    final consumer = _ImportConsumer(
      converter: converter,
      selectedCategories: selectedCategories,
      categoryStats: categoryStats,
      typeStats: typeStats,
      onReadyBatch: readyBatches.add,
    );

    // Progress snapshots read the consumer's live counters (Kotlin's writing-state
    // `progressSnapshot`), since the parse — and therefore `parsed` — has not
    // finished while batches are being written.
    AppleHealthImportProgress writeProgress(AppleHealthImportPhase phase) =>
        AppleHealthImportProgress(
          phase: phase,
          parsedRecords: consumer.parsedRecords,
          parsedWorkouts: consumer.parsedWorkouts,
          parsedCorrelations: consumer.parsedCorrelations,
          parsedActivitySummaries: consumer.parsedActivitySummaries,
          convertedRecords: consumer.convertedRecords,
          importedRecords: imported,
          duplicateSkippedRecords: duplicate,
          notSelectedRecords: consumer.notSelectedRecords,
          unsupportedElements: converter.unsupportedCount,
          skippedRecords: converter.skippedCount,
          failedRecords: converter.invalidCount + failed,
        );

    Future<void> processReadyBatch(List<ConvertedAppleRecord> rawBatch) async {
      // Kotlin `withResumeSkip`: drop records an earlier run already committed.
      final List<ConvertedAppleRecord> batch;
      if (skipRemaining <= 0) {
        batch = rawBatch;
      } else if (skipRemaining >= rawBatch.length) {
        skipRemaining -= rawBatch.length;
        if (skipRemaining == 0) {
          log('Finished skipping previously committed selected records from '
              'checkpoint count=$recordsToSkip');
        }
        return;
      } else {
        final partialSkip = skipRemaining;
        skipRemaining = 0;
        log('Finished skipping previously committed selected records from '
            'checkpoint count=$recordsToSkip partialBatchSkip=$partialSkip');
        batch = rawBatch.sublist(partialSkip);
      }

      onProgress?.call(writeProgress(AppleHealthImportPhase.checkingDuplicates));
      log('Stage started: Checking duplicates batchRecords=${batch.length}');
      final deduplicated = _deduplicateWithinImport(
        batch,
        serviceDiagnostics,
        serviceDiagnosticSummaries,
        typeStats,
      );
      final inFileDuplicates = batch.length - deduplicated.length;
      if (inFileDuplicates > 0) {
        duplicate += inFileDuplicates;
        log('Skipped duplicate records inside export count=$inFileDuplicates');
      }
      final existingIds = await _findExistingClientRecordIds(deduplicated, importLogs);
      final toInsert = <ConvertedAppleRecord>[];
      for (final converted in deduplicated) {
        final clientRecordId = converted.clientRecordId;
        final isDuplicate =
            clientRecordId != null && existingIds.contains(clientRecordId);
        if (isDuplicate) {
          _addServiceDiagnostic(
            serviceDiagnostics,
            serviceDiagnosticSummaries,
            converted,
            'duplicate_existing',
            'A matching Health Connect clientRecordId already exists.',
          );
          _stat(typeStats, converted.appleType).duplicateSkipped += 1;
          duplicate += 1;
        } else {
          toInsert.add(converted);
        }
      }
      final existingDuplicates = deduplicated.length - toInsert.length;
      if (existingDuplicates > 0) {
        log('Skipped records already present in Health Connect count=$existingDuplicates');
      }
      log('Stage finished: Checking duplicates batchRecords=${batch.length} '
          'unique=${deduplicated.length} toInsert=${toInsert.length} '
          'inFileDuplicates=$inFileDuplicates existingDuplicates=$existingDuplicates');

      onProgress?.call(writeProgress(AppleHealthImportPhase.writing));
      log('Stage started: Writing records attempted=${toInsert.length}');
      final result = await _insertConvertedRecords(
        toInsert,
        serviceDiagnostics,
        serviceDiagnosticSummaries,
        typeStats,
        importLogs,
      );
      imported += result.imported;
      duplicate += result.duplicates;
      failed += result.failed;
      log('Stage finished: Writing records attempted=${toInsert.length} '
          'imported=${result.imported} duplicates=${result.duplicates} '
          'failed=${result.failed}');
      committedSelectedRecords += batch.length;
      saveCheckpoint();
      onProgress?.call(writeProgress(AppleHealthImportPhase.writing));
    }

    // Batch N's insert completes before batch N+1's duplicate lookup (a plain
    // sequential await), so cross-batch duplicates inside one export are still
    // caught by the existing-id query.
    Future<void> drain() async {
      while (readyBatches.isNotEmpty) {
        await processReadyBatch(readyBatches.removeFirst());
      }
    }

    final parsed = await _parseExport(
      file,
      importLogs,
      onProgress,
      // Kotlin 1.9.0 (a852d4e): don't read the workout-routes/*.gpx entries at all
      // when Workouts is deselected — a sleep/body/vitals-only import is faster and
      // a damaged route entry can no longer fail an import that never wanted routes.
      AppleHealthParseOptions(
        parseRouteFiles: workoutsSelected,
        // Kotlin 1.9.0 (415f2fe): a known record type whose category is not selected
        // is never materialized — the difference between OOM and not on a
        // multi-gigabyte HeartRate export with only Body selected.
        shouldMaterializeRecord: consumer.shouldMaterializeRecord,
        // The scan's numerator; makes the scan percent climb instead of standing
        // still until the parse returns.
        onElementsParsed: onProgress == null
            ? null
            : (parsedElements) => onProgress(_scanProgress(parsedElements)),
        onRecordSkipped: consumer.onRecordSkipped,
      ),
      consumer: consumer,
      onChunkBoundary: drain,
    );

    // Convert the final buffered groups (blood-pressure pairs, sleep sessions,
    // nutrition, workouts, and the additive-overlap dedup that needs the whole
    // set) and drain the batches they produce.
    onProgress?.call(writeProgress(AppleHealthImportPhase.converting));
    log('Stage started: Converting records');
    consumer.finishBuffered();
    await drain();
    consumer.finishConverted();
    await drain();
    final convertedCount = consumer.convertedRecords;
    final conversionTotals = _totals(_toTypeSummaries(typeStats));
    log('Stage finished: Converting records converted=${conversionTotals.converted} '
        'unsupported=${conversionTotals.unsupported} skipped=${conversionTotals.skipped} '
        'failed=${conversionTotals.failed} '
        'earlySkippedUnselectedRecords=${consumer.earlySkippedUnselectedRecords}');

    final summaries = _toTypeSummaries(typeStats);
    final totals = _totals(summaries);
    final routeArchiveDiagnostics = [
      if (parsed.workoutRouteArchiveFailure != null)
        _routeArchiveDiagnostic(parsed.workoutRouteArchiveFailure!),
    ];
    final diagnostics = [
      ...routeArchiveDiagnostics,
      ...converter.diagnosticsSnapshot(),
      ...serviceDiagnostics,
    ];
    final diagnosticSummaries = _mergeDiagnosticSummaries([
      ...routeArchiveDiagnostics.map(_toSingleDiagnosticSummary),
      ...converter.diagnosticSummariesSnapshot(),
      ...serviceDiagnosticSummaries.values,
    ]);
    final categorySummaries = _categorySummaries(categoryStats);
    log('Import completed converted=${totals.converted} imported=$imported '
        'duplicates=${totals.duplicateSkipped} unsupported=${totals.unsupported} '
        'notSelected=${totals.notSelected} skipped=${totals.skipped} '
        'failed=${totals.failed} diagnostics=${diagnostics.length}');
    onProgress?.call(_progress(
      AppleHealthImportPhase.buildingReport,
      parsed: parsed,
      converted: convertedCount,
      imported: imported,
      duplicate: totals.duplicateSkipped,
      notSelected: totals.notSelected,
      unsupported: totals.unsupported,
      skipped: totals.skipped,
      failed: totals.failed,
    ));
    log('Stage started: Building report diagnostics=${diagnostics.length} '
        'diagnosticGroups=${diagnosticSummaries.length} typeSummaries=${summaries.length}');
    log('Stage finished: Building report');
    final reportText = _buildReportText(
      parsed: parsed,
      imported: imported,
      selectedCategories: selectedCategories,
      summaries: summaries,
      categorySummaries: categorySummaries,
      diagnostics: diagnostics,
      diagnosticSummaries: diagnosticSummaries,
      importLogs: importLogs,
    );
    return AppleHealthImportResult(
      parsedRecords: parsed.parsedRecords,
      parsedWorkouts: parsed.parsedWorkouts,
      parsedCorrelations: parsed.parsedCorrelations,
      parsedActivitySummaries: parsed.parsedActivitySummaries,
      convertedRecords: totals.converted,
      importedRecords: imported,
      duplicateSkippedRecords: totals.duplicateSkipped,
      notSelectedRecords: totals.notSelected,
      unsupportedElements: totals.unsupported,
      skippedRecords: totals.skipped,
      failedRecords: totals.failed,
      workoutRoutesIncomplete: parsed.workoutRouteArchiveFailure != null,
      typeSummaries: summaries,
      diagnostics: diagnostics,
      shareableReportText: reportText,
    );
  }

  Future<AppleParsedExport> _parseExport(
    File file,
    List<String> importLogs,
    AppleHealthImportProgressCallback? onProgress,
    AppleHealthParseOptions options, {
    AppleHealthXmlEventConsumer? consumer,
    Future<void> Function()? onChunkBoundary,
  }) async {
    onProgress?.call(const AppleHealthImportProgress(
      phase: AppleHealthImportPhase.parsing,
    ));
    importLogs.add(_infoLog(
      'Stage started: Scanning export source=${file.path} '
      'parseRouteFiles=${options.parseRouteFiles} '
      'parseRecordDetails=${options.parseRecordDetails}',
    ));
    final parsed = await AppleHealthImportParser.parseFile(
      file,
      consumer: consumer,
      options: options,
      onChunkBoundary: onChunkBoundary,
    );
    importLogs.add(_infoLog(
      'Stage finished: Scanning export records=${parsed.parsedRecords} '
      'workouts=${parsed.parsedWorkouts} correlations=${parsed.parsedCorrelations} '
      'activitySummaries=${parsed.parsedActivitySummaries}',
    ));
    if (parsed.sanitizedControlChars > 0 || parsed.sanitizedAmpersands > 0) {
      importLogs.add(_infoLog(
        'export.xml contained invalid XML that was auto-repaired: '
        'controlCharsRemoved=${parsed.sanitizedControlChars} '
        'ampersandsEscaped=${parsed.sanitizedAmpersands}',
      ));
    }
    final routeFailure = parsed.workoutRouteArchiveFailure;
    if (routeFailure != null) {
      importLogs.add(_warnLog(
        'Workout route archive recovery: ${routeFailure.detail}',
      ));
    }
    return parsed;
  }

  Future<Set<String>> _findExistingClientRecordIds(
    List<ConvertedAppleRecord> records,
    List<String> importLogs,
  ) async {
    final byType = <String, List<ConvertedAppleRecord>>{};
    for (final record in records) {
      if (record.clientRecordId == null) continue;
      byType.putIfAbsent(record.recordType, () => []).add(record);
    }
    final result = <String>{};
    for (final entry in byType.entries) {
      for (final chunk
          in _chunkForDuplicateCheck(entry.value, _maxDuplicateCheckSpanSeconds)) {
        final wantedIds =
            chunk.map((it) => it.clientRecordId!).toSet();
        if (wantedIds.isEmpty) continue;
        final start = chunk
            .map((it) => it.sourceTimeRange.start)
            .reduce((a, b) => a.isBefore(b) ? a : b)
            .subtract(const Duration(seconds: 1));
        final end = chunk
            .map((it) => it.sourceTimeRange.end)
            .reduce((a, b) => a.isAfter(b) ? a : b)
            .add(const Duration(seconds: 1));
        try {
          // orThrow stays ON PURPOSE. The catch below writes the *original*
          // throwable into the shareable report through
          // `AppleHealthImportErrorFormatter`, which reads its runtime type,
          // message and `cause` chain. Switching on the Result here would hand
          // the formatter an `AppFailure` instead, silently rewriting every
          // lookup-failure line of the report the user is asked to file bugs
          // with. The report is the product; the bridge is not in its way.
          final matched = _orThrowImport(
            await _repository.findMatchingImportedClientRecordIds(
              entry.key,
              start,
              end,
              wantedIds,
            ),
          );
          result.addAll(matched);
        } catch (error, stackTrace) {
          importLogs.add(_errorLog(
            'Existing clientRecordId lookup failed recordType=${entry.key} '
            'wanted=${wantedIds.length}',
            error,
            stackTrace,
          ));
        }
      }
    }
    return result;
  }

  Future<_InsertionResult> _insertConvertedRecords(
    List<ConvertedAppleRecord> records,
    List<AppleHealthImportDiagnostic> diagnostics,
    Map<String, AppleHealthImportDiagnosticSummary> diagnosticSummaries,
    Map<String, MutableAppleImportTypeStats> typeStats,
    List<String> importLogs,
  ) async {
    if (records.isEmpty) return const _InsertionResult();
    try {
      // Both orThrow bridges in this method stay ON PURPOSE — they are what the
      // error handling below is written against, and that error handling *is*
      // the importer:
      //
      //   * a failed batch falls back to inserting its records one by one, so a
      //     single poisoned record cannot cost 299 good ones;
      //   * `_isDuplicateClientRecordFailure` classifies the individual failure
      //     by matching STRINGS in `error.toString()` — the Health Connect
      //     provider's own wording ("clientRecordId ... already exists"), which
      //     is the only signal it gives us. Switching on the Result would hand
      //     that classifier an `AppFailure` whose `toString()` is
      //     "UnexpectedFailure: …", and every duplicate would be miscounted as a
      //     hard failure and written into the report as one.
      //
      // Unwrapping `failure.cause` instead would be the same throwable by a
      // longer route, and would also stop catching what the repository throws
      // *outside* its runCatching. Correctness beats purity here.
      _orThrowImport(await _repository
          .insertImportedRecords(records.map((it) => it.record).toList()));
      for (final converted in records) {
        _stat(typeStats, converted.appleType).imported += 1;
      }
      return _InsertionResult(imported: records.length);
    } catch (error, stackTrace) {
      importLogs.add(_errorLog(
        'Batch insert failed count=${records.length}; retrying individually',
        error,
        stackTrace,
      ));
    }

    var imported = 0;
    var duplicates = 0;
    var failed = 0;
    for (final converted in records) {
      try {
        _orThrowImport(
            await _repository.insertImportedRecords([converted.record]));
        _stat(typeStats, converted.appleType).imported += 1;
        imported += 1;
      } catch (error, stackTrace) {
        if (_isDuplicateClientRecordFailure(error)) {
          _stat(typeStats, converted.appleType).duplicateSkipped += 1;
          _addServiceDiagnostic(
            diagnostics,
            diagnosticSummaries,
            converted,
            'duplicate_rejected',
            'Health Connect rejected this as an existing clientRecordId.',
          );
          duplicates += 1;
        } else {
          _stat(typeStats, converted.appleType).failed += 1;
          importLogs.add(_errorLog(
            'Record insert failed appleType=${converted.appleType} '
            'target=${converted.targetType} timeRange=${converted.sourceTimeRange}',
            error,
            stackTrace,
          ));
          _addServiceDiagnostic(
            diagnostics,
            diagnosticSummaries,
            converted,
            'insert_failed',
            AppleHealthImportErrorFormatter.details(error, stackTrace),
          );
          failed += 1;
        }
      }
    }
    return _InsertionResult(
      imported: imported,
      duplicates: duplicates,
      failed: failed,
    );
  }
}

// ── Streaming consumers ──────────────────────────────────────────────────────

/// Streaming consumer for [AppleHealthImportService.analyzeAppleHealthExport]:
/// categorises and counts each element, then drops it so nothing accumulates.
/// Mirrors Kotlin's `StreamingAppleHealthScanAnalysisState`.
class _AnalyzeConsumer implements AppleHealthXmlEventConsumer {
  _AnalyzeConsumer(this.mindfulnessAvailable);

  final bool mindfulnessAvailable;

  final Map<String, MutableAppleImportTypeStats> typeStats = {};
  final Map<AppleHealthImportCategory, _MutableCategorySummary> categoryStats = {};
  final List<AppleHealthImportDiagnostic> diagnostics = [];
  final Map<String, AppleHealthImportDiagnosticSummary> diagnosticSummaries = {};
  final Set<String> _rawDiagnosticTypes = {};

  @override
  void onParsedType(String type) {
    // Parsed totals are seeded from the handler's aggregate counts after the parse.
  }

  @override
  void onRecord(AppleRecord record) {
    final category = analysisCategory(record, mindfulnessAvailable);
    if (category != null) {
      _stat(typeStats, record.type).converted += 1;
      _addCategory(categoryStats, category, 1);
    } else {
      _markUnsupported(
        record.type,
        'No direct Health Connect mapping is implemented for this Apple record '
            'type.',
      );
    }
  }

  @override
  void onWorkout(AppleWorkout workout) {
    _stat(typeStats, workout.workoutActivityType).converted += 1;
    _addCategory(
      categoryStats,
      AppleHealthImportCategory.workouts,
      1,
      routeSessions: workout.routeReferences > 0 ? 1 : 0,
    );
  }

  @override
  void onCorrelation(AppleCorrelation correlation) {
    if (correlation.type == appleBloodPressureCorrelation) {
      _stat(typeStats, correlation.type).converted += 1;
      _addCategory(categoryStats, AppleHealthImportCategory.vitals, 1);
    } else {
      _markUnsupported(
        correlation.type,
        'Correlation type has no direct Health Connect import mapping.',
      );
    }
  }

  @override
  void onActivitySummary() {
    _markUnsupported(
      'ActivitySummary',
      'Apple activity rings and stand hours have no direct writable Health '
          'Connect record.',
    );
  }

  void _markUnsupported(String appleType, String detail) {
    _stat(typeStats, appleType).unsupported += 1;
    final diagnostic = AppleHealthImportDiagnostic(
      appleType: appleType,
      targetType: null,
      reasonCode: 'unsupported',
      timeRange: null,
      unit: null,
      value: null,
      detail: detail,
    );
    addToDiagnosticSummaries(diagnosticSummaries, diagnostic);
    if (_rawDiagnosticTypes.add(appleType)) diagnostics.add(diagnostic);
  }
}

/// Streaming consumer for [AppleHealthImportService.importAppleHealthExport]:
/// converts + selects records as they are parsed and hands full 300-record
/// batches to [_onReadyBatch], buffering only the records whose conversion needs
/// cross-record context (blood-pressure pairing, sleep sessions, nutrition,
/// additive-overlap dedup). Mirrors Kotlin's `StreamingAppleHealthWritingState`.
class _ImportConsumer implements AppleHealthXmlEventConsumer {
  _ImportConsumer({
    required this.converter,
    required this.selectedCategories,
    required this.categoryStats,
    required this.typeStats,
    required this.onReadyBatch,
  });

  final AppleHealthImportConverter converter;
  final Set<AppleHealthImportCategory> selectedCategories;
  final Map<AppleHealthImportCategory, _MutableCategorySummary> categoryStats;
  final Map<String, MutableAppleImportTypeStats> typeStats;
  final void Function(List<ConvertedAppleRecord>) onReadyBatch;

  final List<AppleRecord> _bufferedRecords = [];
  final List<AppleRecord> _overlapDedupRecords = [];
  final List<AppleWorkout> _bufferedWorkouts = [];
  final List<ConvertedAppleRecord> _convertedBatch = [];

  int parsedRecords = 0;
  int parsedWorkouts = 0;
  int parsedCorrelations = 0;
  int parsedActivitySummaries = 0;
  int convertedRecords = 0;
  int notSelectedRecords = 0;
  int earlySkippedUnselectedRecords = 0;

  /// Wired onto [AppleHealthParseOptions.shouldMaterializeRecord].
  bool shouldMaterializeRecord(String type) => _shouldMaterializeRecord(
        type,
        converter.mindfulnessAvailable,
        selectedCategories,
      );

  /// Wired onto [AppleHealthParseOptions.onRecordSkipped]: a compatible record
  /// whose category is not selected is booked here without ever being converted.
  void onRecordSkipped(String type) {
    parsedRecords += 1;
    final category = analysisCategoryForType(type, converter.mindfulnessAvailable);
    if (category == null) return;
    converter.markCompatibleNotSelected(type);
    earlySkippedUnselectedRecords += 1;
    convertedRecords += 1;
    notSelectedRecords += 1;
    _addCategory(categoryStats, category, 1);
  }

  @override
  void onParsedType(String type) => converter.markParsed(type);

  @override
  void onRecord(AppleRecord record) {
    parsedRecords += 1;
    converter.noteWorkoutOverlap(record);
    if (converter.shouldBufferForOverlapDedup(record)) {
      _overlapDedupRecords.add(record);
    } else if (converter.shouldBufferRecord(record)) {
      _bufferedRecords.add(record);
      if (_bufferedRecords.length >= _bufferedRecordBatchSize) {
        _flushBufferedRecords();
      }
    } else {
      final converted = converter.convertStreamingRecord(record);
      if (converted != null) _acceptConverted(converted);
    }
  }

  @override
  void onWorkout(AppleWorkout workout) {
    parsedWorkouts += 1;
    _bufferedWorkouts.add(workout);
  }

  @override
  void onCorrelation(AppleCorrelation correlation) {
    parsedCorrelations += 1;
    converter.convertBufferedGroups(
      records: const [],
      workouts: const [],
      correlations: [correlation],
      parsedActivitySummaries: 0,
      emit: _acceptConverted,
    );
  }

  @override
  void onActivitySummary() {
    parsedActivitySummaries += 1;
  }

  /// Flush the remaining grouped buffers after the parse (Kotlin
  /// `finishBufferedGroups`): the leftover buffered records, then the workouts and
  /// the whole additive-overlap set.
  void finishBuffered() {
    _flushBufferedRecords();
    converter.convertBufferedGroups(
      records: _overlapDedupRecords,
      workouts: _bufferedWorkouts,
      correlations: const [],
      parsedActivitySummaries: parsedActivitySummaries,
      emit: _acceptConverted,
    );
    _overlapDedupRecords.clear();
    _bufferedWorkouts.clear();
  }

  /// Hand the final partial batch to the writer (Kotlin `finishConverted`).
  void finishConverted() => _sendConvertedBatch();

  void _flushBufferedRecords() {
    if (_bufferedRecords.isEmpty) return;
    converter.convertBufferedGroups(
      records: _bufferedRecords,
      workouts: const [],
      correlations: const [],
      parsedActivitySummaries: 0,
      emit: _acceptConverted,
    );
    _bufferedRecords.clear();
  }

  void _acceptConverted(ConvertedAppleRecord converted) {
    convertedRecords += 1;
    final category = importCategory(converted);
    _addCategory(
      categoryStats,
      category,
      1,
      routeSessions: convertedHasExerciseRoute(converted) ? 1 : 0,
    );
    if (!selectedCategories.contains(category)) {
      _stat(typeStats, converted.appleType).notSelected += 1;
      notSelectedRecords += 1;
      return;
    }
    _convertedBatch.add(converted);
    if (_convertedBatch.length >= _convertedBatchSize) {
      _sendConvertedBatch();
    }
  }

  void _sendConvertedBatch() {
    if (_convertedBatch.isEmpty) return;
    onReadyBatch(List.of(_convertedBatch));
    _convertedBatch.clear();
  }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

/// Kotlin `AppleHealthImportService.shouldMaterializeRecord`.
///
/// A record is materialized when its type has no known category (it still has to
/// be converted so it can be reported as unsupported), when its category is
/// selected, or — the narrow, load-bearing exception — when it is a distance or
/// active-energy sample and workouts *are* selected: `noteWorkoutOverlap` needs
/// those samples to protect selected workouts from double-counted overlaps, even
/// though the samples themselves belong to the (unselected) activity category.
bool _shouldMaterializeRecord(
  String type,
  bool mindfulnessAvailable,
  Set<AppleHealthImportCategory> selectedCategories,
) {
  final category = analysisCategoryForType(type, mindfulnessAvailable);
  if (category == null) return true;
  if (selectedCategories.contains(category)) return true;
  return selectedCategories.contains(AppleHealthImportCategory.workouts) &&
      (appleDistanceTypes.contains(type) || type == appleActiveEnergyBurned);
}

MutableAppleImportTypeStats _stat(
  Map<String, MutableAppleImportTypeStats> typeStats,
  String type,
) =>
    typeStats.putIfAbsent(type, MutableAppleImportTypeStats.new);

class _MutableCategorySummary {
  _MutableCategorySummary(this.category);

  final AppleHealthImportCategory category;
  int convertedRecords = 0;
  int routeSessions = 0;
}

void _addCategory(
  Map<AppleHealthImportCategory, _MutableCategorySummary> categoryStats,
  AppleHealthImportCategory category,
  int convertedRecords, {
  int routeSessions = 0,
}) {
  final summary =
      categoryStats.putIfAbsent(category, () => _MutableCategorySummary(category));
  summary.convertedRecords += convertedRecords;
  summary.routeSessions += routeSessions;
}

List<AppleHealthImportCategorySummary> _categorySummaries(
  Map<AppleHealthImportCategory, _MutableCategorySummary> categoryStats,
) =>
    AppleHealthImportCategory.values
        .map((category) => categoryStats[category])
        .whereType<_MutableCategorySummary>()
        .where((summary) => summary.convertedRecords > 0)
        .map((summary) => AppleHealthImportCategorySummary(
              category: summary.category,
              convertedRecords: summary.convertedRecords,
              routeSessions: summary.routeSessions,
            ))
        .toList();

class _Totals {
  const _Totals({
    this.converted = 0,
    this.duplicateSkipped = 0,
    this.notSelected = 0,
    this.unsupported = 0,
    this.skipped = 0,
    this.failed = 0,
  });

  final int converted;
  final int duplicateSkipped;
  final int notSelected;
  final int unsupported;
  final int skipped;
  final int failed;
}

_Totals _totals(List<AppleHealthImportTypeSummary> summaries) {
  var converted = 0;
  var duplicateSkipped = 0;
  var notSelected = 0;
  var unsupported = 0;
  var skipped = 0;
  var failed = 0;
  for (final summary in summaries) {
    converted += summary.converted;
    duplicateSkipped += summary.duplicateSkipped;
    notSelected += summary.notSelected;
    unsupported += summary.unsupported;
    skipped += summary.skipped;
    failed += summary.failed;
  }
  return _Totals(
    converted: converted,
    duplicateSkipped: duplicateSkipped,
    notSelected: notSelected,
    unsupported: unsupported,
    skipped: skipped,
    failed: failed,
  );
}

List<AppleHealthImportTypeSummary> _toTypeSummaries(
  Map<String, MutableAppleImportTypeStats> typeStats,
) {
  final entries = typeStats.entries.toList()
    ..sort((a, b) {
      final byParsed = b.value.parsed.compareTo(a.value.parsed);
      if (byParsed != 0) return byParsed;
      return a.key.compareTo(b.key);
    });
  return entries
      .map((entry) => AppleHealthImportTypeSummary(
            appleType: entry.key,
            parsed: entry.value.parsed,
            converted: entry.value.converted,
            imported: entry.value.imported,
            duplicateSkipped: entry.value.duplicateSkipped,
            notSelected: entry.value.notSelected,
            unsupported: entry.value.unsupported,
            skipped: entry.value.skipped,
            failed: entry.value.failed,
          ))
      .toList();
}

List<ConvertedAppleRecord> _deduplicateWithinImport(
  List<ConvertedAppleRecord> batch,
  List<AppleHealthImportDiagnostic> diagnostics,
  Map<String, AppleHealthImportDiagnosticSummary> diagnosticSummaries,
  Map<String, MutableAppleImportTypeStats> typeStats,
) {
  final seen = <String>{};
  final result = <ConvertedAppleRecord>[];
  for (final converted in batch) {
    final key = converted.clientRecordId ?? converted.fingerprint;
    if (!seen.add(key)) {
      _addServiceDiagnostic(
        diagnostics,
        diagnosticSummaries,
        converted,
        'duplicate_in_file',
        'The export contained another object with the same deterministic import '
            'fingerprint.',
      );
      _stat(typeStats, converted.appleType).duplicateSkipped += 1;
    } else {
      result.add(converted);
    }
  }
  return result;
}

void _addServiceDiagnostic(
  List<AppleHealthImportDiagnostic> diagnostics,
  Map<String, AppleHealthImportDiagnosticSummary> diagnosticSummaries,
  ConvertedAppleRecord converted,
  String reasonCode,
  String detail,
) {
  final diagnostic = AppleHealthImportDiagnostic(
    appleType: converted.appleType,
    targetType: converted.targetType,
    reasonCode: reasonCode,
    timeRange: converted.sourceTimeRange.toString(),
    unit: converted.unit,
    value: converted.value,
    detail: detail,
  );
  addToDiagnosticSummaries(diagnosticSummaries, diagnostic);
  if (diagnostics.length < _maxRawDiagnostics) diagnostics.add(diagnostic);
}

List<AppleHealthImportDiagnosticSummary> _mergeDiagnosticSummaries(
  List<AppleHealthImportDiagnosticSummary> summaries,
) {
  final merged = <String, AppleHealthImportDiagnosticSummary>{};
  for (final summary in summaries) {
    final key = '${summary.appleType} ${summary.targetType ?? ''}'
        ' ${summary.reasonCode} ${summary.detail}';
    final existing = merged[key];
    if (existing != null) {
      existing.count += summary.count;
    } else {
      merged[key] = AppleHealthImportDiagnosticSummary(
        appleType: summary.appleType,
        targetType: summary.targetType,
        reasonCode: summary.reasonCode,
        detail: summary.detail,
        count: summary.count,
        exampleTimeRange: summary.exampleTimeRange,
        exampleUnit: summary.exampleUnit,
        exampleValue: summary.exampleValue,
      );
    }
  }
  return merged.values.toList();
}

List<List<ConvertedAppleRecord>> _chunkForDuplicateCheck(
  List<ConvertedAppleRecord> records,
  int maxSpanSeconds,
) {
  if (records.isEmpty) return const [];
  final sorted = List<ConvertedAppleRecord>.of(records)
    ..sort((a, b) => a.sourceTimeRange.start.compareTo(b.sourceTimeRange.start));
  final chunks = <List<ConvertedAppleRecord>>[];
  var current = <ConvertedAppleRecord>[sorted.first];
  var chunkStart = sorted.first.sourceTimeRange.start;
  for (var index = 1; index < sorted.length; index++) {
    final record = sorted[index];
    final spanSeconds =
        record.sourceTimeRange.start.difference(chunkStart).inSeconds;
    if (spanSeconds > maxSpanSeconds) {
      chunks.add(current);
      current = [record];
      chunkStart = record.sourceTimeRange.start;
    } else {
      current.add(record);
    }
  }
  chunks.add(current);
  return chunks;
}

bool _isDuplicateClientRecordFailure(Object error) {
  final text = _errorChainMatchText(error);
  return text.contains('clientrecordid') ||
      text.contains('client record id') ||
      ((text.contains('duplicate') ||
              text.contains('already exist') ||
              text.contains('already exists')) &&
          text.contains('record'));
}

/// The lower-cased match text for [_isDuplicateClientRecordFailure], built from
/// the whole `cause` chain plus each link's runtime type name — the Dart analogue
/// of Kotlin's `generateSequence(this){it.cause}.joinToString{ message + simpleName }`.
/// The duplicate signal can live in a nested cause or only in a class name (e.g. a
/// `DuplicateRecordException` with a generic message); inspecting just the
/// top-level `toString()` misses those and miscounts a duplicate as a hard failure.
String _errorChainMatchText(Object error) {
  final buffer = StringBuffer();
  final seen = <Object>{};
  Object? current = error;
  while (current != null && seen.add(current)) {
    buffer
      ..write(current.toString())
      ..write(' ')
      ..write(current.runtimeType.toString())
      ..write(' ');
    current = current is AppleHealthImportException ? current.cause : null;
  }
  return buffer.toString().toLowerCase();
}

/// A progress snapshot from *inside* the scan, where no [AppleParsedExport]
/// exists yet — so [_progress] cannot build it.
///
/// Only the element total is known mid-parse, and it is carried in
/// `parsedRecords` because [AppleHealthImportProgress.parsedElements] (the sole
/// number any consumer reads while the phase is `parsing`: the percent, the
/// notification text and the card's line all go through it) is the sum of the
/// four parsed counters. Splitting the total back into records/workouts/
/// correlations/summaries would cost four callbacks' worth of hot-loop work to
/// feed a distinction nobody makes during the scan.
AppleHealthImportProgress _scanProgress(int parsedElements) =>
    AppleHealthImportProgress(
      phase: AppleHealthImportPhase.parsing,
      parsedRecords: parsedElements,
    );

AppleHealthImportProgress _progress(
  AppleHealthImportPhase phase, {
  required AppleParsedExport parsed,
  int converted = 0,
  int imported = 0,
  int duplicate = 0,
  int notSelected = 0,
  int unsupported = 0,
  int skipped = 0,
  int failed = 0,
}) =>
    AppleHealthImportProgress(
      phase: phase,
      parsedRecords: parsed.parsedRecords,
      parsedWorkouts: parsed.parsedWorkouts,
      parsedCorrelations: parsed.parsedCorrelations,
      parsedActivitySummaries: parsed.parsedActivitySummaries,
      convertedRecords: converted,
      importedRecords: imported,
      duplicateSkippedRecords: duplicate,
      notSelectedRecords: notSelected,
      unsupportedElements: unsupported,
      skippedRecords: skipped,
      failedRecords: failed,
    );

AppleHealthImportDiagnostic _routeArchiveDiagnostic(
  AppleWorkoutRouteArchiveFailure failure,
) =>
    AppleHealthImportDiagnostic(
      appleType: 'WorkoutRoute',
      targetType: 'ExerciseRoute',
      reasonCode: 'route_archive_truncated',
      timeRange: null,
      unit: null,
      value: null,
      detail: failure.detail,
    );

AppleHealthImportDiagnosticSummary _toSingleDiagnosticSummary(
  AppleHealthImportDiagnostic diagnostic,
) =>
    AppleHealthImportDiagnosticSummary(
      appleType: diagnostic.appleType,
      targetType: diagnostic.targetType,
      reasonCode: diagnostic.reasonCode,
      detail: diagnostic.detail,
      count: 1,
      exampleTimeRange: diagnostic.timeRange,
      exampleUnit: diagnostic.unit,
      exampleValue: diagnostic.value,
    );

String _infoLog(String message) =>
    '${DateTime.now().toUtc().toIso8601String()} [INFO] $message';

String _warnLog(String message) =>
    '${DateTime.now().toUtc().toIso8601String()} [WARN] $message';

String _errorLog(String message, Object error, [StackTrace? stackTrace]) =>
    '${DateTime.now().toUtc().toIso8601String()} [ERROR] $message\n'
    '${AppleHealthImportErrorFormatter.details(error, stackTrace)}';

class _InsertionResult {
  const _InsertionResult({
    this.imported = 0,
    this.duplicates = 0,
    this.failed = 0,
  });

  final int imported;
  final int duplicates;
  final int failed;
}

String _buildReportText({
  required AppleParsedExport parsed,
  required int imported,
  required Set<AppleHealthImportCategory>? selectedCategories,
  required List<AppleHealthImportTypeSummary> summaries,
  required List<AppleHealthImportCategorySummary> categorySummaries,
  required List<AppleHealthImportDiagnostic> diagnostics,
  required List<AppleHealthImportDiagnosticSummary> diagnosticSummaries,
  required List<String> importLogs,
}) {
  final totals = _totals(summaries);
  final buffer = StringBuffer()
    ..writeln(appleHealthReportHeader())
    ..writeln()
    ..writeln('Summary')
    ..writeln('Parsed records: ${parsed.parsedRecords}')
    ..writeln('Parsed workouts: ${parsed.parsedWorkouts}')
    ..writeln('Parsed correlations: ${parsed.parsedCorrelations}')
    ..writeln('Parsed activity summaries: ${parsed.parsedActivitySummaries}')
    ..writeln('Converted Health Connect records: ${totals.converted}')
    ..writeln('Imported Health Connect records: $imported')
    ..writeln('Duplicate skipped: ${totals.duplicateSkipped}')
    ..writeln('Not selected: ${totals.notSelected}')
    ..writeln('Unsupported: ${totals.unsupported}')
    ..writeln('Skipped: ${totals.skipped}')
    ..writeln('Failed: ${totals.failed}')
    ..writeln(
        'Workout routes incomplete: ${parsed.workoutRouteArchiveFailure != null}');
  if (selectedCategories != null) {
    buffer.writeln(
        'Selected categories: ${selectedCategories.map((c) => c.reportName).join(', ')}');
  }
  final workoutsMissingRoutes = diagnosticSummaries
      .where((summary) => summary.reasonCode == 'workout_route_unavailable')
      .toList()
    ..sort((a, b) {
      final byTime =
          (a.exampleTimeRange ?? '').compareTo(b.exampleTimeRange ?? '');
      if (byTime != 0) return byTime;
      return a.appleType.compareTo(b.appleType);
    });
  if (workoutsMissingRoutes.isNotEmpty) {
    buffer
      ..writeln()
      ..writeln('Activities Requiring Manual Route Import')
      ..writeln(
        'These activities referenced unavailable route geometry. Depending on '
        'the selected categories, their workout sessions may have imported '
        'without routes:',
      );
    for (final diagnostic in workoutsMissingRoutes) {
      buffer.writeln(
        '- activity=${diagnostic.appleType}; '
        'timeRange=${diagnostic.exampleTimeRange ?? 'unknown'}; '
        'occurrences=${diagnostic.count}; ${diagnostic.detail}',
      );
    }
  }
  buffer
    ..writeln()
    ..writeln('Logs');
  if (importLogs.isEmpty) {
    buffer.writeln('No import log entries were recorded.');
  } else {
    for (final entry in importLogs) {
      buffer.writeln(entry);
    }
  }
  buffer
    ..writeln()
    ..writeln('By Import Category');
  if (categorySummaries.isEmpty) {
    buffer.writeln('No Health Connect-compatible categories were detected.');
  } else {
    for (final summary in categorySummaries) {
      final routeText = summary.routeSessions > 0
          ? ', routeSessions=${summary.routeSessions}'
          : '';
      buffer.writeln(
          '- ${summary.category.reportName}: converted=${summary.convertedRecords}$routeText');
    }
  }
  buffer
    ..writeln()
    ..writeln('By Apple Type');
  for (final summary in summaries) {
    buffer.writeln(
      '- ${summary.appleType}: parsed=${summary.parsed}, converted=${summary.converted}, '
      'imported=${summary.imported}, duplicate=${summary.duplicateSkipped}, '
      'notSelected=${summary.notSelected}, unsupported=${summary.unsupported}, '
      'skipped=${summary.skipped}, failed=${summary.failed}',
    );
  }
  buffer
    ..writeln()
    ..writeln('Diagnostic Summary');
  if (diagnosticSummaries.isEmpty) {
    buffer.writeln(
        'No failures, skips, duplicates, or unsupported entries were recorded.');
  } else {
    final reasonSummaries = <String, int>{};
    for (final summary in diagnosticSummaries) {
      reasonSummaries[summary.reasonCode] =
          (reasonSummaries[summary.reasonCode] ?? 0) + summary.count;
    }
    final sortedReasons = reasonSummaries.keys.toList()..sort();
    buffer.writeln(
      'Grouped diagnostic types: ${diagnosticSummaries.length}; '
      '${sortedReasons.map((reason) => '$reason=${reasonSummaries[reason]}').join(', ')}',
    );
    final sorted = List<AppleHealthImportDiagnosticSummary>.of(diagnosticSummaries)
      ..sort((a, b) {
        final byCount = b.count.compareTo(a.count);
        if (byCount != 0) return byCount;
        final byReason = a.reasonCode.compareTo(b.reasonCode);
        if (byReason != 0) return byReason;
        return a.appleType.compareTo(b.appleType);
      });
    for (var index = 0; index < sorted.length; index++) {
      final diagnostic = sorted[index];
      final exampleParts = <String>[
        if (diagnostic.exampleTimeRange != null)
          'exampleTime=${diagnostic.exampleTimeRange}',
        if (diagnostic.exampleUnit != null) 'unit=${diagnostic.exampleUnit}',
        if (diagnostic.exampleValue != null) 'value=${diagnostic.exampleValue}',
      ];
      final exampleText =
          exampleParts.isEmpty ? '' : '; ${exampleParts.join('; ')}';
      buffer.writeln(
        '${index + 1}. count=${diagnostic.count}; reason=${diagnostic.reasonCode}; '
        'appleType=${diagnostic.appleType}; target=${diagnostic.targetType ?? 'none'}; '
        'detail=${diagnostic.detail}$exampleText',
      );
    }
  }
  buffer
    ..writeln()
    ..writeln('Raw Diagnostic Log');
  if (diagnostics.isEmpty) {
    buffer.writeln('No raw diagnostics were recorded.');
  } else {
    if (diagnostics.length >= _maxRawDiagnostics) {
      buffer.writeln(
        'Raw diagnostics were capped at $_maxRawDiagnostics per source; '
        'see Diagnostic Summary above for complete counts.',
      );
    }
    for (var index = 0; index < diagnostics.length; index++) {
      final diagnostic = diagnostics[index];
      buffer.writeln(
        '${index + 1}. reason=${diagnostic.reasonCode}; appleType=${diagnostic.appleType}; '
        'target=${diagnostic.targetType ?? 'none'}; timeRange=${diagnostic.timeRange ?? 'none'}; '
        'unit=${diagnostic.unit ?? 'none'}; value=${diagnostic.value ?? 'none'}; '
        'detail=${diagnostic.detail}',
      );
    }
  }
  return buffer.toString();
}
