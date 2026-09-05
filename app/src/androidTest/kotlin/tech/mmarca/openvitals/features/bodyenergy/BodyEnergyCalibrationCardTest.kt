package tech.mmarca.openvitals.features.bodyenergy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Automatic zones need an age. Without one the model falls back to resting + 70, which reads
 * a brisk walk as zone 5, so Save must refuse loudly.
 */
class BodyEnergyCalibrationCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun setupRefusesToCompleteWithoutABirthYear() {
        var saved: Pair<BodyEnergyCalibration, Int?>? = null
        setCard { calibration, birthYear -> saved = calibration to birthYear }

        composeRule.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertNull("setup must not complete without an age to derive zones from", saved)
        composeRule
            .onNodeWithText(string(R.string.body_energy_calibration_birth_year_required))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun aBirthYearLetsSetupComplete() {
        // With an age, Save goes through and carries the year.
        var saved: Pair<BodyEnergyCalibration, Int?>? = null
        setCard { calibration, birthYear -> saved = calibration to birthYear }

        // The birth year is the only text field while automatic zones are selected.
        composeRule.onNode(hasSetTextAction()).performScrollTo().performTextInput("1990")
        composeRule.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(1990, saved?.second)
    }

    @Test
    fun manualZonesNeedNoBirthYearAtAll() {
        // Typed zones are the ladder, so no age is needed.
        var saved: Pair<BodyEnergyCalibration, Int?>? = null
        setCard { calibration, birthYear -> saved = calibration to birthYear }

        composeRule.onNode(isToggleable()).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertNotNull("manual zones need no age to derive a ladder from", saved)
        composeRule
            .onNodeWithText(string(R.string.body_energy_calibration_birth_year_required))
            .assertDoesNotExist()
    }

    @Test
    fun togglingManualZonesRevealsTheFiveZoneFields() {
        // The ladder is five thresholds. Hidden while automatic zones are selected, all five together otherwise.
        setCard { _, _ -> }

        ZONE_LABELS.forEach { label ->
            composeRule.onNodeWithText(string(label)).assertDoesNotExist()
        }

        composeRule.onNode(isToggleable()).performScrollTo().performClick()
        composeRule.waitForIdle()

        ZONE_LABELS.forEach { label ->
            composeRule.onNodeWithText(string(label)).performScrollTo().assertIsDisplayed()
        }
    }

    private fun setCard(onSave: (BodyEnergyCalibration, Int?) -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    BodyEnergyCalibrationCard(
                        calibration = BodyEnergyCalibration(),
                        bodyProfile = BodyProfile(),
                        onSave = onSave,
                        onResetPersonalTuning = {},
                    )
                }
            }
        }
    }

    private companion object {
        val ZONE_LABELS = listOf(
            R.string.body_energy_calibration_zone_1,
            R.string.body_energy_calibration_zone_2,
            R.string.body_energy_calibration_zone_3,
            R.string.body_energy_calibration_zone_4,
            R.string.body_energy_calibration_zone_5,
        )
    }
}
