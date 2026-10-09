package tech.mmarca.openvitals.wear.features.quicklog

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.wear.MetricUiState
import tech.mmarca.openvitals.wear.WearMetric
import tech.mmarca.openvitals.wear.WearUiState

class EntryLogTest {

    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 6)
    private fun at(day: LocalDate, hour: Int) = day.atTime(hour, 0).toInstant(zone).toEpochMilli()

    @Test
    fun encodeThenDecode_keepsEveryField() {
        val entries = listOf(
            LoggedEntry(EntryType.WATER, 1L, 250.0),
            LoggedEntry(EntryType.BLOOD_PRESSURE, 2L, 121.0, 79.0),
        )
        assertEquals(entries, decodeEntries(encodeEntries(entries)))
    }

    @Test
    fun decode_dropsBrokenAndUnknownLines() {
        val raw = "WATER;1;250.0\nGHOST;2;1.0\nWEIGHT;x;70.0\nWEIGHT;3\nWEIGHT;4;72.5"
        assertEquals(
            listOf(LoggedEntry(EntryType.WATER, 1L, 250.0), LoggedEntry(EntryType.WEIGHT, 4L, 72.5)),
            decodeEntries(raw),
        )
        assertEquals(emptyList<LoggedEntry>(), decodeEntries(null))
    }

    @Test
    fun water_sumsTodayIntoHydration_withHourlyBarsAndAWeek() {
        val entries = listOf(
            LoggedEntry(EntryType.WATER, at(today.minusDays(1), 9), 500.0),
            LoggedEntry(EntryType.WATER, at(today, 8), 250.0),
            LoggedEntry(EntryType.WATER, at(today, 10), 250.0),
            LoggedEntry(EntryType.WATER, at(today, 10), 250.0),
        )
        val hydration = WearUiState().withEntries(entries, today, zone).metrics.getValue(WearMetric.HYDRATION)

        assertEquals(750.0, hydration.current!!, 0.0)
        assertEquals(2_000.0, hydration.goal!!, 0.0)
        assertEquals(11, hydration.today.size)
        assertEquals(500.0, hydration.today[10], 0.0)
        assertEquals(listOf(0.0, 0.0, 0.0, 0.0, 0.0, 500.0, 750.0), hydration.week)
    }

    @Test
    fun spotMeasurement_fillsAnEmptyMetric_butNeverOverridesAReading() {
        val measured = listOf(LoggedEntry(EntryType.HEART_RATE, at(today, 9), 64.0))

        val empty = WearUiState().withEntries(measured, today, zone)
        assertEquals(64.0, empty.metrics.getValue(WearMetric.HEART_RATE).current!!, 0.0)

        val withReading = WearUiState(metrics = mapOf(WearMetric.HEART_RATE to MetricUiState(current = 72.0)))
            .withEntries(measured, today, zone)
        assertEquals(72.0, withReading.metrics.getValue(WearMetric.HEART_RATE).current!!, 0.0)
    }

    @Test
    fun noEntries_leaveMetricsAlone() {
        val state = WearUiState().withEntries(emptyList(), today, zone)
        assertNull(state.metrics[WearMetric.HYDRATION])
    }
}
