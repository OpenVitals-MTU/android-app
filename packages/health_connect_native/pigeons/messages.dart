// Pigeon contract for the `health_connect_native` plugin.
//
// This file defines the Flutter <-> AndroidX Health Connect (Kotlin) bridge.
// It is the SINGLE SOURCE OF TRUTH for the generated message classes:
//
//   * Dart   -> lib/src/messages.g.dart
//   * Kotlin -> android/src/main/kotlin/tech/mmarca/openvitals/health_connect_native/Messages.g.kt
//
// Regenerate both after editing this file (run from the plugin directory):
//
//   dart run pigeon --input pigeons/messages.dart
//
// DESIGN NOTE
// -----------
// Health Connect records are numerous and deeply nested. To keep the Pigeon
// surface small and STABLE across the record-type explosion, records travel the
// bridge as JSON STRINGS (one JSON object per record), while OPERATIONS
// (permissions, feature checks, aggregation, delete/dedup) are strongly typed.
// The canonical record JSON schema is documented in
// `lib/health_connect_native.dart` so the Kotlin (Stage 2) and Dart (Stage 3)
// sides agree without a wide Pigeon data-class surface.
import 'package:pigeon/pigeon.dart';

/// Raw Health Connect availability signals, mapped to the Dart
/// `HealthConnectAvailability` enum on the Flutter side. Kept as separate
/// signals (rather than a native enum) so the enum stays a single source of
/// truth in Dart.
class HealthConnectAvailabilityDetail {
  /// Raw `HealthConnectClient.getSdkStatus` int.
  final int sdkStatus;

  /// True when running in a work/managed profile where Health Connect is
  /// unsupported (Android 13+).
  final bool unsupportedProfile;

  /// True when the standalone Health Connect APK is installed on Android 13-
  /// but the Play Store is not (so it can never be updated).
  final bool standaloneNeedsPlayStore;

  HealthConnectAvailabilityDetail(
    this.sdkStatus,
    this.unsupportedProfile,
    this.standaloneNeedsPlayStore,
  );
}

/// Tri-state result of `HealthConnectFeatures.getFeatureStatus(feature)`, mapped
/// to the Dart `FeatureStatus` enum on the Flutter side. Because the app builds
/// against the latest connect-client alpha, the SDK exposes feature constants
/// that the *installed* Health Connect provider may lag behind on — [unknown]
/// means the provider is too old to even report the feature's status, which the
/// gating logic treats the same as [unavailable].
enum FeatureStatusMsg { unknown, available, unavailable }

// ═══════════════════════════════════════════════════════════════════════════
// TYPED DOMAIN MODELS
//
// These mirror the app's freezed domain models (`lib/domain/model/*`). Times
// cross the bridge as epoch-millis ints (Pigeon has no DateTime); the Dart
// boundary (`HealthConnectNativeDataSource`) maps `*Msg` classes <-> the freezed
// models. Suffix `Msg` avoids colliding with the identically-named domain
// classes imported in the data source.
// ═══════════════════════════════════════════════════════════════════════════

// ── Body (Phase 1) ──────────────────────────────────────────────────────────

enum BodyMeasurementTypeMsg { weight, height, bodyFat }

class WeightEntryMsg {
  final int timeEpochMs;
  final double weightKg;
  final String source;
  final String id;
  final bool isOpenVitalsEntry;
  WeightEntryMsg(
    this.timeEpochMs,
    this.weightKg,
    this.source,
    this.id,
    this.isOpenVitalsEntry,
  );
}

class HeightEntryMsg {
  final int timeEpochMs;
  final double heightCm;
  final String source;
  final String id;
  final bool isOpenVitalsEntry;
  HeightEntryMsg(
    this.timeEpochMs,
    this.heightCm,
    this.source,
    this.id,
    this.isOpenVitalsEntry,
  );
}

class BodyFatEntryMsg {
  final int timeEpochMs;
  final double percent;
  final String source;
  final String id;
  final bool isOpenVitalsEntry;
  BodyFatEntryMsg(
    this.timeEpochMs,
    this.percent,
    this.source,
    this.id,
    this.isOpenVitalsEntry,
  );
}

/// Shared shape for LeanBodyMass / BoneMass / BodyWaterMass entries.
class BodyMassEntryMsg {
  final int timeEpochMs;
  final double massKg;
  final String source;
  BodyMassEntryMsg(this.timeEpochMs, this.massKg, this.source);
}

class BmrEntryMsg {
  final int timeEpochMs;
  final double kcalPerDay;
  final String source;
  BmrEntryMsg(this.timeEpochMs, this.kcalPerDay, this.source);
}

class BodyMeasurementEntryMsg {
  final String id;
  final BodyMeasurementTypeMsg type;
  final int timeEpochMs;
  final double value;
  final String source;
  final bool isOpenVitalsEntry;
  BodyMeasurementEntryMsg(
    this.id,
    this.type,
    this.timeEpochMs,
    this.value,
    this.source,
    this.isOpenVitalsEntry,
  );
}

class BodyMeasurementWriteRequestMsg {
  final BodyMeasurementTypeMsg type;
  final int timeEpochMs;
  final double value;
  BodyMeasurementWriteRequestMsg(this.type, this.timeEpochMs, this.value);
}

// ── Hydration + Mindfulness (Phase 2) ────────────────────────────────────────

class HydrationEntryMsg {
  final int startEpochMs;
  final int endEpochMs;
  final double liters;
  final String source;
  final String id;
  final String? clientRecordId;
  final bool isOpenVitalsEntry;
  HydrationEntryMsg(
    this.startEpochMs,
    this.endEpochMs,
    this.liters,
    this.source,
    this.id,
    this.clientRecordId,
    this.isOpenVitalsEntry,
  );
}

/// One daily hydration total; [dateEpochMs] is local midnight of the day.
class DailyHydrationMsg {
  final int dateEpochMs;
  final double liters;
  DailyHydrationMsg(this.dateEpochMs, this.liters);
}

class HydrationWriteRequestMsg {
  final int timeEpochMs;
  final double volumeLiters;
  final String? drinkId;
  HydrationWriteRequestMsg(this.timeEpochMs, this.volumeLiters, this.drinkId);
}

class MindfulnessSessionMsg {
  final String id;
  final String? title;
  final int startEpochMs;
  final int endEpochMs;
  final int durationMs;
  final String source;
  final bool isOpenVitalsEntry;
  MindfulnessSessionMsg(
    this.id,
    this.title,
    this.startEpochMs,
    this.endEpochMs,
    this.durationMs,
    this.source,
    this.isOpenVitalsEntry,
  );
}

