package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.ZoneOffset
import java.time.LocalDate

data class ExerciseData(
    val id: String,
    val title: String?,
    val exerciseType: Int,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long,
    val source: String,
    val totalDistanceMeters: Double? = null,
    val totalCaloriesKcal: Double? = null,
    val activeCaloriesKcal: Double? = null,
    val steps: Long? = null,
    val wheelchairPushes: Long? = null,
    val averageSpeedMetersPerSecond: Double? = null,
    val averagePowerWatts: Double? = null,
    val averageStepsCadenceRate: Double? = null,
    val averageCyclingCadenceRpm: Double? = null,
    val averageHeartRateBpm: Long? = null,
    val floorsClimbed: Int? = null,
    val elevationGainedMeters: Double? = null,
    val notes: String? = null,
    val startZoneOffset: ZoneOffset? = null,
    val endZoneOffset: ZoneOffset? = null,
    val lastModifiedTime: Instant? = null,
    val clientRecordId: String? = null,
    val clientRecordVersion: Long? = null,
    val recordingMethod: Int? = null,
    val device: ExerciseDeviceData? = null,
    val plannedExerciseSessionId: String? = null,
    val segments: List<ExerciseSegmentData> = emptyList(),
    val laps: List<ExerciseLapData> = emptyList(),
    val route: ExerciseRouteData = ExerciseRouteData(),
    val isOpenVitalsEntry: Boolean = false,
    val totalCaloriesSource: CaloriesBurnedSource = if (totalCaloriesKcal != null) {
        CaloriesBurnedSource.RECORDED_TOTAL
    } else {
        CaloriesBurnedSource.NO_DATA
    },
) {
    val durationMinutes: Long get() = durationMs / 60_000
}

data class ExerciseDeviceData(
    val type: Int,
    val manufacturer: String?,
    val model: String?,
)

data class ExerciseSegmentData(
    val startTime: Instant,
    val endTime: Instant,
    val segmentType: Int,
    val repetitions: Int,
    val setIndex: Int? = null,
    /** Load lifted in this set, when recorded. */
    val weightKg: Double? = null,
) {
    val durationMs: Long get() = endTime.toEpochMilli() - startTime.toEpochMilli()
}

data class ExerciseLapData(
    val startTime: Instant,
    val endTime: Instant,
    val lengthMeters: Double?,
) {
    val durationMs: Long get() = endTime.toEpochMilli() - startTime.toEpochMilli()
}

data class ActivityRecordingLap(
    val startTime: Instant,
    val endTime: Instant,
    val distanceMeters: Double?,
) {
    val durationMs: Long get() = endTime.toEpochMilli() - startTime.toEpochMilli()
}

data class ActivityRecordingMarker(
    val id: String,
    val time: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val name: String,
    val note: String = "",
    val type: String = ActivityRecordingMarkerType.Generic.value,
)

enum class ActivityRecordingMarkerType(val value: String) {
    Generic("generic"),
}

data class ExerciseRouteData(
    val status: ExerciseRouteStatus = ExerciseRouteStatus.NO_DATA,
    val points: List<ExerciseRoutePoint> = emptyList(),
)

enum class ExerciseRouteStatus {
    DATA,
    CONSENT_REQUIRED,
    NO_DATA,
}

data class ExerciseRoutePoint(
    val time: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val horizontalAccuracyMeters: Double?,
    val verticalAccuracyMeters: Double?,
)

data class ActivityPauseInterval(
    val startTime: Instant,
    val endTime: Instant,
)

data class ActivityExerciseSegmentWrite(
    val startTime: Instant,
    val endTime: Instant,
    val segmentType: Int,
    val repetitions: Int = 0,
    val setIndex: Int? = null,
    val weightKg: Double? = null,
)

/** Where an activity's data came from. Decides the recording method Health Connect stores. */
enum class ActivityRecordSource {
    /** Typed in, or recorded by this app on the phone. */
    MANUAL_ENTRY,

    /** A file a paired watch recorded. */
    WATCH,

    /** A file the user picked. Some device recorded it; which one is unknown. */
    FILE,
}

/** A total the activity form owns. Each is stored as one record beside the session. */
enum class ActivityFormMetric { STEPS, DISTANCE, ELEVATION, ACTIVE_CALORIES, TOTAL_CALORIES }

/**
 * What OpenVitals itself stored for one session. Null means it stored nothing for that
 * total, whatever other apps recorded over the same minutes.
 */
data class OwnActivityMetrics(
    val steps: Long? = null,
    val distanceMeters: Double? = null,
    val elevationGainedMeters: Double? = null,
    val activeCaloriesKcal: Double? = null,
    val totalCaloriesKcal: Double? = null,
)

