package tech.mmarca.openvitals.wear

import java.time.Instant
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.roundToInt
import tech.mmarca.openvitals.wearlink.AscentCredit
import tech.mmarca.openvitals.wearlink.WalkingEnergy
import tech.mmarca.openvitals.wearlink.WearLinkProtocol.AscentMinute
import tech.mmarca.openvitals.wearlink.WearLinkProtocol.HeartRateSample
import tech.mmarca.openvitals.wearlink.WearLinkProtocol.StepMinute

/**
 * Today, as the metric tiles draw it: local midnight to the next, cut into
 * [BUCKET_MILLIS] buckets. A series holds one point per bucket up to the one
 * [nowMillis] falls in; [bucketsPerDay] is 92 or 100 on the days the clocks
 * change. The x axis is elapsed time ([fractionAt]), so a 23- or 25-hour day
 * is drawn as it passed and the hour marks sit where those hours fell.
 *
 * Pure, so the bucketing and the midnight and clock-change edges are tested
 * without a watch. The values come from the [MetricStore] the recorders fill.
 */
internal class TileDay(
    val startMillis: Long,
    val endMillis: Long,
    val nowMillis: Long,
    /** Local 00:00, 06:00, 12:00, 18:00 and the next 00:00, as instants. */
    private val hourMarkMillis: List<Long>,
) {

    val bucketsPerDay: Int = ((endMillis - startMillis) / BUCKET_MILLIS).toInt()

    /** The buckets that have begun, the one now is in included. */
    val bucketsSoFar: Int = (((nowMillis - startMillis) / BUCKET_MILLIS).toInt() + 1).coerceIn(1, bucketsPerDay)

    /** Where [epochMillis] falls on the day's axis, 0 at midnight and 1 at the next. */
    fun fractionAt(epochMillis: Long): Float =
        ((epochMillis - startMillis).toDouble() / (endMillis - startMillis)).toFloat().coerceIn(0f, 1f)

    /** Where the hour labels go: [HOUR_MARKS] on the day's axis. */
    val hourMarks: List<Float> get() = hourMarkMillis.map(::fractionAt)

    /** The axis position of the end of [bucket], or of now for the bucket now is in. */
    fun bucketEnd(bucket: Int): Float = fractionAt(minOf(startMillis + (bucket + 1) * BUCKET_MILLIS, nowMillis))

    /** The axis position of the middle of [bucket], never past now. */
    fun bucketMiddle(bucket: Int): Float = fractionAt(minOf(startMillis + bucket * BUCKET_MILLIS + BUCKET_MILLIS / 2, nowMillis))

    /** Whether [epochMillis] falls between midnight and now. */
    fun contains(epochMillis: Long): Boolean = epochMillis in startMillis..nowMillis

    /** The running total of [amounts] at the end of each bucket so far; amounts outside today are left out. */
    fun cumulative(amounts: List<Pair<Long, Double>>): List<Double> {
        val perBucket = DoubleArray(bucketsSoFar)
        for ((at, amount) in amounts) {
            if (contains(at)) perBucket[bucketOf(at)] += amount
        }
        var total = 0.0
        return perBucket.map { amount -> total += amount; total }
    }

    /** The mean heart rate of each bucket so far; null where the watch stored none, so the line breaks. */
    fun meanPerBucket(samples: List<HeartRateSample>): List<Int?> {
        val sums = LongArray(bucketsSoFar)
        val counts = IntArray(bucketsSoFar)
        for (sample in samples) {
            if (!contains(sample.epochMillis)) continue
            val bucket = bucketOf(sample.epochMillis)
            sums[bucket] += sample.bpm.toLong()
            counts[bucket]++
        }
        return List(bucketsSoFar) { if (counts[it] == 0) null else (sums[it].toDouble() / counts[it]).roundToInt() }
    }

    /**
     * The count to show for today: the phone's total at its last sync, plus
     * the [amounts] the watch recorded in the minutes from [phoneFrontierMillis],
     * the first minute the phone's total does not hold; or [ownToday], the
     * watch's own count for the day, when that is larger. The two parts hold
     * different minutes of the watch, but Health Connect leaves out what
     * another source covers at the same time, so a sync can lower the number
     * a little. The newest minute the phone wrote stays the phone's, so at
     * most its late steps wait for the next sync.
     */
    fun shownTotal(ownToday: Double, amounts: List<Pair<Long, Double>>, phoneTotal: Long?, phoneFrontierMillis: Long?): Double {
        if (phoneTotal == null || phoneFrontierMillis == null) return ownToday
        val since = amounts.filter { (at, _) -> at >= phoneFrontierMillis && contains(at) }.sumOf { it.second }
        return maxOf(ownToday, phoneTotal + since)
    }

    /** The lowest and highest heart rate stored today, or null when there is none. */
    fun range(samples: List<HeartRateSample>): IntRange? {
        val today = samples.filter { contains(it.epochMillis) }.map { it.bpm }
        return if (today.isEmpty()) null else today.min()..today.max()
    }

    private fun bucketOf(epochMillis: Long): Int =
        ((epochMillis - startMillis) / BUCKET_MILLIS).toInt().coerceIn(0, bucketsSoFar - 1)

    companion object {
        const val BUCKET_MILLIS: Long = 15L * 60 * 1000
        const val MINUTE_MILLIS: Long = 60L * 1000

        /** The minutes one day can hold, a 25-hour day included: the store read limit for minute rows. */
        const val MAX_MINUTES_PER_DAY: Int = 25 * 60

        /** The local hours the axis is labelled at. */
        val HOUR_MARKS: List<Int> = listOf(0, 6, 12, 18, 24)

        /** At most one heart rate sample every ten seconds (`HeartRateRecorder`) over a 25-hour day. */
        const val MAX_HEART_RATE_SAMPLES_PER_DAY: Int = 25 * 60 * 6

        /** The day [nowMillis] falls on in [zone]. */
        fun of(nowMillis: Long, zone: ZoneId): TileDay {
            val date = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val marks = HOUR_MARKS.map { hour ->
                when (hour) {
                    0 -> start
                    HOURS_PER_DAY -> end
                    else -> date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
                }
            }
            return TileDay(start, end, nowMillis, marks)
        }

        /** Steps per minute as amounts for [cumulative]. */
        fun stepAmounts(minutes: List<StepMinute>): List<Pair<Long, Double>> =
            minutes.map { it.epochMillis to it.steps.toDouble() }

        /** The metres of each step minute at [strideMillimetres] a step. */
        fun distanceAmounts(minutes: List<StepMinute>, strideMillimetres: Int): List<Pair<Long, Double>> =
            minutes.map { it.epochMillis to it.steps * strideMillimetres / MILLIMETRES_PER_METRE }

        /** The walking energy of each step minute, in active kilocalories ([WalkingEnergy]), for [weightGrams]. */
        fun walkingKcalAmounts(minutes: List<StepMinute>, weightGrams: Int): List<Pair<Long, Double>> =
            minutes.map { it.epochMillis to WalkingEnergy.activeKcalPerMinute(it.steps, weightGrams / GRAMS_PER_KILOGRAM) }

        /**
         * The metres of each minute's climb that the steps around it carry,
         * by the rule the phone applies before it writes floors
         * ([AscentCredit.creditedPerMinute]), so elevators drop out here too.
         */
        fun creditedAscent(ascent: List<AscentMinute>, steps: List<StepMinute>): List<Pair<Long, Double>> =
            AscentCredit.creditedPerMinute(
                climbedMetres = ascent.map { it.epochMillis to it.decimetres / DECIMETRES_PER_METRE },
                steps = steps.map { it.epochMillis to it.steps },
            )

        /**
         * Rounded down, after allowing for the error of summing decimetres as
         * doubles: 0.2 + 0.7 + 0.1 m is 0.9999999999999999, and is one metre.
         */
        fun wholeDown(value: Double): Long = floor(value + ROUNDING_SLACK).toLong()

        private const val ROUNDING_SLACK = 1e-9
        private const val DECIMETRES_PER_METRE = 10.0
        private const val MILLIMETRES_PER_METRE = 1_000.0
        private const val GRAMS_PER_KILOGRAM = 1_000.0
        private const val HOURS_PER_DAY = 24
    }
}
