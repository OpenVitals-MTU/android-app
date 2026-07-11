part of 'apple_health_import_converter.dart';

/// Blood-pressure correlation + standalone pairing, ported from the Kotlin
/// `AppleHealthImportBloodPressureConversions.kt`.
extension AppleHealthImportBloodPressureConversions on AppleHealthImportConverter {
  List<ConvertedAppleRecord> convertBloodPressureCorrelations(
    List<AppleCorrelation> correlations,
  ) {
    final result = <ConvertedAppleRecord>[];
    for (final correlation
        in correlations.where((it) => it.type == appleBloodPressureCorrelation)) {
      final systolic = correlation.records
          .where((it) => it.type == appleBloodPressureSystolic)
          .firstOrNull;
      final diastolic = correlation.records
          .where((it) => it.type == appleBloodPressureDiastolic)
          .firstOrNull;
      if (systolic == null || diastolic == null) {
        invalid(
          correlation.type,
          'Blood pressure correlation is missing systolic or diastolic child '
              'record.',
          correlation.timeRangeOrNull()?.toString(),
        );
        continue;
      }
      consumedRecordFingerprints.add(systolic.sourceFingerprint);
      consumedRecordFingerprints.add(diastolic.sourceFingerprint);
      final converted = _buildBloodPressureRecord(
        appleType: correlation.type,
        start: correlation.startDate ?? systolic.startDate ?? diastolic.startDate,
        sourceEnd: correlation.endDate ?? systolic.endDate ?? diastolic.endDate,
        sourceName:
            correlation.sourceName ?? systolic.sourceName ?? diastolic.sourceName,
        unit: systolic.unit ?? diastolic.unit,
        value: '${systolic.rawValue}/${diastolic.rawValue}',
        systolic: systolic.numericValue,
        diastolic: diastolic.numericValue,
        stableParts: [
          'bp_correlation',
          correlation.stableParts(),
          systolic.stableParts(),
          diastolic.stableParts(),
        ],
      );
      if (converted != null) result.add(converted);
    }
    return result;
  }

  List<ConvertedAppleRecord> convertStandaloneBloodPressure(
    List<AppleRecord> records,
  ) {
    final grouped = <String, List<AppleRecord>>{};
    for (final record in records.where((it) =>
        it.type == appleBloodPressureSystolic ||
        it.type == appleBloodPressureDiastolic)) {
      final key = [
        record.sourceName ?? '',
        record.creationDate?.instant.toIso8601String() ?? '',
        record.startDate?.instant.toIso8601String() ?? '',
        record.endDate?.instant.toIso8601String() ?? '',
      ].join('|');
      grouped.putIfAbsent(key, () => []).add(record);
    }

    final result = <ConvertedAppleRecord>[];
    for (final group in grouped.values) {
      final systolic =
          group.where((it) => it.type == appleBloodPressureSystolic).firstOrNull;
      final diastolic =
          group.where((it) => it.type == appleBloodPressureDiastolic).firstOrNull;
      if (systolic == null || diastolic == null) {
        for (final record in group) {
          if (!consumedRecordFingerprints.contains(record.sourceFingerprint)) {
            invalidRecord(
              record,
              'Standalone blood pressure value could not be paired with systolic '
                  'and diastolic values.',
            );
          }
        }
        continue;
      }
      if (consumedRecordFingerprints.contains(systolic.sourceFingerprint) ||
          consumedRecordFingerprints.contains(diastolic.sourceFingerprint)) {
        continue;
      }
      consumedRecordFingerprints.add(systolic.sourceFingerprint);
      consumedRecordFingerprints.add(diastolic.sourceFingerprint);
      final converted = _buildBloodPressureRecord(
        appleType: appleBloodPressureCorrelation,
        start: systolic.startDate ?? diastolic.startDate,
        sourceEnd: systolic.endDate ?? diastolic.endDate,
        sourceName: systolic.sourceName ?? diastolic.sourceName,
        unit: systolic.unit ?? diastolic.unit,
        value: '${systolic.rawValue}/${diastolic.rawValue}',
        systolic: systolic.numericValue,
        diastolic: diastolic.numericValue,
        stableParts: ['bp_pair', systolic.stableParts(), diastolic.stableParts()],
      );
      if (converted != null) result.add(converted);
    }
    return result;
  }

  ConvertedAppleRecord? _buildBloodPressureRecord({
    required String appleType,
    required AppleDateTime? start,
    required AppleDateTime? sourceEnd,
    required String? sourceName,
    required String? unit,
    required String? value,
    required double? systolic,
    required double? diastolic,
    required List<String> stableParts,
  }) {
    final time = start;
    if (time == null) {
      return invalid(
        appleType,
        'Blood pressure is missing measurement time.',
        null,
      );
    }
    if (systolic == null || systolic < 20.0 || systolic > 300.0) {
      return invalid(
        appleType,
        'Systolic value is missing or outside supported range.',
        time.instant.toIso8601String(),
      );
    }
    if (diastolic == null || diastolic < 10.0 || diastolic > 180.0) {
      return invalid(
        appleType,
        'Diastolic value is missing or outside supported range.',
        time.instant.toIso8601String(),
      );
    }
    final fingerprint = buildStableClientRecordId(
      'blood_pressure',
      [...stableParts, sourceName ?? ''],
    );
    markConverted(appleType);
    return ConvertedAppleRecord(
      appleType: appleType,
      targetType: 'BloodPressureRecord',
      fingerprint: fingerprint,
      record: BloodPressureImportRecord(
        clientRecordId: fingerprint,
        time: time.instant,
        zoneOffset: time.offset,
        systolicMmHg: systolic,
        diastolicMmHg: diastolic,
      ),
      sourceTimeRange:
          AppleImportTimeRange(time.instant, sourceEnd?.instant ?? time.instant),
      unit: unit,
      value: value,
    );
  }
}
