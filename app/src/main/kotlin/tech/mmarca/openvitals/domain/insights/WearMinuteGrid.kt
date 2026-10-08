package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import java.time.ZoneOffset
import tech.mmarca.openvitals.domain.model.SleepMinuteKind
import tech.mmarca.openvitals.domain.model.WearMinuteFlags
import tech.mmarca.openvitals.domain.model.WearSleepMinute

/**
 * The watch's rows on a one-minute grid: sorted, duplicates resolved (last
 * wins), missing minutes marked absent so a gap in the recording behaves
 * like a watch that was not worn. Every per-minute series is an array
 * indexed by grid position; NaN means "none". The Wear OS sleep pipeline
 * works on this, never on the row list.
 */
class WearMinuteGrid private constructor(
    private val startMinute: Long,
    val present: BooleanArray,
    val kinds: Array<SleepMinuteKind>,
    val flags: Array<WearMinuteFlags>,
    val sampleCount: IntArray,
    val movement: FloatArray,
    /** Beats per minute, or NaN. */
    val heartRate: FloatArray,
    val heartRateSd: FloatArray,
    val heartRateSamples: IntArray,
    /** Per axis (x, y, z), then per minute, in g. */
    val meanG: Array<FloatArray>,
    val sdG: Array<FloatArray>,
    val zAngleMin: FloatArray,
    val zAngleMax: FloatArray,
    val zAngleDelta: FloatArray,
    val offsetSeconds: IntArray,
) {
    val size: Int get() = present.size

    fun timeAt(index: Int): Instant = Instant.ofEpochSecond((startMinute + index) * SECONDS_PER_MINUTE)

    fun zoneOffsetAt(index: Int): ZoneOffset = ZoneOffset.ofTotalSeconds(offsetSeconds[index])

    /** True when the minute carried at least one heart rate sample. */
    fun hasPulse(index: Int): Boolean = heartRateSamples[index] > 0 && !heartRate[index].isNaN()

    companion object {
        private const val SECONDS_PER_MINUTE = 60L

        fun of(rows: List<WearSleepMinute>): WearMinuteGrid? {
            if (rows.isEmpty()) return null
            val byMinute = LinkedHashMap<Long, WearSleepMinute>()
            for (row in rows.sortedBy { it.time }) byMinute[row.time.epochSecond / SECONDS_PER_MINUTE] = row
            val first = byMinute.keys.min()
            val last = byMinute.keys.max()
            val size = (last - first + 1).toInt()
            val present = BooleanArray(size)
            val kinds = Array(size) { SleepMinuteKind.UNMEASURABLE }
            val flags = Array(size) { WearMinuteFlags.NONE }
            val sampleCount = IntArray(size)
            val movement = FloatArray(size)
            val heartRate = FloatArray(size) { Float.NaN }
            val heartRateSd = FloatArray(size) { Float.NaN }
            val heartRateSamples = IntArray(size)
            val meanG = Array(3) { FloatArray(size) }
            val sdG = Array(3) { FloatArray(size) }
            val zAngleMin = FloatArray(size) { Float.NaN }
            val zAngleMax = FloatArray(size) { Float.NaN }
            val zAngleDelta = FloatArray(size) { Float.NaN }
            val offsetSeconds = IntArray(size)
            var lastOffset = byMinute.values.first().zoneOffset.totalSeconds
            for (index in 0 until size) {
                val row = byMinute[first + index]
                if (row == null) {
                    // An absent minute keeps the offset of the one before it, so the window math stays continuous.
                    offsetSeconds[index] = lastOffset
                    continue
                }
                present[index] = true
                kinds[index] = row.kind
                flags[index] = row.flags
                sampleCount[index] = row.sampleCount
                movement[index] = maxOf(row.movement, 0f).takeIf { it.isFinite() } ?: 0f
                val bpm = row.heartRate
                if (bpm != null && bpm.isFinite() && bpm > 0f) {
                    heartRate[index] = bpm
                    heartRateSd[index] = row.heartRateSd?.takeIf { it.isFinite() } ?: 0f
                }
                heartRateSamples[index] = row.heartRateSamples
                for (axis in 0 until 3) {
                    meanG[axis][index] = row.meanG[axis]
                    sdG[axis][index] = row.sdG[axis]
                }
                zAngleMin[index] = row.zAngleMin ?: Float.NaN
                zAngleMax[index] = row.zAngleMax ?: Float.NaN
                zAngleDelta[index] = row.zAngleDelta ?: Float.NaN
                lastOffset = row.zoneOffset.totalSeconds
                offsetSeconds[index] = lastOffset
            }
            return WearMinuteGrid(
                first, present, kinds, flags, sampleCount, movement, heartRate, heartRateSd, heartRateSamples,
                meanG, sdG, zAngleMin, zAngleMax, zAngleDelta, offsetSeconds,
            )
        }
    }
}
