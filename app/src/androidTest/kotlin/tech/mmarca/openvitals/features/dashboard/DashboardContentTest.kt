package tech.mmarca.openvitals.features.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.testing.dashboardFixtureData
import tech.mmarca.openvitals.testing.dashboardFlowWidgetIds
import tech.mmarca.openvitals.testing.testUnitFormatter
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Kotlin's dashboard has no permission prompt in its body and no sensor-status row,
 * so those cases are true by construction and pinned as such.
 */
class DashboardContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTheSummaryDashboardOnceLoaded() {
        setDashboard()

        composeRule.onAllNodesWithText("Steps").onFirst().assertIsDisplayed()
    }

    @Test
    fun missingPermissionsProduceNoPromptJustTheDashboard() {
        // An access problem goes to the shell's gate, not a prompt among the tiles.
        setDashboard()

        composeRule.onAllNodesWithText("Steps").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Grant permission").assertDoesNotExist()
    }

    @Test
    fun editModeEntersAndExitsWithoutLosingTheGrid() {
        var editToggles = 0
        var editing = false
        composeRule.setContent {
            OpenVitalsTheme {
                val data = dashboardFixtureData()
                DashboardContent(
                    data = data,
                    display = DashboardPresentationMapper.build(
                        data = data,
                        dailyGoals = DashboardDailyGoals(),
                        unitFormatter = testUnitFormatter(),
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    ),
                    unitFormatter = testUnitFormatter(),
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    canGoForward = false,
                    isRefreshing = false,
                    dashboardWidgets = dashboardFlowWidgetIds,
                    isEditingDashboard = editing,
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
                    onToggleDashboardEdit = { editToggles++ },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Edit summary").performClick()
        composeRule.runOnIdle { editing = true }
        composeRule.waitForIdle()

        assertEquals(1, editToggles)
        // The grid survives the mode change.
        composeRule.onAllNodesWithText("Steps").onFirst().assertIsDisplayed()
    }

    @Test
    fun aTileOpensItsMetric() {
        var opened: DashboardWidgetId? = null
        setDashboard(onOpenMetric = { opened = it })

        composeRule.onAllNodesWithText("Steps").onFirst().performClick()
        composeRule.waitForIdle()

        assertEquals(DashboardWidgetId.STEPS, opened)
    }

    private fun setDashboard(
        isEditing: Boolean = false,
        onOpenMetric: (DashboardWidgetId) -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                val unitFormatter = testUnitFormatter()
                val provider = DateTimeFormatterProvider()
                val data = dashboardFixtureData()
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
                    isEditingDashboard = isEditing,
                    onPreviousDay = {},
                    onNextDay = {},
                    onOpenCalendar = {},
                    onMoveWidgetToTarget = { _, _ -> },
                    onRemoveWidget = {},
                    onAddWidget = {},
                    onOpenMetric = onOpenMetric,
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
    }
}