data class ActivityWriteRequest(
    val exerciseType: Int,
    val startTime: Instant,
    val endTime: Instant,
    val title: String? = null,
    val notes: String? = null,
    val plannedExerciseSessionId: String? = null,
    val routePoints: List<ExerciseRoutePoint> = emptyList(),
    val pauseIntervals: List<ActivityPauseInterval> = emptyList(),
    val laps: List<ExerciseLapData> = emptyList(),
    val exerciseSegments: List<ActivityExerciseSegmentWrite> = emptyList(),
    val stepsCount: Long? = null,
    val distanceMeters: Double? = null,
    val elevationGainedMeters: Double? = null,
    val activeCaloriesKcal: Double? = null,
    val totalCaloriesKcal: Double? = null,
    val bleSamples: BleRecordingSampleBuffer = BleRecordingSampleBuffer(),
    val source: ActivityRecordSource = ActivityRecordSource.MANUAL_ENTRY,
    /**
     * A stable key for an imported file. With it every client record id is a function of the
     * file, so importing the file again updates the records instead of adding a second set.
     */
    val importKey: String? = null,
    /**
     * For an edit: the totals the user changed. Every other total keeps the value OpenVitals
     * stored, including one the form does not show for this activity type. Null means every
     * total in this request is a decision, as for a new entry.
     */
    val editedMetrics: Set<ActivityFormMetric>? = null,
) {
    /** This request with every total the user did not change taken from [own]. */
    fun keepingUntouchedMetrics(own: OwnActivityMetrics): ActivityWriteRequest {
        val edited = editedMetrics ?: return this
        return copy(
            stepsCount = if (ActivityFormMetric.STEPS in edited) stepsCount else own.steps,
            distanceMeters = if (ActivityFormMetric.DISTANCE in edited) distanceMeters else own.distanceMeters,
            elevationGainedMeters =
            if (ActivityFormMetric.ELEVATION in edited) elevationGainedMeters else own.elevationGainedMeters,
            activeCaloriesKcal =
            if (ActivityFormMetric.ACTIVE_CALORIES in edited) activeCaloriesKcal else own.activeCaloriesKcal,
            totalCaloriesKcal =
            if (ActivityFormMetric.TOTAL_CALORIES in edited) totalCaloriesKcal else own.totalCaloriesKcal,
        )
    }
}

data class PlannedExerciseData(
    val id: String,
    val title: String?,
    val exerciseType: Int,
    val startTime: Instant,
    val endTime: Instant,
    /** The offset the plan was written in; the day it belongs to is read in this, not the phone's current zone. */
    val startZoneOffset: ZoneOffset? = null,
    val hasExplicitTime: Boolean,
    val completedExerciseSessionId: String?,
    val notes: String?,
    val blockCount: Int,
    val source: String,
    /**
     * The raw `dataOrigin` package. [source] shows a synced record's original
     * app, so ownership gates must use this.
     */
    val dataOriginPackage: String = source,
    val blocks: List<PlannedExerciseBlockData> = emptyList(),
) {
    val durationMs: Long get() = endTime.toEpochMilli() - startTime.toEpochMilli()
}

data class PlannedExerciseBlockData(
    val repetitions: Int,
    val description: String?,
    val steps: List<PlannedExerciseStepData>,
)

data class PlannedExerciseStepData(
    val exerciseType: Int,
    val exercisePhase: Int,
    val description: String?,
    val completion: PlannedExerciseCompletion,
    /** Pace/power/heart-rate/… targets other apps attach; carried through untouched. */
    val performanceTargets: List<PlannedExercisePerformanceTarget> = emptyList(),
)

sealed interface PlannedExerciseCompletion {
    data class Repetitions(val repetitions: Int) : PlannedExerciseCompletion
    data class DurationSeconds(val seconds: Long) : PlannedExerciseCompletion
    data class DistanceMeters(val meters: Double) : PlannedExerciseCompletion
    data class DistanceAndDuration(val meters: Double, val seconds: Long) : PlannedExerciseCompletion
    data class Steps(val steps: Int) : PlannedExerciseCompletion
    data class ActiveCaloriesKcal(val kcal: Double) : PlannedExerciseCompletion
    data class TotalCaloriesKcal(val kcal: Double) : PlannedExerciseCompletion
    data object Manual : PlannedExerciseCompletion
    data object Unknown : PlannedExerciseCompletion
}

sealed interface PlannedExercisePerformanceTarget {
    data class Power(val minWatts: Double, val maxWatts: Double) : PlannedExercisePerformanceTarget
    data class Speed(val minMetersPerSecond: Double, val maxMetersPerSecond: Double) : PlannedExercisePerformanceTarget
    data class Cadence(val minRpm: Double, val maxRpm: Double) : PlannedExercisePerformanceTarget
    data class HeartRate(val minBpm: Double, val maxBpm: Double) : PlannedExercisePerformanceTarget
    data class Weight(val kilograms: Double) : PlannedExercisePerformanceTarget
    data class RateOfPerceivedExertion(val rpe: Int) : PlannedExercisePerformanceTarget
    data object Amrap : PlannedExercisePerformanceTarget
    data object Unknown : PlannedExercisePerformanceTarget
}

data class PlannedExerciseWriteRequest(
    val id: String? = null,
    val exerciseType: Int,
    val startTime: Instant,
    val endTime: Instant,
    val title: String? = null,
    val notes: String? = null,
    val blocks: List<PlannedExerciseBlockData>,
)

data class DailySteps(
    val date: LocalDate,
    val steps: Long,
    val distanceMeters: Double,
    val wheelchairPushes: Long? = null,
    val floorsClimbed: Int? = null,
    val activeCaloriesKcal: Double? = null,
    val elevationGainedMeters: Double? = null,
)

data class ActivityProgressPoint(
    val time: Instant,
    val totalSteps: Long,
    val totalDistanceMeters: Double?,
    val totalCaloriesBurnedKcal: Double?,
    val totalActiveCaloriesKcal: Double? = null,
    val totalWheelchairPushes: Long? = null,
    val totalFloorsClimbed: Int? = null,
    val totalElevationGainedMeters: Double? = null,
)

data class SpeedSample(
    val time: Instant,
    val metersPerSecond: Double,
    val source: String,
)

enum class ActivityCadenceKind {
    CYCLING,
    STEPS,
}

data class ActivityCadenceSample(
    val time: Instant,
    val rate: Double,
    val kind: ActivityCadenceKind,
    val source: String,
)
