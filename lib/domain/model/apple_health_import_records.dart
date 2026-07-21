/// The importer's intermediate "record" model — the Dart analogue of the Kotlin
/// converter's Health Connect `androidx.health.connect.client.records.Record`
/// output.
///
/// The Kotlin importer converts each Apple element into a strongly-typed Health
/// Connect record and then bulk-inserts it. The Dart `health` package has no
/// equivalent constructible record type, so the converter emits these pure-Dart
/// [ImportRecord]s instead. The write path ([HealthDataSource.insertImportedRecords])
/// maps them onto the `health` package as best it can (see TODO(health-pkg)).
///
/// Every record carries a deterministic [clientRecordId] (prefixed
/// `apple_health_`) used for cross-import de-duplication, exactly as the Kotlin
/// `Metadata.clientRecordId`.
library;

/// Sleep stage classification, mirroring `SleepSessionRecord.STAGE_TYPE_*`.
enum SleepStageType { awakeInBed, sleeping, light, deep, rem, awake }

/// Menstruation flow, mirroring `MenstruationFlowRecord.FLOW_*`.
enum MenstruationFlowType { unknown, light, medium, heavy }

/// Ovulation test result, mirroring `OvulationTestRecord.RESULT_*`.
enum OvulationResultType { inconclusive, positive, negative, high }

/// Cervical mucus appearance, mirroring `CervicalMucusRecord.APPEARANCE_*`.
enum CervicalMucusAppearance {
  unknown,
  dry,
  sticky,
  creamy,
  watery,
  eggWhite,
}

/// Cervical mucus sensation, mirroring `CervicalMucusRecord.SENSATION_*`.
enum CervicalMucusSensation { unknown, light, medium, heavy }

/// Sexual-activity protection, mirroring `SexualActivityRecord.PROTECTION_USED_*`.
enum SexualActivityProtection { unknown, protected, unprotected }

/// Exercise type, mirroring the `ExerciseSessionRecord.EXERCISE_TYPE_*`
/// subset that [appleWorkoutActivityTypeToExerciseType] can produce.
enum ImportExerciseType {
  running,
  biking,
  walking,
  hiking,
  wheelchair,
  rowing,
  paddling,
  skiing,
  snowboarding,
  snowshoeing,
  skating,
  sailing,
  surfing,
  swimmingOpenWater,
  golf,
  yoga,
  pilates,
  elliptical,
  strengthTraining,
  stairClimbing,
  otherWorkout,
}

/// A single [ImportRecord]'s time and its wall-clock zone offset (Kotlin
/// `Instant` + `ZoneOffset`).
class ImportInstant {
  const ImportInstant(this.time, this.zoneOffset);

  /// UTC instant.
  final DateTime time;

  /// Wall-clock offset from UTC, or `null` if the export had none.
  final Duration? zoneOffset;
}

/// Base class for every converted record the importer can produce.
sealed class ImportRecord {
  const ImportRecord({required this.clientRecordId, required this.targetType});

  /// Deterministic dedupe id, prefixed `apple_health_` (Kotlin
  /// `Metadata.clientRecordId`).
  final String clientRecordId;

  /// The Health Connect record class name this maps to (e.g. `StepsRecord`),
  /// used for reporting and duplicate-check grouping (Kotlin `recordType`).
  final String targetType;
}

class StepsImportRecord extends ImportRecord {
  const StepsImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.count,
  }) : super(targetType: 'StepsRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final int count;
}

class DistanceImportRecord extends ImportRecord {
  const DistanceImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.meters,
  }) : super(targetType: 'DistanceRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final double meters;
}

class ActiveCaloriesBurnedImportRecord extends ImportRecord {
  const ActiveCaloriesBurnedImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.kilocalories,
  }) : super(targetType: 'ActiveCaloriesBurnedRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final double kilocalories;
}

class BasalMetabolicRateImportRecord extends ImportRecord {
  const BasalMetabolicRateImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.kilocaloriesPerDay,
  }) : super(targetType: 'BasalMetabolicRateRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double kilocaloriesPerDay;
}

