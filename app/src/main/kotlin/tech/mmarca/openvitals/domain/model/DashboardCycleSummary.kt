package tech.mmarca.openvitals.domain.model

import java.time.LocalDate
import tech.mmarca.openvitals.domain.cycle.CurrentCyclePhase
import tech.mmarca.openvitals.domain.cycle.CycleEstimateResult
import tech.mmarca.openvitals.domain.cycle.CycleStatistics

/** What the dashboard tile and the metric widget show of the cycle: the recorded day, the phase, the estimate. */
data class DashboardCycleSummary(
    val currentCycleDay: Int?,
    val cycleStartDate: LocalDate?,
    val phase: CurrentCyclePhase,
    val estimate: CycleEstimateResult,
)

fun CycleStatistics.toDashboardSummary(): DashboardCycleSummary = DashboardCycleSummary(
    currentCycleDay = currentCycleDay,
    cycleStartDate = currentCycle?.startDate,
    phase = currentPhase,
    estimate = estimate,
)
