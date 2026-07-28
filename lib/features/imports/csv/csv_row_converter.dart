/// One CSV row → the Health Connect records it represents.
///
/// Pure and synchronous: no repository, no clock, no I/O. Everything the
/// conversion needs is the row, the mapping, and the catalog.
library;

import '../../../domain/model/apple_health_import_records.dart';
import '../../../domain/model/import_client_record_id.dart';
import 'csv_column_mapping.dart';
import 'csv_datetime_format.dart';
import 'csv_import_metric.dart';
import 'csv_import_models.dart';
import 'csv_table_reader.dart';

/// The namespace every CSV-imported record's `clientRecordId` carries.
///
/// Distinct from the Apple importer's so the two can never collide on the same
/// id and silently overwrite each other's records.
const String kCsvClientRecordIdNamespace = 'csv';

/// What one row produced.
class CsvRowConversion {
  const CsvRowConversion({
    this.records = const [],
    this.diagnostics = const [],
  });

  final List<ImportRecord> records;
  final List<CsvImportDiagnostic> diagnostics;
}

/// Converts [row] under [mapping].
///
/// Failure granularity matches the Apple importer: a bad **timestamp** or a row
/// too short costs the whole row, because nothing in it can be placed in time; a
/// bad **value** costs only that metric, so one unparsable body-fat cell does
/// not throw away a perfectly good weight.
CsvRowConversion convertCsvRow({
  required CsvRow row,
  required CsvImportMapping mapping,
}) {
  final timestampColumn = mapping.timestampColumn;
  if (timestampColumn == null) return const CsvRowConversion();

  final metricColumns = mapping.metricColumns;
  if (metricColumns.isEmpty) return const CsvRowConversion();

  // A row is "too short" only relative to what the mapping actually reads.
  // Trailing empty columns are normal in exports and must not reject the row.
  final highestIndex = [
    timestampColumn.columnIndex,
    for (final column in metricColumns) column.columnIndex,
  ].reduce((a, b) => a > b ? a : b);
  if (row.fields.length <= highestIndex) {
    return CsvRowConversion(
      diagnostics: [
        CsvImportDiagnostic(
          rowNumber: row.rowNumber,
          reason: CsvImportDiagnosticReason.wrongFieldCount,
          detail: '${row.fields.length} fields, needs ${highestIndex + 1}',
        ),
      ],
    );
  }

  final timestampText = row.cell(timestampColumn.columnIndex);
  if (timestampText == null) {
    return CsvRowConversion(
      diagnostics: [
        CsvImportDiagnostic(
          rowNumber: row.rowNumber,
          reason: CsvImportDiagnosticReason.missingTimestamp,
          columnIndex: timestampColumn.columnIndex,
        ),
      ],
    );
  }

  final instant = resolveCsvInstant(timestampText, mapping.dateTime);
  if (instant == null) {
    return CsvRowConversion(
      diagnostics: [
        CsvImportDiagnostic(
          rowNumber: row.rowNumber,
          reason: CsvImportDiagnosticReason.unparsableTimestamp,
          columnIndex: timestampColumn.columnIndex,
          detail: timestampText,
        ),
      ],
    );
  }

  // Resolved once per row, before the loop, because any number of mass-share
  // metrics can need it.
  final rowWeightKg = _resolveRowWeightKg(row, mapping);

  final records = <ImportRecord>[];
  final diagnostics = <CsvImportDiagnostic>[];

  for (final column in metricColumns) {
    final metric = column.metric!;
    final spec = kCsvMetricCatalog[metric];
    final interpretation = column.effectiveInterpretation;
    if (spec == null || interpretation == null) continue;

    final text = row.cell(column.columnIndex);
    // A blank cell is a gap in the data, not an error: scales skip metrics.
    if (text == null) continue;

    final raw = parseCsvNumber(text);
    if (raw == null) {
      diagnostics.add(
        CsvImportDiagnostic(
          rowNumber: row.rowNumber,
          reason: CsvImportDiagnosticReason.unparsableNumber,
          columnIndex: column.columnIndex,
          detail: text,
        ),
      );
      continue;
    }

    final double canonical;
    switch (interpretation) {
      case CsvDirectValue(:final unit):
        canonical = convertCsvValueToCanonical(raw, unit);
      case CsvMassShareOfWeight(:final unit):
        if (rowWeightKg == null || rowWeightKg <= 0) {
          diagnostics.add(
            CsvImportDiagnostic(
              rowNumber: row.rowNumber,
              reason: CsvImportDiagnosticReason.derivationMissingWeight,
              columnIndex: column.columnIndex,
            ),
          );
          continue;
        }
        final massKg = convertCsvValueToCanonical(raw, unit);
        canonical = massKg / rowWeightKg * 100;
    }

    if (canonical < spec.plausibleMin || canonical > spec.plausibleMax) {
      diagnostics.add(
        CsvImportDiagnostic(
          rowNumber: row.rowNumber,
          reason: CsvImportDiagnosticReason.outOfRange,
          columnIndex: column.columnIndex,
          detail: canonical.toStringAsFixed(2),
        ),
      );
      continue;
    }

    records.add(
      buildCsvImportRecord(
        metric: metric,
        value: canonical,
        instant: instant,
      ),
    );
  }

  return CsvRowConversion(records: records, diagnostics: diagnostics);
}