class FloorsClimbedImportRecord extends ImportRecord {
  const FloorsClimbedImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.floors,
  }) : super(targetType: 'FloorsClimbedRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final double floors;
}

class ElevationGainedImportRecord extends ImportRecord {
  const ElevationGainedImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.meters,
  }) : super(targetType: 'ElevationGainedRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final double meters;
}

class WheelchairPushesImportRecord extends ImportRecord {
  const WheelchairPushesImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.count,
  }) : super(targetType: 'WheelchairPushesRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final int count;
}

class SpeedSampleValue {
  const SpeedSampleValue(this.time, this.metersPerSecond);

  final DateTime time;
  final double metersPerSecond;
}

class SpeedImportRecord extends ImportRecord {
  const SpeedImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.samples,
  }) : super(targetType: 'SpeedRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final List<SpeedSampleValue> samples;
}

class HeartRateSampleValue {
  const HeartRateSampleValue(this.time, this.beatsPerMinute);

  final DateTime time;
  final int beatsPerMinute;
}

class HeartRateImportRecord extends ImportRecord {
  const HeartRateImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.samples,
  }) : super(targetType: 'HeartRateRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final List<HeartRateSampleValue> samples;
}

class RestingHeartRateImportRecord extends ImportRecord {
  const RestingHeartRateImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.beatsPerMinute,
  }) : super(targetType: 'RestingHeartRateRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final int beatsPerMinute;
}

/// A single-instant heart-rate-variability RMSSD reading, in milliseconds
/// (`HeartRateVariabilityRmssdRecord.heartRateVariabilityMillis`).
class HeartRateVariabilityRmssdImportRecord extends ImportRecord {
  const HeartRateVariabilityRmssdImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.rmssdMillis,
  }) : super(targetType: 'HeartRateVariabilityRmssdRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double rmssdMillis;
}

/// A single-instant mass measurement in kilograms.
class WeightImportRecord extends ImportRecord {
  const WeightImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.kilograms,
  }) : super(targetType: 'WeightRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double kilograms;
}

class HeightImportRecord extends ImportRecord {
  const HeightImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.meters,
  }) : super(targetType: 'HeightRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double meters;
}

class BodyFatImportRecord extends ImportRecord {
  const BodyFatImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.percent,
  }) : super(targetType: 'BodyFatRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double percent;
}

class LeanBodyMassImportRecord extends ImportRecord {
  const LeanBodyMassImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.kilograms,
  }) : super(targetType: 'LeanBodyMassRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double kilograms;
}

class BoneMassImportRecord extends ImportRecord {
  const BoneMassImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.kilograms,
  }) : super(targetType: 'BoneMassRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double kilograms;
}

class BodyWaterMassImportRecord extends ImportRecord {
  const BodyWaterMassImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.kilograms,
  }) : super(targetType: 'BodyWaterMassRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double kilograms;
}

class HydrationImportRecord extends ImportRecord {
  const HydrationImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.milliliters,
  }) : super(targetType: 'HydrationRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final double milliliters;
}

class OxygenSaturationImportRecord extends ImportRecord {
  const OxygenSaturationImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.percent,
  }) : super(targetType: 'OxygenSaturationRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double percent;
}

class RespiratoryRateImportRecord extends ImportRecord {
  const RespiratoryRateImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.rate,
  }) : super(targetType: 'RespiratoryRateRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double rate;
}

class BodyTemperatureImportRecord extends ImportRecord {
  const BodyTemperatureImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.celsius,
  }) : super(targetType: 'BodyTemperatureRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double celsius;
}

class BloodGlucoseImportRecord extends ImportRecord {
  const BloodGlucoseImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.millimolesPerLiter,
  }) : super(targetType: 'BloodGlucoseRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double millimolesPerLiter;

  double get milligramsPerDeciliter => millimolesPerLiter * 18.0;
}

class Vo2MaxImportRecord extends ImportRecord {
  const Vo2MaxImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.vo2MillilitersPerMinuteKilogram,
  }) : super(targetType: 'Vo2MaxRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double vo2MillilitersPerMinuteKilogram;
}

