package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.cycle.AgeBand
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.cycle.TrackingContext
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** Settings → Cycle: the cards that take plain callbacks. The reminders card drives a Hilt view model and is not here. */
class CycleSettingsCardsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contextsCard_reportsTheToggledContext() {
        var toggled: Pair<TrackingContext, Boolean>? = null
        setCard { CycleContextsCard(profile = CycleTrackingProfile(), onToggleContext = { context, on -> toggled = context to on }) }

        composeRule.onNodeWithText(string(R.string.cycle_context_pcos)).performScrollTo().performClick()

        assertEquals(TrackingContext.PCOS to true, toggled)
    }

    @Test
    fun ageBandCard_offersThePickerWithoutABirthYear() {
        var selected: AgeBand? = AgeBand.UNDER_20
        setCard { CycleAgeBandCard(selected = null, derived = null, onSelect = { selected = it }) }

        composeRule.onNodeWithText(string(R.string.cycle_age_band_30_34)).performScrollTo().performClick()
        assertEquals(AgeBand.AGE_30_34, selected)

        // Declining is an answer of its own.
        composeRule.onNodeWithText(string(R.string.cycle_age_band_none)).performScrollTo().performClick()
        assertEquals(null, selected)
    }

    @Test
    fun ageBandCard_showsTheDerivedBandInsteadOfThePicker() {
        setCard { CycleAgeBandCard(selected = null, derived = AgeBand.AGE_30_34, onSelect = {}) }

        composeRule.onNodeWithText(string(R.string.cycle_settings_age_band_from_body_profile_hint)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_age_band_none)).assertDoesNotExist()
    }

    @Test
    fun deleteJournalCard_asksBeforeDeleting() {
        var deleted = 0
        setCard { CycleDeleteJournalCard(isDeleting = false, onDelete = { deleted++ }) }

        composeRule.onNodeWithText(string(R.string.cycle_settings_delete_action)).performScrollTo().performClick()
        assertEquals(0, deleted)
        composeRule.onNodeWithText(string(R.string.cycle_settings_delete_confirm_title)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.action_delete)).performClick()
        assertEquals(1, deleted)
    }

    private fun setCard(content: @Composable () -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) { content() }
            }
        }
    }
}
