package tech.mmarca.openvitals.devices.garmin

import java.time.Instant

/**
 * Garmin's epoch: 1989-12-31T00:00:00Z. Directory entries use seconds
 * since it; a wire value of 0 is the "no date" sentinel.
 */
object GarminTime {

    const val GARMIN_EPOCH_SECONDS = 631065600L

    /** A Garmin device timestamp (seconds since the Garmin epoch) as a UTC [Instant]. */
    fun toInstant(garminTimestamp: Long): Instant =
        Instant.ofEpochSecond(garminTimestamp + GARMIN_EPOCH_SECONDS)

    /** An instant as a Garmin timestamp, for telling the watch the time. */
    fun fromInstant(time: Instant): Long = time.epochSecond - GARMIN_EPOCH_SECONDS
}
