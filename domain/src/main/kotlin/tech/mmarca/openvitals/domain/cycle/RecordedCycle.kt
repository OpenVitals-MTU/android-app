package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Why the user keeps a cycle out of the estimate. Never inferred. */
enum class CycleExclusionReason {
    ILLNESS,
    MEDICAL_TREATMENT,
    CONTRACEPTION_CHANGE,
    STRESS_OR_TRAVEL,
    OTHER,
    ;

    companion object {
        fun fromName(name: String?): CycleExclusionReason? =
            entries.firstOrNull { it.name == name }
    }
}

/**
 * One cycle derived from recorded bleeding. It starts on a flow day that follows
 * more than [CycleCalculations.MaxBreakInBleedingDays] free days and ends the day
 * before the next start.
 */
data class RecordedCycle(
    val startDate: LocalDate,
    /** The day before the next start, or null while the cycle is open. */
    val endDate: LocalDate?,
    /** Recorded flow level per day, from `CycleRecordValues`. */
    val flowDays: Map<LocalDate, Int> = emptyMap(),
    /** Spotting days inside the cycle. They count as bleeding, not as period flow. */
    val spottingDays: Set<LocalDate> = emptySet(),
    val isExcludedFromEstimates: Boolean = false,
    val exclusionReason: CycleExclusionReason? = null,
) {
    val isCurrent: Boolean
        get() = endDate == null

    /** Days from the start to [today] inclusive, or to the end for a closed cycle. */
    fun lengthDays(today: LocalDate): Int {
        val last = endDate ?: today
        return ChronoUnit.DAYS.between(startDate, last).toInt() + 1
    }

    val bleedingDayCount: Int
        get() = (flowDays.keys + spottingDays).size

    /** The last day with period flow. Spotting does not count. */
    val lastFlowDay: LocalDate?
        get() = flowDays.keys.maxOrNull()

    /** The heaviest flow level recorded, or null without flow. */
    val peakFlow: Int?
        get() = flowDays.values.maxOrNull()

    fun contains(date: LocalDate): Boolean =
        !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate))
}

/** Builds [RecordedCycle]s from bleeding days and the user's exclusions. */
object CycleHistory {

    /**
     * @param flowDays flow level per day, light to heavy.
     * @param spottingDays intermenstrual bleeding days; they never start a cycle.
     * @param exclusions reason per excluded date. An exclusion belongs to the
     *   cycle that contains its date, so a start that moves by a day keeps it.
     */
    fun build(
        flowDays: Map<LocalDate, Int>,
        spottingDays: Set<LocalDate> = emptySet(),
        exclusions: Map<LocalDate, CycleExclusionReason?> = emptyMap(),
    ): List<RecordedCycle> {
        val segments = CycleCalculations.bleedingSegments(flowDays.keys)
        val starts = segments.map { it.start }
        return starts.mapIndexed { index, start ->
            val end = starts.getOrNull(index + 1)?.minusDays(1)
            val inCycle = { date: LocalDate ->
                !date.isBefore(start) && (end == null || !date.isAfter(end))
            }
            val exclusion = exclusions.entries.firstOrNull { inCycle(it.key) }
            RecordedCycle(
                startDate = start,
                endDate = end,
                flowDays = flowDays.filterKeys(inCycle),
                spottingDays = spottingDays.filterTo(mutableSetOf(), inCycle),
                isExcludedFromEstimates = exclusion != null,
                exclusionReason = exclusion?.value,
            )
        }
    }
}
