package tech.mmarca.openvitals.wear

import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * The accelerometer part of one minute's features, per
 * `docs/engineering/sleep-minute-features.md`: per-axis mean and standard
 * deviation, and the z-angle per five-second epoch. Samples arrive in g.
 * Pure Kotlin; `MinuteAggregator` owns one per open minute and the jerk
 * count, which crosses minute boundaries.
 */
internal class AccelerationMinuteFeatures(private val startEpochMillis: Long) {

    /** What one closed minute yields. Angles in degrees; null when no epoch was valid. */
    class Result(
        val sampleCount: Int,
        val meanG: DoubleArray,
        val sdG: DoubleArray,
        /** The valid epochs' angles, in epoch order. */
        val angles: List<Double>,
    )

    private val x = ArrayList<Double>(NOMINAL_SAMPLES)
    private val y = ArrayList<Double>(NOMINAL_SAMPLES)
    private val z = ArrayList<Double>(NOMINAL_SAMPLES)
    private val epochs = Array(EPOCHS_PER_MINUTE) { Triple(ArrayList<Double>(), ArrayList<Double>(), ArrayList<Double>()) }

    val sampleCount: Int get() = x.size

    fun add(epochMillis: Long, ax: Double, ay: Double, az: Double) {
        x += ax
        y += ay
        z += az
        val epoch = ((epochMillis - startEpochMillis) / EPOCH_MILLIS).toInt()
        if (epoch in 0 until EPOCHS_PER_MINUTE) {
            epochs[epoch].first += ax
            epochs[epoch].second += ay
            epochs[epoch].third += az
        }
    }

    fun close(): Result {
        val (mx, sx) = meanSd(x)
        val (my, sy) = meanSd(y)
        val (mz, sz) = meanSd(z)
        val angles = ArrayList<Double>(EPOCHS_PER_MINUTE)
        for ((ex, ey, ez) in epochs) {
            if (ez.size < MIN_EPOCH_SAMPLES) continue
            val medX = median(ex)
            val medY = median(ey)
            val medZ = median(ez)
            angles += atan2(medZ, sqrt(medX * medX + medY * medY)) * 180.0 / Math.PI
        }
        return Result(x.size, doubleArrayOf(mx, my, mz), doubleArrayOf(sx, sy, sz), angles)
    }

    companion object {
        const val NOMINAL_SAMPLES = 300
        const val EPOCH_MILLIS = 5_000L
        const val EPOCHS_PER_MINUTE = 12
        const val MIN_EPOCH_SAMPLES = 3

        /** Two-pass population mean and standard deviation; zeros for no samples. */
        fun meanSd(values: List<Double>): Pair<Double, Double> {
            val n = values.size
            if (n == 0) return 0.0 to 0.0
            var sum = 0.0
            for (v in values) sum += v
            val mean = sum / n
            var squares = 0.0
            for (v in values) squares += (v - mean) * (v - mean)
            val variance = squares / n
            return mean to if (variance > 0.0) sqrt(variance) else 0.0
        }

        /** The middle value, or the mean of the two middle values. */
        fun median(values: List<Double>): Double {
            val ordered = values.sorted()
            val n = ordered.size
            return if (n % 2 == 1) ordered[n / 2] else (ordered[n / 2 - 1] + ordered[n / 2]) / 2.0
        }
    }
}
