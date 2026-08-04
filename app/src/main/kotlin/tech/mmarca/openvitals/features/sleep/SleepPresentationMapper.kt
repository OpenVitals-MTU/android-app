package tech.mmarca.openvitals.features.sleep

import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.baselinePeriodBefore
import tech.mmarca.openvitals.core.period.displayPeriodFor
import tech.mmarca.openvitals.core.period.previousPeriodFor
import tech.mmarca.openvitals.domain.insights.CrossMetricValue
import tech.mmarca.openvitals.domain.insights.SleepScoreConfidence
import tech.mmarca.openvitals.domain.insights.calculateSleepScoreForDate
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailySleepDuration
import tech.mmarca.openvitals.domain.model.AwakeStageTypes
import tech.mmarca.openvitals.domain.model.CoreStageTypes
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.model.SleepNapGap
import tech.mmarca.openvitals.domain.model.combineNightStages
import tech.mmarca.openvitals.domain.model.dailyNaps
import tech.mmarca.openvitals.domain.model.durationMsForTypes
import tech.mmarca.openvitals.domain.model.dailySleepSummary
import tech.mmarca.openvitals.domain.model.sleepDurationMsFromStages
import tech.mmarca.openvitals.domain.model.sleepSessionsForRange
import tech.mmarca.openvitals.domain.model.sleepSessionsUnionMs
import tech.mmarca.openvitals.domain.model.splitNightAndNaps
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

private const val MinutesPerDay = 24 * 60

object SleepPresentationMapper {

    fun build(
        query: PeriodLoadQuery,
        sleepWindow: SleepWindow,
        sessions: List<SleepData>,
        previousSessions: List<SleepData>,
        baselineSessions: List<SleepData>,
        dailyDurations: List<DailySleepDuration> = emptyList(),
        previousDailyDurations: List<DailySleepDuration> = emptyList(),
        baselineDailyDurations: List<DailySleepDuration> = emptyList(),
        crossDailyHrv: List<DailyHrv>,
    ): SleepDisplayState {
        val selectedPeriod = displayPeriodFor(
            range = query.range,
            anchorDate = query.selectedDate,
            weekPeriodMode = query.weekPeriodMode,
        )
        val previousPeriod = previousPeriodFor(
            range = query.range,
            anchorDate = query.selectedDate,
            weekPeriodMode = query.weekPeriodMode,
        )
        val baselinePeriod = baselinePeriodBefore(selectedPeriod)
        val dailySessions = sleepSessionsForRange(
            sessions = sessions,
            selectedDate = query.selectedDate,
            sleepWindow = sleepWindow,
        )
        val dailySummary = dailySleepSummary(
            sessions = sessions,
            selectedDate = query.selectedDate,
            sleepWindow = sleepWindow,
        )
        val durationPoints = sleepDurationPoints(
            sessions = sessions,
            period = selectedPeriod,
            sleepWindow = sleepWindow,
        )
        val previousDurationPoints = sleepDurationPoints(
            sessions = previousSessions,
            period = previousPeriod,
            sleepWindow = sleepWindow,
        )
        val baselineDurationPoints = sleepDurationPoints(
            sessions = baselineSessions,
            period = baselinePeriod,
            sleepWindow = sleepWindow,
        )
        val sleepScoreSessions = (baselineSessions + sessions).distinctBy { it.id }
        val overviewDays = sleepOverviewDays(
            sessions = sessions,
            scoreSessions = sleepScoreSessions,
            dailyDurations = dailyDurations,
            period = selectedPeriod,
            sleepWindow = sleepWindow,
        )

        return SleepDisplayState(
            dailySessions = dailySessions,
            dailySummary = dailySummary,
            dayNaps = dailyNaps(
                sessions = sessions,
                selectedDate = query.selectedDate,
                sleepWindow = sleepWindow,
            ),
            selectedPeriod = selectedPeriod,
            previousPeriod = previousPeriod,
            baselinePeriod = baselinePeriod,
            durationPoints = durationPoints,
            previousDurationPoints = previousDurationPoints,
            baselineDurationPoints = baselineDurationPoints,
            overviewDays = overviewDays,
            overviewSummary = overviewDays.toSleepOverviewSummary(),
            crossMetricHrvValues = crossDailyHrv.map { CrossMetricValue(it.date, it.rmssdMs) },
        )
    }
}

