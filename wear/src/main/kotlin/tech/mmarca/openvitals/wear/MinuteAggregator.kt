package tech.mmarca.openvitals.wear

import java.util.TreeMap
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Folds accelerometer readings, the worn state and screen wake-ups into one
 * row per clock minute: the movement input of the phone's sleep estimator,
 * on the scale the estimator was fitted on.
 *
 * Movement is an actigraphy count: the number of readings whose acceleration
 * magnitude jumped by more than [Config.jerkThreshold] since the previous
 * reading, times [Config.movementPerCount]. A still wrist counts nothing; a
 * turn in bed counts a short burst; a waking wrist counts most of the minute.
 * Garmin's own per-minute movement, which the estimator's thresholds come
 * from, is zero for a still minute and around ten for a restless one, so the
 * scale is set to land there. Both constants are the knobs to turn once a
 * night of real data is in.
 *
 * Pure Kotlin, so a test can feed it readings. A minute closes once
 * [Config.closeLagMillis] have passed since its start, by which time the
 * sensor hub has flushed every batch that belongs to it.
 */
class MinuteAggregator(private val config: Config = Config()) {

    data class Config(
        /** Metres per second squared. Below this a magnitude change is sensor noise, not a movement. */
        val jerkThreshold: Float = 0.5f,
        /** How much movement one counted reading is worth. */
        val movementPerCount: Float = 0.2f,
        /** Fewer counted readings than this is a still minute. */
        val minCounts: Int = 3,
        /** The movement a minute saturates at. */
        val maxMovement: Float = 30f,
        /** A minute closes this long after it started, once every batch for it has arrived. */
        val closeLagMillis: Long = 90_000L,
    )

    /** One closed minute. [offBody] is true when the watch was off the wrist at any point of it. */
    data class ClosedMinute(
        val startEpochMillis: Long,
        val movement: Float,
        val offBody: Boolean,
        val screenOn: Boolean,
    )

    private class Bucket {
        var counts = 0
        var offBody = false
        var screenOn = false
    }

    private val buckets = TreeMap<Long, Bucket>()
    private var lastMagnitude = Float.NaN
    private var worn = true

    fun onAcceleration(epochMillis: Long, x: Float, y: Float, z: Float) {
        val magnitude = sqrt(x * x + y * y + z * z)
        val bucket = bucketAt(epochMillis)
        if (!lastMagnitude.isNaN() && abs(magnitude - lastMagnitude) > config.jerkThreshold) bucket.counts++
        lastMagnitude = magnitude
        if (!worn) bucket.offBody = true
    }

    /** The off-body sensor's verdict. Sticky until the next one. */
    fun onWorn(epochMillis: Long, worn: Boolean) {
        this.worn = worn
        if (!worn) bucketAt(epochMillis).offBody = true
    }

    /** The screen became interactive: the wearer looked at the watch, so the minute is awake. */
    fun onScreenOn(epochMillis: Long) {
        bucketAt(epochMillis).screenOn = true
    }

    /** Closes and returns every minute old enough as of [nowEpochMillis], oldest first. */
    fun close(nowEpochMillis: Long): List<ClosedMinute> {
        val closed = ArrayList<ClosedMinute>()
        val iterator = buckets.entries.iterator()
        while (iterator.hasNext()) {
            val (start, bucket) = iterator.next()
            if (nowEpochMillis - start < config.closeLagMillis) break
            closed += ClosedMinute(
                startEpochMillis = start,
                movement = movementOf(bucket.counts),
                offBody = bucket.offBody,
                screenOn = bucket.screenOn,
            )
            iterator.remove()
        }
        return closed
    }

    private fun movementOf(counts: Int): Float =
        if (counts < config.minCounts) 0f else (counts * config.movementPerCount).coerceAtMost(config.maxMovement)

    private fun bucketAt(epochMillis: Long): Bucket =
        buckets.getOrPut(epochMillis - epochMillis % MINUTE_MILLIS) { Bucket() }

    companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
