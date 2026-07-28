/// Runs a CSV import: stream rows, convert, count what is already there, write
/// in batches, and tally the result.
///
/// Deliberately NOT a foreground service. The app declares exactly one, which is
/// why an Apple Health import already refuses to run while a GPS recording is
/// active — and that service exists for multi-gigabyte exports taking tens of
/// minutes. A body-composition CSV is bounded by how often a human stands on a
/// scale: ten years of twice-daily weigh-ins is ~7,300 rows and a file well under
/// a megabyte. Staying in-process is not just cheaper, it means a CSV import can
/// run *while* an activity is being recorded.
library;

import 'dart:async';

import '../../../core/result/app_failure.dart';
import '../../../core/result/result.dart';
import '../../../data/repository/contract/import_write_repository.dart';
import '../../../domain/model/apple_health_import_records.dart';
import 'csv_column_mapping.dart';
import 'csv_import_models.dart';
import 'csv_row_converter.dart';
import 'csv_table_reader.dart';

/// Records handed to Health Connect per insert call.
const int kCsvWriteBatchSize = 300;

/// Ids per existing-record lookup. Rows arrive in time order, so 500 daily
/// weigh-ins is one query covering roughly 1.4 years.
const int kCsvDuplicateLookupChunk = 500;

/// A cooperative cancel flag the UI flips; checked at row and batch boundaries.
class CsvImportCancellation {
  bool _cancelled = false;

  bool get isCancelled => _cancelled;

  void cancel() => _cancelled = true;
}

/// Orchestrates one import.
class CsvImportService {
  const CsvImportService(
    this._repository, {
    // A named parameter cannot be private, so the field cannot be an
    // initializing formal — same shape as ImportWriteRepositoryImpl.
    CsvTableReader reader = const CsvTableReader(),
    // ignore: prefer_initializing_formals
  }) : _reader = reader;

  final ImportWriteRepository _repository;
  final CsvTableReader _reader;

  /// Imports [path] under [mapping], reporting progress through [onProgress].
  ///
  /// Never throws: every failure becomes a [CsvImportResult] the screen renders.
  Future<CsvImportResult> run({
    required String path,
    required CsvDialect dialect,
    required CsvImportMapping mapping,
    bool hasHeaderRow = true,
    void Function(CsvImportProgress progress)? onProgress,
    CsvImportCancellation? cancellation,
  }) async {
    final totalBytes = await _reader.byteLength(path);
    var progress = CsvImportProgress(totalBytes: totalBytes);
    final diagnostics = <CsvImportDiagnostic>[];
    final counts = <CsvImportDiagnosticReason, int>{};

    void record(CsvImportDiagnostic diagnostic) {
      counts.update(diagnostic.reason, (it) => it + 1, ifAbsent: () => 1);
      if (diagnostics.length < kCsvMaxRetainedDiagnostics) {
        diagnostics.add(diagnostic);
      }
    }

    // Guards against one file listing the same measurement twice. Cross-import
    // duplicates are a separate, chunked lookup below.
    final seenIds = <String>{};
    final pending = <ImportRecord>[];

    CsvImportResult finish(CsvImportOutcome outcome, {String? error}) =>
        CsvImportResult(
          outcome: outcome,
          progress: progress,
          diagnostics: List.unmodifiable(diagnostics),
          diagnosticCounts: Map.unmodifiable(counts),
          error: error,
        );

    try {
      final stream = _reader.rows(
        path,
        dialect: dialect,
        hasHeaderRow: hasHeaderRow,
      );

      await for (final row in stream) {
        if (cancellation?.isCancelled ?? false) {
          final flush = await _flush(pending, record);
          progress = _apply(progress, flush);
          onProgress?.call(progress);
          return finish(CsvImportOutcome.cancelled);
        }

        final conversion = convertCsvRow(row: row, mapping: mapping);
        for (final diagnostic in conversion.diagnostics) {
          record(diagnostic);
        }
        progress = progress.copyWith(
          rowsRead: progress.rowsRead + 1,
          rejected: progress.rejected + conversion.diagnostics.length,
          bytesRead: row.bytesRead,
        );

        for (final candidate in conversion.records) {
          if (seenIds.add(candidate.clientRecordId)) pending.add(candidate);
        }

        if (pending.length >= kCsvWriteBatchSize) {
          final flush = await _flush(pending, record);
          if (flush.rateLimited) {
            progress = _apply(progress, flush);
            onProgress?.call(progress);
            return finish(
              CsvImportOutcome.rateLimited,
              error: flush.error,
            );
          }
          progress = _apply(progress, flush);
        }
        onProgress?.call(progress);
      }

      final flush = await _flush(pending, record);
      progress = _apply(progress, flush);
      onProgress?.call(progress);
      if (flush.rateLimited) {
        return finish(CsvImportOutcome.rateLimited, error: flush.error);
      }
      return finish(CsvImportOutcome.completed);
    } on CsvReadException catch (error) {
      return finish(CsvImportOutcome.failed, error: error.message);
    } catch (error) {
      return finish(CsvImportOutcome.failed, error: '$error');
    }
  }

