package tech.mmarca.openvitals.devices.garmin

import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The watch's live readings, as they arrive over the held link.
 *
 * In memory only, deliberately: these are a window on what the wrist is
 * doing RIGHT NOW, and a value that outlives the connection is a lie the UI
 * would keep telling. Everything worth keeping arrives later as FIT records
 * through the normal sync, with the watch's own timestamps and gap-filling.
 */
@Singleton
class GarminRealtimeStore @Inject constructor() {

    private val state = MutableStateFlow(GarminRealtimeState())
    val readings: StateFlow<GarminRealtimeState> = state

    fun record(reading: GarminRealtimeReading, now: Instant = Instant.now()) {
        state.value = when (reading) {
            is GarminRealtimeReading.HeartRate -> state.value.copy(
                heartRateBpm = reading.bpm,
                heartRateAt = now,
            )
            is GarminRealtimeReading.Steps -> state.value.copy(
                steps = reading.steps,
                stepGoal = reading.goal.takeIf { it > 0 },
                stepsAt = now,
            )
            is GarminRealtimeReading.Respiration -> state.value.copy(
                breathsPerMinute = reading.breathsPerMinute,
                respirationAt = now,
            )
            is GarminRealtimeReading.SpO2 -> state.value.copy(
                spo2Percent = reading.percent,
                spo2At = now,
            )
            // Beat-to-beat intervals arrive many times a second and mean
            // nothing on their own; they are for a future HRV calculation,
            // not for display, so nothing is stored yet.
            is GarminRealtimeReading.Hrv -> state.value
        }
    }

    /** Called when the link goes: live values must not outlive their link. */
    fun clear() {
        state.value = GarminRealtimeState()
    }
}

/**
 * The latest live values, each with when it arrived so a stale one can be
 * hidden rather than shown as current.
 */
data class GarminRealtimeState(
    val heartRateBpm: Int? = null,
    val heartRateAt: Instant? = null,
    val steps: Int? = null,
    val stepGoal: Int? = null,
    val stepsAt: Instant? = null,
    val breathsPerMinute: Int? = null,
    val respirationAt: Instant? = null,
    val spo2Percent: Int? = null,
    val spo2At: Instant? = null,
) {
    /**
     * Heart rate if it is recent enough to still describe the wearer. The
     * watch pushes on change, so a quiet minute is normal and a much longer
     * silence means the reading has stopped being live.
     */
    fun freshHeartRate(now: Instant = Instant.now()): Int? {
        val at = heartRateAt ?: return null
        if (Duration.between(at, now) > FRESHNESS) return null
        return heartRateBpm
    }

    fun freshSteps(now: Instant = Instant.now()): Int? {
        val at = stepsAt ?: return null
        if (Duration.between(at, now) > FRESHNESS) return null
        return steps
    }

    private companion object {
        val FRESHNESS: Duration = Duration.ofMinutes(2)
    }
}
