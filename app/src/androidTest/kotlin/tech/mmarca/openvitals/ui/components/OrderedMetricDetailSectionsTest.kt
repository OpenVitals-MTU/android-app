package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Every metric detail screen is laid out by this one function. The stored order drives the
 * layout, and edit mode leaves the reorder reachable without a drag.
 */
class OrderedMetricDetailSectionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersInTheDefaultOrderAndHidesInvisibleSections() {
        setSections(isEditing = false)

        // The default order puts the goal above statistics above entries.
        val goalTop = composeRule.onNodeWithTag(GOAL).getUnclippedBoundsInRoot().top
        val statsTop = composeRule.onNodeWithTag(STATS).getUnclippedBoundsInRoot().top
        val entriesTop = composeRule.onNodeWithTag(ENTRIES).getUnclippedBoundsInRoot().top

        assertTrue("goal renders above statistics", goalTop < statsTop)
        assertTrue("statistics renders above entries", statsTop < entriesTop)

        // An invisible section is never built, so there is no empty slot for it.
        composeRule.onNodeWithTag(INTRADAY).assertDoesNotExist()
    }

    @Test
    fun editModeExposesEachSectionWithItsMoveActions() {
        setSections(isEditing = true)

        // The accessibility actions are the only non-drag route to reordering.
        val middle = composeRule.onNodeWithContentDescription(MetricDetailSectionId.STATISTICS.name)
        middle.assertIsDisplayed()

        val actionLabels = middle.fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .map { it.label }
        assertTrue(
            "the middle section can move up",
            string(R.string.cd_move_section_up) in actionLabels,
        )
        assertTrue(
            "the middle section can move down",
            string(R.string.cd_move_section_down) in actionLabels,
        )
    }

    @Test
    fun aSectionThatIsNotBeingEditedIsNotAnnouncedAsReorderable() {
        // Outside edit mode the boxes must not announce move actions that do nothing.
        setSections(isEditing = false)

        composeRule.onNodeWithContentDescription(MetricDetailSectionId.STATISTICS.name)
            .assertDoesNotExist()
    }

    @Test
    fun theFirstSectionCannotBeMovedUpOffTheTop() {
        setSections(isEditing = true)

        val labels = composeRule
            .onNodeWithContentDescription(MetricDetailSectionId.DAILY_GOAL.name)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .map { it.label }

        assertEquals(listOf(string(R.string.cd_move_section_down)), labels)
    }

    private fun setSections(isEditing: Boolean) {
        composeRule.setContent {
            OpenVitalsTheme {
                val context = MetricDetailSectionContext(
                    listState = rememberMetricDetailSectionListState(),
                    order = DefaultMetricDetailSectionOrder,
                    isEditingSections = isEditing,
                    onMoveSectionToTarget = { _, _ -> },
                    onMoveSection = { _, _ -> },
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    renderOrderedMetricDetailSections(context) {
                        // Declared out of order: the stored order must decide.
                        section(MetricDetailSectionId.ENTRIES) { Tag(ENTRIES) }
                        section(MetricDetailSectionId.STATISTICS) { Tag(STATS) }
                        section(MetricDetailSectionId.DAILY_GOAL) { Tag(GOAL) }
                        section(MetricDetailSectionId.INTRADAY_CHART, visible = false) {
                            Tag(INTRADAY)
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Tag(tag: String) {
        Text(text = tag, modifier = Modifier.testTag(tag))
    }

    private companion object {
        const val GOAL = "section-goal"
        const val STATS = "section-stats"
        const val ENTRIES = "section-entries"
        const val INTRADAY = "section-intraday"
    }
}
