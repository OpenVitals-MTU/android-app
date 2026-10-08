package tech.mmarca.openvitals.domain.insights

import kotlin.math.abs
import kotlin.math.exp
import tech.mmarca.openvitals.domain.model.SleepMinuteKind

/**
 * Sleep or wake per grid minute, for the rows the watch records. Three
 * votes on one linear score, after Walch 2019, whose heart rate term lifted
 * wake specificity from 71% to 82% on polysomnography-labelled nights:
 *
 * - activity: the Cole-Kripke weighted movement divided by its threshold,
 *   so this term alone reproduces the Garmin path's boundary exactly and
 *   sleep sensitivity cannot drop below it;
 * - heart rate volatility: Walch's difference-of-Gaussians filter over the
 *   minute heart rate, its rolling standard deviation, normalised by the
 *   night's own 90th percentile of deviation from the mean (floored at a
 *   few beats, under which a flat night would amplify sensor noise);
 * - arm angle: one when the z-angle moved more than five degrees over the
 *   trailing five minutes (van Hees 2015), else zero.
 *
 * A minute the watch was not worn for is a gap; a minute the watch itself
 * called awake is wake. Webster's rescoring runs last, as for Cole-Kripke.
 * Pure and deterministic.
 */
object SleepWakeScorer {

    data class Config(
        /** Cole-Kripke one-minute weights for t-4..t+2, scaled so the centre is 1; Oakley's are a swap. */
        val weights: FloatArray = ColeKripkeWeights,
        val centre: Int = 4,
        val threshold: Float = 8f,
        /** Activity assigned to a minute the watch itself called awake. */
        val awakeActivity: Float = 10f,
        val angleWindowMinutes: Int = 5,
        val angleStillDeg: Float = 5f,
        /** Walch: σ 120 s and 600 s on a one-minute grid. */
        val heartRateSigmaShortMinutes: Float = 2f,
        val heartRateSigmaLongMinutes: Float = 10f,
        /** Half the rolling window of the volatility: 10 minutes. */
        val heartRateSdHalf: Int = 5,
        val heartRateNormPercentile: Float = 0.90f,
        /** Beats per minute. A night flatter than this is normalised by this instead. */
        val heartRateNormFloorBpm: Float = 3f,
        val activityWeight: Float = 1.0f,
        val heartRateVolatilityWeight: Float = 0.5f,
        val angleMovingWeight: Float = 0.3f,
        val wakeScore: Float = 1.0f,
        val websterPasses: Int = WebsterRescoring.DefaultPasses,
    )

    fun score(grid: WearMinuteGrid, wear: WearStates, config: Config = Config()): CharArray {
        val n = grid.size
        val activity = FloatArray(n) { index ->
            when {
                !wear.isWorn(index) -> 0f
                grid.kinds[index] == SleepMinuteKind.AWAKE -> config.awakeActivity
                else -> grid.movement[index]
            }
        }
        val volatility = heartRateVolatility(grid, wear, config)
        val labels = CharArray(n)
        for (index in 0 until n) {
            if (!wear.isWorn(index)) {
                labels[index] = SleepLabels.Gap
                continue
            }
            if (grid.kinds[index] == SleepMinuteKind.AWAKE) {
                labels[index] = SleepLabels.Wake
                continue
            }
            var weighted = 0f
            for (offset in config.weights.indices) {
                val at = index + offset - config.centre
                if (at in 0 until n) weighted += config.weights[offset] * activity[at]
            }
            val activityTerm = weighted / config.threshold
            val heartTerm = volatility[index].takeIf { !it.isNaN() } ?: 0f
            val angleTerm = if (angleMoved(grid, wear, index, config)) 1f else 0f
            val score = config.activityWeight * activityTerm +
                config.heartRateVolatilityWeight * heartTerm +
                config.angleMovingWeight * angleTerm
            labels[index] = if (score >= config.wakeScore) SleepLabels.Wake else SleepLabels.Sleep
        }
        WebsterRescoring.apply(labels, config.websterPasses)
        return labels
    }

    /** True when the z-angle range over the trailing window reaches the still threshold. */
    private fun angleMoved(grid: WearMinuteGrid, wear: WearStates, index: Int, config: Config): Boolean {
        var low = Float.POSITIVE_INFINITY
        var high = Float.NEGATIVE_INFINITY
        for (at in maxOf(0, index - config.angleWindowMinutes + 1)..index) {
            if (!wear.isWorn(at)) continue
            val min = grid.zAngleMin[at]
            val max = grid.zAngleMax[at]
            if (min.isNaN() || max.isNaN()) continue
            if (min < low) low = min
            if (max > high) high = max
        }
        return high - low >= config.angleStillDeg
    }

    /**
     * Walch's heart rate feature on the minute grid: a difference of
     * Gaussians amplifies periods of change, and its rolling standard
     * deviation is normalised by the night's 90th percentile of
     * |rate − mean|. The within-minute standard deviation the watch sends is
     * not used yet. NaN without a rate.
     */
    internal fun heartRateVolatility(grid: WearMinuteGrid, wear: WearStates, config: Config): FloatArray {
        val n = grid.size
        val rate = FloatArray(n) { index -> if (wear.isWorn(index)) grid.heartRate[index] else Float.NaN }
        val finite = rate.filter { !it.isNaN() }
        if (finite.size < 4) return FloatArray(n) { Float.NaN }
        val mean = finite.average().toFloat()
        val deviations = finite.map { abs(it - mean) }.sorted()
        val norm = deviations[(config.heartRateNormPercentile * deviations.size).toInt().coerceIn(0, deviations.size - 1)]
            .coerceAtLeast(config.heartRateNormFloorBpm)
        val short = gaussian(rate, config.heartRateSigmaShortMinutes)
        val long = gaussian(rate, config.heartRateSigmaLongMinutes)
        val difference = FloatArray(n) { index ->
            if (short[index].isNaN() || long[index].isNaN()) Float.NaN else short[index] - long[index]
        }
        val rolling = SptWindowDetector.rollingSd(difference, config.heartRateSdHalf)
        return FloatArray(n) { index -> if (rolling[index].isNaN()) Float.NaN else rolling[index] / norm }
    }

    /** Gaussian smoothing over the finite values, truncated at three sigma; NaN where none. */
    internal fun gaussian(values: FloatArray, sigma: Float): FloatArray {
        val reach = (3 * sigma).toInt().coerceAtLeast(1)
        val kernel = FloatArray(2 * reach + 1) { exp(-0.5 * ((it - reach) / sigma) * ((it - reach) / sigma)).toFloat() }
        return FloatArray(values.size) { index ->
            var sum = 0f
            var weight = 0f
            for (offset in -reach..reach) {
                val at = index + offset
                if (at !in values.indices || values[at].isNaN()) continue
                val k = kernel[offset + reach]
                sum += k * values[at]
                weight += k
            }
            if (weight == 0f) Float.NaN else sum / weight
        }
    }

    private val ColeKripkeWeights = floatArrayOf(0.29f, 0.42f, 0.23f, 0.31f, 1.00f, 0.36f, 0.25f)
}
