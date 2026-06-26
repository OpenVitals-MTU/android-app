package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.insights.DefaultWeeklyIntensityMinutesTarget
import tech.mmarca.openvitals.domain.insights.IntensityMinutesConfidence
import tech.mmarca.openvitals.domain.insights.SleepScoreEstimate

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
    val sleepScore: SleepScoreEstimate = SleepScoreEstimate.NoData,
    val weightKg: Double? = null,
    val weightTime: Instant? = null,
    val heightCm: Double? = null,
    val heightTime: Instant? = null,
    val bmi: Double? = null,
    val avgHeartRateBpm: Long = 0,
    val heartRateSampleCount: Int = 0,
    val heartRateSampleStartTime: Instant? = null,
    val heartRateSampleEndTime: Instant? = null,
    val restingHeartRateBpm: Long = 0,
    val restingHeartRateBaselineBpm: Long? = null,
    val hrvRmssdMs: Double? = null,
    val hrvBaselineRmssdMs: Double? = null,
    val hrvSampleCount: Int = 0,
    val hrvSampleStartTime: Instant? = null,
    val hrvSampleEndTime: Instant? = null,
    val bodyFatPercent: Double = 0.0,
    val leanMassKg: Double? = null,
    val bmrKcal: Double? = null,
    val boneMassKg: Double? = null,
    val bodyWaterMassKg: Double? = null,
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
    val latestBloodGlucoseMillimolesPerLiter: Double? = null,
    val latestSkinTemperatureDeltaCelsius: Double? = null,
    val weeklyCardioLoad: DashboardWeeklyCardioLoad? = null,
    val weeklyIntensityMinutes: DashboardWeeklyIntensityMinutes? = null,
    val floorsClimbed: Int? = null,
    val elevationGainedMeters: Double? = null,
    val wheelchairPushes: Long? = null,
    val mindfulnessMinutes: Int? = null,
    val menstruationPeriodDays: Int? = null,
    val ovulationTestCount: Int? = null,
    val latestBasalBodyTemperatureCelsius: Double? = null,
    val missingPermissions: Set<String> = emptySet(),
    val loadedMetrics: Set<DashboardMetric> = emptySet(),
    val metricSourcePackages: Map<DashboardMetric, String> = emptyMap(),
    val caloriesKcalSource: CaloriesBurnedSource = if (caloriesKcal > 0.0) {
        CaloriesBurnedSource.RECORDED_TOTAL
    } else {
        CaloriesBurnedSource.NO_DATA
    },
)

