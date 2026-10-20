package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.sqrt
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry

/** One night of skin temperature: its local date, the data source that wrote it, and its change from that device's baseline. */
data class SkinTemperatureNight(
    val date: LocalDate,
    val source: String,
    val deltaCelsius: Double,
)

/**
 * The nightly value of each source.
 *
 * A night is the local date of the record's end (the morning it was read). Its value is the
 * mean of the record's delta samples, or the record's average delta when it carries no
 * samples; several records of one date and source are averaged. This is a product choice:
 * the studies behind the shift rule use other nightly statistics (Shilaih 2018 takes the
 * 99th percentile of LOESS-smoothed night data), and a mean is the simplest value that a
 * record with only an average can also give.
 */
object SkinTemperatureNights {
    fun fromEntries(entries: List<SkinTemperatureEntry>, zone: ZoneId): List<SkinTemperatureNight> =
        entries
            .mapNotNull { entry ->
                val value = if (entry.deltas.isNotEmpty()) {
                    entry.deltas.map { it.deltaCelsius }.average()
                } else {
                    entry.averageDeltaCelsius
                } ?: return@mapNotNull null
                Triple(entry.endTime.atZone(zone).toLocalDate(), entry.source, value)
            }
            .groupBy { (date, source, _) -> date to source }
            .map { (key, values) -> SkinTemperatureNight(key.first, key.second, values.map { it.third }.average()) }
            .sortedWith(compareBy({ it.source }, { it.date }))
}

sealed interface SkinTemperatureShiftResult {
    /** The signal was strong enough and no shift was found. Never a statement about ovulation. */
    data object None : SkinTemperatureShiftResult

    /**
     * A sustained rise, found afterwards. [firstHighDay] is the first of the three high nights;
     * [amplitudeCelsius] is their mean minus the mean of the six nights before. Retrospective,
     * never a prediction, never a fertile or a safe day.
     */
    data class Confirmed(
        val firstHighDay: LocalDate,
        val source: String,
        val amplitudeCelsius: Double,
    ) : SkinTemperatureShiftResult

    /** Enough nights, but the best 3-night mean sits under 0.2 °C above the lowest 6-night mean. */
    data class SignalTooWeak(val amplitudeCelsius: Double, val source: String) : SkinTemperatureShiftResult

    /** Fewer than [SkinTemperatureShiftCalculator.MIN_NIGHTS] nights of any one source. */
    data class InsufficientData(val nights: Int) : SkinTemperatureShiftResult

    /** A rise was found, but it leaves the follicular or luteal phase outside its plausible range. */
    data class NotPlausible(
        val firstHighDay: LocalDate,
        val source: String,
        val follicularDays: Int,
        /** Null while the cycle is still running: the next period start is not known. */
        val lutealDays: Int?,
    ) : SkinTemperatureShiftResult
}

/** The verdict across sources, with the source that carries it and the nights of that source. */
data class SkinTemperatureShiftAnalysis(
    val result: SkinTemperatureShiftResult,
    /** The source the result rests on, or null when there is no night at all. */
    val source: String?,
    val bySource: Map<String, SkinTemperatureShiftResult>,
)

/**
 * A retrospective rise in night skin temperature, read from the nights of one cycle.
 *
 * It is separate from the basal-temperature [ThermalShiftCalculator] and from every estimate
 * in the app: nothing in this file feeds the next-period estimate, [MarkerEstimate] or the
 * marker detector, because no interval from a wrist or finger rise to the next period has
 * been verified. It claims no fertility, no contraception and no diagnosis.
 *
 * Steps, per cycle and per data source:
 * 1. Cleaning after Thigpen et al., J Med Internet Res 2025 (10.2196/60667): the series is
 *    centred around 0 (on the median), isolated spikes beyond 2 standard deviations are
 *    dropped (not every night beyond 2 SD of the whole cycle: that would drop a genuine rise),
 *    and gaps of at most 3 days are filled linearly. At least 70% of the days between the
 *    first and last night must be real nights; the first high night must be a real one. Their algorithm also uses a tuned
 *    Butterworth band-pass filter and hysteresis thresholding; this one does NOT replicate
 *    them.
 * 2. Signal gate. Wang et al., Hum Reprod 2025 (10.1093/humrep/deaf005) evaluate cycles
 *    with a signal of at least 0.2 °C. Turning that condition into a gate is a PRODUCT
 *    HEURISTIC derived from it, not a rule of the paper: with at least [MIN_NIGHTS] nights,
 *    the best 3-night mean minus the lowest 6-night mean before it must reach 0.2 °C, else the result
 *    is [SkinTemperatureShiftResult.SignalTooWeak] and nothing is guessed. A fixed 0.2 °C
 *    depends on the sensor: some devices smooth what they pass on, and a smoothed set
 *    from another device showed a mean post-ovulation rise of only about 0.05 °C.
 * 3. Shift rule after Shilaih et al., Biosci Rep 2018 (10.1042/bsr20171279): a night is the
 *    first high night when it and the next two nights each lie at least 0.2 °C above at
 *    least five of the six nights before the first high night. (The paper's wording leaves
 *    open whether the six nights slide with each of the three; the six before the first
 *    high night are used for all three, the stricter reading of the 6-low rule of
 *    [ThermalShiftCalculator].)
 * 4. Plausibility after Thigpen: a follicular phase of 10 to 90 days and, only when the next
 *    period start is known (a completed cycle), a luteal phase of 7 to 17 days, counted from
 *    the day before the first high night (ovulation is one to two days earlier). Outside it
 *    the result is [SkinTemperatureShiftResult.NotPlausible], not a shift.
 *
 * The wording rule for every caller: "no shift detected", never "anovulatory", and the day is
 * shown as found afterwards, not as a fertile or safe day.
 */
