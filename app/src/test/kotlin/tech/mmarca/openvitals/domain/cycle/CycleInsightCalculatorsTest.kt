package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CycleRecordValues

/** The thermal shift, symptom pattern, longitudinal stats, catalog, tips and facts. */
class CycleInsightCalculatorsTest {

    private val start = LocalDate.parse("2026-03-01")

    private fun readings(vararg celsius: Double, disturbedAt: Set<Int> = emptySet()) =
        celsius.mapIndexed { index, value -> BbtReading(start.plusDays(index.toLong()), value, index in disturbedAt) }

    // Thermal shift.

    @Test
    fun `six lows and three highs confirm a shift with the highest low as coverline`() {
        val result = ThermalShiftCalculator.evaluateCycle(
            start,
            readings(36.4, 36.5, 36.3, 36.45, 36.4, 36.5, 36.6, 36.65, 36.75),
        )
        val confirmed = result as ThermalShiftResult.Confirmed

        assertEquals(36.5, confirmed.coverlineCelsius, 1e-9)
        assertEquals(start.plusDays(6), confirmed.firstHighDay)
        assertEquals(6, confirmed.baselineLowTemps.size)
        assertEquals(3, confirmed.highTemps.size)
    }

    @Test
    fun `the third high must clear the coverline by two tenths`() {
        val result = ThermalShiftCalculator.evaluateCycle(
            start,
            readings(36.4, 36.5, 36.3, 36.45, 36.4, 36.5, 36.6, 36.65, 36.68),
        )
        assertEquals(ThermalShiftResult.None, result)
    }

    @Test
    fun `disturbed readings are skipped and gaps over three days break the run`() {
        val skipped = ThermalShiftCalculator.evaluateCycle(
            start,
            readings(36.4, 36.5, 36.3, 36.45, 36.4, 36.5, 37.4, 36.6, 36.65, 36.75, disturbedAt = setOf(6)),
        )
        assertTrue(skipped is ThermalShiftResult.Confirmed)

        val sparse = listOf(36.4, 36.5, 36.3, 36.45, 36.4, 36.5, 36.6, 36.65, 36.75)
            .mapIndexed { index, value -> BbtReading(start.plusDays(index * 4L), value) }
        assertEquals(ThermalShiftResult.None, ThermalShiftCalculator.evaluateCycle(start, sparse))
    }

    @Test
    fun `fewer than nine readings never confirm`() {
        assertEquals(
            ThermalShiftResult.None,
            ThermalShiftCalculator.evaluateCycle(start, readings(36.4, 36.5, 36.3, 36.45, 36.4, 36.5, 36.8, 36.9)),
        )
    }

    // Symptom patterns.

    private fun cycles(vararg starts: LocalDate): List<RecordedCycle> =
        CycleHistory.build(starts.associateWith { CycleRecordValues.FLOW_MEDIUM })

    @Test
    fun `symptoms are counted per phase and per cycle`() {
        val second = start.plusDays(28)
        val history = cycles(start, second)
        val days = listOf(
            SymptomDay(start, setOf(CycleSymptom.CRAMPS)),
            SymptomDay(start.plusDays(20), setOf(CycleSymptom.CRAMPS, CycleSymptom.BLOATING)),
            SymptomDay(second.plusDays(1), setOf(CycleSymptom.CRAMPS)),
        )
        val patterns = SymptomPatternCalculator.calculate(history, days, bleedingDays = setOf(start, second))

        val cramps = patterns.first { it.symptom == CycleSymptom.CRAMPS }
        assertEquals(3, cramps.totalOccurrences)
        assertEquals(2, cramps.cycleCount)
        assertEquals(2, cramps.phaseBreakdown[CyclePhase.MENSTRUAL])
        assertEquals(1, cramps.phaseBreakdown[CyclePhase.LUTEAL])
        assertEquals(CyclePhase.MENSTRUAL, cramps.mostFrequentPhase)
        assertEquals(listOf(CycleSymptom.CRAMPS, CycleSymptom.BLOATING), patterns.map { it.symptom })
    }

