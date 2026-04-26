package tech.mmarca.openvitals.data.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
    val caloriesInKcal: Double? = null,
    val latestSystolicMmHg: Int? = null,
    val latestDiastolicMmHg: Int? = null,
    val latestSpO2Percent: Double? = null,
    val latestVo2Max: Double? = null,
    val floorsClimbed: Int? = null,
    val elevationGainedMeters: Double? = null,
    val mindfulnessMinutes: Int? = null,
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
    val title: String? = null,
    val notes: String? = null,
    val startZoneOffset: ZoneOffset? = null,
    val endZoneOffset: ZoneOffset? = null,
    val lastModifiedTime: Instant? = null,
    val clientRecordId: String? = null,
    val clientRecordVersion: Long? = null,
    val recordingMethod: Int? = null,
    val device: SleepDeviceData? = null,
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

data class SleepDeviceData(
    val type: Int,
    val manufacturer: String?,
    val model: String?,
)

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
        const val STAGE_AWAKE_IN_BED = 7

        fun stageLabel(type: Int): String = when (type) {
            STAGE_AWAKE -> "Awake"
            STAGE_SLEEPING -> "Sleeping"
            STAGE_OUT_OF_BED -> "Out of bed"
            STAGE_LIGHT -> "Light"
            STAGE_DEEP -> "Deep"
            STAGE_REM -> "REM"
            STAGE_AWAKE_IN_BED -> "Awake in bed"
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

data class DailyHydration(
    val date: LocalDate,
    val liters: Double,
)

data class NutritionEntry(
    val time: Instant,
    val mealType: Int,
    val name: String?,
    val energyKcal: Double?,
    val proteinGrams: Double?,
    val carbsGrams: Double?,
    val fatGrams: Double?,
    val fiberGrams: Double?,
    val sugarGrams: Double?,
    val source: String,
)

data class DailyMacros(
    val date: LocalDate,
    val energyKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
)

// ─── Mindfulness ─────────────────────────────────────────────────────────────

data class MindfulnessSession(
    val id: String,
    val title: String?,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long,
    val source: String,
) {
    val durationMinutes: Long get() = durationMs / 60_000
}

// ─── Cycle tracking ─────────────────────────────────────────────────────────

data class CycleData(
    val menstruationFlows: List<MenstruationFlowEntry> = emptyList(),
    val menstruationPeriods: List<MenstruationPeriodEntry> = emptyList(),
    val ovulationTests: List<OvulationTestEntry> = emptyList(),
    val cervicalMucus: List<CervicalMucusEntry> = emptyList(),
    val basalBodyTemperature: List<BasalBodyTemperatureEntry> = emptyList(),
) {
    val hasData: Boolean
        get() = menstruationFlows.isNotEmpty() ||
            menstruationPeriods.isNotEmpty() ||
            ovulationTests.isNotEmpty() ||
            cervicalMucus.isNotEmpty() ||
            basalBodyTemperature.isNotEmpty()
}

data class MenstruationFlowEntry(
    val time: Instant,
    val flow: Int,
    val source: String,
)

data class MenstruationPeriodEntry(
    val startTime: Instant,
    val endTime: Instant,
    val source: String,
) {
    val durationMs: Long get() = endTime.toEpochMilli() - startTime.toEpochMilli()
}

data class OvulationTestEntry(
    val time: Instant,
    val result: Int,
    val source: String,
)

data class CervicalMucusEntry(
    val time: Instant,
    val appearance: Int,
    val sensation: Int,
    val source: String,
)

data class BasalBodyTemperatureEntry(
    val time: Instant,
    val temperatureCelsius: Double,
    val measurementLocation: Int,
    val source: String,
)

// ─── Vitals ──────────────────────────────────────────────────────────────────

data class BloodPressureEntry(
    val time: Instant,
    val systolicMmHg: Int,
    val diastolicMmHg: Int,
    val source: String,
)

data class SpO2Entry(
    val time: Instant,
    val percent: Double,
    val source: String,
)

data class RespiratoryRateEntry(
    val time: Instant,
    val breathsPerMinute: Double,
    val source: String,
)

data class BodyTempEntry(
    val time: Instant,
    val temperatureCelsius: Double,
    val source: String,
)

data class Vo2MaxEntry(
    val time: Instant,
    val vo2MaxMlPerKgPerMin: Double,
    val source: String,
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
