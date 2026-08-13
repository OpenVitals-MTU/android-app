package tech.mmarca.openvitals.domain.insights

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
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.model.dailySleepSummary
import tech.mmarca.openvitals.domain.model.sleepDurationMsFromStages

/*
 * Sleep score — a 0–100 wellness estimate aligned with Garmin's three-pillar
 * framing (duration, quality, overnight recovery) and the sleep-science
 * references that framing cites:
 *
 * - Duration: NSF age-banded recommendations (Hirshkowitz 2015) and AASM/SRS
 *   adult consensus that healthy adults need ≥7 h (Watson 2015). Regularly
 *   sleeping <7 h associates with cardiometabolic risk (Grandner 2014; Liu 2013).
 * - Quality: NSF sleep-quality recommendations (Ohayon 2017) — efficiency,
 *   wake after sleep onset / continuity, and restorative stage architecture
 *   (deep + REM) when consumer staging is available.
 * - Overnight recovery: autonomic recovery during the night from HRV RMSSD
 *   relative to the personal baseline — the same recovery science Garmin
 *   describes for Body Battery / overnight recovery.
 *
 * Missing inputs never invent numbers: stage and HRV pillars go neutral and
 * confidence drops. The score is not a diagnosis.
 */

/** Duration pillar weight (NSF / AASM quantity). */
internal const val SleepScoreDurationWeight = 40.0

/** Quality pillar weight (NSF quality: efficiency + continuity + stages). */
internal const val SleepScoreQualityWeight = 40.0

/** Overnight recovery pillar weight (HRV / ANS). */
internal const val SleepScoreRecoveryWeight = 20.0

private const val EfficiencyShare = 15.0
private const val ContinuityShare = 15.0
private const val StageShare = 10.0

private const val MinimumScoredSleepMinutes = 60.0
private const val NeutralMissingRatio = 0.7
private const val MinutesPerDay = 24 * 60

/** Lookback used when scoring a single day (regularity metadata + HRV baseline). */
internal const val SleepScoreLookbackDays = 7L

enum class SleepScoreConfidence {
    HIGH,
    MEDIUM,
    LOW,
    NO_DATA,
}

/**
 * NSF 2015 recommended sleep duration window for an age band, plus soft floors
 * and ceilings used when scoring outside the ideal range.
 */
data class SleepDurationTarget(
    val idealMinHours: Double,
    val idealMaxHours: Double,
    val floorHours: Double,
    val ceilingHours: Double,
)

data class SleepScoreEstimate(
    val score: Int = 0,
    val confidence: SleepScoreConfidence = SleepScoreConfidence.NO_DATA,
    val durationPoints: Double = 0.0,
    val qualityPoints: Double = 0.0,
    val recoveryPoints: Double = 0.0,
    /** Breakdown of [qualityPoints] for the detail screen. */
    val efficiencyPoints: Double = 0.0,
    val continuityPoints: Double = 0.0,
    val stageBalancePoints: Double = 0.0,
    val sleepDurationMinutes: Double = 0.0,
    val timeInBedMinutes: Double = 0.0,
    val sleepEfficiencyPercent: Double = 0.0,
    val wakeAfterSleepOnsetMinutes: Double = 0.0,
    val deepSleepPercentOfSleep: Double? = null,
    val remSleepPercentOfSleep: Double? = null,
    val overnightHrvRmssdMs: Double? = null,
    val overnightHrvBaselineRmssdMs: Double? = null,
    val durationTarget: SleepDurationTarget? = null,
    val ageYearsUsed: Int? = null,
    /** Timing difference vs recent nights — context only, not a scored pillar. */
    val regularityDifferenceMinutes: Double? = null,
    val regularityBaselineNights: Int = 0,
    val sleepStageCount: Int = 0,
    val usesSleepStages: Boolean = false,
    val usesExplicitAwakeStages: Boolean = false,
    val usesOvernightHrv: Boolean = false,
) {
    companion object {
        val NoData = SleepScoreEstimate()
    }
}

/**
 * Optional overnight HRV inputs for the recovery pillar. [rmssdMs] should be
 * measured inside the sleep session window; [baselineRmssdMs] is the personal
 * recent baseline (e.g. median of prior nights).
 */
data class OvernightHrvInput(
    val rmssdMs: Double,
    val baselineRmssdMs: Double,
)

