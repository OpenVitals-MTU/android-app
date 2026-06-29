package tech.mmarca.openvitals.domain.dashboard

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.periodFor
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.insights.CardioLoadEstimate
import tech.mmarca.openvitals.domain.insights.CardioLoadTimeWindow
import tech.mmarca.openvitals.domain.insights.IntensityMinutesConfidence
import tech.mmarca.openvitals.domain.insights.IntensityMinutesEstimate
import tech.mmarca.openvitals.domain.insights.IntensityWorkoutInput
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoadTargetSource
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.toWeekPeriodMode

/**
 * Pure dashboard combine helpers. Health Connect I/O stays in [tech.mmarca.openvitals.data.repository.dashboard.DashboardDataLoader].
 */
object DashboardAggregator {

    fun cardioLoadPeriod(date: LocalDate, activityWeekMode: ActivityWeekMode): DatePeriod =
        periodFor(
            range = TimeRange.WEEK,
            anchorDate = date,
            today = date,
            weekPeriodMode = activityWeekMode.toWeekPeriodMode(),
        )

    fun weeklyCardioTarget(
        currentScore: Int,
        daysElapsed: Int,
        previousWeekScores: List<Int>,
    ): WeeklyCardioTarget? {
        val previousBaseline = previousWeekScores
            .filter { it > 0 }
            .medianDoubleOrNull()
        if (previousBaseline != null) {
            return WeeklyCardioTarget(
                score = previousBaseline.roundCardioTarget(),
                source = DashboardWeeklyCardioLoadTargetSource.RECENT_HISTORY,
            )
        }

        if (currentScore <= 0 || daysElapsed <= 0) return null
        return WeeklyCardioTarget(
            score = (currentScore * 7.0 / daysElapsed).roundCardioTarget(),
            source = DashboardWeeklyCardioLoadTargetSource.CURRENT_PACE,
        )
    }

    fun List<CardioLoadEstimate>.weeklyCardioConfidence(): CardioLoadConfidence {
        val tracked = filter { it.score > 0 && it.confidence != CardioLoadConfidence.NO_DATA }
        return when {
            tracked.isEmpty() -> CardioLoadConfidence.NO_DATA
            tracked.any { it.confidence == CardioLoadConfidence.HIGH } -> CardioLoadConfidence.HIGH
            tracked.any { it.confidence == CardioLoadConfidence.MEDIUM } -> CardioLoadConfidence.MEDIUM
            else -> CardioLoadConfidence.LOW
        }
    }

    fun List<IntensityMinutesEstimate>.weeklyIntensityConfidence(): IntensityMinutesConfidence {
        val tracked = filter {
            it.moderateEquivalentMinutes > 0 && it.confidence != IntensityMinutesConfidence.NO_DATA
        }
        return when {
            tracked.isEmpty() -> IntensityMinutesConfidence.NO_DATA
            tracked.any { it.confidence == IntensityMinutesConfidence.HIGH } -> IntensityMinutesConfidence.HIGH
            tracked.any { it.confidence == IntensityMinutesConfidence.MEDIUM } -> IntensityMinutesConfidence.MEDIUM
            else -> IntensityMinutesConfidence.LOW
        }
    }

    fun List<ExerciseData>.cardioLoadWindows(date: LocalDate, zone: ZoneId): List<CardioLoadTimeWindow> {
        val dayStart = date.atStartOfDay(zone).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
        return mapNotNull { workout ->
            if (!workout.endTime.isAfter(dayStart) || !workout.startTime.isBefore(dayEnd)) return@mapNotNull null
            CardioLoadTimeWindow(
                start = maxOf(workout.startTime, dayStart),
                end = minOf(workout.endTime, dayEnd),
            ).takeIf { it.durationMinutes > 0.0 }
        }
    }

