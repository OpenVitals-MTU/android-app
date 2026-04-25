package tech.mmarca.openvitals.data.model

import java.time.Instant
import java.time.LocalDate

// ─── Dashboard snapshot ───────────────────────────────────────────────────────

data class DashboardData(
    val date: LocalDate,
    val steps: Long = 0L,
    val distanceMeters: Double = 0.0,
    val caloriesKcal: Double = 0.0,
    val hydrationLiters: Double = 0.0,
    val workout: ExerciseData? = null,
    val sleep: SleepData? = null,
    val weightKg: Double = 0.0,
    val avgHeartRateBpm: Long = 0,
    val restingHeartRateBpm: Long = 0,
    val bodyFatPercent: Double = 0.0,
    val floorsClimbed: Int? = null,
    val elevationGainedMeters: Double? = null,
    val missingPermissions: Set<String> = emptySet(),
)

// ─── Exercise / Activity ──────────────────────────────────────────────────────

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
) {
    val durationMinutes: Long get() = durationMs / 60_000
}

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
)

// ─── Sleep ────────────────────────────────────────────────────────────────────

data class SleepData(
    val id: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long,
    val source: String,
    val stages: List<SleepStage> = emptyList(),
) {
    val durationHours: Double get() = durationMs / 3_600_000.0
    val durationFormatted: String
        get() {
            val h = durationMs / 3_600_000
            val m = (durationMs % 3_600_000) / 60_000
            return "${h}h ${m}m"
        }
}

data class SleepStage(
    val startTime: Instant,
    val endTime: Instant,
    val stageType: Int,
) {
    val durationMs: Long get() = endTime.toEpochMilli() - startTime.toEpochMilli()

    companion object {
        const val STAGE_UNKNOWN = 0
        const val STAGE_AWAKE = 1
        const val STAGE_SLEEPING = 2
        const val STAGE_OUT_OF_BED = 3
        const val STAGE_LIGHT = 4
        const val STAGE_DEEP = 5
        const val STAGE_REM = 6

        fun stageLabel(type: Int): String = when (type) {
            STAGE_AWAKE -> "Awake"
            STAGE_SLEEPING -> "Sleeping"
            STAGE_OUT_OF_BED -> "Out of bed"
            STAGE_LIGHT -> "Light"
            STAGE_DEEP -> "Deep"
            STAGE_REM -> "REM"
            else -> "Unknown"
        }
    }
}

// ─── Heart rate ───────────────────────────────────────────────────────────────

data class HeartRateSample(
    val time: Instant,
    val beatsPerMinute: Long,
    val source: String,
)

data class HeartRateSummary(
    val date: LocalDate,
    val avgBpm: Long,
    val minBpm: Long,
    val maxBpm: Long,
)

data class DailyRestingHR(
    val date: LocalDate,
    val bpm: Long,
)

data class DailyHrv(
    val date: LocalDate,
    val rmssdMs: Double,
)

// ─── Body metrics ─────────────────────────────────────────────────────────────

data class WeightEntry(
    val time: Instant,
    val weightKg: Double,
    val source: String,
)

data class BodyFatEntry(
    val time: Instant,
    val percent: Double,
    val source: String,
)

// ─── Nutrition ────────────────────────────────────────────────────────────────

data class DailyNutrition(
    val date: LocalDate,
    val hydrationLiters: Double,
    val caloriesBurnedKcal: Double,
)

// ─── Source attribution ───────────────────────────────────────────────────────

data class DataSource(
    val packageName: String,
    val deviceManufacturer: String?,
    val deviceModel: String?,
) {
    val displayName: String
        get() = when {
            packageName.contains("samsung") -> "Samsung Health"
            packageName.contains("fitbit") -> "Fitbit"
            packageName.contains("opentracks") -> "OpenTracks"
            packageName.contains("strava") -> "Strava"
            packageName.contains("garmin") -> "Garmin Connect"
            packageName.contains("polar") -> "Polar Flow"
            packageName.contains("google.android.apps.fitness") -> "Google Fit"
            else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
}

// ─── Health Connect availability ──────────────────────────────────────────────

enum class HealthConnectAvailability {
    AVAILABLE,
    NEEDS_PROVIDER_UPDATE,
    NOT_SUPPORTED,
}

// ─── Time range ───────────────────────────────────────────────────────────────

enum class TimeRange(val label: String, val days: Int) {
    DAY("Day", 1),
    WEEK("Week", 7),
    MONTH("Month", 30),
    YEAR("Year", 365),
}