/**
 * Builds overnight HRV inputs by averaging RMSSD samples inside each night's
 * main sleep window and using the median of prior nights as baseline.
 */
fun overnightHrvInputsByDate(
    sessions: List<SleepData>,
    hrvSamples: List<HrvSample>,
    start: LocalDate,
    end: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): Map<LocalDate, OvernightHrvInput> {
    if (end.isBefore(start) || hrvSamples.isEmpty()) return emptyMap()
    val sessionsByDate = sessions.groupBy { it.endTime.atZone(zone).toLocalDate() }
    val nightRmssd = generateSequence(start) { date ->
        date.plusDays(1).takeUnless { it.isAfter(end) }
    }.associateWith { date ->
        val session = sessionsByDate[date].orEmpty().mainSleepSession() ?: return@associateWith null
        hrvSamples
            .asSequence()
            .filter { sample ->
                !sample.time.isBefore(session.startTime) && !sample.time.isAfter(session.endTime)
            }
            .map { it.rmssdMs }
            .filter { it > 0.0 }
            .toList()
            .takeIf { it.isNotEmpty() }
            ?.average()
    }
    return overnightHrvInputsFromNightRmssd(nightRmssd)
}

/**
 * Builds overnight HRV inputs from already-bucketed daily RMSSD values
 * (for example sleep-screen cross-metric HRV).
 */
fun overnightHrvInputsFromDaily(
    dailyHrv: List<DailyHrv>,
    start: LocalDate,
    end: LocalDate,
): Map<LocalDate, OvernightHrvInput> {
    if (end.isBefore(start) || dailyHrv.isEmpty()) return emptyMap()
    val byDate = dailyHrv
        .filter { it.rmssdMs > 0.0 && !it.date.isBefore(start) && !it.date.isAfter(end) }
        .associate { it.date to it.rmssdMs }
    val nightRmssd = generateSequence(start) { date ->
        date.plusDays(1).takeUnless { it.isAfter(end) }
    }.associateWith { byDate[it] }
    return overnightHrvInputsFromNightRmssd(nightRmssd)
}

private fun overnightHrvInputsFromNightRmssd(
    nightRmssd: Map<LocalDate, Double?>,
): Map<LocalDate, OvernightHrvInput> {
    val ordered = nightRmssd.keys.sorted()
    return ordered.mapNotNull { date ->
        val rmssd = nightRmssd[date] ?: return@mapNotNull null
        val baselineValues = ordered
            .asSequence()
            .takeWhile { it.isBefore(date) }
            .mapNotNull { nightRmssd[it] }
            .toList()
        val baseline = baselineValues.medianOrNull() ?: return@mapNotNull null
        date to OvernightHrvInput(rmssdMs = rmssd, baselineRmssdMs = baseline)
    }.toMap()
}

private fun List<Double>.medianOrNull(): Double? {
    if (isEmpty()) return null
    val sorted = sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 0) {
        (sorted[mid - 1] + sorted[mid]) / 2.0
    } else {
        sorted[mid]
    }
}

fun calculateSleepScoresByDate(
    sessions: List<SleepData>,
    start: LocalDate,
    end: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
    ageYears: Int? = null,
    overnightHrvByDate: Map<LocalDate, OvernightHrvInput> = emptyMap(),
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
            ageYears = ageYears,
            overnightHrv = overnightHrvByDate[date],
        )
    }.toMap()
}

fun calculateSleepScoreForDate(
    selectedDate: LocalDate,
    sessions: List<SleepData>,
    sleepWindow: SleepWindow,
    zone: ZoneId = ZoneId.systemDefault(),
    ageYears: Int? = null,
    overnightHrv: OvernightHrvInput? = null,
): SleepScoreEstimate {
    val selectedSleep = dailySleepSummary(
        sessions = sessions,
        selectedDate = selectedDate,
        sleepWindow = sleepWindow,
        zone = zone,
    )
    val startDate = selectedDate.minusDays(SleepScoreLookbackDays - 1)
    val previousSessions = generateSequence(startDate) { date ->
        date.plusDays(1).takeIf { it.isBefore(selectedDate) }
    }.mapNotNull { date ->
        dailySleepSummary(
            sessions = sessions,
            selectedDate = date,
            sleepWindow = sleepWindow,
            zone = zone,
        )
    }.toList()

    return calculateSleepScore(
        session = selectedSleep,
        previousSessions = previousSessions,
        zone = zone,
        ageYears = ageYears,
        overnightHrv = overnightHrv,
    )
}

