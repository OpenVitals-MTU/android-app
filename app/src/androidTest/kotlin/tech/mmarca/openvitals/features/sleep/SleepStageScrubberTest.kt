package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * When the scrub readout appears, and whether the chart hoards the page's gestures.
 * The lane labels read "Light - 4h 10m", so a bare stage name can only come from the tooltip.
 */
class SleepStageScrubberTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theHypnogramStaysSilentUntilAFingerIsOnIt() {
        // A crosshair drawn before anyone touched the chart would sit permanently over the night.
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Hypnogram()
                }
            }
        }

        composeRule.onNodeWithText(LIGHT).assertDoesNotExist()

        val chart = composeRule.onNodeWithTag(CHART)
        chart.performTouchInput { down(Offset(width * 0.5f, height * 0.3f)) }
        chart.performTouchInput { moveTo(Offset(width * 0.25f, height * 0.3f)) }
        composeRule.waitForIdle()

        // A quarter into this night is 01:12, inside the first Light block.
        composeRule.onNodeWithText(LIGHT).assertIsDisplayed()

        chart.performTouchInput { up() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(LIGHT).assertDoesNotExist()
    }

    @Test
    fun aVerticalDragStartingOnTheHypnogramStillScrollsThePage() {
        // If the chart swallowed vertical drags the page would be stuck.
        lateinit var scrollState: ScrollState
        composeRule.setContent {
            OpenVitalsTheme {
                scrollState = rememberScrollState()
                Column(Modifier.verticalScroll(scrollState)) {
                    Hypnogram()
                    Spacer(Modifier.height(2000.dp))
                }
            }
        }

        composeRule.runOnIdle { assertTrue("the page starts unscrolled", scrollState.value == 0) }

        composeRule.onNodeWithTag(CHART).performTouchInput {
            swipeUp(startY = height * 0.6f, endY = height * 0.1f)
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertTrue("a vertical drag must be left to the scrolling page", scrollState.value > 0)
        }
        // And it did not scrub on the way past.
        composeRule.onNodeWithText(LIGHT).assertDoesNotExist()
    }

    @Composable
    private fun Hypnogram() {
        SleepStagesLaneChart(
            stages = NIGHT,
            unitFormatter = FORMATTER,
            timeFormatter = DateTimeFormatterProvider().shortTime(),
            modifier = Modifier.testTag(CHART),
            timelineStart = BEDTIME,
            timelineEnd = WAKE_UP,
        )
    }

    private companion object {
        const val CHART = "sleep_hypnogram"

        val ZONE: ZoneId = ZoneId.systemDefault()

        /** A fixed night that straddles midnight: 470 minutes from 23:15 to 07:05. */
        val BEDTIME: Instant = LocalDateTime.of(2026, 6, 21, 23, 15).atZone(ZONE).toInstant()
        val WAKE_UP: Instant = LocalDateTime.of(2026, 6, 22, 7, 5).atZone(ZONE).toInstant()

        val NIGHT: List<SleepStage> = listOf(
            stage(SleepStage.STAGE_AWAKE, 0, 10),
            stage(SleepStage.STAGE_LIGHT, 10, 145),
            stage(SleepStage.STAGE_DEEP, 145, 275),
            stage(SleepStage.STAGE_REM, 275, 355),
            stage(SleepStage.STAGE_LIGHT, 355, 470),
        )

        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        val LIGHT: String get() = string(R.string.sleep_stage_light)

        private fun stage(stageType: Int, startMinute: Int, endMinute: Int) = SleepStage(
            startTime = BEDTIME.plusSeconds(startMinute * 60L),
            endTime = BEDTIME.plusSeconds(endMinute * 60L),
            stageType = stageType,
        )
    }
}
