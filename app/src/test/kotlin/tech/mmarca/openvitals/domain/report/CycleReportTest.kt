package tech.mmarca.openvitals.domain.report

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.cycle.CycleSymptom
import tech.mmarca.openvitals.domain.cycle.RecordedCycle
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.CycleRecordValues

/** The cycle section counts what was recorded; the numbers a clinician asks for, nothing inferred. */
class CycleReportTest {
    private val start = LocalDate.of(2026, 5, 1)
    private val end = LocalDate.of(2026, 7, 31)

    private fun cycle(from: LocalDate, to: LocalDate?, flowDays: Int = 4, peak: Int = CycleRecordValues.FLOW_MEDIUM, spotting: Set<LocalDate> = emptySet(), excluded: CycleExclusionReason? = null) =
        RecordedCycle(
            startDate = from,
            endDate = to,
            flowDays = (0 until flowDays).associate { from.plusDays(it.toLong()) to (if (it == 1) peak else CycleRecordValues.FLOW_LIGHT) },
            spottingDays = spotting,
            isExcludedFromEstimates = excluded != null,
            exclusionReason = excluded,
        )

    private val first = cycle(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 5, 17))
    private val second = cycle(LocalDate.of(2026, 5, 18), LocalDate.of(2026, 6, 16), spotting = setOf(LocalDate.of(2026, 5, 19), LocalDate.of(2026, 6, 1)))
    private val third = cycle(LocalDate.of(2026, 6, 17), LocalDate.of(2026, 7, 14), excluded = CycleExclusionReason.ILLNESS)
    private val open = cycle(LocalDate.of(2026, 7, 15), null, peak = CycleRecordValues.FLOW_HEAVY)

    @Test
    fun `the daily series is the cycle day, restarting at each start, clipped to the range`() {
        val series = cycleDailySeries(listOf(first, second), start, LocalDate.of(2026, 5, 20))

        assertEquals(start, series.first().date)
        // May 1 is day 12 of a cycle that started on April 20.
        assertEquals(12.0, series.first().value, 0.0)
        assertEquals(1.0, series.first { it.date == LocalDate.of(2026, 5, 18) }.value, 0.0)
        assertEquals(LocalDate.of(2026, 5, 20), series.last().date)
        assertEquals(3.0, series.last().value, 0.0)
    }

    @Test
    fun `lengths are counted over the closed cycles, and the open one is in progress`() {
        val detail = cycleDetail(listOf(first, second, third, open), emptyList(), start, end)!!

        assertEquals(4, detail.cycles.size)
        assertEquals(3, detail.completedCycles)
        assertEquals(listOf(28, 30, 28), detail.cycles.mapNotNull { it.lengthDays })
        assertNull(detail.cycles.last().lengthDays)
        assertNull(detail.cycles.last().end)
        assertEquals(28.67, detail.meanLengthDays!!, 0.01)
        assertEquals(28.0, detail.medianLengthDays!!, 0.0)
        assertEquals(1.15, detail.sdLengthDays!!, 0.01)
        assertEquals(28, detail.minLengthDays)
        assertEquals(30, detail.maxLengthDays)
        assertTrue(detail.cycles[2].excluded)
        assertEquals(CycleExclusionReason.ILLNESS, detail.cycles[2].exclusionReason)
        assertEquals(CycleRecordValues.FLOW_HEAVY, detail.cycles.last().peakFlow)
    }

    @Test
    fun `bleeding counts the range's days, and spotting a week past a start is intermenstrual`() {
        val detail = cycleDetail(listOf(first, second), emptyList(), start, end)!!

        // The first cycle's flow days are in April, outside the range; the second's four are in.
        assertEquals(4, detail.bleedingDays)
        assertEquals(2, detail.spottingDays)
        assertEquals(1, detail.intermenstrualDays)
        // Bleeding days per cycle count flow and distinct spotting days: 4 and 5.
        assertEquals(4.5, detail.meanBleedingDays!!, 0.0)
    }

    @Test
    fun `pain and symptoms are split by bleeding days, and notes keep their order`() {
        val entries = listOf(
            CycleJournalEntry(date = LocalDate.of(2026, 5, 18), painLevel = 4, symptoms = setOf(CycleSymptom.CRAMPS), notes = "heavy start"),
            CycleJournalEntry(date = LocalDate.of(2026, 5, 19), painLevel = 2, symptoms = setOf(CycleSymptom.CRAMPS, CycleSymptom.HEADACHE)),
            CycleJournalEntry(date = LocalDate.of(2026, 6, 10), painLevel = 1, symptoms = setOf(CycleSymptom.HEADACHE), notes = "calm"),
            CycleJournalEntry(date = LocalDate.of(2026, 6, 12), painLevel = 5),
        )

        val detail = cycleDetail(listOf(second), entries, start, end)!!

        assertEquals(2, detail.painDaysOnBleeding)
        assertEquals(1, detail.painDaysOffBleeding)
        assertEquals(2, detail.severePainDays)
        assertEquals(3.0, detail.meanPain!!, 0.0)
        assertEquals(CycleSymptom.CRAMPS, detail.symptomFrequency.first().symptom)
        assertEquals(2 to 0, detail.symptomFrequency.first().let { it.onBleeding to it.offBleeding })
        assertEquals(1 to 1, detail.symptomFrequency[1].let { it.onBleeding to it.offBleeding })
        assertEquals(listOf("heavy start", "calm"), detail.notes.map { it.text })
    }

    @Test
    fun `nothing in the range means no section`() {
        assertNull(cycleDetail(listOf(first), emptyList(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
        assertTrue(cycleDailySeries(listOf(first), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)).isEmpty())
    }
}
