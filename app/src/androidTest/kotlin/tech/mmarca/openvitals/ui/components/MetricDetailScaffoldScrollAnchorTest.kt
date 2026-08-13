package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Deleting an entry reloads the period, and a reload raises the "syncing"
 * banner — an item that appears at the TOP of the list, above everything the
 * reader has scrolled past, and vanishes again when the load lands.
 *
 * That it costs nothing is not obvious, and it is not free: it holds because
 * [orderedMetricDetailSections] gives every section `item(key = sectionId)`, so
 * the list anchors its scroll position to the section the reader is inside
 * rather than to its index. Drop those keys and the page would lurch down and
 * back on every delete, on every metric screen — a regression with no failing
 * assertion anywhere near the code that caused it.
 *
 * So these tests pin the property rather than fix a fault. The item count is
 * asserted alongside the position for a reason: the banner sits off-screen and
 * is never composed, so a scaffold that had quietly stopped inserting anything
 * would hold the position assertions perfectly while testing nothing.
 */
class MetricDetailScaffoldScrollAnchorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theSyncBannerAppearing_doesNotMoveWhatTheReaderIsLookingAt() {
        var isLoading by mutableStateOf(false)
        val listState = MetricDetailSectionListState(
            androidx.compose.foundation.lazy.LazyListState(),
        )

        composeRule.setContent {
            OpenVitalsTheme {
                MetricDetailScaffold(
                    isLoading = isLoading,
                    selectedRange = TimeRange.YEAR,
                    selectedDate = LocalDate.of(2026, 5, 10),
                    onRefresh = {},
                    onSelectRange = {},
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onSelectDate = {},
                    sectionListState = listState,
                ) {
                    orderedMetricDetailSections(
                        listState = listState,
                        order = ORDER,
                        isEditingSections = false,
                        onMoveSectionToTarget = { _, _ -> },
                        onMoveSection = { _, _ -> },
                    ) {
                        section(MetricDetailSectionId.PERIOD_CHART) {
                            Column(Modifier.fillMaxWidth()) {
                                Text("Chart", Modifier.height(SECTION_HEIGHT))
                            }
                        }
                        // The long entries list the reader scrolled into.
                        section(MetricDetailSectionId.ENTRIES) {
                            Column(Modifier.fillMaxWidth()) {
                                repeat(ROWS) { row ->
                                    Text("Reading $row", Modifier.height(ROW_HEIGHT))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Scroll deep into the entries, the way the reported flow does.
        composeRule.awaitScrollTo(listState, index = 2, offset = SCROLL_OFFSET_PX)

        val before = composeRule.onNodeWithText(ANCHOR_ROW).fetchSemanticsNode().boundsInRoot.top
        val itemsBefore = composeRule.itemCount(listState)

        // The delete's reload raises the banner.
        isLoading = true
        composeRule.waitForIdle()

        // The banner sits above the viewport, so it is never composed and
        // cannot be found by text — but it IS in the list. Without this the
        // assertion below would hold for a scaffold that inserted nothing.
        assertEquals(
            "the sync banner was not added to the list, so nothing was tested",
            itemsBefore + 1,
            composeRule.itemCount(listState),
        )
        val after = composeRule.onNodeWithText(ANCHOR_ROW).fetchSemanticsNode().boundsInRoot.top

        assertTrue(
            "the row the reader was looking at moved by ${abs(after - before)}px when the " +
                "sync banner appeared; it must stay put",
            abs(after - before) < TOLERANCE_PX,
        )
    }

    @Test
    fun theSyncBannerLeaving_doesNotMoveItBack() {
        var isLoading by mutableStateOf(true)
        val listState = MetricDetailSectionListState(
            androidx.compose.foundation.lazy.LazyListState(),
        )

        composeRule.setContent {
            OpenVitalsTheme {
                MetricDetailScaffold(
                    isLoading = isLoading,
                    selectedRange = TimeRange.YEAR,
                    selectedDate = LocalDate.of(2026, 5, 10),
                    onRefresh = {},
                    onSelectRange = {},
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onSelectDate = {},
                    sectionListState = listState,
                ) {
                    orderedMetricDetailSections(
                        listState = listState,
                        order = ORDER,
                        isEditingSections = false,
                        onMoveSectionToTarget = { _, _ -> },
                        onMoveSection = { _, _ -> },
                    ) {
                        section(MetricDetailSectionId.PERIOD_CHART) {
                            Column(Modifier.fillMaxWidth()) {
                                Text("Chart", Modifier.height(SECTION_HEIGHT))
                            }
                        }
                        section(MetricDetailSectionId.ENTRIES) {
                            Column(Modifier.fillMaxWidth()) {
                                repeat(ROWS) { row ->
                                    Text("Reading $row", Modifier.height(ROW_HEIGHT))
                                }
                            }
                        }
                    }
                }
            }
        }

        composeRule.awaitScrollTo(listState, index = 2, offset = SCROLL_OFFSET_PX)

        val before = composeRule.onNodeWithText(ANCHOR_ROW).fetchSemanticsNode().boundsInRoot.top
        val itemsBefore = composeRule.itemCount(listState)

        isLoading = false
        composeRule.waitForIdle()

        assertEquals(
            "the sync banner was not removed from the list, so nothing was tested",
            itemsBefore - 1,
            composeRule.itemCount(listState),
        )
        val after = composeRule.onNodeWithText(ANCHOR_ROW).fetchSemanticsNode().boundsInRoot.top

        assertTrue(
            "the row moved by ${abs(after - before)}px when the sync banner left",
            abs(after - before) < TOLERANCE_PX,
        )
    }

    private companion object {
        val ORDER = listOf(MetricDetailSectionId.PERIOD_CHART, MetricDetailSectionId.ENTRIES)
        val SECTION_HEIGHT = 400.dp
        val ROW_HEIGHT = 48.dp
        const val ROWS = 40
        const val SCROLL_OFFSET_PX = 900
        const val ANCHOR_ROW = "Reading 25"

        /** One row's worth of slack; anything larger is a visible jump. */
        const val TOLERANCE_PX = 8f
    }
}

/** How many items the list holds, banner included, composed or not. */
private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.itemCount(
    listState: MetricDetailSectionListState,
): Int = runOnIdle { listState.lazyListState.layoutInfo.totalItemsCount }

/** Scrolls [listState] and waits for the list to settle there. */
private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.awaitScrollTo(
    listState: MetricDetailSectionListState,
    index: Int,
    offset: Int,
) {
    runOnIdle {
        kotlinx.coroutines.runBlocking {
            listState.lazyListState.scrollToItem(index, offset)
        }
    }
    waitForIdle()
}
