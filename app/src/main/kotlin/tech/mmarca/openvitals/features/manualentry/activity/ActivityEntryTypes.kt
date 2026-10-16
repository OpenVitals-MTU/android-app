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
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.activeSteps

enum class ActivityRecordingSensor {
    NONE,
    GPS,
    BLE,
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

val ActivityEntryType.supportsStepCounting: Boolean
    get() = repetitionUnit == ActivityRepetitionUnit.STEPS

/** Generic set types (calisthenics, strength sets): every step picks its own exercise. */
val ActivityEntryType.supportsMixedExercises: Boolean
    get() = supportsSetRepetitions && recordingSensor == ActivityRecordingSensor.NONE

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
    // A trainer ride has a distance and no route; FIT names it (`sub_sport` 5/6).
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
        id = "stationary_bike",
        labelRes = R.string.exercise_type_stationary_bike,
        supportsGpsRoute = false,
        supportsDistance = true,
        recordingSensor = ActivityRecordingSensor.BLE,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        labelRes = R.string.exercise_type_walking,
        supportsElevation = true,
        repetitionUnit = ActivityRepetitionUnit.STEPS,
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
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        labelRes = R.string.exercise_type_strength_training,
        supportsGpsRoute = false,
        supportsDistance = false,
        recordingSensor = ActivityRecordingSensor.BLE,
    ),
    // Generic set sessions for plans that mix exercises. Manual-entry and plan only.
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        id = "strength_sets",
        labelRes = R.string.exercise_type_strength_sets,
        supportsGpsRoute = false,
        supportsDistance = false,
        recordingSensor = ActivityRecordingSensor.NONE,
        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
        repetitionUnit = ActivityRepetitionUnit.REPETITIONS,
    ),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        id = "calisthenics",
        labelRes = R.string.exercise_type_calisthenics,
        supportsGpsRoute = false,
        supportsDistance = false,
        recordingSensor = ActivityRecordingSensor.NONE,
        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
        repetitionUnit = ActivityRepetitionUnit.REPETITIONS,
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
    // Squats have their own segment. Same recognizer as push-ups.
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        id = "squats",
        labelRes = R.string.hc_segment_squat,
        supportsGpsRoute = false,
        supportsDistance = false,
        recordingSensor = ActivityRecordingSensor.PROXIMITY,
        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT,
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
    // The rest of Health Connect's session types, so the form can write every type
    // the detail screen can show. Alphabetical by the English label; the curated
    // block above stays first. Each is one of three shapes.
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON, R.string.hc_exercise_type_badminton),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL, R.string.hc_exercise_type_baseball),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL, R.string.hc_exercise_type_basketball),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP, R.string.hc_exercise_type_boot_camp),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_BOXING, R.string.hc_exercise_type_boxing),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_CRICKET, R.string.hc_exercise_type_cricket),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_DANCING, R.string.hc_exercise_type_dancing),
    indoorDistanceActivity(ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL, R.string.hc_exercise_type_elliptical),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS, R.string.hc_exercise_type_exercise_class),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_FENCING, R.string.hc_exercise_type_fencing),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN, R.string.hc_exercise_type_football_american),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN, R.string.hc_exercise_type_football_australian),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC, R.string.hc_exercise_type_frisbee_disc),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING, R.string.hc_exercise_type_guided_breathing),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS, R.string.hc_exercise_type_gymnastics),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL, R.string.hc_exercise_type_handball),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING, R.string.hc_exercise_type_high_intensity_interval_training),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY, R.string.hc_exercise_type_ice_hockey),
    routeActivity(ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING, R.string.hc_exercise_type_ice_skating),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS, R.string.hc_exercise_type_martial_arts),
    routeActivity(ExerciseSessionRecord.EXERCISE_TYPE_PARAGLIDING, R.string.hc_exercise_type_paragliding, supportsElevation = true),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_PILATES, R.string.hc_exercise_type_pilates),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL, R.string.hc_exercise_type_racquetball),
    routeActivity(ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING, R.string.hc_exercise_type_rock_climbing, supportsElevation = true),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY, R.string.hc_exercise_type_roller_hockey),
    indoorDistanceActivity(ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE, R.string.hc_exercise_type_rowing_machine),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_RUGBY, R.string.hc_exercise_type_rugby),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING, R.string.hc_exercise_type_scuba_diving),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_SOCCER, R.string.hc_exercise_type_soccer),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL, R.string.hc_exercise_type_softball),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_SQUASH, R.string.hc_exercise_type_squash),
    indoorDistanceActivity(ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING, R.string.hc_exercise_type_stair_climbing, supportsElevation = true),
    indoorDistanceActivity(ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE, R.string.hc_exercise_type_stair_climbing_machine),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING, R.string.hc_exercise_type_stretching),
    indoorDistanceActivity(ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL, R.string.hc_exercise_type_swimming_pool),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS, R.string.hc_exercise_type_table_tennis),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_TENNIS, R.string.hc_exercise_type_tennis),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL, R.string.hc_exercise_type_volleyball),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO, R.string.hc_exercise_type_water_polo),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING, R.string.hc_exercise_type_weightlifting),
    timedActivity(ExerciseSessionRecord.EXERCISE_TYPE_YOGA, R.string.hc_exercise_type_yoga),
    ActivityEntryType(
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
        labelRes = R.string.exercise_type_other_workout,
        supportsGpsRoute = false,
        supportsDistance = false,
    ),
)

