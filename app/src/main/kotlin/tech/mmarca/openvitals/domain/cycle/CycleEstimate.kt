package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** The next period as a range. [cycleCount] intervals informed it. */
data class CycleEstimate(
    val earliestDate: LocalDate,
    val centralDate: LocalDate,
    val latestDate: LocalDate,
    val cycleCount: Int,
    /** Spread of the intervals used, longest minus shortest. */
    val variabilityDays: Int,
) {
    val window: ClosedRange<LocalDate>
        get() = earliestDate..latestDate
}

/**
 * Whether an estimate exists, and if not, why. A user with long cycles has
 * recorded plenty of history, and saying otherwise would be false.
 */
sealed interface CycleEstimateResult {
    data class Available(val estimate: CycleEstimate) : CycleEstimateResult

    /** Fewer than two recorded cycle starts. */
    data object NeedsMoreHistory : CycleEstimateResult

    /** Starts exist, but no interval falls inside [CycleEstimateCalculator.plausibleCycleDays]. */
    data object IntervalsOutOfRange : CycleEstimateResult

    val estimateOrNull: CycleEstimate?
        get() = (this as? Available)?.estimate
}

/**
 * Next-period estimate from recorded cycle starts.
 *
 * The central date is the last start plus the rounded mean of the recent
 * intervals. The half-width is a 95% window on the sample variance shrunk
 * toward a population prior, so thin history stays honestly wide.
 */
object CycleEstimateCalculator {
    private const val MINIMUM_INTERVALS = 1
    private const val MINIMUM_CYCLE_DAYS = 15
    private const val MAXIMUM_CYCLE_DAYS = 90
    private const val RECENT_INTERVAL_WINDOW = 6

    /** Interval lengths this calculator can model. Summaries use the same filter. */
    val plausibleCycleDays: IntRange = MINIMUM_CYCLE_DAYS..MAXIMUM_CYCLE_DAYS

    /** Prior weight in pseudo-observations. It washes out as history accumulates. */
    private const val PRIOR_WEIGHT = 2.0

    /** Prior weight once the record itself shows persistent variability. */
    private const val PRIOR_WEIGHT_HIGH_VARIABILITY = 0.5

    /**
     * STRAW+10 variability: a swing of 7 days or more between consecutive
     * cycles, recurring within 10 cycles. Internal only; showing it would be staging.
     */
    private const val VARIABILITY_SWING_DAYS = 7
    private const val PERSISTENCE_CYCLE_WINDOW = 10
    private const val PERSISTENCE_MIN_OCCURRENCES = 2

    /** About a 95% window under normality. */
    private const val WINDOW_Z = 1.96

    /** Never claim a tighter window than this. */
    private const val MINIMUM_RANGE_RADIUS_DAYS = 3

    /** The 95% half-width of the widest measured band (SD 11.19 days). */
    private const val MAXIMUM_RANGE_RADIUS_DAYS = 22

    /** The intervals the estimate uses: start to start, in range, last six. */
    fun recentIntervalLengths(cycles: List<RecordedCycle>): List<Int> =
        recordedIntervalLengths(cycles).takeLast(RECENT_INTERVAL_WINDOW)

    /**
     * @param ageBand selects the variability prior; null uses the undeclared value.
     * @param hasTimingContext floors the variance at the prior. It can only widen.
     */
    fun evaluate(
        cycles: List<RecordedCycle>,
        ageBand: AgeBand? = null,
        hasTimingContext: Boolean = false,
    ): CycleEstimateResult {
        val sorted = cycles.sortedBy { it.startDate }
        if (sorted.map { it.startDate }.distinct().size < MINIMUM_INTERVALS + 1) {
            return CycleEstimateResult.NeedsMoreHistory
        }
        val lengths = recordedIntervalLengths(sorted)
        if (lengths.size < MINIMUM_INTERVALS) return CycleEstimateResult.IntervalsOutOfRange

        val recent = lengths.takeLast(RECENT_INTERVAL_WINDOW)
        val averageLength = recent.average().roundToInt()
        // Persistence is defined over ten cycles, so it is judged on the wider list.
        val highVariability = hasPersistentVariability(lengths)
        val radius = rangeRadiusDays(
            lengths = recent,
            highVariability = highVariability,
            hasTimingContext = hasTimingContext,
            priorSdDays = ageBand?.variationSdDays ?: AgeBand.UNDECLARED_VARIATION_SD_DAYS,
        )
        val last = sorted.last()
        val central = last.startDate.plusDays(averageLength.toLong())
        val earliest = maxOf(central.minusDays(radius.toLong()), earliestFloor(last))
        return CycleEstimateResult.Available(
            CycleEstimate(
                earliestDate = earliest,
                centralDate = central,
                latestDate = central.plusDays(radius.toLong()),
                cycleCount = recent.size,
                variabilityDays = recent.max() - recent.min(),
            ),
        )
    }

