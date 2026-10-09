package tech.mmarca.openvitals.wear.features.measure

import kotlin.math.abs
import kotlin.math.sqrt

/** Readings a heart rate measurement needs before it settles. */
internal const val HeartRateReadingsNeeded = 8

/** RR intervals an HRV reading needs; fewer means too short or too noisy. */
internal const val HrvIntervalsNeeded = 30

/**
 * The settled pulse from a run of readings: the median of the last five, so
 * one stray reading while the sensor locks on does not move it. Zero readings
 * mean "no signal" and are ignored.
 */
internal fun settledHeartRate(readings: List<Double>): Double? {
    val valid = readings.filter { it in 25.0..250.0 }
    if (valid.size < HeartRateReadingsNeeded) return null
    return valid.takeLast(5).sorted()[2]
}

/** Beat-to-beat intervals in milliseconds from beat timestamps in nanoseconds. */
internal fun rrIntervalsMillis(beatTimestampsNanos: List<Long>): List<Double> =
    beatTimestampsNanos.zipWithNext { a, b -> (b - a) / 1_000_000.0 }

/**
 * RMSSD in milliseconds, after dropping intervals no heart produces (outside
 * 300 to 2000 ms, 30 to 200 bpm) and ectopic beats that differ from the
 * previous kept interval by more than 20 %. Null when too few remain.
 */
internal fun rmssd(rrMillis: List<Double>): Double? {
    val clean = mutableListOf<Double>()
    rrMillis.filter { it in 300.0..2000.0 }.forEach { rr ->
        val previous = clean.lastOrNull()
        if (previous == null || abs(rr - previous) <= previous * 0.2) clean += rr
    }
    if (clean.size < HrvIntervalsNeeded) return null
    val squaredDiffs = clean.zipWithNext { a, b -> (b - a) * (b - a) }
    return sqrt(squaredDiffs.average())
}
