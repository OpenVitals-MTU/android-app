package tech.mmarca.openvitals.domain.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import java.time.LocalDate

class DataConfidenceTest {

    @Test fun `confidence calculates coverage inside the selected period`() {
        val period = DatePeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 7))

        val confidence = dataConfidence(
            period = period,
            trackedDates = listOf(
                LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 3),
                LocalDate.of(2026, 5, 3),
                LocalDate.of(2026, 5, 8),
            ),
            sampleCount = 5,
            sources = listOf("com.example.watch"),
        )

        assertEquals(7, confidence.expectedDays)
        assertEquals(2, confidence.trackedDays)
        assertEquals(29, confidence.coveragePercent)
    }

    @Test fun `mixed sources are reported as medium confidence`() {
        val confidence = dataConfidence(
            period = DatePeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 1)),
            trackedDates = listOf(LocalDate.of(2026, 5, 1)),
            sampleCount = 12,
            sources = listOf("com.example.watch", "com.example.scale"),
        )

        assertEquals(DataConfidenceLevel.MEDIUM, confidence.level)
        assertEquals(DataSourceConsistency.MIXED_SOURCES, confidence.sourceConsistency)
        assertTrue(confidence.warnings.contains(DataConfidenceWarning.MIXED_SOURCES))
    }

    @Test fun `calculated values include a calculated warning`() {
        val confidence = dataConfidence(
            period = DatePeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 1)),
            trackedDates = listOf(LocalDate.of(2026, 5, 1)),
            sampleCount = 1,
            sources = listOf("com.example.scale"),
            valueKind = DataValueKind.CALCULATED,
        )

        assertEquals(DataConfidenceLevel.LOW, confidence.level)
        assertTrue(confidence.warnings.contains(DataConfidenceWarning.CALCULATED_VALUE))
    }

    @Test fun `empty samples are low confidence`() {
        val confidence = dataConfidence(
            period = DatePeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
            trackedDates = emptyList(),
            sampleCount = 0,
        )

        assertEquals(DataConfidenceLevel.LOW, confidence.level)
        assertTrue(confidence.warnings.contains(DataConfidenceWarning.NO_SOURCE_DETAILS))
    }

    @Test fun `manual entries are reported`() {
        val confidence = dataConfidence(
            period = DatePeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3)),
            trackedDates = listOf(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 3),
            ),
            sampleCount = 3,
            sources = listOf("com.example.app"),
            manualEntryCount = 1,
        )

        assertEquals(DataConfidenceLevel.MEDIUM, confidence.level)
        assertTrue(confidence.warnings.contains(DataConfidenceWarning.MANUAL_ENTRIES))
    }
}
