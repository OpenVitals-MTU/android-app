import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/imports/csv/csv_column_mapping.dart';
import 'package:openvitals/features/imports/csv/csv_datetime_format.dart';
import 'package:openvitals/features/imports/csv/csv_import_metric.dart';

const List<List<String>> _sample = [
  ['2026-07-01 08:12:00', '78.4', '15.2'],
  ['2026-07-02 08:14:00', '78.6', '15.3'],
];

CsvImportMapping mappingOf(
  List<CsvColumnMapping> columns, {
  CsvDateTimeSettings dateTime = const CsvDateTimeSettings(
    format: CsvDateTimeFormat.yearFirst,
    zone: CsvTimeZoneMode.utc,
  ),
}) =>
    CsvImportMapping(columns: columns, dateTime: dateTime);

void main() {
  group('validateCsvMapping', () {
    test('a complete mapping reports no issues', () {
      final issues = validateCsvMapping(
        mappingOf(const [
          CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
          CsvColumnMapping(
            columnIndex: 1,
            role: CsvColumnRole.metric,
            metric: CsvImportMetric.weight,
            interpretation: CsvDirectValue(CsvUnit.kilograms),
          ),
        ]),
        _sample,
      );

      expect(issues, isEmpty);
    });

    test('a mapping with no timestamp column reports it', () {
      final issues = validateCsvMapping(
        mappingOf(const [
          CsvColumnMapping(
            columnIndex: 1,
            role: CsvColumnRole.metric,
            metric: CsvImportMetric.weight,
            interpretation: CsvDirectValue(CsvUnit.kilograms),
          ),
        ]),
        _sample,
      );

      expect(issues, contains(CsvMappingIssue.noTimestampColumn));
    });

    test('two timestamp columns report the conflict', () {
      final issues = validateCsvMapping(
        mappingOf(const [
          CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
          CsvColumnMapping(columnIndex: 1, role: CsvColumnRole.timestamp),
        ]),
        _sample,
      );

      expect(issues, contains(CsvMappingIssue.multipleTimestampColumns));
    });

    test('a mapping with no metric column reports it', () {
      final issues = validateCsvMapping(
        mappingOf(const [
          CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
        ]),
        _sample,
      );

      expect(issues, contains(CsvMappingIssue.noMetricColumns));
    });

    test('two columns mapped to the same metric report the duplicate', () {
      final issues = validateCsvMapping(
        mappingOf(const [
          CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
          CsvColumnMapping(
            columnIndex: 1,
            role: CsvColumnRole.metric,
            metric: CsvImportMetric.weight,
            interpretation: CsvDirectValue(CsvUnit.kilograms),
          ),
          CsvColumnMapping(
            columnIndex: 2,
            role: CsvColumnRole.metric,
            metric: CsvImportMetric.weight,
            interpretation: CsvDirectValue(CsvUnit.kilograms),
          ),
        ]),
        _sample,
      );

      expect(issues, contains(CsvMappingIssue.duplicateMetric));
    });

    test(
      'body fat as a mass with no weight column reports that it needs one',
      () {
        final issues = validateCsvMapping(
          mappingOf(const [
            CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
            CsvColumnMapping(
              columnIndex: 2,
              role: CsvColumnRole.metric,
              metric: CsvImportMetric.bodyFat,
              interpretation: CsvMassShareOfWeight(CsvUnit.kilograms),
            ),
          ]),
          _sample,
        );

        expect(issues, contains(CsvMappingIssue.massShareNeedsWeightColumn));
      },
    );

    test('body fat as a percentage needs no weight column', () {
      final issues = validateCsvMapping(
        mappingOf(const [
          CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
          CsvColumnMapping(
            columnIndex: 2,
            role: CsvColumnRole.metric,
            metric: CsvImportMetric.bodyFat,
            interpretation: CsvDirectValue(CsvUnit.percent),
          ),
        ]),
        _sample,
      );

      expect(issues, isNot(contains(CsvMappingIssue.massShareNeedsWeightColumn)));
    });

    test('a date format matching no sampled row reports it', () {
      final issues = validateCsvMapping(
        mappingOf(
          const [
            CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
            CsvColumnMapping(
              columnIndex: 1,
              role: CsvColumnRole.metric,
              metric: CsvImportMetric.weight,
              interpretation: CsvDirectValue(CsvUnit.kilograms),
            ),
          ],
          dateTime: const CsvDateTimeSettings(
            format: CsvDateTimeFormat.epochSeconds,
          ),
        ),
        _sample,
      );

      expect(
        issues,
        contains(CsvMappingIssue.timestampFormatMatchesNoSampleRow),
      );
    });

    test(
      'an undecidable day/month order is reported while the format is still '
      'automatic',
      () {
        final issues = validateCsvMapping(
          mappingOf(
            const [
              CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
              CsvColumnMapping(
                columnIndex: 1,
                role: CsvColumnRole.metric,
                metric: CsvImportMetric.weight,
                interpretation: CsvDirectValue(CsvUnit.kilograms),
              ),
            ],
            dateTime: const CsvDateTimeSettings(),
          ),
          const [
            ['01/07/2026', '78.4'],
            ['02/08/2026', '78.6'],
          ],
        );

        expect(issues, contains(CsvMappingIssue.ambiguousDayMonthOrder));
      },
    );

    test(
      'choosing day-first answers the ambiguity and clears the issue',
      () {
        // Once the user has said which ordering it is, repeating the question
        // would block a mapping that is now fully specified.
        final issues = validateCsvMapping(
          mappingOf(
            const [
              CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
              CsvColumnMapping(
                columnIndex: 1,
                role: CsvColumnRole.metric,
                metric: CsvImportMetric.weight,
                interpretation: CsvDirectValue(CsvUnit.kilograms),
              ),
            ],
            dateTime: const CsvDateTimeSettings(
              format: CsvDateTimeFormat.dayFirst,
              zone: CsvTimeZoneMode.utc,
            ),
          ),
          const [
            ['01/07/2026', '78.4'],
            ['02/08/2026', '78.6'],
          ],
        );

        expect(issues, isEmpty);
      },
    );
  });

  group('initialCsvMapping', () {
    test('the first column that parses as a date is pre-selected', () {
      final mapping = initialCsvMapping(
        headerRow: const ['Date', 'Weight (kg)', 'Fat mass (kg)'],
        sample: _sample,
      );

      expect(mapping.timestampColumn?.columnIndex, 0);
    });

    test('no metric is guessed from a header name', () {
      // Guessing metrics from labels would be the vendor-preset behaviour this
      // importer deliberately does without.
      final mapping = initialCsvMapping(
        headerRow: const ['Date', 'Weight (kg)', 'Fat mass (kg)'],
        sample: _sample,
      );

      expect(mapping.metricColumns, isEmpty);
    });

    test('a file with no date-like column selects no timestamp', () {
      final mapping = initialCsvMapping(
        headerRow: const ['A', 'B'],
        sample: const [
          ['x', '1'],
          ['y', '2'],
        ],
      );

      expect(mapping.timestampColumn, isNull);
    });
  });

  group('requiredWritePermissions', () {
    test('only the mapped metrics permissions are required', () {
      final mapping = mappingOf(const [
        CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
        CsvColumnMapping(
          columnIndex: 1,
          role: CsvColumnRole.metric,
          metric: CsvImportMetric.weight,
          interpretation: CsvDirectValue(CsvUnit.kilograms),
        ),
      ]);

      expect(mapping.requiredWritePermissions, hasLength(1));
      expect(
        mapping.requiredWritePermissions.single,
        'android.permission.health.WRITE_WEIGHT',
      );
    });

    test('a body-composition mapping requires one permission per metric', () {
      final mapping = mappingOf(const [
        CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
        CsvColumnMapping(
          columnIndex: 1,
          role: CsvColumnRole.metric,
          metric: CsvImportMetric.weight,
          interpretation: CsvDirectValue(CsvUnit.kilograms),
        ),
        CsvColumnMapping(
          columnIndex: 2,
          role: CsvColumnRole.metric,
          metric: CsvImportMetric.boneMass,
          interpretation: CsvDirectValue(CsvUnit.kilograms),
        ),
      ]);

      expect(mapping.requiredWritePermissions, {
        'android.permission.health.WRITE_WEIGHT',
        'android.permission.health.WRITE_BONE_MASS',
      });
    });
  });

  group('detectCsvUnitInHeader', () {
    test('a parenthesised unit is read off the header', () {
      expect(detectCsvUnitInHeader('Weight (kg)'), CsvUnit.kilograms);
      expect(detectCsvUnitInHeader('Fat mass (lb)'), CsvUnit.pounds);
      expect(detectCsvUnitInHeader('Body fat (%)'), CsvUnit.percent);
    });

    test('a unit word inside the label is not read as the unit', () {
      // Only the parenthesised tail counts, so this cannot become grams.
      expect(detectCsvUnitInHeader('Weight in grams of food'), isNull);
    });

    test('a header with no unit reads as none', () {
      expect(detectCsvUnitInHeader('Comments'), isNull);
      expect(detectCsvUnitInHeader('Date'), isNull);
    });
  });
}
