package tech.mmarca.openvitals.wear

import kotlin.math.pow
import kotlin.math.roundToInt
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

/**
 * Turns barometer readings into the height climbed per clock minute, in
 * decimetres. Readings are averaged over 10 seconds and turned into
 * altitude. A rise counts once it reaches [DEAD_BAND_METRES] above the
 * lowest altitude since the last counted rise, and then with all of its
 * metres; going down moves that low point down. The dead band keeps the
 * barometer's wobble of about a metre from adding up.
 *
 * Every ascent counts, an elevator ride and a slow drift of the weather
 * too. The phone keeps only minutes with steps; see `WearOsFloorsImport`.
 *
 * Pure, so it is testable off the watch. [AscentRecorder] is the Android side.
 */
class AscentMinuteCounter(
    /** The newest stored minute, so a restart inside a minute adds to it instead of replacing it. */
    latest: WearLinkProtocol.AscentMinute?,
) {
    private var minute: WearLinkProtocol.AscentMinute? = latest
    private var bucketStart = NO_BUCKET
    private var bucketSum = 0.0
    private var bucketCount = 0
    private var lowest: Double? = null

    /** The minute's row to store after a reading of [hectopascals] at [epochMillis], or null when no rise was counted. */
    fun onPressure(epochMillis: Long, hectopascals: Float): WearLinkProtocol.AscentMinute? {
        if (!hectopascals.isFinite() || hectopascals <= 0f) return null
        val bucket = epochMillis - Math.floorMod(epochMillis, SMOOTHING_MILLIS)
        var row: WearLinkProtocol.AscentMinute? = null
        if (bucket != bucketStart) {
            if (bucketCount > 0) row = onAltitude(bucketStart, altitudeMetres(bucketSum / bucketCount))
            // After a gap the low point is stale: the weather moved it.
            if (bucketStart != NO_BUCKET && bucket - bucketStart > MAX_GAP_MILLIS) lowest = null
            bucketStart = bucket
            bucketSum = 0.0
            bucketCount = 0
        }
        bucketSum += hectopascals
        bucketCount++
        return row
    }

    private fun onAltitude(epochMillis: Long, metres: Double): WearLinkProtocol.AscentMinute? {
        val low = lowest
        if (low == null || metres <= low) {
            lowest = metres
            return null
        }
        if (metres - low < DEAD_BAND_METRES) return null
        lowest = metres
        val decimetres = ((metres - low) * 10).roundToInt()
        val start = epochMillis - Math.floorMod(epochMillis, MINUTE_MILLIS)
        val before = minute?.takeIf { it.epochMillis == start }?.decimetres ?: 0
        val total = (before + decimetres).coerceAtMost(WearLinkProtocol.MAX_ASCENT_DECIMETRES_PER_MINUTE)
        return WearLinkProtocol.AscentMinute(start, total).also { minute = it }
    }

    companion object {
        /** A rise smaller than this is not counted: the barometer wobbles by about a metre. Same as one floor, 3 m. */
        const val DEAD_BAND_METRES = 3.0

        const val SMOOTHING_MILLIS = 10_000L

        /** Longer without a reading, and the next one starts a new low point. */
        const val MAX_GAP_MILLIS = 2 * 60_000L

        private const val MINUTE_MILLIS = 60_000L
        private const val NO_BUCKET = Long.MIN_VALUE

        /** The international barometric formula against standard sea-level pressure, as `SensorManager.getAltitude`. */
        fun altitudeMetres(hectopascals: Double): Double =
            44_330.0 * (1.0 - (hectopascals / 1013.25).pow(1.0 / 5.255))
    }
}