/// The row's body weight in kg, or null when the mapping has no weight column,
/// the cell is blank, or it does not parse.
double? _resolveRowWeightKg(CsvRow row, CsvImportMapping mapping) {
  final column = mapping.weightColumn;
  if (column == null) return null;
  final text = row.cell(column.columnIndex);
  if (text == null) return null;
  final raw = parseCsvNumber(text);
  if (raw == null) return null;
  final interpretation = column.effectiveInterpretation;
  if (interpretation is! CsvDirectValue) return null;
  return convertCsvValueToCanonical(raw, interpretation.unit);
}

/// Builds the record for [metric] at [instant] from an already-canonical [value].
ImportRecord buildCsvImportRecord({
  required CsvImportMetric metric,
  required double value,
  required CsvInstant instant,
}) {
  final targetType = kCsvMetricCatalog[metric]!.targetType;
  final clientRecordId = buildCsvClientRecordId(
    targetType: targetType,
    utc: instant.utc,
  );

  return switch (metric) {
    CsvImportMetric.weight => WeightImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        kilograms: value,
      ),
    CsvImportMetric.bodyFat => BodyFatImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        percent: value,
      ),
    CsvImportMetric.leanBodyMass => LeanBodyMassImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        kilograms: value,
      ),
    CsvImportMetric.boneMass => BoneMassImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        kilograms: value,
      ),
    CsvImportMetric.bodyWaterMass => BodyWaterMassImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        kilograms: value,
      ),
    CsvImportMetric.height => HeightImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        meters: value,
      ),
    CsvImportMetric.basalMetabolicRate => BasalMetabolicRateImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        kilocaloriesPerDay: value,
      ),
    // Health Connect models heart rate as a SERIES, not an instant. A CSV row is
    // one spot reading, so it becomes a one-sample series whose window is the
    // single instant — the same shape the Apple importer builds for a lone
    // HKQuantityTypeIdentifierHeartRate sample.
    CsvImportMetric.heartRate => HeartRateImportRecord(
        clientRecordId: clientRecordId,
        startTime: instant.utc,
        startZoneOffset: instant.offset,
        endTime: instant.utc,
        endZoneOffset: instant.offset,
        samples: [HeartRateSampleValue(instant.utc, value.round())],
      ),
    // Health Connect stores this one as an integer.
    CsvImportMetric.restingHeartRate => RestingHeartRateImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        beatsPerMinute: value.round(),
      ),
    CsvImportMetric.heartRateVariability =>
      HeartRateVariabilityRmssdImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        rmssdMillis: value,
      ),
    CsvImportMetric.oxygenSaturation => OxygenSaturationImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        percent: value,
      ),
    CsvImportMetric.respiratoryRate => RespiratoryRateImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        rate: value,
      ),
    CsvImportMetric.bodyTemperature => BodyTemperatureImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        celsius: value,
      ),
    CsvImportMetric.basalBodyTemperature => BasalBodyTemperatureImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        celsius: value,
      ),
    CsvImportMetric.bloodGlucose => BloodGlucoseImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        millimolesPerLiter: value,
      ),
    CsvImportMetric.vo2Max => Vo2MaxImportRecord(
        clientRecordId: clientRecordId,
        time: instant.utc,
        zoneOffset: instant.offset,
        vo2MillilitersPerMinuteKilogram: value,
      ),
  };
}

/// The identity of a CSV-imported record: **record type and instant, not value.**
///
/// Health Connect upserts on `clientRecordId`, so leaving the value out is what
/// makes re-importing a corrected file REPLACE the old record instead of leaving
/// two weights at the same instant. The cost is that two genuinely different
/// measurements at the identical instant collapse to one — for a scale, the right
/// trade.
///
/// Also deliberately excluded: the file name, the column header, the unit chosen
/// and the mapping. Re-exporting the same history with the columns reordered, or
/// in pounds instead of kilograms, resolves to the same records.
String buildCsvClientRecordId({
  required String targetType,
  required DateTime utc,
}) =>
    buildImportClientRecordId(
      kCsvClientRecordIdNamespace,
      targetType,
      [targetType, utc.toUtc().millisecondsSinceEpoch].join('|'),
    );

