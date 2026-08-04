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
fun SleepData.asleepDurationMs(): Long {
    if (stages.isEmpty()) return durationMs
    val asleepMs = stages
        .filter { it.stageType.isAsleepStageType() }
        .sumOf { it.durationMs }
    return if (asleepMs > 0L) asleepMs else durationMs
}

private fun Int.isAsleepStageType(): Boolean = when (this) {
    SleepStage.STAGE_SLEEPING,
    SleepStage.STAGE_LIGHT,
    SleepStage.STAGE_DEEP,
    SleepStage.STAGE_REM,
    -> true
    else -> false
}

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
    val gapStages = orderedSessions.zipWithNext().mapNotNull { (previous, next) ->
        val gap = java.time.Duration.between(previous.endTime, next.startTime)
        if (!gap.isNegative && gap > java.time.Duration.ZERO && gap <= maxGap) {
            SleepStage(previous.endTime, next.startTime, SleepStage.STAGE_AWAKE)
        } else {
            null
        }
    }
    return (stages + gapStages)
        .sortedWith(compareBy<SleepStage> { it.startTime }.thenBy { it.endTime })
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

/** Total duration of the stages whose [SleepStage.stageType] is in [types]. */
fun List<SleepStage>.durationMsForTypes(types: Set<Int>): Long =
    filter { it.stageType in types }.sumOf { it.durationMs.coerceAtLeast(0L) }

internal fun sleepDurationMsFromStages(
    stages: List<SleepStage>,
    fallbackDurationMs: Long,
): Long {
    if (stages.isEmpty()) return fallbackDurationMs.coerceAtLeast(0L)

    val sleepStageDurationMs = stages
        .filter { it.stageType.isSleepDurationStage() }
        .sumOf { it.durationMs.coerceAtLeast(0L) }

    return sleepStageDurationMs.takeIf { it > 0L } ?: fallbackDurationMs.coerceAtLeast(0L)
}

private fun Int.isSleepDurationStage(): Boolean = when (this) {
    SleepStage.STAGE_SLEEPING,
    SleepStage.STAGE_LIGHT,
    SleepStage.STAGE_DEEP,
    SleepStage.STAGE_REM -> true
    else -> false
}
