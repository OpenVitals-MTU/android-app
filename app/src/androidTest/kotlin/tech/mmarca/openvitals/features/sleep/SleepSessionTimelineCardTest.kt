package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Ports the timeline-card half of Flutter's `sleep_stage_share_card_test.dart`.
 *
 * The day view's night card is the one place the SHAPE of a night is on show —
 * when the deep sleep happened, not merely how much of it there was. Reduced to a
 * proportional strip it would only restate the breakdown card underneath it.
 */
class SleepSessionTimelineCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theNightIsDrawnAsALaneChartAndNotAFlatBar() {
        // One lane per stage, stacked: Deep must sit below Light, at the time it
        // happened. A single strip of segments laid end to end would still show
        // every stage name and every duration, and be worth nothing.
        setCard(onClick = null)

        composeRule.onNodeWithText(string(R.string.sleep_stage_light)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.sleep_stage_deep)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.sleep_stage_rem)).assertIsDisplayed()

        val awakeTop = composeRule
            .onNodeWithText(string(R.string.sleep_stage_awake))
            .getUnclippedBoundsInRoot()
            .top
        val deepTop = composeRule
            .onNodeWithText(string(R.string.sleep_stage_deep))
            .getUnclippedBoundsInRoot()
            .top

        assertTrue(
            "the stage lanes must be stacked down the card, not laid out in a row",
            deepTop - awakeTop > 100.dp,
        )
    }

    @Test
    fun tappingTheCardOpensThatNightAndSaysSoFirst() {
        // The card is the way into the detail screen, and the tap has to survive
        // the hypnogram's own scrub gesture sitting directly under the finger.
        var opened = 0
        setCard(onClick = { opened++ })

        composeRule.onNodeWithText(string(R.string.action_details)).assertIsDisplayed()

        composeRule.onNodeWithTag(CARD).performClick()
        composeRule.runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun aMergedNightOffersNoDetailToOpen() {
        // Two sessions in one night are shown as a single merged summary whose id
        // belongs to no record. There is nothing to open, so the card must not
        // advertise a way in that would dead-end.
        setCard(onClick = null)

        composeRule.onNodeWithText(string(R.string.action_details)).assertDoesNotExist()
    }

    private fun setCard(onClick: (() -> Unit)?) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SleepSessionTimelineCard(
                        session = SESSION,
                        selectedDate = NIGHT_DATE,
                        unitFormatter = FORMATTER,
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        onClick = onClick,
                        modifier = Modifier.testTag(CARD),
                        timeRangeText = "01:09 - 04:36",
                    )
                }
            }
        }
    }

    private companion object {
        const val CARD = "sleep_session_timeline_card"

        val ZONE: ZoneId = ZoneId.systemDefault()

        /** A fixed past night: 01:09 to 04:36, fully covered by contiguous stages. */
        val NIGHT_DATE: LocalDate = LocalDate.of(2026, 7, 12)
        val NIGHT_START: Instant =
            LocalDateTime.of(2026, 7, 12, 1, 9).atZone(ZONE).toInstant()

        val SESSION = SleepData(
            id = "s1",
            startTime = NIGHT_START,
            endTime = NIGHT_START.plusSeconds(207 * 60L),
            durationMs = 207 * 60_000L,
            source = "com.test.tracker",
            stages = listOf(
                stage(SleepStage.STAGE_LIGHT, 0, 106),
                stage(SleepStage.STAGE_DEEP, 106, 159),
                stage(SleepStage.STAGE_REM, 159, 207),
            ),
        )

        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        private fun stage(stageType: Int, startMinute: Int, endMinute: Int) = SleepStage(
            startTime = NIGHT_START.plusSeconds(startMinute * 60L),
            endTime = NIGHT_START.plusSeconds(endMinute * 60L),
            stageType = stageType,
        )
    }
}
