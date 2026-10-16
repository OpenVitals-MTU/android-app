package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How often one symptom was recorded, and in which phase. Counts only, no inference. */
data class SymptomPattern(
    val symptom: CycleSymptom,
    val totalOccurrences: Int,
    val cycleCount: Int,
    val phaseBreakdown: Map<CyclePhase, Int>,
    val mostFrequentPhase: CyclePhase?,
)

/** One day's recorded symptoms, the input of [SymptomPatternCalculator]. */
data class SymptomDay(
    val date: LocalDate,
    val symptoms: Set<CycleSymptom>,
)

/**
 * Symptom counts by phase across all cycles. The phase here
 * is a coarse retrospective split of each closed cycle (menstrual: first five
 * days or any bleeding day; luteal: the last fourteen), which is enough to
 * describe co-occurrence and nothing more.
 */
object SymptomPatternCalculator {
    private const val MENSTRUAL_DAYS = 5
    private const val LUTEAL_DAYS = 14
    private const val MIN_CYCLE_LENGTH = 10
    private const val OPEN_CYCLE_LENGTH = 28

    fun calculate(
        cycles: List<RecordedCycle>,
        days: List<SymptomDay>,
        bleedingDays: Set<LocalDate>,
    ): List<SymptomPattern> {
        val recorded = days.filter { it.symptoms.isNotEmpty() }
        if (recorded.isEmpty()) return emptyList()
        val sorted = cycles.sortedBy { it.startDate }

        class Accumulator {
            var total = 0
            val cycleKeys = mutableSetOf<String>()
            val byPhase = CyclePhase.entries.associateWithTo(mutableMapOf()) { 0 }
        }

        val accumulators = mutableMapOf<CycleSymptom, Accumulator>()
        for (day in recorded) {
            val (cycle, phase) = resolve(day.date, sorted, bleedingDays)
            val cycleKey = cycle?.startDate?.toString() ?: "unassigned_${day.date.year}_${day.date.monthValue}"
            for (symptom in day.symptoms) {
                val accumulator = accumulators.getOrPut(symptom) { Accumulator() }
                accumulator.total++
                accumulator.cycleKeys += cycleKey
                accumulator.byPhase[phase] = accumulator.byPhase.getValue(phase) + 1
            }
        }
        return accumulators.map { (symptom, accumulator) ->
            val top = accumulator.byPhase.values.max()
            SymptomPattern(
                symptom = symptom,
                totalOccurrences = accumulator.total,
                cycleCount = accumulator.cycleKeys.size,
                phaseBreakdown = accumulator.byPhase.toMap(),
                mostFrequentPhase = if (top > 0) accumulator.byPhase.entries.first { it.value == top }.key else null,
            )
        }.sortedWith(compareByDescending<SymptomPattern> { it.totalOccurrences }.thenBy { it.symptom.id })
    }

    private fun resolve(
        date: LocalDate,
        sorted: List<RecordedCycle>,
        bleedingDays: Set<LocalDate>,
    ): Pair<RecordedCycle?, CyclePhase> {
        val index = sorted.indexOfLast { !it.startDate.isAfter(date) }
        val cycle = sorted.getOrNull(index)
        if (date in bleedingDays) return cycle to CyclePhase.MENSTRUAL
        if (cycle == null || !cycle.contains(date)) return null to CyclePhase.FOLLICULAR

        val length = (cycle.endDate?.let { ChronoUnit.DAYS.between(cycle.startDate, it).toInt() + 1 } ?: OPEN_CYCLE_LENGTH)
            .coerceAtLeast(MIN_CYCLE_LENGTH)
        val dayIndex = ChronoUnit.DAYS.between(cycle.startDate, date).toInt()
        val lutealStart = maxOf(MENSTRUAL_DAYS + 1, length - LUTEAL_DAYS)
        val ovulatoryStart = maxOf(MENSTRUAL_DAYS, lutealStart - 2)
        val phase = when {
            dayIndex < MENSTRUAL_DAYS -> CyclePhase.MENSTRUAL
            dayIndex in ovulatoryStart until lutealStart -> CyclePhase.OVULATORY
            dayIndex >= lutealStart -> CyclePhase.LUTEAL
            else -> CyclePhase.FOLLICULAR
        }
        return cycle to phase
    }
}
