/// HKCategoryType value → domain enum mappings, ported 1:1 from the Kotlin
/// `AppleHealthImportCategoryMappings.kt`.
library;

import '../../../domain/model/apple_health_import_records.dart';
import 'apple_health_import_types.dart';

SleepStageType? mapSleepStageType(String? value) {
  switch (value) {
    case appleSleepInBed:
      return SleepStageType.awakeInBed;
    case appleSleepAsleep:
    case appleSleepAsleepUnspecified:
      return SleepStageType.sleeping;
    case appleSleepAsleepCore:
      return SleepStageType.light;
    case appleSleepAsleepDeep:
      return SleepStageType.deep;
    case appleSleepAsleepRem:
      return SleepStageType.rem;
    case appleSleepAwake:
      return SleepStageType.awake;
    default:
      return null;
  }
}

ImportExerciseType mapWorkoutActivityTypeToExerciseType(String rawType) {
  final type = rawType.replaceFirst('HKWorkoutActivityType', '').toLowerCase();
  bool has(String needle) => type.contains(needle);
  if (has('running')) return ImportExerciseType.running;
  if (has('cycling') || has('biking')) return ImportExerciseType.biking;
  if (has('walking')) return ImportExerciseType.walking;
  if (has('hiking')) return ImportExerciseType.hiking;
  if (has('wheelchair')) return ImportExerciseType.wheelchair;
  if (has('rowing')) return ImportExerciseType.rowing;
  if (has('paddle') || has('kayak')) return ImportExerciseType.paddling;
  if (has('ski')) return ImportExerciseType.skiing;
  if (has('snowboard')) return ImportExerciseType.snowboarding;
  if (has('snow')) return ImportExerciseType.snowshoeing;
  if (has('skating')) return ImportExerciseType.skating;
  if (has('sailing')) return ImportExerciseType.sailing;
  if (has('surf')) return ImportExerciseType.surfing;
  if (has('swim')) return ImportExerciseType.swimmingOpenWater;
  if (has('golf')) return ImportExerciseType.golf;
  if (has('yoga')) return ImportExerciseType.yoga;
  if (has('pilates')) return ImportExerciseType.pilates;
  if (has('elliptical')) return ImportExerciseType.elliptical;
  if (has('strength') || has('traditionalstrengthtraining')) {
    return ImportExerciseType.strengthTraining;
  }
  if (has('stair')) return ImportExerciseType.stairClimbing;
  return ImportExerciseType.otherWorkout;
}

MenstruationFlowType mapMenstrualFlow(String? value) {
  switch (value) {
    case 'HKCategoryValueMenstrualFlowLight':
      return MenstruationFlowType.light;
    case 'HKCategoryValueMenstrualFlowMedium':
      return MenstruationFlowType.medium;
    case 'HKCategoryValueMenstrualFlowHeavy':
      return MenstruationFlowType.heavy;
    default:
      return MenstruationFlowType.unknown;
  }
}

OvulationResultType mapOvulationResult(String? value) {
  switch (value) {
    case 'HKCategoryValueOvulationTestResultPositive':
      return OvulationResultType.positive;
    case 'HKCategoryValueOvulationTestResultNegative':
      return OvulationResultType.negative;
    case 'HKCategoryValueOvulationTestResultLuteinizingHormoneSurge':
      return OvulationResultType.high;
    default:
      return OvulationResultType.inconclusive;
  }
}

CervicalMucusAppearance mapCervicalMucusAppearance(String? value) {
  switch (value) {
    case 'HKCategoryValueCervicalMucusQualityDry':
      return CervicalMucusAppearance.dry;
    case 'HKCategoryValueCervicalMucusQualitySticky':
      return CervicalMucusAppearance.sticky;
    case 'HKCategoryValueCervicalMucusQualityCreamy':
      return CervicalMucusAppearance.creamy;
    case 'HKCategoryValueCervicalMucusQualityWatery':
      return CervicalMucusAppearance.watery;
    case 'HKCategoryValueCervicalMucusQualityEggWhite':
      return CervicalMucusAppearance.eggWhite;
    default:
      return CervicalMucusAppearance.unknown;
  }
}

SexualActivityProtection mapProtectionUsed(Map<String, String> metadata) {
  final value = metadata['HKSexualActivityProtectionUsed'] ??
      metadata['HKMetadataKeySexualActivityProtectionUsed'];
  switch (value?.toLowerCase()) {
    case 'true':
    case '1':
    case 'yes':
      return SexualActivityProtection.protected;
    case 'false':
    case '0':
    case 'no':
      return SexualActivityProtection.unprotected;
    default:
      return SexualActivityProtection.unknown;
  }
}
