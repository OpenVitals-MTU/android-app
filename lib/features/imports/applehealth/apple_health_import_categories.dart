/// Category classification for converted and pre-analysis records, ported from
/// the Kotlin `AppleHealthImportCategories.kt`.
library;

import 'apple_health_import_models.dart';
import 'apple_health_import_records.dart';
import 'apple_health_import_types.dart';

AppleHealthImportCategory importCategory(ConvertedAppleRecord converted) {
  switch (converted.targetType) {
    case 'ExerciseSessionRecord':
      return AppleHealthImportCategory.workouts;
    case 'StepsRecord':
    case 'DistanceRecord':
    case 'ActiveCaloriesBurnedRecord':
    case 'TotalCaloriesBurnedRecord':
    case 'BasalMetabolicRateRecord':
    case 'FloorsClimbedRecord':
    case 'ElevationGainedRecord':
    case 'WheelchairPushesRecord':
    case 'SpeedRecord':
      return AppleHealthImportCategory.activity;
    case 'HeartRateRecord':
    case 'RestingHeartRateRecord':
    case 'HeartRateVariabilityRmssdRecord':
      return AppleHealthImportCategory.heart;
    case 'SleepSessionRecord':
      return AppleHealthImportCategory.sleep;
    case 'WeightRecord':
    case 'HeightRecord':
    case 'BodyFatRecord':
    case 'LeanBodyMassRecord':
    case 'BoneMassRecord':
    case 'BodyWaterMassRecord':
      return AppleHealthImportCategory.body;
    case 'BloodPressureRecord':
    case 'OxygenSaturationRecord':
    case 'RespiratoryRateRecord':
    case 'BodyTemperatureRecord':
    case 'BloodGlucoseRecord':
    case 'Vo2MaxRecord':
      return AppleHealthImportCategory.vitals;
    case 'NutritionRecord':
      return AppleHealthImportCategory.nutrition;
    case 'HydrationRecord':
      return AppleHealthImportCategory.hydration;
    case 'MindfulnessSessionRecord':
      return AppleHealthImportCategory.mindfulness;
    case 'MenstruationFlowRecord':
    case 'OvulationTestRecord':
    case 'CervicalMucusRecord':
    case 'BasalBodyTemperatureRecord':
    case 'IntermenstrualBleedingRecord':
    case 'SexualActivityRecord':
      return AppleHealthImportCategory.cycle;
    default:
      return AppleHealthImportCategory.activity;
  }
}

bool convertedHasExerciseRoute(ConvertedAppleRecord converted) {
  final record = converted.record;
  return record is ExerciseSessionImportRecord && record.hasRoute;
}

/// Category prediction used by the pre-import analysis scan, keyed by Apple
/// source type. Must stay in sync with [importCategory].
AppleHealthImportCategory? analysisCategory(
  AppleRecord record,
  bool mindfulnessAvailable,
) =>
    analysisCategoryForType(record.type, mindfulnessAvailable);

/// The same classification from a bare Apple type string, so the importer can
/// decide a record's category at the SAX boundary — before any [AppleRecord] is
/// built (Kotlin `String.analysisCategory`).
AppleHealthImportCategory? analysisCategoryForType(
  String type,
  bool mindfulnessAvailable,
) {
  switch (type) {
    case appleStepCount:
    case appleDistanceWalkingRunning:
    case appleDistanceCycling:
    case appleDistanceSwimming:
    case appleDistanceWheelchair:
    case appleActiveEnergyBurned:
    case appleBasalEnergyBurned:
    case appleFlightsClimbed:
    case appleElevationAscended:
    case applePushCount:
    case appleWalkingSpeed:
      return AppleHealthImportCategory.activity;
    case appleHeartRate:
    case appleRestingHeartRate:
      return AppleHealthImportCategory.heart;
    case appleSleepAnalysis:
      return AppleHealthImportCategory.sleep;
    case appleBodyMass:
    case appleHeight:
    case appleBodyFatPercentage:
    case appleLeanBodyMass:
    case appleBoneMass:
    case appleBodyWaterMass:
      return AppleHealthImportCategory.body;
    case appleBloodPressureSystolic:
    case appleBloodPressureDiastolic:
    case appleOxygenSaturation:
    case appleRespiratoryRate:
    case appleBodyTemperature:
    case appleBloodGlucose:
    case appleVo2Max:
      return AppleHealthImportCategory.vitals;
    case appleBasalBodyTemperature:
      return AppleHealthImportCategory.cycle;
    case appleDietaryWater:
      return AppleHealthImportCategory.hydration;
    case appleMindfulSession:
      return mindfulnessAvailable
          ? AppleHealthImportCategory.mindfulness
          : null;
  }
  if (appleNutritionTypes.contains(type)) {
    return AppleHealthImportCategory.nutrition;
  }
  if (appleCycleCategoryTypes.contains(type)) {
    return AppleHealthImportCategory.cycle;
  }
  return null;
}
