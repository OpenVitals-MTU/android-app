package tech.mmarca.openvitals.data.model

import java.time.Duration
import java.time.Instant
import java.util.Base64

private const val MERGED_SLEEP_SESSION_ID_PREFIX = "merged:"
private const val MERGED_SLEEP_SESSION_ID_SEPARATOR = "."
// Mirrors Gadgetbridge's sleep-session analysis: short quiet wake/no-data gaps keep one night together.
private val DEFAULT_SLEEP_SESSION_MERGE_GAP: Duration = Duration.ofMinutes(60)

internal fun mergeSleepSessions(
    sessions: List<SleepData>,
    maxGap: Duration = DEFAULT_SLEEP_SESSION_MERGE_GAP,
): List<SleepData> {
    if (sessions.size < 2) return sessions.sortedByDescending { it.endTime }

    val groups = mutableListOf<List<SleepData>>()
    val currentGroup = mutableListOf<SleepData>()

    sessions
        .sortedWith(compareBy<SleepData> { it.startTime }.thenBy { it.endTime })
        .forEach { session ->
            val currentEnd = currentGroup.maxOfOrNull { it.endTime }
            if (
                currentGroup.isEmpty() ||
                (currentEnd != null && shouldMergeSleepSessions(currentGroup, currentEnd, session, maxGap))
            ) {
                currentGroup += session
            } else {
                groups += currentGroup.toList()
                currentGroup.clear()
                currentGroup += session
            }
        }

    if (currentGroup.isNotEmpty()) groups += currentGroup.toList()

    return groups
        .map { group -> group.toMergedSleepSession(maxGap) }
        .sortedByDescending { it.endTime }
}

internal fun mergedSleepSessionComponentIds(id: String): List<String>? {
    if (!id.startsWith(MERGED_SLEEP_SESSION_ID_PREFIX)) return null
    val encodedIds = id
        .removePrefix(MERGED_SLEEP_SESSION_ID_PREFIX)
        .split(MERGED_SLEEP_SESSION_ID_SEPARATOR)
        .filter { it.isNotBlank() }

    if (encodedIds.isEmpty()) return null

    return runCatching {
        encodedIds.map { encodedId ->
            String(Base64.getUrlDecoder().decode(encodedId), Charsets.UTF_8)
        }
    }.getOrNull()
}

private fun shouldMergeSleepSessions(
    currentGroup: List<SleepData>,
    currentEnd: Instant,
    nextSession: SleepData,
    maxGap: Duration,
): Boolean {
    val source = currentGroup.firstOrNull()?.source ?: return false
    if (nextSession.source != source) return false

    val gap = Duration.between(currentEnd, nextSession.startTime)
    return gap <= maxGap
}

private fun List<SleepData>.toMergedSleepSession(maxGap: Duration): SleepData {
    if (size == 1) return single().copy(stages = single().stages.sortedBy { it.startTime })

    val ordered = sortedWith(compareBy<SleepData> { it.startTime }.thenBy { it.endTime })
    val first = ordered.first()
    val last = ordered.maxBy { it.endTime }
    val startTime = first.startTime
    val endTime = last.endTime
    val distinctSources = ordered.map { it.source }.distinct()

    return SleepData(
        id = mergedSleepSessionId(ordered.map { it.id }),
        startTime = startTime,
        endTime = endTime,
        durationMs = ordered.sumOf { it.durationMs.coerceAtLeast(0L) },
        source = distinctSources.singleOrNull() ?: first.source,
        title = ordered
            .mapNotNull { session -> session.title?.takeIf { it.isNotBlank() } }
            .distinct()
            .singleOrNull() ?: first.title,
        notes = ordered
            .mapNotNull { session -> session.notes?.takeIf { it.isNotBlank() } }
            .distinct()
            .singleOrNull(),
        startZoneOffset = first.startZoneOffset,
        endZoneOffset = last.endZoneOffset,
        lastModifiedTime = ordered.mapNotNull { it.lastModifiedTime }.maxOrNull(),
        clientRecordId = null,
        clientRecordVersion = null,
        recordingMethod = ordered.mapNotNull { it.recordingMethod }.distinct().singleOrNull(),
        device = ordered.mapNotNull { it.device }.distinct().singleOrNull(),
        stages = mergedSleepStages(ordered, maxGap),
    )
}

private fun mergedSleepSessionId(ids: List<String>): String =
    ids.distinct()
        .joinToString(MERGED_SLEEP_SESSION_ID_SEPARATOR) { id ->
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(id.toByteArray(Charsets.UTF_8))
        }
        .let { encodedIds -> "$MERGED_SLEEP_SESSION_ID_PREFIX$encodedIds" }

private fun mergedSleepStages(
    orderedSessions: List<SleepData>,
    maxGap: Duration,
): List<SleepStage> {
    val stages = orderedSessions
        .flatMap { it.stages }
        .distinctBy { stage -> Triple(stage.startTime, stage.endTime, stage.stageType) }

    if (stages.isEmpty()) return emptyList()

    val gapStages = orderedSessions
        .zipWithNext()
        .mapNotNull { (previous, next) ->
            val gap = Duration.between(previous.endTime, next.startTime)
            if (!gap.isNegative && gap > Duration.ZERO && gap <= maxGap) {
                SleepStage(
                    startTime = previous.endTime,
                    endTime = next.startTime,
                    stageType = SleepStage.STAGE_AWAKE,
                )
            } else {
                null
            }
        }

    return (stages + gapStages).sortedWith(
        compareBy<SleepStage> { it.startTime }.thenBy { it.endTime },
    )
}
