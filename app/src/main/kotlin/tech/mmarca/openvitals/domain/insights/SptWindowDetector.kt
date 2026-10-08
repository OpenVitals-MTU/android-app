package tech.mmarca.openvitals.domain.insights

import kotlin.math.sqrt

/**
 * Finds the night's sleep period time window on the grid before any minute
 * is scored, as the actigraphy literature does, from two signals:
 *
 * - the arm angle (HDCZA, van Hees 2018): the 5-minute rolling median of
 *   the z-angle's successive change falls below a threshold set from the
 *   recording's own quiet level (15 × its 10th percentile, bounded); blocks
 *   of at least 30 minutes, gaps under 60 minutes merged, the longest block
 *   is the window. C-statistic 0.83–0.86 against polysomnography.
 * - the heart rate (Posa and colleagues 2022, PMC9106748): minutes whose
 *   heart rate sits below a quantile of the recording's distribution,
 *   smoothed with a 5-minute median, sequences under 20 minutes dropped and
 *   gaps under 90 minutes merged, the longest sequence is the window.
 *
 * The angle window is the envelope; the heart rate window only tightens
 * its end, never its onset and never widens it, and is ignored when the
 * two barely overlap: the heart rate keeps falling for an hour after sleep
 * onset, so the paper's onsets ran 20 to 40 minutes late against
 * polysomnography while its offsets were within 15 minutes. A minute the
 * watch was not worn for breaks every block. Pure and deterministic.
 */
object SptWindowDetector {

    enum class Source { ANGLE, HEART_RATE, BOTH }

    /** Grid indices, `[onset, end)`. */
    data class SptWindow(val onset: Int, val end: Int, val source: Source) {
        val length: Int get() = end - onset
    }

    data class Config(
        /** Half the HDCZA rolling median window: 5 minutes. */
        val angleMedianHalf: Int = 2,
        val anglePercentile: Float = 0.10f,
        val angleThresholdFactor: Float = 15f,
        /**
         * Degrees per five seconds. GGIR bounds the threshold too. On the
         * polysomnography nights the sleeping z-angle change sits at 0.0 to
         * 0.2 degrees, so the 10th percentile is zero and the floor decides:
         * at 0.2 restless sleep broke the window (accuracy 0.73, seven nights
         * unfound), at 1.0 the window mostly held (0.88, two unfound), at 1.5
         * it holds (0.90, one unfound); 2.0 changes nothing more.
         */
        val angleThresholdMinDeg: Float = 1.5f,
        val angleThresholdMaxDeg: Float = 2.5f,
        val angleBlockMinMinutes: Int = 30,
        val angleGapMergeMinutes: Int = 60,
        /**
         * The share of the recording's minutes expected asleep. 0.325 for a
         * 24-hour recording in the paper, 0.80 for a night-only one; the
         * phone pulls the 20-hour night window, where 8 hours is 0.40.
         */
        val heartRateQuantile: Float = 0.40f,
        val heartRateMedianHalf: Int = 2,
        val heartRateMinSequenceMinutes: Int = 20,
        val heartRateGapMergeMinutes: Int = 90,
        /**
         * Whether the heart rate window may tighten the angle window's end.
         * Off: on the polysomnography nights the tightening cut two nights
         * short by an hour or more (REM raises the pulse late in the night)
         * and raised the offset error from 13.6 to 21.9 minutes. The heart
         * rate window still stands in when the arm angle finds no window.
         */
        val useHeartRateWindow: Boolean = false,
        /** Below this overlap with the angle window the heart rate window is ignored. */
        val minOverlapShare: Float = 0.5f,
        /** A window with fewer worn minutes than this is no night. */
        val minWornMinutes: Int = 180,
    )

    fun detect(grid: WearMinuteGrid, wear: WearStates, config: Config = Config()): SptWindow? {
        val angle = angleWindow(grid, wear, config)
        val heart = if (config.useHeartRateWindow || angle == null) heartRateWindow(grid, wear, config) else null
        val window = when {
            angle == null && heart == null -> return null
            angle == null -> SptWindow(heart!!.first, heart.last + 1, Source.HEART_RATE)
            heart == null -> SptWindow(angle.first, angle.last + 1, Source.ANGLE)
            else -> tighten(angle, heart, config)
        }
        var worn = 0
        for (index in window.onset until window.end) if (wear.isWorn(index)) worn++
        return window.takeIf { worn >= config.minWornMinutes }
    }

    private fun tighten(angle: IntRange, heart: IntRange, config: Config): SptWindow {
        val overlap = maxOf(0, minOf(angle.last, heart.last) - maxOf(angle.first, heart.first) + 1)
        if (overlap < config.minOverlapShare * (angle.last - angle.first + 1)) return SptWindow(angle.first, angle.last + 1, Source.ANGLE)
        val end = if (heart.last in angle) heart.last + 1 else angle.last + 1
        val source = if (end == angle.last + 1) Source.ANGLE else Source.BOTH
        return SptWindow(angle.first, end, source)
    }

