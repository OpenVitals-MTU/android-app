package tech.mmarca.openvitals.data.model

import java.time.LocalDate
import kotlin.math.roundToInt
import tech.mmarca.openvitals.core.insights.CardioLoadConfidence

data class DashboardData(
    val date: LocalDate,
    val steps: Long = 0L,
    val distanceMeters: Double = 0.0,
    val caloriesKcal: Double = 0.0,
    val activeCaloriesKcal: Double? = null,
    val hydrationLiters: Double = 0.0,
    val workout: ExerciseData? = null,
    val workouts: List<ExerciseData> = emptyList(),
    val sleep: SleepData? = null,
    val weightKg: Double = 0.0,
    val heightCm: Double? = null,
    val bmi: Double? = null,
    val avgHeartRateBpm: Long = 0,
    val restingHeartRateBpm: Long = 0,
    val hrvRmssdMs: Double? = null,
    val bodyFatPercent: Double = 0.0,
    val leanMassKg: Double? = null,
    val bmrKcal: Double? = null,
    val boneMassKg: Double? = null,
    val caloriesInKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
    val latestSystolicMmHg: Int? = null,
    val latestDiastolicMmHg: Int? = null,
    val latestSpO2Percent: Double? = null,
    val latestVo2Max: Double? = null,
    val avgRespiratoryRate: Double? = null,
    val latestBodyTemperatureCelsius: Double? = null,
    val weeklyCardioLoad: DashboardWeeklyCardioLoad? = null,
    val floorsClimbed: Int? = null,
    val elevationGainedMeters: Double? = null,
    val mindfulnessMinutes: Int? = null,
    val menstruationPeriodDays: Int? = null,
    val ovulationTestCount: Int? = null,
    val latestBasalBodyTemperatureCelsius: Double? = null,
    val missingPermissions: Set<String> = emptySet(),
    val loadedMetrics: Set<DashboardMetric> = emptySet(),
)

