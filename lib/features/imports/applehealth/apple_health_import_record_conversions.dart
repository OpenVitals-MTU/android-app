part of 'apple_health_import_converter.dart';

/// Body / vitals / hydration / cycle / mindfulness single-record conversions,
/// ported from the Kotlin `AppleHealthImportRecordConversions.kt`.
extension AppleHealthImportRecordConversions on AppleHealthImportConverter {
  ConvertedAppleRecord _instantRecord(
    AppleRecord record,
    AppleDateTime start,
    String targetType,
    String fingerprintPrefix,
    ImportRecord Function(String clientRecordId) build, {
    String? valueOverride,
  }) {
    final fingerprint = record.stableClientRecordId(fingerprintPrefix);
    markConverted(record.type);
    return ConvertedAppleRecord(
      appleType: record.type,
      targetType: targetType,
      fingerprint: fingerprint,
      record: build(_metadataClientRecordId(record, targetType)),
      sourceTimeRange: AppleImportTimeRange(start.instant, start.instant),
      unit: record.unit,
      value: valueOverride ?? record.valueForReport,
    );
  }

  ConvertedAppleRecord? convertWeight(AppleRecord record, AppleDateTime start) {
    final value = record.numericValue;
    final kg = value == null ? null : toKilograms(value, record.unit);
    if (kg == null || kg <= 0.0) {
      return invalidRecord(
        record,
        'Weight is missing, unsupported unit, or not positive.',
      );
    }
    return _instantRecord(
      record,
      start,
      'WeightRecord',
      'weight',
      (clientRecordId) => WeightImportRecord(
        clientRecordId: clientRecordId,
        time: start.instant,
        zoneOffset: start.offset,
        kilograms: kg,
      ),
    );
  }

  ConvertedAppleRecord? convertHeight(AppleRecord record, AppleDateTime start) {
    final value = record.numericValue;
    final meters = value == null ? null : toMeters(value, record.unit);
    if (meters == null || meters <= 0.0) {
      return invalidRecord(
        record,
        'Height is missing, unsupported unit, or not positive.',
      );
    }
    return _instantRecord(
      record,
      start,
      'HeightRecord',
      'height',
      (clientRecordId) => HeightImportRecord(
        clientRecordId: clientRecordId,
        time: start.instant,
        zoneOffset: start.offset,
        meters: meters,
      ),
    );
  }

  ConvertedAppleRecord? convertBodyFat(AppleRecord record, AppleDateTime start) {
    final value = record.numericValue;
    final percent = value == null ? null : toPercentage(value, record.unit);
    if (percent == null || percent < 0.0 || percent > 100.0) {
      return invalidRecord(
        record,
        'Body fat is missing, unsupported unit, or outside 0..100%.',
      );
    }
    return _instantRecord(
      record,
      start,
      'BodyFatRecord',
      'body_fat',
      (clientRecordId) => BodyFatImportRecord(
        clientRecordId: clientRecordId,
        time: start.instant,
        zoneOffset: start.offset,
        percent: percent,
      ),
    );
  }

  ConvertedAppleRecord? convertLeanMass(AppleRecord record, AppleDateTime start) {
    final value = record.numericValue;
    final kg = value == null ? null : toKilograms(value, record.unit);
    if (kg == null || kg <= 0.0) {
      return invalidRecord(
        record,
        'Lean body mass is missing, unsupported unit, or not positive.',
      );
    }
    return _instantRecord(
      record,
      start,
      'LeanBodyMassRecord',
      'lean_mass',
      (clientRecordId) => LeanBodyMassImportRecord(
        clientRecordId: clientRecordId,
        time: start.instant,
        zoneOffset: start.offset,
        kilograms: kg,
      ),
    );
  }

  ConvertedAppleRecord? convertBoneMass(AppleRecord record, AppleDateTime start) {
    final value = record.numericValue;
    final kg = value == null ? null : toKilograms(value, record.unit);
    if (kg == null || kg <= 0.0) {
      return invalidRecord(
        record,
        'Bone mass is missing, unsupported unit, or not positive.',
      );
    }
    return _instantRecord(
      record,
      start,
      'BoneMassRecord',
      'bone_mass',
      (clientRecordId) => BoneMassImportRecord(
        clientRecordId: clientRecordId,
        time: start.instant,
        zoneOffset: start.offset,
        kilograms: kg,
      ),
    );
  }

