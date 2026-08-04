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
 * Port of the `BodyProfileCard` cases of Flutter's
 * `test/features/settings/body_settings_cards_test.dart`.
 *
 * The card holds a draft and only commits it on Save, so what is worth pinning
 * is the seam between the stored profile and that draft: what the fields start
 * at, what the labels say about where a value came from, and what Save emits.
 *
 * The Flutter file's heart-zone cases are not here. Kotlin keeps zones in
 * `BodyEnergyCalibrationCard` rather than inside this card, so they are a
 * layout difference rather than an unported case, and the matrix records them
 * as such.
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
        // A weight Health Connect actually recorded is labelled differently
        // from one the user typed, so an unedited profile is not mistaken for
        // a measurement.
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
        // The rest of the profile rides along untouched rather than being
        // reset to whatever the edited field's siblings happened to render as.
        assertEquals(1990, saved?.birthYear)
        assertEquals(52, saved?.restingHeartRateBpm)
    }

    @Test
    fun theBirthYearIsNotAskedForTwice() {
        // Both this card and the calibration card once carried a birth year,
        // which left the Body profile screen asking for the same fact twice
        // and disagreeing about it.
        setCard(STORED)

        composeRule
            .onAllNodesWithText(string(R.string.body_energy_calibration_birth_year))
            .assertCountEquals(1)
    }

    @Test
    fun standaloneItStillCarriesABirthYearAndASave() {
        // Whatever else the Body profile screen shows around it, this card on
        // its own has to be enough to complete a profile.
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
