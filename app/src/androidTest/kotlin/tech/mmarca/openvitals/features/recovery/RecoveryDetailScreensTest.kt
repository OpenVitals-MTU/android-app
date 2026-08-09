package tech.mmarca.openvitals.features.recovery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.SleepScoreConfidence
import tech.mmarca.openvitals.domain.insights.SleepScoreEstimate
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Ports of Flutter's `sleep_score_detail_screen_test.dart` and
 * `sleep_efficiency_detail_screen_test.dart`.
 *
 * Both screens exist to explain a derived number rather than to report a
 * measurement, so the collapsible calculation card is the point of them: a user
 * who distrusts a score has nowhere else to look. Its default state matters too
 * — expanded by default would bury the score it is explaining.
 */
class RecoveryDetailScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theSleepScoreCalculationCardExpandsAndCollapses() {
        composeRule.setContent {
            OpenVitalsTheme {
                SleepScoreDetailContent(
                    day = emptyDay(),
                    unitFormatter = FORMATTER,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                )
            }
        }

        // Collapsed first: the explanation is available, not imposed.
        composeRule
            .onNodeWithText(string(R.string.action_show_calculation))
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText(string(R.string.action_hide_calculation))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.sleep_score_formula)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.action_hide_calculation)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.action_show_calculation)).assertIsDisplayed()
    }

    @Test
    fun aNoDataDaySleepScoreStillRendersItsCards() {
        // A night with nothing recorded still has to explain what the score
        // would have been made of, or the screen is blank with no reason given.
        composeRule.setContent {
            OpenVitalsTheme {
                SleepScoreDetailContent(
                    day = emptyDay(),
                    unitFormatter = FORMATTER,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.recovery_sleep_score)).assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.sleep_score_calculation_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun theSleepEfficiencyCalculationCardExpandsAndCollapses() {
        composeRule.setContent {
            OpenVitalsTheme {
                SleepEfficiencyDetailContent(
                    day = emptyDay(),
                    unitFormatter = FORMATTER,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                )
            }
        }

        composeRule
            .onNodeWithText(string(R.string.action_show_calculation))
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.sleep_efficiency_formula)).assertIsDisplayed()
    }

    @Test
    fun aNoDataDaySleepEfficiencyStillRendersItsCards() {
        composeRule.setContent {
            OpenVitalsTheme {
                SleepEfficiencyDetailContent(
                    day = emptyDay(),
                    unitFormatter = FORMATTER,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.recovery_sleep_efficiency)).assertIsDisplayed()
    }

    @Test
    fun theSleepScoreScreenRendersItsFourCardsFromAFixedEstimate() {
        // The estimate is injected rather than recomputed, so every number on
        // the screen is traceable to one field of it. That is the point: the
        // four cards are a chain — verdict, method, the day's figures, the
        // literature — and a screen that renders three of them explains a score
        // it never shows the workings for.
        composeRule.setContent {
            OpenVitalsTheme {
                SleepScoreDetailContent(
                    day = scoredDay(),
                    unitFormatter = FORMATTER,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                )
            }
        }

        // 1. Summary: the score, its confidence and the not-a-diagnosis note.
        composeRule.onNodeWithText(string(R.string.recovery_sleep_score)).assertIsDisplayed()
        composeRule.onNodeWithText(FORMATTER.count(ESTIMATE.score)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.sleep_score_confidence_high)).assertIsDisplayed()
        scrollTo(string(R.string.sleep_score_not_diagnostic))

        // 2. The method, collapsed behind its toggle.
        scrollTo(string(R.string.sleep_score_calculation_title))
        scrollTo(string(R.string.action_show_calculation))

        // 3. The day's figures: the scored pillars, quality breakdown, then the
        //    measurements they were derived from.
        scrollTo(string(R.string.sleep_score_day_numbers_title))
        scrollTo(string(R.string.sleep_score_component_duration))
        scrollTo(FORMATTER.decimal(ESTIMATE.durationPoints, 1))
        scrollTo(string(R.string.sleep_score_component_quality))
        scrollTo(FORMATTER.decimal(ESTIMATE.qualityPoints, 1))
        scrollTo(string(R.string.sleep_score_component_recovery))
        scrollTo(FORMATTER.decimal(ESTIMATE.recoveryPoints, 1))
        scrollTo(FORMATTER.decimal(ESTIMATE.efficiencyPoints, 1))
        scrollTo(string(R.string.sleep_score_component_continuity))
        scrollTo(string(R.string.sleep_score_component_stages))
        scrollTo(FORMATTER.duration(SLEEP_MINUTES.minutesAsMillis()))
        scrollTo(FORMATTER.duration(TIME_IN_BED_MINUTES.minutesAsMillis()))
        scrollTo(FORMATTER.percent(ESTIMATE.sleepEfficiencyPercent, 0).value)
        scrollTo(string(R.string.sleep_score_baseline_nights))
        scrollTo(string(R.string.sleep_score_stage_records))
        scrollTo(string(R.string.recovery_sleep_schedule))
        // Stages, awake stages, and overnight HRV: the richest quality note.
        scrollTo(string(R.string.sleep_score_quality_stage_awake_hrv))

        // 4. Science references behind the score.
        scrollTo(string(R.string.references_backed_links))
        scrollTo(string(R.string.reference_garmin_sleep_score))
        scrollTo(string(R.string.reference_nsf_sleep_duration))
        scrollTo(string(R.string.reference_aasm_sleep_duration))
        scrollTo(string(R.string.reference_nsf_sleep_quality))
        scrollTo(string(R.string.reference_multidimensional_sleep_health))
        scrollTo(string(R.string.reference_sleep_efficiency_definition))
    }

    @Test
    fun theSleepEfficiencyScreenRendersItsFourCardsFromAFixedEstimate() {
        composeRule.setContent {
            OpenVitalsTheme {
                SleepEfficiencyDetailContent(
                    day = scoredDay(),
                    unitFormatter = FORMATTER,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                )
            }
        }

        // 1. Summary: the efficiency headline rather than the score, its
        //    confidence and the note.
        composeRule.onNodeWithText(string(R.string.recovery_sleep_efficiency)).assertIsDisplayed()
        composeRule
            .onAllNodesWithText(FORMATTER.percent(ESTIMATE.sleepEfficiencyPercent, 0).value)
            .onFirst()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.sleep_score_confidence_high)).assertIsDisplayed()
        scrollTo(string(R.string.sleep_efficiency_not_diagnostic))

        // 2. The method, collapsed behind its toggle.
        scrollTo(string(R.string.sleep_efficiency_calculation_title))
        scrollTo(string(R.string.action_show_calculation))

        // 3. The day's figures — the two durations the ratio is made of, the
        //    wake time it subtracts, and the records behind them.
        scrollTo(string(R.string.sleep_efficiency_day_numbers_title))
        scrollTo(string(R.string.sleep_score_efficiency))
        scrollTo(FORMATTER.duration(SLEEP_MINUTES.minutesAsMillis()))
        scrollTo(FORMATTER.duration(TIME_IN_BED_MINUTES.minutesAsMillis()))
        scrollTo(string(R.string.sleep_score_waso))
        scrollTo(string(R.string.recovery_sleep_schedule))
        scrollTo(string(R.string.sleep_score_stage_records))

        // 4. Three references here, not the score screen's four.
        scrollTo(string(R.string.references_backed_links))
        scrollTo(string(R.string.reference_sleep_efficiency_definition))
        scrollTo(string(R.string.reference_sleep_efficiency_denominator))
        scrollTo(string(R.string.reference_sleep_assessment_methods))
    }

    /** Scrolls the lazy list to [text] and asserts it arrived on screen. */
    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
        composeRule.onAllNodesWithText(text).onFirst().assertIsDisplayed()
    }

    private fun emptyDay() = RecoveryDay(date = DAY)

    private fun scoredDay(): RecoveryDay {
        val end = DAY.atTime(6, 30).atZone(ZoneId.systemDefault()).toInstant()
        return RecoveryDay(
            date = DAY,
            sessions = listOf(
                SleepData(
                    id = "s1",
                    startTime = end.minusSeconds(8 * 3600),
                    endTime = end,
                    durationMs = 8 * 3_600_000L,
                    source = "com.test.tracker",
                ),
            ),
            sleepScore = ESTIMATE,
        )
    }

    private companion object {
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        /** A fixed past night, so the day title never becomes "Today". */
        val DAY: LocalDate = LocalDate.of(2026, 6, 23)

        const val SLEEP_MINUTES = 450.0
        const val TIME_IN_BED_MINUTES = 480.0

        val ESTIMATE = SleepScoreEstimate(
            score = 82,
            confidence = SleepScoreConfidence.HIGH,
            durationPoints = 30.0,
            qualityPoints = 36.0,
            recoveryPoints = 16.0,
            efficiencyPoints = 15.0,
            continuityPoints = 12.0,
            stageBalancePoints = 9.0,
            sleepDurationMinutes = SLEEP_MINUTES,
            timeInBedMinutes = TIME_IN_BED_MINUTES,
            sleepEfficiencyPercent = 93.75,
            wakeAfterSleepOnsetMinutes = 20.0,
            deepSleepPercentOfSleep = 18.0,
            remSleepPercentOfSleep = 22.0,
            overnightHrvRmssdMs = 48.0,
            overnightHrvBaselineRmssdMs = 45.0,
            regularityDifferenceMinutes = 15.0,
            regularityBaselineNights = 5,
            sleepStageCount = 12,
            usesSleepStages = true,
            usesExplicitAwakeStages = true,
            usesOvernightHrv = true,
        )

        fun Double.minutesAsMillis(): Long = (this * 60_000).roundToLong()
    }
}