fun sleepDurationPoints(
    sessions: List<SleepData>,
    period: DatePeriod,
    sleepWindow: SleepWindow,
): List<SleepDurationPoint> {
    val zone = ZoneId.systemDefault()

    return generateSequence(period.start) { current ->
        current.plusDays(1).takeUnless { it.isAfter(period.end) }
    }.map { date ->
        SleepDurationPoint(
            date = date,
            hours = dailySleepSummary(
                sessions = sessions,
                selectedDate = date,
                sleepWindow = sleepWindow,
                zone = zone,
            )?.let { summary ->
                sleepDurationMsFromStages(summary.stages, summary.durationMs) / 3_600_000.0
            } ?: 0.0,
        )
    }.toList()
}

private fun sleepOverviewDays(
    sessions: List<SleepData>,
    scoreSessions: List<SleepData>,
    dailyDurations: List<DailySleepDuration>,
    period: DatePeriod,
    sleepWindow: SleepWindow,
): List<SleepOverviewDay> {
    val zone = ZoneId.systemDefault()
    val dates = datesInPeriod(period)
    val durationsByDate = dailyDurations.associateBy { it.date }
    val sessionsByDate = dates.associateWith { date ->
        sleepSessionsForRange(
            sessions = sessions,
            selectedDate = date,
            sleepWindow = sleepWindow,
            zone = zone,
        )
    }

    return dates.map { date ->
        SleepOverviewDay(
            date = date,
            sessions = sessionsByDate[date].orEmpty(),
            aggregateDurationMs = durationsByDate[date]?.durationMs,
            sleepScore = calculateSleepScoreForDate(
                selectedDate = date,
                sessions = scoreSessions,
                sleepWindow = sleepWindow,
                zone = zone,
            ),
        )
    }
}

private fun datesInPeriod(period: DatePeriod): List<LocalDate> =
    generateSequence(period.start) { current ->
        current.plusDays(1).takeUnless { it.isAfter(period.end) }
    }.toList()

/**
 * One [SleepScheduleDay] per overview day for the time-aligned schedule chart: the night's
 * in-bed window (earliest start → latest end across that day's sessions) plus the union of all
 * stages. Nights with no sessions produce null start/end and empty stages.
 */
fun List<SleepOverviewDay>.toSleepScheduleDays(): List<SleepScheduleDay> = map { day ->
    val sessions = day.sessions
    SleepScheduleDay(
        date = day.date,
        inBedStart = sessions.minOfOrNull { it.startTime },
        inBedEnd = sessions.maxOfOrNull { it.endTime },
        stages = sessions.flatMap { it.stages }.sortedBy { it.startTime },
    )
}

fun List<SleepOverviewDay>.toSleepOverviewSummary(): SleepOverviewSummary {
    val nights = filter { it.sleepDurationMs > 0L }
    val scoredDays = filter { it.sleepScore.confidence != SleepScoreConfidence.NO_DATA }
    val mainSessions = nights.mapNotNull { it.mainSleepSession }
    val averageByNight = size > 1
    val durationSource = if (averageByNight) nights else this

    return SleepOverviewSummary(
        dates = map { it.date },
        sleepScore = scoredDays
            .takeIf { it.isNotEmpty() }
            ?.map { it.sleepScore.score }
            ?.average()
            ?.roundToInt(),
        sleepScoreConfidence = scoredDays.sleepScoreConfidence(),
        sleepDurationMs = durationSource.averageDurationMs { it.sleepDurationMs },
        timeInBedMs = durationSource.averageDurationMs { it.timeInBedMs },
        schedule = mainSessions.averageSchedule(),
        awakeDurationMs = durationSource.averageDurationMs { it.awakeDurationMs },
        remDurationMs = durationSource.averageDurationMs { it.remDurationMs },
        coreDurationMs = durationSource.averageDurationMs { it.coreDurationMs },
        deepDurationMs = durationSource.averageDurationMs { it.deepDurationMs },
        sleepEfficiencyPercent = scoredDays
            .takeIf { it.isNotEmpty() }
            ?.map { it.sleepScore.sleepEfficiencyPercent }
            ?.average(),
        remValues = map { it.remDurationMs.toDouble() },
        deepValues = map { it.deepDurationMs.toDouble() },
        efficiencyValues = map { day ->
            if (day.sleepScore.confidence == SleepScoreConfidence.NO_DATA) {
                0.0
            } else {
                day.sleepScore.sleepEfficiencyPercent
            }
        },
    )
}