class MindfulnessSessionWriteRequestMsg {
  final String title;
  final int startEpochMs;
  final int endEpochMs;
  MindfulnessSessionWriteRequestMsg(this.title, this.startEpochMs, this.endEpochMs);
}

// ── Vitals (Phase 3) ─────────────────────────────────────────────────────────

enum VitalsMeasurementTypeMsg { bloodPressure, spo2, respiratoryRate, bodyTemperature }

class BloodPressureEntryMsg {
  final int timeEpochMs;
  final int systolicMmHg;
  final int diastolicMmHg;
  final String source;
  final String id;
  final bool isOpenVitalsEntry;
  BloodPressureEntryMsg(
    this.timeEpochMs,
    this.systolicMmHg,
    this.diastolicMmHg,
    this.source,
    this.id,
    this.isOpenVitalsEntry,
  );
}

class SpO2EntryMsg {
  final int timeEpochMs;
  final double percent;
  final String source;
  final String id;
  final bool isOpenVitalsEntry;
  SpO2EntryMsg(this.timeEpochMs, this.percent, this.source, this.id, this.isOpenVitalsEntry);
}

class RespiratoryRateEntryMsg {
  final int timeEpochMs;
  final double breathsPerMinute;
  final String source;
  final String id;
  final bool isOpenVitalsEntry;
  RespiratoryRateEntryMsg(
    this.timeEpochMs,
    this.breathsPerMinute,
    this.source,
    this.id,
    this.isOpenVitalsEntry,
  );
}

class BodyTempEntryMsg {
  final int timeEpochMs;
  final double temperatureCelsius;
  final String source;
  final String id;
  final bool isOpenVitalsEntry;
  BodyTempEntryMsg(
    this.timeEpochMs,
    this.temperatureCelsius,
    this.source,
    this.id,
    this.isOpenVitalsEntry,
  );
}

class Vo2MaxEntryMsg {
  final int timeEpochMs;
  final double vo2MaxMlPerKgPerMin;
  final String source;
  Vo2MaxEntryMsg(this.timeEpochMs, this.vo2MaxMlPerKgPerMin, this.source);
}

class BloodGlucoseEntryMsg {
  final int timeEpochMs;
  final double millimolesPerLiter;
  final int specimenSource;
  final int mealType;
  final int relationToMeal;
  final String source;
  BloodGlucoseEntryMsg(
    this.timeEpochMs,
    this.millimolesPerLiter,
    this.specimenSource,
    this.mealType,
    this.relationToMeal,
    this.source,
  );
}

class SkinTemperatureEntryMsg {
  final int startEpochMs;
  final int endEpochMs;
  final double? baselineCelsius;
  final double? averageDeltaCelsius;
  final double? minDeltaCelsius;
  final double? maxDeltaCelsius;
  final int measurementLocation;
  final String source;
  SkinTemperatureEntryMsg(
    this.startEpochMs,
    this.endEpochMs,
    this.baselineCelsius,
    this.averageDeltaCelsius,
    this.minDeltaCelsius,
    this.maxDeltaCelsius,
    this.measurementLocation,
    this.source,
  );
}

class VitalsMeasurementEntryMsg {
  final String id;
  final VitalsMeasurementTypeMsg type;
  final int timeEpochMs;
  final double value;
  final double? secondaryValue;
  final String source;
  final bool isOpenVitalsEntry;
  VitalsMeasurementEntryMsg(
    this.id,
    this.type,
    this.timeEpochMs,
    this.value,
    this.secondaryValue,
    this.source,
    this.isOpenVitalsEntry,
  );
}

class VitalsMeasurementWriteRequestMsg {
  final VitalsMeasurementTypeMsg type;
  final int timeEpochMs;
  final double value;
  final double? secondaryValue;
  VitalsMeasurementWriteRequestMsg(
    this.type,
    this.timeEpochMs,
    this.value,
    this.secondaryValue,
  );
}

// ── Cycle (Phase 4) — all read-only ──────────────────────────────────────────

class MenstruationFlowEntryMsg {
  final int timeEpochMs;
  final int flow;
  final String source;
  MenstruationFlowEntryMsg(this.timeEpochMs, this.flow, this.source);
}

class MenstruationPeriodEntryMsg {
  final int startEpochMs;
  final int endEpochMs;
  final String source;
  MenstruationPeriodEntryMsg(this.startEpochMs, this.endEpochMs, this.source);
}

class OvulationTestEntryMsg {
  final int timeEpochMs;
  final int result;
  final String source;
  OvulationTestEntryMsg(this.timeEpochMs, this.result, this.source);
}

class CervicalMucusEntryMsg {
  final int timeEpochMs;
  final int appearance;
  final int sensation;
  final String source;
  CervicalMucusEntryMsg(this.timeEpochMs, this.appearance, this.sensation, this.source);
}

class BasalBodyTemperatureEntryMsg {
  final int timeEpochMs;
  final double temperatureCelsius;
  final int measurementLocation;
  final String source;
  BasalBodyTemperatureEntryMsg(
    this.timeEpochMs,
    this.temperatureCelsius,
    this.measurementLocation,
    this.source,
  );
}

class IntermenstrualBleedingEntryMsg {
  final int timeEpochMs;
  final String source;
  IntermenstrualBleedingEntryMsg(this.timeEpochMs, this.source);
}

class SexualActivityEntryMsg {
  final int timeEpochMs;
  final int protectionUsed;
  final String source;
  SexualActivityEntryMsg(this.timeEpochMs, this.protectionUsed, this.source);
}

// ── Heart (Phase 5) ──────────────────────────────────────────────────────────

class HeartRateSampleMsg {
  final int timeEpochMs;
  final int beatsPerMinute;
  final String source;
  HeartRateSampleMsg(this.timeEpochMs, this.beatsPerMinute, this.source);
}

/// One heart-rate aggregate bucket (avg bpm over [startEpochMs]..bucket end);
/// the Dart side turns these into `HeartRateSample`s via the shared helper.
class HeartRateAggBucketMsg {
  final int startEpochMs;
  final int avgBpm;
  HeartRateAggBucketMsg(this.startEpochMs, this.avgBpm);
}

class HeartRateSummaryMsg {
  final int dateEpochMs;
  final int avgBpm;
  final int minBpm;
  final int maxBpm;
  HeartRateSummaryMsg(this.dateEpochMs, this.avgBpm, this.minBpm, this.maxBpm);
}