fun DashboardData.mergeLoaded(other: DashboardData): DashboardData =
    copy(
        steps = if (DashboardMetric.STEPS in other.loadedMetrics) other.steps else steps,
        distanceMeters = if (DashboardMetric.DISTANCE in other.loadedMetrics) other.distanceMeters else distanceMeters,
        caloriesKcal = if (DashboardMetric.CALORIES_OUT in other.loadedMetrics) other.caloriesKcal else caloriesKcal,
        activeCaloriesKcal = if (DashboardMetric.ACTIVE_CALORIES in other.loadedMetrics) other.activeCaloriesKcal else activeCaloriesKcal,
        hydrationLiters = if (DashboardMetric.HYDRATION in other.loadedMetrics) other.hydrationLiters else hydrationLiters,
        workout = if (DashboardMetric.WORKOUT in other.loadedMetrics) other.workout else workout,
        workouts = if (DashboardMetric.WORKOUT in other.loadedMetrics) other.workouts else workouts,
        sleep = if (DashboardMetric.SLEEP in other.loadedMetrics) other.sleep else sleep,
        weightKg = if (DashboardMetric.WEIGHT in other.loadedMetrics || DashboardMetric.BMI in other.loadedMetrics) {
            other.weightKg
        } else {
            weightKg
        },
        heightCm = if (DashboardMetric.HEIGHT in other.loadedMetrics || DashboardMetric.BMI in other.loadedMetrics) {
            other.heightCm
        } else {
            heightCm
        },
        bmi = if (DashboardMetric.BMI in other.loadedMetrics) other.bmi else bmi,
        avgHeartRateBpm = if (DashboardMetric.AVG_HEART_RATE in other.loadedMetrics) other.avgHeartRateBpm else avgHeartRateBpm,
        restingHeartRateBpm = if (DashboardMetric.RESTING_HEART_RATE in other.loadedMetrics) {
            other.restingHeartRateBpm
        } else {
            restingHeartRateBpm
        },
        hrvRmssdMs = if (DashboardMetric.HRV in other.loadedMetrics) other.hrvRmssdMs else hrvRmssdMs,
        bodyFatPercent = if (DashboardMetric.BODY_FAT in other.loadedMetrics) other.bodyFatPercent else bodyFatPercent,
        leanMassKg = if (DashboardMetric.LEAN_MASS in other.loadedMetrics) other.leanMassKg else leanMassKg,
        bmrKcal = if (DashboardMetric.BMR in other.loadedMetrics) other.bmrKcal else bmrKcal,
        boneMassKg = if (DashboardMetric.BONE_MASS in other.loadedMetrics) other.boneMassKg else boneMassKg,
        caloriesInKcal = if (DashboardMetric.CALORIES_IN in other.loadedMetrics) other.caloriesInKcal else caloriesInKcal,
        proteinGrams = if (DashboardMetric.PROTEIN in other.loadedMetrics) other.proteinGrams else proteinGrams,
        carbsGrams = if (DashboardMetric.CARBS in other.loadedMetrics) other.carbsGrams else carbsGrams,
        fatGrams = if (DashboardMetric.FAT in other.loadedMetrics) other.fatGrams else fatGrams,
        latestSystolicMmHg = if (DashboardMetric.BLOOD_PRESSURE in other.loadedMetrics) {
            other.latestSystolicMmHg
        } else {
            latestSystolicMmHg
        },
        latestDiastolicMmHg = if (DashboardMetric.BLOOD_PRESSURE in other.loadedMetrics) {
            other.latestDiastolicMmHg
        } else {
            latestDiastolicMmHg
        },
        latestSpO2Percent = if (DashboardMetric.SPO2 in other.loadedMetrics) other.latestSpO2Percent else latestSpO2Percent,
        latestVo2Max = if (DashboardMetric.VO2_MAX in other.loadedMetrics) other.latestVo2Max else latestVo2Max,
        avgRespiratoryRate = if (DashboardMetric.RESPIRATORY_RATE in other.loadedMetrics) {
            other.avgRespiratoryRate
        } else {
            avgRespiratoryRate
        },
        latestBodyTemperatureCelsius = if (DashboardMetric.BODY_TEMPERATURE in other.loadedMetrics) {
            other.latestBodyTemperatureCelsius
        } else {
            latestBodyTemperatureCelsius
        },
        weeklyCardioLoad = if (DashboardMetric.WEEKLY_CARDIO_LOAD in other.loadedMetrics) {
            other.weeklyCardioLoad
        } else {
            weeklyCardioLoad
        },
        floorsClimbed = if (DashboardMetric.FLOORS in other.loadedMetrics) other.floorsClimbed else floorsClimbed,
        elevationGainedMeters = if (DashboardMetric.ELEVATION in other.loadedMetrics) {
            other.elevationGainedMeters
        } else {
            elevationGainedMeters
        },
        mindfulnessMinutes = if (DashboardMetric.MINDFULNESS in other.loadedMetrics) {
            other.mindfulnessMinutes
        } else {
            mindfulnessMinutes
        },
        menstruationPeriodDays = if (DashboardMetric.CYCLE in other.loadedMetrics) {
            other.menstruationPeriodDays
        } else {
            menstruationPeriodDays
        },
        ovulationTestCount = if (DashboardMetric.CYCLE in other.loadedMetrics) other.ovulationTestCount else ovulationTestCount,
        latestBasalBodyTemperatureCelsius = if (DashboardMetric.CYCLE in other.loadedMetrics) {
            other.latestBasalBodyTemperatureCelsius
        } else {
            latestBasalBodyTemperatureCelsius
        },
        missingPermissions = missingPermissions + other.missingPermissions,
        loadedMetrics = loadedMetrics + other.loadedMetrics,
    )

data class DashboardWeeklyCardioLoad(
    val currentScore: Int,
    val targetScore: Int,
    val todayScore: Int,
    val confidence: CardioLoadConfidence,
    val targetSource: DashboardWeeklyCardioLoadTargetSource,
) {
    val progressFraction: Float
        get() = if (targetScore > 0) {
            (currentScore / targetScore.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val progressPercent: Int
        get() = (progressFraction * 100f).roundToInt()

    val todayProgressPercent: Int
        get() = if (targetScore > 0) {
            (todayScore * 100.0 / targetScore).roundToInt().coerceAtLeast(0)
        } else {
            0
        }
}

enum class DashboardWeeklyCardioLoadTargetSource {
    RECENT_HISTORY,
    CURRENT_PACE,
}
