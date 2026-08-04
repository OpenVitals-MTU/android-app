package tech.mmarca.openvitals.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The line a screen reader gets instead of the chart.
 *
 * A chart is a `Canvas` and publishes nothing on its own, so before this a
 * screen-reader user swiping down a metric screen went from the title straight
 * past the chart with no indication one existed. The summary is not a
 * description of the drawing — it is what a sighted user takes from a
 * two-second glance: what is plotted, over what span, and the headline number.
 *
 * Pinned because the ordering is the accessible part: these are read back to
 * back as a user moves down a screen, and a summary that reorders its clauses
 * per chart is harder to follow than one that is merely terse.
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
        // "Steps" alone tells a screen-reader user nothing about why there is no
        // chart — it reads as though the data failed to load.
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
        // Several call sites pass "" rather than null when they have no value.
        // Joining that would produce "Steps, " with a trailing pause.
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
