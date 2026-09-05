package tech.mmarca.openvitals.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** The header slot, the range labels, and the navigator's title once a range is chosen. Shared by every metric screen. */
class MetricDetailScaffoldHeaderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTheHeaderSlotTheRangeLabelsAndTheContent() {
        setScaffold(range = TimeRange.WEEK)

        composeRule.onNodeWithText(HEADER).assertIsDisplayed()
        composeRule.onNodeWithText(CONTENT).assertIsDisplayed()
        listOf(
            R.string.range_day,
            R.string.range_week,
            R.string.range_month,
            R.string.range_year,
        ).forEach { composeRule.onNodeWithText(string(it)).assertExists() }
    }

    @Test
    fun selectingARangeReportsItAndRetitlesTheNavigator() {
        var selected: TimeRange? = null
        setScaffold(range = TimeRange.WEEK, onSelectRange = { selected = it })

        composeRule.onNodeWithText(string(R.string.range_day)).performClick()
        composeRule.waitForIdle()

        assertEquals(TimeRange.DAY, selected)
    }

    @Test
    fun aDayRangeAnchoredOnTodayNamesItToday() {
        // The navigator's title is derived from the range and the anchor, not stored.
        setScaffold(range = TimeRange.DAY, date = LocalDate.now())

        composeRule.onNodeWithText(string(R.string.period_today)).assertIsDisplayed()
    }

    private fun setScaffold(
        range: TimeRange,
        date: LocalDate = ANCHOR,
        onSelectRange: (TimeRange) -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                MetricDetailScaffold(
                    isLoading = false,
                    selectedRange = range,
                    selectedDate = date,
                    onRefresh = {},
                    onSelectRange = onSelectRange,
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onSelectDate = {},
                    headerItems = { item { Text(HEADER) } },
                ) {
                    item { Text(CONTENT) }
                }
            }
        }
    }

    private companion object {
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
        const val HEADER = "header slot content"
        const val CONTENT = "period content"
    }
}