class RestingHeartRateSampleMsg {
  final int timeEpochMs;
  final int beatsPerMinute;
  final String source;
  RestingHeartRateSampleMsg(this.timeEpochMs, this.beatsPerMinute, this.source);
}

class DailyRestingHRMsg {
  final int dateEpochMs;
  final int bpm;
  DailyRestingHRMsg(this.dateEpochMs, this.bpm);
}

class HrvSampleMsg {
  final int timeEpochMs;
  final double rmssdMs;
  final String source;
  HrvSampleMsg(this.timeEpochMs, this.rmssdMs, this.source);
}

class DailyHrvMsg {
  final int dateEpochMs;
  final double rmssdMs;
  DailyHrvMsg(this.dateEpochMs, this.rmssdMs);
}

// ── Vitals daily aggregates (Phase 3 / Stage 4) ──────────────────────────────
// Long-range vitals charts (week/month/year) plot one point per day. Reading a
// year of RAW records just to average them per day marshals thousands of
// records across this channel; these carry the per-day mean already bucketed on
// the Kotlin side. [count] is how many raw readings the day averaged, so the
// Dart side can reconstruct a count-weighted period mean without the raw list.

class DailyVitalPointMsg {
  final int dateEpochMs;
  final double value;
  final int count;
  DailyVitalPointMsg(this.dateEpochMs, this.value, this.count);
}

class DailyBloodPressurePointMsg {
  final int dateEpochMs;
  final double systolic;
  final double diastolic;
  final int count;
  DailyBloodPressurePointMsg(
    this.dateEpochMs,
    this.systolic,
    this.diastolic,
    this.count,
  );
}

/// A batch of Health Connect changes for one record type, from a changes token.
/// [upsertedDayEpochMs] is the deduped set of local day-starts touched by
/// inserted/updated records (so the cache recomputes just those days).
/// [hasDeletions] is true if any record was deleted — deletions carry only an id,
/// not a date, so the cache full-rebuilds that metric when set. Paginate with
/// [nextToken] while [hasMore]; [tokenExpired] means start over from a fresh
/// token + full read.
class VitalsChangesMsg {
  final List<int> upsertedDayEpochMs;
  final bool hasDeletions;
  final String nextToken;
  final bool tokenExpired;
  final bool hasMore;
  VitalsChangesMsg(
    this.upsertedDayEpochMs,
    this.hasDeletions,
    this.nextToken,
    this.tokenExpired,
    this.hasMore,
  );
}

// ── Nutrition (Phase 6) ──────────────────────────────────────────────────────

enum CaloriesBurnedSourceMsg { noData, recordedTotal, estimatedActiveAndBmr }

/// Nutrient maps are keyed by the `NutritionNutrient.storageName` strings
/// (e.g. "ENERGY", "TOTAL_CARBOHYDRATE") — kcal for energy nutrients, grams for
/// mass nutrients.
class NutritionEntryMsg {
  final int startEpochMs;
  final int endEpochMs;
  final int mealType;
  final String? name;
  final String source;
  final String id;
  final String? clientRecordId;
  final bool isOpenVitalsEntry;
  final Map<String, double> nutrientValues;
  NutritionEntryMsg(
    this.startEpochMs,
    this.endEpochMs,
    this.mealType,
    this.name,
    this.source,
    this.id,
    this.clientRecordId,
    this.isOpenVitalsEntry,
    this.nutrientValues,
  );
}

class DailyMacrosMsg {
  final int dateEpochMs;
  final Map<String, double> nutrientValues;
  DailyMacrosMsg(this.dateEpochMs, this.nutrientValues);
}

class DailyNutritionMsg {
  final int dateEpochMs;
  final double hydrationLiters;
  final double caloriesBurnedKcal;
  final CaloriesBurnedSourceMsg caloriesBurnedSource;
  DailyNutritionMsg(
    this.dateEpochMs,
    this.hydrationLiters,
    this.caloriesBurnedKcal,
    this.caloriesBurnedSource,
  );
}

class NutritionWriteRequestMsg {
  final int timeEpochMs;
  final String? name;
  final Map<String, double> nutrientValues;
  final String? associatedHydrationClientRecordId;
  NutritionWriteRequestMsg(
    this.timeEpochMs,
    this.name,
    this.nutrientValues,
    this.associatedHydrationClientRecordId,
  );
}

// ── Sleep (Phase 7) ──────────────────────────────────────────────────────────

class SleepStageMsg {
  final int startEpochMs;
  final int endEpochMs;
  final int stageType;
  SleepStageMsg(this.startEpochMs, this.endEpochMs, this.stageType);
}

class SleepDeviceDataMsg {
  final int type;
  final String? manufacturer;
  final String? model;
  SleepDeviceDataMsg(this.type, this.manufacturer, this.model);
}

// ── Activity / Exercise (Phase 8) ────────────────────────────────────────────

enum ExerciseRouteStatusMsg { data, consentRequired, noData }

class ExerciseDeviceDataMsg {
  final int type;
  final String? manufacturer;
  final String? model;
  ExerciseDeviceDataMsg(this.type, this.manufacturer, this.model);
}

class ExerciseSegmentMsg {
  final int startEpochMs;
  final int endEpochMs;
  final int segmentType;
  final int repetitions;
  final int? setIndex;
  ExerciseSegmentMsg(
    this.startEpochMs,
    this.endEpochMs,
    this.segmentType,
    this.repetitions,
    this.setIndex,
  );
}

class ExerciseLapMsg {
  final int startEpochMs;
  final int endEpochMs;
  final double? lengthMeters;
  ExerciseLapMsg(this.startEpochMs, this.endEpochMs, this.lengthMeters);
}

class ExerciseRoutePointMsg {
  final int timeEpochMs;
  final double latitude;
  final double longitude;
  final double? altitudeMeters;
  final double? horizontalAccuracyMeters;
  final double? verticalAccuracyMeters;
  ExerciseRoutePointMsg(
    this.timeEpochMs,
    this.latitude,
    this.longitude,
    this.altitudeMeters,
    this.horizontalAccuracyMeters,
    this.verticalAccuracyMeters,
  );
}

class ExerciseRouteMsg {
  final ExerciseRouteStatusMsg status;
  final List<ExerciseRoutePointMsg> points;
  ExerciseRouteMsg(this.status, this.points);
}

