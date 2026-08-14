package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
        const val STAGE_AWAKE = 1
        const val STAGE_SLEEPING = 2
        const val STAGE_OUT_OF_BED = 3
        const val STAGE_LIGHT = 4
        const val STAGE_DEEP = 5
        const val STAGE_REM = 6
        const val STAGE_AWAKE_IN_BED = 7
    }
}

/**
 * Time actually ASLEEP: stage durations with the awake epochs excluded. A
 * night with 8h in bed and 40 minutes awake reads 7h20m. Sessions whose
 * writer recorded no stages keep the plain session duration.
 */
fun SleepData.asleepDurationMs(): Long = sleepDurationMsFromStages(stages, durationMs)

data class DailySleepDuration(
    val date: LocalDate,
    val durationMs: Long,
) {
    val durationHours: Double get() = durationMs / 3_600_000.0
}

data class SleepReadData(
    val sessions: List<SleepData> = emptyList(),
    val dailyAggregateDurations: List<DailySleepDuration> = emptyList(),
)

/**
 * The union of slept intervals in milliseconds — a sweep-merge, so overlapping
 * cross-source survivors can never sum past real time.
 */
fun sleepSessionsUnionMs(sessions: Iterable<SleepData>): Long {
    val intervals = sessions
        .filter { it.endTime.isAfter(it.startTime) }
        .map { it.startTime to it.endTime }
        .sortedBy { it.first }
    if (intervals.isEmpty()) return 0L
    var totalMs = 0L
    var currentStart = intervals.first().first
    var currentEnd = intervals.first().second
    for ((start, end) in intervals.drop(1)) {
        if (start.isAfter(currentEnd)) {
            totalMs += currentEnd.toEpochMilli() - currentStart.toEpochMilli()
            currentStart = start
            currentEnd = end
        } else if (end.isAfter(currentEnd)) {
            currentEnd = end
        }
    }
    return totalMs + (currentEnd.toEpochMilli() - currentStart.toEpochMilli())
}

/**
 * Concatenates the stages of [orderedSessions], deduplicating identical
 * (start, end, type) triples, then fills each inter-segment gap no wider than
 * [maxGap] with an explicit Awake stage — a wake-split night keeps continuous
 * stage coverage and its schedule bar has no hole.
 */
fun combineNightStages(
    orderedSessions: List<SleepData>,
    maxGap: java.time.Duration,
): List<SleepStage> {
    val seen = mutableSetOf<Triple<java.time.Instant, java.time.Instant, Int>>()
    val stages = mutableListOf<SleepStage>()
    for (stage in orderedSessions.flatMap { it.stages }) {
        if (seen.add(Triple(stage.startTime, stage.endTime, stage.stageType))) {
            stages += stage
        }
    }
    if (stages.isEmpty()) return emptyList()
    // A gap between two sessions is time OUT of bed, not time awake in it. Typing it
    // STAGE_AWAKE made a 90-minute get-up read as wake-after-sleep-onset: it inflated the
    // Awake row, and through it sleep efficiency and the score's WASO term. STAGE_OUT_OF_BED
    // is excluded from AwakeStageTypes and from SleepScore.isAwakeStage, so the gap still
    // shows on the timeline without being counted as restless time in bed. Its overlap rank
    // is the lowest of any real stage, so a recorded stage always wins the disputed region.
    val gapStages = orderedSessions.zipWithNext().mapNotNull { (previous, next) ->
        val gap = java.time.Duration.between(previous.endTime, next.startTime)
        if (!gap.isNegative && gap > java.time.Duration.ZERO && gap <= maxGap) {
            SleepStage(previous.endTime, next.startTime, SleepStage.STAGE_OUT_OF_BED)
        } else {
            null
        }
    }
    return resolveSleepStages(
        stages = (stages + gapStages)
            .sortedWith(compareBy<SleepStage> { it.startTime }.thenBy { it.endTime }),
        sessionStart = orderedSessions.minOf { it.startTime },
        sessionEnd = orderedSessions.maxOf { it.endTime },
    )
}

/** Sum of stage durations, negative stages ignored. */
fun List<SleepStage>.totalStageMs(): Long = sumOf { it.durationMs.coerceAtLeast(0L) }

/** A fully-staged night is ~1.0; below this the hypnogram would mostly be a guess. */
const val MinSleepStageCoverage = 0.5

/**
 * Whether the session's stages cover enough of its span to draw a truthful
 * hypnogram. Coverage = summed stage time over the full session span.
 */
fun sleepSessionHasReliableStages(
    session: SleepData,
    minCoverage: Double = MinSleepStageCoverage,
): Boolean {
    if (session.stages.isEmpty()) return false
    val spanMs = session.endTime.toEpochMilli() - session.startTime.toEpochMilli()
    if (spanMs <= 0L) return false
    return session.stages.totalStageMs().toDouble() / spanMs >= minCoverage
}

/** Stage types shown as "Awake" in grouped breakdowns (in-bed awake, excluding out-of-bed). */
val AwakeStageTypes: Set<Int> = setOf(SleepStage.STAGE_AWAKE, SleepStage.STAGE_AWAKE_IN_BED)

