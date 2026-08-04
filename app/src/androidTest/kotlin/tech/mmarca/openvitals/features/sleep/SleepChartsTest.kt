package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme
import tech.mmarca.openvitals.R

/**
 * Ports of Flutter's `sleep_schedule_chart_test.dart` and the one case of
 * `sleep_stage_share_card_test.dart` that maps onto Kotlin's card.
 *
 * Kotlin's `SleepStageShareCard` is a vertical per-stage breakdown with no tap
 * target, so the share-card file's pixel-width and tap cases are a layout
 * difference rather than missing coverage; its lane-chart and tap cases belong to
 * the day card and live in `SleepSessionTimelineCardTest`.
 */
class SleepChartsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theScheduleChartDrawsItsTitleAndSummary() {
        setSchedule(days = listOf(night(ANCHOR)))

        composeRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(SUMMARY).assertIsDisplayed()
    }

    @Test
    fun theScheduleChartRendersNothingWhenNoNightHasABedtime() {
        // Every night lacking a bedtime leaves no axis to draw against. Drawing
        // an empty frame with a title would present "no data" as a measurement.
        setSchedule(days = listOf(SleepScheduleDay(date = ANCHOR, inBedStart = null, inBedEnd = null)))

        composeRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeRule.onNodeWithText(SUMMARY).assertDoesNotExist()
    }

    @Test
    fun anImpossibleNightStillRenders_itJustDoesNotScale() {
        // A session that ends before it starts is a real thing to receive from
        // another app. It must not take the chart down with it.
        val start = ANCHOR.atStartOfDay(ZONE).plusHours(23).toInstant()
        setSchedule(
            days = listOf(
                SleepScheduleDay(
                    date = ANCHOR,
                    inBedStart = start,
                    inBedEnd = start.minusSeconds(3_600),
                ),
                night(ANCHOR.minusDays(1)),
            ),
        )

        composeRule.onNodeWithText(TITLE).assertIsDisplayed()
    }

    @Test
    fun noStageDataHidesTheShareCardRatherThanDrawingEmptyBars() {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SleepStageShareCard(
                        durations = SleepStageDurations(awakeMs = 0, remMs = 0, lightMs = 0, deepMs = 0),
                        unitFormatter = FORMATTER,
                    )
                }
            }
        }

        composeRule.onNodeWithText(string(R.string.sleep_stages_share_title)).assertDoesNotExist()
    }

    @Test
    fun stageDataDrawsTheShareCard() {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SleepStageShareCard(
                        durations = SleepStageDurations(
                            awakeMs = 20L * 60_000L,
                            remMs = 90L * 60_000L,
                            lightMs = 240L * 60_000L,
                            deepMs = 70L * 60_000L,
                        ),
                        unitFormatter = FORMATTER,
                    )
                }
            }
        }

        composeRule.onNodeWithText(string(R.string.sleep_stages_share_title)).assertIsDisplayed()
    }

    @Test
    fun tappingANightReportsThatNightAndNotItsNeighbour() {
        // The bars are painted on a canvas, so the only thing standing between a
        // tap and the right night is the slot arithmetic. Off by one slot and the
        // day list below the chart shows the wrong night's sleep, convincingly.
        val week = (0..6).map { ANCHOR.minusDays(6L - it) }
        var reported: LocalDate? = null
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SleepScheduleStageChart(
                        title = TITLE,
                        summaryText = SUMMARY,
                        days = week.map { night(it) },
                        sleepWindow = SleepWindow.Default,
                        selectedRange = TimeRange.WEEK,
                        period = DatePeriod(week.first(), week.last()),
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        onDateSelected = { reported = it },
                        modifier = Modifier.testTag(CHART),
                    )
                }
            }
        }

        composeRule.onNodeWithTag(CHART).performTouchInput { clickSlot(2, week.size) }
        composeRule.runOnIdle { assertEquals(week[2], reported) }

        composeRule.onNodeWithTag(CHART).performTouchInput { clickSlot(5, week.size) }
        composeRule.runOnIdle { assertEquals(week[5], reported) }
    }

    /**
     * The centre of day [index]'s slot, in the tapped card's own coordinates: the
     * chart canvas is inset by the card padding and reserves a strip on its right
     * for the clock-time labels.
     */
    private fun TouchInjectionScope.clickSlot(index: Int, dayCount: Int) {
        val cardPadding = 16.dp.toPx()
        val barsWidth = width - 2 * cardPadding - 46.dp.toPx()
        val slotWidth = barsWidth / dayCount
        click(Offset(cardPadding + slotWidth * (index + 0.5f), height * 0.4f))
    }

    private fun night(date: LocalDate) = SleepScheduleDay(
        date = date,
        inBedStart = date.minusDays(1).atStartOfDay(ZONE).plusHours(23).toInstant(),
        inBedEnd = date.atStartOfDay(ZONE).plusHours(7).toInstant(),
    )

    private fun setSchedule(days: List<SleepScheduleDay>) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SleepScheduleStageChart(
                        title = TITLE,
                        summaryText = SUMMARY,
                        days = days,
                        sleepWindow = SleepWindow.Default,
                        selectedRange = TimeRange.WEEK,
                        period = DatePeriod(ANCHOR.minusDays(6), ANCHOR),
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    )
                }
            }
        }
    }

    private companion object {
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
        val ZONE: ZoneId = ZoneId.systemDefault()
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
        const val CHART = "sleep_schedule_chart"
        const val TITLE = "Sleep schedule"
        const val SUMMARY = "This week · 7h 20m"
    }
}