/// Intrinsic exercise-session fields, plus the two route metrics that are only
/// obtainable by aggregating over the session window ([totalDistanceMeters] /
/// [averageSpeedMetersPerSecond]). The remaining aggregate-derived metrics are
/// resolved on the Dart side / left null, matching the current data source.
class ExerciseDataMsg {
  final String id;
  final String? title;
  final int exerciseType;
  final int startEpochMs;
  final int endEpochMs;
  final String source;
  final String? notes;
  final String? clientRecordId;
  final String? plannedExerciseSessionId;
  final ExerciseDeviceDataMsg? device;
  final List<ExerciseSegmentMsg> segments;
  final List<ExerciseLapMsg> laps;
  final ExerciseRouteMsg route;
  final bool isOpenVitalsEntry;

  /// `DistanceRecord.DISTANCE_TOTAL` aggregated over the session window. Null
  /// unless the session was read through `readExerciseSessionsWithMetrics` with
  /// the read-distance permission granted.
  final double? totalDistanceMeters;

  /// `SpeedRecord.SPEED_AVG` aggregated over the session window. Null unless the
  /// session was read through `readExerciseSessionsWithMetrics` with the
  /// read-speed permission granted (or the provider recorded no speed samples).
  final double? averageSpeedMetersPerSecond;

  // The record's own provenance — the same block [SleepDataMsg] carries, and
  // dropped from here for the same reason: nobody noticed a message could be
  // missing a field. It is NOT cosmetic on an exercise session. `recordingMethod`
  // is how the activities screen counts manually-entered workouts, and
  // `lastModifiedTime` is the tie-breaker that decides WHICH of two duplicate
  // sessions survives deduplication — with it always null, that comparison was
  // always a draw.
  final int? startZoneOffsetSeconds;
  final int? endZoneOffsetSeconds;
  final int? lastModifiedEpochMs;
  final int? clientRecordVersion;
  final int? recordingMethod;

  ExerciseDataMsg(
    this.id,
    this.title,
    this.exerciseType,
    this.startEpochMs,
    this.endEpochMs,
    this.source,
    this.notes,
    this.clientRecordId,
    this.plannedExerciseSessionId,
    this.device,
    this.segments,
    this.laps,
    this.route,
    this.isOpenVitalsEntry,
    this.totalDistanceMeters,
    this.averageSpeedMetersPerSecond,
    this.startZoneOffsetSeconds,
    this.endZoneOffsetSeconds,
    this.lastModifiedEpochMs,
    this.clientRecordVersion,
    this.recordingMethod,
  );
}

/// The sibling-record totals for one exercise session's window — the numbers the
/// `ExerciseSessionRecord` itself does not carry. Every field is null when the
/// caller did not ask for that metric, or when no such record covers the window.
class ExerciseSessionMetricsMsg {
  /// `DistanceRecord.DISTANCE_TOTAL`.
  final double? totalDistanceMeters;

  /// `SpeedRecord.SPEED_AVG`.
  final double? averageSpeedMetersPerSecond;

  /// `StepsRecord.COUNT_TOTAL`.
  final int? steps;

  /// `TotalCaloriesBurnedRecord.ENERGY_TOTAL`, in kcal.
  final double? totalCaloriesKcal;

  /// `ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL`, in kcal.
  final double? activeCaloriesKcal;

  /// `ElevationGainedRecord.ELEVATION_GAINED_TOTAL`.
  final double? elevationGainedMeters;

  /// `FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL`.
  final int? floorsClimbed;

  /// `WheelchairPushesRecord.COUNT_TOTAL`.
  final int? wheelchairPushes;

  /// `PowerRecord.POWER_AVG`, in watts.
  ///
  /// The last of these to be wired up, and the app had every OTHER piece of it
  /// already: it asks Health Connect for READ_POWER, tells you so during
  /// onboarding, writes `PowerRecord` from BLE sensors, and renders an "Average
  /// power" row on the activity screen. It just never read it back, so that row
  /// said "Not available" on every ride anyone ever recorded with a power meter.
  final double? averagePowerWatts;

  ExerciseSessionMetricsMsg(
    this.totalDistanceMeters,
    this.averageSpeedMetersPerSecond,
    this.steps,
    this.totalCaloriesKcal,
    this.activeCaloriesKcal,
    this.elevationGainedMeters,
    this.floorsClimbed,
    this.wheelchairPushes,
    this.averagePowerWatts,
  );
}

class SpeedSampleMsg {
  final int timeEpochMs;
  final double metersPerSecond;
  final String source;
  SpeedSampleMsg(this.timeEpochMs, this.metersPerSecond, this.source);
}

/// One cadence sample. [isCycling] tells apart a `CyclingPedalingCadenceRecord`
/// (revolutions per minute) from a `StepsCadenceRecord` (steps per minute) —
/// the two share a shape but not a unit.
class ActivityCadenceSampleMsg {
  final int timeEpochMs;
  final double rate;
  final bool isCycling;
  final String source;
  ActivityCadenceSampleMsg(
    this.timeEpochMs,
    this.rate,
    this.isCycling,
    this.source,
  );
}

// ── Planned exercise sessions ────────────────────────────────────────────────

/// Which `ExerciseCompletionGoal` a planned step carries. Health Connect models
/// a handful more; anything the app does not understand arrives as [unknown]
/// and is written back as manual completion.
enum PlannedExerciseCompletionKindMsg {
  repetitions,
  durationSeconds,
  manual,
  unknown,
}

class PlannedExerciseStepMsg {
  final int exerciseType;
  final int exercisePhase;
  final String? description;
  final PlannedExerciseCompletionKindMsg completionKind;

  /// Set only when [completionKind] is `repetitions`.
  final int? completionRepetitions;

  /// Set only when [completionKind] is `durationSeconds`.
  final int? completionSeconds;
  PlannedExerciseStepMsg(
    this.exerciseType,
    this.exercisePhase,
    this.description,
    this.completionKind,
    this.completionRepetitions,
    this.completionSeconds,
  );
}

class PlannedExerciseBlockMsg {
  final int repetitions;
  final String? description;
  final List<PlannedExerciseStepMsg> steps;
  PlannedExerciseBlockMsg(this.repetitions, this.description, this.steps);
}

class PlannedExerciseSessionMsg {
  final String id;
  final String? title;
  final int exerciseType;
  final int startEpochMs;
  final int endEpochMs;
  final bool hasExplicitTime;
  final String? completedExerciseSessionId;
  final String? notes;
  final String source;
  final List<PlannedExerciseBlockMsg> blocks;
  PlannedExerciseSessionMsg(
    this.id,
    this.title,
    this.exerciseType,
    this.startEpochMs,
    this.endEpochMs,
    this.hasExplicitTime,
    this.completedExerciseSessionId,
    this.notes,
    this.source,
    this.blocks,
  );
}

