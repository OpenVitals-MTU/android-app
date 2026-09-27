package tech.mmarca.openvitals.features.caffeine

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CaffeineDailyStat
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.model.CaffeineEntryInsight
import tech.mmarca.openvitals.domain.model.CaffeineInsights
import tech.mmarca.openvitals.domain.model.CaffeinePoint
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** Each range leads with what it is for: today with the active dose, longer periods with totals. */
class CaffeineContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun todayLeadsWithTheActiveDoseAndTheDecayCurve() {
        setContent(
            CaffeineUiState(
                isLoading = false,
                selectedRange = TimeRange.DAY,
                selectedDate = LocalDate.now(),
                display = INSIGHTS,
            ),
        )

        composeRule.onNodeWithText(string(R.string.caffeine_current_title)).assertIsDisplayed()
        // The curve card used to draw with no title while the translated string sat unused.
        scrollTo(string(R.string.caffeine_curve_title))
        composeRule.onNodeWithText(string(R.string.caffeine_curve_title)).assertIsDisplayed()
    }

    @Test
    fun aPastDayShowsWhatWasDrunkNotWhatIsActiveNow() {
        setContent(
            CaffeineUiState(
                isLoading = false,
                selectedRange = TimeRange.DAY,
                selectedDate = LocalDate.now().minusDays(3),
                display = INSIGHTS,
            ),
        )

        // The card leads with it; the statistics repeat it further down.
        composeRule.onAllNodesWithText(string(R.string.stat_total_intake)).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.caffeine_current_title)).assertDoesNotExist()
    }

    @Test
    fun aWeekKeepsTheSleepNightsAndTheBreakdowns() {
        setContent(
            CaffeineUiState(
                isLoading = false,
                selectedRange = TimeRange.WEEK,
                display = INSIGHTS,
            ),
        )

        scrollTo(string(R.string.caffeine_safe_calendar))
        composeRule.onNodeWithText(string(R.string.caffeine_safe_calendar)).assertIsDisplayed()
        scrollTo(string(R.string.caffeine_time_of_day))
        composeRule.onNodeWithText(string(R.string.caffeine_time_of_day)).assertIsDisplayed()
        // The day curve belongs to the Day range.
        composeRule.onNodeWithText(string(R.string.caffeine_curve_title)).assertDoesNotExist()
    }

    @Test
    fun anEmptyWeekSaysSoOnce() {
        setContent(
            CaffeineUiState(
                isLoading = false,
                selectedRange = TimeRange.WEEK,
                display = INSIGHTS.copy(entryInsights = emptyList()),
            ),
        )

        composeRule.onNodeWithText(string(R.string.caffeine_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.caffeine_safe_calendar)).assertDoesNotExist()
    }

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    private fun setContent(state: CaffeineUiState) {
        composeRule.setContent {
            OpenVitalsTheme {
                val sectionContext = MetricDetailSectionContext(
                    listState = rememberMetricDetailSectionListState(),
                    order = DefaultMetricDetailSectionOrder,
                    isEditingSections = false,
                    onMoveSectionToTarget = { _, _ -> },
                    onMoveSection = { _, _ -> },
                )
                LazyColumn {
                    caffeinePeriodContent(
                        sectionContext = sectionContext,
                        state = state,
                        period = PERIOD,
                        unitFormatter = FORMATTER,
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        chartDaySelection = ChartDaySelection(selectedDate = null, onDateSelected = {}),
                        onCompleteSetup = {},
                        onSkipSetup = {},
                        onSelectEntry = {},
                        onOpenDrink = { _, _ -> },
                        onDeleteEntry = {},
                    )
                }
            }
        }
    }

    private companion object {
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        /** A fixed afternoon, so the curve never depends on when the suite runs. */
        val ANCHOR: Instant = Instant.parse("2026-06-23T14:00:00Z")

        val PERIOD = DatePeriod(LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 28))

        val COFFEE = CaffeineEntry(
            id = "coffee",
            startTime = ANCHOR,
            endTime = ANCHOR.plusSeconds(600),
            caffeineMg = 95.0,
            name = "Coffee",
            source = "Test source",
            mealType = 0,
        )

        val INSIGHTS = CaffeineInsights(
            currentMg = 142.0,
            todayTotalMg = 215.0,
            periodTotalMg = 215.0,
            sleepThresholdMg = 50,
            bedtime = LocalTime.of(23, 0),
            timeToThresholdMinutes = 240L,
            curvePoints = List(12) { step ->
                CaffeinePoint(
                    time = ANCHOR.plusSeconds(step * 3_600L),
                    valueMg = 215.0 - step * 12.0,
                )
            },
            dailyStats = List(7) { day ->
                CaffeineDailyStat(
                    date = PERIOD.start.plusDays(day.toLong()),
                    totalMg = if (day == 1) 215.0 else 0.0,
                    bedtimeMg = if (day == 1) 40.0 else 0.0,
                    safeForSleep = true,
                )
            },
            entryInsights = listOf(
                CaffeineEntryInsight(
                    entry = COFFEE,
                    currentContributionMg = 0.0,
                    inferredCategory = CaffeineSourceCategory.COFFEE,
                ),
            ),
        )
    }
}