/** Stage types shown as "Core" (Apple naming) — light plus generic sleeping. */
val CoreStageTypes: Set<Int> = setOf(SleepStage.STAGE_LIGHT, SleepStage.STAGE_SLEEPING)

/** Total duration of the stages whose [SleepStage.stageType] is in [types], overlaps counted once. */
fun List<SleepStage>.durationMsForTypes(types: Set<Int>): Long =
    filter { it.stageType in types }.unionDurationMs()

internal fun sleepDurationMsFromStages(
    stages: List<SleepStage>,
    fallbackDurationMs: Long,
): Long {
    if (stages.isEmpty()) return fallbackDurationMs.coerceAtLeast(0L)

    // Union, not a sum: a writer recording the same stretch under two stages
    // must never make asleep time exceed time in bed.
    val sleepStageDurationMs = stages
        .filter { it.stageType.isSleepDurationStage() }
        .unionDurationMs()

    return sleepStageDurationMs.takeIf { it > 0L } ?: fallbackDurationMs.coerceAtLeast(0L)
}

private fun Int.isSleepDurationStage(): Boolean = when (this) {
    SleepStage.STAGE_SLEEPING,
    SleepStage.STAGE_LIGHT,
    SleepStage.STAGE_DEEP,
    SleepStage.STAGE_REM -> true
    else -> false
}

/**
 * When a writer records overlapping stage intervals for one session — the
 * bug shape: one stretch stored as BOTH light and deep — every instant must
 * belong to exactly one stage. This resolves the overlaps the way Google Fit
 * renders the same data: the deeper stage wins the disputed region.
 */
private fun sleepStageOverlapRank(stageType: Int): Int = when (stageType) {
    SleepStage.STAGE_DEEP -> 7
    SleepStage.STAGE_REM -> 6
    SleepStage.STAGE_LIGHT -> 5
    SleepStage.STAGE_SLEEPING -> 4
    SleepStage.STAGE_AWAKE_IN_BED -> 3
    SleepStage.STAGE_AWAKE -> 2
    SleepStage.STAGE_OUT_OF_BED -> 1
    else -> 0
}

/**
 * The canonical stage timeline for a session: stages clipped to
 * [sessionStart, sessionEnd], overlaps resolved by [sleepStageOverlapRank],
 * adjacent fragments of one winner merged. All stored [SleepData.stages] pass
 * through here, so every duration, percentage, and hypnogram downstream
 * describes the same non-overlapping intervals. An already-clean list is
 * returned as-is.
 */
fun resolveSleepStages(
    stages: List<SleepStage>,
    sessionStart: Instant,
    sessionEnd: Instant,
): List<SleepStage> {
    if (stages.isEmpty()) return stages
    if (stagesAlreadyResolved(stages, sessionStart, sessionEnd)) return stages

    val clipped = stages.mapNotNull { stage ->
        val start = maxOf(stage.startTime, sessionStart)
        val end = minOf(stage.endTime, sessionEnd)
        if (end.isAfter(start)) stage.copy(startTime = start, endTime = end) else null
    }
    if (clipped.isEmpty()) return emptyList()

    val boundaries = clipped
        .flatMap { listOf(it.startTime, it.endTime) }
        .distinct()
        .sorted()
    val resolved = mutableListOf<SleepStage>()
    boundaries.zipWithNext().forEach { (segmentStart, segmentEnd) ->
        val winner = clipped
            .filter { !it.startTime.isAfter(segmentStart) && !it.endTime.isBefore(segmentEnd) }
            .maxByOrNull { sleepStageOverlapRank(it.stageType) }
            ?: return@forEach // an uncovered gap between stages stays a gap
        val previous = resolved.lastOrNull()
        if (previous != null && previous.stageType == winner.stageType && previous.endTime == segmentStart) {
            resolved[resolved.lastIndex] = previous.copy(endTime = segmentEnd)
        } else {
            resolved += SleepStage(segmentStart, segmentEnd, winner.stageType)
        }
    }
    return resolved
}

private fun stagesAlreadyResolved(
    stages: List<SleepStage>,
    sessionStart: Instant,
    sessionEnd: Instant,
): Boolean {
    var previousEnd = sessionStart
    for (stage in stages) {
        if (stage.startTime.isBefore(previousEnd)) return false
        if (!stage.endTime.isAfter(stage.startTime)) return false
        previousEnd = stage.endTime
    }
    return !previousEnd.isAfter(sessionEnd)
}

private fun List<SleepStage>.unionDurationMs(): Long {
    val intervals = filter { it.endTime.isAfter(it.startTime) }.sortedBy { it.startTime }
    if (intervals.isEmpty()) return 0L
    var totalMs = 0L
    var currentStart = intervals.first().startTime
    var currentEnd = intervals.first().endTime
    for (stage in intervals.drop(1)) {
        if (stage.startTime.isAfter(currentEnd)) {
            totalMs += currentEnd.toEpochMilli() - currentStart.toEpochMilli()
            currentStart = stage.startTime
            currentEnd = stage.endTime
        } else if (stage.endTime.isAfter(currentEnd)) {
            currentEnd = stage.endTime
        }
    }
    return totalMs + (currentEnd.toEpochMilli() - currentStart.toEpochMilli())
}
