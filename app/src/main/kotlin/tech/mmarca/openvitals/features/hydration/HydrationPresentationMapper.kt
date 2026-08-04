package tech.mmarca.openvitals.features.hydration

import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.displayPeriodFor
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.CrossMetricValue
import tech.mmarca.openvitals.domain.insights.crossMetricInsight
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.WeightEntry
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

object HydrationPresentationMapper {

    fun build(
        query: PeriodLoadQuery,
        dailyGoalLiters: Double,
        dailyHydration: List<DailyHydration>,
        previousDailyHydration: List<DailyHydration>,
        baselineDailyHydration: List<DailyHydration>,
        crossWeightEntries: List<WeightEntry>,
        today: LocalDate = LocalDate.now(),
    ): HydrationDisplayState {
        val selectedPeriod = displayPeriodFor(
            range = query.range,
            anchorDate = query.selectedDate,
            weekPeriodMode = query.weekPeriodMode,
        )
        val tracked = dailyHydration.filter { it.liters > 0.0 }
        val summary = dailyHydration.summaryForGoal(
            dailyGoalLiters = dailyGoalLiters,
            elapsedDays = elapsedDays(selectedPeriod, today),
            today = today,
        )
        val previousTotal = previousDailyHydration.sumOf { it.liters }
        val primaryValues = dailyHydration.map { CrossMetricValue(it.date, it.liters) }
        val secondaryValues = weightFluctuationValues(crossWeightEntries)

        return HydrationDisplayState(
            selectedPeriod = selectedPeriod,
            hasData = tracked.isNotEmpty(),
            summary = summary,
            periodComparison = periodComparison(
                currentValue = summary.totalLiters,
                previousValue = previousTotal,
            ),
            previousTotalLiters = previousTotal,
            baselineValues = baselineDailyHydration.map { BaselineValue(it.date, it.liters) },
            crossMetricInsight = crossMetricInsight(
                primaryValues = primaryValues,
                secondaryValues = secondaryValues,
            ),
            trackedDates = tracked.map { it.date },
            sampleCount = tracked.size,
            dayLiters = dailyHydration.sumOf { it.liters },
            // How much of the period you actually met the goal on — NOT the average of the
            // days you happened to log. Dividing by tracked days meant that logging one day
            // and hitting the goal filled the bar completely, so the bar rewarded you for
            // logging less.
            goalProgress = if (summary.elapsedDays > 0) {
                (summary.goalMetDays.toDouble() / summary.elapsedDays).coerceIn(0.0, 1.0)
            } else {
                0.0
            },
        )
    }
}

/**
 * Days of [period] that have happened. A goal you have not had the chance to miss yet must
 * not count against you, so a period running past today is cut at today.
 */
private fun elapsedDays(period: DatePeriod, today: LocalDate): Int {
    val end = if (period.end.isAfter(today)) today else period.end
    if (end.isBefore(period.start)) return 0
    return (end.toEpochDay() - period.start.toEpochDay()).toInt() + 1
}

private fun List<DailyHydration>.summaryForGoal(
    dailyGoalLiters: Double,
    elapsedDays: Int,
    today: LocalDate,
): HydrationPeriodSummary {
    val sorted = sortedBy { it.date }
    val totalLiters = sumOf { it.liters }
    val trackedDays = count { it.liters > 0.0 }
    val goalMetDays = count { it.meetsDailyGoal(dailyGoalLiters) }
    var currentGoalStreak = 0
    var longestGoalStreak = 0
    sorted.forEach { day ->
        if (day.meetsDailyGoal(dailyGoalLiters)) {
            currentGoalStreak += 1
            longestGoalStreak = maxOf(longestGoalStreak, currentGoalStreak)
        } else {
            currentGoalStreak = 0
        }
    }
    val reversed = sorted.asReversed()
    var trailingGoalStreak = 0
    for (day in reversed) {
        if (!day.meetsDailyGoal(dailyGoalLiters)) {
            // A day that has not finished yet cannot break the streak — the same protection
            // elapsedDays gives goalProgress. Without it the current streak collapsed to 0 at
            // midnight until today's goal was met.
            if (day.date.isBefore(today)) break else continue
        }
        trailingGoalStreak += 1
    }
    return HydrationPeriodSummary(
        totalLiters = totalLiters,
        trackedDays = trackedDays,
        loggedDays = size,
        elapsedDays = elapsedDays,
        // Deliberately still per TRACKED day: an average of the days you drank is a
        // meaningful number, where a bar's denominator is not.
        averageLiters = trackedDays.takeIf { it > 0 }?.let { totalLiters / it } ?: 0.0,
        bestDayLiters = maxOfOrNull { it.liters } ?: 0.0,
        goalMetDays = goalMetDays,
        goalSuccessRatePercent = trackedDays.takeIf { it > 0 }?.let { goalMetDays * 100 / it } ?: 0,
        currentTrackedStreakDays = reversed.takeWhile { it.liters > 0.0 }.count(),
        currentGoalStreakDays = trailingGoalStreak,
        longestGoalStreakDays = longestGoalStreak,
    )
}

private fun DailyHydration.meetsDailyGoal(dailyGoalLiters: Double): Boolean =
    dailyGoalLiters > 0.0 && liters >= dailyGoalLiters

private fun weightFluctuationValues(entries: List<WeightEntry>): List<CrossMetricValue> {
    val zone = ZoneId.systemDefault()
    val dailyWeights = entries
        .groupBy { it.time.atZone(zone).toLocalDate() }
        .mapValues { (_, dayEntries) -> dayEntries.map { it.weightKg }.average() }
        .toSortedMap()

    var previousWeight: Double? = null
    return dailyWeights.mapNotNull { (date, weight) ->
        val previous = previousWeight
        previousWeight = weight
        previous?.let { CrossMetricValue(date, abs(weight - it)) }
    }
}
