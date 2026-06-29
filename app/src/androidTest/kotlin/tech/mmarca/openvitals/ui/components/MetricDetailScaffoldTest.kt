package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme
import java.time.LocalDate

class MetricDetailScaffoldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun metricDetailScaffold_reportsTimeRangeSelection() {
        var selectedRange = TimeRange.DAY

        composeRule.setContent {
            OpenVitalsTheme {
                MetricDetailScaffold(
                    isLoading = false,
                    selectedRange = selectedRange,
                    selectedDate = LocalDate.now().minusDays(1),
                    onRefresh = {},
                    onSelectRange = { selectedRange = it },
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onSelectDate = {},
                    content = scaffoldTestContent,
                )
            }
        }

        composeRule.onNodeWithText("Week").performClick()

        composeRule.runOnIdle {
            assertEquals(TimeRange.WEEK, selectedRange)
        }
    }

    @Test
    fun metricDetailScaffold_periodNavigatorSwipesFireCallbacks() {
        var previousCount = 0
        var nextCount = 0
        val date = LocalDate.now().minusDays(1)

        composeRule.setContent {
            OpenVitalsTheme {
                MetricDetailScaffold(
                    isLoading = false,
                    selectedRange = TimeRange.DAY,
                    selectedDate = date,
                    onRefresh = {},
                    onSelectRange = {},
                    onPreviousPeriod = { previousCount += 1 },
                    onNextPeriod = { nextCount += 1 },
                    onSelectDate = {},
                    content = scaffoldTestContent,
                )
            }
        }

        composeRule.onNodeWithText("Yesterday").performTouchInput { swipeLeft() }
        composeRule.runOnIdle {
            assertEquals(0, previousCount)
            assertEquals(1, nextCount)
        }

        composeRule.onNodeWithText("Yesterday").performTouchInput { swipeRight() }
        composeRule.runOnIdle {
            assertEquals(1, previousCount)
            assertEquals(1, nextCount)
        }
    }

    @Test
    fun metricDetailScaffold_displaysErrorMessage() {
        composeRule.setContent {
            OpenVitalsTheme {
                MetricDetailScaffold(
                    isLoading = false,
                    selectedRange = TimeRange.DAY,
                    selectedDate = LocalDate.now(),
                    error = "Unable to load sleep data",
                    onRefresh = {},
                    onSelectRange = {},
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onSelectDate = {},
                    content = scaffoldTestContent,
                )
            }
        }

        composeRule.onNodeWithText("Unable to load sleep data").assertExists()
    }

    @Test
    fun metricDetailScaffold_rendersContentForComputedPeriod() {
        var renderedPeriod: DatePeriod? = null
        val date = LocalDate.of(2026, 6, 15)

        composeRule.setContent {
            OpenVitalsTheme {
                MetricDetailScaffold(
                    isLoading = false,
                    selectedRange = TimeRange.WEEK,
                    selectedDate = date,
                    onRefresh = {},
                    onSelectRange = {},
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onSelectDate = {},
                    content = { period ->
                        renderedPeriod = period
                        item {
                            Text(
                                text = period.start.toString(),
                                modifier = Modifier.testTag("period-content"),
                            )
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithTag("period-content").assertExists()
        composeRule.runOnIdle {
            assertEquals(LocalDate.of(2026, 6, 15), renderedPeriod?.start)
            assertEquals(LocalDate.of(2026, 6, 21), renderedPeriod?.end)
        }
    }
}

private val scaffoldTestContent: LazyListScope.(DatePeriod) -> Unit = { period ->
    item {
        Text(
            text = period.start.toString(),
            modifier = Modifier.testTag("period-content"),
        )
    }
}
