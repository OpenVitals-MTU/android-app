package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The card holds a draft and commits on Save: what the fields start at, what the labels say
 * about where a value came from, and what Save emits. Heart zones live in `BodyEnergyCalibrationCard`.
 */
class BodyProfileCardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun seedsItsFieldsFromTheStoredProfile() {
        setCard(STORED)

        composeRule.onNodeWithText("1990").assertIsDisplayed()
        composeRule.onNodeWithText("72.5").assertIsDisplayed()
        composeRule.onNodeWithText("178.0").assertIsDisplayed()
        composeRule.onNodeWithText("52").assertIsDisplayed()
        composeRule.onNodeWithText("188").assertIsDisplayed()
    }

    @Test
    fun showsWhereAMeasuredValueCameFrom() {
        // A measured weight is labelled differently from a typed one.
        setCard(STORED, weightMeasured = true)

        composeRule
            .onNodeWithText(
                string(R.string.settings_body_profile_measured_label, string(R.string.settings_body_profile_weight)),
            )
            .assertIsDisplayed()
        // Height was not measured, so it keeps the plain label.
        composeRule.onNodeWithText(string(R.string.settings_body_profile_height)).assertIsDisplayed()
    }

    @Test
    fun editingAFieldAndSavingEmitsTheUpdatedProfile() {
        var saved: BodyProfile? = null
        setCard(STORED, onSave = { saved = it })

        composeRule.onNodeWithText("72.5").performTextClearance()
        composeRule.onNodeWithText(string(R.string.settings_body_profile_weight)).performTextInput("80.0")
        composeRule.onNodeWithText(string(R.string.action_save)).performClick()
        composeRule.waitForIdle()

        assertEquals(80.0, saved?.weightKg)
        // The rest of the profile rides along untouched.
        assertEquals(1990, saved?.birthYear)
        assertEquals(52, saved?.restingHeartRateBpm)
    }

    @Test
    fun theBirthYearIsNotAskedForTwice() {
        // Both cards once carried a birth year and disagreed about it.
        setCard(STORED)

        composeRule
            .onAllNodesWithText(string(R.string.body_energy_calibration_birth_year))
            .assertCountEquals(1)
    }

    @Test
    fun standaloneItStillCarriesABirthYearAndASave() {
        // This card on its own has to be enough to complete a profile.
        setCard(BodyProfile())

        composeRule.onNodeWithText(string(R.string.body_energy_calibration_birth_year)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_save)).assertIsDisplayed()
    }

    private fun setCard(
        profile: BodyProfile,
        weightMeasured: Boolean = false,
        onSave: (BodyProfile) -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    BodyProfileCard(
                        profile = profile,
                        unitSystem = UnitSystem.METRIC,
                        onSave = onSave,
                        weightMeasured = weightMeasured,
                    )
                }
            }
        }
    }

    private companion object {
        val STORED = BodyProfile(
            birthYear = 1990,
            weightKg = 72.5,
            heightCm = 178.0,
            restingHeartRateBpm = 52,
            maxHeartRateBpm = 188,
        )
    }
}
