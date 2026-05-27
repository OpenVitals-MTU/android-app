package tech.mmarca.openvitals.features.manualentry

import androidx.annotation.StringRes
import androidx.health.connect.client.records.ExerciseSessionRecord
import tech.mmarca.openvitals.R

data class ActivityEntryType(
    val exerciseType: Int,
    @param:StringRes val labelRes: Int,
    val supportsGpsRoute: Boolean = true,
    val supportsDistance: Boolean = true,
    val supportsElevation: Boolean = false,
)

val DefaultActivityEntryTypes: List<ActivityEntryType> = listOf(
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        labelRes = R.string.exercise_type_running,
        supportsElevation = true,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        labelRes = R.string.exercise_type_biking,
        supportsElevation = true,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        labelRes = R.string.exercise_type_walking,
        supportsElevation = true,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
        labelRes = R.string.exercise_type_hiking,
        supportsElevation = true,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR,
        labelRes = R.string.exercise_type_wheelchair,
        supportsElevation = true,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
        labelRes = R.string.exercise_type_rowing,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_PADDLING,
        labelRes = R.string.exercise_type_paddling,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_SKIING,
        labelRes = R.string.exercise_type_skiing,
        supportsElevation = true,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING,
        labelRes = R.string.exercise_type_snowboarding,
        supportsElevation = true,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING,
        labelRes = R.string.exercise_type_snowshoeing,
        supportsElevation = true,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_SKATING,
        labelRes = R.string.exercise_type_skating,
        supportsElevation = true,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_SAILING,
        labelRes = R.string.exercise_type_sailing,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_SURFING,
        labelRes = R.string.exercise_type_surfing,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
        labelRes = R.string.exercise_type_swimming_open_water,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_GOLF,
        labelRes = R.string.exercise_type_golf,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
        labelRes = R.string.exercise_type_other_workout,
        supportsGpsRoute = false,
        supportsDistance = false,
    ),
)
