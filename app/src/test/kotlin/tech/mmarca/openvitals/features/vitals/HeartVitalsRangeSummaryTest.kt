package tech.mmarca.openvitals.features.vitals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import java.time.LocalDate

class HeartVitalsRangeSummaryTest {

    private val day = LocalDate.of(2026, 6, 10)

    @Test
    fun `resting heart rate summary is null for no days, not an invented 40 to 80 range`() {
        assertNull(restingHeartRateRangeSummary(emptyList()))
    }

    @Test
    fun `resting heart rate summary reports the real average and range`() {
        val summary = restingHeartRateRangeSummary(
            listOf(
                DailyRestingHR(day, bpm = 52L),
                DailyRestingHR(day.minusDays(1), bpm = 58L),
            )
        )!!

        assertEquals(55L, summary.average)
        assertEquals(52L, summary.min)
        assertEquals(58L, summary.max)
    }

    @Test
    fun `hrv summary is null for no days, not an invented 0 to 100 range`() {
        assertNull(hrvRangeSummary(emptyList()))
    }

    @Test
    fun `hrv summary reports the real average and range`() {
        val summary = hrvRangeSummary(
            listOf(
                DailyHrv(day, rmssdMs = 40.0),
                DailyHrv(day.minusDays(1), rmssdMs = 60.0),
            )
        )!!

        assertEquals(50.0, summary.average, 0.01)
        assertEquals(40.0, summary.min, 0.01)
        assertEquals(60.0, summary.max, 0.01)
    }

    @Test
    fun `a single day is its own average, min and max`() {
        val summary = restingHeartRateRangeSummary(listOf(DailyRestingHR(day, bpm = 61L)))!!

        assertEquals(61L, summary.average)
        assertEquals(61L, summary.min)
        assertEquals(61L, summary.max)
    }
}
