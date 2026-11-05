package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.domain.model.BbtDisturbance
import tech.mmarca.openvitals.domain.model.CervicalMucusEntry
import tech.mmarca.openvitals.domain.model.CervicalSensation
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import tech.mmarca.openvitals.domain.model.OvulationTestEntry

/** Which recorded signs count as markers, and on which day. */
class OvulationMarkerDetectorTest {

    private val start = LocalDate.parse("2025-03-01")
    private val cycle = RecordedCycle(startDate = start, endDate = null)
    private val today = start.plusDays(27)

    @Test
    fun `the first positive LH test is the marker, high and negative results are not`() {
        val observations = CycleObservations.from(
            CycleData(
                ovulationTests = listOf(
                    lh(day = 11, CycleRecordValues.OVULATION_HIGH),
                    lh(day = 14, CycleRecordValues.OVULATION_POSITIVE),
                    lh(day = 15, CycleRecordValues.OVULATION_POSITIVE),
                    lh(day = 16, CycleRecordValues.OVULATION_NEGATIVE),
                ),
            ),
            journal = emptyList(),
            zone = ZoneOffset.UTC,
        )

        assertEquals(mapOf(OvulationMarker.LH_TEST to day(14)), detect(observations))
    }

    @Test
    fun `the temperature marker is the first high day of the shift and skips disturbed readings`() {
        val temps = listOf(36.4, 36.4, 36.5, 36.4, 36.4, 36.5, 37.3, 36.7, 36.7, 36.8)
        val data = CycleData(basalBodyTemperature = temps.mapIndexed { index, celsius -> bbt(day = 8 + index, celsius) })
        val disturbedSpike = listOf(CycleJournalEntry(date = day(14), bbtDisturbances = setOf(BbtDisturbance.FEVER)))

        val observations = CycleObservations.from(data, disturbedSpike, ZoneOffset.UTC)

        assertTrue(observations.bbtReadings.single { it.date == day(14) }.disturbed)
        // With the fever day skipped, the six lows run to day 13 and the highs start on day 15.
        assertEquals(mapOf(OvulationMarker.TEMPERATURE to day(15)), detect(observations))
    }

    @Test
    fun `the mucus peak needs three observed lesser days after the last best day`() {
        val egg = CycleRecordValues.MUCUS_APPEARANCE_EGG_WHITE
        val sticky = CycleRecordValues.MUCUS_APPEARANCE_STICKY
        val peak = CycleObservations.from(
            CycleData(cervicalMucus = listOf(mucus(12, egg), mucus(13, egg), mucus(14, sticky), mucus(15, sticky), mucus(16, sticky))),
            journal = emptyList(),
            zone = ZoneOffset.UTC,
        )
        val gap = peak.copy(mucusDays = peak.mucusDays - day(15))
        val unfinished = peak.copy(mucusDays = peak.mucusDays - day(16))

        assertEquals(mapOf(OvulationMarker.MUCUS to day(13)), detect(peak))
        assertTrue("a missing day leaves the peak unknown", detect(gap).isEmpty())
        assertTrue(detect(unfinished).isEmpty())
    }

    @Test
    fun `a wet or slippery sensation is best quality, watery mucus is not`() {
        val journal = listOf(CervicalSensation.SLIPPERY, CervicalSensation.DAMP, CervicalSensation.DRY, CervicalSensation.DRY)
            .mapIndexed { index, sensation -> CycleJournalEntry(date = day(12 + index), cervicalSensation = sensation) }
        val watery = CycleData(cervicalMucus = listOf(mucus(12, CycleRecordValues.MUCUS_APPEARANCE_WATERY)))

        val observations = CycleObservations.from(watery, journal, ZoneOffset.UTC)

        assertTrue(observations.mucusDays.getValue(day(12)))
        assertEquals(mapOf(OvulationMarker.MUCUS to day(12)), detect(observations))
        assertFalse(CycleObservations.from(watery, emptyList(), ZoneOffset.UTC).mucusDays.getValue(day(12)))
    }

    @Test
    fun `signs outside the cycle or after today are ignored`() {
        val closed = cycle.copy(endDate = day(20))
        val observations = CycleObservations(lhPositiveDays = setOf(start.minusDays(3), day(22)))

        assertTrue(OvulationMarkerDetector.detect(closed, observations, today).isEmpty())
        assertTrue(OvulationMarkerDetector.detect(cycle, observations, today = day(21)).isEmpty())
    }

    private fun detect(observations: CycleObservations) = OvulationMarkerDetector.detect(cycle, observations, today)

    /** Cycle day [day], one-based. */
    private fun day(day: Int): LocalDate = start.plusDays(day - 1L)

    private fun noon(day: Int) = day(day).atTime(7, 0).toInstant(ZoneOffset.UTC)

    private fun lh(day: Int, result: Int) = OvulationTestEntry(time = noon(day), result = result, source = "test")

    private fun bbt(day: Int, celsius: Double) =
        BasalBodyTemperatureEntry(time = noon(day), temperatureCelsius = celsius, measurementLocation = 0, source = "test")

    private fun mucus(day: Int, appearance: Int) =
        CervicalMucusEntry(time = noon(day), appearance = appearance, sensation = 0, source = "test")
}
