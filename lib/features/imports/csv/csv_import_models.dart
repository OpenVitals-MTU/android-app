/// Progress, diagnostics and outcome for a CSV import run.
library;

/// Why a row, or one metric on a row, did not produce a record.
enum CsvImportDiagnosticReason {
  /// The timestamp cell was empty. Costs the whole row.
  missingTimestamp,

  /// The timestamp cell did not parse under the chosen format. Whole row.
  unparsableTimestamp,

  /// The row had fewer fields than the mapping refers to. Whole row.
  wrongFieldCount,

  /// A metric cell was not a number. Costs that metric only.
  unparsableNumber,

  /// The converted value is outside what a human body can be. That metric only.
  outOfRange,

  /// Body fat was mapped as a mass, but this row has no usable weight to
  /// divide by. That metric only.
  derivationMissingWeight,

  /// Health Connect refused the record.
  writeFailed,
}

/// One rejected row or metric, named well enough for the user to go find it.
class CsvImportDiagnostic {
  const CsvImportDiagnostic({
    required this.rowNumber,
    required this.reason,
    this.columnIndex,
    this.detail,
  });

  /// 1-based line number in the file.
  final int rowNumber;
  final CsvImportDiagnosticReason reason;
  final int? columnIndex;
  final String? detail;
}

/// How far along a run is.
class CsvImportProgress {
  const CsvImportProgress({
    this.rowsRead = 0,
    this.written = 0,
    this.alreadyPresent = 0,
    this.rejected = 0,
    this.bytesRead = 0,
    this.totalBytes = 0,
  });

  final int rowsRead;

  /// Records handed to Health Connect. Includes upserts over existing records.
  final int written;

  /// Records whose id was already in Health Connect. They are still written —
  /// the id cannot say whether the value changed — so this is "you have imported
  /// this before", not "skipped".
  final int alreadyPresent;

  /// Metrics or rows that produced no record.
  final int rejected;

  final int bytesRead;
  final int totalBytes;

  /// 0..1, or null when the file size is unknown.
  double? get fraction {
    if (totalBytes <= 0) return null;
    return (bytesRead / totalBytes).clamp(0.0, 1.0);
  }

  CsvImportProgress copyWith({
    int? rowsRead,
    int? written,
    int? alreadyPresent,
    int? rejected,
    int? bytesRead,
    int? totalBytes,
  }) =>
      CsvImportProgress(
        rowsRead: rowsRead ?? this.rowsRead,
        written: written ?? this.written,
        alreadyPresent: alreadyPresent ?? this.alreadyPresent,
        rejected: rejected ?? this.rejected,
        bytesRead: bytesRead ?? this.bytesRead,
        totalBytes: totalBytes ?? this.totalBytes,
      );
}

/// Why a run stopped.
enum CsvImportOutcome {
  /// Reached the end of the file.
  completed,

  /// The user cancelled. Everything written before that stays written.
  cancelled,

  /// Health Connect rate-limited us. Re-running later resumes from the top and
  /// re-dedupes, which is cheap at the sizes this importer targets.
  rateLimited,

  /// Something else failed; [CsvImportResult.error] says what.
  failed,
}

/// The end of a run.
class CsvImportResult {
  const CsvImportResult({
    required this.outcome,
    required this.progress,
    this.diagnostics = const [],
    this.diagnosticCounts = const {},
    this.error,
  });

  final CsvImportOutcome outcome;
  final CsvImportProgress progress;

  /// Capped at [kCsvMaxRetainedDiagnostics]; [diagnosticCounts] stays complete.
  final List<CsvImportDiagnostic> diagnostics;

  /// Every rejection, counted by reason, with nothing dropped.
  final Map<CsvImportDiagnosticReason, int> diagnosticCounts;

  final String? error;

  bool get wroteNothing => progress.written == 0;
}

/// Matches the Apple importer's cap: grouped counts stay exact, the per-row log
/// stops growing so a re-import of an already-imported file cannot produce an
/// unbounded report.
const int kCsvMaxRetainedDiagnostics = 1000;
