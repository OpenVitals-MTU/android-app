package tech.mmarca.openvitals.wear

import kotlin.math.pow
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

/**
 * Turns barometer readings into floors climbed per clock minute. Readings
 * are averaged over 10 seconds and turned into altitude. Each 3 m above the
 * lowest altitude since the last floor is one floor; going down moves that
 * low point down.
 *
 * Every ascent counts, an elevator ride and a slow drift of the weather
 * too. The phone keeps only minutes with steps; see `WearOsFloorsImport`.
 *
 * Pure, so it is testable off the watch. [FloorRecorder] is the Android side.
 */
class FloorMinuteCounter(
    /** The newest stored minute, so a restart inside a minute adds to it instead of replacing it. */
    latest: WearLinkProtocol.FloorMinute?,
) {
    private var minute: WearLinkProtocol.FloorMinute? = latest
    private var bucketStart = NO_BUCKET
    private var bucketSum = 0.0
    private var bucketCount = 0
    private var lowest: Double? = null

    /** The minute's row to store after a reading of [hectopascals] at [epochMillis], or null when no floor was climbed. */
    fun onPressure(epochMillis: Long, hectopascals: Float): WearLinkProtocol.FloorMinute? {
        if (!hectopascals.isFinite() || hectopascals <= 0f) return null
        val bucket = epochMillis - Math.floorMod(epochMillis, SMOOTHING_MILLIS)
        var row: WearLinkProtocol.FloorMinute? = null
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

    private fun onAltitude(epochMillis: Long, metres: Double): WearLinkProtocol.FloorMinute? {
        val low = lowest
        if (low == null || metres <= low) {
            lowest = metres
            return null
        }
        val floors = ((metres - low) / METRES_PER_FLOOR).toInt()
        if (floors == 0) return null
        lowest = low + floors * METRES_PER_FLOOR
        val start = epochMillis - Math.floorMod(epochMillis, MINUTE_MILLIS)
        val before = minute?.takeIf { it.epochMillis == start }?.floors ?: 0
        val total = (before + floors).coerceAtMost(WearLinkProtocol.MAX_FLOORS_PER_MINUTE)
        return WearLinkProtocol.FloorMinute(start, total).also { minute = it }
    }

    companion object {
        /** One floor: 3 m, about 10 ft. */
        const val METRES_PER_FLOOR = 3.0

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
