package tech.mmarca.openvitals.domain.model

import java.time.Instant

/**
 * What one scale broadcast carried. A weigh-in arrives as two broadcasts,
 * one with the weight, the heart rate and the first impedance, one with the
 * second impedance, and either can be missed. So every field is optional.
 */
data class ScaleReading(
    val weightKg: Double? = null,
    val heartRateBpm: Int? = null,
    /** The 50 kHz reading, sent with the weight. */
    val impedanceLowOhm: Double? = null,
    /** The 250 kHz reading, sent on its own. */
    val impedanceHighOhm: Double? = null,
) {
    /** The scale broadcasts an empty reading once the person has stepped off. */
    val isEmpty: Boolean
        get() = weightKg == null && heartRateBpm == null && impedanceLowOhm == null && impedanceHighOhm == null
}

/**
 * One weigh-in as the scale measured it, built up from its broadcasts.
 * [scaleTimestamp] and [profile] are its identity: every broadcast of one
 * weigh-in repeats them.
 */
data class ScaleWeighIn(
    /** Seconds on the scale's own clock. An identity, not a trusted time. */
    val scaleTimestamp: Long,
    /** The user slot the scale assigned the weigh-in to. */
    val profile: Int,
    /** When the weigh-in happened. Fixed by the first broadcast. */
    val time: Instant,
    val reading: ScaleReading,
    /** Raised by every broadcast that added something. Doubles as the Health Connect record version. */
    val updatedMillis: Long,
    /** The [updatedMillis] that last reached Health Connect, or null while nothing has. */
    val writtenMillis: Long? = null,
) {
    /** Nothing can be written before the weight is in: the other records hang off it. */
    val isWritable: Boolean
        get() = reading.weightKg != null

    val isPending: Boolean
        get() = isWritable && (writtenMillis == null || writtenMillis < updatedMillis)

    /**
     * This weigh-in with what [next] adds or changes, or this same instance
     * when [next] brings nothing new. A value the scale restates wins.
     */
    fun mergedWith(next: ScaleReading, nowMillis: Long): ScaleWeighIn {
        val merged = ScaleReading(
            weightKg = next.weightKg ?: reading.weightKg,
            heartRateBpm = next.heartRateBpm ?: reading.heartRateBpm,
            impedanceLowOhm = next.impedanceLowOhm ?: reading.impedanceLowOhm,
            impedanceHighOhm = next.impedanceHighOhm ?: reading.impedanceHighOhm,
        )
        if (merged == reading) return this
        // Strictly rising, whatever the clock did: Health Connect ignores an equal version.
        return copy(reading = merged, updatedMillis = maxOf(nowMillis, updatedMillis + 1))
    }
}
