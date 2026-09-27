package tech.mmarca.openvitals.features.manualentry.cycle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** The day log card: the bleeding scale, the optional sections, and the permission callout. */
class CycleEntryContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setCard(
        state: CycleEntryUiState,
        actions: CycleEntryActions = CycleEntryActions.None,
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())) {
                    CycleEntryCard(
                        state = state,
                        unitSystem = UnitSystem.METRIC,
                        actions = actions,
                        onSave = {},
                        onRequestWritePermission = {},
                    )
                }
            }
        }
    }

    private fun readyState() = CycleEntryUiState(
        isCheckingPermission = false,
        isLoadingDay = false,
        grantedKinds = CycleEntryKind.entries.toSet(),
    )

    @Test
    fun theBleedingScaleOffersEveryStepAndReportsTheTap() {
        var selected: BleedingOption? = null
        setCard(readyState(), CycleEntryActions.None.copy(onBleeding = { selected = it }))

        composeRule.onNodeWithText(string(R.string.cycle_bleeding_not_recorded)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_bleeding_none)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_entry_section_spotting)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_flow_heavy)).performClick()

        assertEquals(BleedingOption.HEAVY, selected)
    }

    @Test
    fun theOptionalSectionsStayCollapsedUntilOpened() {
        setCard(readyState())

        composeRule.onNodeWithText(string(R.string.cycle_entry_more_show)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_entry_biomarkers_show)).performScrollTo().assertIsDisplayed()

        setCard(readyState().copy(showMore = true, showBiomarkers = true))

        composeRule.onNodeWithText(string(R.string.cycle_entry_symptoms)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_observation_basal_body_temperature)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_entry_section_hcg)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun withoutAnyWritePermissionTheCalloutShowsAndSaveStillWorksForTheJournal() {
        setCard(readyState().copy(grantedKinds = emptySet()))

        composeRule.onNodeWithText(string(R.string.cycle_entry_permission_needed)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_entry_save)).performScrollTo().assertIsDisplayed()
    }
}
