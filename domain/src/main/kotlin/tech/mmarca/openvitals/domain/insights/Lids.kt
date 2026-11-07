package tech.mmarca.openvitals.domain.insights

/**
 * Locomotor Inactivity During Sleep (Winnebeck and colleagues 2018): a
 * non-linear conversion of wrist activity, `100 / (activity + 1)`, that
 * runs from near zero when the wrist moves to 100 when it is still, and
 * once smoothed over half an hour rises and falls with the ultradian
 * NREM/REM cycle: the stillest stretches are the deepest sleep, the
 * twitching ones REM. A stage prior from the wrist alone.
 */
object Lids {

    /** Per-minute LIDS, smoothed by a centred moving average of [smoothMinutes]. NaN activity is skipped. */
    fun of(activity: FloatArray, scale: Float = 1f, smoothMinutes: Int = 30): FloatArray {
        val raw = FloatArray(activity.size) { index ->
            val a = activity[index]
            if (a.isNaN()) Float.NaN else 100f / (maxOf(a, 0f) / scale + 1f)
        }
        val half = smoothMinutes / 2
        return FloatArray(raw.size) { index ->
            var sum = 0f
            var count = 0
            for (at in maxOf(0, index - half)..minOf(raw.size - 1, index + half)) {
                if (raw[at].isNaN()) continue
                sum += raw[at]
                count++
            }
            if (count == 0) Float.NaN else sum / count
        }
    }

    /**
     * The smoothed LIDS centred on its median and scaled to roughly [-1, 1]
     * by its median absolute deviation, so a weight means the same on every
     * night. Zero where LIDS is NaN or the night is flat.
     */
    fun normalised(lids: FloatArray): FloatArray {
        val finite = lids.filter { !it.isNaN() }.sorted()
        if (finite.size < 2) return FloatArray(lids.size)
        val median = finite[finite.size / 2]
        val spread = finite.map { kotlin.math.abs(it - median) }.sorted()[finite.size / 2]
        if (spread <= 0f) return FloatArray(lids.size)
        return FloatArray(lids.size) { index ->
            if (lids[index].isNaN()) 0f else ((lids[index] - median) / spread).coerceIn(-3f, 3f)
        }
    }
}