  ConvertedAppleRecord? convertBodyWaterMass(
    AppleRecord record,
    AppleDateTime start,
  ) {
    final value = record.numericValue;
    final kg = value == null ? null : toKilograms(value, record.unit);
    if (kg == null || kg <= 0.0) {
      return invalidRecord(
        record,
        'Body water mass is missing, unsupported unit, or not positive.',
      );
    }
    return _instantRecord(
      record,
      start,
      'BodyWaterMassRecord',
      'body_water_mass',
      (clientRecordId) => BodyWaterMassImportRecord(
        clientRecordId: clientRecordId,
        time: start.instant,
        zoneOffset: start.offset,
        kilograms: kg,
      ),
    );
  }

  ConvertedAppleRecord? convertHydration(AppleRecord record, AppleInterval iv) {
    final value = record.numericValue;
    final milliliters = value == null ? null : toMilliliters(value, record.unit);
    if (milliliters == null || milliliters <= 0.0) {
      return invalidRecord(
        record,
        'Hydration is missing, unsupported unit, or not positive.',
      );
    }
    final fingerprint = record.stableClientRecordId('hydration');
    markConverted(record.type);
    return ConvertedAppleRecord(
      appleType: record.type,
      targetType: 'HydrationRecord',
      fingerprint: fingerprint,
      record: HydrationImportRecord(
        clientRecordId: _metadataClientRecordId(record, 'HydrationRecord'),
        startTime: iv.start.instant,
        startZoneOffset: iv.start.offset,
        endTime: iv.end.instant,
        endZoneOffset: iv.end.offset,
        milliliters: milliliters,
      ),
      sourceTimeRange: AppleImportTimeRange(iv.start.instant, iv.end.instant),
      unit: record.unit,
      value: record.valueForReport,
    );
  }

  ConvertedAppleRecord? convertOxygenSaturation(
    AppleRecord record,
    AppleDateTime start,
  ) {
    final value = record.numericValue;
    final percent = value == null ? null : toPercentage(value, record.unit);
    if (percent == null || percent < 0.0 || percent > 100.0) {
      return invalidRecord(
        record,
        'Oxygen saturation is missing, unsupported unit, or outside 0..100%.',
      );
    }
    return _instantRecord(
      record,
      start,
      'OxygenSaturationRecord',
      'spo2',
      (clientRecordId) => OxygenSaturationImportRecord(
        clientRecordId: clientRecordId,
        time: start.instant,
        zoneOffset: start.offset,
        percent: percent,
      ),
    );
  }

  ConvertedAppleRecord? convertRespiratoryRate(
    AppleRecord record,
    AppleDateTime start,
  ) {
    final rate = record.numericValue;
    if (rate == null || rate <= 0.0) {
      return invalidRecord(
        record,
        'Respiratory rate is missing or not positive.',
      );
    }
    return _instantRecord(
      record,
      start,
      'RespiratoryRateRecord',
      'respiratory_rate',
      (clientRecordId) => RespiratoryRateImportRecord(
        clientRecordId: clientRecordId,
        time: start.instant,
        zoneOffset: start.offset,
        rate: rate,
      ),
    );
  }

  ConvertedAppleRecord? convertBodyTemperature(
    AppleRecord record,
    AppleDateTime start,
  ) {
    final value = record.numericValue;
    final celsius = value == null ? null : toCelsius(value, record.unit);
    if (celsius == null) {
      return invalidRecord(
        record,
        'Body temperature is missing or has an unsupported unit.',
      );
    }
    return _instantRecord(
      record,
      start,
      'BodyTemperatureRecord',
      'body_temperature',
      (clientRecordId) => BodyTemperatureImportRecord(
        clientRecordId: clientRecordId,
        time: start.instant,
        zoneOffset: start.offset,
        celsius: celsius,
      ),
    );
  }

  ConvertedAppleRecord? convertBloodGlucose(
    AppleRecord record,
    AppleDateTime start,
  ) {
    final value = record.numericValue;
    final glucose =
        value == null ? null : toMillimolesPerLiter(value, record.unit);
    if (glucose == null) {
      return invalidRecord(
        record,
        'Blood glucose is missing or has an unsupported unit.',
      );
    }
    return _instantRecord(
      record,
      start,
      'BloodGlucoseRecord',
      'blood_glucose',
      (clientRecordId) => BloodGlucoseImportRecord(
        clientRecordId: clientRecordId,
        time: start.instant,
        zoneOffset: start.offset,
        millimolesPerLiter: glucose,
      ),
    );
  }

