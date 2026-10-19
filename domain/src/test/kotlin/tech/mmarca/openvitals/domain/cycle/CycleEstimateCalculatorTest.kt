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

    @Test
    fun `without markers the estimate counts from the calendar`() {
        val cycles = closedCycles("2025-01-01", 28, 28, 28)

        val estimate = requireNotNull(CycleEstimateCalculator.evaluate(cycles, markers = emptyMap()).estimateOrNull)

        assertEquals(CycleEstimateBasis.Calendar(), estimate.basis)
        assertEquals(LocalDate.parse("2025-04-23"), estimate.centralDate)
    }

    @Test
    fun `an LH test in the current cycle anchors the estimate on the published prior`() {
        val cycles = closedCycles("2025-01-01", 28, 28, 28)
        val lh = LocalDate.parse("2025-04-10")

        val estimate = requireNotNull(
            CycleEstimateCalculator.evaluate(cycles, markers = currentMarkers(cycles, OvulationMarker.LH_TEST to lh)).estimateOrNull,
        )

        // 13.77 days after the positive test; one-sided ceil(1.96 * 1.86) = 4.
        assertEquals(lh.plusDays(14), estimate.centralDate)
        assertEquals(4, radiusOf(estimate))
        assertEquals(CycleEstimateBasis.Marker(OvulationMarker.LH_TEST, lh, emptySet(), 0), estimate.basis)
    }

    @Test
    fun `a first cycle with a marker gets an estimate before any interval exists`() {
        val only = listOf(cycle("2025-01-01"))
        val shift = LocalDate.parse("2025-01-17")

        val estimate = CycleEstimateCalculator.evaluate(only, markers = mapOf(only[0].startDate to mapOf(OvulationMarker.TEMPERATURE to shift)))

        assertEquals(shift.plusDays(11), requireNotNull(estimate.estimateOrNull).centralDate)
    }

    @Test
    fun `the user's own marker intervals outweigh the prior and narrow the window`() {
        val cycles = closedCycles("2025-01-01", 30, 30, 30, 30)
        // Each earlier cycle: first high day on cycle day 21, next start nine days later.
        val history = cycles.dropLast(1).associate { it.startDate to mapOf(OvulationMarker.TEMPERATURE to it.startDate.plusDays(20)) }
        val shift = cycles.last().startDate.plusDays(20)

        val estimate = requireNotNull(
            CycleEstimateCalculator.evaluate(
                cycles,
                markers = history + (cycles.last().startDate to mapOf(OvulationMarker.TEMPERATURE to shift)),
            ).estimateOrNull,
        )

        // (4 × 10 + 2 × 11.0) / 6 = 10.33 → 10 days; variance (3 × 0 + 2 × 4.28) / 5 → ceil(1.96 × 1.31) = 3.
        assertEquals(shift.plusDays(10), estimate.centralDate)
        assertEquals(3, radiusOf(estimate))
        assertEquals(4, (estimate.basis as CycleEstimateBasis.Marker).intervalCount)
    }

    @Test
    fun `mucus and temperature that agree confirm each other`() {
        val cycles = closedCycles("2025-01-01", 28, 28)
        val shift = LocalDate.parse("2025-03-15")

        val estimate = requireNotNull(
            CycleEstimateCalculator.evaluate(
                cycles,
                markers = currentMarkers(cycles, OvulationMarker.TEMPERATURE to shift, OvulationMarker.MUCUS to shift.minusDays(2)),
            ).estimateOrNull,
        )

        val basis = estimate.basis as CycleEstimateBasis.Marker
        assertEquals(OvulationMarker.TEMPERATURE, basis.anchor)
        assertEquals(setOf(OvulationMarker.TEMPERATURE, OvulationMarker.MUCUS), basis.confirmedBy)
    }

    @Test
    fun `markers that disagree fall back to the calendar and say so`() {
        val cycles = closedCycles("2025-01-01", 28, 28)
        val shift = LocalDate.parse("2025-03-15")
        val calendar = requireNotNull(CycleEstimateCalculator.evaluate(cycles).estimateOrNull)

        val estimate = requireNotNull(
            CycleEstimateCalculator.evaluate(
                cycles,
                markers = currentMarkers(cycles, OvulationMarker.TEMPERATURE to shift, OvulationMarker.LH_TEST to shift.plusDays(5)),
            ).estimateOrNull,
        )

        assertEquals(calendar.copy(basis = CycleEstimateBasis.Calendar(markersDisagree = true)), estimate)
    }

    @Test
    fun `an excluded current cycle keeps the calendar estimate`() {
        val cycles = closedCycles("2025-01-01", 28, 28).let { it.dropLast(1) + it.last().copy(isExcludedFromEstimates = true) }

        val estimate = CycleEstimateCalculator.evaluate(
            cycles,
            markers = currentMarkers(cycles, OvulationMarker.LH_TEST to LocalDate.parse("2025-03-12")),
        )

        assertEquals(CycleEstimateBasis.Calendar(), requireNotNull(estimate.estimateOrNull).basis)
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

    /** Closed cycles from [first] with the given lengths, then an open current cycle. */
    private fun closedCycles(first: String, vararg lengths: Int): List<RecordedCycle> {
        val starts = lengths.runningFold(LocalDate.parse(first)) { date, length -> date.plusDays(length.toLong()) }
        return starts.mapIndexed { index, start ->
            RecordedCycle(startDate = start, endDate = starts.getOrNull(index + 1)?.minusDays(1))
        }
    }

    private fun currentMarkers(cycles: List<RecordedCycle>, vararg markers: Pair<OvulationMarker, LocalDate>) =
        mapOf(cycles.last().startDate to markers.toMap())

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
