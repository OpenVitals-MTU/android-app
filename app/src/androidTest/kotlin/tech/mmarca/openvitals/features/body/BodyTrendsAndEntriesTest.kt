package tech.mmarca.openvitals.features.body

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The trend-chart and entry-row cases of Flutter's
 * `test/features/body/body_screen_test.dart`.
 *
 * The body screen is a stack of metrics the user may track none, one or all of.
 * Two things follow from that and neither is visible from the view model: a
 * metric with no readings must not draw an empty chart that reads as a flat
 * line, and a reading this app did not write must not offer to delete it —
 * Health Connect refuses the delete, so the row would swipe away and come back.
 */
class BodyTrendsAndEntriesTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theAggregateChartsOnlyTheMetricsThatWereActuallyTracked() {
        // Weight was tracked; body fat was not. Every metric is named in the
        // statistics grid whether it has data or not, so "is body fat on the
        // screen" proves nothing — the question is whether a chart is drawn for
        // it. The trends section comes first and the statistics grid directly
        // after it, so the charts are exactly what lies between the two
        // headings.
        setBody(state(weights = listOf(weight())))

        val trends = string(R.string.section_body_trends)
        composeRule.onNodeWithText(trends).assertIsDisplayed()

        val nodes = snapshot()
        val trendsTop = nodes.topOf(trends)
        val statisticsTop = nodes.topOf(string(R.string.section_statistics))
        assertTrue("the trends heading should come before the statistics grid", trendsTop < statisticsTop)

        assertTrue(
            "the tracked metric should be charted under the trends heading",
            nodes.topsOf(string(R.string.metric_weight)).any { it in trendsTop..statisticsTop },
        )
        assertTrue(
            "an untracked metric should draw no chart at all",
            nodes.topsOf(string(R.string.metric_body_fat)).none { it in trendsTop..statisticsTop },
        )
    }

    @Test
    fun swipingAnOpenVitalsReadingAwayDeletesItThroughTheRepository() {
        val deleted = mutableListOf<Pair<BodyMeasurementType, String>>()
        setBody(state(weights = listOf(weight())), onDelete = { type, id -> deleted += type to id })

        swipeAwayTheWeightRow()

        assertEquals(listOf(BodyMeasurementType.WEIGHT to WEIGHT_ID), deleted)
    }

    @Test
    fun aReadingThisAppDidNotWriteIsNotSwipeDeletable() {
        // Health Connect only lets an app delete its own records, so offering
        // the gesture at all would promise a delete that silently fails.
        val deleted = mutableListOf<Pair<BodyMeasurementType, String>>()
        setBody(
            state(weights = listOf(weight(isOpenVitalsEntry = false, source = "com.other.scale"))),
            onDelete = { type, id -> deleted += type to id },
        )

        swipeAwayTheWeightRow()

        assertEquals(emptyList<Pair<BodyMeasurementType, String>>(), deleted)
        composeRule.onNodeWithText(weightRowText()).assertIsDisplayed()
    }

    private fun swipeAwayTheWeightRow() {
        val row = weightRowText()
        scrollTo(row)
        composeRule.onNodeWithText(row).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
    }

    /** The reading row's own label: the metric name and the value, as rendered. */
    private fun weightRowText(): String =
        "${string(R.string.metric_weight)} · ${FORMATTER.weight(WEIGHT_KG).text}"

    private fun state(
        isLoading: Boolean = false,
        weights: List<WeightEntry> = emptyList(),
    ) = BodyUiState(
        isLoading = isLoading,
        selectedRange = TimeRange.MONTH,
        selectedDate = ANCHOR,
        weightEntries = weights,
    )

    private fun weight(
        isOpenVitalsEntry: Boolean = true,
        source: String = "tech.mmarca.openvitals",
    ) = WeightEntry(
        id = WEIGHT_ID,
        time = ANCHOR.atStartOfDay(ZoneId.systemDefault()).plusHours(7).toInstant(),
        weightKg = WEIGHT_KG,
        source = source,
        isOpenVitalsEntry = isOpenVitalsEntry,
    )

    private fun setBody(
        state: BodyUiState,
        onDelete: (BodyMeasurementType, String) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                val sectionContext = MetricDetailSectionContext(
                    listState = rememberMetricDetailSectionListState(),
                    order = DefaultMetricDetailSectionOrder,
                    isEditingSections = false,
                    onMoveSectionToTarget = { _, _ -> },
                    onMoveSection = { _, _ -> },
                )
                LazyColumn {
                    bodyContent(
                        state = state,
                        period = DatePeriod(ANCHOR.withDayOfMonth(1), ANCHOR),
                        unitFormatter = FORMATTER,
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        chartDaySelection = ChartDaySelection(
                            selectedDate = null,
                            onDateSelected = {},
                        ),
                        sectionContext = sectionContext,
                        onEditBodyMeasurement = { _, _ -> },
                        onDeleteBodyMeasurement = onDelete,
                    )
                }
            }
        }
    }

    private fun scrollTo(text: String) {
        // The screen's own list; the swipeable rows contribute no scroll-to-index.
        composeRule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasText(text))
    }

    /**
     * One reading of the merged semantics tree, so that every position compared
     * below comes from the same frame. Positions are unclipped, because a node
     * below the fold is still laid out.
     */
    private fun snapshot(): List<Rendered> {
        val collected = mutableListOf<Rendered>()
        fun walk(node: SemanticsNode) {
            collected += Rendered(
                texts = node.config.getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty(),
                top = node.positionInRoot.y,
            )
            node.children.forEach(::walk)
        }
        walk(composeRule.onRoot().fetchSemanticsNode())
        return collected
    }

    private data class Rendered(val texts: List<String>, val top: Float)

    private fun List<Rendered>.topsOf(text: String): List<Float> =
        filter { text in it.texts }.map { it.top }

    private fun List<Rendered>.topOf(text: String): Float =
        topsOf(text).firstOrNull() ?: error("nothing rendering '$text'")

    private companion object {
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        const val WEIGHT_ID = "weight-1"
        const val WEIGHT_KG = 72.5
    }
}
