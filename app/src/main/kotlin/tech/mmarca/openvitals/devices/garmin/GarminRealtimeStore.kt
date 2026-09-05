package tech.mmarca.openvitals.devices.garmin

import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The watch's live readings over the held link. In memory only: a value
 * that outlives the connection is a lie. The FIT sync keeps the record.
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
            // Beat-to-beat intervals are for a future HRV calculation; not stored yet.
            is GarminRealtimeReading.Hrv -> state.value
        }
    }

    /** Called when the link goes: live values must not outlive their link. */
    fun clear() {
        state.value = GarminRealtimeState()
    }
}

/** The latest live values, each with its arrival time, so a stale one can be hidden. */
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
    /** Heart rate if recent enough to describe the wearer. The watch pushes on change. */
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
