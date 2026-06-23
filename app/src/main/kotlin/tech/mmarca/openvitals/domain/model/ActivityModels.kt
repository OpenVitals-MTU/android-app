package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
)

data class PlannedExerciseData(
    val id: String,
    val title: String?,
    val exerciseType: Int,
    val startTime: Instant,
    val endTime: Instant,
    val hasExplicitTime: Boolean,
    val completedExerciseSessionId: String?,
    val notes: String?,
    val blockCount: Int,
    val source: String,
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
)

sealed interface PlannedExerciseCompletion {
    data class Repetitions(val repetitions: Int) : PlannedExerciseCompletion
    data class DurationSeconds(val seconds: Long) : PlannedExerciseCompletion
    data object Manual : PlannedExerciseCompletion
    data object Unknown : PlannedExerciseCompletion
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

data class StepProgressPoint(
    val time: Instant,
    val totalSteps: Long,
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