fun calculateSleepScore(
    session: SleepData?,
    previousSessions: List<SleepData>,
    zone: ZoneId = ZoneId.systemDefault(),
    ageYears: Int? = null,
    overnightHrv: OvernightHrvInput? = null,
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
    val hasStagedArchitecture = session.stages.any { it.stageType.isDeepOrRemStage() }

    val target = sleepDurationTargetForAge(ageYears)
    val durationPoints = durationPoints(sleepDurationMinutes / 60.0, target)

    val stagePercents = stagePercents(session, sleepDurationMs)
    val quality = sleepQualityPillar(
        session = session,
        sleepDurationMs = sleepDurationMs,
        sleepEfficiencyPercent = sleepEfficiencyPercent,
        wakeAfterSleepOnsetMinutes = wakeAfterSleepOnsetMinutes,
    )
    val efficiencyPts = quality.efficiencyPoints
    val continuityPts = quality.continuityPoints
    val stagePts = quality.stageBalancePoints
    val qualityPoints = quality.total

    val usesOvernightHrv = overnightHrv != null &&
        overnightHrv.rmssdMs > 0.0 &&
        overnightHrv.baselineRmssdMs > 0.0
    val recoveryPoints = overnightHrv
        ?.takeIf { usesOvernightHrv }
        ?.let { recoveryPoints(it.rmssdMs, it.baselineRmssdMs) }
        ?: (SleepScoreRecoveryWeight * NeutralMissingRatio)

    val score = (durationPoints + qualityPoints + recoveryPoints)
        .roundToInt()
        .coerceIn(0, 100)

    return SleepScoreEstimate(
        score = score,
        confidence = when {
            hasSleepStages && hasExplicitAwakeStages && usesOvernightHrv && baselineMidpoints.size >= 2 ->
                SleepScoreConfidence.HIGH
            (hasSleepStages || usesOvernightHrv) && baselineMidpoints.isNotEmpty() ->
                SleepScoreConfidence.MEDIUM
            hasSleepStages || usesOvernightHrv || baselineMidpoints.isNotEmpty() ->
                SleepScoreConfidence.MEDIUM
            else -> SleepScoreConfidence.LOW
        },
        durationPoints = durationPoints,
        qualityPoints = qualityPoints,
        recoveryPoints = recoveryPoints,
        efficiencyPoints = efficiencyPts,
        continuityPoints = continuityPts,
        stageBalancePoints = stagePts,
        sleepDurationMinutes = sleepDurationMinutes,
        timeInBedMinutes = timeInBedMinutes,
        sleepEfficiencyPercent = sleepEfficiencyPercent,
        wakeAfterSleepOnsetMinutes = wakeAfterSleepOnsetMinutes,
        deepSleepPercentOfSleep = stagePercents?.first,
        remSleepPercentOfSleep = stagePercents?.second,
        overnightHrvRmssdMs = overnightHrv?.rmssdMs?.takeIf { usesOvernightHrv },
        overnightHrvBaselineRmssdMs = overnightHrv?.baselineRmssdMs?.takeIf { usesOvernightHrv },
        durationTarget = target,
        ageYearsUsed = ageYears,
        regularityDifferenceMinutes = regularityDifference,
        regularityBaselineNights = baselineMidpoints.size,
        sleepStageCount = session.stages.size,
        usesSleepStages = hasSleepStages,
        usesExplicitAwakeStages = hasExplicitAwakeStages,
        usesOvernightHrv = usesOvernightHrv,
    )
}

/**
 * NSF 2015 age-banded recommended sleep duration. Adults default to 7–9 h when
 * age is unknown (AASM/SRS adult consensus).
 */
fun sleepDurationTargetForAge(ageYears: Int?): SleepDurationTarget = when {
    ageYears == null -> SleepDurationTarget(7.0, 9.0, 4.0, 11.0)
    ageYears < 14 -> SleepDurationTarget(9.0, 11.0, 6.0, 13.0) // school-aged 6–13
    ageYears in 14..17 -> SleepDurationTarget(8.0, 10.0, 5.0, 12.0)
    ageYears in 18..64 -> SleepDurationTarget(7.0, 9.0, 4.0, 11.0)
    else -> SleepDurationTarget(7.0, 8.0, 4.0, 10.0) // older adults 65+
}

