package tech.mmarca.openvitals.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Every surface names a period the same way. Only the navigator passed `weekPeriodMode`,
 * so rolling ranges showed "Last 30 days" beside "This month". It now defaults from
 * `LocalPeriodWeekMode`. These assert on the count of a label, not on any one surface.
 */
class PeriodTitleAgreementTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rollingModeNamesAPeriodTheSameOnEverySurface() {
        val today = LocalDate.now()
        setThreeSurfaces(
            mode = WeekPeriodMode.LAST_7_DAYS,
            period = DatePeriod(today.minusDays(29), today),
        )

        // All three: an unthreaded call site shows up as a count of 1.
        composeRule
            .onAllNodesWithText(string(R.string.period_last_30_days))
            .assertCountEquals(SURFACES)
        composeRule.onNodeWithText(string(R.string.period_this_month)).assertDoesNotExist()
    }

    @Test
    fun calendarModeStillNamesItTheCalendarWay() {
        // The fix must not drag every user onto rolling labels.
        val today = LocalDate.now()
        setThreeSurfaces(
            mode = WeekPeriodMode.MONDAY_TO_SUNDAY,
            period = DatePeriod(today.withDayOfMonth(1), today),
        )

        composeRule
            .onAllNodesWithText(string(R.string.period_this_month))
            .assertCountEquals(SURFACES)
        composeRule.onNodeWithText(string(R.string.period_last_30_days)).assertDoesNotExist()
    }

    /** Three call sites that do not pass the mode: a card subtitle, a chart summary and a bare title. */
    private fun setThreeSurfaces(mode: WeekPeriodMode, period: DatePeriod) {
        composeRule.setContent {
            OpenVitalsTheme {
                CompositionLocalProvider(LocalPeriodWeekMode provides mode) {
                    androidx.compose.foundation.layout.Column {
                        repeat(SURFACES) {
                            Text(localizedPeriodTitle(TimeRange.MONTH, period))
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val SURFACES = 3
    }
}
