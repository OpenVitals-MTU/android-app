package tech.mmarca.openvitals.features.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** The stride field is dead while the feature is off, and the save lands in meters whatever the field displays. */
class StepDistanceBackfillCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setCard(
        enabled: Boolean,
        strideLengthMeters: Double = 0.7,
        unitSystem: UnitSystem = UnitSystem.METRIC,
        onSave: (Boolean, Double) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                StepDistanceBackfillCard(
                    enabled = enabled,
                    strideLengthMeters = strideLengthMeters,
                    unitSystem = unitSystem,
                    onSave = onSave,
                )
            }
        }
    }

    @Test
    fun strideFieldIsDeadWhileTheToggleIsOff() {
        setCard(enabled = false)

        composeRule.onNodeWithText(string(R.string.settings_step_distance_stride_label))
            .assertIsNotEnabled()
    }

    @Test
    fun savingRoundTripsCentimetersToMeters() {
        var savedEnabled = false
        var savedMeters = 0.0
        setCard(enabled = true, onSave = { enabled, meters ->
            savedEnabled = enabled
            savedMeters = meters
        })

        composeRule.onNodeWithText(string(R.string.settings_step_distance_stride_label))
            .performTextClearance()
        composeRule.onNodeWithText(string(R.string.settings_step_distance_stride_label))
            .performTextInput("75")
        composeRule.onNodeWithText(string(R.string.action_save))
            .performClick()

        assertTrue(savedEnabled)
        assertEquals(0.75, savedMeters, 1e-6)
    }

    @Test
    fun imperialShowsInches() {
        setCard(enabled = true, strideLengthMeters = 0.762, unitSystem = UnitSystem.IMPERIAL)

        composeRule.onNodeWithText("in").assertIsDisplayed()
        composeRule.onNodeWithText("30.0").assertIsDisplayed()
    }
}