class BasalBodyTemperatureImportRecord extends ImportRecord {
  const BasalBodyTemperatureImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.celsius,
  }) : super(targetType: 'BasalBodyTemperatureRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double celsius;
}

class MindfulnessSessionImportRecord extends ImportRecord {
  const MindfulnessSessionImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.title,
  }) : super(targetType: 'MindfulnessSessionRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final String title;
}

class MenstruationFlowImportRecord extends ImportRecord {
  const MenstruationFlowImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.flow,
  }) : super(targetType: 'MenstruationFlowRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final MenstruationFlowType flow;
}

class OvulationTestImportRecord extends ImportRecord {
  const OvulationTestImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.result,
  }) : super(targetType: 'OvulationTestRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final OvulationResultType result;
}

class CervicalMucusImportRecord extends ImportRecord {
  const CervicalMucusImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.appearance,
    required this.sensation,
  }) : super(targetType: 'CervicalMucusRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final CervicalMucusAppearance appearance;
  final CervicalMucusSensation sensation;
}

class IntermenstrualBleedingImportRecord extends ImportRecord {
  const IntermenstrualBleedingImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
  }) : super(targetType: 'IntermenstrualBleedingRecord');

  final DateTime time;
  final Duration? zoneOffset;
}

class SexualActivityImportRecord extends ImportRecord {
  const SexualActivityImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.protectionUsed,
  }) : super(targetType: 'SexualActivityRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final SexualActivityProtection protectionUsed;
}

class BloodPressureImportRecord extends ImportRecord {
  const BloodPressureImportRecord({
    required super.clientRecordId,
    required this.time,
    required this.zoneOffset,
    required this.systolicMmHg,
    required this.diastolicMmHg,
  }) : super(targetType: 'BloodPressureRecord');

  final DateTime time;
  final Duration? zoneOffset;
  final double systolicMmHg;
  final double diastolicMmHg;
}

class SleepStageValue {
  const SleepStageValue({
    required this.startTime,
    required this.endTime,
    required this.stage,
  });

  final DateTime startTime;
  final DateTime endTime;
  final SleepStageType stage;
}

class SleepSessionImportRecord extends ImportRecord {
  const SleepSessionImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.title,
    required this.stages,
  }) : super(targetType: 'SleepSessionRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final String title;
  final List<SleepStageValue> stages;
}

class NutritionImportRecord extends ImportRecord {
  const NutritionImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.name,
    required this.nutrientGrams,
    required this.energyKilocalories,
  }) : super(targetType: 'NutritionRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final String? name;

  /// Nutrient masses in grams, keyed by nutrient name (e.g. `protein`).
  final Map<String, double> nutrientGrams;
  final double? energyKilocalories;
}

/// A single GPS location on an exercise route (Kotlin `ExerciseRoute.Location`).
class ExerciseRouteLocation {
  const ExerciseRouteLocation({
    required this.time,
    required this.latitude,
    required this.longitude,
    this.altitudeMeters,
    this.horizontalAccuracyMeters,
    this.verticalAccuracyMeters,
  });

  final DateTime time;
  final double latitude;
  final double longitude;
  final double? altitudeMeters;
  final double? horizontalAccuracyMeters;
  final double? verticalAccuracyMeters;
}

class ExerciseRoute {
  const ExerciseRoute(this.route);

  final List<ExerciseRouteLocation> route;
}

class ExerciseSessionImportRecord extends ImportRecord {
  const ExerciseSessionImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.exerciseType,
    required this.title,
    required this.route,
  }) : super(targetType: 'ExerciseSessionRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final ImportExerciseType exerciseType;
  final String title;

  /// The synthesized GPS route, or `null` when the workout has no usable route.
  final ExerciseRoute? route;

  bool get hasRoute => route != null;
}

/// A single-instant total energy expenditure over an interval (basal + active),
/// mirroring `TotalCaloriesBurnedRecord`.
class TotalCaloriesBurnedImportRecord extends ImportRecord {
  const TotalCaloriesBurnedImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.kilocalories,
  }) : super(targetType: 'TotalCaloriesBurnedRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final double kilocalories;
}