  ConvertedAppleRecord? convertVo2Max(AppleRecord record, AppleDateTime start) {
    final vo2 = record.numericValue;
    if (vo2 == null || vo2 < 1.0 || vo2 > 100.0) {
      return invalidRecord(
        record,
        'VO2 max is missing or outside 1..100 mL/kg/min.',
      );
    }
    return _instantRecord(
      record,
      start,
      'Vo2MaxRecord',
      'vo2_max',
      (clientRecordId) => Vo2MaxImportRecord(
        clientRecordId: clientRecordId,
        time: start.instant,
        zoneOffset: start.offset,
        vo2MillilitersPerMinuteKilogram: vo2,
      ),
    );
  }

  ConvertedAppleRecord? convertBasalBodyTemperature(
    AppleRecord record,
    AppleDateTime start,
  ) {
    final value = record.numericValue;
    final celsius = value == null ? null : toCelsius(value, record.unit);
    if (celsius == null) {
      return invalidRecord(
        record,
        'Basal body temperature is missing or has an unsupported unit.',
      );
    }
    return _instantRecord(
      record,
      start,
      'BasalBodyTemperatureRecord',
      'basal_body_temperature',
      (clientRecordId) => BasalBodyTemperatureImportRecord(
        clientRecordId: clientRecordId,
        time: start.instant,
        zoneOffset: start.offset,
        celsius: celsius,
      ),
    );
  }

  ConvertedAppleRecord? convertMindfulness(
    AppleRecord record,
    AppleInterval iv,
  ) {
    if (!mindfulnessAvailable) {
      return skippedNull(
        record,
        'feature_unavailable',
        'Mindfulness sessions are not available in this Health Connect provider.',
      );
    }
    final fingerprint = record.stableClientRecordId('mindfulness');
    markConverted(record.type);
    return ConvertedAppleRecord(
      appleType: record.type,
      targetType: 'MindfulnessSessionRecord',
      fingerprint: fingerprint,
      record: MindfulnessSessionImportRecord(
        clientRecordId:
            _metadataClientRecordId(record, 'MindfulnessSessionRecord'),
        startTime: iv.start.instant,
        startZoneOffset: iv.start.offset,
        endTime: iv.end.instant,
        endZoneOffset: iv.end.offset,
        title: 'Apple Health mindfulness',
      ),
      sourceTimeRange: AppleImportTimeRange(iv.start.instant, iv.end.instant),
      unit: record.unit,
      value: record.valueForReport,
    );
  }

  ConvertedAppleRecord? convertCycleCategory(
    AppleRecord record,
    AppleDateTime start,
  ) {
    final fingerprint = record.stableClientRecordId('cycle');
    final clientRecordId =
        _metadataClientRecordId(record, _substringAfterLast(record.type, 'Identifier'));
    final rawValue = record.rawValue ?? '';
    final ImportRecord importRecord;
    switch (record.type) {
      case appleMenstrualFlow:
        importRecord = MenstruationFlowImportRecord(
          clientRecordId: clientRecordId,
          time: start.instant,
          zoneOffset: start.offset,
          flow: mapMenstrualFlow(rawValue),
        );
      case appleOvulationTest:
        importRecord = OvulationTestImportRecord(
          clientRecordId: clientRecordId,
          time: start.instant,
          zoneOffset: start.offset,
          result: mapOvulationResult(rawValue),
        );
      case appleCervicalMucus:
        importRecord = CervicalMucusImportRecord(
          clientRecordId: clientRecordId,
          time: start.instant,
          zoneOffset: start.offset,
          appearance: mapCervicalMucusAppearance(rawValue),
          sensation: CervicalMucusSensation.unknown,
        );
      case appleIntermenstrualBleeding:
        importRecord = IntermenstrualBleedingImportRecord(
          clientRecordId: clientRecordId,
          time: start.instant,
          zoneOffset: start.offset,
        );
      case appleSexualActivity:
        importRecord = SexualActivityImportRecord(
          clientRecordId: clientRecordId,
          time: start.instant,
          zoneOffset: start.offset,
          protectionUsed: mapProtectionUsed(record.metadata),
        );
      default:
        return unsupportedNull(
          record,
          'No direct cycle mapping is implemented for this Apple record type.',
        );
    }
    markConverted(record.type);
    return ConvertedAppleRecord(
      appleType: record.type,
      targetType: importRecord.targetType,
      fingerprint: fingerprint,
      record: importRecord,
      sourceTimeRange: AppleImportTimeRange(start.instant, start.instant),
      unit: record.unit,
      value: record.valueForReport,
    );
  }
}
