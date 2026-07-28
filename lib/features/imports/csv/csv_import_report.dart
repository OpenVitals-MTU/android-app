/// The text report a finished CSV import can hand back to the user.
///
/// Pure: takes the run's inputs and outcome, returns a string. No clock, no I/O,
/// no context — so it is trivially testable and can be rendered from anywhere.
///
/// Deliberately NOT sanitised, exactly like the Apple Health import report: it is
/// an explicit user export for troubleshooting, so it names the file, the columns
/// and the values that were rejected. It carries no health values that the user's
/// own CSV did not already contain.
library;

import 'csv_column_mapping.dart';
import 'csv_datetime_format.dart';
import 'csv_import_metric.dart';
import 'csv_import_models.dart';

/// The suggested file name for a saved CSV import report.
const String kCsvImportReportFileName = 'openvitals-csv-import-report.txt';

/// Renders [result] and the mapping that produced it as plain text.
///
/// The file name and dialect are passed in from the run rather than re-derived,
/// so the report describes what actually happened rather than what a fresh sniff
/// would decide now.
String buildCsvImportReport({
  required String? fileName,
  required CsvImportMapping mapping,
  required CsvImportResult result,
  required List<String> headerRow,
  String? fieldDelimiter,
  bool? hasHeaderRow,
}) {
  final buffer = StringBuffer()
    ..writeln('OpenVitals CSV import report')
    ..writeln('===========================')
    ..writeln()
    ..writeln('File: ${fileName ?? '(unnamed)'}')
    ..writeln('Outcome: ${_outcomeLabel(result.outcome)}');
  if (result.error case final error?) {
    buffer.writeln('Error: $error');
  }

  buffer
    ..writeln()
    ..writeln('Totals')
    ..writeln('------')
    ..writeln('Rows read:       ${result.progress.rowsRead}')
    ..writeln('Records written: ${result.progress.written}')
    ..writeln('Already present: ${result.progress.alreadyPresent}')
    ..writeln('Rejected:        ${result.progress.rejected}')
    ..writeln()
    ..writeln('Parsing')
    ..writeln('-------');
  if (fieldDelimiter != null) {
    buffer.writeln('Separator:  ${_delimiterLabel(fieldDelimiter)}');
  }
  if (hasHeaderRow != null) {
    buffer.writeln('Header row: ${hasHeaderRow ? 'yes' : 'no'}');
  }
  buffer
    ..writeln('Date format: ${mapping.dateTime.format.name}')
    ..writeln('Time zone:   ${_zoneLabel(mapping.dateTime)}');
  if (mapping.dateTime.customPattern case final pattern?
      when pattern.isNotEmpty) {
    buffer.writeln('Custom pattern: $pattern');
  }

  buffer
    ..writeln()
    ..writeln('Column mapping')
    ..writeln('--------------');
  for (final column in mapping.columns) {
    final header = column.columnIndex < headerRow.length
        ? headerRow[column.columnIndex]
        : 'Column ${column.columnIndex + 1}';
    buffer.writeln(
      '[${column.columnIndex}] $header -> ${_roleLabel(column)}',
    );
  }

  if (result.diagnosticCounts.isNotEmpty) {
    buffer
      ..writeln()
      ..writeln('Rejections by reason')
      ..writeln('--------------------');
    // Complete counts, never truncated — the per-row log below is what gets
    // capped.
    for (final entry in result.diagnosticCounts.entries) {
      buffer.writeln('${_reasonLabel(entry.key)}: ${entry.value}');
    }
  }

  if (result.diagnostics.isNotEmpty) {
    final total = result.diagnosticCounts.values
        .fold<int>(0, (sum, count) => sum + count);
    buffer
      ..writeln()
      ..writeln('Rejected rows')
      ..writeln('-------------');
    for (final diagnostic in result.diagnostics) {
      final column = diagnostic.columnIndex == null
          ? ''
          : ' column ${diagnostic.columnIndex}';
      final detail = diagnostic.detail == null ? '' : ' (${diagnostic.detail})';
      buffer.writeln(
        'Row ${diagnostic.rowNumber}$column: '
        '${_reasonLabel(diagnostic.reason)}$detail',
      );
    }
    if (total > result.diagnostics.length) {
      buffer.writeln(
        '... and ${total - result.diagnostics.length} more '
        '(the per-row log is capped at $kCsvMaxRetainedDiagnostics; '
        'the counts above are complete).',
      );
    }
  }

  return buffer.toString();
}

String _outcomeLabel(CsvImportOutcome outcome) => switch (outcome) {
      CsvImportOutcome.completed => 'completed',
      CsvImportOutcome.cancelled => 'cancelled by the user',
      CsvImportOutcome.rateLimited => 'stopped — Health Connect rate limit',
      CsvImportOutcome.failed => 'failed',
    };

String _delimiterLabel(String delimiter) => switch (delimiter) {
      ',' => 'comma',
      ';' => 'semicolon',
      '\t' => 'tab',
      '|' => 'pipe',
      _ => delimiter,
    };

String _zoneLabel(CsvDateTimeSettings settings) => switch (settings.zone) {
      CsvTimeZoneMode.device => 'device time zone',
      CsvTimeZoneMode.utc => 'UTC',
      CsvTimeZoneMode.fixedOffset =>
        'fixed offset ${_offsetLabel(settings.fixedOffset)}',
    };

String _offsetLabel(Duration? offset) {
  if (offset == null) return '+00:00';
  final sign = offset.isNegative ? '-' : '+';
  final absolute = offset.abs();
  final hours = absolute.inHours.toString().padLeft(2, '0');
  final minutes = (absolute.inMinutes % 60).toString().padLeft(2, '0');
  return '$sign$hours:$minutes';
}

String _roleLabel(CsvColumnMapping column) {
  switch (column.role) {
    case CsvColumnRole.ignore:
      return 'not imported';
    case CsvColumnRole.timestamp:
      return 'date and time';
    case CsvColumnRole.metric:
      final metric = column.metric;
      if (metric == null) return 'not imported';
      final interpretation = column.effectiveInterpretation;
      return '${metric.name} (${_interpretationLabel(interpretation)})';
  }
}

String _interpretationLabel(CsvValueInterpretation? interpretation) =>
    switch (interpretation) {
      CsvDirectValue(:final unit) => unit.name,
      CsvMassShareOfWeight(:final unit) =>
        '${unit.name} as a share of the weight column',
      null => 'default',
    };

String _reasonLabel(CsvImportDiagnosticReason reason) => switch (reason) {
      CsvImportDiagnosticReason.missingTimestamp => 'no date and time',
      CsvImportDiagnosticReason.unparsableTimestamp => 'date not understood',
      CsvImportDiagnosticReason.wrongFieldCount => 'too few columns',
      CsvImportDiagnosticReason.unparsableNumber => 'value not a number',
      CsvImportDiagnosticReason.outOfRange => 'value outside a plausible range',
      CsvImportDiagnosticReason.derivationMissingWeight =>
        'no weight to derive the percentage from',
      CsvImportDiagnosticReason.writeFailed =>
        'Health Connect refused the record',
    };