    @Test
    fun `days without symptoms produce no patterns`() {
        assertTrue(SymptomPatternCalculator.calculate(cycles(start), listOf(SymptomDay(start, emptySet())), emptySet()).isEmpty())
    }

    // Longitudinal stats.

    @Test
    fun `longitudinal stats mark the current cycle, exclusions and seven-day swings`() {
        val flow = listOf(start, start.plusDays(28), start.plusDays(56), start.plusDays(92), start.plusDays(120))
            .associateWith { CycleRecordValues.FLOW_LIGHT }
        val history = CycleHistory.build(flow, exclusions = mapOf(start.plusDays(30) to CycleExclusionReason.OTHER))
        val stats = LongitudinalCycleStatsCalculator.calculate(history, today = start.plusDays(125))

        assertEquals(5, stats.totalCyclesCount)
        assertEquals(4, stats.completedCyclesCount)
        assertEquals(1, stats.excludedCyclesCount)
        assertEquals(start.plusDays(120), stats.items.first().startDate)
        assertTrue(stats.items.first().isCurrent)
        assertEquals(6, stats.items.first().lengthDays)
        // Lengths 28, 28 (excluded), 36, 28: the swing is judged against the last included cycle.
        assertTrue(stats.items[1].hasStrawSwing)
        assertTrue(stats.items[2].hasStrawSwing)
        assertEquals(28.0, stats.medianDays!!, 1e-9)
        assertEquals((28 + 36 + 28) / 3.0, stats.meanDays!!, 1e-9)
    }

    @Test
    fun `no cycles gives the empty stats`() {
        assertEquals(LongitudinalCycleStats.Empty, LongitudinalCycleStatsCalculator.calculate(emptyList(), start))
    }

    // Catalog, tips and facts.

    @Test
    fun `the catalog adds symptoms only for observation contexts`() {
        val base = ObservationCatalog.symptomsFor(emptySet())
        assertEquals(10, base.size)
        assertTrue(CycleSymptom.BREAST_TENDERNESS !in base)

        val pms = ObservationCatalog.symptomsFor(setOf(TrackingContext.PMS, TrackingContext.PMDD))
        assertEquals(base + listOf(CycleSymptom.BREAST_TENDERNESS, CycleSymptom.MOOD_CHANGES, CycleSymptom.ANXIETY, CycleSymptom.ACNE), pms)

        assertEquals(base, ObservationCatalog.symptomsFor(setOf(TrackingContext.PCOS, TrackingContext.THYROID)))
        assertTrue(CycleSymptom.PELVIC_PAIN_OUTSIDE_PERIOD in ObservationCatalog.symptomsFor(setOf(TrackingContext.ENDOMETRIOSIS)))
    }

    @Test
    fun `tips need their context and prefer recent symptoms`() {
        val date = LocalDate.parse("2026-05-05")
        val plain = PhaseTips.forDate(CyclePhase.LUTEAL, date)
        assertNull(plain.targetContext)

        val pmdd = PhaseTips.forDate(CyclePhase.LUTEAL, date, declaredContexts = setOf(TrackingContext.PMDD))
        assertEquals(TrackingContext.PMDD, pmdd.targetContext)

        val paced = PhaseTips.forDate(
            CyclePhase.LUTEAL,
            date,
            declaredContexts = setOf(TrackingContext.PMDD),
            recentSymptoms = setOf(CycleSymptom.ANXIETY),
        )
        assertEquals("luteal_pmdd_pacing", paced.id)
        assertEquals(paced, PhaseTips.forDate(CyclePhase.LUTEAL, date, setOf(TrackingContext.PMDD), setOf(CycleSymptom.ANXIETY)))
    }

    @Test
    fun `every phase has a tip and the fact of the day is stable`() {
        CyclePhase.entries.forEach { phase -> PhaseTips.forDate(phase, start) }
        val date = LocalDate.parse("2026-05-05")
        assertEquals(CycleFacts.forDate(date), CycleFacts.forDate(date))
        assertEquals(24, CycleFacts.ALL.distinctBy { it.id }.size)
        assertEquals(30, PhaseTips.ALL.distinctBy { it.id }.size)
    }
}
