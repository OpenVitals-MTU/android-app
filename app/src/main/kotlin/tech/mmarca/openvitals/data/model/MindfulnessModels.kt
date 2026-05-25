package tech.mmarca.openvitals.data.model

import java.time.Instant

data class MindfulnessSession(
    val id: String,
    val title: String?,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long,
    val source: String,
) {
    val durationMinutes: Long get() = durationMs / 60_000
}

enum class MindfulnessBellSound {
    STRUCK,
    RUBBED,
    BRIGHT,
    TEMPLE,
    HARMONY,
}

data class MindfulnessTimerConfig(
    val durationMinutes: Int,
    val intervalMinutes: Int?,
    val bellSound: MindfulnessBellSound,
)

data class MindfulnessSessionWriteRequest(
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
)
