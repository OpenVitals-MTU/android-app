package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CycleRecordValues

/** The estimate's boundaries. */
class CycleEstimateCalculatorTest {

    @Test
    fun `fewer than two starts gives no estimate`() {
        assertNull(CycleEstimateCalculator.estimateNextPeriod(listOf(cycle("2025-01-01"))))
        assertNull(CycleEstimateCalculator.estimateNextPeriod(emptyList()))
    }

    @Test
    fun `missing history and unmodellable intervals are told apart`() {
        assertEquals(
            CycleEstimateResult.NeedsMoreHistory,
            CycleEstimateCalculator.evaluate(listOf(cycle("2025-01-01"))),
        )
        // Every interval exceeds 90 days. This user has recorded enough.
        val long = listOf(cycle("2025-01-01"), cycle("2025-05-01"), cycle("2025-09-01"))
        assertEquals(CycleEstimateResult.IntervalsOutOfRange, CycleEstimateCalculator.evaluate(long))
    }

    @Test
    fun `a single interval gives a wide low-confidence range`() {
        val estimate = requireNotNull(
            CycleEstimateCalculator.estimateNextPeriod(listOf(cycle("2025-01-01"), cycle("2025-01-29"))),
        )

        assertEquals(1, estimate.cycleCount)
        assertEquals(LocalDate.parse("2025-02-26"), estimate.centralDate)
        // One interval carries no variability, so the undeclared prior sets the radius: ceil(1.96 * 4.54).
        assertEquals(9, radiusOf(estimate))
        assertEquals(0, estimate.variabilityDays)
    }

    @Test
    fun `the window is symmetric around the central date`() {
        val estimate = requireNotNull(
            CycleEstimateCalculator.estimateNextPeriod(
                listOf(cycle("2025-01-01"), cycle("2025-01-30"), cycle("2025-02-26")),
            ),
        )
        assertEquals(
            ChronoUnit.DAYS.between(estimate.earliestDate, estimate.centralDate),
            ChronoUnit.DAYS.between(estimate.centralDate, estimate.latestDate),
        )
    }

    @Test
    fun `a perfectly regular history keeps the three-day floor`() {
        val cycles = (0..6).map { cycle(LocalDate.parse("2025-01-01").plusDays(28L * it)) }
        val estimate = requireNotNull(CycleEstimateCalculator.estimateNextPeriod(cycles))

        assertEquals(0, estimate.variabilityDays)
        assertTrue(radiusOf(estimate) >= 3)
    }

    @Test
    fun `irregular intervals widen the window`() {
        val regular = (0..6).map { cycle(LocalDate.parse("2025-01-01").plusDays(28L * it)) }
        val irregular = listOf(
            cycle("2025-01-01"), cycle("2025-01-26"), cycle("2025-02-27"), cycle("2025-03-25"), cycle("2025-05-02"),
        )
        val regularRadius = radiusOf(requireNotNull(CycleEstimateCalculator.estimateNextPeriod(regular)))
        val irregularRadius = radiusOf(requireNotNull(CycleEstimateCalculator.estimateNextPeriod(irregular)))

        assertTrue("$irregularRadius should widen versus $regularRadius", irregularRadius > regularRadius)
    }

    @Test
    fun `uncertainty does not grow with more consistent evidence`() {
        val few = (0..1).map { cycle(LocalDate.parse("2025-01-01").plusDays(28L * it)) }
        val many = (0..6).map { cycle(LocalDate.parse("2025-01-01").plusDays(28L * it)) }
        val fewRadius = radiusOf(requireNotNull(CycleEstimateCalculator.estimateNextPeriod(few)))
        val manyRadius = radiusOf(requireNotNull(CycleEstimateCalculator.estimateNextPeriod(many)))

        assertTrue(manyRadius <= fewRadius)
    }

    @Test
    fun `the radius stays bounded for erratic history`() {
        val erratic = listOf(
            cycle("2025-01-01"), cycle("2025-01-17"), cycle("2025-04-10"), cycle("2025-04-28"), cycle("2025-07-20"),
        )
        assertTrue(radiusOf(requireNotNull(CycleEstimateCalculator.estimateNextPeriod(erratic))) <= 22)
    }

    @Test
    fun `recurring seven-day swings withdraw the population prior`() {
        val oneOff = intervalsToCycles(listOf(28, 28, 28, 36, 28, 28))
        val recurring = intervalsToCycles(listOf(28, 36, 28, 37, 29, 36))
        val steady = radiusOf(requireNotNull(CycleEstimateCalculator.estimateNextPeriod(oneOff)))
        val variable = radiusOf(requireNotNull(CycleEstimateCalculator.estimateNextPeriod(recurring)))

        assertEquals(8, steady)
        assertTrue("$variable should widen versus $steady", variable > steady)
    }

    @Test
    fun `a single large swing does not trigger the variability path`() {
        val estimate = requireNotNull(
            CycleEstimateCalculator.estimateNextPeriod(intervalsToCycles(listOf(28, 28, 28, 28, 37))),
        )
        assertTrue(radiusOf(estimate) < 12)
    }

