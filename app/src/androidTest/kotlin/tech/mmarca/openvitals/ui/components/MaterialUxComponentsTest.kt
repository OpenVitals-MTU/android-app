package tech.mmarca.openvitals.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.insights.DataConfidence
import tech.mmarca.openvitals.core.insights.DataConfidenceLevel
import tech.mmarca.openvitals.core.insights.DataSourceConsistency
import tech.mmarca.openvitals.core.insights.DataValueKind
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

class MaterialUxComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun adaptiveScaffold_exposesTopLevelNavigationAndAddAction() {
        var selectedRoute: String? = null

        composeRule.setContent {
            OpenVitalsTheme {
                OpenVitalsAdaptiveScaffold(
                    title = "Dashboard",
                    navigationDestinations = listOf(
                        OpenVitalsNavigationDestination(
                            route = "dashboard",
                            labelRes = R.string.bottom_nav_dashboard,
                            icon = Icons.Outlined.Dashboard,
                        ),
                        OpenVitalsNavigationDestination(
                            route = "sleep",
                            labelRes = R.string.screen_sleep,
                            icon = Icons.Outlined.Bed,
                        ),
                    ),
                    currentRoute = "dashboard",
                    showTopBar = true,
                    showNavigation = true,
                    canNavigateBack = false,
                    onNavigateBack = {},
                    onNavigate = { selectedRoute = it },
                    navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
                    navigationContentDescription = "Back",
                    action = MetricAction(
                        labelRes = R.string.action_add,
                        icon = Icons.Outlined.Add,
                        onClick = { selectedRoute = "manual_entry" },
                    ),
                ) {
                    Text("Content")
                }
            }
        }

        composeRule.onNodeWithText("Sleep").performClick()
        composeRule.runOnIdle {
            assertEquals("sleep", selectedRoute)
        }

        composeRule.onNodeWithText("Add").performClick()
        composeRule.runOnIdle {
            assertEquals("manual_entry", selectedRoute)
        }
    }

    @Test
    fun timeRangeSelector_reportsSegmentedSelection() {
        var selectedRange = TimeRange.DAY

        composeRule.setContent {
            OpenVitalsTheme {
                TimeRangeSelector(
                    selected = selectedRange,
                    onSelect = { selectedRange = it },
                )
            }
        }

        composeRule.onNodeWithText("Week").performClick()

        composeRule.runOnIdle {
            assertEquals(TimeRange.WEEK, selectedRange)
        }
    }

    @Test
    fun dataConfidenceCard_exposesSemanticSummary() {
        composeRule.setContent {
            OpenVitalsTheme {
                DataConfidenceCard(
                    confidence = DataConfidence(
                        level = DataConfidenceLevel.HIGH,
                        expectedDays = 7,
                        trackedDays = 7,
                        sampleCount = 14,
                        coveragePercent = 100,
                        sources = listOf("com.fitbit.Fitbit"),
                        sourceConsistency = DataSourceConsistency.SINGLE_SOURCE,
                        valueKind = DataValueKind.MEASURED,
                        manualEntryCount = 0,
                        warnings = emptyList(),
                    ),
                    accentColor = Color(0xFF0B6B58),
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            "Data confidence. High confidence. 7 of 7 days tracked (100%). " +
                "14 records. Source: Fitbit. Measured Health Connect records"
        ).assertExists()
    }
}
