package tech.mmarca.openvitals.features.cycle

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.MenstruationFlowEntry
import tech.mmarca.openvitals.domain.model.MenstruationPeriodEntry
import tech.mmarca.openvitals.domain.model.OvulationTestEntry

class CyclePresentationMapperTest {

    private val anchorDate = LocalDate.of(2026, 4, 15)
    private val monthQuery = PeriodLoadQuery(
        range = TimeRange.MONTH,
        anchorDate = anchorDate,
        weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    )

    @Test fun `display has data when cycle records exist`() {
        val data = CycleData(
            menstruationFlows = listOf(
                MenstruationFlowEntry(
                    time = instant("2026-04-11T10:00:00Z"),
                    flow = FLOW_MEDIUM,
                    source = "flow",
                )
            ),
            ovulationTests = listOf(
                OvulationTestEntry(
                    time = instant("2026-04-12T10:00:00Z"),
                    result = 1,
                    source = "ovulation",
                )
            ),
        )

        val display = CyclePresentationMapper.build(
            query = monthQuery,
            data = data,
        )

        assertTrue(display.hasData)
        assertEquals(1, display.summary.ovulationTestCount)
        assertEquals(2, display.summary.totalEntryCount)
        assertEquals(2, display.sampleCount)
        assertTrue(display.calendarDays.isNotEmpty())
    }

    @Test fun `display has no data for empty cycle data`() {
        val display = CyclePresentationMapper.build(
            query = monthQuery,
            data = CycleData(),
        )

        assertFalse(display.hasData)
        assertEquals(0, display.summary.periodDays)
        assertEquals(0, display.summary.totalEntryCount)
        assertTrue(display.trackedDates.isEmpty())
    }

    @Test fun `summary counts period days with active menstruation`() {
        val data = CycleData(
            menstruationPeriods = listOf(
                MenstruationPeriodEntry(
                    startTime = instant("2026-04-10T00:00:00Z"),
                    endTime = instant("2026-04-13T00:00:00Z"),
                    source = "period",
                )
            ),
            basalBodyTemperature = listOf(
                BasalBodyTemperatureEntry(
                    time = instant("2026-04-11T08:00:00Z"),
                    temperatureCelsius = 36.4,
                    measurementLocation = 4,
                    source = "bbt",
                ),
                BasalBodyTemperatureEntry(
                    time = instant("2026-04-12T09:00:00Z"),
                    temperatureCelsius = 36.7,
                    measurementLocation = 10,
                    source = "bbt",
                ),
            ),
        )

        val display = CyclePresentationMapper.build(
            query = monthQuery,
            data = data,
        )

        assertTrue(display.summary.periodDays >= 3)
        assertEquals(2, display.summary.bbtReadingCount)
        assertEquals(36.7, display.summary.latestBbtCelsius ?: 0.0, 0.01)
        assertEquals(10, display.summary.latestBbtMeasurementLocation)
    }

    private fun instant(value: String): Instant = Instant.parse(value)
}