internal fun durationPoints(hours: Double, target: SleepDurationTarget): Double {
    val ratio = when {
        hours in target.idealMinHours..target.idealMaxHours -> 1.0
        hours < target.idealMinHours -> {
            val span = (target.idealMinHours - target.floorHours).coerceAtLeast(0.01)
            ((hours - target.floorHours) / span).coerceIn(0.0, 1.0)
        }
        else -> {
            val span = (target.ceilingHours - target.idealMaxHours).coerceAtLeast(0.01)
            ((target.ceilingHours - hours) / span).coerceIn(0.0, 1.0)
        }
    }
    return SleepScoreDurationWeight * ratio
}

/**
 * The quality pillar of one night: how well it was slept, as distinct from how
 * long ([durationPoints]) and how the autonomic system answered
 * ([recoveryPoints]).
 *
 * Its own type because Body Energy needs this pillar WITHOUT the other two —
 * its sleep charge already counts the minutes and already reads overnight HRV,
 * so the whole sleep score would count both of those twice.
 */
data class SleepQualityPillar(
    val efficiencyPoints: Double,
    val continuityPoints: Double,
    val stageBalancePoints: Double,
    /** Whether the night carried deep/REM staging, or only its bounds. */
    val staged: Boolean,
    /**
     * The same night read as a CONTINUOUS 0..1, for models that ask how much it
     * restored rather than whether it was healthy.
     *
     * The scored points answer a clinical question and answer it with
     * thresholds: efficiency at or above 85% is good, time awake at or under
     * twenty minutes is good, and both earn full marks the moment they clear.
     * That is the right shape for a score citing NSF and AASM, and the wrong
     * shape for recovery — it makes a flawless night and a merely good one
     * identical, when the following day is not.
     *
     * So this ramp keeps going where the score stops: efficiency all the way to
     * 100%, time awake all the way to none. Stage architecture is NOT stretched
     * the same way, because more deep sleep past the healthy band is not more
     * recovery — a band is genuinely what that one is.
     */
    val continuousFraction: Double,
) {
    val total: Double get() = efficiencyPoints + continuityPoints + stageBalancePoints

    /** [total] as a fraction of the pillar's weight, 0..1. */
    val fraction: Double get() = (total / SleepScoreQualityWeight).coerceIn(0.0, 1.0)
}

/**
 * The quality pillar for [session]. Without staging the stage share is
 * redistributed into efficiency and continuity, so quality still totals
 * [SleepScoreQualityWeight] rather than being quietly capped at three quarters
 * of it.
 */
fun sleepQualityPillar(
    session: SleepData,
    sleepDurationMs: Long,
    sleepEfficiencyPercent: Double,
    wakeAfterSleepOnsetMinutes: Double,
): SleepQualityPillar {
    val stagePercents = stagePercents(session, sleepDurationMs)
    val staged = session.stages.any { it.stageType.isDeepOrRemStage() } && stagePercents != null
    // The continuous read. Weighted like the scored pillar so the two stay
    // recognisably the same measure, with the stage share redistributed the
    // same way when a night has no staging to judge.
    val continuousEfficiency = ((sleepEfficiencyPercent - 65.0) / 35.0).coerceIn(0.0, 1.0)
    val continuousContinuity = ((90.0 - wakeAfterSleepOnsetMinutes) / 90.0).coerceIn(0.0, 1.0)
    val continuousStages = stagePercents
        ?.takeIf { staged }
        ?.let { stageBalancePoints(it) / StageShare }
    val continuousFraction = if (continuousStages == null) {
        (continuousEfficiency * (EfficiencyShare + StageShare / 2.0) +
            continuousContinuity * (ContinuityShare + StageShare / 2.0)) / SleepScoreQualityWeight
    } else {
        (continuousEfficiency * EfficiencyShare +
            continuousContinuity * ContinuityShare +
            continuousStages * StageShare) / SleepScoreQualityWeight
    }

    return SleepQualityPillar(
        efficiencyPoints = if (staged) {
            efficiencyPoints(sleepEfficiencyPercent)
        } else {
            (EfficiencyShare + StageShare / 2.0) *
                ((sleepEfficiencyPercent - 65.0) / 20.0).coerceIn(0.0, 1.0)
        },
        continuityPoints = if (staged) {
            continuityPoints(wakeAfterSleepOnsetMinutes)
        } else {
            (ContinuityShare + StageShare / 2.0) *
                ((90.0 - wakeAfterSleepOnsetMinutes) / 70.0).coerceIn(0.0, 1.0)
        },
        stageBalancePoints = if (staged) stageBalancePoints(stagePercents!!) else 0.0,
        staged = staged,
        continuousFraction = continuousFraction.coerceIn(0.0, 1.0),
    )
}