    @Test
    fun `honest uncertainty is not truncated at fourteen days`() {
        val estimate = requireNotNull(
            CycleEstimateCalculator.estimateNextPeriod(intervalsToCycles(listOf(16, 84, 18, 83, 17, 85))),
        )
        assertTrue(radiusOf(estimate) > 14)
        assertTrue(radiusOf(estimate) <= 22)
    }

    @Test
    fun `the age band selects the prior`() {
        val cycles = intervalsToCycles(listOf(28, 28))
        val lowest = radiusOf(requireNotNull(CycleEstimateCalculator.estimateNextPeriod(cycles, AgeBand.AGE_35_39)))
        val highest = radiusOf(requireNotNull(CycleEstimateCalculator.estimateNextPeriod(cycles, AgeBand.AGE_50_PLUS)))

        assertTrue(highest > lowest)
    }

    @Test
    fun `an undeclared band is not the narrowest one`() {
        val cycles = intervalsToCycles(listOf(28, 28))
        val undeclared = radiusOf(requireNotNull(CycleEstimateCalculator.estimateNextPeriod(cycles)))
        val narrowest = radiusOf(requireNotNull(CycleEstimateCalculator.estimateNextPeriod(cycles, AgeBand.AGE_35_39)))

        assertTrue(undeclared >= narrowest)
    }

    @Test
    fun `a declared timing context widens the window and never moves the central date`() {
        val cycles = intervalsToCycles(listOf(28, 29, 28))
        val plain = requireNotNull(CycleEstimateCalculator.estimateNextPeriod(cycles))
        val declared = requireNotNull(
            CycleEstimateCalculator.estimateNextPeriod(cycles, AgeBand.AGE_50_PLUS, hasTimingContext = true),
        )

        assertTrue(radiusOf(declared) > radiusOf(plain))
        assertEquals(plain.centralDate, declared.centralDate)
    }

    @Test
    fun `observed variability outranks a declaration`() {
        val declaredOnly = radiusOf(
            requireNotNull(
                CycleEstimateCalculator.estimateNextPeriod(
                    intervalsToCycles(listOf(28, 29, 28, 29, 28, 29)),
                    hasTimingContext = true,
                ),
            ),
        )
        val observed = radiusOf(
            requireNotNull(CycleEstimateCalculator.estimateNextPeriod(intervalsToCycles(listOf(24, 40, 26, 42, 25, 41)))),
        )
        assertTrue(observed > declaredOnly)
    }

    @Test
    fun `excluded cycles are omitted from the intervals`() {
        val cycles = listOf(
            cycle("2026-01-01"),
            cycle("2026-01-29"),
            cycle("2026-02-26", excluded = true),
            cycle("2026-04-12"),
        )
        val estimate = requireNotNull(CycleEstimateCalculator.estimateNextPeriod(cycles))

        assertEquals(LocalDate.parse("2026-05-10"), estimate.centralDate)
    }

    @Test
    fun `a gap above 90 days breaks the history`() {
        val estimate = requireNotNull(CycleEstimateCalculator.estimateNextPeriod(intervalsToCycles(listOf(28, 95, 28))))

        assertEquals(1, estimate.cycleCount)
        assertEquals(9, radiusOf(estimate))
    }

    @Test
    fun `the earliest date does not fall inside recorded flow`() {
        val start = LocalDate.parse("2026-01-01")
        val next = start.plusDays(28)
        val cycles = listOf(
            cycle(start),
            RecordedCycle(
                startDate = next,
                endDate = null,
                flowDays = (0..6L).associate { next.plusDays(it) to CycleRecordValues.FLOW_MEDIUM },
            ),
        )
        val estimate = requireNotNull(CycleEstimateCalculator.estimateNextPeriod(cycles, AgeBand.AGE_50_PLUS))

        assertTrue(!estimate.earliestDate.isBefore(next.plusDays(7)))
        assertEquals(next.plusDays(28), estimate.centralDate)
    }

    private fun intervalsToCycles(intervals: List<Int>): List<RecordedCycle> {
        var date = LocalDate.parse("2025-01-01")
        val cycles = mutableListOf(cycle(date))
        for (interval in intervals) {
            date = date.plusDays(interval.toLong())
            cycles += cycle(date)
        }
        return cycles
    }

    private fun radiusOf(estimate: CycleEstimate): Int =
        ChronoUnit.DAYS.between(estimate.centralDate, estimate.latestDate).toInt()

    private fun cycle(start: String, excluded: Boolean = false) = cycle(LocalDate.parse(start), excluded)

    private fun cycle(start: LocalDate, excluded: Boolean = false) = RecordedCycle(
        startDate = start,
        endDate = null,
        isExcludedFromEstimates = excluded,
        exclusionReason = if (excluded) CycleExclusionReason.ILLNESS else null,
    )
}