class PlannedExerciseWriteRequestMsg {
  /// When set, the existing plan is deleted and replaced (Health Connect has no
  /// in-place update for planned sessions).
  final String? id;
  final int exerciseType;
  final int startEpochMs;
  final int endEpochMs;
  final String? title;
  final String? notes;
  final List<PlannedExerciseBlockMsg> blocks;
  PlannedExerciseWriteRequestMsg(
    this.id,
    this.exerciseType,
    this.startEpochMs,
    this.endEpochMs,
    this.title,
    this.notes,
    this.blocks,
  );
}

// ── Apple Health import (Phase 9) ────────────────────────────────────────────

class ImportSampleMsg {
  final int timeEpochMs;
  final double value;
  ImportSampleMsg(this.timeEpochMs, this.value);
}

class ImportSleepStageMsg {
  final int startEpochMs;
  final int endEpochMs;
  final int stage;
  ImportSleepStageMsg(this.startEpochMs, this.endEpochMs, this.stage);
}

/// A typed, discriminated import record. [recordType] is the canonical schema
/// name (e.g. "Steps", "Nutrition"); scalar fields ride in [doubleFields] /
/// [intFields] keyed as the native builder expects, with the complex records
/// (HR/Speed samples, sleep stages, exercise route) in the typed lists. Times
/// are interval [startEpochMs]..[endEpochMs] (endEpochMs null = instant record).
class ImportRecordMsg {
  final String recordType;
  final String clientRecordId;
  final int startEpochMs;
  final int? endEpochMs;
  final int? startZoneOffsetSeconds;
  final int? endZoneOffsetSeconds;
  final Map<String, double> doubleFields;
  final Map<String, int> intFields;
  final String? name;
  final List<ImportSampleMsg> samples;
  final List<ImportSleepStageMsg> sleepStages;
  final List<ExerciseRoutePointMsg> routePoints;
  ImportRecordMsg(
    this.recordType,
    this.clientRecordId,
    this.startEpochMs,
    this.endEpochMs,
    this.startZoneOffsetSeconds,
    this.endZoneOffsetSeconds,
    this.doubleFields,
    this.intFields,
    this.name,
    this.samples,
    this.sleepStages,
    this.routePoints,
  );
}

class ActivityPauseIntervalMsg {
  final int startEpochMs;
  final int endEpochMs;
  ActivityPauseIntervalMsg(this.startEpochMs, this.endEpochMs);
}

class BleHeartRateSampleMsg {
  final int timeEpochMs;
  final int beatsPerMinute;
  BleHeartRateSampleMsg(this.timeEpochMs, this.beatsPerMinute);
}

class BlePowerSampleMsg {
  final int timeEpochMs;
  final double watts;
  BlePowerSampleMsg(this.timeEpochMs, this.watts);
}

class BleCyclingCadenceSampleMsg {
  final int timeEpochMs;
  final int rpm;
  BleCyclingCadenceSampleMsg(this.timeEpochMs, this.rpm);
}

class BleSpeedSampleMsg {
  final int timeEpochMs;
  final double metersPerSecond;

  /// Running speed and cycling speed are written as separate `SpeedRecord`s
  /// (clientRecordId kinds `running_speed` vs `speed`), mirroring the Kotlin
  /// `BleSpeedSample.isRunning` split.
  final bool isRunning;
  BleSpeedSampleMsg(this.timeEpochMs, this.metersPerSecond, this.isRunning);
}

class BleStepsCadenceSampleMsg {
  final int timeEpochMs;
  final int stepsPerMinute;
  BleStepsCadenceSampleMsg(this.timeEpochMs, this.stepsPerMinute);
}

/// Recorded BLE sensor series captured during an activity recording; mirrors
/// the Dart/Kotlin `BleRecordingSampleBuffer`.
class ActivityBleSamplesMsg {
  final List<BleHeartRateSampleMsg> heartRateSamples;
  final List<BlePowerSampleMsg> powerSamples;
  final List<BleCyclingCadenceSampleMsg> cyclingCadenceSamples;
  final List<BleSpeedSampleMsg> speedSamples;
  final List<BleStepsCadenceSampleMsg> stepsCadenceSamples;
  ActivityBleSamplesMsg(
    this.heartRateSamples,
    this.powerSamples,
    this.cyclingCadenceSamples,
    this.speedSamples,
    this.stepsCadenceSamples,
  );
}

class ActivityWriteRequestMsg {
  final int exerciseType;
  final int startEpochMs;
  final int endEpochMs;
  final String? title;
  final String? notes;
  final String? plannedExerciseSessionId;
  final List<ExerciseSegmentMsg> segments;
  final List<ExerciseLapMsg> laps;
  final List<ExerciseRoutePointMsg> routePoints;

  /// Pauses recorded during a live recording. When [segments] is empty the
  /// native side synthesizes active + PAUSE `ExerciseSegment`s from these.
  final List<ActivityPauseIntervalMsg>? pauseIntervals;

  /// Session totals written as standalone records alongside the session
  /// (DistanceRecord, ElevationGainedRecord, ActiveCaloriesBurnedRecord,
  /// TotalCaloriesBurnedRecord, StepsRecord). All canonical units.
  final int? stepsCount;
  final double? distanceMeters;
  final double? elevationGainedMeters;
  final double? activeCaloriesKcal;
  final double? totalCaloriesKcal;

  /// Recorded BLE sensor series written as sample-series records
  /// (HeartRate/Power/CyclingPedalingCadence/Speed/StepsCadence).
  final ActivityBleSamplesMsg? bleSamples;
  ActivityWriteRequestMsg(
    this.exerciseType,
    this.startEpochMs,
    this.endEpochMs,
    this.title,
    this.notes,
    this.plannedExerciseSessionId,
    this.segments,
    this.laps,
    this.routePoints,
    this.pauseIntervals,
    this.stepsCount,
    this.distanceMeters,
    this.elevationGainedMeters,
    this.activeCaloriesKcal,
    this.totalCaloriesKcal,
    this.bleSamples,
  );
}

/// Raw (unmerged) sleep session; merging + range selection happen on the Dart
/// side. `durationMs` is recomputed from stages by the Dart mapper.
class SleepDataMsg {
  final String id;
  final int startEpochMs;
  final int endEpochMs;
  final String source;
  final String? title;
  final String? notes;
  final String? clientRecordId;
  final SleepDeviceDataMsg? device;
  final List<SleepStageMsg> stages;

