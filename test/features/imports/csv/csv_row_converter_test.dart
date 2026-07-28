import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';
import 'package:openvitals/features/imports/csv/csv_column_mapping.dart';
import 'package:openvitals/features/imports/csv/csv_datetime_format.dart';
import 'package:openvitals/features/imports/csv/csv_import_metric.dart';
import 'package:openvitals/features/imports/csv/csv_import_models.dart';
import 'package:openvitals/features/imports/csv/csv_row_converter.dart';
import 'package:openvitals/features/imports/csv/csv_table_reader.dart';

/// The Withings scale export this feature was built for:
/// `Date,"Weight (kg)","Fat mass (kg)","Bone mass (kg)","Muscle mass (kg)","Hydration (kg)",Comments`
CsvImportMapping withingsMapping({
  CsvValueInterpretation? bodyFatInterpretation,
  bool includeWeight = true,
}) =>
    CsvImportMapping(
      columns: [
        const CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
        CsvColumnMapping(
          columnIndex: 1,
          role: includeWeight ? CsvColumnRole.metric : CsvColumnRole.ignore,
          metric: includeWeight ? CsvImportMetric.weight : null,
          interpretation:
              includeWeight ? const CsvDirectValue(CsvUnit.kilograms) : null,
        ),
        CsvColumnMapping(
          columnIndex: 2,
          role: CsvColumnRole.metric,
          metric: CsvImportMetric.bodyFat,
          interpretation: bodyFatInterpretation ??
              const CsvMassShareOfWeight(CsvUnit.kilograms),
        ),
        const CsvColumnMapping(
          columnIndex: 3,
          role: CsvColumnRole.metric,
          metric: CsvImportMetric.boneMass,
          interpretation: CsvDirectValue(CsvUnit.kilograms),
        ),
        const CsvColumnMapping(
          columnIndex: 4,
          role: CsvColumnRole.metric,
          metric: CsvImportMetric.leanBodyMass,
          interpretation: CsvDirectValue(CsvUnit.kilograms),
        ),
        const CsvColumnMapping(
          columnIndex: 5,
          role: CsvColumnRole.metric,
          metric: CsvImportMetric.bodyWaterMass,
          interpretation: CsvDirectValue(CsvUnit.kilograms),
        ),
        const CsvColumnMapping(columnIndex: 6),
      ],
      dateTime: const CsvDateTimeSettings(
        format: CsvDateTimeFormat.yearFirst,
        zone: CsvTimeZoneMode.utc,
      ),
    );

CsvRow row(List<String> fields, {int rowNumber = 2}) =>
    CsvRow(rowNumber: rowNumber, fields: fields);

