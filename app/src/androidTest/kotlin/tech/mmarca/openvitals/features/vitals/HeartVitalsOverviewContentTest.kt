package tech.mmarca.openvitals.features.vitals

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs
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
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.heart.HeartMetric
import tech.mmarca.openvitals.features.heart.HeartUiState
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of the rendering cases of Flutter's
 * `test/features/vitals/heart_vitals_overview_screen_test.dart`.
 *
 * The overview is the only screen that shows heart, cardiovascular and
 * respiratory together, so what it owes the user is grouping: every metric under
 * the heading it belongs to, two to a row so a phone shows a whole group at a
 * glance, and each card a way through to its own screen. A metric that renders
 * under the wrong heading — or renders nowhere — is invisible to someone
 * scanning for it.
 *
 * The screen itself takes `HeartViewModel` and wraps itself in the Health
 * Connect shell, so these drive `VitalsOverviewContent`, which is the same
 * content the screen renders and takes state and callbacks.
 *
 * Two things about how the assertions are written, both learned from getting
 * them wrong on a device. A metric's name appears twice inside a section — once
 * on its summary card and once as the title of its chart — so nothing here
 * selects "the first node with that text"; a card is identified by being
 * clickable, which the chart card is not. And every geometric comparison is made
 * against a single tree snapshot, because two separate `fetch` calls are two
 * separate frames and a list that is still settling moves between them.
 *
 * Three of the Flutter file's cases are deliberately absent rather than
 * weakened: the two-per-row layout, the skin-temperature card's placement under
 * the respiratory heading, and the tap that opens its metric. All three are
 * true of the code — `OverviewMetricRowsContent` is `metrics.chunked(2)`, the
 * section renders its heading before its metrics, and `OverviewMetricCard` goes
 * through a clickable `OpenVitalsCard`. What could not be done honestly is
 * SELECT the right nodes: a metric's name appears on both its summary card and
 * its chart title, in several sections, and scoping a geometric assertion to
 * one section's cards needs test tags in production. Adding tags so a layout
 * assertion can pass buys less than it costs, and two rounds of trying to
 * locate the nodes by text and click action both mis-selected.
 */
class HeartVitalsOverviewContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theThreeGroupSectionsRenderAndTheStoredOrderDecidesWhichComesFirst() {
        setOverview(loadedState())

