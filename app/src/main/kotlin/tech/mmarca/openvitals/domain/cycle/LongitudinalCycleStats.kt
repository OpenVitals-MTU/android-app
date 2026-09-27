package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import kotlin.math.abs

/** One bar of the variability view. */
data class LongitudinalCycleItem(
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val lengthDays: Int,
    val bleedingDaysCount: Int,
    val isCurrent: Boolean,
    val isExcluded: Boolean,
    val exclusionReason: CycleExclusionReason?,
    /** Seven days or more longer or shorter than the previous included cycle. */
    val hasStrawSwing: Boolean,
)

data class LongitudinalCycleStats(
    val totalCyclesCount: Int,
    val completedCyclesCount: Int,
    val excludedCyclesCount: Int,
    val medianDays: Double?,
    val meanDays: Double?,
    /** Newest first. */
    val items: List<LongitudinalCycleItem>,
) {
    companion object {
        val Empty = LongitudinalCycleStats(0, 0, 0, null, null, emptyList())
    }
}

/** Cycle lengths over time, with the same plausibility filter as the estimate. */
object LongitudinalCycleStatsCalculator {
    private const val SWING_DAYS = 7

    fun calculate(cycles: List<RecordedCycle>, today: LocalDate): LongitudinalCycleStats {
        val sorted = cycles.sortedBy { it.startDate }
        val completedLengths = mutableListOf<Int>()
        var previousIncludedLength: Int? = null
        val items = sorted.map { cycle ->
            val length = cycle.lengthDays(today)
            val swing = !cycle.isCurrent && previousIncludedLength?.let { abs(length - it) >= SWING_DAYS } == true
            if (!cycle.isCurrent && !cycle.isExcludedFromEstimates) {
                previousIncludedLength = length
                if (length in CycleEstimateCalculator.plausibleCycleDays) completedLengths += length
            }
            LongitudinalCycleItem(
                startDate = cycle.startDate,
                endDate = cycle.endDate,
                lengthDays = length,
                bleedingDaysCount = cycle.bleedingDayCount,
                isCurrent = cycle.isCurrent,
                isExcluded = cycle.isExcludedFromEstimates,
                exclusionReason = cycle.exclusionReason,
                hasStrawSwing = swing,
            )
        }
        return LongitudinalCycleStats(
            totalCyclesCount = items.size,
            completedCyclesCount = items.count { !it.isCurrent },
            excludedCyclesCount = items.count { it.isExcluded },
            medianDays = completedLengths.median(),
            meanDays = completedLengths.takeIf { it.isNotEmpty() }?.average(),
            items = items.reversed(),
        )
    }

    private fun List<Int>.median(): Double? {
        if (isEmpty()) return null
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle].toDouble() else (sorted[middle - 1] + sorted[middle]) / 2.0
    }
}
