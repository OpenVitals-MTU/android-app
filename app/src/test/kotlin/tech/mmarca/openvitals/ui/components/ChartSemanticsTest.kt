package tech.mmarca.openvitals.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The line a screen reader gets instead of the chart: what is plotted, over what span,
 * and the headline number. The clause order is the accessible part.
 */
class ChartSemanticsTest {

    @Test
    fun `the title comes first, then the span, then the number`() {
        assertThat(
            chartSemanticSummary(
                title = "Steps",
                summaryText = "6,432 today",
                rangeLabel = "This week",
            ),
        ).isEqualTo("Steps, This week, 6,432 today")
    }

    @Test
    fun `a chart with no range still reads as a sentence`() {
        assertThat(chartSemanticSummary(title = "Steps", summaryText = "6,432 today"))
            .isEqualTo("Steps, 6,432 today")
    }

    @Test
    fun `an empty chart falls back to its empty label rather than announcing a bare title`() {
        // "Steps" alone reads as though the data failed to load.
        assertThat(
            chartSemanticSummary(
                title = "Steps",
                summaryText = null,
                emptyLabel = "No data for this period",
            ),
        ).isEqualTo("Steps, No data for this period")
    }

    @Test
    fun `a blank summary is treated as absent, not announced as an empty clause`() {
        // Several call sites pass "" rather than null; joining it would leave a trailing pause.
        assertThat(chartSemanticSummary(title = "Steps", summaryText = "   "))
            .isEqualTo("Steps")
        assertThat(chartSemanticSummary(title = "Steps", summaryText = "", rangeLabel = ""))
            .isEqualTo("Steps")
    }

    @Test
    fun `the empty label is only used when there is no summary`() {
        assertThat(
            chartSemanticSummary(
                title = "Steps",
                summaryText = "6,432 today",
                emptyLabel = "No data",
            ),
        ).isEqualTo("Steps, 6,432 today")
    }

    @Test
    fun `surrounding whitespace never reaches the reader`() {
        assertThat(chartSemanticSummary(title = "  Steps  ", summaryText = " 6,432 today "))
            .isEqualTo("Steps, 6,432 today")
    }
}