    fun List<ExerciseData>.intensityWorkoutInputs(date: LocalDate, zone: ZoneId): List<IntensityWorkoutInput> {
        val dayStart = date.atStartOfDay(zone).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
        return mapNotNull { workout ->
            if (!workout.endTime.isAfter(dayStart) || !workout.startTime.isBefore(dayEnd)) return@mapNotNull null
            val overlapStart = maxOf(workout.startTime, dayStart)
            val overlapEnd = minOf(workout.endTime, dayEnd)
            if (!overlapEnd.isAfter(overlapStart)) return@mapNotNull null
            val overlapMinutes = Duration.between(overlapStart, overlapEnd).seconds.toDouble() / 60.0
            if (overlapMinutes <= 0.0) return@mapNotNull null
            val totalMinutes = workout.durationMs.coerceAtLeast(0L).toDouble() / 60_000.0
            val activeCalories = workout.activeCaloriesKcal?.takeIf { totalMinutes > 0.0 }?.let { calories ->
                calories * (overlapMinutes / totalMinutes)
            }
            IntensityWorkoutInput(
                durationMinutes = overlapMinutes,
                activeCaloriesKcal = activeCalories,
            )
        }
    }

    fun datesInRange(start: LocalDate, end: LocalDate): Sequence<LocalDate> =
        if (start.isAfter(end)) {
            emptySequence()
        } else {
            generateSequence(start) { date ->
                date.plusDays(1).takeUnless { it.isAfter(end) }
            }
        }

    fun List<Long>.medianLongOrNull(): Long? {
        if (isEmpty()) return null
        val sorted = sorted()
        return sorted[sorted.lastIndex / 2]
    }

    fun List<Int>.medianDoubleOrNull(): Double? {
        if (isEmpty()) return null
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle].toDouble()
        }
    }

    fun List<Double>.medianDoubleValuesOrNull(): Double? {
        if (isEmpty()) return null
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    fun mergeDerivedDashboardProjection(base: DashboardData, projection: DashboardData): DashboardData =
        base.copy(
            caloriesKcal = if (DashboardMetric.CALORIES_OUT in projection.loadedMetrics &&
                projection.caloriesKcalSource == CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR
            ) {
                projection.caloriesKcal
            } else {
                base.caloriesKcal
            },
            caloriesKcalSource = if (DashboardMetric.CALORIES_OUT in projection.loadedMetrics &&
                projection.caloriesKcalSource == CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR
            ) {
                projection.caloriesKcalSource
            } else {
                base.caloriesKcalSource
            },
            bmi = if (DashboardMetric.BMI in projection.loadedMetrics) projection.bmi else base.bmi,
            ffmi = if (DashboardMetric.FFMI in projection.loadedMetrics) projection.ffmi else base.ffmi,
            sleepScore = if (DashboardMetric.SLEEP in projection.loadedMetrics) projection.sleepScore else base.sleepScore,
            restingHeartRateBaselineBpm = if (DashboardMetric.RESTING_HEART_RATE in projection.loadedMetrics) {
                projection.restingHeartRateBaselineBpm
            } else {
                base.restingHeartRateBaselineBpm
            },
            hrvRmssdMs = if (DashboardMetric.HRV in projection.loadedMetrics) projection.hrvRmssdMs else base.hrvRmssdMs,
            hrvBaselineRmssdMs = if (DashboardMetric.HRV in projection.loadedMetrics) {
                projection.hrvBaselineRmssdMs
            } else {
                base.hrvBaselineRmssdMs
            },
            weeklyCardioLoad = if (DashboardMetric.WEEKLY_CARDIO_LOAD in projection.loadedMetrics) {
                projection.weeklyCardioLoad
            } else {
                base.weeklyCardioLoad
            },
            weeklyIntensityMinutes = if (DashboardMetric.INTENSITY_MINUTES in projection.loadedMetrics) {
                projection.weeklyIntensityMinutes
            } else {
                base.weeklyIntensityMinutes
            },
            loadedMetrics = base.loadedMetrics + projection.loadedMetrics,
        )

    fun Double.roundCardioTarget(): Int =
        ((this / 5.0).roundToInt() * 5).coerceAtLeast(5)
}

data class WeeklyCardioTarget(
    val score: Int,
    val source: DashboardWeeklyCardioLoadTargetSource,
)
