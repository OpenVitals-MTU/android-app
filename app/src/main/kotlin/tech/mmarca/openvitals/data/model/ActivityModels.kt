package tech.mmarca.openvitals.data.model

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

data class ActivityWriteRequest(
    val exerciseType: Int,
    val startTime: Instant,
    val endTime: Instant,
    val title: String? = null,
    val notes: String? = null,
    val routePoints: List<ExerciseRoutePoint> = emptyList(),
    val pauseIntervals: List<ActivityPauseInterval> = emptyList(),
    val distanceMeters: Double? = null,
    val elevationGainedMeters: Double? = null,
    val activeCaloriesKcal: Double? = null,
    val totalCaloriesKcal: Double? = null,
)

data class DailySteps(
    val date: LocalDate,
    val steps: Long,
    val distanceMeters: Double,
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
    val totalFloorsClimbed: Int? = null,
    val totalElevationGainedMeters: Double? = null,
)
