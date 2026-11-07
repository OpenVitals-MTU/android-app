package tech.mmarca.openvitals.wear

import tech.mmarca.openvitals.wearlink.WearLinkProtocol.HeartRateSample

/** The heart rate tile's headline: the newest sample and how old it is, or why there is none. */
internal sealed interface TileHeartRate {
    data object NotGranted : TileHeartRate

    /** No sample yet, or only one older than [STALE_AFTER_MILLIS]: off the wrist, or charging. */
    data object None : TileHeartRate

    data class Recent(val bpm: Int, val minutesAgo: Long) : TileHeartRate

    companion object {
        /** A heart rate older than an hour says nothing about now. */
        const val STALE_AFTER_MILLIS: Long = 60L * 60 * 1000

        fun of(granted: Boolean, latest: HeartRateSample?, nowMillis: Long): TileHeartRate {
            if (!granted) return NotGranted
            val sample = latest ?: return None
            val age = (nowMillis - sample.epochMillis).coerceAtLeast(0)
            if (age > STALE_AFTER_MILLIS) return None
            return Recent(sample.bpm, age / TileDay.MINUTE_MILLIS)
        }
    }
}
