part of 'apple_health_import_converter.dart';

/// Single-record dispatch, ported from the Kotlin
/// `AppleHealthImportSingleRecordConversions.kt`.
extension AppleHealthImportSingleRecordConversions on AppleHealthImportConverter {
  ConvertedAppleRecord? convertSingleRecord(AppleRecord record) {
    final start = record.startDate;
    if (start == null) {
      return invalidRecord(record, 'Record is missing startDate.');
    }
    final end = record.endDate ?? start;
    final iv = interval(start, end);
    final value = record.numericValue;

    ConvertedAppleRecord built(
      String targetType,
      String fingerprint,
      ImportRecord importRecord,
    ) {
      markConverted(record.type);
      return ConvertedAppleRecord(
        appleType: record.type,
        targetType: targetType,
        fingerprint: fingerprint,
        record: importRecord,
        sourceTimeRange: AppleImportTimeRange(iv.start.instant, iv.end.instant),
        unit: record.unit,
        value: record.valueForReport,
      );
    }

    switch (record.type) {
      case appleStepCount:
        final count = value?.round();
        if (count == null || count <= 0) {
          return invalidRecord(record, 'Step count is missing or not positive.');
        }
        final fingerprint = record.stableClientRecordId('steps');
        return built(
          'StepsRecord',
          fingerprint,
          StepsImportRecord(
            clientRecordId: _metadataClientRecordId(record, 'StepsRecord'),
            startTime: iv.start.instant,
            startZoneOffset: iv.start.offset,
            endTime: iv.end.instant,
            endZoneOffset: iv.end.offset,
            count: count,
          ),
        );

      case appleDistanceWalkingRunning:
      case appleDistanceCycling:
      case appleDistanceSwimming:
      case appleDistanceWheelchair:
        final meters = value == null ? null : toMeters(value, record.unit);
        if (meters == null || meters <= 0.0) {
          return invalidRecord(
            record,
            'Distance is missing, unsupported unit, or not positive.',
          );
        }
        final fingerprint = record.stableClientRecordId('distance');
        return built(
          'DistanceRecord',
          fingerprint,
          DistanceImportRecord(
            clientRecordId: _metadataClientRecordId(record, 'DistanceRecord'),
            startTime: iv.start.instant,
            startZoneOffset: iv.start.offset,
            endTime: iv.end.instant,
            endZoneOffset: iv.end.offset,
            meters: meters,
          ),
        );

      case appleActiveEnergyBurned:
        final kcal = value == null ? null : toKilocalories(value, record.unit);
        if (kcal == null || kcal <= 0.0) {
          return invalidRecord(
            record,
            'Active energy is missing, unsupported unit, or not positive.',
          );
        }
        final fingerprint = record.stableClientRecordId('active_calories');
        return built(
          'ActiveCaloriesBurnedRecord',
          fingerprint,
          ActiveCaloriesBurnedImportRecord(
            clientRecordId:
                _metadataClientRecordId(record, 'ActiveCaloriesBurnedRecord'),
            startTime: iv.start.instant,
            startZoneOffset: iv.start.offset,
            endTime: iv.end.instant,
            endZoneOffset: iv.end.offset,
            kilocalories: kcal,
          ),
        );

      case appleWalkingSpeed:
        final mps =
            value == null ? null : toMetersPerSecond(value, record.unit);
        if (mps == null || mps < 0.0) {
          return invalidRecord(
            record,
            'Walking speed is missing, unsupported unit, or negative.',
          );
        }
        final fingerprint = record.stableClientRecordId('walking_speed');
        return built(
          'SpeedRecord',
          fingerprint,
          SpeedImportRecord(
            clientRecordId: _metadataClientRecordId(record, 'SpeedRecord'),
            startTime: iv.start.instant,
            startZoneOffset: iv.start.offset,
            endTime: iv.end.instant,
            endZoneOffset: iv.end.offset,
            samples: [SpeedSampleValue(iv.start.instant, mps)],
          ),
        );

      case appleBasalEnergyBurned:
        final kcal = value == null ? null : toKilocalories(value, record.unit);
        if (kcal == null || kcal <= 0.0) {
          return invalidRecord(
            record,
            'Basal energy is missing, unsupported unit, or not positive.',
          );
        }
        final durationSeconds =
            iv.end.instant.difference(iv.start.instant).inSeconds;
        if (durationSeconds <= 0) {
          return invalidRecord(
            record,
            'Basal energy record has no positive duration.',
          );
        }
        final kcalPerDay = kcal * 86400.0 / durationSeconds;
        final fingerprint = record.stableClientRecordId('bmr');
        return built(
          'BasalMetabolicRateRecord',
          fingerprint,
          BasalMetabolicRateImportRecord(
            clientRecordId:
                _metadataClientRecordId(record, 'BasalMetabolicRateRecord'),
            time: iv.start.instant,
            zoneOffset: iv.start.offset,
            kilocaloriesPerDay: kcalPerDay,
          ),
        );

      case appleFlightsClimbed:
        final floors = value;
        if (floors == null || floors <= 0.0) {
          return invalidRecord(
            record,
            'Flights climbed is missing or not positive.',
          );
        }
        final fingerprint = record.stableClientRecordId('floors');
        return built(
          'FloorsClimbedRecord',
          fingerprint,
          FloorsClimbedImportRecord(
            clientRecordId:
                _metadataClientRecordId(record, 'FloorsClimbedRecord'),
            startTime: iv.start.instant,
            startZoneOffset: iv.start.offset,
            endTime: iv.end.instant,
            endZoneOffset: iv.end.offset,
            floors: floors,
          ),
        );

      case appleElevationAscended:
        final meters = value == null ? null : toMeters(value, record.unit);
        if (meters == null || meters <= 0.0) {
          return invalidRecord(
            record,
            'Elevation is missing, unsupported unit, or not positive.',
          );
        }
        final fingerprint = record.stableClientRecordId('elevation');
        return built(
          'ElevationGainedRecord',
          fingerprint,
          ElevationGainedImportRecord(
            clientRecordId:
                _metadataClientRecordId(record, 'ElevationGainedRecord'),
            startTime: iv.start.instant,
            startZoneOffset: iv.start.offset,
            endTime: iv.end.instant,
            endZoneOffset: iv.end.offset,
            meters: meters,
          ),
        );

      case applePushCount:
        final count = value?.round();
        if (count == null || count <= 0) {
          return invalidRecord(
            record,
            'Wheelchair pushes is missing or not positive.',
          );
        }
        final fingerprint = record.stableClientRecordId('wheelchair_pushes');
        return built(
          'WheelchairPushesRecord',
          fingerprint,
          WheelchairPushesImportRecord(
            clientRecordId:
                _metadataClientRecordId(record, 'WheelchairPushesRecord'),
            startTime: iv.start.instant,
            startZoneOffset: iv.start.offset,
            endTime: iv.end.instant,
            endZoneOffset: iv.end.offset,
            count: count,
          ),
        );

      case appleHeartRate:
        final bpm = value?.round();
        if (bpm == null || bpm < 1 || bpm > 300) {
          return invalidRecord(record, 'Heart rate is outside 1..300 bpm.');
        }
        final fingerprint = record.stableClientRecordId('heart_rate');
        return built(
          'HeartRateRecord',
          fingerprint,
          HeartRateImportRecord(
            clientRecordId: _metadataClientRecordId(record, 'HeartRateRecord'),
            startTime: iv.start.instant,
            startZoneOffset: iv.start.offset,
            endTime: iv.end.instant,
            endZoneOffset: iv.end.offset,
            samples: [HeartRateSampleValue(start.instant, bpm)],
          ),
        );

      case appleRestingHeartRate:
        final bpm = value?.round();
        if (bpm == null || bpm < 1 || bpm > 300) {
          return invalidRecord(
            record,
            'Resting heart rate is outside 1..300 bpm.',
          );
        }
        final fingerprint = record.stableClientRecordId('resting_hr');
        return built(
          'RestingHeartRateRecord',
          fingerprint,
          RestingHeartRateImportRecord(
            clientRecordId:
                _metadataClientRecordId(record, 'RestingHeartRateRecord'),
            time: start.instant,
            zoneOffset: start.offset,
            beatsPerMinute: bpm,
          ),
        );

      case appleBodyMass:
        return convertWeight(record, start);
      case appleHeight:
        return convertHeight(record, start);
      case appleBodyFatPercentage:
        return convertBodyFat(record, start);
      case appleLeanBodyMass:
        return convertLeanMass(record, start);
      case appleBoneMass:
        return convertBoneMass(record, start);
      case appleBodyWaterMass:
        return convertBodyWaterMass(record, start);
      case appleDietaryWater:
        return convertHydration(record, iv);
      case appleOxygenSaturation:
        return convertOxygenSaturation(record, start);
      case appleRespiratoryRate:
        return convertRespiratoryRate(record, start);
      case appleBodyTemperature:
        return convertBodyTemperature(record, start);
      case appleBloodGlucose:
        return convertBloodGlucose(record, start);
      case appleVo2Max:
        return convertVo2Max(record, start);
      case appleBasalBodyTemperature:
        return convertBasalBodyTemperature(record, start);
      case appleMenstrualFlow:
      case appleOvulationTest:
      case appleCervicalMucus:
      case appleIntermenstrualBleeding:
      case appleSexualActivity:
        return convertCycleCategory(record, start);
      case appleMindfulSession:
        return convertMindfulness(record, iv);
      case appleHeartRateVariabilitySdnn:
        return unsupportedNull(
          record,
          'Apple exports HRV as SDNN; Health Connect record in this SDK is '
              'RMSSD, so this is not imported.',
        );
      default:
        return unsupportedNull(
          record,
          'No direct Health Connect mapping is implemented for this Apple '
              'record type.',
        );
    }
  }
}
