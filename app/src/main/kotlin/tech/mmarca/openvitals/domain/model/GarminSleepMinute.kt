package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.ZoneOffset

/** What a watch that does not stage sleep said about one minute. */
enum class SleepMinuteKind(val storageName: String) {
    /** A feature row: heart rate and movement, left for the phone to classify. */
    RAW("raw"),

    /** The watch was sure the wearer was awake. */
    AWAKE("awake"),

    /** Not worn, or no signal. */
    UNMEASURABLE("unmeasurable"),
    ;

    companion object {
        fun fromStorageName(name: String): SleepMinuteKind? = entries.firstOrNull { it.storageName == name }
    }
}

/** The estimator's input for one minute. Only movement and heart rate are used. */
data class SleepMinute(
    val time: Instant,
    val kind: SleepMinuteKind,
    /** Movement count for the minute; zero when still. Ignored unless [kind] is RAW. */
    val movement: Float = 0f,
    /** Beats per minute, or null when the row carried none. */
    val heartRate: Float? = null,
)

/**
 * One stored minute of unstaged Garmin sleep data. Health Connect has no
 * record type for per-minute movement, so this lives in the app's own
 * database as estimator input until the night is written as a session.
 */
data class GarminSleepMinute(
    val time: Instant,
    val kind: SleepMinuteKind,
    val heartRate: Double?,
    val movement: Double?,
    /** Activity magnitude. Stored for later refinements; not used by the estimator. */
    val activity: Double?,
    /** The watch's UTC offset at that minute. */
    val zoneOffset: ZoneOffset,
    /** The ten raw float16 features of a RAW row, in file order. */
    val features: FloatArray?,
) {
    fun toSleepMinute(): SleepMinute = SleepMinute(
        time = time,
        kind = kind,
        movement = movement?.toFloat() ?: 0f,
        heartRate = heartRate?.toFloat(),
    )
}
