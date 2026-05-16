package tech.mmarca.openvitals.data.model

import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal data class SleepRangeWindow(
    val start: Instant,
    val end: Instant,
)

internal fun sleepRangeWindowFor(
    selectedDate: LocalDate,
    sleepRangeMode: SleepRangeMode,
    zone: ZoneId = ZoneId.systemDefault(),
): SleepRangeWindow {
    val start = when (sleepRangeMode) {
        SleepRangeMode.ROLLING_24H -> selectedDate.atStartOfDay()
        SleepRangeMode.NOON -> selectedDate.minusDays(1).atTime(LocalTime.NOON)
        SleepRangeMode.EVENING_18H -> selectedDate.minusDays(1).atTime(18, 0)
    }
    val end = when (sleepRangeMode) {
        SleepRangeMode.ROLLING_24H -> selectedDate.plusDays(1).atStartOfDay()
        SleepRangeMode.NOON -> selectedDate.atTime(LocalTime.NOON)
        SleepRangeMode.EVENING_18H -> selectedDate.atTime(18, 0)
    }
    return SleepRangeWindow(
        start = start.atZone(zone).toInstant(),
        end = end.atZone(zone).toInstant(),
    )
}

internal fun sleepSessionsForRange(
    sessions: List<SleepData>,
    selectedDate: LocalDate,
    sleepRangeMode: SleepRangeMode,
    zone: ZoneId = ZoneId.systemDefault(),
): List<SleepData> {
    val window = sleepRangeWindowFor(selectedDate, sleepRangeMode, zone)
    return sessions
        .filter { session -> window.containsEnd(session) }
        .sortedWith(compareBy<SleepData> { it.startTime }.thenBy { it.endTime })
}

internal fun dailySleepSummary(
    sessions: List<SleepData>,
    selectedDate: LocalDate,
    sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    zone: ZoneId = ZoneId.systemDefault(),
): SleepData? {
    val dailySessions = sleepSessionsForRange(
        sessions = sessions,
        selectedDate = selectedDate,
        sleepRangeMode = sleepRangeMode,
        zone = zone,
    )

    if (dailySessions.isEmpty()) return null
    if (dailySessions.size == 1) {
        return dailySessions.single().copy(stages = dailySessions.single().stages.sortedBy { it.startTime })
    }

    val first = dailySessions.first()
    val last = dailySessions.maxBy { it.endTime }
    val distinctSources = dailySessions.map { it.source }.distinct()

    return SleepData(
        id = "daily:${selectedDate}",
        startTime = first.startTime,
        endTime = last.endTime,
        durationMs = dailySessions.sumOf { it.durationMs.coerceAtLeast(0L) },
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
        stages = dailySessions
            .flatMap { it.stages }
            .distinctBy { stage -> Triple(stage.startTime, stage.endTime, stage.stageType) }
            .sortedWith(compareBy<SleepStage> { it.startTime }.thenBy { it.endTime }),
    )
}

private fun SleepRangeWindow.containsEnd(session: SleepData): Boolean =
    !session.endTime.isBefore(start) && session.endTime.isBefore(end)
