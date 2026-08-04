package tech.mmarca.openvitals.features.manualentry.activity.recording

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryType
import tech.mmarca.openvitals.features.manualentry.activity.activityEntryTypeById
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Ports the sensor-readiness half of the `setup screen` group of Flutter's
 * `test/features/manualentry/activity/recording/activity_recording_screen_test.dart`.
 *
 * Live rep counting depends on hardware not every phone has. A device without a
 * proximity sensor cannot count push-ups at all, and the difference between
 * saying so here and letting the user start anyway is the difference between a
 * known limitation and a workout that silently records zero reps. The panel is
 * also where a user learns where to put the phone, which is what makes the
 * count work in the first place.
 */
class RecordingGuidancePanelTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aMissingSensorIsNamedAndManualEntryIsOfferedInstead() {
        setPanel(
            activityType = requireType("push_ups"),
            readiness = RecordingSensorReadiness(
                hasRequiredSensor = false,
                hasActivityRecognitionPermission = true,
            ),
        )

        composeRule.onNodeWithText(string(R.string.activity_recording_how_it_works)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_recording_guidance_push_ups)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_recording_sensor_unavailable_manual)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_recording_sensor_ready)).assertDoesNotExist()
    }

    @Test
    fun aRepActivityWithItsSensorReportsReady() {
        setPanel(
            activityType = requireType("pull_ups"),
            readiness = RecordingSensorReadiness(
                hasRequiredSensor = true,
                hasActivityRecognitionPermission = true,
            ),
        )

        composeRule.onNodeWithText(string(R.string.activity_recording_guidance_pull_ups)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_recording_sensor_ready)).assertIsDisplayed()
    }

    @Test
    fun aStepCountedActivityWithoutActivityRecognitionSaysWhatIsMissing() {
        // The step detector is there, but Android will not hand its events over
        // without the permission — so "ready" would be a lie.
        setPanel(
            activityType = requireType("treadmill"),
            readiness = RecordingSensorReadiness(
                hasRequiredSensor = true,
                hasActivityRecognitionPermission = false,
            ),
        )

        composeRule
            .onNodeWithText(string(R.string.activity_recording_activity_recognition_missing))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.activity_recording_sensor_ready)).assertDoesNotExist()
    }

    private fun requireType(id: String): ActivityEntryType =
        checkNotNull(activityEntryTypeById(id)) { "no activity entry type with id $id" }

    private fun setPanel(activityType: ActivityEntryType, readiness: RecordingSensorReadiness) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    RecordingGuidancePanel(
                        activityType = activityType,
                        sensorReadiness = readiness,
                    )
                }
            }
        }
    }
}
