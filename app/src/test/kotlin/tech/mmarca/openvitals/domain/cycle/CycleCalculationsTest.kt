package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CycleRecordValues

/**
 * Segmentation with a one-day break tolerance, day numbering, and the hand-off
 * to the estimate. Segments also drive the derived MenstruationPeriodRecord
 * spans, so regressions corrupt stored data.
 */
class CycleCalculationsTest {

    private fun day(n: Int): LocalDate = LocalDate.of(2026, 1, 1).plusDays((n - 1).toLong())

    // Segments.

    @Test
    fun `a single bleeding day is one single-day segment`() {
        val segments = CycleCalculations.bleedingSegments(listOf(day(5)))
        assertEquals(listOf(day(5)..day(5)), segments)
    }

    @Test
    fun `a one-day gap stays inside the same segment`() {
        val segments = CycleCalculations.bleedingSegments(listOf(day(1), day(2), day(4)))
        assertEquals(listOf(day(1)..day(4)), segments)
    }

    @Test
    fun `a two-day gap starts a new segment`() {
        val segments = CycleCalculations.bleedingSegments(listOf(day(1), day(2), day(5)))
        assertEquals(listOf(day(1)..day(2), day(5)..day(5)), segments)
    }

    @Test
    fun `unsorted and duplicate days are tolerated`() {
        val segments = CycleCalculations.bleedingSegments(listOf(day(2), day(1), day(2)))
        assertEquals(listOf(day(1)..day(2)), segments)
    }

    // Cycle starts.

    @Test
    fun `empty input yields empty statistics`() {
        val stats = computeDays(emptyList(), day(10))
        assertEquals(CycleStatistics(), stats)
    }

    @Test
    fun `current cycle day counts from the last start inclusive`() {
        val stats = computeDays(listOf(day(10)), day(12))
        assertEquals(3, stats.currentCycleDay)
    }

    @Test
    fun `cycle day is null when the last start is older than 99 days`() {
        val stats = computeDays(listOf(day(1)), day(1).plusDays(99))
        assertNull(stats.currentCycleDay)
    }

    @Test
    fun `cycle day is 99 at the display limit`() {
        val stats = computeDays(listOf(day(1)), day(1).plusDays(98))
        assertEquals(99, stats.currentCycleDay)
    }

    // History.

    @Test
    fun `cycles close the day before the next start and keep their flow days`() {
        val flow = mapOf(
            day(1) to CycleRecordValues.FLOW_HEAVY,
            day(2) to CycleRecordValues.FLOW_LIGHT,
            day(29) to CycleRecordValues.FLOW_MEDIUM,
        )
        val stats = CycleCalculations.compute(flow, day(30), spottingDays = setOf(day(15)))

        assertEquals(2, stats.cycles.size)
        val first = stats.cycles[0]
        assertEquals(day(28), first.endDate)
        assertEquals(2, first.flowDays.size)
        assertEquals(setOf(day(15)), first.spottingDays)
        assertEquals(3, first.bleedingDayCount)
        assertEquals(CycleRecordValues.FLOW_HEAVY, first.peakFlow)
        assertTrue(stats.cycles[1].isCurrent)
        assertEquals(stats.cycles[1], stats.currentCycle)
    }

    @Test
    fun `an exclusion belongs to the cycle that contains its date`() {
        val flow = listOf(day(1), day(29), day(57)).associateWith { CycleRecordValues.FLOW_MEDIUM }
        val stats = CycleCalculations.compute(
            flow,
            day(60),
            exclusions = mapOf(day(40) to CycleExclusionReason.ILLNESS),
        )

        assertTrue(stats.cycles[1].isExcludedFromEstimates)
        assertEquals(CycleExclusionReason.ILLNESS, stats.cycles[1].exclusionReason)
        assertTrue(stats.cycles.filterIndexed { index, _ -> index != 1 }.none { it.isExcludedFromEstimates })
    }

    // Estimate hand-off.

    private fun startsEvery(lengthDays: Long, count: Int): List<LocalDate> =
        (0 until count).map { day(1).plusDays(lengthDays * it) }

    private fun bleedingFor(starts: List<LocalDate>): List<LocalDate> =
        starts.flatMap { start -> (0..3L).map(start::plusDays) }

    /** Flow days of one level, for tests that only care about the dates. */
    private fun computeDays(days: Collection<LocalDate>, today: LocalDate): CycleStatistics =
        CycleCalculations.compute(days.associateWith { CycleRecordValues.FLOW_MEDIUM }, today)

    @Test
    fun `one start gives no estimate and no average`() {
        val stats = computeDays(bleedingFor(startsEvery(28, 1)), day(10))

        assertEquals(CycleEstimateResult.NeedsMoreHistory, stats.estimate)
        assertTrue(stats.predictedWindows.isEmpty())
        assertNull(stats.averageCycleLengthDays)
        assertTrue(stats.recentIntervalLengths.isEmpty())
    }

    @Test
    fun `two starts give a wide estimate one mean apart`() {
        val starts = startsEvery(28, 2)
        val stats = computeDays(bleedingFor(starts), starts.last().plusDays(5))

        val estimate = stats.estimate.estimateOrNull!!
        assertEquals(starts.last().plusDays(28), estimate.centralDate)
        assertEquals(listOf(estimate.window), stats.predictedWindows)
        assertEquals(28.0, stats.averageCycleLengthDays!!, 1e-9)
        assertEquals(listOf(28), stats.recentIntervalLengths)
    }

    @Test
    fun `the recent intervals are the start-to-start gaps`() {
        val starts = listOf(day(1), day(26), day(54), day(85))
        val stats = computeDays(bleedingFor(starts), day(90))

        assertEquals(listOf(25, 28, 31), stats.recentIntervalLengths.sorted())
    }

    @Test
    fun `the phase follows the estimate`() {
        val starts = startsEvery(28, 4)
        val stats = computeDays(bleedingFor(starts), starts.last().plusDays(9))

        assertEquals(
            CurrentCyclePhase.Available(CyclePhase.FOLLICULAR, PhaseCertainty.ESTIMATED),
            stats.currentPhase,
        )
    }

    @Test
    fun `an excluded cycle drops out of the intervals`() {
        val starts = listOf(day(1), day(29), day(57), day(103))
        val stats = CycleCalculations.compute(
            bleedingFor(starts).associateWith { CycleRecordValues.FLOW_MEDIUM },
            day(105),
            exclusions = mapOf(day(60) to CycleExclusionReason.STRESS_OR_TRAVEL),
        )

        assertEquals(listOf(28, 28), stats.recentIntervalLengths)
    }
}