object SkinTemperatureShiftCalculator {
    const val MIN_NIGHTS = 15
    const val MIN_SHIFT_CELSIUS = 0.2
    private const val HIGH_NIGHTS = 3
    private const val LOW_NIGHTS = 6
    private const val LOWS_REQUIRED = 5
    private const val OUTLIER_SD = 2.0
    private const val MAX_SPIKE_NIGHTS = 2
    private const val MAX_FILL_DAYS = 3
    private const val MIN_REAL_SHARE = 0.7
    private const val MIN_FOLLICULAR_DAYS = 10
    private const val MAX_FOLLICULAR_DAYS = 90
    private const val MIN_LUTEAL_DAYS = 7
    private const val MAX_LUTEAL_DAYS = 17

    /**
     * Every source of [nights] on its own, then one verdict: a confirmed shift (the larger
     * amplitude) before no shift, before a weak signal, before an implausible one, before
     * too few nights. [nextPeriodStart] is null while the cycle is running.
     */
    fun evaluateCycle(
        cycleStart: LocalDate,
        nextPeriodStart: LocalDate?,
        nights: List<SkinTemperatureNight>,
    ): SkinTemperatureShiftAnalysis {
        val bySource = nights.groupBy { it.source }
            .mapValues { (source, own) -> evaluateSource(cycleStart, nextPeriodStart, source, own) }
        val best = bySource.entries.minWithOrNull(
            compareBy<Map.Entry<String, SkinTemperatureShiftResult>> { rank(it.value) }.thenByDescending { strength(it.value) }.thenBy { it.key },
        )
        return SkinTemperatureShiftAnalysis(
            result = best?.value ?: SkinTemperatureShiftResult.InsufficientData(nights = 0),
            source = best?.key,
            bySource = bySource,
        )
    }

    fun evaluateSource(
        cycleStart: LocalDate,
        nextPeriodStart: LocalDate?,
        source: String,
        nights: List<SkinTemperatureNight>,
    ): SkinTemperatureShiftResult {
        val inCycle = nights
            .filter { !it.date.isBefore(cycleStart) && (nextPeriodStart == null || it.date.isBefore(nextPeriodStart)) }
            .groupBy { it.date }
            .map { (date, same) -> date to same.map { it.deltaCelsius }.average() }
            .sortedBy { it.first }
        if (inCycle.size < MIN_NIGHTS) return SkinTemperatureShiftResult.InsufficientData(inCycle.size)
        val span = ChronoUnit.DAYS.between(inCycle.first().first, inCycle.last().first) + 1
        if (inCycle.size < MIN_REAL_SHARE * span) return SkinTemperatureShiftResult.InsufficientData(inCycle.size)

        val series = cleaned(inCycle) ?: return SkinTemperatureShiftResult.InsufficientData(inCycle.size)
        if (series.size < LOW_NIGHTS + HIGH_NIGHTS) return SkinTemperatureShiftResult.InsufficientData(inCycle.size)
        val values = series.map { it.value }

        val amplitude = strongestAmplitude(values)
        if (amplitude < MIN_SHIFT_CELSIUS) return SkinTemperatureShiftResult.SignalTooWeak(amplitude, source)

        val first = firstHighIndex(series) ?: return SkinTemperatureShiftResult.None
        val firstHighDay = series[first].date
        val rise = values.subList(first, first + HIGH_NIGHTS).average() -
            values.subList(first - LOW_NIGHTS, first).average()
        val follicular = ChronoUnit.DAYS.between(cycleStart, firstHighDay).toInt()
        // Ovulation is one to two days before the first high night: count the luteal phase from the day before it.
        val luteal = nextPeriodStart?.let { ChronoUnit.DAYS.between(firstHighDay, it).toInt() + 1 }
        val plausible = follicular in MIN_FOLLICULAR_DAYS..MAX_FOLLICULAR_DAYS &&
            (luteal == null || luteal in MIN_LUTEAL_DAYS..MAX_LUTEAL_DAYS)
        return if (plausible) {
            SkinTemperatureShiftResult.Confirmed(firstHighDay, source, rise)
        } else {
            SkinTemperatureShiftResult.NotPlausible(firstHighDay, source, follicular, luteal)
        }
    }