/// The canonical values [metric] would take across [rows].
///
/// Runs the REAL conversion, derivations and plausibility rejections included,
/// so the range the confirm step shows is the range that will actually be
/// written — which is the whole point of showing it. A fat-mass column divided
/// by the wrong weight column surfaces here as 3% or 150%.
List<double> previewCanonicalValues({
  required List<List<String>> rows,
  required CsvImportMapping mapping,
  required CsvImportMetric metric,
}) {
  final targetType = kCsvMetricCatalog[metric]?.targetType;
  if (targetType == null) return const [];

  final values = <double>[];
  for (var index = 0; index < rows.length; index++) {
    final conversion = convertCsvRow(
      // +2: 1-based, and past the header row.
      row: CsvRow(rowNumber: index + 2, fields: rows[index]),
      mapping: mapping,
    );
    for (final record in conversion.records) {
      if (record.targetType != targetType) continue;
      final value = canonicalValueOf(record);
      if (value != null) values.add(value);
    }
  }
  return values;
}

/// The earliest and latest wall-clock times [rows] resolve to under [mapping],
/// or null when none of them parse.
///
/// Wall clock, not the UTC instant: this is shown to the user to check against
/// the dates they can see in their own file, so it has to read the way the file
/// reads. The offset is added back for exactly that reason.
///
/// Resolved through [resolveCsvInstant], the same path the import takes, so the
/// span cannot disagree with what gets written. It is the guard the single-row
/// echo cannot be: a day/month mix-up that happens to leave row 1 plausible
/// shows up here as a span running to the wrong month, or backwards.
({DateTime first, DateTime last})? previewInstantRange({
  required List<List<String>> rows,
  required CsvImportMapping mapping,
}) {
  final column = mapping.timestampColumn;
  if (column == null) return null;

  DateTime? first;
  DateTime? last;
  for (final fields in rows) {
    if (column.columnIndex >= fields.length) continue;
    final text = fields[column.columnIndex].trim();
    if (text.isEmpty) continue;
    final instant = resolveCsvInstant(text, mapping.dateTime);
    if (instant == null) continue;
    final wallClock = instant.utc.add(instant.offset);
    if (first == null || wallClock.isBefore(first)) first = wallClock;
    if (last == null || wallClock.isAfter(last)) last = wallClock;
  }

  if (first == null || last == null) return null;
  return (first: first, last: last);
}

/// The single number a body record carries, in its canonical unit.
double? canonicalValueOf(ImportRecord record) => switch (record) {
      WeightImportRecord(:final kilograms) => kilograms,
      BodyFatImportRecord(:final percent) => percent,
      LeanBodyMassImportRecord(:final kilograms) => kilograms,
      BoneMassImportRecord(:final kilograms) => kilograms,
      BodyWaterMassImportRecord(:final kilograms) => kilograms,
      HeightImportRecord(:final meters) => meters,
      BasalMetabolicRateImportRecord(:final kilocaloriesPerDay) =>
        kilocaloriesPerDay,
      // A CSV-built heart rate always has exactly one sample.
      HeartRateImportRecord(:final samples) when samples.isNotEmpty =>
        samples.first.beatsPerMinute.toDouble(),
      RestingHeartRateImportRecord(:final beatsPerMinute) =>
        beatsPerMinute.toDouble(),
      HeartRateVariabilityRmssdImportRecord(:final rmssdMillis) => rmssdMillis,
      OxygenSaturationImportRecord(:final percent) => percent,
      RespiratoryRateImportRecord(:final rate) => rate,
      BodyTemperatureImportRecord(:final celsius) => celsius,
      BasalBodyTemperatureImportRecord(:final celsius) => celsius,
      BloodGlucoseImportRecord(:final millimolesPerLiter) => millimolesPerLiter,
      Vo2MaxImportRecord(:final vo2MillilitersPerMinuteKilogram) =>
        vo2MillilitersPerMinuteKilogram,
      _ => null,
    };

/// Parses a numeric cell, tolerating a comma decimal separator and thousands
/// separators.
///
/// A semicolon-delimited European export writes `78,4`; reading that as 784 or
/// as null would both be wrong. Only unambiguous shapes are accepted — a value
/// containing both separators is read with the LAST one as the decimal point.
double? parseCsvNumber(String text) {
  var value = text.trim();
  if (value.isEmpty) return null;

  // Strip anything that is not part of a number (stray units, currency, spaces).
  value = value.replaceAll(RegExp(r'[^0-9,.\-+eE]'), '');
  if (value.isEmpty) return null;

  final lastComma = value.lastIndexOf(',');
  final lastDot = value.lastIndexOf('.');
  if (lastComma >= 0 && lastDot >= 0) {
    if (lastComma > lastDot) {
      // 1.234,5 — dot groups, comma decides.
      value = value.replaceAll('.', '').replaceFirst(',', '.');
    } else {
      // 1,234.5 — comma groups.
      value = value.replaceAll(',', '');
    }
  } else if (lastComma >= 0) {
    // Only commas. Treat a single one as the decimal point; several means
    // thousands grouping.
    value = value.indexOf(',') == lastComma
        ? value.replaceFirst(',', '.')
        : value.replaceAll(',', '');
  }

  return double.tryParse(value);
}
