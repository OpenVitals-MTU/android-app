import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/imports/csv/csv_column_mapping.dart';
import 'package:openvitals/features/imports/csv/csv_datetime_format.dart';
import 'package:openvitals/features/imports/csv/csv_import_metric.dart';
import 'package:openvitals/features/imports/csv/csv_import_models.dart';
import 'package:openvitals/features/imports/csv/csv_import_report.dart';

const _headerRow = ['Date', 'Weight (kg)', 'Fat mass (kg)', 'Comments'];

CsvImportMapping _mapping({
  CsvValueInterpretation bodyFat = const CsvMassShareOfWeight(
    CsvUnit.kilograms,
  ),
  CsvDateTimeSettings? dateTime,
}) =>
    CsvImportMapping(
      columns: [
        const CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
        const CsvColumnMapping(
          columnIndex: 1,
          role: CsvColumnRole.metric,
          metric: CsvImportMetric.weight,
          interpretation: CsvDirectValue(CsvUnit.kilograms),
        ),
        CsvColumnMapping(
          columnIndex: 2,
          role: CsvColumnRole.metric,
          metric: CsvImportMetric.bodyFat,
          interpretation: bodyFat,
        ),
        const CsvColumnMapping(columnIndex: 3),
      ],
      dateTime: dateTime ??
          const CsvDateTimeSettings(
            format: CsvDateTimeFormat.yearFirst,
            zone: CsvTimeZoneMode.utc,
          ),
    );

CsvImportResult _result({
  CsvImportOutcome outcome = CsvImportOutcome.completed,
  List<CsvImportDiagnostic> diagnostics = const [],
  Map<CsvImportDiagnosticReason, int> counts = const {},
  String? error,
}) =>
    CsvImportResult(
      outcome: outcome,
      progress: const CsvImportProgress(
        rowsRead: 120,
        written: 118,
        alreadyPresent: 4,
        rejected: 2,
      ),
      diagnostics: diagnostics,
      diagnosticCounts: counts,
      error: error,
    );

String _report({
  CsvImportMapping? mapping,
  CsvImportResult? result,
  String? fileName = 'withings.csv',
  String? delimiter = ',',
  bool? hasHeader = true,
}) =>
    buildCsvImportReport(
      fileName: fileName,
      mapping: mapping ?? _mapping(),
      result: result ?? _result(),
      headerRow: _headerRow,
      fieldDelimiter: delimiter,
      hasHeaderRow: hasHeader,
    );

void main() {
  group('buildCsvImportReport', () {
    test('the report names the file and the outcome', () {
      final report = _report();

      expect(report, contains('File: withings.csv'));
      expect(report, contains('Outcome: completed'));
    });

    test('an unnamed file is reported rather than left blank', () {
      expect(_report(fileName: null), contains('File: (unnamed)'));
    });

    test('every tally from the run appears', () {
      final report = _report();

      expect(report, contains('Rows read:       120'));
      expect(report, contains('Records written: 118'));
      expect(report, contains('Already present: 4'));
      expect(report, contains('Rejected:        2'));
    });

    test('the parsing settings that produced the run are recorded', () {
      final report = _report(delimiter: ';', hasHeader: false);

      expect(report, contains('Separator:  semicolon'));
      expect(report, contains('Header row: no'));
      expect(report, contains('Date format: yearFirst'));
      expect(report, contains('Time zone:   UTC'));
    });

    test('a fixed offset is written out in full', () {
      final report = _report(
        mapping: _mapping(
          dateTime: const CsvDateTimeSettings(
            format: CsvDateTimeFormat.yearFirst,
            zone: CsvTimeZoneMode.fixedOffset,
            fixedOffset: Duration(hours: -5, minutes: -30),
          ),
        ),
      );

      expect(report, contains('fixed offset -05:30'));
    });

    test('a custom date pattern is recorded so a bad one can be spotted', () {
      final report = _report(
        mapping: _mapping(
          dateTime: const CsvDateTimeSettings(
            format: CsvDateTimeFormat.custom,
            customPattern: 'dd MMM yyyy HH:mm',
          ),
        ),
      );

      expect(report, contains('Custom pattern: dd MMM yyyy HH:mm'));
    });

    test('every column is listed with the role it was given', () {
      final report = _report();

      expect(report, contains('[0] Date -> date and time'));
      expect(report, contains('[1] Weight (kg) -> weight (kilograms)'));
      expect(report, contains('[3] Comments -> not imported'));
    });

    test(
      'a derived body fat says what it was derived from, not just its unit',
      () {
        // "kilograms" alone on a body-fat column would misdescribe what was
        // written, which is the thing a troubleshooting report must not do.
        expect(
          _report(),
          contains(
            '[2] Fat mass (kg) -> bodyFat '
            '(kilograms as a share of the weight column)',
          ),
        );
      },
    );

    test('rejection counts are grouped by reason', () {
      final report = _report(
        result: _result(
          counts: const {
            CsvImportDiagnosticReason.unparsableTimestamp: 2,
            CsvImportDiagnosticReason.outOfRange: 1,
          },
        ),
      );

      expect(report, contains('date not understood: 2'));
      expect(report, contains('value outside a plausible range: 1'));
    });

    test('individual rejected rows name the row, column and value', () {
      final report = _report(
        result: _result(
          diagnostics: const [
            CsvImportDiagnostic(
              rowNumber: 7,
              reason: CsvImportDiagnosticReason.unparsableNumber,
              columnIndex: 2,
              detail: 'n/a',
            ),
          ],
          counts: const {CsvImportDiagnosticReason.unparsableNumber: 1},
        ),
      );

      expect(report, contains('Row 7 column 2: value not a number (n/a)'));
    });

    test(
      'a capped per-row log says how many were dropped and that the counts are '
      'not',
      () {
        final report = _report(
          result: _result(
            diagnostics: const [
              CsvImportDiagnostic(
                rowNumber: 2,
                reason: CsvImportDiagnosticReason.unparsableTimestamp,
              ),
            ],
            counts: const {CsvImportDiagnosticReason.unparsableTimestamp: 51},
          ),
        );

        expect(report, contains('... and 50 more'));
        expect(report, contains('the counts above are complete'));
      },
    );

    test('a clean run has no rejection sections at all', () {
      final report = _report();

      expect(report, isNot(contains('Rejections by reason')));
      expect(report, isNot(contains('Rejected rows')));
    });

    test('a failed run carries its error text', () {
      final report = _report(
        result: _result(
          outcome: CsvImportOutcome.failed,
          error: 'File not found',
        ),
      );

      expect(report, contains('Outcome: failed'));
      expect(report, contains('Error: File not found'));
    });

    test('a rate-limited run says so rather than reading as a plain stop', () {
      expect(
        _report(result: _result(outcome: CsvImportOutcome.rateLimited)),
        contains('Health Connect rate limit'),
      );
    });
  });
}
