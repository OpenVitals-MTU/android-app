package tech.mmarca.openvitals.features.heart

import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.preferences.UnitSystem
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry

class HeartVitalsSummariesTest {

    private val formatter = UnitFormatter(
        unitSystemProvider = { UnitSystem.METRIC },
        localeProvider = { Locale.US },
    )

    @Test fun `respiratoryRateSummaryMetric returns null for empty entries`() {
        assertNull(
            respiratoryRateSummaryMetric(
                entries = emptyList(),
                selectedRange = TimeRange.WEEK,
                period = DatePeriod(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 26)),
                unitFormatter = formatter,
            )
        )
    }

    @Test fun `day respiratory summary uses latest reading`() {
        val period = DatePeriod(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 20))

        val metric = respiratoryRateSummaryMetric(
            entries = listOf(
                reading("2026-04-20T08:00:00Z", 13.0, "early"),
                reading("2026-04-20T18:00:00Z", 15.6, "late"),
            ),
            selectedRange = TimeRange.DAY,
            period = period,
            unitFormatter = formatter,
        )

        assertEquals("Respiratory rate", metric?.title)
        assertEquals("15.6", metric?.value)
        assertEquals("br/min", metric?.unit)
        assertEquals("late", metric?.source)
    }

    @Test fun `week respiratory summary averages daily buckets`() {
        val period = DatePeriod(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 26))

        val metric = respiratoryRateSummaryMetric(
            entries = listOf(
                reading("2026-04-20T12:00:00Z", 12.0),
                reading("2026-04-20T18:00:00Z", 16.0),
                reading("2026-04-21T12:00:00Z", 18.0, source = "other"),
            ),
            selectedRange = TimeRange.WEEK,
            period = period,
            unitFormatter = formatter,
        )

        assertEquals("Avg respiratory rate", metric?.title)
        assertEquals("16.0", metric?.value)
        assertEquals("br/min", metric?.unit)
        assertEquals("3 readings", metric?.source)
    }

    @Test fun `respiratoryRateDaySummaries groups readings by date`() {
        val summaries = respiratoryRateDaySummaries(
            listOf(
                reading("2026-04-20T12:00:00Z", 12.0),
                reading("2026-04-20T18:00:00Z", 18.0),
                reading("2026-04-21T12:00:00Z", 21.0),
            )
        ).sortedBy { it.date }

        assertEquals(2, summaries.size)
        assertEquals(LocalDate.of(2026, 4, 20), summaries[0].date)
        assertEquals(15.0, summaries[0].average, 0.0)
        assertEquals(12.0, summaries[0].min, 0.0)
        assertEquals(18.0, summaries[0].max, 0.0)
        assertEquals(2, summaries[0].readings)
    }

    private fun reading(
        time: String,
        breathsPerMinute: Double,
        source: String = "source",
    ) = RespiratoryRateEntry(
        time = Instant.parse(time),
        breathsPerMinute = breathsPerMinute,
        source = source,
    )
}
