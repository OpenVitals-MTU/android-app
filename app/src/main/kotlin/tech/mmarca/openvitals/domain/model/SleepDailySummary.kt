package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import tech.mmarca.openvitals.domain.preferences.SleepWindow

/** Display-level synthetic id for a night built from two or more segments. */
const val MERGED_NIGHT_ID_PREFIX = "daily:"

/**
 * Two contiguous sleep clusters further apart than this are a night and a nap,
 * not one night.
 */
val SleepNapGap: Duration = Duration.ofHours(3)

internal data class SleepRangeWindow(
    val start: Instant,
    val end: Instant,
)

/**
 * Date D's night ends at D [SleepWindow.endHour] and starts at
 * [SleepWindow.startHour] — on D-1 when the window spans midnight (the 18:00 →
 * 10:00 default), on D itself when it does not (00:00 → 12:00) — a night lands
 * on its WAKE-UP date either way. Anchoring a same-day window's start on D-1
 * stretched every night to 36 hours: each date's window swallowed the
 * neighbouring night too, the longer of the two won the night, and the loser
 * was filed as that date's "nap".
 */
internal fun sleepRangeStartFor(
    selectedDate: LocalDate,
    sleepWindow: SleepWindow,
): LocalDateTime {
    val startHour = sleepWindow.startHour.coerceIn(0, 23)
    val spansMidnight = startHour >= sleepWindow.endHour.coerceIn(0, 23)
    val anchorDate = if (spansMidnight) selectedDate.minusDays(1) else selectedDate
    return anchorDate.atTime(startHour, 0)
}

internal fun sleepRangeEndFor(
    selectedDate: LocalDate,
    sleepWindow: SleepWindow,
): LocalDateTime = selectedDate.atTime(sleepWindow.endHour.coerceIn(0, 23), 0)

internal fun sleepRangeWindowFor(
    selectedDate: LocalDate,
    sleepWindow: SleepWindow,
    zone: ZoneId = ZoneId.systemDefault(),
): SleepRangeWindow = SleepRangeWindow(
    start = sleepRangeStartFor(selectedDate, sleepWindow).atZone(zone).toInstant(),
    end = sleepRangeEndFor(selectedDate, sleepWindow).atZone(zone).toInstant(),
)

/**
 * The sessions belonging to [selectedDate]'s night. Membership is by START
 * time only: a sleep-in running past the morning boundary stays with its
 * night instead of dropping out.
 */
internal fun sleepSessionsForRange(
    sessions: List<SleepData>,
    selectedDate: LocalDate,
    sleepWindow: SleepWindow,
    zone: ZoneId = ZoneId.systemDefault(),
): List<SleepData> {
    val window = sleepRangeWindowFor(selectedDate, sleepWindow, zone)
    return sessions
        .filter { session -> window.containsStart(session) }
        .sortedWith(sleepSessionOrder)
}

/** The result of splitting a night window's sessions into the night and its naps. */
internal data class SleepNightSplit(
    val night: List<SleepData>,
    val naps: List<SleepData>,
)

/**
 * Clusters the window's sessions by contiguity (gap ≤ [napGap], measured
 * against the cluster's running max end so nested sessions never split) and
 * keeps the cluster with the largest UNION of slept time as the night;
 * everything else is a nap.
 */
internal fun splitNightAndNaps(
    windowedSessions: List<SleepData>,
    napGap: Duration = SleepNapGap,
): SleepNightSplit {
    if (windowedSessions.isEmpty()) return SleepNightSplit(night = emptyList(), naps = emptyList())
    val sorted = windowedSessions.sortedWith(sleepSessionOrder)

    val clusters = mutableListOf<List<SleepData>>()
    var current = mutableListOf(sorted.first())
    var clusterEnd = sorted.first().endTime
    for (session in sorted.drop(1)) {
        if (Duration.between(clusterEnd, session.startTime) > napGap) {
            clusters += current
            current = mutableListOf(session)
            clusterEnd = session.endTime
        } else {
            current += session
            if (session.endTime.isAfter(clusterEnd)) clusterEnd = session.endTime
        }
    }
    clusters += current

    var night: List<SleepData> = emptyList()
    var bestTotal = -1L
    for (cluster in clusters) {
        val total = sleepSessionsUnionMs(cluster)
        if (total > bestTotal) {
            bestTotal = total
            night = cluster
        }
    }
    val naps = clusters
        .filterNot { it === night }
        .flatten()
        .sortedWith(sleepSessionOrder)
    return SleepNightSplit(night = night, naps = naps)
}

