package tech.mmarca.openvitals.devices.garmin

import java.time.Instant

/**
 * Garmin's device epoch: 1989-12-31T00:00:00Z, i.e. 631065600 seconds after
 * the Unix epoch. Port of `GarminTimeUtils.GARMIN_TIME_EPOCH`.
 *
 * Directory entries timestamp files in seconds since this epoch. A wire value
 * of 0 is the watch's "no date" sentinel and is surfaced as null by the
 * caller, never as a real instant at the Garmin epoch.
 */
object GarminTime {

    const val GARMIN_EPOCH_SECONDS = 631065600L

    /** A Garmin device timestamp (seconds since the Garmin epoch) as a UTC [Instant]. */
    fun toInstant(garminTimestamp: Long): Instant =
        Instant.ofEpochSecond(garminTimestamp + GARMIN_EPOCH_SECONDS)

    /**
     * The inverse — an instant as a Garmin timestamp. Needed when the sync
     * tells the watch the current time.
     */
    fun fromInstant(time: Instant): Long = time.epochSecond - GARMIN_EPOCH_SECONDS
}
