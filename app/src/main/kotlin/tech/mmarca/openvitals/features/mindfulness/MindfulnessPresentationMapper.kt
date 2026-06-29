package tech.mmarca.openvitals.features.mindfulness

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.displayPeriodFor
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.CrossMetricValue
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.insights.crossMetricInsight
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.dailySleepSummary
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import java.time.ZoneId

object MindfulnessPresentationMapper {

    private val goalKey = MetricDailyGoalKey.MINDFULNESS_MINUTES

    fun build(
        query: PeriodLoadQuery,
        dailyGoalMinutes: Double,
        sleepRangeMode: SleepRangeMode,
        sessions: List<MindfulnessSession>,
        previousSessions: List<MindfulnessSession>,
        baselineSessions: List<MindfulnessSession>,
        crossSleepSessions: List<SleepData>,
    ): MindfulnessDisplayState {
        val selectedPeriod = displayPeriodFor(
            range = query.range,
            anchorDate = query.selectedDate,
            weekPeriodMode = query.weekPeriodMode,
        )
        val dailyMinutes = mindfulnessDailyMinutes(sessions)
        val summary = sessions.summary()
        val previousTotalMs = previousSessions.sumOf { it.durationMs.coerceAtLeast(0L) }
        val baselineValues = mindfulnessDailyMinutes(baselineSessions)
            .map { BaselineValue(it.date, it.minutes) }
        val goalProgress = dailyGoalProgress(
            values = dailyMinutes.map { DailyGoalValue(it.date, it.minutes) },
            period = selectedPeriod,
            target = dailyGoalMinutes,
            direction = goalKey.direction,
        )
        val zone = ZoneId.systemDefault()
        val trackedDates = sessions.map { it.startTime.atZone(zone).toLocalDate() }.distinct()

        return MindfulnessDisplayState(
            selectedPeriod = selectedPeriod,
            hasData = sessions.isNotEmpty(),
            summary = summary,
            dailyMinutes = dailyMinutes,
            goalProgress = goalProgress,
            periodComparison = periodComparison(
                currentValue = summary.totalMs.toDouble(),
                previousValue = previousTotalMs.toDouble(),
            ),
            previousTotalMs = previousTotalMs,
            baselineValues = baselineValues,
            baselineInsight = personalBaselineInsight(
                currentValue = dailyMinutes.map { it.minutes }.takeIf { it.isNotEmpty() }?.average() ?: 0.0,
                values = baselineValues,
                referenceDate = selectedPeriod.start.minusDays(1),
            ),
            crossMetricInsight = crossMetricInsight(
                primaryValues = dailyMinutes.map { CrossMetricValue(it.date, it.minutes) },
                secondaryValues = sleepDurationValues(
                    sessions = crossSleepSessions,
                    period = selectedPeriod,
                    sleepRangeMode = sleepRangeMode,
                ),
            ),
            trackedDates = trackedDates,
            sampleCount = sessions.size,
        )
    }
}

private fun List<MindfulnessSession>.summary(): MindfulnessPeriodSummary {
    val totalMs = sumOf { it.durationMs.coerceAtLeast(0L) }
    val averageMs = takeIf { it.isNotEmpty() }?.let { totalMs / it.size } ?: 0L
    val longestMs = maxOfOrNull { it.durationMs.coerceAtLeast(0L) } ?: 0L
    return MindfulnessPeriodSummary(
        totalMinutes = sumOf { it.durationMinutes },
        totalMs = totalMs,
        sessionCount = size,
        averageDurationMs = averageMs,
        longestSessionMs = longestMs,
    )
}

private fun mindfulnessDailyMinutes(sessions: List<MindfulnessSession>): List<MindfulnessDayValue> {
    val zone = ZoneId.systemDefault()
    return sessions
        .groupBy { it.startTime.atZone(zone).toLocalDate() }
        .map { (date, daySessions) ->
            MindfulnessDayValue(
                date = date,
                minutes = daySessions.sumOf { it.durationMs.coerceAtLeast(0L) }.toDouble() / 60_000.0,
            )
        }
}

private fun sleepDurationValues(
    sessions: List<SleepData>,
    period: tech.mmarca.openvitals.core.period.DatePeriod,
    sleepRangeMode: SleepRangeMode,
): List<CrossMetricValue> {
    val zone = ZoneId.systemDefault()
    return generateSequence(period.start) { current ->
        current.plusDays(1).takeUnless { it.isAfter(period.end) }
    }.map { date ->
        CrossMetricValue(
            date = date,
            value = dailySleepSummary(
                sessions = sessions,
                selectedDate = date,
                sleepRangeMode = sleepRangeMode,
                zone = zone,
            )?.durationHours ?: 0.0,
        )
    }.toList()
}
