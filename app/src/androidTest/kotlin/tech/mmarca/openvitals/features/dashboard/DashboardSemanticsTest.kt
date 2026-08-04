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
 * Port of Flutter's `test/features/dashboard/dashboard_semantics_test.dart`.
 *
 * The dashboard as a screen reader hears it. Not a full accessibility audit —
 * it pins the two properties that rot silently.
 *
 * A tile whose title reaches the user only as pixels (a canvas-drawn label, a
 * `clearAndSetSemantics`) reads out as an unlabeled group: the number is
 * announced with nothing to say what it counts. And a quick action that renders
 * as a decorated row rather than a button is invisible to every user who
 * navigates by tapping through controls — the affordance is there on screen and
 * absent from the tree that TalkBack walks.
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

        // The day the whole screen is about is announced, so a reader who lands
        // mid-screen can still tell which day these numbers belong to.
        composeRule.onNodeWithText(localizedDaySubtitle(data.date)).assertIsDisplayed()

        // The two primary actions are labelled AND tappable through the tree —
        // a label with no click action is a control a screen reader cannot press.
        composeRule.onNodeWithText(string(R.string.dashboard_action_log))
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithText(string(R.string.dashboard_action_start_workout))
            .assertIsDisplayed()
            .assertHasClickAction()

        // The icon-only affordance carries a description rather than reaching a
        // reader as "button", which is the failure an icon button invites.
        composeRule.onNodeWithContentDescription(string(R.string.cd_edit_dashboard))
            .assertHasClickAction()
    }
}
