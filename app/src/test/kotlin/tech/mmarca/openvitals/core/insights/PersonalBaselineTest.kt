package tech.mmarca.openvitals.core.insights

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
