package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Cross-cycle statistics from recorded bleeding. Everything degrades with sparse
 * data: the estimate needs two starts, the phase needs an estimate.
 */
data class CycleStatistics(
    val cycleStarts: List<LocalDate> = emptyList(),
    /** Blank past [CycleCalculations.MaxDisplayableCycleDay] days. */
    val currentCycleDay: Int? = null,
    /** Mean of the intervals the estimate uses. */
    val averageCycleLengthDays: Double? = null,
    /** The estimated window, when there is one. Kept as a list for the calendar. */
    val predictedWindows: List<ClosedRange<LocalDate>> = emptyList(),
    val cycles: List<RecordedCycle> = emptyList(),
    val estimate: CycleEstimateResult = CycleEstimateResult.NeedsMoreHistory,
    val currentPhase: CurrentCyclePhase =
        CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.NO_CURRENT_CYCLE),
    val recentIntervalLengths: List<Int> = emptyList(),
) {
    val currentCycle: RecordedCycle?
        get() = cycles.lastOrNull()?.takeIf { it.isCurrent }
}

/**
 * Pure cycle arithmetic on local days. A cycle starts on a flow day preceded
 * by more than [MaxBreakInBleedingDays] free days. The estimate and the phase
 * live in [CycleEstimateCalculator] and [CurrentCyclePhaseCalculator].
 * A timezone change can shift a day.
 */
object CycleCalculations {

    const val MaxBreakInBleedingDays = 1
    const val MaxDisplayableCycleDay = 99

    /** Groups bleeding days into segments tolerating gaps of [maxBreakDays]. Each start is a cycle start. */
    fun bleedingSegments(
        bleedingDays: Collection<LocalDate>,
        maxBreakDays: Int = MaxBreakInBleedingDays,
    ): List<ClosedRange<LocalDate>> {
        if (bleedingDays.isEmpty()) return emptyList()
        val sorted = bleedingDays.toSortedSet().toList()
        val segments = mutableListOf<ClosedRange<LocalDate>>()
        var start = sorted.first()
        var end = sorted.first()
        for (day in sorted.drop(1)) {
            val gap = ChronoUnit.DAYS.between(end, day) - 1
            if (gap > maxBreakDays) {
                segments.add(start..end)
                start = day
            }
            end = day
        }
        segments.add(start..end)
        return segments
    }

    /**
     * @param flowDays period flow per day; these define the cycles.
     * @param spottingDays intermenstrual bleeding; counted, never a start.
     * @param exclusions cycles the user keeps out of the estimate, by a date inside them.
     * @param profile the declared contexts and age band.
     * @param todayBleeding what the journal says about today, when the flow days do not.
     */
    fun compute(
        flowDays: Map<LocalDate, Int>,
        today: LocalDate,
        spottingDays: Set<LocalDate> = emptySet(),
        exclusions: Map<LocalDate, CycleExclusionReason?> = emptyMap(),
        profile: CycleTrackingProfile = CycleTrackingProfile(),
        todayBleeding: DayBleeding? = null,
    ): CycleStatistics {
        val cycles = CycleHistory.build(flowDays, spottingDays, exclusions)
        if (cycles.isEmpty()) return CycleStatistics()

        val lastStart = cycles.last().startDate
        val daysSinceStart = ChronoUnit.DAYS.between(lastStart, today)
        val currentCycleDay = (daysSinceStart + 1)
            .takeIf { daysSinceStart >= 0 && it <= MaxDisplayableCycleDay }
            ?.toInt()

        val estimate = CycleEstimateCalculator.evaluate(
            cycles = cycles,
            ageBand = profile.ageBand,
            hasTimingContext = profile.hasTimingContext,
        )
        val recent = CycleEstimateCalculator.recentIntervalLengths(cycles)
        val phase = CurrentCyclePhaseCalculator.evaluate(
            today = today,
            currentCycle = cycles.last().takeIf { it.isCurrent },
            todayBleeding = todayBleeding,
            estimateResult = estimate,
        )
        return CycleStatistics(
            cycleStarts = cycles.map { it.startDate },
            currentCycleDay = currentCycleDay,
            averageCycleLengthDays = recent.takeIf { it.isNotEmpty() }?.average(),
            predictedWindows = listOfNotNull(estimate.estimateOrNull?.window),
            cycles = cycles,
            estimate = estimate,
            currentPhase = phase,
            recentIntervalLengths = recent,
        )
    }

}
