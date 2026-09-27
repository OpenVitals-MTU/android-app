package tech.mmarca.openvitals.features.cycle

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.cycle.BbtReading
import tech.mmarca.openvitals.domain.cycle.CurrentCyclePhase
import tech.mmarca.openvitals.domain.cycle.CycleEstimateResult
import tech.mmarca.openvitals.domain.cycle.CycleFact
import tech.mmarca.openvitals.domain.cycle.LongitudinalCycleStats
import tech.mmarca.openvitals.domain.cycle.PhaseIndeterminateReason
import tech.mmarca.openvitals.domain.cycle.PhaseTip
import tech.mmarca.openvitals.domain.cycle.RecordedCycle
import tech.mmarca.openvitals.domain.cycle.SymptomPattern
import tech.mmarca.openvitals.domain.cycle.ThermalShiftResult
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.DayBleedingChoice

@Immutable
data class CycleDisplayState(
    val selectedPeriod: DatePeriod = DatePeriod(LocalDate.now(), LocalDate.now()),
    val hasData: Boolean = false,
    val summary: CyclePeriodSummary = CyclePeriodSummary(),
    val calendarDays: List<CycleDay> = emptyList(),
    val trackedDates: List<LocalDate> = emptyList(),
    val sampleCount: Int = 0,
    val sources: List<String> = emptyList(),
    val today: CycleTodayDisplay = CycleTodayDisplay(),
    val history: LongitudinalCycleStats = LongitudinalCycleStats.Empty,
    val thermal: CycleThermalDisplay? = null,
    val patterns: List<SymptomPattern> = emptyList(),
)

@Immutable
data class CyclePeriodSummary(
    val periodDays: Int = 0,
    val ovulationTestCount: Int = 0,
    val bbtReadingCount: Int = 0,
    val totalEntryCount: Int = 0,
)

/** The top of the cycle screen: today, regardless of the period being browsed. */
@Immutable
data class CycleTodayDisplay(
    val today: LocalDate = LocalDate.now(),
    val currentCycle: RecordedCycle? = null,
    val cycleDay: Int? = null,
    val phase: CurrentCyclePhase = CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.NO_CURRENT_CYCLE),
    val estimate: CycleEstimateResult = CycleEstimateResult.NeedsMoreHistory,
    /** Shown when the phase is known. */
    val tip: PhaseTip? = null,
    /** Shown when it is not. */
    val fact: CycleFact? = null,
    val journal: CycleJournalEntry? = null,
    val bleeding: DayBleedingChoice? = null,
    val recentIntervalLengths: List<Int> = emptyList(),
    val totalCycles: Int = 0,
    /** Whether a context or an age band was declared. Drives the setup card. */
    val hasProfile: Boolean = false,
) {
    val hasCycleHistory: Boolean
        get() = totalCycles > 0
}

/** The current cycle's morning temperatures and the shift rule's verdict. */
@Immutable
data class CycleThermalDisplay(
    val cycleStart: LocalDate,
    val readings: List<BbtReading>,
    val shift: ThermalShiftResult,
)
