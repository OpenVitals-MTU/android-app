package tech.mmarca.openvitals.wear

import java.util.TreeMap
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Folds accelerometer readings, heart rate samples, the worn and charging
 * state and screen wake-ups into one `SM` row per clock minute, exactly as
 * `docs/engineering/sleep-minute-features.md` specifies. The Python mirror
 * is `tool/sleep_accel_fixture/features.py`; `SleepMinuteFeatureVectorTest`
 * holds the two together.
 *
 * Readings arrive in g. Pure Kotlin, so a test can feed it. A minute closes
 * once [Config.closeLagMillis] have passed since its start, by which time
 * the sensor hub has flushed every batch that belongs to it.
 */
class MinuteAggregator(
    /** The watch's UTC offset at a minute, seconds. */
    private val offsetSecondsAt: (epochMillis: Long) -> Int,
    /** Whether heart rate is being recorded, read when a minute closes. */
    private val isHeartRateRecording: () -> Boolean,
    private val config: Config = Config(),
) {

    data class Config(
        /** In g. Below this a magnitude change is sensor noise, not a movement. */
        val jerkThresholdG: Double = 0.051,
        /** How much movement one counted reading is worth, at the nominal rate. */
        val movementPerCount: Double = 0.2,
        /** Fewer rate-normalised counts than this is a still minute. */
        val minCounts: Double = 3.0,
        /** The movement a minute saturates at. */
        val maxMovement: Double = 30.0,
        /** Fewer samples than this and the hub dropped part of the minute. */
        val sparseSamples: Int = 150,
        /** A minute closes this long after it started, once every batch for it has arrived. */
        val closeLagMillis: Long = 90_000L,
    )

    private class Bucket(start: Long) {
        val features = AccelerationMinuteFeatures(start)
        var counts = 0
        val heartRates = ArrayList<Double>(8)
        var charging = false
        var offBody = false
        var screenOn = false
        var heartRateNoContact = false
    }

    private val buckets = TreeMap<Long, Bucket>()
    private var lastMagnitude = Double.NaN
    private var lastAngle: Double? = null
    private var worn = true
    private var charging = false

    fun onAcceleration(epochMillis: Long, xG: Double, yG: Double, zG: Double) {
        val bucket = bucketAt(epochMillis)
        bucket.features.add(epochMillis, xG, yG, zG)
        val magnitude = sqrt(xG * xG + yG * yG + zG * zG)
        if (!lastMagnitude.isNaN() && abs(magnitude - lastMagnitude) > config.jerkThresholdG) bucket.counts++
        lastMagnitude = magnitude
        if (!worn) bucket.offBody = true
        if (charging) bucket.charging = true
    }

    /** A stored heart rate sample; never a no-contact reading. */
    fun onHeartRate(epochMillis: Long, bpm: Int) {
        bucketAt(epochMillis).heartRates += bpm.toDouble()
    }

    /** The off-body sensor's verdict. Sticky until the next one. */
    fun onWorn(epochMillis: Long, worn: Boolean) {
        this.worn = worn
        if (!worn) bucketAt(epochMillis).offBody = true
    }

    /** The charger plugged or unplugged. Sticky until the next one. */
    fun onCharging(epochMillis: Long, charging: Boolean) {
        this.charging = charging
        if (charging) bucketAt(epochMillis).charging = true
    }

    /** The screen became interactive: the wearer looked at the watch, so the minute is awake. */
    fun onScreenOn(epochMillis: Long) {
        bucketAt(epochMillis).screenOn = true
    }

    /** The heart rate sensor reported no contact, an unreliable reading or 0 bpm. */
    fun onHeartRateContact(epochMillis: Long, contact: Boolean) {
        if (!contact) bucketAt(epochMillis).heartRateNoContact = true
    }

    /** Closes and returns every minute old enough as of [nowEpochMillis], oldest first. */
    fun close(nowEpochMillis: Long): List<WearLinkProtocol.SleepMinute> {
        val closed = ArrayList<WearLinkProtocol.SleepMinute>()
        val iterator = buckets.entries.iterator()
        while (iterator.hasNext()) {
            val (start, bucket) = iterator.next()
            if (nowEpochMillis - start < config.closeLagMillis) break
            closed += row(start, bucket)
            iterator.remove()
        }
        return closed
    }

    private fun row(start: Long, bucket: Bucket): WearLinkProtocol.SleepMinute {
        val accel = bucket.features.close()
        val n = accel.sampleCount
        val countsNorm = if (n > 0) bucket.counts * AccelerationMinuteFeatures.NOMINAL_SAMPLES.toDouble() / n else 0.0
        val movement = if (countsNorm < config.minCounts) 0.0 else minOf(config.maxMovement, config.movementPerCount * countsNorm)

        val diffs = ArrayList<Double>(accel.angles.size)
        var previous = lastAngle
        for (angle in accel.angles) {
            if (previous != null) diffs += abs(angle - previous)
            previous = angle
        }
        if (accel.angles.isNotEmpty()) lastAngle = accel.angles.last()

        var flags = 0
        if (bucket.charging) flags = flags or WearLinkProtocol.FLAG_CHARGING
        if (bucket.offBody) flags = flags or WearLinkProtocol.FLAG_OFF_BODY
        if (bucket.screenOn) flags = flags or WearLinkProtocol.FLAG_SCREEN_ON
        if (bucket.heartRateNoContact) flags = flags or WearLinkProtocol.FLAG_HR_NO_CONTACT
        val heartRateRecording = isHeartRateRecording()
        if (heartRateRecording) flags = flags or WearLinkProtocol.FLAG_HR_RECORDING
        if (n < config.sparseSamples) flags = flags or WearLinkProtocol.FLAG_SPARSE

        val hn = bucket.heartRates.size
        val kind = when {
            bucket.charging || bucket.offBody || (heartRateRecording && hn == 0) -> WearLinkProtocol.MinuteKind.UNMEASURABLE
            bucket.screenOn -> WearLinkProtocol.MinuteKind.AWAKE
            else -> WearLinkProtocol.MinuteKind.RAW
        }
        val (heartMean, heartSd) = AccelerationMinuteFeatures.meanSd(bucket.heartRates)

        return WearLinkProtocol.SleepMinute(
            epochMillis = start,
            kind = kind,
            offsetSeconds = offsetSecondsAt(start),
            flags = flags,
            sampleCount = n,
            movement10 = round(movement * 10),
            bpm = if (hn > 0) round(heartMean) else null,
            heartRateSd10 = if (hn > 0) round(heartSd * 10) else null,
            heartRateSamples = hn,
            meanMilliG = IntArray(3) { round(accel.meanG[it] * 1000) },
            sdMilliG = IntArray(3) { round(accel.sdG[it] * 1000) },
            zAngleMin = accel.angles.minOrNull()?.let(::round),
            zAngleMax = accel.angles.maxOrNull()?.let(::round),
            zAngleDelta10 = if (diffs.isEmpty()) null else round(diffs.sum() / diffs.size * 10),
        )
    }

    private fun bucketAt(epochMillis: Long): Bucket {
        val start = epochMillis - epochMillis % MINUTE_MILLIS
        return buckets.getOrPut(start) {
            Bucket(start).also {
                it.charging = charging
                it.offBody = !worn
            }
        }
    }

    companion object {
        const val MINUTE_MILLIS = 60_000L

        /** `floor(x + 0.5)`, the one rounding the specification allows. */
        fun round(value: Double): Int = Math.round(value).toInt()
    }
}
