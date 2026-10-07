package tech.mmarca.openvitals.domain.insights

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PersonalBaselineTest {

    private val referenceDate: LocalDate = LocalDate.of(2026, 5, 23)

    @Test
    fun calculatesTrailingWindowAverages() {
        val values = (0 until 90).map { offset ->
            BaselineValue(referenceDate.minusDays(offset.toLong()), 10.0)
        } + BaselineValue(referenceDate.minusDays(91), 1_000.0)

        val insight = personalBaselineInsight(
            currentValue = 12.0,
            values = values,
            referenceDate = referenceDate,
        )

        assertNotNull(insight)
        val summaries = checkNotNull(insight).summaries.associateBy { it.windowDays }
        assertEquals(10.0, checkNotNull(summaries[30]).average, 0.0001)
        assertEquals(10.0, checkNotNull(summaries[60]).average, 0.0001)
        assertEquals(10.0, checkNotNull(summaries[90]).average, 0.0001)
        assertEquals(90, insight.primarySummary.sampleCount)
    }

    @Test
    fun marksValuesInsideStandardDeviationAsUsual() {
        val values = listOf(8.0, 10.0, 12.0)
            .mapIndexed { index, value ->
                BaselineValue(referenceDate.minusDays(index.toLong()), value)
            }

        val insight = personalBaselineInsight(
            currentValue = 11.0,
            values = values,
            referenceDate = referenceDate,
            windows = listOf(30),
        )

        assertEquals(BaselineStatus.USUAL, checkNotNull(insight).status)
    }

    @Test
    fun marksValuesOutsideUsualRangeButBelowAnomalyThreshold() {
        val values = listOf(8.0, 10.0, 12.0)
            .mapIndexed { index, value ->
                BaselineValue(referenceDate.minusDays(index.toLong()), value)
            }

        val insight = personalBaselineInsight(
            currentValue = 13.0,
            values = values,
            referenceDate = referenceDate,
            windows = listOf(30),
        )

        assertEquals(BaselineStatus.ABOVE, checkNotNull(insight).status)
    }

    @Test
    fun marksTwoStandardDeviationsAsAnomaly() {
        val values = listOf(8.0, 10.0, 12.0)
            .mapIndexed { index, value ->
                BaselineValue(referenceDate.minusDays(index.toLong()), value)
            }

        val insight = personalBaselineInsight(
            currentValue = 14.0,
            values = values,
            referenceDate = referenceDate,
            windows = listOf(30),
        )

        assertEquals(BaselineStatus.UNUSUAL_HIGH, checkNotNull(insight).status)
    }

    @Test
    fun dropsZeroAndNegativeDaysByDefault() {
        // A zero day on a count metric is a day nothing was logged.
        val values = listOf(-0.2, 0.0, 0.1, 0.3, 0.5)
            .mapIndexed { index, value ->
                BaselineValue(referenceDate.minusDays(index.toLong()), value)
            }

        val insight = checkNotNull(
            personalBaselineInsight(
                currentValue = 0.3,
                values = values,
                referenceDate = referenceDate,
                windows = listOf(30),
            ),
        )

        assertEquals(3, insight.primarySummary.sampleCount)
        assertEquals(0.3, insight.primarySummary.average, 0.0001)
    }

    @Test
    fun keepsZeroAndNegativeDaysForASignedMetric() {
        // A skin temperature variation below its baseline is a reading, not a gap.
        val values = listOf(-0.2, 0.0, 0.1, 0.3, 0.5)
            .mapIndexed { index, value ->
                BaselineValue(referenceDate.minusDays(index.toLong()), value)
            }

        val insight = checkNotNull(
            personalBaselineInsight(
                currentValue = -0.4,
                values = values,
                referenceDate = referenceDate,
                windows = listOf(30),
                includeNonPositive = true,
            ),
        )

        assertEquals(5, insight.primarySummary.sampleCount)
        assertEquals(0.14, insight.primarySummary.average, 0.0001)
        assertEquals(-0.54, insight.deviation, 0.0001)
        assertEquals(BaselineStatus.UNUSUAL_LOW, insight.status)
    }

    @Test
    fun returnsNullWhenThereAreNotEnoughSamples() {
        val values = listOf(
            BaselineValue(referenceDate, 10.0),
            BaselineValue(referenceDate.minusDays(1), 12.0),
        )

        val insight = personalBaselineInsight(
            currentValue = 11.0,
            values = values,
            referenceDate = referenceDate,
            windows = listOf(30),
        )

        assertNull(insight)
    }
}