private val SleepOverviewDay.nightSessions: List<SleepData>
    get() = splitNightAndNaps(sessions).night

private val SleepOverviewDay.sleepDurationMs: Long
    get() = nightSessions.let { night ->
        sleepDurationMsFromStages(
            stages = combineNightStages(night, maxGap = SleepNapGap),
            fallbackDurationMs = sleepSessionsUnionMs(night),
        )
    }

private val SleepOverviewDay.timeInBedMs: Long
    get() = nightSessions.let { night ->
        val start = night.minOfOrNull { it.startTime } ?: return@let 0L
        val end = night.maxOfOrNull { it.endTime } ?: return@let 0L
        (end.toEpochMilli() - start.toEpochMilli()).coerceAtLeast(0L)
    }

private val SleepOverviewDay.awakeDurationMs: Long
    get() = combineNightStages(nightSessions, maxGap = SleepNapGap).durationMsForTypes(AwakeStageTypes)

private val SleepOverviewDay.remDurationMs: Long
    get() = sessions.stageDurationMs(setOf(SleepStage.STAGE_REM))

private val SleepOverviewDay.coreDurationMs: Long
    get() = sessions.stageDurationMs(CoreStageTypes)

private val SleepOverviewDay.deepDurationMs: Long
    get() = sessions.stageDurationMs(setOf(SleepStage.STAGE_DEEP))

private val SleepOverviewDay.mainSleepSession: SleepData?
    get() = sessions.mainSleepSession()

private fun List<SleepOverviewDay>.averageDurationMs(selector: (SleepOverviewDay) -> Long): Long {
    val values = map(selector).filter { it > 0L }
    return values
        .takeIf { it.isNotEmpty() }
        ?.let { (it.sum().toDouble() / it.size).roundToLong() }
        ?: 0L
}

private fun List<SleepOverviewDay>.sleepScoreConfidence(): SleepScoreConfidence = when {
    isEmpty() -> SleepScoreConfidence.NO_DATA
    all { it.sleepScore.confidence == SleepScoreConfidence.HIGH } -> SleepScoreConfidence.HIGH
    any {
        it.sleepScore.confidence == SleepScoreConfidence.HIGH ||
            it.sleepScore.confidence == SleepScoreConfidence.MEDIUM
    } -> SleepScoreConfidence.MEDIUM
    else -> SleepScoreConfidence.LOW
}

private fun List<SleepData>.averageSchedule(): SleepOverviewSchedule? {
    if (isEmpty()) return null
    val zone = ZoneId.systemDefault()
    val startMinute = circularMeanMinutes(
        map { session -> session.startTime.atZone(zone).toLocalTime().toMinuteOfDay() },
    )
    val endMinute = circularMeanMinutes(
        map { session -> session.endTime.atZone(zone).toLocalTime().toMinuteOfDay() },
    )
    return SleepOverviewSchedule(startMinute = startMinute, endMinute = endMinute)
}

private fun List<SleepData>.mainSleepSession(): SleepData? =
    maxByOrNull { sleepDurationMsFromStages(it.stages, it.durationMs) }

private fun List<SleepData>.stageDurationMs(stageTypes: Set<Int>): Long =
    sumOf { session -> session.stages.durationMsForTypes(stageTypes) }

private fun circularMeanMinutes(values: List<Int>): Int {
    if (values.isEmpty()) return 0
    val sinMean = values.sumOf { sin(it.toDouble() / MinutesPerDay * 2.0 * PI) } / values.size
    val cosMean = values.sumOf { cos(it.toDouble() / MinutesPerDay * 2.0 * PI) } / values.size
    val angle = atan2(sinMean, cosMean).let { if (it < 0.0) it + 2.0 * PI else it }
    return (angle / (2.0 * PI) * MinutesPerDay).roundToInt() % MinutesPerDay
}

private fun java.time.LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
