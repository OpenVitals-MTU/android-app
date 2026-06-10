package tech.mmarca.openvitals.domain.model

import java.time.Instant

data class MindfulnessSession(
    val id: String,
    val title: String?,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long,
    val source: String,
    val isOpenVitalsEntry: Boolean = false,
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
)