  // The record's own provenance, all of it shown on the sleep detail screen.
  // Health Connect has always had these; the port simply never carried them
  // across the bridge, so the screen said "Not available" for five rows that
  // Kotlin filled in.
  //
  // Zone offsets are the SECONDS a writer recorded the session in, not this
  // phone's current zone: a night slept in another timezone is the whole reason
  // Health Connect stores them, so they must not be recomputed on this side.
  final int? startZoneOffsetSeconds;
  final int? endZoneOffsetSeconds;
  final int? lastModifiedEpochMs;
  final int? clientRecordVersion;
  final int? recordingMethod;

  SleepDataMsg(
    this.id,
    this.startEpochMs,
    this.endEpochMs,
    this.source,
    this.title,
    this.notes,
    this.clientRecordId,
    this.device,
    this.stages,
    this.startZoneOffsetSeconds,
    this.endZoneOffsetSeconds,
    this.lastModifiedEpochMs,
    this.clientRecordVersion,
    this.recordingMethod,
  );
}

@ConfigurePigeon(
  PigeonOptions(
    dartOut: 'lib/src/messages.g.dart',
    kotlinOut:
        'android/src/main/kotlin/tech/mmarca/openvitals/health_connect_native/Messages.g.kt',
    kotlinOptions: KotlinOptions(
      package: 'tech.mmarca.openvitals.health_connect_native',
    ),
    dartPackageName: 'health_connect_native',
  ),
)

/// Host (Android/Kotlin) API surface backed by `HealthConnectClient`.
///
/// All record payloads are JSON strings; see the record JSON schema in
/// `lib/health_connect_native.dart`. Permission strings are Health Connect
/// permission identifiers (e.g. `android.permission.health.READ_STEPS`).
@HostApi()
abstract class HealthConnectHostApi {
  /// Maps to `HealthConnectClient.getSdkStatus(context)`.
  ///
  /// Returns the raw SDK status int (e.g. `SDK_AVAILABLE`,
  /// `SDK_UNAVAILABLE`, `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED`).
  int getSdkStatus();

  /// Fuller availability picture than [getSdkStatus]: SDK status plus the
  /// work-profile and standalone-needs-Play-Store overrides, so Dart can resolve
  /// NOT_SUPPORTED / NEEDS_PLAY_STORE / NEEDS_PROVIDER_UPDATE / AVAILABLE.
  HealthConnectAvailabilityDetail availabilityDetail();

  /// Mirrors the user's "pause Health Connect sync" toggle into the native
  /// sync-gate. While disabled, reads short-circuit to empty and writes throw.
  void setSyncEnabled(bool enabled);

  /// Returns the native sync-gate's current state.
  bool getSyncEnabled();

  /// Returns the subset of [permissions] currently granted.
  @async
  List<String> getGrantedPermissions(List<String> permissions);

  /// Returns the subset of [permissions] the installed Health Connect provider
  /// actually recognizes. Because the app builds against a newer connect-client
  /// than the on-device provider, some permission strings (e.g. newer record
  /// types like STEPS_CADENCE) are undefined on this device and can never be
  /// granted; filtering them keeps the permission taxonomy honest.
  @async
  List<String> filterSupportedPermissions(List<String> permissions);

  /// Launches the Health Connect permission contract via the Activity and
  /// resolves to whether every requested permission ended up granted.
  @async
  bool requestPermissions(List<String> permissions);

  /// Opens the Health Connect page for this app (app-specific permission
  /// management on Android 14+, falling back to Health Connect settings) so the
  /// user can manually grant permissions the runtime dialog reports as
  /// non-requestable (e.g. planned exercise, exercise routes, background/history
  /// access). Returns whether a page was launched.
  @async
  bool openHealthConnectSettings();

  /// Tri-state availability of an optional Health Connect feature on this device
  /// via `HealthConnectFeatures.getFeatureStatus`, e.g. `"SKIN_TEMPERATURE"`,
  /// `"MINDFULNESS_SESSION"`, `"PLANNED_EXERCISE"`,
  /// `"READ_HEALTH_DATA_HISTORY"`, `"READ_HEALTH_DATA_IN_BACKGROUND"`.
  /// Unrecognized keys resolve to [FeatureStatusMsg.unknown].
  @async
  FeatureStatusMsg getFeatureStatus(String feature);

  /// Runs an aggregation over [aggregateMetrics] in the given window, returning
  /// a metric-key -> value map (value is `null` when Health Connect has no data
  /// for that metric in the window).
  @async
  Map<String, double?> aggregate(
    List<String> aggregateMetrics,
    int startEpochMs,
    int endEpochMs,
  );

  /// Aggregates [aggregateMetrics] grouped into buckets of [bucketType]
  /// (e.g. `"DAYS"`, `"WEEKS"`, `"MONTHS"`), returning one JSON object (as a
  /// String) per bucket with its time range and aggregated values.
  @async
  List<String> aggregateGroupByPeriodJson(
    List<String> aggregateMetrics,
    int startEpochMs,
    int endEpochMs,
    String bucketType,
  );

  /// Aggregates [aggregateMetrics] into fixed-length buckets of
  /// [bucketMinutes], returning one JSON object (as a String) per bucket with
  /// its time range and aggregated values.
  ///
  /// Unlike [aggregateGroupByPeriodJson], which slices on calendar periods, this
  /// slices on a wall-clock duration — what an intraday (hour-by-hour) chart
  /// needs. Buckets with no data are omitted by Health Connect.
  @async
  List<String> aggregateGroupByDurationJson(
    List<String> aggregateMetrics,
    int startEpochMs,
    int endEpochMs,
    int bucketMinutes,
  );

  /// Import dedup helper: of the supplied [clientRecordIds], returns the subset
  /// that ALREADY exist in Health Connect for [recordType].
  @async
  List<String> filterExistingClientIds(
    String recordType,
    List<String> clientRecordIds,
  );

  // ── Body (Phase 1) — typed reads/writes via BodyHealthReader ──────────────

