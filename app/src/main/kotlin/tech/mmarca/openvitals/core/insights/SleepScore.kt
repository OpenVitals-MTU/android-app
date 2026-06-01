package tech.mmarca.openvitals.core.insights

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.SleepStage
import tech.mmarca.openvitals.data.model.sleepDurationMsFromStages

private const val DurationWeight = 35.0
private const val EfficiencyWeight = 30.0
private const val ContinuityWeight = 20.0
private const val RegularityWeight = 15.0
private const val MinimumScoredSleepMinutes = 60.0
private const val NeutralRegularityRatio = 0.7
private const val MinutesPerDay = 24 * 60

enum class SleepScoreConfidence {
    HIGH,
    MEDIUM,
    LOW,
    NO_DATA,
}

data class SleepScoreEstimate(
    val score: Int = 0,
    val confidence: SleepScoreConfidence = SleepScoreConfidence.NO_DATA,
    val durationPoints: Double = 0.0,
    val efficiencyPoints: Double = 0.0,
    val continuityPoints: Double = 0.0,
    val regularityPoints: Double = 0.0,
    val sleepDurationMinutes: Double = 0.0,
    val timeInBedMinutes: Double = 0.0,
    val sleepEfficiencyPercent: Double = 0.0,
    val wakeAfterSleepOnsetMinutes: Double = 0.0,
    val regularityDifferenceMinutes: Double? = null,
    val regularityBaselineNights: Int = 0,
    val sleepStageCount: Int = 0,
    val usesSleepStages: Boolean = false,
    val usesExplicitAwakeStages: Boolean = false,
) {
    companion object {
        val NoData = SleepScoreEstimate()
    }
}

fun calculateSleepScoresByDate(
    sessions: List<SleepData>,
    start: LocalDate,
    end: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): Map<LocalDate, SleepScoreEstimate> {
    if (end.isBefore(start)) return emptyMap()

    val dates = generateSequence(start) { date ->
        date.plusDays(1).takeUnless { it.isAfter(end) }
    }.toList()
    val sessionsByDate = sessions.groupBy { it.endTime.atZone(zone).toLocalDate() }
    val mainSessions = dates.associateWith { date -> sessionsByDate[date].orEmpty().mainSleepSession() }

    return dates.mapIndexed { index, date ->
        val previousSessions = dates
            .take(index)
            .mapNotNull { previousDate -> mainSessions[previousDate] }
        date to calculateSleepScore(
            session = mainSessions[date],
            previousSessions = previousSessions,
            zone = zone,
        )
    }.toMap()
}

fun calculateSleepScore(
    session: SleepData?,
    previousSessions: List<SleepData>,
    zone: ZoneId = ZoneId.systemDefault(),
): SleepScoreEstimate {
    session ?: return SleepScoreEstimate.NoData
    val timeInBedMs = Duration.between(session.startTime, session.endTime)
        .toMillis()
        .coerceAtLeast(0L)
    val sleepDurationMs = sleepDurationMsFromStages(session.stages, session.durationMs)
        .coerceAtLeast(0L)
    if (timeInBedMs <= 0L || sleepDurationMs < Duration.ofMinutes(MinimumScoredSleepMinutes.toLong()).toMillis()) {
        return SleepScoreEstimate.NoData
    }

    val sleepDurationMinutes = sleepDurationMs.toDouble() / 60_000.0
    val timeInBedMinutes = timeInBedMs.toDouble() / 60_000.0
    val sleepEfficiencyPercent = (sleepDurationMs / timeInBedMs.toDouble() * 100.0)
        .coerceIn(0.0, 100.0)
    val explicitWakeMs = session.wakeAfterSleepOnsetMs()
    val wakeAfterSleepOnsetMinutes = (explicitWakeMs ?: (timeInBedMs - sleepDurationMs).coerceAtLeast(0L))
        .toDouble() / 60_000.0
    val midpoint = session.sleepMidpointMinute(zone)
    val baselineMidpoints = previousSessions.map { it.sleepMidpointMinute(zone) }
    val regularityDifference = if (baselineMidpoints.size >= 2) {
        circularMinuteDifference(midpoint, circularMeanMinutes(baselineMidpoints)).toDouble()
    } else {
        null
    }
    val hasSleepStages = session.stages.any { it.stageType.isSleepStage() }
    val hasExplicitAwakeStages = session.stages.any { it.stageType.isAwakeStage() }

    val durationPoints = durationPoints(sleepDurationMinutes / 60.0)
    val efficiencyPoints = efficiencyPoints(sleepEfficiencyPercent)
    val continuityPoints = continuityPoints(wakeAfterSleepOnsetMinutes)
    val regularityPoints = regularityDifference
        ?.let(::regularityPoints)
        ?: RegularityWeight * NeutralRegularityRatio
    val score = (durationPoints + efficiencyPoints + continuityPoints + regularityPoints)
        .roundToInt()
        .coerceIn(0, 100)

    return SleepScoreEstimate(
        score = score,
        confidence = when {
            hasSleepStages && hasExplicitAwakeStages && baselineMidpoints.size >= 3 -> SleepScoreConfidence.HIGH
            hasSleepStages || baselineMidpoints.size >= 2 -> SleepScoreConfidence.MEDIUM
            else -> SleepScoreConfidence.LOW
        },
        durationPoints = durationPoints,
        efficiencyPoints = efficiencyPoints,
        continuityPoints = continuityPoints,
        regularityPoints = regularityPoints,
        sleepDurationMinutes = sleepDurationMinutes,
        timeInBedMinutes = timeInBedMinutes,
        sleepEfficiencyPercent = sleepEfficiencyPercent,
        wakeAfterSleepOnsetMinutes = wakeAfterSleepOnsetMinutes,
        regularityDifferenceMinutes = regularityDifference,
        regularityBaselineNights = baselineMidpoints.size,
        sleepStageCount = session.stages.size,
        usesSleepStages = hasSleepStages,
        usesExplicitAwakeStages = hasExplicitAwakeStages,
    )
}