    /** HDCZA on the per-minute z-angle change. Inclusive grid indices, or null. */
    internal fun angleWindow(grid: WearMinuteGrid, wear: WearStates, config: Config): IntRange? {
        val n = grid.size
        val change = FloatArray(n) { index ->
            if (wear.isWorn(index)) grid.zAngleDelta[index] else Float.NaN
        }
        val smoothed = rollingMedian(change, config.angleMedianHalf)
        val quiet = percentile(smoothed, config.anglePercentile) ?: return null
        val threshold = (config.angleThresholdFactor * quiet).coerceIn(config.angleThresholdMinDeg, config.angleThresholdMaxDeg)
        val still = BooleanArray(n) { index -> !smoothed[index].isNaN() && smoothed[index] < threshold }
        return longestBlock(still, wear, config.angleBlockMinMinutes, config.angleGapMergeMinutes)
    }

    /** The heart rate quantile method. Inclusive grid indices, or null. */
    internal fun heartRateWindow(grid: WearMinuteGrid, wear: WearStates, config: Config): IntRange? {
        val n = grid.size
        val rate = FloatArray(n) { index -> if (wear.isWorn(index)) grid.heartRate[index] else Float.NaN }
        val cutoff = percentile(rate, config.heartRateQuantile) ?: return null
        val low = FloatArray(n) { index -> if (rate[index].isNaN()) Float.NaN else if (rate[index] <= cutoff) 1f else 0f }
        val smoothed = rollingMedian(low, config.heartRateMedianHalf)
        val asleep = BooleanArray(n) { index -> !smoothed[index].isNaN() && smoothed[index] >= 0.5f }
        return longestBlock(asleep, wear, config.heartRateMinSequenceMinutes, config.heartRateGapMergeMinutes)
    }

    /**
     * Runs of [flag] at least [minMinutes] long; a gap shorter than
     * [mergeMinutes] between two runs joins them unless the watch was not
     * worn somewhere in it. The longest merged run wins, the earlier on a tie.
     */
    private fun longestBlock(flag: BooleanArray, wear: WearStates, minMinutes: Int, mergeMinutes: Int): IntRange? {
        val runs = ArrayList<IntRange>()
        var start = -1
        for (index in 0..flag.size) {
            val on = index < flag.size && flag[index] && wear.isWorn(index)
            if (on && start < 0) start = index
            if (!on && start >= 0) {
                if (index - start >= minMinutes) runs += start until index
                start = -1
            }
        }
        if (runs.isEmpty()) return null
        val merged = ArrayList<IntRange>()
        for (run in runs) {
            val previous = merged.lastOrNull()
            if (previous != null && run.first - previous.last - 1 < mergeMinutes &&
                (previous.last + 1 until run.first).all { wear.isWorn(it) }
            ) {
                merged[merged.size - 1] = previous.first..run.last
            } else {
                merged += run
            }
        }
        return merged.maxWithOrNull(compareBy<IntRange> { it.last - it.first }.thenByDescending { it.first })
    }

    /** The median of the finite values within ±[half]; NaN where none. */
    internal fun rollingMedian(values: FloatArray, half: Int): FloatArray = FloatArray(values.size) { index ->
        val picked = ArrayList<Float>(2 * half + 1)
        for (at in maxOf(0, index - half)..minOf(values.size - 1, index + half)) {
            if (!values[at].isNaN()) picked += values[at]
        }
        if (picked.isEmpty()) Float.NaN else {
            picked.sort()
            val n = picked.size
            if (n % 2 == 1) picked[n / 2] else (picked[n / 2 - 1] + picked[n / 2]) / 2f
        }
    }

    /** The [share] quantile of the finite values (nearest rank), or null when there are none. */
    internal fun percentile(values: FloatArray, share: Float): Float? {
        val finite = values.filter { !it.isNaN() }.sorted()
        if (finite.isEmpty()) return null
        val rank = (share * finite.size).toInt().coerceIn(0, finite.size - 1)
        return finite[rank]
    }

    /** Population standard deviation of the finite values within ±[half]; NaN with fewer than four. */
    internal fun rollingSd(values: FloatArray, half: Int): FloatArray = FloatArray(values.size) { index ->
        var count = 0
        var sum = 0.0
        for (at in maxOf(0, index - half)..minOf(values.size - 1, index + half)) {
            if (values[at].isNaN()) continue
            count++
            sum += values[at]
        }
        if (count < 4) Float.NaN else {
            val mean = sum / count
            var squares = 0.0
            for (at in maxOf(0, index - half)..minOf(values.size - 1, index + half)) {
                if (values[at].isNaN()) continue
                squares += (values[at] - mean) * (values[at] - mean)
            }
            sqrt(squares / count).toFloat()
        }
    }
}
