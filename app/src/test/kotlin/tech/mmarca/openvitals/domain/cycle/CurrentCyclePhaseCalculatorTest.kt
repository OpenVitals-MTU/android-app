package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CycleRecordValues

/** The phase boundaries. */
class CurrentCyclePhaseCalculatorTest {
    private val start = LocalDate.parse("2026-07-01")
    private val central = LocalDate.parse("2026-07-30")
    private val cycle = RecordedCycle(startDate = start, endDate = null)

    @Test
    fun `the cycle start is a recorded menstrual phase`() {
        assertPhase(start, CyclePhase.MENSTRUAL, PhaseCertainty.RECORDED)
    }

    @Test
    fun `flow today is a recorded menstrual phase whatever the day`() {
        assertPhase(start.plusDays(3), CyclePhase.MENSTRUAL, PhaseCertainty.RECORDED, DayBleeding.FLOW)
        assertPhase(start.plusDays(8), CyclePhase.MENSTRUAL, PhaseCertainty.RECORDED, DayBleeding.FLOW)
    }

    @Test
    fun `a flow day recorded in the cycle counts without a journal entry`() {
        val withFlow = cycle.copy(flowDays = mapOf(start.plusDays(3) to CycleRecordValues.FLOW_LIGHT))
        assertEquals(
            CurrentCyclePhase.Available(CyclePhase.MENSTRUAL, PhaseCertainty.RECORDED),
            CurrentCyclePhaseCalculator.evaluate(start.plusDays(3), withFlow, null, result(5)),
        )
    }

    @Test
    fun `missing early bleeding detail stays indeterminate`() {
        assertReason(start.plusDays(3), PhaseIndeterminateReason.EARLY_CYCLE_WITHOUT_BLEEDING_DETAIL)
        assertReason(
            start.plusDays(3),
            PhaseIndeterminateReason.EARLY_CYCLE_WITHOUT_BLEEDING_DETAIL,
            DayBleeding.SPOTTING,
        )
    }

    @Test
    fun `an explicit none in the early cycle moves on to the estimate`() {
        assertPhase(start.plusDays(3), CyclePhase.FOLLICULAR, PhaseCertainty.ESTIMATED, DayBleeding.NONE)
    }

    @Test
    fun `before the ovulation band is estimated follicular`() {
        assertPhase(start.plusDays(8), CyclePhase.FOLLICULAR, PhaseCertainty.ESTIMATED)
        assertPhase(LocalDate.parse("2026-07-14"), CyclePhase.FOLLICULAR, PhaseCertainty.ESTIMATED)
    }

    @Test
    fun `the ovulation band is two days around the luteal anchor`() {
        assertReason(LocalDate.parse("2026-07-15"), PhaseIndeterminateReason.PHASE_TRANSITION)
        assertReason(LocalDate.parse("2026-07-17"), PhaseIndeterminateReason.PHASE_TRANSITION)
        assertReason(LocalDate.parse("2026-07-19"), PhaseIndeterminateReason.PHASE_TRANSITION)
        assertPhase(LocalDate.parse("2026-07-20"), CyclePhase.LUTEAL, PhaseCertainty.ESTIMATED)
    }

    @Test
    fun `a stable history permits the central ovulatory label only`() {
        val stable = result(radius = 5, cycleCount = 6, variabilityDays = 4)
        assertEquals(
            CurrentCyclePhase.Available(CyclePhase.OVULATORY, PhaseCertainty.ESTIMATED),
            CurrentCyclePhaseCalculator.evaluate(LocalDate.parse("2026-07-17"), cycle, null, stable),
        )
        val variable = result(radius = 5, cycleCount = 6, variabilityDays = 8)
        assertEquals(
            CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.PHASE_TRANSITION),
            CurrentCyclePhaseCalculator.evaluate(LocalDate.parse("2026-07-17"), cycle, null, variable),
        )
    }

    @Test
    fun `the luteal span does not depend on the radius`() {
        for (radius in listOf(4, 5, 9, 11, 22)) {
            assertEquals(
                CurrentCyclePhase.Available(CyclePhase.LUTEAL, PhaseCertainty.ESTIMATED),
                CurrentCyclePhaseCalculator.evaluate(LocalDate.parse("2026-07-20"), cycle, null, result(radius)),
            )
        }
    }

    @Test
    fun `the next period window starts two days before the central date`() {
        assertPhase(LocalDate.parse("2026-07-27"), CyclePhase.LUTEAL, PhaseCertainty.ESTIMATED)
        assertReason(LocalDate.parse("2026-07-28"), PhaseIndeterminateReason.NEXT_PERIOD_WINDOW)
        assertReason(LocalDate.parse("2026-08-04"), PhaseIndeterminateReason.NEXT_PERIOD_WINDOW)
        assertReason(LocalDate.parse("2026-08-05"), PhaseIndeterminateReason.ESTIMATE_EXPIRED)
    }

    @Test
    fun `insufficient and unsupported histories keep distinct reasons`() {
        val today = start.plusDays(8)
        assertEquals(
            CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.NEEDS_MORE_HISTORY),
            CurrentCyclePhaseCalculator.evaluate(today, cycle, DayBleeding.NONE, CycleEstimateResult.NeedsMoreHistory),
        )
        assertEquals(
            CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.INTERVALS_OUT_OF_RANGE),
            CurrentCyclePhaseCalculator.evaluate(today, cycle, DayBleeding.NONE, CycleEstimateResult.IntervalsOutOfRange),
        )
    }

    @Test
    fun `no current cycle is its own reason`() {
        assertEquals(
            CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.NO_CURRENT_CYCLE),
            CurrentCyclePhaseCalculator.evaluate(start, null, null, result(5)),
        )
        assertEquals(
            CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.NO_CURRENT_CYCLE),
            CurrentCyclePhaseCalculator.evaluate(start.minusDays(1), cycle, null, result(5)),
        )
    }

    private fun assertPhase(
        today: LocalDate,
        phase: CyclePhase,
        certainty: PhaseCertainty,
        bleeding: DayBleeding? = null,
    ) {
        assertEquals(
            CurrentCyclePhase.Available(phase, certainty),
            CurrentCyclePhaseCalculator.evaluate(today, cycle, bleeding, result(5)),
        )
    }

    private fun assertReason(today: LocalDate, reason: PhaseIndeterminateReason, bleeding: DayBleeding? = null) {
        assertEquals(
            CurrentCyclePhase.Indeterminate(reason),
            CurrentCyclePhaseCalculator.evaluate(today, cycle, bleeding, result(5)),
        )
    }

    private fun result(radius: Int, cycleCount: Int = 3, variabilityDays: Int = 4) =
        CycleEstimateResult.Available(
            CycleEstimate(
                earliestDate = central.minusDays(radius.toLong()),
                centralDate = central,
                latestDate = central.plusDays(radius.toLong()),
                cycleCount = cycleCount,
                variabilityDays = variabilityDays,
            ),
        )
}
