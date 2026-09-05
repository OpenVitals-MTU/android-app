package tech.mmarca.openvitals.features.dashboard

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.testing.dashboardFixtureData
import tech.mmarca.openvitals.testing.dashboardFlowWidgetIds
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.testing.testUnitFormatter
import tech.mmarca.openvitals.ui.components.localizedDaySubtitle
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The dashboard as a screen reader hears it: tiles are named, not unlabeled groups with a
 * bare number, and quick actions are buttons in the tree TalkBack walks.
 */
class DashboardSemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tilesAndActionsAnnounceThemselvesToTheScreenReader() {
        val data = dashboardFixtureData()
        val unitFormatter = testUnitFormatter()
        val provider = DateTimeFormatterProvider()

        composeRule.setContent {
            OpenVitalsTheme {
                DashboardContent(
                    data = data,
                    display = DashboardPresentationMapper.build(
                        data = data,
                        dailyGoals = DashboardDailyGoals(),
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = provider,
                    ),
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = provider,
                    canGoForward = false,
                    isRefreshing = false,
                    dashboardWidgets = dashboardFlowWidgetIds,
                    isEditingDashboard = false,
                    onPreviousDay = {},
                    onNextDay = {},
                    onOpenCalendar = {},
                    onMoveWidgetToTarget = { _, _ -> },
                    onRemoveWidget = {},
                    onAddWidget = {},
                    onOpenMetric = {},
                    onOpenActivities = {},
                    onOpenActivity = {},
                    onEditActivity = {},
                    onDeleteActivity = {},
                    onOpenLog = {},
                    onStartActivity = {},
                    onToggleDashboardEdit = {},
                )
            }
        }

        // Metric tiles are named, not anonymous groups carrying a bare number.
        composeRule.onAllNodesWithText(string(R.string.metric_steps)).onFirst().assertIsDisplayed()

        // The day is announced, so a reader landing mid-screen knows which day these numbers belong to.
        composeRule.onNodeWithText(localizedDaySubtitle(data.date)).assertIsDisplayed()

        // Labelled and tappable through the tree.
        composeRule.onNodeWithText(string(R.string.dashboard_action_log))
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithText(string(R.string.dashboard_action_start_workout))
            .assertIsDisplayed()
            .assertHasClickAction()

        // The icon-only affordance carries a description rather than reading as "button".
        composeRule.onNodeWithContentDescription(string(R.string.cd_edit_dashboard))
            .assertHasClickAction()
    }
}