  @async
  List<WeightEntryMsg> readWeightEntries(int startEpochMs, int endEpochMs);
  @async
  WeightEntryMsg? readLatestWeight();
  @async
  List<HeightEntryMsg> readHeightEntries(int startEpochMs, int endEpochMs);
  @async
  HeightEntryMsg? readLatestHeightEntry();
  @async
  List<BodyFatEntryMsg> readBodyFatEntries(int startEpochMs, int endEpochMs);
  @async
  BodyFatEntryMsg? readLatestBodyFat();
  @async
  List<BodyMassEntryMsg> readLeanBodyMassEntries(int startEpochMs, int endEpochMs);
  @async
  BodyMassEntryMsg? readLatestLeanBodyMass();
  @async
  List<BmrEntryMsg> readBmrEntries(int startEpochMs, int endEpochMs);
  @async
  BmrEntryMsg? readLatestBmr();
  @async
  List<BodyMassEntryMsg> readBoneMassEntries(int startEpochMs, int endEpochMs);
  @async
  BodyMassEntryMsg? readLatestBoneMass();
  @async
  List<BodyMassEntryMsg> readBodyWaterMassEntries(int startEpochMs, int endEpochMs);
  @async
  BodyMassEntryMsg? readLatestBodyWaterMass();
  @async
  String writeBodyMeasurementEntry(BodyMeasurementWriteRequestMsg request);
  @async
  BodyMeasurementEntryMsg? readBodyMeasurementEntry(
    BodyMeasurementTypeMsg type,
    String id,
  );
  @async
  void updateBodyMeasurementEntry(String id, BodyMeasurementWriteRequestMsg request);
  @async
  void deleteBodyMeasurementEntry(BodyMeasurementTypeMsg type, String id);

  // ── Hydration (Phase 2) ────────────────────────────────────────────────────

  @async
  double? readHydrationLiters(int startEpochMs, int endEpochMs);
  @async
  List<DailyHydrationMsg> readDailyHydration(int startEpochMs, int endEpochMs);
  @async
  List<HydrationEntryMsg> readHydrationEntries(int startEpochMs, int endEpochMs);
  @async
  HydrationEntryMsg? readHydrationEntry(String id);
  @async
  String writeHydrationEntry(HydrationWriteRequestMsg request);
  @async
  void updateHydrationEntry(String id, HydrationWriteRequestMsg request);
  @async
  String? deleteHydrationEntry(String id);

  // ── Mindfulness (Phase 2) ──────────────────────────────────────────────────

  @async
  List<MindfulnessSessionMsg> readMindfulnessSessions(int startEpochMs, int endEpochMs);
  @async
  MindfulnessSessionMsg? readMindfulnessSession(String id);
  @async
  int readMindfulnessMinutes(int startEpochMs, int endEpochMs);
  @async
  String writeMindfulnessSessionEntry(MindfulnessSessionWriteRequestMsg request);
  @async
  void updateMindfulnessSessionEntry(String id, MindfulnessSessionWriteRequestMsg request);
  @async
  void deleteMindfulnessSessionEntry(String id);

  // ── Vitals (Phase 3) ───────────────────────────────────────────────────────

  @async
  List<BloodPressureEntryMsg> readBloodPressureEntries(int startEpochMs, int endEpochMs);
  @async
  BloodPressureEntryMsg? readLatestBloodPressure(int startEpochMs, int endEpochMs);
  @async
  List<SpO2EntryMsg> readSpO2Entries(int startEpochMs, int endEpochMs);
  @async
  SpO2EntryMsg? readLatestSpO2(int startEpochMs, int endEpochMs);
  @async
  List<RespiratoryRateEntryMsg> readRespiratoryRateEntries(int startEpochMs, int endEpochMs);
  @async
  List<BodyTempEntryMsg> readBodyTemperatureEntries(int startEpochMs, int endEpochMs);
  @async
  List<Vo2MaxEntryMsg> readVo2MaxEntries(int startEpochMs, int endEpochMs);
  @async
  Vo2MaxEntryMsg? readLatestVo2Max(int startEpochMs, int endEpochMs);
  @async
  List<BloodGlucoseEntryMsg> readBloodGlucoseEntries(int startEpochMs, int endEpochMs);
  @async
  List<SkinTemperatureEntryMsg> readSkinTemperatureEntries(int startEpochMs, int endEpochMs);
  // Daily-bucketed vitals for long-range charts (see DailyVitalPointMsg). Each
  // averages the day's raw readings on the Kotlin side so a year of records
  // never crosses this channel.
  @async
  List<DailyBloodPressurePointMsg> readDailyBloodPressure(int startEpochMs, int endEpochMs);
  @async
  List<DailyVitalPointMsg> readDailySpO2(int startEpochMs, int endEpochMs);
  @async
  List<DailyVitalPointMsg> readDailyRespiratoryRate(int startEpochMs, int endEpochMs);
  @async
  List<DailyVitalPointMsg> readDailyBodyTemperature(int startEpochMs, int endEpochMs);
  @async
  List<DailyVitalPointMsg> readDailyVo2Max(int startEpochMs, int endEpochMs);
  @async
  List<DailyVitalPointMsg> readDailyBloodGlucose(int startEpochMs, int endEpochMs);
  @async
  List<DailyVitalPointMsg> readDailySkinTemperature(int startEpochMs, int endEpochMs);
  // Latest reading in a window, so a long-range card can show the true newest
  // value/source without loading the raw list. (Blood pressure, SpO2 and VO2max
  // already have theirs above.)
  @async
  RespiratoryRateEntryMsg? readLatestRespiratoryRate(int startEpochMs, int endEpochMs);
  @async
  BodyTempEntryMsg? readLatestBodyTemperature(int startEpochMs, int endEpochMs);
  @async
  BloodGlucoseEntryMsg? readLatestBloodGlucose(int startEpochMs, int endEpochMs);
  @async
  SkinTemperatureEntryMsg? readLatestSkinTemperature(int startEpochMs, int endEpochMs);
  // Changes API for the local daily-aggregate cache: register a token for one
  // record type (by canonical name, e.g. "RespiratoryRate"), then poll changes.
  @async
  String getVitalsChangesToken(String recordType);
  @async
  VitalsChangesMsg getVitalsChanges(String token);
  @async
  String writeVitalsMeasurementEntry(VitalsMeasurementWriteRequestMsg request);
  @async
  VitalsMeasurementEntryMsg? readVitalsMeasurementEntry(
    VitalsMeasurementTypeMsg type,
    String id,
  );
  @async
  void updateVitalsMeasurementEntry(String id, VitalsMeasurementWriteRequestMsg request);
  @async
  void deleteVitalsMeasurementEntry(VitalsMeasurementTypeMsg type, String id);

  // ── Cycle (Phase 4) — read-only ────────────────────────────────────────────

