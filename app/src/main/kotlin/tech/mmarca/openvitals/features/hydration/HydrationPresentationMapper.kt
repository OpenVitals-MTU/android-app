package tech.mmarca.openvitals.features.hydration

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.displayPeriodFor
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.CrossMetricValue
import tech.mmarca.openvitals.domain.insights.crossMetricInsight
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.WeightEntry
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
    ): HydrationDisplayState {
        val selectedPeriod = displayPeriodFor(
            range = query.range,
            anchorDate = query.selectedDate,
            weekPeriodMode = query.weekPeriodMode,
        )
        val tracked = dailyHydration.filter { it.liters > 0.0 }
        val summary = dailyHydration.summaryForGoal(dailyGoalLiters)
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
        )
    }
}

private fun List<DailyHydration>.summaryForGoal(dailyGoalLiters: Double): HydrationPeriodSummary {
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
    return HydrationPeriodSummary(
        totalLiters = totalLiters,
        trackedDays = trackedDays,
        loggedDays = size,
        averageLiters = trackedDays.takeIf { it > 0 }?.let { totalLiters / it } ?: 0.0,
        bestDayLiters = maxOfOrNull { it.liters } ?: 0.0,
        goalMetDays = goalMetDays,
        goalSuccessRatePercent = trackedDays.takeIf { it > 0 }?.let { goalMetDays * 100 / it } ?: 0,
        currentTrackedStreakDays = reversed.takeWhile { it.liters > 0.0 }.count(),
        currentGoalStreakDays = reversed.takeWhile { it.meetsDailyGoal(dailyGoalLiters) }.count(),
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
