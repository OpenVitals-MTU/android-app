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
 * A delete's reload raises the "syncing" banner at the top of the list. The scroll position
 * holds because [orderedMetricDetailSections] keys every section item, so the list anchors
 * to the section the reader is inside. The item count is asserted alongside, because the
 * banner is never composed and a scaffold that inserted nothing would pass the position check.
 */
class MetricDetailScaffoldScrollAnchorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aReloadRaising_isLoading_insertsNothingAnywhere() {
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

        // The delete's reload flips the load flag.
        isLoading = true
        composeRule.waitForIdle()

        // The banner is above the viewport, never composed, but it is in the list.
        assertEquals(
            "the reload inserted an item into the list; the transient banner was removed",
            itemsBefore,
            composeRule.itemCount(listState),
        )
        val after = composeRule.onNodeWithText(ANCHOR_ROW).fetchSemanticsNode().boundsInRoot.top

        assertTrue(
            "the row the reader was looking at moved by ${abs(after - before)}px when the " +
                "reload started; it must stay put",
            abs(after - before) < TOLERANCE_PX,
        )
    }

    @Test
    fun aReloadLanding_removesNothingAndMovesNothingBack() {
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
            "the load landing removed an item from the list; the transient banner was removed",
            itemsBefore,
            composeRule.itemCount(listState),
        )
        val after = composeRule.onNodeWithText(ANCHOR_ROW).fetchSemanticsNode().boundsInRoot.top

        assertTrue(
            "the row moved by ${abs(after - before)}px when the reload finished",
            abs(after - before) < TOLERANCE_PX,
        )
    }

    @Test
    fun thePersistentSyncPausedBanner_entersTheListExactlyOnce() {
        var syncPaused by mutableStateOf(false)
        val listState = MetricDetailSectionListState(
            androidx.compose.foundation.lazy.LazyListState(),
        )

        composeRule.setContent {
            OpenVitalsTheme {
                MetricDetailScaffold(
                    isLoading = false,
                    syncPaused = syncPaused,
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

        // The paused banner is persistent: when sync pauses, exactly one item
        // enters the list, and its text is visible at the top — unlike the old
        // transient banner, it stays.
        val itemsBefore = composeRule.itemCount(listState)
        syncPaused = true
        composeRule.waitForIdle()

        assertEquals(
            "the paused banner did not land as exactly one list item",
            itemsBefore + 1,
            composeRule.itemCount(listState),
        )
        val pausedText = androidx.test.core.app.ApplicationProvider
            .getApplicationContext<android.content.Context>()
            .getString(tech.mmarca.openvitals.R.string.health_connect_sync_paused)
        composeRule.onNodeWithText(pausedText).assertExists()
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
