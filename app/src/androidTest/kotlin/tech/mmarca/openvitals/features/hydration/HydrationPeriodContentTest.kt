package tech.mmarca.openvitals.features.hydration

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.dayAxisLabelsFor
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Which of the two empty messages the screen chooses: nothing drunk, or drinks logged that
 * did not count as hydration. Entry order and the day chart are tested here because
 * Kotlin decides both inside the content.
 */
class HydrationPeriodContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTheSummaryOnceLoaded() {
        setContent(state(hasData = true, dailyHydration = listOf(DailyHydration(ANCHOR, 2.1))))

        // A week summarises a total; only a day view is titled with the metric name alone.
        composeRule.onNodeWithText(string(R.string.metric_total_hydration)).assertIsDisplayed()
    }

    @Test
    fun showsTheEmptyPlaceholderWithNoData() {
        setContent(state(hasData = false))

        composeRule
            .onNodeWithText(string(R.string.message_no_hydration_period))
            .assertIsDisplayed()
    }

    @Test
    fun aPeriodWithDrinksButNoHydrationSaysSoDifferently() {
        // Beverages were logged; none added hydration. "No entries" would contradict the list below.
        setContent(state(hasData = false, entries = listOf(entry())))

        composeRule
            .onNodeWithText(string(R.string.message_no_hydration_added_period))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.message_no_hydration_period))
            .assertDoesNotExist()
    }

    @Test
    fun aLoadingPeriodDoesNotClaimThereIsNothingToShow() {
        setContent(state(hasData = false, isLoading = true))

        composeRule
            .onNodeWithText(string(R.string.message_no_hydration_period))
            .assertDoesNotExist()
    }

    @Test
    fun theEntryListIsNewestFirst() {
        // The glass just logged must be on top, not under every glass from the start of the period.
        val morning = entry(id = "morning", hour = 9, liters = 0.3)
        val afternoon = entry(id = "afternoon", hour = 14, liters = 0.5)
        val formatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        composeRule.setContent {
            OpenVitalsTheme {
                // Handed over oldest-first, so rendering the input order fails.
                HydrationEntriesContent(
                    entries = listOf(morning, afternoon),
                    unitFormatter = formatter,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                )
            }
        }

        val newest = composeRule
            .onNodeWithText(formatter.hydration(afternoon.liters).text)
            .getUnclippedBoundsInRoot()
        val oldest = composeRule
            .onNodeWithText(formatter.hydration(morning.liters).text)
            .getUnclippedBoundsInRoot()

        assertTrue(
            "the 14:00 drink should sit above the 09:00 one",
            newest.top < oldest.top,
        )
    }

    @Test
    fun aDayIsDrawnOverAnHourAxisRatherThanAsOneBarForTheWholeDay() {
        // The day view once drew the week chart with a single fat bar. A day chart says when you drank.
        setContent(
            state(
                hasData = true,
                selectedRange = TimeRange.DAY,
                entries = listOf(entry(id = "morning", hour = 8, liters = 0.3)),
            ),
        )

        dayAxisLabelsFor().forEach { hourLabel ->
            composeRule.onNodeWithText(hourLabel).assertIsDisplayed()
        }
        // The period chart is titled; the day chart is not.
        composeRule.onNodeWithText(string(R.string.metric_hydration_trend)).assertDoesNotExist()
    }

    private fun state(
        hasData: Boolean,
        isLoading: Boolean = false,
        selectedRange: TimeRange = TimeRange.WEEK,
        dailyHydration: List<DailyHydration> = emptyList(),
        entries: List<HydrationEntry> = emptyList(),
    ) = HydrationUiState(
        isLoading = isLoading,
        selectedRange = selectedRange,
        selectedDate = ANCHOR,
        dailyHydration = dailyHydration,
        hydrationEntries = entries,
        display = HydrationDisplayState(
            selectedPeriod = if (selectedRange == TimeRange.DAY) {
                DatePeriod(ANCHOR, ANCHOR)
            } else {
                DatePeriod(ANCHOR.minusDays(6), ANCHOR)
            },
            hasData = hasData,
            summary = HydrationPeriodSummary(totalLiters = 12.5, trackedDays = 7, loggedDays = 5),
        ),
    )

    private fun entry(
        id: String = "entry-1",
        hour: Int = 9,
        liters: Double = 0.0,
    ) = HydrationEntry(
        id = id,
        startTime = ANCHOR.atStartOfDay(ZoneId.systemDefault()).plusHours(hour.toLong()).toInstant(),
        endTime = ANCHOR.atStartOfDay(ZoneId.systemDefault()).plusHours(hour.toLong()).toInstant(),
        liters = liters,
        source = "tech.mmarca.openvitals",
    )

    private fun setContent(state: HydrationUiState) {
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
                    hydrationPeriodContent(
                        sectionContext = sectionContext,
                        state = state,
                        period = state.display.selectedPeriod,
                        unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }),
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        chartDaySelection = ChartDaySelection(selectedDate = null, onDateSelected = {}),
                        hasNotificationPermission = true,
                        onDecreaseGoal = {},
                        onIncreaseGoal = {},
                        onToggleReminders = {},
                        onRequestNotificationPermission = {},
                        onDecreaseInterval = {},
                        onIncreaseInterval = {},
                        onSelectActiveStartTime = { _: LocalTime -> },
                        onSelectActiveEndTime = { _: LocalTime -> },
                        onEditHydrationEntry = {},
                        onDeleteHydrationEntry = {},
                    )
                }
            }
        }
    }

    private companion object {
        /** A fixed past date, so the period never straddles today. */
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
    }
}
