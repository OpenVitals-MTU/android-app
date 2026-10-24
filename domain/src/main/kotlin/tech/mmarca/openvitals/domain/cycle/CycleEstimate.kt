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
    val basis: CycleEstimateBasis = CycleEstimateBasis.Calendar(),
) {
    val window: ClosedRange<LocalDate>
        get() = earliestDate..latestDate
}

/** What the estimate counts from. */
sealed interface CycleEstimateBasis {
    /** Start-to-start intervals. [markersDisagree] when recorded markers were set aside. */
    data class Calendar(val markersDisagree: Boolean = false) : CycleEstimateBasis

    /**
     * A marker recorded in the current cycle plus the interval from that marker to
     * the next start. [confirmedBy] lists the markers that agree with the temperature
     * shift, the shift included; empty when one marker stands alone.
     */
    data class Marker(
        val anchor: OvulationMarker,
        val anchorDate: LocalDate,
        val confirmedBy: Set<OvulationMarker>,
        val intervalCount: Int,
    ) : CycleEstimateBasis
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
     * @param markers the ovulation markers per cycle, keyed by cycle start. A marker in
     *   the current cycle replaces the calendar estimate; see [MarkerEstimate].
     */
    fun evaluate(
        cycles: List<RecordedCycle>,
        ageBand: AgeBand? = null,
        hasTimingContext: Boolean = false,
        markers: Map<LocalDate, Map<OvulationMarker, LocalDate>> = emptyMap(),
    ): CycleEstimateResult {
        val sorted = cycles.sortedBy { it.startDate }
        val calendar = calendarEstimate(sorted, ageBand, hasTimingContext)
        return when (val marker = MarkerEstimate.evaluate(sorted, markers, hasTimingContext)) {
            null -> calendar
            MarkerEstimate.Disagreement -> calendar.withBasis(CycleEstimateBasis.Calendar(markersDisagree = true))
            is MarkerEstimate.Estimate -> CycleEstimateResult.Available(marker.estimate)
        }
    }

    private fun calendarEstimate(sorted: List<RecordedCycle>, ageBand: AgeBand?, hasTimingContext: Boolean): CycleEstimateResult {
        if (sorted.map { it.startDate }.distinct().size < MINIMUM_INTERVALS + 1) {
            return CycleEstimateResult.NeedsMoreHistory
        }
        val lengths = recordedIntervalLengths(sorted)
        if (lengths.size < MINIMUM_INTERVALS) return CycleEstimateResult.IntervalsOutOfRange

        val recent = lengths.takeLast(RECENT_INTERVAL_WINDOW)
        val averageLength = recent.average().roundToInt()
        // Persistence is defined over ten cycles, so it is judged on the wider list.
        val highVariability = hasPersistentVariability(lengths)
        val priorSd = ageBand?.variationSdDays ?: AgeBand.UNDECLARED_VARIATION_SD_DAYS
        val radius = rangeRadiusDays(
            variance = shrunkVariance(
                lengths = recent,
                priorVariance = priorSd * priorSd,
                priorWeight = if (highVariability) PRIOR_WEIGHT_HIGH_VARIABILITY else PRIOR_WEIGHT,
            ),
            priorVariance = priorSd * priorSd,
            hasTimingContext = hasTimingContext,
            minimumDays = MINIMUM_RANGE_RADIUS_DAYS,
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
    internal fun earliestFloor(cycle: RecordedCycle): LocalDate {
        val afterFlow = cycle.lastFlowDay?.plusDays(1)
        return if (afterFlow != null && afterFlow.isAfter(cycle.startDate)) afterFlow else cycle.startDate
    }

    /**
     * The sample variance shrunk toward the prior. A range divided by two is not
     * a dispersion estimate and understated uncertainty most when history was thinnest.
     */
    internal fun shrunkVariance(lengths: List<Int>, priorVariance: Double, priorWeight: Double = PRIOR_WEIGHT): Double {
        val n = lengths.size
        if (n < 2) return priorVariance
        val mean = lengths.average()
        val sampleVariance = lengths.sumOf { (it - mean) * (it - mean) } / (n - 1)
        val sampleWeight = (n - 1).toDouble()
        return (sampleWeight * sampleVariance + priorWeight * priorVariance) / (sampleWeight + priorWeight)
    }

    /** Half-width of a 95% window on [variance], at least [minimumDays]. */
    internal fun rangeRadiusDays(
        variance: Double,
        priorVariance: Double,
        hasTimingContext: Boolean,
        minimumDays: Int,
    ): Int {
        // A declared timing context guarantees at least population-level uncertainty.
        val flooredVariance = if (hasTimingContext) max(variance, priorVariance) else variance
        val radius = ceil(WINDOW_Z * sqrt(flooredVariance)).toInt()
        return radius.coerceIn(minimumDays, MAXIMUM_RANGE_RADIUS_DAYS)
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

private fun CycleEstimateResult.withBasis(basis: CycleEstimateBasis): CycleEstimateResult =
    if (this is CycleEstimateResult.Available) CycleEstimateResult.Available(estimate.copy(basis = basis)) else this