private fun List<SleepData>.mainSleepSession(): SleepData? =
    maxByOrNull { sleepDurationMsFromStages(it.stages, it.durationMs) }

private fun durationPoints(hours: Double): Double {
    val ratio = when {
        hours in 7.0..9.0 -> 1.0
        hours < 7.0 -> ((hours - 4.0) / 3.0).coerceIn(0.0, 1.0)
        else -> ((11.0 - hours) / 2.0).coerceIn(0.0, 1.0)
    }
    return DurationWeight * ratio
}

private fun efficiencyPoints(efficiencyPercent: Double): Double =
    EfficiencyWeight * ((efficiencyPercent - 65.0) / 20.0).coerceIn(0.0, 1.0)

private fun continuityPoints(wakeAfterSleepOnsetMinutes: Double): Double =
    ContinuityWeight * ((90.0 - wakeAfterSleepOnsetMinutes) / 70.0).coerceIn(0.0, 1.0)

private fun regularityPoints(regularityDifferenceMinutes: Double): Double =
    RegularityWeight * ((180.0 - regularityDifferenceMinutes) / 150.0).coerceIn(0.0, 1.0)

private fun SleepData.sleepMidpointMinute(zone: ZoneId): Int {
    val durationMs = Duration.between(startTime, endTime).toMillis().coerceAtLeast(0L)
    val midpoint = startTime.plusMillis(durationMs / 2)
    val localTime = midpoint.atZone(zone).toLocalTime()
    return localTime.hour * 60 + localTime.minute
}

private fun circularMeanMinutes(values: List<Int>): Int {
    val sinMean = values.sumOf { sin(it.toDouble() / MinutesPerDay * 2.0 * PI) } / values.size
    val cosMean = values.sumOf { cos(it.toDouble() / MinutesPerDay * 2.0 * PI) } / values.size
    val angle = atan2(sinMean, cosMean).let { if (it < 0.0) it + 2.0 * PI else it }
    return (angle / (2.0 * PI) * MinutesPerDay).roundToInt() % MinutesPerDay
}

private fun circularMinuteDifference(first: Int, second: Int): Int {
    val difference = abs(first - second)
    return minOf(difference, MinutesPerDay - difference)
}

private fun SleepData.wakeAfterSleepOnsetMs(): Long? {
    val sleepStages = stages
        .filter { it.stageType.isSleepStage() }
        .sortedBy { it.startTime }
    if (sleepStages.isEmpty()) return null

    val sleepStart = sleepStages.first().startTime
    val sleepEnd = sleepStages.last().endTime
    return stages
        .filter { it.stageType.isAwakeStage() }
        .sumOf { it.overlapMs(sleepStart, sleepEnd) }
}

private fun SleepStage.overlapMs(windowStart: Instant, windowEnd: Instant): Long {
    val overlapStart = maxOf(startTime, windowStart)
    val overlapEnd = minOf(endTime, windowEnd)
    if (!overlapEnd.isAfter(overlapStart)) return 0L
    return Duration.between(overlapStart, overlapEnd).toMillis().coerceAtLeast(0L)
}

private fun Int.isSleepStage(): Boolean = when (this) {
    SleepStage.STAGE_SLEEPING,
    SleepStage.STAGE_LIGHT,
    SleepStage.STAGE_DEEP,
    SleepStage.STAGE_REM -> true
    else -> false
}

private fun Int.isAwakeStage(): Boolean = when (this) {
    SleepStage.STAGE_AWAKE,
    SleepStage.STAGE_AWAKE_IN_BED -> true
    else -> false
}