  /// Writes everything in [pending], clearing it. Counts how many ids were
  /// already in Health Connect first — purely to report "you have imported this
  /// before"; the write happens either way, because the id excludes the value
  /// and so cannot say whether anything changed. Health Connect upserts.
  Future<_FlushOutcome> _flush(
    List<ImportRecord> pending,
    void Function(CsvImportDiagnostic) record,
  ) async {
    if (pending.isEmpty) return const _FlushOutcome();
    final batch = List<ImportRecord>.from(pending);
    pending.clear();

    final alreadyPresent = await _countExisting(batch);

    switch (await _repository.insertImportedRecords(batch)) {
      case Ok():
        return _FlushOutcome(
          written: batch.length,
          alreadyPresent: alreadyPresent,
        );
      case Err(:final RateLimitFailure failure):
        return _FlushOutcome(
          rateLimited: true,
          error: failure.message,
          alreadyPresent: alreadyPresent,
        );
      case Err():
        // The batch is ATOMIC — Health Connect wrote none of it and the failure
        // does not name the record it choked on. Retry singly so the good
        // records still land and only the guilty one is counted as rejected.
        var written = 0;
        var rejected = 0;
        for (final single in batch) {
          switch (await _repository.insertImportedRecords([single])) {
            case Ok():
              written++;
            case Err(:final RateLimitFailure failure):
              return _FlushOutcome(
                written: written,
                rejected: rejected,
                alreadyPresent: alreadyPresent,
                rateLimited: true,
                error: failure.message,
              );
            case Err(:final failure):
              rejected++;
              record(
                CsvImportDiagnostic(
                  rowNumber: 0,
                  reason: CsvImportDiagnosticReason.writeFailed,
                  // `message` lives on the concrete failures, not on AppFailure;
                  // every one of them puts it in toString().
                  detail: '${single.targetType}: $failure',
                ),
              );
          }
        }
        return _FlushOutcome(
          written: written,
          rejected: rejected,
          alreadyPresent: alreadyPresent,
        );
    }
  }

  /// How many of [batch]'s ids Health Connect already holds.
  ///
  /// Grouped by record type (the lookup takes one) and chunked, with the window
  /// spanning only the chunk's own instants. A lookup failure is not fatal: it
  /// costs an accurate "already present" count, not the import.
  Future<int> _countExisting(List<ImportRecord> batch) async {
    final byType = <String, List<ImportRecord>>{};
    for (final record in batch) {
      byType.putIfAbsent(record.targetType, () => []).add(record);
    }

    var total = 0;
    for (final entry in byType.entries) {
      final records = entry.value;
      for (var start = 0;
          start < records.length;
          start += kCsvDuplicateLookupChunk) {
        final chunk = records.sublist(
          start,
          (start + kCsvDuplicateLookupChunk).clamp(0, records.length),
        );
        final times = chunk.map(_instantOf).nonNulls.toList()..sort();
        if (times.isEmpty) continue;
        final result = await _repository.findMatchingImportedClientRecordIds(
          entry.key,
          times.first.subtract(const Duration(seconds: 1)),
          times.last.add(const Duration(seconds: 1)),
          {for (final record in chunk) record.clientRecordId},
        );
        if (result case Ok(:final value)) total += value.length;
      }
    }
    return total;
  }
}

/// The instant a record sits at, used only to bound the duplicate-lookup window.
///
/// Every metric the CSV catalog can produce is a single measurement, so `time`
/// covers all of them — except heart rate, which Health Connect models as a
/// series even when a CSV row supplies exactly one sample. A record this does
/// not recognise is skipped for windowing, which costs an accurate
/// already-present count rather than the import.
DateTime? _instantOf(ImportRecord record) => switch (record) {
      WeightImportRecord(:final time) => time,
      BodyFatImportRecord(:final time) => time,
      LeanBodyMassImportRecord(:final time) => time,
      BoneMassImportRecord(:final time) => time,
      BodyWaterMassImportRecord(:final time) => time,
      HeightImportRecord(:final time) => time,
      BasalMetabolicRateImportRecord(:final time) => time,
      HeartRateImportRecord(:final startTime) => startTime,
      RestingHeartRateImportRecord(:final time) => time,
      HeartRateVariabilityRmssdImportRecord(:final time) => time,
      OxygenSaturationImportRecord(:final time) => time,
      RespiratoryRateImportRecord(:final time) => time,
      BodyTemperatureImportRecord(:final time) => time,
      BasalBodyTemperatureImportRecord(:final time) => time,
      BloodGlucoseImportRecord(:final time) => time,
      Vo2MaxImportRecord(:final time) => time,
      _ => null,
    };

class _FlushOutcome {
  const _FlushOutcome({
    this.written = 0,
    this.alreadyPresent = 0,
    this.rejected = 0,
    this.rateLimited = false,
    this.error,
  });

  final int written;
  final int alreadyPresent;
  final int rejected;
  final bool rateLimited;
  final String? error;
}

CsvImportProgress _apply(CsvImportProgress progress, _FlushOutcome flush) =>
    progress.copyWith(
      written: progress.written + flush.written,
      alreadyPresent: progress.alreadyPresent + flush.alreadyPresent,
      rejected: progress.rejected + flush.rejected,
    );
