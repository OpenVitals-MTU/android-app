package tech.mmarca.openvitals.features.manualentry.activity.recording

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import java.time.Duration
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.BleHeartRateSample
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.testing.testUnitFormatter
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of Flutter's
 * `test/features/manualentry/activity/recording/activity_recorded_sensor_summary_test.dart`.
 *
 * This is the only look a user gets at what the strap actually recorded before
 * the session is written to Health Connect. The average, the range and the
 * sample count are the numbers they would use to decide the recording is
 * trustworthy — and the elapsed axis has to span the SESSION, not the samples,
 * or a strap that dropped out for the last ten minutes looks like a shorter ride.
 */
class ActivityRecordedSensorSummaryTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTheHeartRateCardWithItsAverageRangeAndSampleCount() {
        val formatter = testUnitFormatter()
        setSummary(samples = buffer(), sessionStart = SESSION_START, sessionEnd = SESSION_END)

        composeRule.onNodeWithText(string(R.string.activity_recording_live_heart_rate)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.summary_average)).performScrollTo().assertIsDisplayed()
        // 120..149 across thirty samples averages 134.5, which rounds to 135.
        // The y-axis names the same value at its midpoint, so this is not unique.
        composeRule.onAllNodesWithText(formatter.heartRate(135).text).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.summary_range)).performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithText("${formatter.heartRate(120).text}-${formatter.heartRate(149).text}")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.summary_samples)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(formatter.count(30)).performScrollTo().assertIsDisplayed()

        // The elapsed axis spans the whole hour the session lasted, even though
        // the last sample landed two minutes short of it.
        composeRule.onNodeWithText("0:00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1:00:00").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun withoutASessionRangeTheAxisFallsBackToTheSamples() {
        // A draft that has lost its session bounds still has to plot something
        // honest: the span the samples themselves cover, not a guessed hour.
        setSummary(samples = buffer(), sessionStart = null, sessionEnd = null)

        // Samples run 0, 2, ... 58 minutes, so the axis ends at 58:00.
        composeRule.onNodeWithText("58:00").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun withNoSamplesAtAllThereIsNoCard() {
        // An empty chart claiming a heart rate would be worse than no chart.
        setSummary(samples = BleRecordingSampleBuffer(), sessionStart = SESSION_START, sessionEnd = SESSION_END)

        composeRule.onNodeWithText(string(R.string.activity_recording_live_heart_rate)).assertDoesNotExist()
    }

    private fun buffer(): BleRecordingSampleBuffer = BleRecordingSampleBuffer(
        heartRateSamples = List(30) { minute ->
            BleHeartRateSample(
                time = SESSION_START.plus(Duration.ofMinutes(minute * 2L)),
                beatsPerMinute = 120L + minute,
            )
        },
    )

    private fun setSummary(
        samples: BleRecordingSampleBuffer,
        sessionStart: Instant?,
        sessionEnd: Instant?,
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ActivityRecordedSensorSummary(
                        samples = samples,
                        unitFormatter = testUnitFormatter(),
                        sessionStart = sessionStart,
                        sessionEnd = sessionEnd,
                    )
                }
            }
        }
    }

    private companion object {
        val SESSION_START: Instant = Instant.parse("2026-06-01T08:00:00Z")
        val SESSION_END: Instant = Instant.parse("2026-06-01T09:00:00Z")
    }
}
