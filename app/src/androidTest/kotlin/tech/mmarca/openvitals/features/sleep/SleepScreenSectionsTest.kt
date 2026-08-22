package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.CrossMetricValue
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Ports the section-composition cases of Flutter's `sleep_screen_test.dart` and
 * `sleep_overview_card_test.dart`.
 *
 * These are the parts of the Sleep screen a user notices only when they are wrong:
 * a correlation claimed from too little data, a page that renders blank because of
 * the day it was opened on, an empty state that names the wrong span of time.
 */
class SleepScreenSectionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aCoupleOfNightsIsNotEnoughToClaimASleepHrvCorrelation() {
        // Two nights can be made to correlate perfectly by chance. Showing the card
        // anyway would dress up noise as a finding about the user's own body.
        composeRule.setContent {
            OpenVitalsTheme {
                Column {
                    SleepHrvInsightSectionContent(
                        durationPoints = durationPoints(2),
                        hrvValues = hrvValues(2),
                    )
                }
            }
        }

        composeRule.onNodeWithText(string(R.string.cross_sleep_hrv_title)).assertDoesNotExist()
        composeRule
            .onNodeWithText(string(R.string.section_cross_metric_insights))
            .assertDoesNotExist()
    }

    @Test
    fun aWeekOfNightsPairedWithHrvShowsTheCorrelationCard() {
        // The other half of the same rule: once there are enough paired nights the
        // insight has to actually appear, or the threshold is indistinguishable
        // from the card being broken.
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SleepHrvInsightSectionContent(
                        durationPoints = durationPoints(5),
                        hrvValues = hrvValues(5),
                    )
                }
            }
        }

        composeRule
            .onNodeWithText(string(R.string.cross_sleep_hrv_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun theDayViewClosesWithTheDataSourceEducationLink() {
        // Sleep numbers come from whichever app wrote them, and the day view is
        // where a user lands when a figure looks wrong. The link out to the data
        // sources is the answer to "where did this come from" and has to survive
        // section reordering at the bottom of the list.
        setSleepContent { sectionContext ->
            sleepDayContent(
                state = dayState(),
                display = dayDisplay(),
                period = DatePeriod(ANCHOR, ANCHOR),
                unitFormatter = FORMATTER,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                sectionContext = sectionContext,
                onOpenSleepSession = {},
                onOpenSleepScore = null,
                onOpenSleepEfficiency = null,
                onDecreaseGoal = {},
                onIncreaseGoal = {},
            )
        }

        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText(string(R.string.health_connect_data_source_manage)))
        composeRule
            .onNodeWithText(string(R.string.health_connect_data_source_manage))
            .assertIsDisplayed()
    }

    @Test
    fun aPeriodViewStillRendersWhenTheSelectedDayHasNoSleep() {
        // Open the app after midnight and today has no night yet, so the selected
        // day's summary is null while the week behind it is full of data. The period
        // view must not be derived from the selected day: a user switching to the
        // week would get a blank page for a week they did sleep through.
        setSleepContent { sectionContext ->
            sleepPeriodContent(
                state = weekState(),
                display = weekDisplay(),
                period = DatePeriod(ANCHOR.minusDays(6), ANCHOR),
                chartDaySelection = ChartDaySelection(selectedDate = null, onDateSelected = {}),
                unitFormatter = FORMATTER,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                sectionContext = sectionContext,
                onOpenSleepSession = {},
                onOpenSleepScore = null,
                onOpenSleepEfficiency = null,
                onDecreaseGoal = {},
                onIncreaseGoal = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.recovery_sleep_duration)).assertIsDisplayed()
        composeRule.onNodeWithText(FORMATTER.duration(7 * HOUR_MS)).assertIsDisplayed()
        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("sleep_week_period_content"))
        composeRule.onNodeWithTag("sleep_week_period_content").assertExists()
    }

    @Test
    fun theDayEmptyStateNamesTheSelectedDayAndNotThePeriod() {
        // "No sleep data for the selected day" and "in the selected period" answer
        // different questions. Showing the period wording on the day view tells a
        // user their whole week is missing when only last night is.
        composeRule.setContent {
            OpenVitalsTheme {
                LazyColumn { sleepNoDataContent(selectedRange = TimeRange.DAY) }
            }
        }

        composeRule
            .onNodeWithText(string(R.string.message_no_sleep_day_selected))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.message_no_sleep_period)).assertDoesNotExist()
    }

    @Test
    fun theOverviewKeepsAsleepTimeInBedAndAwakeAsThreeDistinctFigures() {
        // The point of the overview is "focus on sleep, not time in bed": asleep is
        // promoted, time in bed is kept but demoted, and awake sits beside it. Wire
        // any two of them to the same source and the card silently stops saying
        // anything — three identical numbers still look like a working card.
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SleepOverviewSectionContent(
                        summary = SleepOverviewSummary(
                            sleepScore = 82,
                            sleepDurationMs = 6 * HOUR_MS + 30 * MINUTE_MS,
                            timeInBedMs = 7 * HOUR_MS + 45 * MINUTE_MS,
                            awakeDurationMs = 45 * MINUTE_MS,
                            remDurationMs = 60 * MINUTE_MS,
                            coreDurationMs = 200 * MINUTE_MS,
                            deepDurationMs = 60 * MINUTE_MS,
                            sleepEfficiencyPercent = 84.0,
                        ),
                        selectedRange = TimeRange.WEEK,
                        period = DatePeriod(ANCHOR.minusDays(6), ANCHOR),
                        unitFormatter = FORMATTER,
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        onOpenSleepScore = null,
                        onOpenSleepEfficiency = null,
                    )
                }
            }
        }

        composeRule.onNodeWithText(string(R.string.recovery_sleep_duration)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.sleep_time_in_bed)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.sleep_stage_awake)).assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.recovery_sleep_efficiency))
            .performScrollTo()
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(FORMATTER.duration(6 * HOUR_MS + 30 * MINUTE_MS))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(FORMATTER.duration(7 * HOUR_MS + 45 * MINUTE_MS))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(FORMATTER.duration(45 * MINUTE_MS))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun theDayViewDropsTheKeyMetricsListButKeepsTheOverviewCards() {
        // On a single night the four key metrics are all restatements: the schedule is
        // the timeline chart's own axis, REM and deep sleep are measured bars in the
        // share-of-time-in-bed card, and sleep efficiency was superseded by the sleep
        // score that feeds the recharge battery. The overview cards above them are not
        // redundant, so hiding the list must not take those with it.
        setSleepContent { sectionContext ->
            sleepDayContent(
                state = dayState(),
                display = dayDisplay(),
                period = DatePeriod(ANCHOR, ANCHOR),
                unitFormatter = FORMATTER,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                sectionContext = sectionContext,
                onOpenSleepSession = {},
                onOpenSleepScore = null,
                onOpenSleepEfficiency = null,
                onDecreaseGoal = {},
                onIncreaseGoal = {},
            )
        }

        // The whole overview is one lazy item, so scrolling to it composes the key
        // metrics too if they are still there to compose.
        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText(string(R.string.recovery_sleep_score)))
        composeRule.onNodeWithText(string(R.string.recovery_sleep_score)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.activities_key_metrics)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.recovery_sleep_schedule)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.recovery_rem_sleep)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.recovery_deep_sleep)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.recovery_sleep_efficiency)).assertDoesNotExist()
    }

    private fun setSleepContent(content: LazyListScope.(MetricDetailSectionContext) -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                val sectionContext = sectionContext()
                LazyColumn(state = sectionContext.listState.lazyListState) {
                    content(sectionContext)
                }
            }
        }
    }

    @Composable
    private fun sectionContext() = MetricDetailSectionContext(
        listState = rememberMetricDetailSectionListState(),
        order = DefaultMetricDetailSectionOrder,
        isEditingSections = false,
        onMoveSectionToTarget = { _, _ -> },
        onMoveSection = { _, _ -> },
    )

    private fun dayState() = SleepUiState(
        isLoading = false,
        selectedRange = TimeRange.DAY,
        selectedDate = ANCHOR,
        sessions = listOf(night(ANCHOR)),
    )

    private fun dayDisplay() = SleepDisplayState(
        dailySessions = listOf(night(ANCHOR)),
        dailySummary = night(ANCHOR),
        selectedPeriod = DatePeriod(ANCHOR, ANCHOR),
        durationPoints = listOf(SleepDurationPoint(date = ANCHOR, hours = 7.0)),
        overviewDays = listOf(SleepOverviewDay(date = ANCHOR, sessions = listOf(night(ANCHOR)))),
        overviewSummary = overviewSummary(),
    )

    private fun weekState() = SleepUiState(
        isLoading = false,
        selectedRange = TimeRange.WEEK,
        selectedDate = ANCHOR,
        sessions = (1..3).map { night(ANCHOR.minusDays(it.toLong())) },
    )

    /** A week with nights behind it, but nothing at all for the selected day. */
    private fun weekDisplay() = SleepDisplayState(
        dailySessions = emptyList(),
        dailySummary = null,
        selectedPeriod = DatePeriod(ANCHOR.minusDays(6), ANCHOR),
        durationPoints = (0..6).map {
            val date = ANCHOR.minusDays(6L - it)
            SleepDurationPoint(date = date, hours = if (date == ANCHOR) 0.0 else 7.0)
        },
        overviewDays = (1..3).map { offset ->
            val date = ANCHOR.minusDays(offset.toLong())
            SleepOverviewDay(date = date, sessions = listOf(night(date)))
        },
        overviewSummary = overviewSummary(),
    )

    private fun overviewSummary() = SleepOverviewSummary(
        dates = (0..6).map { ANCHOR.minusDays(6L - it) },
        sleepDurationMs = 7 * HOUR_MS,
        timeInBedMs = 8 * HOUR_MS,
        awakeDurationMs = 30 * MINUTE_MS,
        remDurationMs = 90 * MINUTE_MS,
        coreDurationMs = 240 * MINUTE_MS,
        deepDurationMs = 60 * MINUTE_MS,
    )

    private fun night(date: LocalDate): SleepData {
        val start = LocalDateTime.of(date.minusDays(1), LocalTime.of(23, 0))
            .atZone(ZONE)
            .toInstant()
        val end = start.plusSeconds(8 * 3600)
        return SleepData(
            id = "sleep-$date",
            startTime = start,
            endTime = end,
            durationMs = 8 * HOUR_MS,
            source = "com.test.tracker",
            stages = listOf(
                stage(start, start.plusSeconds(4 * 3600), SleepStage.STAGE_LIGHT),
                stage(start.plusSeconds(4 * 3600), start.plusSeconds(6 * 3600), SleepStage.STAGE_DEEP),
                stage(start.plusSeconds(6 * 3600), end, SleepStage.STAGE_REM),
            ),
        )
    }

    private fun stage(start: Instant, end: Instant, stageType: Int) =
        SleepStage(startTime = start, endTime = end, stageType = stageType)

    private fun durationPoints(count: Int) = (0 until count).map { index ->
        SleepDurationPoint(date = ANCHOR.minusDays(index.toLong()), hours = 6.0 + index)
    }

    private fun hrvValues(count: Int) = (0 until count).map { index ->
        CrossMetricValue(date = ANCHOR.minusDays(index.toLong()), value = 40.0 + index * 3)
    }

    private companion object {
        const val MINUTE_MS = 60_000L
        const val HOUR_MS = 3_600_000L

        /** Anchored on a fixed past date so the fixture never drifts. */
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
        val ZONE: ZoneId = ZoneId.systemDefault()
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
    }
}