class PowerSampleValue {
  const PowerSampleValue(this.time, this.watts);
  final DateTime time;
  final double watts;
}

class PowerImportRecord extends ImportRecord {
  const PowerImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.samples,
  }) : super(targetType: 'PowerRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final List<PowerSampleValue> samples;
}

class StepsCadenceSampleValue {
  const StepsCadenceSampleValue(this.time, this.rate);
  final DateTime time;
  final double rate;
}

class StepsCadenceImportRecord extends ImportRecord {
  const StepsCadenceImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.samples,
  }) : super(targetType: 'StepsCadenceRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final List<StepsCadenceSampleValue> samples;
}

class CyclingPedalingCadenceSampleValue {
  const CyclingPedalingCadenceSampleValue(this.time, this.revolutionsPerMinute);
  final DateTime time;
  final double revolutionsPerMinute;
}

class CyclingPedalingCadenceImportRecord extends ImportRecord {
  const CyclingPedalingCadenceImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.samples,
  }) : super(targetType: 'CyclingPedalingCadenceRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final List<CyclingPedalingCadenceSampleValue> samples;
}

/// A single skin-temperature reading over an interval: a baseline plus a series
/// of deltas from it (`SkinTemperatureRecord`).
class SkinTemperatureDeltaValue {
  const SkinTemperatureDeltaValue(this.time, this.deltaCelsius);
  final DateTime time;
  final double deltaCelsius;
}

class SkinTemperatureImportRecord extends ImportRecord {
  const SkinTemperatureImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.baselineCelsius,
    required this.measurementLocation,
    required this.deltas,
  }) : super(targetType: 'SkinTemperatureRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final double? baselineCelsius;
  final int measurementLocation;
  final List<SkinTemperatureDeltaValue> deltas;
}

/// A menstrual period span (`MenstruationPeriodRecord`) — interval only, no value.
class MenstruationPeriodImportRecord extends ImportRecord {
  const MenstruationPeriodImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
  }) : super(targetType: 'MenstruationPeriodRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
}

/// One step within a planned-exercise block (`PlannedExerciseStep`).
class PlannedExerciseStepValue {
  const PlannedExerciseStepValue({
    required this.exerciseType,
    required this.exercisePhase,
    this.description,
    required this.completionKind,
    this.completionRepetitions,
    this.completionSeconds,
  });

  final int exerciseType;
  final int exercisePhase;
  final String? description;

  /// The step's completion goal kind, matching `PlannedExerciseCompletionKindMsg`
  /// ordinals: 0 repetitions, 1 durationSeconds, 2 manual, 3 unknown.
  final int completionKind;
  final int? completionRepetitions;
  final int? completionSeconds;
}

/// One block of a planned-exercise session (`PlannedExerciseBlock`).
class PlannedExerciseBlockValue {
  const PlannedExerciseBlockValue({
    required this.repetitions,
    this.description,
    required this.steps,
  });

  final int repetitions;
  final String? description;
  final List<PlannedExerciseStepValue> steps;
}

/// A planned (future) workout with its block/step structure
/// (`PlannedExerciseSessionRecord`). `exerciseType` is the raw Health Connect
/// exercise-type int (not the limited [ImportExerciseType]).
class PlannedExerciseSessionImportRecord extends ImportRecord {
  const PlannedExerciseSessionImportRecord({
    required super.clientRecordId,
    required this.startTime,
    required this.startZoneOffset,
    required this.endTime,
    required this.endZoneOffset,
    required this.exerciseType,
    required this.title,
    required this.notes,
    required this.blocks,
  }) : super(targetType: 'PlannedExerciseSessionRecord');

  final DateTime startTime;
  final Duration? startZoneOffset;
  final DateTime endTime;
  final Duration? endZoneOffset;
  final int exerciseType;
  final String? title;
  final String? notes;
  final List<PlannedExerciseBlockValue> blocks;
}
