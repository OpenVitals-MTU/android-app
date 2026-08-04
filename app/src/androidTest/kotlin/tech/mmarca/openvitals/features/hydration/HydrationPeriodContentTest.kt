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
 * Port of the rendering cases of Flutter's
 * `test/features/hydration/hydration_screen_test.dart`.
 *
 * The totals and the has-data decision are covered on the JVM. What is only
 * visible here is which of the two empty messages the screen chooses — a
 * distinction Kotlin makes and Flutter does not, and one that matters: a period
 * where nothing was drunk and a period where drinks were logged but none of
 * them counted as hydration are different facts about the user's week, and only
 * one of them means "you did not log anything".
 *
 * The entry-order case from `hydration_display_test.dart` and the day-chart case
 * from `hydration_intraday_chart_test.dart` live here too: Kotlin sorts inside
 * the content and picks the chart inside the content, so neither has a display
 * field a JVM test could read.
 */
class HydrationPeriodContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTheSummaryOnceLoaded() {
        setContent(state(hasData = true, dailyHydration = listOf(DailyHydration(ANCHOR, 2.1))))

        // A week summarises a total; only a DAY view is titled with the metric
        // name alone, so asserting the latter here would pass for the wrong reason.
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
        // Beverages were logged; none of them added hydration. Telling the user
        // "no entries were recorded" would contradict the list of entries the
        // very same screen is about to show them.
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
        // The list is a log, and a log the user scans top-down to check what they
        // have already drunk. Sorted the other way, the glass they just logged is
        // buried under every glass from the start of the period.
        val morning = entry(id = "morning", hour = 9, liters = 0.3)
        val afternoon = entry(id = "afternoon", hour = 14, liters = 0.5)
        val formatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        composeRule.setContent {
            OpenVitalsTheme {
                // Handed to the content oldest-first, so a content that simply
                // renders what it is given fails rather than passes by luck.
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
        // The Day view once drew the WEEK chart with a single day in it: one fat
        // bar restating a total the card above it already shows. A day chart
        // exists to say WHEN you drank, and that is the hour axis under it.
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
        // The period bar chart is titled; the day chart is not. Its title showing
        // up here would mean the day fell back to the week chart.
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
