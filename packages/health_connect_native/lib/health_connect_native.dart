/// Flutter bridge to the native AndroidX Health Connect Kotlin client.
///
/// This library re-exports the Pigeon-generated [HealthConnectHostApi] as the
/// plugin's public API. Records cross the bridge as JSON strings; the canonical
/// JSON schema is documented below so the Kotlin host (Stage 2) and the Dart
/// data source (Stage 3) agree on one wire format.
///
/// ANDROID-ONLY: on non-Android platforms the underlying platform channel has
/// no host implementation and calls will throw. Guard usage with
/// `defaultTargetPlatform == TargetPlatform.android` (or feature detection via
/// [HealthConnectHostApi.getSdkStatus]).
library;

import 'src/messages.g.dart';

// Export the whole generated surface: [HealthConnectHostApi] plus the typed
// domain message classes/enums (the `*Msg` types) the data source maps to the
// app's freezed models. (Pigeon's codec is private and not re-exported.)
export 'src/messages.g.dart';

/// Thin, app-facing client over the generated Pigeon host API.
///
/// Stage 1 keeps this a minimal wrapper: it simply owns a
/// [HealthConnectHostApi] instance ([api]) that callers use directly. Later
/// stages may add convenience helpers (typed record models, retry/paging, etc.)
/// on top without changing the Pigeon contract.
class HealthConnectNative {
  /// Creates a client, optionally injecting a custom [HealthConnectHostApi]
  /// (e.g. a fake in tests).
  HealthConnectNative({HealthConnectHostApi? api})
    : api = api ?? HealthConnectHostApi();

  /// The generated Pigeon host API bound to the default binary messenger.
  final HealthConnectHostApi api;
}