void main() {
  group('convertCsvRow', () {
    test('a Withings row produces one record for each mapped metric', () {
      final conversion = convertCsvRow(
        row: row(['2026-07-01 08:12:00', '78.4', '15.2', '3.1', '55.0', '42.3', '']),
        mapping: withingsMapping(),
      );

      expect(conversion.diagnostics, isEmpty);
      expect(
        conversion.records.map((it) => it.targetType).toList(),
        [
          'WeightRecord',
          'BodyFatRecord',
          'BoneMassRecord',
          'LeanBodyMassRecord',
          'BodyWaterMassRecord',
        ],
      );
    });

    test('fat mass in kilograms becomes a body-fat percentage of the row weight',
        () {
      final conversion = convertCsvRow(
        row: row(['2026-07-01 08:12:00', '78.4', '15.2', '3.1', '55.0', '42.3', '']),
        mapping: withingsMapping(),
      );

      final bodyFat = conversion.records.whereType<BodyFatImportRecord>().single;

      // 15.2 / 78.4 * 100
      expect(bodyFat.percent, closeTo(19.3877, 0.001));
    });

    test(
      'a fat-mass row with no weight value keeps its other metrics and reports '
      'the missing derivation',
      () {
        final conversion = convertCsvRow(
          row: row(['2026-07-01 08:12:00', '', '15.2', '3.1', '55.0', '42.3', '']),
          mapping: withingsMapping(),
        );

        expect(conversion.records.map((it) => it.targetType), [
          'BoneMassRecord',
          'LeanBodyMassRecord',
          'BodyWaterMassRecord',
        ]);
        expect(
          conversion.diagnostics.single.reason,
          CsvImportDiagnosticReason.derivationMissingWeight,
        );
      },
    );

    test('body fat given directly as a percentage needs no weight column', () {
      final conversion = convertCsvRow(
        row: row(['2026-07-01 08:12:00', '', '19.4', '', '', '', '']),
        mapping: withingsMapping(
          bodyFatInterpretation: const CsvDirectValue(CsvUnit.percent),
          includeWeight: false,
        ),
      );

      expect(conversion.diagnostics, isEmpty);
      expect(
        conversion.records.whereType<BodyFatImportRecord>().single.percent,
        19.4,
      );
    });

    test('a weight in pounds converts to kilograms', () {
      final conversion = convertCsvRow(
        row: row(['2026-07-01 08:12:00', '172.8']),
        mapping: CsvImportMapping(
          columns: const [
            CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
            CsvColumnMapping(
              columnIndex: 1,
              role: CsvColumnRole.metric,
              metric: CsvImportMetric.weight,
              interpretation: CsvDirectValue(CsvUnit.pounds),
            ),
          ],
          dateTime: const CsvDateTimeSettings(
            format: CsvDateTimeFormat.yearFirst,
            zone: CsvTimeZoneMode.utc,
          ),
        ),
      );

      expect(
        conversion.records.whereType<WeightImportRecord>().single.kilograms,
        closeTo(78.38, 0.01),
      );
    });

    test('a height in centimetres is stored in metres', () {
      final conversion = convertCsvRow(
        row: row(['2026-07-01 08:12:00', '183']),
        mapping: CsvImportMapping(
          columns: const [
            CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
            CsvColumnMapping(
              columnIndex: 1,
              role: CsvColumnRole.metric,
              metric: CsvImportMetric.height,
              interpretation: CsvDirectValue(CsvUnit.centimeters),
            ),
          ],
          dateTime: const CsvDateTimeSettings(
            format: CsvDateTimeFormat.yearFirst,
            zone: CsvTimeZoneMode.utc,
          ),
        ),
      );

      expect(
        conversion.records.whereType<HeightImportRecord>().single.meters,
        closeTo(1.83, 0.0001),
      );
    });

    test('an unparsable timestamp rejects the whole row', () {
      final conversion = convertCsvRow(
        row: row(['not a date', '78.4', '15.2', '3.1', '55.0', '42.3', '']),
        mapping: withingsMapping(),
      );

      expect(conversion.records, isEmpty);
      expect(
        conversion.diagnostics.single.reason,
        CsvImportDiagnosticReason.unparsableTimestamp,
      );
    });

    test('an empty timestamp cell rejects the whole row', () {
      final conversion = convertCsvRow(
        row: row(['', '78.4', '15.2', '3.1', '55.0', '42.3', '']),
        mapping: withingsMapping(),
      );

      expect(conversion.records, isEmpty);
      expect(
        conversion.diagnostics.single.reason,
        CsvImportDiagnosticReason.missingTimestamp,
      );
    });

    test('a row shorter than the mapped columns is rejected as malformed', () {
      final conversion = convertCsvRow(
        row: row(['2026-07-01 08:12:00', '78.4']),
        mapping: withingsMapping(),
      );

      expect(conversion.records, isEmpty);
      expect(
        conversion.diagnostics.single.reason,
        CsvImportDiagnosticReason.wrongFieldCount,
      );
    });

    test(
      'an implausible weight is rejected while the rest of the row still lands',
      () {
        final conversion = convertCsvRow(
          row: row(['2026-07-01 08:12:00', '900', '15.2', '3.1', '55.0', '42.3', '']),
          mapping: withingsMapping(
            bodyFatInterpretation: const CsvDirectValue(CsvUnit.percent),
          ),
        );

        expect(
          conversion.diagnostics.single.reason,
          CsvImportDiagnosticReason.outOfRange,
        );
        expect(conversion.records.whereType<WeightImportRecord>(), isEmpty);
        expect(conversion.records.whereType<BoneMassImportRecord>(), hasLength(1));
      },
    );

    test(
      'a derived body fat outside the plausible range is rejected rather than '
      'stored',
      () {
        // Fat mass divided by a weight column that is not body weight — the
        // failure this guard exists for.
        final conversion = convertCsvRow(
          row: row(['2026-07-01 08:12:00', '16.0', '15.2', '3.1', '55.0', '42.3', '']),
          mapping: withingsMapping(),
        );

        expect(
          conversion.diagnostics.map((it) => it.reason),
          contains(CsvImportDiagnosticReason.outOfRange),
        );
        expect(conversion.records.whereType<BodyFatImportRecord>(), isEmpty);
      },
    );

    test('an unparsable metric cell costs only that metric', () {
      final conversion = convertCsvRow(
        row: row(['2026-07-01 08:12:00', '78.4', 'n/a', '3.1', '55.0', '42.3', '']),
        mapping: withingsMapping(
          bodyFatInterpretation: const CsvDirectValue(CsvUnit.percent),
        ),
      );

      expect(
        conversion.diagnostics.single.reason,
        CsvImportDiagnosticReason.unparsableNumber,
      );
      expect(conversion.records, hasLength(4));
    });

    test('a blank metric cell is a gap, not an error', () {
      final conversion = convertCsvRow(
        row: row(['2026-07-01 08:12:00', '78.4', '', '3.1', '55.0', '42.3', '']),
        mapping: withingsMapping(),
      );

      expect(conversion.diagnostics, isEmpty);
      expect(conversion.records.whereType<BodyFatImportRecord>(), isEmpty);
      expect(conversion.records, hasLength(4));
    });

    test('the record carries the resolved instant and its wall-clock offset', () {
      final conversion = convertCsvRow(
        row: row(['2026-07-01 08:12:00', '78.4']),
        mapping: CsvImportMapping(
          columns: const [
            CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
            CsvColumnMapping(
              columnIndex: 1,
              role: CsvColumnRole.metric,
              metric: CsvImportMetric.weight,
              interpretation: CsvDirectValue(CsvUnit.kilograms),
            ),
          ],
          dateTime: const CsvDateTimeSettings(
            format: CsvDateTimeFormat.yearFirst,
            zone: CsvTimeZoneMode.fixedOffset,
            fixedOffset: Duration(hours: 2),
          ),
        ),
      );

      final record = conversion.records.whereType<WeightImportRecord>().single;
      expect(record.time, DateTime.utc(2026, 7, 1, 6, 12));
      expect(record.zoneOffset, const Duration(hours: 2));
    });
  });

  group('buildCsvClientRecordId', () {
    test('the id is namespaced to csv so it cannot collide with apple_health',
        () {
      final id = buildCsvClientRecordId(
        targetType: 'WeightRecord',
        utc: DateTime.utc(2026, 7, 1, 6, 12),
      );

      expect(id, startsWith('csv_weightrecord_'));
    });

    test('the same measurement in pounds and kilograms yields the same id', () {
      final metric = convertCsvRow(
        row: row(['2026-07-01 08:12:00', '78.4']),
        mapping: _weightMapping(const CsvDirectValue(CsvUnit.kilograms)),
      ).records.single;
      final imperial = convertCsvRow(
        row: row(['2026-07-01 08:12:00', '172.84']),
        mapping: _weightMapping(const CsvDirectValue(CsvUnit.pounds)),
      ).records.single;

      expect(imperial.clientRecordId, metric.clientRecordId);
    });

    test(
      'a corrected value at the same instant keeps the id, so the re-import '
      'replaces the record instead of duplicating it',
      () {
        // This IS the upsert contract: Health Connect replaces on a matching
        // clientRecordId, so excluding the value from the id is what makes a
        // corrected file overwrite rather than double up.
        final before = convertCsvRow(
          row: row(['2026-07-01 08:12:00', '78.4']),
          mapping: _weightMapping(const CsvDirectValue(CsvUnit.kilograms)),
        ).records.single;
        final after = convertCsvRow(
          row: row(['2026-07-01 08:12:00', '78.6']),
          mapping: _weightMapping(const CsvDirectValue(CsvUnit.kilograms)),
        ).records.single;

        expect(after.clientRecordId, before.clientRecordId);
        expect(
          (after as WeightImportRecord).kilograms,
          isNot((before as WeightImportRecord).kilograms),
        );
      },
    );

    test('a different instant yields a different id', () {
      final first = buildCsvClientRecordId(
        targetType: 'WeightRecord',
        utc: DateTime.utc(2026, 7, 1, 6, 12),
      );
      final second = buildCsvClientRecordId(
        targetType: 'WeightRecord',
        utc: DateTime.utc(2026, 7, 2, 6, 12),
      );

      expect(second, isNot(first));
    });

    test('two metrics at the same instant get different ids', () {
      expect(
        buildCsvClientRecordId(
          targetType: 'WeightRecord',
          utc: DateTime.utc(2026, 7, 1),
        ),
        isNot(
          buildCsvClientRecordId(
            targetType: 'BodyFatRecord',
            utc: DateTime.utc(2026, 7, 1),
          ),
        ),
      );
    });
  });

  group('vitals metrics', () {
    test('a Withings temperature export row becomes a BodyTemperatureRecord',
        () {
      // The real file: date,"value (°C)" with quoted timestamps.
      final record = _convertOne(
        '36.6',
        CsvImportMetric.bodyTemperature,
        const CsvDirectValue(CsvUnit.celsius),
      );

      expect(record, isA<BodyTemperatureImportRecord>());
      expect((record as BodyTemperatureImportRecord).celsius, 36.6);
      expect(record.time, DateTime.utc(2023, 10, 9, 7, 8, 1));
    });

    test('a temperature in Fahrenheit converts to Celsius', () {
      final record = _convertOne(
        '98.6',
        CsvImportMetric.bodyTemperature,
        const CsvDirectValue(CsvUnit.fahrenheit),
      );

      expect(
        (record as BodyTemperatureImportRecord).celsius,
        closeTo(37.0, 0.001),
      );
    });

    test('a heart rate becomes a one-sample series at that instant', () {
      // Health Connect models heart rate as a series even for a spot reading.
      final record = _convertOne(
        '62',
        CsvImportMetric.heartRate,
        const CsvDirectValue(CsvUnit.beatsPerMinute),
      );

      expect(record, isA<HeartRateImportRecord>());
      final hr = record as HeartRateImportRecord;
      expect(hr.startTime, hr.endTime);
      expect(hr.samples, hasLength(1));
      expect(hr.samples.single.beatsPerMinute, 62);
      expect(hr.samples.single.time, hr.startTime);
    });

    test('a fractional heart rate rounds, because the record stores an int', () {
      final record = _convertOne(
        '61.6',
        CsvImportMetric.restingHeartRate,
        const CsvDirectValue(CsvUnit.beatsPerMinute),
      );

      expect((record as RestingHeartRateImportRecord).beatsPerMinute, 62);
    });

    test('HRV in seconds converts to milliseconds', () {
      final record = _convertOne(
        '0.045',
        CsvImportMetric.heartRateVariability,
        const CsvDirectValue(CsvUnit.seconds),
      );

      expect(
        (record as HeartRateVariabilityRmssdImportRecord).rmssdMillis,
        closeTo(45, 0.001),
      );
    });

    test('SpO2 given as a fraction becomes a percentage', () {
      final record = _convertOne(
        '0.97',
        CsvImportMetric.oxygenSaturation,
        const CsvDirectValue(CsvUnit.fraction),
      );

      expect(
        (record as OxygenSaturationImportRecord).percent,
        closeTo(97, 0.001),
      );
    });

    test('blood glucose in mg/dL converts to mmol/L', () {
      final record = _convertOne(
        '90',
        CsvImportMetric.bloodGlucose,
        const CsvDirectValue(CsvUnit.milligramsPerDeciliter),
      );

      expect(
        (record as BloodGlucoseImportRecord).millimolesPerLiter,
        closeTo(5.0, 0.001),
      );
    });

    test('respiratory rate and VO2 max map to their records', () {
      expect(
        _convertOne('14', CsvImportMetric.respiratoryRate,
            const CsvDirectValue(CsvUnit.breathsPerMinute)),
        isA<RespiratoryRateImportRecord>(),
      );
      expect(
        _convertOne('48', CsvImportMetric.vo2Max,
            const CsvDirectValue(CsvUnit.millilitersPerKgPerMinute)),
        isA<Vo2MaxImportRecord>(),
      );
    });

    test('basal body temperature is distinct from body temperature', () {
      final record = _convertOne(
        '36.4',
        CsvImportMetric.basalBodyTemperature,
        const CsvDirectValue(CsvUnit.celsius),
      );

      expect(record, isA<BasalBodyTemperatureImportRecord>());
      expect(record.targetType, 'BasalBodyTemperatureRecord');
    });

    test('a temperature of 300 is rejected as implausible', () {
      // A Fahrenheit column mapped as Celsius, most likely.
      final conversion = convertCsvRow(
        row: const CsvRow(rowNumber: 2, fields: ['2023-10-09 07:08:01', '300']),
        mapping: _singleMetricMapping(
          CsvImportMetric.bodyTemperature,
          const CsvDirectValue(CsvUnit.celsius),
        ),
      );

      expect(conversion.records, isEmpty);
      expect(
        conversion.diagnostics.single.reason,
        CsvImportDiagnosticReason.outOfRange,
      );
    });

    test('every catalog metric can build a record from its canonical value', () {
      // Guards the switch in buildCsvImportRecord against a metric added to the
      // enum without a case: the analyzer catches that, but this catches a case
      // that builds the WRONG record type.
      for (final metric in CsvImportMetric.values) {
        final spec = kCsvMetricCatalog[metric]!;
        final record = buildCsvImportRecord(
          metric: metric,
          value: (spec.plausibleMin + spec.plausibleMax) / 2,
          instant: CsvInstant(DateTime.utc(2026, 7, 1), Duration.zero),
        );
        expect(record.targetType, spec.targetType, reason: '$metric');
        expect(canonicalValueOf(record), isNotNull, reason: '$metric');
      }
    });
  });

  group('previewInstantRange', () {
    const rows = [
      ['2026-07-03 08:11:00', '78.2'],
      ['2026-07-01 08:12:00', '78.4'],
      ['2026-07-02 08:14:00', '78.6'],
    ];

    test('the span covers the earliest and latest row, not the file order', () {
      final range = previewInstantRange(
        rows: rows,
        mapping: _weightMapping(const CsvDirectValue(CsvUnit.kilograms)),
      );

      expect(range!.first, DateTime.utc(2026, 7, 1, 8, 12));
      expect(range.last, DateTime.utc(2026, 7, 3, 8, 11));
    });

    test(
      'reading the same file day-first instead of month-first moves the span '
      'to a different month',
      () {
        // The whole point of showing the span: `01/07` is plausible either way
        // on its own, but the RANGE it implies is not.
        const ambiguous = [
          ['01/07/2026', '78.4'],
          ['02/07/2026', '78.6'],
          ['03/07/2026', '78.2'],
        ];
        CsvImportMapping mappingFor(CsvDateTimeFormat format) =>
            CsvImportMapping(
              columns: const [
                CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
                CsvColumnMapping(
                  columnIndex: 1,
                  role: CsvColumnRole.metric,
                  metric: CsvImportMetric.weight,
                  interpretation: CsvDirectValue(CsvUnit.kilograms),
                ),
              ],
              dateTime: CsvDateTimeSettings(
                format: format,
                zone: CsvTimeZoneMode.utc,
              ),
            );

        final dayFirst = previewInstantRange(
          rows: ambiguous,
          mapping: mappingFor(CsvDateTimeFormat.dayFirst),
        );
        final monthFirst = previewInstantRange(
          rows: ambiguous,
          mapping: mappingFor(CsvDateTimeFormat.monthFirst),
        );

        // Day-first: three days in July. Month-first: three months, Jan–Mar.
        expect(dayFirst!.first.month, 7);
        expect(dayFirst.last.month, 7);
        expect(monthFirst!.first.month, 1);
        expect(monthFirst.last.month, 3);
      },
    );

    test('rows that do not parse are skipped rather than widening the span',
        () {
      final range = previewInstantRange(
        rows: const [
          ['2026-07-01 08:12:00', '78.4'],
          ['not a date', '78.5'],
          ['', '78.6'],
        ],
        mapping: _weightMapping(const CsvDirectValue(CsvUnit.kilograms)),
      );

      expect(range!.first, DateTime.utc(2026, 7, 1, 8, 12));
      expect(range.last, DateTime.utc(2026, 7, 1, 8, 12));
    });

    test('a sample where nothing parses reports no span', () {
      expect(
        previewInstantRange(
          rows: const [
            ['not a date', '78.4'],
          ],
          mapping: _weightMapping(const CsvDirectValue(CsvUnit.kilograms)),
        ),
        isNull,
      );
    });

    test('a mapping with no timestamp column reports no span', () {
      expect(
        previewInstantRange(
          rows: rows,
          mapping: CsvImportMapping(
            columns: const [
              CsvColumnMapping(
                columnIndex: 1,
                role: CsvColumnRole.metric,
                metric: CsvImportMetric.weight,
                interpretation: CsvDirectValue(CsvUnit.kilograms),
              ),
            ],
          ),
        ),
        isNull,
      );
    });

    test('the span is the wall clock in the file, not the UTC instant', () {
      // A +02:00 file says 08:12 on the wall; showing 06:12 would look like a
      // bug to the user comparing against their own spreadsheet.
      final range = previewInstantRange(
        rows: const [
          ['2026-07-01 08:12:00', '78.4'],
        ],
        mapping: CsvImportMapping(
          columns: const [
            CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
            CsvColumnMapping(
              columnIndex: 1,
              role: CsvColumnRole.metric,
              metric: CsvImportMetric.weight,
              interpretation: CsvDirectValue(CsvUnit.kilograms),
            ),
          ],
          dateTime: const CsvDateTimeSettings(
            format: CsvDateTimeFormat.yearFirst,
            zone: CsvTimeZoneMode.fixedOffset,
            fixedOffset: Duration(hours: 2),
          ),
        ),
      );

      expect(range!.first.hour, 8);
    });
  });

  group('parseCsvNumber', () {
    test('a comma decimal separator parses as a decimal, not a thousands mark',
        () {
      expect(parseCsvNumber('78,4'), 78.4);
    });

    test('a dot decimal separator parses unchanged', () {
      expect(parseCsvNumber('78.4'), 78.4);
    });

    test('European grouping with a comma decimal parses correctly', () {
      expect(parseCsvNumber('1.234,5'), 1234.5);
    });

    test('English grouping with a dot decimal parses correctly', () {
      expect(parseCsvNumber('1,234.5'), 1234.5);
    });

    test('a trailing unit is stripped rather than failing the cell', () {
      expect(parseCsvNumber('78.4 kg'), 78.4);
    });

    test('a negative value keeps its sign', () {
      expect(parseCsvNumber('-3.2'), -3.2);
    });

    test('a non-numeric cell parses as null', () {
      expect(parseCsvNumber('n/a'), isNull);
      expect(parseCsvNumber(''), isNull);
    });
  });
}

