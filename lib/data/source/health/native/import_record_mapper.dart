import 'package:health_connect_native/health_connect_native.dart';

import '../../../../domain/model/apple_health_import_records.dart';

/// Maps a domain [ImportRecord] to a typed [ImportRecordMsg] for the native
/// bulk-insert bridge (replaces the JSON import serializer). Enum values are
/// resolved to their Health Connect ints here so the native builder stays
/// domain-agnostic.

const Map<SleepStageType, int> _sleepStage = {
  SleepStageType.awake: 1,
  SleepStageType.sleeping: 2,
  SleepStageType.light: 4,
  SleepStageType.deep: 5,
  SleepStageType.rem: 6,
  SleepStageType.awakeInBed: 7,
};

const Map<OvulationResultType, int> _ovulationResult = {
  OvulationResultType.inconclusive: 0,
  OvulationResultType.positive: 1,
  OvulationResultType.high: 2,
  OvulationResultType.negative: 3,
};

const Map<ImportExerciseType, int> _exerciseType = {
  ImportExerciseType.running: 56,
  ImportExerciseType.biking: 8,
  ImportExerciseType.walking: 79,
  ImportExerciseType.hiking: 37,
  ImportExerciseType.wheelchair: 82,
  ImportExerciseType.rowing: 53,
  ImportExerciseType.paddling: 46,
  ImportExerciseType.skiing: 61,
  ImportExerciseType.snowboarding: 62,
  ImportExerciseType.snowshoeing: 63,
  ImportExerciseType.skating: 60,
  ImportExerciseType.sailing: 58,
  ImportExerciseType.surfing: 72,
  ImportExerciseType.swimmingOpenWater: 73,
  ImportExerciseType.golf: 32,
  ImportExerciseType.yoga: 83,
  ImportExerciseType.pilates: 48,
  ImportExerciseType.elliptical: 25,
  ImportExerciseType.strengthTraining: 70,
  ImportExerciseType.stairClimbing: 68,
  ImportExerciseType.otherWorkout: 0,
};