  @async
  List<MenstruationFlowEntryMsg> readMenstruationFlowEntries(int startEpochMs, int endEpochMs);
  @async
  List<MenstruationPeriodEntryMsg> readMenstruationPeriods(int startEpochMs, int endEpochMs);
  @async
  List<OvulationTestEntryMsg> readOvulationTests(int startEpochMs, int endEpochMs);
  @async
  List<CervicalMucusEntryMsg> readCervicalMucusEntries(int startEpochMs, int endEpochMs);
  @async
  List<BasalBodyTemperatureEntryMsg> readBasalBodyTemperatureEntries(int startEpochMs, int endEpochMs);
  @async
  List<IntermenstrualBleedingEntryMsg> readIntermenstrualBleedingEntries(int startEpochMs, int endEpochMs);
  @async
  List<SexualActivityEntryMsg> readSexualActivityEntries(int startEpochMs, int endEpochMs);

  // ── Heart (Phase 5) ────────────────────────────────────────────────────────

  @async
  int? readAvgHeartRate(int startEpochMs, int endEpochMs);
  @async
  List<HeartRateSampleMsg> readRawHeartRateSamples(int startEpochMs, int endEpochMs);
  @async
  List<HeartRateAggBucketMsg> readHeartRateAggregatedBuckets(
    int startEpochMs,
    int endEpochMs,
    int bucketMs,
  );
  @async
  List<HeartRateSummaryMsg> readDailyHeartRateSummaries(int startEpochMs, int endEpochMs);
  @async
  int? readRestingHeartRate(int startEpochMs, int endEpochMs);
  @async
  List<RestingHeartRateSampleMsg> readRestingHeartRateSamples(int startEpochMs, int endEpochMs);
  @async
  List<DailyRestingHRMsg> readDailyRestingHR(int startEpochMs, int endEpochMs);
  @async
  List<HrvSampleMsg> readHrvSamples(int startEpochMs, int endEpochMs);
  @async
  List<DailyHrvMsg> readDailyHRV(int startEpochMs, int endEpochMs);

  // ── Nutrition (Phase 6) ────────────────────────────────────────────────────

  @async
  double? readCaloriesInKcal(int startEpochMs, int endEpochMs);
  @async
  List<DailyNutritionMsg> readDailyNutrition(
    int startEpochMs,
    int endEpochMs,
    bool includeHydration,
    bool includeCalories,
    bool includeEstimatedCalories,
  );
  @async
  List<DailyMacrosMsg> readDailyMacros(int startEpochMs, int endEpochMs);
  @async
  List<NutritionEntryMsg> readNutritionEntries(int startEpochMs, int endEpochMs);
  @async
  String writeNutritionEntry(NutritionWriteRequestMsg request);
  @async
  String? deleteNutritionEntry(String id);
  @async
  void deleteHydrationNutritionEntry(String hydrationClientRecordId);

  // ── Sleep (Phase 7) ────────────────────────────────────────────────────────

  @async
  List<SleepDataMsg> readSleepSessionsRaw(int startEpochMs, int endEpochMs);
  @async
  SleepDataMsg? readSleepSessionById(String id);

  // ── Activity / Exercise (Phase 8) ──────────────────────────────────────────

  @async
  List<ExerciseDataMsg> readExerciseSessions(int startEpochMs, int endEpochMs);

  /// Exercise sessions in the window, each backfilled with the route metrics
  /// that only an aggregate over the session window can produce:
  /// `DistanceRecord.DISTANCE_TOTAL` (when [includeDistance]) and
  /// `SpeedRecord.SPEED_AVG` (when [includeSpeed]). The two flags are the
  /// caller's granted read-distance / read-speed permissions — an ungranted
  /// metric is simply left out of the aggregate (null on the way back), never
  /// an error.
  @async
  List<ExerciseDataMsg> readExerciseSessionsWithMetrics(
    int startEpochMs,
    int endEpochMs,
    bool includeDistance,
    bool includeSpeed,
  );
  @async
  ExerciseDataMsg? readExerciseSessionById(String id);

  /// Every sibling-record total for ONE session's window.
  ///
  /// An `ExerciseSessionRecord` carries none of these: a watch writes its steps,
  /// distance, calories and elevation as separate records covering the same span,
  /// so the only way to attach them to the session is to aggregate over its
  /// window. [readExerciseSessionsWithMetrics] does this for a LIST of sessions
  /// but only for distance and speed; opening one session needs the rest of them.
  ///
  /// [metrics] names the aggregates the caller holds a read permission for — see
  /// `ExerciseSessionMetric.wireName`. An unnamed metric is left out of the
  /// request and comes back null; an unknown name is ignored rather than throwing,
  /// so an older host stays compatible with a newer caller.
  @async
  ExerciseSessionMetricsMsg readExerciseSessionMetrics(
    int startEpochMs,
    int endEpochMs,
    List<String> metrics,
  );

  @async
  List<SpeedSampleMsg> readSpeedSamples(int startEpochMs, int endEpochMs);

  /// Cycling-pedaling and steps cadence samples in the window, merged and
  /// ordered by time.
  @async
  List<ActivityCadenceSampleMsg> readActivityCadenceSamples(
    int startEpochMs,
    int endEpochMs,
  );

  /// Planned (scheduled) exercise sessions overlapping the window.
  @async
  List<PlannedExerciseSessionMsg> readPlannedExerciseSessions(
    int startEpochMs,
    int endEpochMs,
  );

  /// Inserts a planned exercise session, replacing [PlannedExerciseWriteRequestMsg.id]
  /// when supplied. Returns the new record id.
  @async
  String writePlannedExerciseSession(PlannedExerciseWriteRequestMsg request);
  @async
  String writeActivityEntry(ActivityWriteRequestMsg request);

  /// Inserts several activities in a SINGLE Health Connect call, returning one
  /// client record id per request, in order.
  ///
  /// Health Connect charges its rate limit per API CALL, not per record — a
  /// quota failure reads `requested: 1` however many records the call carried.
  /// Writing a folder of route files one call at a time therefore spends a unit
  /// of quota per file and exhausts the daily allowance after a couple of
  /// thousand, which is exactly what a bulk FIT import does. One call carrying
  /// fifty activities costs one unit.
  ///
  /// Insertion is atomic per call: if any record is rejected, NOTHING in the
  /// batch is written. Callers must therefore be prepared to fall back to
  /// single writes to find the file at fault (see [insertImportedRecords],
  /// which the Apple Health import already uses this way).
  @async
  List<String> writeActivityEntries(List<ActivityWriteRequestMsg> requests);

  @async
  void updateActivityEntry(String id, ActivityWriteRequestMsg request);
  @async
  void deleteActivityEntry(String id);

  // ── Apple Health import (Phase 9) ──────────────────────────────────────────

  /// Typed bulk insert of imported records; returns the inserted HC ids.
  @async
  List<String> insertImportedRecords(List<ImportRecordMsg> records);
}