    fun estimateNextPeriod(
        cycles: List<RecordedCycle>,
        ageBand: AgeBand? = null,
        hasTimingContext: Boolean = false,
    ): CycleEstimate? = evaluate(cycles, ageBand, hasTimingContext).estimateOrNull

    /** Start-to-start lengths. A gap over 90 days resets the history; excluded cycles are skipped. */
    private fun recordedIntervalLengths(cycles: List<RecordedCycle>): List<Int> {
        val sorted = cycles.sortedBy { it.startDate }
        val lengths = mutableListOf<Int>()
        for (index in 0 until sorted.size - 1) {
            val current = sorted[index]
            val next = sorted[index + 1]
            val days = ChronoUnit.DAYS.between(current.startDate, next.startDate).toInt()
            if (days > MAXIMUM_CYCLE_DAYS) {
                lengths.clear()
                continue
            }
            if (current.isExcludedFromEstimates) continue
            if (days in plausibleCycleDays) lengths.add(days)
        }
        return lengths
    }

    /** The window never starts inside recorded flow. */
    private fun earliestFloor(cycle: RecordedCycle): LocalDate {
        val afterFlow = cycle.lastFlowDay?.plusDays(1)
        return if (afterFlow != null && afterFlow.isAfter(cycle.startDate)) afterFlow else cycle.startDate
    }

    /**
     * Half-width of the window: the sample variance shrunk toward the prior,
     * then a 95% band. A range divided by two is not a dispersion estimate and
     * understated uncertainty most when history was thinnest.
     */
    private fun rangeRadiusDays(
        lengths: List<Int>,
        highVariability: Boolean,
        hasTimingContext: Boolean,
        priorSdDays: Double,
    ): Int {
        val n = lengths.size
        val priorVariance = priorSdDays * priorSdDays
        val shrunkVariance = if (n < 2) {
            priorVariance
        } else {
            val mean = lengths.average()
            val sampleVariance = lengths.sumOf { (it - mean) * (it - mean) } / (n - 1)
            val priorWeight = if (highVariability) PRIOR_WEIGHT_HIGH_VARIABILITY else PRIOR_WEIGHT
            val sampleWeight = (n - 1).toDouble()
            (sampleWeight * sampleVariance + priorWeight * priorVariance) / (sampleWeight + priorWeight)
        }
        // A declared timing context guarantees at least population-level uncertainty.
        val flooredVariance = if (hasTimingContext) max(shrunkVariance, priorVariance) else shrunkVariance
        val radius = ceil(WINDOW_Z * sqrt(flooredVariance)).toInt()
        return radius.coerceIn(MINIMUM_RANGE_RADIUS_DAYS, MAXIMUM_RANGE_RADIUS_DAYS)
    }

    /**
     * Two separate 7-day excursions inside ten cycles. A spike that returns
     * (+N then -N) is one unusual month, not two.
     */
    private fun hasPersistentVariability(lengths: List<Int>): Boolean {
        val diffs = lengths.takeLast(PERSISTENCE_CYCLE_WINDOW).zipWithNext { previous, next -> next - previous }
        var excursions = 0
        var index = 0
        while (index < diffs.size) {
            if (abs(diffs[index]) >= VARIABILITY_SWING_DAYS) {
                excursions++
                val next = diffs.getOrNull(index + 1)
                if (next != null && abs(next) >= VARIABILITY_SWING_DAYS && (diffs[index] > 0) != (next > 0)) {
                    index += 2
                    continue
                }
            }
            index++
        }
        return excursions >= PERSISTENCE_MIN_OCCURRENCES
    }
}