ImportRecordMsg importRecordMsg(ImportRecord record) {
  ImportRecordMsg interval(
    String type,
    DateTime start,
    Duration? sZone,
    DateTime end,
    Duration? eZone, {
    Map<String, double> doubles = const {},
    Map<String, int> ints = const {},
    String? name,
    String? notes,
    List<ImportSampleMsg> samples = const [],
    List<ImportSleepStageMsg> sleepStages = const [],
    List<ExerciseRoutePointMsg> routePoints = const [],
    List<ExerciseSegmentMsg> segments = const [],
    List<ExerciseLapMsg> laps = const [],
    String? plannedExerciseId,
  }) =>
      ImportRecordMsg(
        recordType: type,
        clientRecordId: record.clientRecordId,
        startEpochMs: start.millisecondsSinceEpoch,
        endEpochMs: end.millisecondsSinceEpoch,
        startZoneOffsetSeconds: sZone?.inSeconds,
        endZoneOffsetSeconds: eZone?.inSeconds,
        doubleFields: doubles,
        intFields: ints,
        name: name,
        samples: samples,
        sleepStages: sleepStages,
        routePoints: routePoints,
        notes: notes,
        segments: segments,
        laps: laps,
        plannedExerciseId: plannedExerciseId,
        plannedBlocks: const [],
      );

  ImportRecordMsg instant(
    String type,
    DateTime time,
    Duration? zone, {
    Map<String, double> doubles = const {},
    Map<String, int> ints = const {},
    String? name,
  }) =>
      ImportRecordMsg(
        recordType: type,
        clientRecordId: record.clientRecordId,
        startEpochMs: time.millisecondsSinceEpoch,
        endEpochMs: null,
        startZoneOffsetSeconds: zone?.inSeconds,
        endZoneOffsetSeconds: null,
        doubleFields: doubles,
        intFields: ints,
        name: name,
        samples: const [],
        sleepStages: const [],
        routePoints: const [],
        segments: const [],
        laps: const [],
        plannedBlocks: const [],
      );

  switch (record) {
    case StepsImportRecord r:
      return interval('Steps', r.startTime, r.startZoneOffset, r.endTime,
          r.endZoneOffset, doubles: {'count': r.count.toDouble()});
    case DistanceImportRecord r:
      return interval('Distance', r.startTime, r.startZoneOffset, r.endTime,
          r.endZoneOffset, doubles: {'distanceMeters': r.meters});
    case ActiveCaloriesBurnedImportRecord r:
      return interval('ActiveCaloriesBurned', r.startTime, r.startZoneOffset,
          r.endTime, r.endZoneOffset, doubles: {'energyKcal': r.kilocalories});
    case BasalMetabolicRateImportRecord r:
      return instant('BasalMetabolicRate', r.time, r.zoneOffset,
          doubles: {'kcalPerDay': r.kilocaloriesPerDay});
    case FloorsClimbedImportRecord r:
      return interval('FloorsClimbed', r.startTime, r.startZoneOffset, r.endTime,
          r.endZoneOffset, doubles: {'floors': r.floors});
    case ElevationGainedImportRecord r:
      return interval('ElevationGained', r.startTime, r.startZoneOffset,
          r.endTime, r.endZoneOffset, doubles: {'elevationMeters': r.meters});
    case WheelchairPushesImportRecord r:
      return interval('WheelchairPushes', r.startTime, r.startZoneOffset,
          r.endTime, r.endZoneOffset, doubles: {'count': r.count.toDouble()});
    case SpeedImportRecord r:
      return interval('Speed', r.startTime, r.startZoneOffset, r.endTime,
          r.endZoneOffset,
          samples: [
            for (final s in r.samples)
              ImportSampleMsg(
                  timeEpochMs: s.time.millisecondsSinceEpoch,
                  value: s.metersPerSecond),
          ]);
    case HeartRateImportRecord r:
      return interval('HeartRate', r.startTime, r.startZoneOffset, r.endTime,
          r.endZoneOffset,
          samples: [
            for (final s in r.samples)
              ImportSampleMsg(
                  timeEpochMs: s.time.millisecondsSinceEpoch,
                  value: s.beatsPerMinute.toDouble()),
          ]);
    case RestingHeartRateImportRecord r:
      return instant('RestingHeartRate', r.time, r.zoneOffset,
          doubles: {'bpm': r.beatsPerMinute.toDouble()});
    case HeartRateVariabilityRmssdImportRecord r:
      return instant('HeartRateVariabilityRmssd', r.time, r.zoneOffset,
          doubles: {'rmssdMillis': r.rmssdMillis});
    case WeightImportRecord r:
      return instant('Weight', r.time, r.zoneOffset,
          doubles: {'weightKg': r.kilograms});
    case HeightImportRecord r:
      return instant('Height', r.time, r.zoneOffset,
          doubles: {'heightMeters': r.meters});
    case BodyFatImportRecord r:
      return instant('BodyFat', r.time, r.zoneOffset,
          doubles: {'percentage': r.percent});
    case LeanBodyMassImportRecord r:
      return instant('LeanBodyMass', r.time, r.zoneOffset,
          doubles: {'massKg': r.kilograms});
    case BoneMassImportRecord r:
      return instant('BoneMass', r.time, r.zoneOffset,
          doubles: {'massKg': r.kilograms});
    case BodyWaterMassImportRecord r:
      return instant('BodyWaterMass', r.time, r.zoneOffset,
          doubles: {'massKg': r.kilograms});
    case HydrationImportRecord r:
      return interval('Hydration', r.startTime, r.startZoneOffset, r.endTime,
          r.endZoneOffset, doubles: {'volumeLiters': r.milliliters / 1000.0});
    case OxygenSaturationImportRecord r:
      return instant('OxygenSaturation', r.time, r.zoneOffset,
          doubles: {'percentage': r.percent});
    case RespiratoryRateImportRecord r:
      return instant('RespiratoryRate', r.time, r.zoneOffset,
          doubles: {'rate': r.rate});
    case BodyTemperatureImportRecord r:
      return instant('BodyTemperature', r.time, r.zoneOffset,
          doubles: {'temperatureCelsius': r.celsius});
    case BasalBodyTemperatureImportRecord r:
      return instant('BasalBodyTemperature', r.time, r.zoneOffset,
          doubles: {'temperatureCelsius': r.celsius});
    case BloodGlucoseImportRecord r:
      return instant('BloodGlucose', r.time, r.zoneOffset,
          doubles: {'levelMmolL': r.millimolesPerLiter});
    case Vo2MaxImportRecord r:
      return instant('Vo2Max', r.time, r.zoneOffset, doubles: {
        'vo2MillilitersPerMinuteKilogram': r.vo2MillilitersPerMinuteKilogram
      });
    case BloodPressureImportRecord r:
      return instant('BloodPressure', r.time, r.zoneOffset, doubles: {
        'systolicMmHg': r.systolicMmHg,
        'diastolicMmHg': r.diastolicMmHg,
      });
    case SleepSessionImportRecord r:
      return interval('Sleep', r.startTime, r.startZoneOffset, r.endTime,
          r.endZoneOffset,
          name: r.title,
          sleepStages: [
            for (final s in r.stages)
              ImportSleepStageMsg(
                startEpochMs: s.startTime.millisecondsSinceEpoch,
                endEpochMs: s.endTime.millisecondsSinceEpoch,
                stage: _sleepStage[s.stage] ?? 0,
              ),
          ]);
    case NutritionImportRecord r:
      return interval('Nutrition', r.startTime, r.startZoneOffset, r.endTime,
          r.endZoneOffset,
          name: r.name,
          doubles: {
            if (r.energyKilocalories != null) 'energyKcal': r.energyKilocalories!,
            ...r.nutrientGrams,
          });
    case ExerciseSessionImportRecord r:
      return interval('ExerciseSession', r.startTime, r.startZoneOffset,
          r.endTime, r.endZoneOffset,
          name: r.title,
          ints: {'exerciseType': _exerciseType[r.exerciseType] ?? 0},
          routePoints: [
            for (final p in r.route?.route ?? const <ExerciseRouteLocation>[])
              ExerciseRoutePointMsg(
                timeEpochMs: p.time.millisecondsSinceEpoch,
                latitude: p.latitude,
                longitude: p.longitude,
                altitudeMeters: p.altitudeMeters,
                horizontalAccuracyMeters: p.horizontalAccuracyMeters,
                verticalAccuracyMeters: p.verticalAccuracyMeters,
              ),
          ]);
    case MindfulnessSessionImportRecord r:
      return interval('MindfulnessSession', r.startTime, r.startZoneOffset,
          r.endTime, r.endZoneOffset,
          name: r.title);
    case MenstruationFlowImportRecord r:
      return instant('MenstruationFlow', r.time, r.zoneOffset,
          ints: {'flow': r.flow.index});
    case OvulationTestImportRecord r:
      return instant('OvulationTest', r.time, r.zoneOffset,
          ints: {'result': _ovulationResult[r.result] ?? 0});
    case CervicalMucusImportRecord r:
      return instant('CervicalMucus', r.time, r.zoneOffset, ints: {
        'appearance': r.appearance.index,
        'sensation': r.sensation.index,
      });
    case IntermenstrualBleedingImportRecord r:
      return instant('IntermenstrualBleeding', r.time, r.zoneOffset);
    case SexualActivityImportRecord r:
      return instant('SexualActivity', r.time, r.zoneOffset,
          ints: {'protectionUsed': r.protectionUsed.index});
  }
}
