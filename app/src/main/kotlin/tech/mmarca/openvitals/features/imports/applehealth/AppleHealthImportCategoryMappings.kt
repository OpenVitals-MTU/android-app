package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SleepSessionRecord
import java.util.Locale

internal fun String?.toSleepStageType(): Int? =
    when (this) {
        AppleSleepInBed -> SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED
        AppleSleepAsleep,
        AppleSleepAsleepUnspecified,
        -> SleepSessionRecord.STAGE_TYPE_SLEEPING
        AppleSleepAsleepCore -> SleepSessionRecord.STAGE_TYPE_LIGHT
        AppleSleepAsleepDeep -> SleepSessionRecord.STAGE_TYPE_DEEP
        AppleSleepAsleepRem -> SleepSessionRecord.STAGE_TYPE_REM
        AppleSleepAwake -> SleepSessionRecord.STAGE_TYPE_AWAKE
        else -> null
    }

internal fun String.toExerciseType(): Int {
    val type = removePrefix("HKWorkoutActivityType").lowercase(Locale.US)
    return when {
        "running" in type -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        "cycling" in type || "biking" in type -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
        "walking" in type -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
        "hiking" in type -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
        "wheelchair" in type -> ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
        "rowing" in type -> ExerciseSessionRecord.EXERCISE_TYPE_ROWING
        "paddle" in type || "kayak" in type -> ExerciseSessionRecord.EXERCISE_TYPE_PADDLING
        "ski" in type -> ExerciseSessionRecord.EXERCISE_TYPE_SKIING
        "snowboard" in type -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING
        "snow" in type -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING
        "skating" in type -> ExerciseSessionRecord.EXERCISE_TYPE_SKATING
        "sailing" in type -> ExerciseSessionRecord.EXERCISE_TYPE_SAILING
        "surf" in type -> ExerciseSessionRecord.EXERCISE_TYPE_SURFING
        "swim" in type -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER
        "golf" in type -> ExerciseSessionRecord.EXERCISE_TYPE_GOLF
        "yoga" in type -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
        "pilates" in type -> ExerciseSessionRecord.EXERCISE_TYPE_PILATES
        "elliptical" in type -> ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL
        "strength" in type || "traditionalstrengthtraining" in type -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
        "stair" in type -> ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING
        else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
    }
}

internal fun String?.toMenstrualFlow(): Int =
    when (this) {
        "HKCategoryValueMenstrualFlowLight" -> MenstruationFlowRecord.FLOW_LIGHT
        "HKCategoryValueMenstrualFlowMedium" -> MenstruationFlowRecord.FLOW_MEDIUM
        "HKCategoryValueMenstrualFlowHeavy" -> MenstruationFlowRecord.FLOW_HEAVY
        else -> MenstruationFlowRecord.FLOW_UNKNOWN
    }

internal fun String?.toOvulationResult(): Int =
    when (this) {
        "HKCategoryValueOvulationTestResultPositive" -> OvulationTestRecord.RESULT_POSITIVE
        "HKCategoryValueOvulationTestResultNegative" -> OvulationTestRecord.RESULT_NEGATIVE
        "HKCategoryValueOvulationTestResultLuteinizingHormoneSurge" -> OvulationTestRecord.RESULT_HIGH
        else -> OvulationTestRecord.RESULT_INCONCLUSIVE
    }

internal fun String?.toCervicalMucusAppearance(): Int =
    when (this) {
        "HKCategoryValueCervicalMucusQualityDry" -> CervicalMucusRecord.APPEARANCE_DRY
        "HKCategoryValueCervicalMucusQualitySticky" -> CervicalMucusRecord.APPEARANCE_STICKY
        "HKCategoryValueCervicalMucusQualityCreamy" -> CervicalMucusRecord.APPEARANCE_CREAMY
        "HKCategoryValueCervicalMucusQualityWatery" -> CervicalMucusRecord.APPEARANCE_WATERY
        "HKCategoryValueCervicalMucusQualityEggWhite" -> CervicalMucusRecord.APPEARANCE_EGG_WHITE
        else -> CervicalMucusRecord.APPEARANCE_UNKNOWN
    }

internal fun Map<String, String>.toProtectionUsed(): Int {
    val value = this["HKSexualActivityProtectionUsed"] ?: this["HKMetadataKeySexualActivityProtectionUsed"]
    return when (value?.lowercase(Locale.US)) {
        "true", "1", "yes" -> SexualActivityRecord.PROTECTION_USED_PROTECTED
        "false", "0", "no" -> SexualActivityRecord.PROTECTION_USED_UNPROTECTED
        else -> SexualActivityRecord.PROTECTION_USED_UNKNOWN
    }
}