CsvImportMapping _weightMapping(CsvValueInterpretation interpretation) =>
    CsvImportMapping(
      columns: [
        const CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
        CsvColumnMapping(
          columnIndex: 1,
          role: CsvColumnRole.metric,
          metric: CsvImportMetric.weight,
          interpretation: interpretation,
        ),
      ],
      dateTime: const CsvDateTimeSettings(
        format: CsvDateTimeFormat.yearFirst,
        zone: CsvTimeZoneMode.utc,
      ),
    );

/// A mapping of one timestamp column plus one metric column.
CsvImportMapping _singleMetricMapping(
  CsvImportMetric metric,
  CsvValueInterpretation interpretation,
) =>
    CsvImportMapping(
      columns: [
        const CsvColumnMapping(columnIndex: 0, role: CsvColumnRole.timestamp),
        CsvColumnMapping(
          columnIndex: 1,
          role: CsvColumnRole.metric,
          metric: metric,
          interpretation: interpretation,
        ),
      ],
      dateTime: const CsvDateTimeSettings(
        format: CsvDateTimeFormat.yearFirst,
        zone: CsvTimeZoneMode.utc,
      ),
    );

ImportRecord _convertOne(
  String value,
  CsvImportMetric metric,
  CsvValueInterpretation interpretation,
) =>
    convertCsvRow(
      row: CsvRow(rowNumber: 2, fields: ['2023-10-09 07:08:01', value]),
      mapping: _singleMetricMapping(metric, interpretation),
    ).records.single;
