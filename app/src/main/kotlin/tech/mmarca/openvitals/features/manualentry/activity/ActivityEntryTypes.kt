package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import androidx.annotation.StringRes
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import tech.mmarca.openvitals.R

enum class ActivityRecordingSensor {
    NONE,
    GPS,
    PROXIMITY,
    ACCELEROMETER,
    STEP_DETECTOR,
}

enum class ActivityRepetitionUnit {
    REPETITIONS,
    STEPS,
}

data class ActivityEntryType(
    val exerciseType: Int,
    val id: String = exerciseType.toString(),
    @param:StringRes val labelRes: Int,
    val supportsGpsRoute: Boolean = true,
    val supportsDistance: Boolean = true,
    val supportsElevation: Boolean = false,
    val recordingSensor: ActivityRecordingSensor = if (supportsGpsRoute) {
        ActivityRecordingSensor.GPS
    } else {
        ActivityRecordingSensor.NONE
    },
    val segmentType: Int? = null,
    val defaultTitle: String? = null,
    val repetitionUnit: ActivityRepetitionUnit? = null,
)

val ActivityEntryType.supportsLiveRecording: Boolean
    get() = recordingSensor != ActivityRecordingSensor.NONE

val ActivityEntryType.supportsSetRepetitions: Boolean
    get() = repetitionUnit == ActivityRepetitionUnit.REPETITIONS

val ActivityEntryType.isRepetitionLike: Boolean
    get() = repetitionUnit != null

fun activityEntryTypeById(id: String?): ActivityEntryType? =
    id?.let { typeId -> DefaultActivityEntryTypes.firstOrNull { it.id == typeId } }

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
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
        id = "treadmill",
        labelRes = R.string.exercise_type_treadmill,
        supportsGpsRoute = false,
        supportsDistance = true,
        recordingSensor = ActivityRecordingSensor.STEP_DETECTOR,
        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL,
        repetitionUnit = ActivityRepetitionUnit.STEPS,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        id = "push_ups",
        labelRes = R.string.exercise_type_push_ups,
        supportsGpsRoute = false,
        supportsDistance = false,
        recordingSensor = ActivityRecordingSensor.PROXIMITY,
        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
        defaultTitle = "Push-ups",
        repetitionUnit = ActivityRepetitionUnit.REPETITIONS,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        id = "pull_ups",
        labelRes = R.string.exercise_type_pull_ups,
        supportsGpsRoute = false,
        supportsDistance = false,
        recordingSensor = ActivityRecordingSensor.ACCELEROMETER,
        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP,
        repetitionUnit = ActivityRepetitionUnit.REPETITIONS,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        id = "rope_skipping",
        labelRes = R.string.exercise_type_rope_skipping,
        supportsGpsRoute = false,
        supportsDistance = false,
        recordingSensor = ActivityRecordingSensor.ACCELEROMETER,
        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMP_ROPE,
        repetitionUnit = ActivityRepetitionUnit.REPETITIONS,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS,
        id = "trampoline_jumping",
        labelRes = R.string.exercise_type_trampoline_jumping,
        supportsGpsRoute = false,
        supportsDistance = false,
        recordingSensor = ActivityRecordingSensor.ACCELEROMETER,
        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
        defaultTitle = "Trampoline jumping",
        repetitionUnit = ActivityRepetitionUnit.REPETITIONS,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
        labelRes = R.string.exercise_type_other_workout,
        supportsGpsRoute = false,
        supportsDistance = false,
    ),
)