// =============================================================================
// CANONICAL RECORD JSON SCHEMA
// =============================================================================
//
// Every record is a single JSON object. Keys use camelCase. Times are epoch
// milliseconds (UTC); zone offsets are seconds east of UTC. Enum-like fields are
// carried as the Health Connect integer constants (Stage 2/3 map to/from typed
// enums). Fields marked `?` are optional/nullable.
//
// -----------------------------------------------------------------------------
// Common keys (all record types)
// -----------------------------------------------------------------------------
//   recordType             String   One of the RecordType values listed below.
//   id                     String   Health Connect record id (present on reads).
//   clientRecordId         String?  App-assigned stable id (dedup / upsert key).
//   clientRecordVersion    int?     App-assigned monotonically increasing version.
//   dataOriginPackage      String   Source app package (present on reads).
//   lastModifiedEpochMs    int?     Health Connect last-modified time (reads).
//   recordingMethod        int?     RecordingMethod constant (manual/automatic/...).
//   device                 object?  { type:int, manufacturer?:String, model?:String }
//
// Interval records additionally carry:
//   startEpochMs           int
//   endEpochMs             int
//   startZoneOffsetSeconds int?
//   endZoneOffsetSeconds   int?
//
// Instant records additionally carry:
//   timeEpochMs            int
//   zoneOffsetSeconds      int?
//
// -----------------------------------------------------------------------------
// RecordType values
// -----------------------------------------------------------------------------
//   "Steps", "Distance", "ActiveCaloriesBurned", "TotalCaloriesBurned",
//   "FloorsClimbed", "ElevationGained", "WheelchairPushes", "ExerciseSession",
//   "Speed", "StepsCadence", "CyclingPedalingCadence", "Power", "Sleep",
//   "HeartRate", "RestingHeartRate", "HeartRateVariabilityRmssd", "Weight",
//   "Height", "BodyFat", "LeanBodyMass", "BoneMass", "BodyWaterMass",
//   "BasalMetabolicRate", "Hydration", "Nutrition", "MindfulnessSession",
//   "MenstruationFlow", "MenstruationPeriod", "OvulationTest", "CervicalMucus",
//   "BasalBodyTemperature", "IntermenstrualBleeding", "SexualActivity",
//   "BloodPressure", "OxygenSaturation", "RespiratoryRate", "BodyTemperature",
//   "SkinTemperature", "BloodGlucose", "Vo2Max"
//
// -----------------------------------------------------------------------------
// Type-specific fields
// -----------------------------------------------------------------------------
// Activity (interval unless noted):
//   Steps:                 count:int
//   Distance:              distanceMeters:double
//   ActiveCaloriesBurned:  energyKcal:double
//   TotalCaloriesBurned:   energyKcal:double
//   FloorsClimbed:         floors:double
//   ElevationGained:       elevationMeters:double
//   WheelchairPushes:      count:int
//   ExerciseSession:
//       exerciseType:int, title?:String, notes?:String,
//       plannedExerciseSessionId?:String,
//       segments:[ { startEpochMs:int, endEpochMs:int, segmentType:int,
//                    repetitions:int } ],
//       laps:[ { startEpochMs:int, endEpochMs:int, lengthMeters?:double } ],
//       route:{ points:[ { timeEpochMs:int, latitude:double, longitude:double,
//                          altitudeMeters?:double, horizontalAccuracyMeters?:double,
//                          verticalAccuracyMeters?:double } ] }?
//   Speed (interval series):
//       samples:[ { timeEpochMs:int, metersPerSecond:double } ]
//   StepsCadence (interval series):
//       samples:[ { timeEpochMs:int, rate:double } ]
//   CyclingPedalingCadence (interval series):
//       samples:[ { timeEpochMs:int, revolutionsPerMinute:double } ]
//   Power (interval series):
//       samples:[ { timeEpochMs:int, watts:double } ]
//
// Sleep (interval):
//   Sleep:  title?:String, notes?:String,
//           stages:[ { startEpochMs:int, endEpochMs:int, stage:int } ]
//
// Heart:
//   HeartRate (interval series):
//           samples:[ { timeEpochMs:int, bpm:int } ]
//   RestingHeartRate (instant):        bpm:int
//   HeartRateVariabilityRmssd (instant): rmssdMs:double
//
// Body (instant unless noted):
//   Weight:               weightKg:double
//   Height:               heightMeters:double
//   BodyFat:              percentage:double
//   LeanBodyMass:         massKg:double
//   BoneMass:             massKg:double
//   BodyWaterMass:        massKg:double
//   BasalMetabolicRate:   kcalPerDay:double
//   OxygenSaturation:     percentage:double
//   Vo2Max:               vo2MillilitersPerMinuteKilogram:double,
//                         measurementMethod?:int
//
// Nutrition / Hydration (interval):
//   Hydration:            volumeLiters:double
//   Nutrition:            name?:String, mealType?:int,
//                         energyKcal?:double, protein?:double,
//                         totalCarbohydrate?:double, totalFat?:double,
//                         saturatedFat?:double, sugar?:double, fiber?:double,
//                         sodium?:double, cholesterol?:double, potassium?:double,
//                         calcium?:double, iron?:double, ... (all HC nutrients,
//                         grams unless the HC field is a mass/quantity)
//
// Mindfulness (interval):
//   MindfulnessSession:   mindfulnessSessionType?:int, title?:String, notes?:String
//
// Cycle tracking (enums are HC int constants):
//   MenstruationFlow (instant):        flow:int
//   MenstruationPeriod (interval):     (interval only)
//   OvulationTest (instant):           result:int
//   CervicalMucus (instant):           appearance:int, sensation:int
//   IntermenstrualBleeding (instant):  (instant only)
//   SexualActivity (instant):          protectionUsed:int
//   BasalBodyTemperature (instant):    temperatureCelsius:double,
//                                      measurementLocation?:int
//
// Vitals:
//   BloodPressure (instant):
//           systolicMmHg:double, diastolicMmHg:double,
//           bodyPosition?:int, measurementLocation?:int
//   RespiratoryRate (instant):         rate:double
//   BodyTemperature (instant):         temperatureCelsius:double,
//                                      measurementLocation?:int
//   SkinTemperature (interval):        baselineCelsius?:double, measurementLocation?:int,
//           deltas:[ { timeEpochMs:int, deltaCelsius:double } ]
//   BloodGlucose (instant):
//           levelMmolL:double, specimenSource?:int, mealType?:int,
//           relationToMeal?:int
//
// =============================================================================
