package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The statistical cycle rules: segmentation with a one-day break tolerance, the three-cycle
 * prediction threshold, the SD-driven window width, and day numbering. They also drive the
 * derived MenstruationPeriodRecord spans, so regressions corrupt stored data.
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
        val stats = CycleCalculations.compute(emptyList(), day(10))
        assertEquals(CycleStatistics(), stats)
    }

    @Test
    fun `current cycle day counts from the last start inclusive`() {
        val stats = CycleCalculations.compute(listOf(day(10)), day(12))
        assertEquals(3, stats.currentCycleDay)
    }

    @Test
    fun `cycle day is null when the last start is older than 99 days`() {
        val stats = CycleCalculations.compute(listOf(day(1)), day(1).plusDays(99))
        assertNull(stats.currentCycleDay)
    }

    @Test
    fun `cycle day is 99 at the display limit`() {
        val stats = CycleCalculations.compute(listOf(day(1)), day(1).plusDays(98))
        assertEquals(99, stats.currentCycleDay)
    }

    // Predictions.

    private fun startsEvery(lengthDays: Long, count: Int): List<LocalDate> =
        (0 until count).map { day(1).plusDays(lengthDays * it) }

    private fun bleedingFor(starts: List<LocalDate>): List<LocalDate> =
        starts.flatMap { start -> (0..3L).map(start::plusDays) }

    @Test
    fun `three starts means two completed cycles and no prediction`() {
        val stats = CycleCalculations.compute(bleedingFor(startsEvery(28, 3)), day(80))
        assertTrue(stats.predictedWindows.isEmpty())
        assertNull(stats.averageCycleLengthDays)
    }

    @Test
    fun `four regular starts predict three windows one mean apart`() {
        val starts = startsEvery(28, 4)
        val stats = CycleCalculations.compute(bleedingFor(starts), starts.last().plusDays(5))

        assertEquals(28.0, stats.averageCycleLengthDays!!, 1e-9)
        assertEquals(3, stats.predictedWindows.size)
        val lastStart = starts.last()
        stats.predictedWindows.forEachIndexed { index, window ->
            val predicted = lastStart.plusDays(28L * (index + 1))
            assertEquals(predicted.minusDays(1)..predicted.plusDays(1), window)
        }
    }

    @Test
    fun `steady cycles get a narrow one-day window`() {
        val stats = CycleCalculations.compute(bleedingFor(startsEvery(28, 4)), day(95))
        val window = stats.predictedWindows.first()
        assertEquals(2L, java.time.temporal.ChronoUnit.DAYS.between(window.start, window.endInclusive))
    }

    @Test
    fun `irregular cycles widen the window to two days`() {
        // Lengths 25, 28, 31 -> SD ~2.45 >= 1.5.
        val starts = listOf(day(1), day(26), day(54), day(85))
        val stats = CycleCalculations.compute(bleedingFor(starts), day(90))
        val window = stats.predictedWindows.first()
        assertEquals(4L, java.time.temporal.ChronoUnit.DAYS.between(window.start, window.endInclusive))
    }

    @Test
    fun `the mean is rounded half-up when placing windows`() {
        // Lengths 28, 28, 29, 29 -> mean 28.5 -> distance 29.
        val starts = listOf(day(1), day(29), day(57), day(86), day(115))
        val stats = CycleCalculations.compute(bleedingFor(starts), day(116))
        assertEquals(day(115).plusDays(29).minusDays(1), stats.predictedWindows.first().start)
    }

    @Test
    fun `prediction is suppressed for implausibly short cycles`() {
        // Mean length 5: distance - 5 < variation, windows would overlap.
        val starts = listOf(day(1), day(6), day(11), day(16))
        val bleeding = starts.map { listOf(it) }.flatten()
        val stats = CycleCalculations.compute(bleeding, day(17))
        assertTrue(stats.predictedWindows.isEmpty())
    }
}
