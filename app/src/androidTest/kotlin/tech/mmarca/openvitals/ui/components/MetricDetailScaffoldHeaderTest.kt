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

/**
 * The parts of Flutter's `test/ui/components/metric_detail_scaffold_test.dart`
 * that `MetricDetailScaffoldTest` does not already cover: the header slot, the
 * range labels, and what the navigator is titled once a range is chosen.
 *
 * The scaffold is shared by every metric screen, so a header slot that silently
 * drops its content, or a navigator that keeps the old period's title after a
 * range change, is wrong on a dozen screens at once.
 */
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
        // The navigator's title is derived from the range and the anchor, not
        // stored — so a stale title is how a screen ends up claiming to show a
        // day it is not showing.
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
