package tech.mmarca.openvitals.domain.model

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VitalsDailySynthesisTest {

    private val date = LocalDate.of(2026, 6, 10)

    @Test fun `weighted mean weighs each day by its reading count`() {
        val points = listOf(
            DailyVitalPoint(date = date, value = 90.0, count = 1),
            DailyVitalPoint(date = date.plusDays(1), value = 100.0, count = 3),
        )

        assertEquals(97.5, points.weightedMeanOrNull()!!, 0.0001)
        assertEquals(4, points.totalReadings())
    }

    @Test fun `weighted mean is null with no readings`() {
        assertNull(emptyList<DailyVitalPoint>().weightedMeanOrNull())
    }

    @Test fun `synthesised entries land at local midnight with no source`() {
        val entries = listOf(DailyVitalPoint(date = date, value = 97.5, count = 12)).toSpO2Entries()

        val entry = entries.single()
        assertEquals(date.atStartOfDay(ZoneId.systemDefault()).toInstant(), entry.time)
        assertEquals(97.5, entry.percent, 0.0001)
        assertEquals("", entry.source)
    }

    @Test fun `blood pressure entries round the day averages`() {
        val entries = listOf(
            DailyBloodPressurePoint(date = date, systolic = 120.4, diastolic = 79.6, count = 2),
        ).toBloodPressureEntries()

        val entry = entries.single()
        assertEquals(120, entry.systolicMmHg)
        assertEquals(80, entry.diastolicMmHg)
    }

    @Test fun `skin temperature entries carry the day average delta`() {
        val entries = listOf(DailyVitalPoint(date = date, value = 0.35, count = 7)).toSkinTemperatureEntries()

        val entry = entries.single()
        assertEquals(0.35, entry.averageDeltaCelsius!!, 0.0001)
        assertNull(entry.baselineCelsius)
        assertEquals(entry.startTime, entry.endTime)
    }
}