/**
 * NSF quality: sleep efficiency ≥85% is good. Map 65%→0 and 85%→full so the
 * clinically meaningful threshold earns full credit.
 */
internal fun efficiencyPoints(efficiencyPercent: Double): Double =
    EfficiencyShare * ((efficiencyPercent - 65.0) / 20.0).coerceIn(0.0, 1.0)

/**
 * NSF quality: WASO ≤20 min is good. Map 90 min→0 and 20 min→full.
 */
internal fun continuityPoints(wakeAfterSleepOnsetMinutes: Double): Double =
    ContinuityShare * ((90.0 - wakeAfterSleepOnsetMinutes) / 70.0).coerceIn(0.0, 1.0)

/**
 * Restorative stage architecture: deep ~13–23% and REM ~20–25% of total sleep
 * time are typical adult targets. Soft trapezoid scoring around those bands.
 */
internal fun stageBalancePoints(deepAndRemPercent: Pair<Double, Double>): Double {
    val (deep, rem) = deepAndRemPercent
    val deepRatio = bandRatio(deep, idealMin = 13.0, idealMax = 23.0, floor = 5.0, ceiling = 35.0)
    val remRatio = bandRatio(rem, idealMin = 20.0, idealMax = 25.0, floor = 8.0, ceiling = 35.0)
    return StageShare * ((deepRatio + remRatio) / 2.0)
}

/**
 * Overnight recovery from HRV: overnight RMSSD at or above the personal baseline
 * earns full credit; values materially below baseline reduce the pillar.
 * The mapping is continuous so progressive impairment always lowers the score.
 */
internal fun recoveryPoints(rmssdMs: Double, baselineRmssdMs: Double): Double {
    val ratio = (rmssdMs / baselineRmssdMs).coerceAtLeast(0.0)
    val scoreRatio = when {
        ratio >= 1.0 -> 1.0
        ratio >= 0.7 -> 0.55 + 0.45 * ((ratio - 0.7) / 0.3)
        else -> (ratio / 0.7) * 0.55
    }
    return SleepScoreRecoveryWeight * scoreRatio
}

private fun bandRatio(
    value: Double,
    idealMin: Double,
    idealMax: Double,
    floor: Double,
    ceiling: Double,
): Double = when {
    value in idealMin..idealMax -> 1.0
    value < idealMin -> ((value - floor) / (idealMin - floor)).coerceIn(0.0, 1.0)
    else -> ((ceiling - value) / (ceiling - idealMax)).coerceIn(0.0, 1.0)
}

private fun stagePercents(
    session: SleepData,
    sleepDurationMs: Long,
): Pair<Double, Double>? {
    if (sleepDurationMs <= 0L) return null
    val deepMs = session.stages
        .filter { it.stageType == SleepStage.STAGE_DEEP }
        .sumOf { it.durationMs.coerceAtLeast(0L) }
    val remMs = session.stages
        .filter { it.stageType == SleepStage.STAGE_REM }
        .sumOf { it.durationMs.coerceAtLeast(0L) }
    if (deepMs == 0L && remMs == 0L) return null
    return (deepMs * 100.0 / sleepDurationMs) to (remMs * 100.0 / sleepDurationMs)
}

private fun List<SleepData>.mainSleepSession(): SleepData? =
    maxByOrNull { sleepDurationMsFromStages(it.stages, it.durationMs) }

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

internal fun SleepData.wakeAfterSleepOnsetMs(): Long? {
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

private fun Int.isDeepOrRemStage(): Boolean = when (this) {
    SleepStage.STAGE_DEEP,
    SleepStage.STAGE_REM -> true
    else -> false
}