    private class CleanNight(val date: LocalDate, val value: Double, val real: Boolean)

    /**
     * Centred on the median, spikes dropped, gaps filled linearly: null when a gap is longer
     * than [MAX_FILL_DAYS].
     *
     * This departs from Thigpen's rejection over the whole series: against the whole cycle, a
     * genuine luteal rise can sit beyond 2 SD and would be dropped and re-filled, softening
     * the onset. Only a spike is dropped: a run of at most [MAX_SPIKE_NIGHTS] nights beyond
     * 2 SD whose neighbours on both sides are not. A rise is longer and stays.
     */
    private fun cleaned(nights: List<Pair<LocalDate, Double>>): List<CleanNight>? {
        val sorted = nights.map { it.second }.sorted()
        val median = if (sorted.size % 2 == 1) sorted[sorted.size / 2] else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
        val centred = nights.map { it.first to it.second - median }
        val sd = sqrt(centred.sumOf { it.second * it.second } / centred.size)
        val outlier = centred.map { sd > 0.0 && abs(it.second) > OUTLIER_SD * sd }
        val drop = BooleanArray(centred.size)
        var start = 0
        while (start < outlier.size) {
            if (!outlier[start]) { start++; continue }
            var end = start
            while (end + 1 < outlier.size && outlier[end + 1]) end++
            if (end - start + 1 <= MAX_SPIKE_NIGHTS && start > 0 && end < outlier.size - 1) {
                for (k in start..end) drop[k] = true
            }
            start = end + 1
        }
        val kept = centred.filterIndexed { index, _ -> !drop[index] }
        if (kept.size < 2) return kept.map { CleanNight(it.first, it.second, true) }
        val filled = ArrayList<CleanNight>()
        var previous = kept.first()
        filled += CleanNight(previous.first, previous.second, true)
        for (next in kept.drop(1)) {
            val gap = ChronoUnit.DAYS.between(previous.first, next.first).toInt()
            if (gap - 1 > MAX_FILL_DAYS) return null
            for (step in 1 until gap) {
                filled += CleanNight(
                    previous.first.plusDays(step.toLong()),
                    previous.second + (next.second - previous.second) * step / gap,
                    false,
                )
            }
            filled += CleanNight(next.first, next.second, true)
            previous = next
        }
        return filled
    }

    /**
     * The best 3-night mean minus the lowest 6-night mean that comes before it: a series that
     * falls after a spike must not pass.
     */
    private fun strongestAmplitude(values: List<Double>): Double {
        var best = Double.NEGATIVE_INFINITY
        for (high in LOW_NIGHTS..values.size - HIGH_NIGHTS) {
            val highMean = values.subList(high, high + HIGH_NIGHTS).average()
            val lowMean = (0..high - LOW_NIGHTS).minOf { values.subList(it, it + LOW_NIGHTS).average() }
            best = maxOf(best, highMean - lowMean)
        }
        return if (best == Double.NEGATIVE_INFINITY) 0.0 else best
    }

    /** The first high night must be a real night, not a filled one. */
    private fun firstHighIndex(series: List<CleanNight>): Int? {
        val values = series.map { it.value }
        return (LOW_NIGHTS..values.size - HIGH_NIGHTS).firstOrNull { index ->
            val lows = values.subList(index - LOW_NIGHTS, index)
            series[index].real && (index until index + HIGH_NIGHTS).all { night ->
                lows.count { values[night] - it >= MIN_SHIFT_CELSIUS } >= LOWS_REQUIRED
            }
        }
    }

    private fun rank(result: SkinTemperatureShiftResult): Int = when (result) {
        is SkinTemperatureShiftResult.Confirmed -> 0
        SkinTemperatureShiftResult.None -> 1
        is SkinTemperatureShiftResult.SignalTooWeak -> 2
        is SkinTemperatureShiftResult.NotPlausible -> 3
        is SkinTemperatureShiftResult.InsufficientData -> 4
    }

    private fun strength(result: SkinTemperatureShiftResult): Double = when (result) {
        is SkinTemperatureShiftResult.Confirmed -> result.amplitudeCelsius
        is SkinTemperatureShiftResult.SignalTooWeak -> result.amplitudeCelsius
        is SkinTemperatureShiftResult.InsufficientData -> result.nights.toDouble()
        else -> 0.0
    }
}