/** Travels outdoors: a GPS route, a distance, a live recording. */
private fun routeActivity(
    exerciseType: Int,
    @StringRes labelRes: Int,
    supportsElevation: Boolean = false,
): ActivityEntryType = ActivityEntryType(
    exerciseType = exerciseType,
    labelRes = labelRes,
    supportsElevation = supportsElevation,
)

/** A machine or a pool: a distance but no route; recorded as a timed session with a strap. */
private fun indoorDistanceActivity(
    exerciseType: Int,
    @StringRes labelRes: Int,
    supportsElevation: Boolean = false,
): ActivityEntryType = ActivityEntryType(
    exerciseType = exerciseType,
    labelRes = labelRes,
    supportsGpsRoute = false,
    supportsDistance = true,
    supportsElevation = supportsElevation,
    recordingSensor = ActivityRecordingSensor.BLE,
)

/** A timed session with an optional heart rate strap, like strength training. */
private fun timedActivity(
    exerciseType: Int,
    @StringRes labelRes: Int,
): ActivityEntryType = ActivityEntryType(
    exerciseType = exerciseType,
    labelRes = labelRes,
    supportsGpsRoute = false,
    supportsDistance = false,
    recordingSensor = ActivityRecordingSensor.BLE,
)

/**
 * The entry type a plan opens as: a generic set session for mixed plans, or
 * the exercise's own type when one claims the segment.
 */
fun PlannedExerciseData.toActivityEntryType(): ActivityEntryType? {
    val activeSteps = activeSteps()
    if (activeSteps.map { it.exerciseType }.distinct().size > 1) {
        return genericSetActivityType(exerciseType)
    }
    val activeSegmentType = activeSteps
        .firstOrNull { it.completion is PlannedExerciseCompletion.Repetitions }
        ?.exerciseType
    val candidates = DefaultActivityEntryTypes.sortedBy { it.recordingSensor == ActivityRecordingSensor.NONE }
    return candidates.firstOrNull { type ->
        type.exerciseType == exerciseType && type.segmentType != null && type.segmentType == activeSegmentType
    } ?: candidates.firstOrNull { type ->
        type.exerciseType == exerciseType && type.supportsSetRepetitions
    } ?: candidates.firstOrNull { type ->
        type.exerciseType == exerciseType
    }
}

/** The manual-only set type for a session type; strength sets when the type has no generic entry. */
fun genericSetActivityType(exerciseType: Int): ActivityEntryType =
    DefaultActivityEntryTypes.firstOrNull { type ->
        type.exerciseType == exerciseType && type.supportsMixedExercises
    } ?: DefaultActivityEntryTypes.first { it.id == "strength_sets" }
