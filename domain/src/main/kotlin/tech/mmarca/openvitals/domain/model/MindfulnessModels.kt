package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class MindfulnessSession(
    val id: String,
    val title: String?,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long,
    val source: String,
    val isOpenVitalsEntry: Boolean = false,
    val notes: String? = null,
) {
    val durationMinutes: Long get() = durationMs / 60_000
}

/** Minutes per local day, each session counted on the day it starts. The day value everywhere. */
fun List<MindfulnessSession>.minutesByStartDate(zone: ZoneId): Map<LocalDate, Double> =
    groupBy { it.startTime.atZone(zone).toLocalDate() }
        .mapValues { (_, sessions) -> sessions.sumOf { it.durationMs.coerceAtLeast(0L) } / 60_000.0 }

enum class MindfulnessBellSound {
    STRUCK,
    RUBBED,
    BRIGHT,
    TEMPLE,
    HARMONY,
}

enum class MindfulnessBackgroundSound {
    NONE,
    BOWL,
    MEDITATION,
    CHIMES,
    DREAMSCAPE,
}

data class MindfulnessTimerConfig(
    val durationMinutes: Int,
    val intervalMinutes: Int?,
    val bellSound: MindfulnessBellSound,
    val backgroundSound: MindfulnessBackgroundSound = MindfulnessBackgroundSound.NONE,
)

data class MindfulnessSessionWriteRequest(
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val notes: String? = null,
)
