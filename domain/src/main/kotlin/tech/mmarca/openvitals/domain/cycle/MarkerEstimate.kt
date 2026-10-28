package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Next-period estimate from an ovulation marker recorded in the current cycle:
 * the marker day plus the interval from that marker to the next start.
 *
 * The interval after ovulation varies less within a woman than the days before
 * it (Henry et al. 2024, Hum Reprod 39(11)), so a recorded marker narrows the
 * window. Each marker has its own interval, learned from the user's earlier
 * cycles and shrunk toward a published prior, so no ovulation day is assumed.
 */
internal object MarkerEstimate {

    sealed interface Result
    data class Estimate(val estimate: CycleEstimate) : Result

    /** A temperature shift and another marker were recorded, and they do not agree. */
    data object Disagreement : Result

    /** A marker within this many days before the first high day agrees with it. */
    private const val AGREEMENT_DAYS = 3

    /** Marker-to-start intervals outside this range are not modelled. */
    private val plausibleIntervalDays = 7..20

    private const val RECENT_INTERVAL_WINDOW = 6

    /** Prior weight in pseudo-observations, as for the calendar estimate. */
    private const val PRIOR_WEIGHT = 2.0

    /** A marker estimate places the next start no closer than this. */
    private const val MINIMUM_RANGE_RADIUS_DAYS = 2

    /** Prior for the days from the marker to the next start. */
    private data class Prior(val meanDays: Double, val sdDays: Double)

    private val priors = mapOf(
        // Soumpasis et al. 2020, Hum Reprod Open: luteal 12.77 ± 1.86 days counted from the day after the LH surge.
        OvulationMarker.LH_TEST to Prior(meanDays = 13.77, sdDays = 1.86),
        // Henry et al. 2024, Hum Reprod 39(11): luteal phase from the temperature shift, median 10.9 days,
        // between-women variance 4.3.
        OvulationMarker.TEMPERATURE to Prior(meanDays = 11.0, sdDays = 2.07),
        // The mucus peak usually falls one to three days before the temperature rise; the weakest prior.
        OvulationMarker.MUCUS to Prior(meanDays = 13.0, sdDays = 2.5),
    )

    /** Priority when several markers are usable: the most direct sign of the LH surge first. */
    private val anchorOrder = listOf(OvulationMarker.LH_TEST, OvulationMarker.TEMPERATURE, OvulationMarker.MUCUS)

    /** Null when the current cycle has no marker, or is open to no estimate at all. */
    fun evaluate(
        sorted: List<RecordedCycle>,
        markers: Map<LocalDate, Map<OvulationMarker, LocalDate>>,
        hasTimingContext: Boolean,
    ): Result? {
        val current = sorted.lastOrNull()?.takeIf { it.isCurrent && !it.isExcludedFromEstimates } ?: return null
        val recorded = markers[current.startDate].orEmpty()
        if (recorded.isEmpty()) return null

        val temperature = recorded[OvulationMarker.TEMPERATURE]
        val agreeing = if (temperature == null) {
            emptySet()
        } else {
            recorded.filter { (marker, date) ->
                marker != OvulationMarker.TEMPERATURE &&
                    ChronoUnit.DAYS.between(date, temperature) in 0..AGREEMENT_DAYS
            }.keys
        }
        val anchor = when {
            agreeing.isNotEmpty() -> if (OvulationMarker.LH_TEST in agreeing) OvulationMarker.LH_TEST else OvulationMarker.TEMPERATURE
            temperature != null && recorded.size > 1 -> return Disagreement
            else -> anchorOrder.first { it in recorded }
        }
        val confirmedBy = if (agreeing.isEmpty()) emptySet() else agreeing + OvulationMarker.TEMPERATURE
        return Estimate(estimate(current, anchor, recorded.getValue(anchor), confirmedBy, sorted, markers, hasTimingContext))
    }

    private fun estimate(
        current: RecordedCycle,
        anchor: OvulationMarker,
        anchorDate: LocalDate,
        confirmedBy: Set<OvulationMarker>,
        sorted: List<RecordedCycle>,
        markers: Map<LocalDate, Map<OvulationMarker, LocalDate>>,
        hasTimingContext: Boolean,
    ): CycleEstimate {
        val intervals = pastIntervals(anchor, sorted, markers)
        val prior = priors.getValue(anchor)
        val priorVariance = prior.sdDays * prior.sdDays
        val mean = (intervals.sum() + PRIOR_WEIGHT * prior.meanDays) / (intervals.size + PRIOR_WEIGHT)
        val radius = CycleEstimateCalculator.rangeRadiusDays(
            variance = CycleEstimateCalculator.shrunkVariance(intervals, priorVariance),
            priorVariance = priorVariance,
            hasTimingContext = hasTimingContext,
            minimumDays = MINIMUM_RANGE_RADIUS_DAYS,
        )
        val central = anchorDate.plusDays(mean.roundToInt().toLong())
        return CycleEstimate(
            earliestDate = maxOf(central.minusDays(radius.toLong()), CycleEstimateCalculator.earliestFloor(current)),
            centralDate = central,
            latestDate = central.plusDays(radius.toLong()),
            cycleCount = intervals.size,
            variabilityDays = if (intervals.isEmpty()) 0 else intervals.max() - intervals.min(),
            basis = CycleEstimateBasis.Marker(anchor, anchorDate, confirmedBy, intervals.size),
        )
    }

    /** Days from [marker] to the next start in earlier, closed, included cycles; the last six. */
    private fun pastIntervals(
        marker: OvulationMarker,
        sorted: List<RecordedCycle>,
        markers: Map<LocalDate, Map<OvulationMarker, LocalDate>>,
    ): List<Int> = sorted
        .filter { !it.isCurrent && !it.isExcludedFromEstimates }
        .mapNotNull { cycle ->
            val day = markers[cycle.startDate]?.get(marker) ?: return@mapNotNull null
            val nextStart = requireNotNull(cycle.endDate).plusDays(1)
            ChronoUnit.DAYS.between(day, nextStart).toInt().takeIf { it in plausibleIntervalDays }
        }
        .takeLast(RECENT_INTERVAL_WINDOW)
}