/**
 * Every nap belonging to [selectedDate]: the night window's non-night clusters
 * plus sessions starting in the daytime gap. Night and daytime tile the whole
 * day by start time, so nothing is ever dropped — the selected date's own
 * evening belongs to the NEXT date's night.
 */
internal fun dailyNaps(
    sessions: List<SleepData>,
    selectedDate: LocalDate,
    sleepWindow: SleepWindow = SleepWindow.Default,
    zone: ZoneId = ZoneId.systemDefault(),
): List<SleepData> {
    val nightNaps = splitNightAndNaps(
        sleepSessionsForRange(sessions, selectedDate, sleepWindow, zone),
    ).naps
    val daytimeStart = selectedDate.atTime(sleepWindow.endHour.coerceIn(0, 23), 0).atZone(zone).toInstant()
    // The gap runs to where the NEXT date's night begins — the same evening for
    // a midnight-spanning window, the next midnight for a same-day one.
    val daytimeEnd = sleepRangeStartFor(selectedDate.plusDays(1), sleepWindow).atZone(zone).toInstant()
    val daytimeNaps = sessions.filter { session ->
        !session.startTime.isBefore(daytimeStart) && session.startTime.isBefore(daytimeEnd)
    }
    return (nightNaps + daytimeNaps).sortedWith(sleepSessionOrder)
}

/**
 * The one canonical night for [selectedDate]: the window's night cluster,
 * naps excluded, duration as the UNION of slept intervals (overlapping
 * cross-source survivors can never exceed real time), and stages concatenated
 * with wake gaps filled. A single-segment night keeps its real id — and stays
 * tappable; a multi-segment night gets the synthetic un-openable
 * [MERGED_NIGHT_ID_PREFIX] id.
 */
internal fun dailySleepSummary(
    sessions: List<SleepData>,
    selectedDate: LocalDate,
    sleepWindow: SleepWindow = SleepWindow.Default,
    zone: ZoneId = ZoneId.systemDefault(),
): SleepData? {
    val windowed = sleepSessionsForRange(sessions, selectedDate, sleepWindow, zone)
    val dailySessions = splitNightAndNaps(windowed).night
    val nightDurationMs = sleepSessionsUnionMs(dailySessions)

    if (dailySessions.isEmpty()) return null
    if (dailySessions.size == 1) {
        val single = dailySessions.single()
        return single.copy(
            stages = single.stages.sortedBy { it.startTime },
            durationMs = nightDurationMs,
        )
    }

    val first = dailySessions.first()
    val last = dailySessions.maxBy { it.endTime }
    val distinctSources = dailySessions.map { it.source }.distinct()

    return SleepData(
        id = "$MERGED_NIGHT_ID_PREFIX$selectedDate",
        startTime = first.startTime,
        endTime = last.endTime,
        durationMs = nightDurationMs,
        source = distinctSources.singleOrNull() ?: first.source,
        title = dailySessions
            .mapNotNull { session -> session.title?.takeIf { it.isNotBlank() } }
            .distinct()
            .singleOrNull() ?: first.title,
        notes = dailySessions
            .mapNotNull { session -> session.notes?.takeIf { it.isNotBlank() } }
            .distinct()
            .singleOrNull(),
        startZoneOffset = first.startZoneOffset,
        endZoneOffset = last.endZoneOffset,
        lastModifiedTime = dailySessions.mapNotNull { it.lastModifiedTime }.maxOrNull(),
        clientRecordId = null,
        clientRecordVersion = null,
        recordingMethod = dailySessions.mapNotNull { it.recordingMethod }.distinct().singleOrNull(),
        device = dailySessions.mapNotNull { it.device }.distinct().singleOrNull(),
        stages = combineNightStages(dailySessions, maxGap = SleepNapGap),
    )
}

private val sleepSessionOrder =
    compareBy<SleepData> { it.startTime }.thenBy { it.endTime }

private fun SleepRangeWindow.containsStart(session: SleepData): Boolean =
    !session.startTime.isBefore(start) && session.startTime.isBefore(end)