fun DashboardData.mergeLoaded(other: DashboardData): DashboardData =
    copy(
        steps = if (DashboardMetric.STEPS in other.loadedMetrics) other.steps else steps,
        distanceMeters = if (DashboardMetric.DISTANCE in other.loadedMetrics) other.distanceMeters else distanceMeters,
        caloriesKcal = if (DashboardMetric.CALORIES_OUT in other.loadedMetrics) other.caloriesKcal else caloriesKcal,
        caloriesKcalSource = if (DashboardMetric.CALORIES_OUT in other.loadedMetrics) {
            other.caloriesKcalSource
        } else {
            caloriesKcalSource
        },
        activeCaloriesKcal = if (DashboardMetric.ACTIVE_CALORIES in other.loadedMetrics) other.activeCaloriesKcal else activeCaloriesKcal,
        hydrationLiters = if (DashboardMetric.HYDRATION in other.loadedMetrics) other.hydrationLiters else hydrationLiters,
        workout = if (DashboardMetric.WORKOUT in other.loadedMetrics) other.workout else workout,
        workouts = if (DashboardMetric.WORKOUT in other.loadedMetrics) other.workouts else workouts,
        sleep = if (DashboardMetric.SLEEP in other.loadedMetrics) other.sleep else sleep,
        sleepScore = if (DashboardMetric.SLEEP in other.loadedMetrics) other.sleepScore else sleepScore,
        weightKg = if (DashboardMetric.WEIGHT in other.loadedMetrics || DashboardMetric.BMI in other.loadedMetrics) {
            other.weightKg
        } else {
            weightKg
        },
        weightTime = if (DashboardMetric.WEIGHT in other.loadedMetrics || DashboardMetric.BMI in other.loadedMetrics) {
            other.weightTime
        } else {
            weightTime
        },
        heightCm = if (DashboardMetric.HEIGHT in other.loadedMetrics || DashboardMetric.BMI in other.loadedMetrics) {
            other.heightCm
        } else {
            heightCm
        },
        heightTime = if (DashboardMetric.HEIGHT in other.loadedMetrics || DashboardMetric.BMI in other.loadedMetrics) {
            other.heightTime
        } else {
            heightTime
        },
        bmi = if (DashboardMetric.BMI in other.loadedMetrics) other.bmi else bmi,
        avgHeartRateBpm = if (DashboardMetric.AVG_HEART_RATE in other.loadedMetrics) other.avgHeartRateBpm else avgHeartRateBpm,
        heartRateSampleCount = if (DashboardMetric.AVG_HEART_RATE in other.loadedMetrics) {
            other.heartRateSampleCount
        } else {
            heartRateSampleCount
        },
        heartRateSampleStartTime = if (DashboardMetric.AVG_HEART_RATE in other.loadedMetrics) {
            other.heartRateSampleStartTime
        } else {
            heartRateSampleStartTime
        },
        heartRateSampleEndTime = if (DashboardMetric.AVG_HEART_RATE in other.loadedMetrics) {
            other.heartRateSampleEndTime
        } else {
            heartRateSampleEndTime
        },
        restingHeartRateBpm = if (DashboardMetric.RESTING_HEART_RATE in other.loadedMetrics) {
            other.restingHeartRateBpm
        } else {
            restingHeartRateBpm
        },
        restingHeartRateBaselineBpm = if (DashboardMetric.RESTING_HEART_RATE in other.loadedMetrics) {
            other.restingHeartRateBaselineBpm
        } else {
            restingHeartRateBaselineBpm
        },
        hrvRmssdMs = if (DashboardMetric.HRV in other.loadedMetrics) other.hrvRmssdMs else hrvRmssdMs,
        hrvBaselineRmssdMs = if (DashboardMetric.HRV in other.loadedMetrics) other.hrvBaselineRmssdMs else hrvBaselineRmssdMs,
        hrvSampleCount = if (DashboardMetric.HRV in other.loadedMetrics) other.hrvSampleCount else hrvSampleCount,
        hrvSampleStartTime = if (DashboardMetric.HRV in other.loadedMetrics) {
            other.hrvSampleStartTime
        } else {
            hrvSampleStartTime
        },
        hrvSampleEndTime = if (DashboardMetric.HRV in other.loadedMetrics) {
            other.hrvSampleEndTime
        } else {
            hrvSampleEndTime
        },
        bodyFatPercent = if (DashboardMetric.BODY_FAT in other.loadedMetrics) other.bodyFatPercent else bodyFatPercent,
        leanMassKg = if (DashboardMetric.LEAN_MASS in other.loadedMetrics) other.leanMassKg else leanMassKg,
        bmrKcal = if (DashboardMetric.BMR in other.loadedMetrics) other.bmrKcal else bmrKcal,
        boneMassKg = if (DashboardMetric.BONE_MASS in other.loadedMetrics) other.boneMassKg else boneMassKg,
        bodyWaterMassKg = if (DashboardMetric.BODY_WATER_MASS in other.loadedMetrics) {
            other.bodyWaterMassKg
        } else {
            bodyWaterMassKg
        },
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
        latestBloodGlucoseMillimolesPerLiter = if (DashboardMetric.BLOOD_GLUCOSE in other.loadedMetrics) {
            other.latestBloodGlucoseMillimolesPerLiter
        } else {
            latestBloodGlucoseMillimolesPerLiter
        },
        latestSkinTemperatureDeltaCelsius = if (DashboardMetric.SKIN_TEMPERATURE in other.loadedMetrics) {
            other.latestSkinTemperatureDeltaCelsius
        } else {
            latestSkinTemperatureDeltaCelsius
        },
        weeklyCardioLoad = if (DashboardMetric.WEEKLY_CARDIO_LOAD in other.loadedMetrics) {
            other.weeklyCardioLoad
        } else {
            weeklyCardioLoad
        },
        weeklyIntensityMinutes = if (DashboardMetric.INTENSITY_MINUTES in other.loadedMetrics) {
            other.weeklyIntensityMinutes
        } else {
            weeklyIntensityMinutes
        },
        floorsClimbed = if (DashboardMetric.FLOORS in other.loadedMetrics) other.floorsClimbed else floorsClimbed,
        elevationGainedMeters = if (DashboardMetric.ELEVATION in other.loadedMetrics) {
            other.elevationGainedMeters
        } else {
            elevationGainedMeters
        },
        wheelchairPushes = if (DashboardMetric.WHEELCHAIR_PUSHES in other.loadedMetrics) {
            other.wheelchairPushes
        } else {
            wheelchairPushes
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
        metricSourcePackages = metricSourcePackages + other.metricSourcePackages,
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

data class DashboardWeeklyIntensityMinutes(
    val moderateMinutes: Int,
    val vigorousMinutes: Int,
    val moderateEquivalentMinutes: Int,
    val targetMinutes: Int = DefaultWeeklyIntensityMinutesTarget,
    val todayModerateEquivalentMinutes: Int,
    val daysElapsed: Int,
    val confidence: IntensityMinutesConfidence,
) {
    val progressFraction: Float
        get() = if (targetMinutes > 0) {
            (moderateEquivalentMinutes / targetMinutes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val progressPercent: Int
        get() = (progressFraction * 100f).roundToInt()

    val expectedByNowMinutes: Int
        get() = if (targetMinutes > 0) {
            (targetMinutes * daysElapsed.coerceIn(1, 7) / 7.0).roundToInt().coerceAtLeast(1)
        } else {
            0
        }

    val isOnPace: Boolean
        get() = moderateEquivalentMinutes >= expectedByNowMinutes
}