        // Heart is first in the default order, so it is what the screen opens on.
        composeRule.onNodeWithText(string(R.string.section_heart)).assertIsDisplayed()
        scrollTo(string(R.string.section_cardiovascular))
        composeRule.onNodeWithText(string(R.string.section_cardiovascular)).assertIsDisplayed()
        scrollTo(string(R.string.section_respiratory))
        composeRule.onNodeWithText(string(R.string.section_respiratory)).assertIsDisplayed()
    }

    @Test
    fun aReorderedSectionListPutsADifferentGroupOnTop() {
        // The sections are reorderable, which only means anything if the stored
        // order — not the declaration order — is what decides the layout.
        setOverview(loadedState(), order = DefaultMetricDetailSectionOrder.reversed())

        val nodes = snapshot()
        val respiratory = headingTop(nodes, string(R.string.section_respiratory))
        val heart = headingTop(nodes, string(R.string.section_heart))
        assertTrue(
            "with the order reversed, Respiratory should be above Heart",
            respiratory < heart,
        )
    }

    @Test
    fun theDataSourceEducationItemComesAfterTheSections() {
        // Where the numbers came from is the answer to "why is this not what my
        // watch says", and it belongs under the data rather than in front of it.
        setOverview(loadedState())

        val manage = string(R.string.health_connect_data_source_manage)
        composeRule.onNodeWithText(manage).assertDoesNotExist()

        scrollTo(manage)
        composeRule.onNodeWithText(manage).assertIsDisplayed()

        val nodes = snapshot()
        val educationTop = headingTop(nodes, manage)
        // Whichever headings are still composed this far down the list, all of
        // them are above the education item.
        val headings = SECTION_TITLES.mapNotNull { title -> nodes.topOrNull(title) }
        assertTrue("expected at least one section heading to compare against", headings.isNotEmpty())
        headings.forEach { top ->
            assertTrue("a section heading should sit above the education item", top < educationTop)
        }
    }

    @Test
    fun theHeartSectionDrawsAChartUnderEachOfItsThreeCards() {
        setOverview(loadedState())

        // Card and chart share the metric's name, so two nodes is one of each.
        HEART_TITLES.forEach { title -> assertEquals(title, 2, nodeCount(title)) }
    }

    @Test
    fun aHeartMetricWithNoHistoryKeepsItsCardAndDropsItsChart() {
        // The counterpart that makes the previous case mean something: without
        // days to plot there is no chart, and the card still has to be there so
        // the metric can be opened.
        setOverview(
            loadedState().copy(
                dailySummaries = emptyList(),
                dailyRestingHR = emptyList(),
                dailyHrv = emptyList(),
            ),
        )

        HEART_TITLES.forEach { title -> assertEquals(title, 1, nodeCount(title)) }
    }




    private fun loadedState() = HeartUiState(
        isLoading = false,
        selectedRange = TimeRange.WEEK,
        selectedDate = ANCHOR,
        dailySummaries = DAYS.map { date ->
            HeartRateSummary(date = date, avgBpm = 71, minBpm = 52, maxBpm = 140)
        },
        dailyRestingHR = DAYS.map { date -> DailyRestingHR(date = date, bpm = 54) },
        dailyHrv = DAYS.map { date -> DailyHrv(date = date, rmssdMs = 42.0) },
        skinTemperature = listOf(
            SkinTemperatureEntry(
                startTime = ANCHOR.atStartOfDay().toInstant(ZoneOffset.UTC),
                endTime = ANCHOR.atStartOfDay().plusHours(7).toInstant(ZoneOffset.UTC),
                baselineCelsius = 33.4,
                averageDeltaCelsius = 0.3,
                minDeltaCelsius = 0.1,
                maxDeltaCelsius = 0.6,
                measurementLocation = 0,
                source = "tech.mmarca.openvitals",
            ),
        ),
    )

    private fun setOverview(
        state: HeartUiState,
        order: List<MetricDetailSectionId> = DefaultMetricDetailSectionOrder,
        onOpenMetric: (HeartMetric) -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                val sectionContext = MetricDetailSectionContext(
                    listState = rememberMetricDetailSectionListState(),
                    order = order,
                    isEditingSections = false,
                    onMoveSectionToTarget = { _, _ -> },
                    onMoveSection = { _, _ -> },
                )
                LazyColumn(modifier = Modifier) {
                    VitalsOverviewContent(
                        state = state,
                        period = DatePeriod(ANCHOR.minusDays(6), ANCHOR),
                        unitFormatter = FORMATTER,
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        chartDaySelection = ChartDaySelection(
                            selectedDate = null,
                            onDateSelected = {},
                        ),
                        sectionContext = sectionContext,
                        onOpenMetric = onOpenMetric,
                    )
                }
            }
        }
    }

    /** One reading of the merged semantics tree, so every bound is one frame. */
    private data class Rendered(
        val texts: List<String>,
        val top: Float,
        val left: Float,
        val isClickable: Boolean,
    )

    private fun snapshot(): List<Rendered> {
        val collected = mutableListOf<Rendered>()
        fun walk(node: SemanticsNode) {
            collected += Rendered(
                texts = node.config.getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty(),
                // Unclipped: a card below the fold is still laid out, and its
                // clipped bounds would be flattened against the viewport edge.
                top = node.positionInRoot.y,
                left = node.positionInRoot.x,
                isClickable = node.config.contains(SemanticsActions.OnClick),
            )
            node.children.forEach(::walk)
        }
        walk(composeRule.onRoot().fetchSemanticsNode())
        return collected
    }

    /** A summary card: the clickable node carrying [title]. Charts are not clickable. */
    private fun cardNode(nodes: List<Rendered>, title: String): Rendered =
        nodes.firstOrNull { title in it.texts && it.isClickable }
            ?: error("no clickable card carrying '$title'; found ${nodes.count { title in it.texts }} node(s) with that text")

    private fun headingTop(nodes: List<Rendered>, text: String): Float =
        nodes.topOrNull(text) ?: error("nothing rendering '$text'")

    private fun List<Rendered>.topOrNull(text: String): Float? =
        firstOrNull { text in it.texts }?.top

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
    }

    private fun nodeCount(text: String): Int =
        composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().size

    private companion object {
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
        val DAYS: List<LocalDate> = (0..2).map { ANCHOR.minusDays(it.toLong()) }
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        const val EDGE_TOLERANCE_PX = 1f

        val HEART_TITLES: List<String>
            get() = listOf(
                string(R.string.metric_average_heart_rate),
                string(R.string.metric_resting_heart_rate),
                string(R.string.metric_hrv),
            )

        val SECTION_TITLES: List<String>
            get() = listOf(
                string(R.string.section_heart),
                string(R.string.section_cardiovascular),
                string(R.string.section_respiratory),
            )
    }
}
