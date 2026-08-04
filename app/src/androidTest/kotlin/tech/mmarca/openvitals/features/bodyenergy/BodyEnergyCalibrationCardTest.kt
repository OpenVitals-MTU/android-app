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
 * Ports the setup-gate cases of Flutter's
 * `test/features/bodyenergy/body_energy_details_screen_test.dart`.
 *
 * Automatic heart zones are Tanaka against age, so with no birth year the model
 * falls back to resting + 70 — for a resting 60 that claims a maximum of 130 and
 * reads a brisk walk as zone 5. A score built on that is not rougher, it is
 * wrong, and the user has no way to tell. So Save refusing is the behaviour
 * worth pinning, and it has to refuse loudly enough that the user knows why.
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
        // The other half of the gate: once there is an age to derive zones from,
        // Save goes through and carries the year with it — the screen has no
        // other way to learn that setup is done.
        var saved: Pair<BodyEnergyCalibration, Int?>? = null
        setCard { calibration, birthYear -> saved = calibration to birthYear }

        // The birth year is the only text field on the card while automatic
        // zones are selected.
        composeRule.onNode(hasSetTextAction()).performScrollTo().performTextInput("1990")
        composeRule.onNodeWithText(string(R.string.action_save)).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(1990, saved?.second)
    }

    @Test
    fun manualZonesNeedNoBirthYearAtAll() {
        // Typed zones ARE the ladder, so there is nothing left to derive from an
        // age. Demanding one anyway would block the one route a user without a
        // birth year has to a trustworthy score.
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
        // Port of `BodyEnergyCalibrationCard > toggling manual zones reveals the
        // five zone fields` from Flutter's `body_settings_cards_test.dart`.
        //
        // The ladder is five thresholds, not one: showing some of them would let
        // a user save a partial ladder and get a score built on zones they never
        // set. Hidden while automatic zones are selected, all five together the
        // moment they are not.
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
