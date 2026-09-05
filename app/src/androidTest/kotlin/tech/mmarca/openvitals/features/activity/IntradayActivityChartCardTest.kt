package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.HydrationColor
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * [IntradayActivityChartCard], shared by six features. A day with nothing must say so instead
 * of drawing an empty plot, and a day with readings gets the hour row.
 */
class IntradayActivityChartCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun anEmptyDaySaysSoAndDrawsNoPlot() {
        setCard(points = emptyList(), valueText = "0 L")

        composeRule
            .onNodeWithText(string(R.string.summary_empty_day, EMPTY_LABEL))
            .assertIsDisplayed()
        // No plot means no hour row either.
        composeRule.onNodeWithText(NOON).assertDoesNotExist()
    }

    @Test
    fun aDayWithReadingsDrawsThePlotAndTheHourRow() {
        setCard(points = listOf(at(9) to 500.0))

        composeRule.onNodeWithText(NOON).assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.summary_empty_day, EMPTY_LABEL))
            .assertDoesNotExist()
    }

    private fun setCard(
        points: List<Pair<Instant, Double>>,
        valueText: String = "1.2 L",
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                IntradayActivityChartCard(
                    selectedDate = DAY,
                    title = "Water",
                    valueText = valueText,
                    emptyText = EMPTY_LABEL,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    points = points,
                    accentColor = HydrationColor,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    private companion object {
        val ZONE: ZoneId = ZoneId.systemDefault()

        /** A fixed past day, so the card never takes its "today" branch. */
        val DAY: LocalDate = LocalDate.of(2026, 6, 22)

        const val EMPTY_LABEL = "Water"
        const val NOON = "12:00"

        fun at(hour: Int): Instant =
            DAY.atStartOfDay(ZONE).plusHours(hour.toLong()).toInstant()
    }
}
