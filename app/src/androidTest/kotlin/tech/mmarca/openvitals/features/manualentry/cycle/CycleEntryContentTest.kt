package tech.mmarca.openvitals.features.manualentry.cycle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The cycle day-log is one screen with a section per Health Connect record
 * type; a nightly log is a couple of chip taps and one save. Create mode must
 * show every section, edit mode only the record being edited — a scoped edit
 * that rendered the other sections would silently write records the user
 * never meant to touch.
 */
class CycleEntryContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setCard(state: CycleEntryUiState, onSelectFlow: (Int?) -> Unit = {}) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    CycleEntryCard(
                        state = state,
                        unitSystem = UnitSystem.METRIC,
                        onDateChanged = {},
                        onEntryTimeChanged = {},
                        onSelectFlow = onSelectFlow,
                        onToggleSpotting = {},
                        onSelectSexualActivity = {},
                        onSelectOvulation = {},
                        onSelectMucusAppearance = {},
                        onSelectMucusSensation = {},
                        onBbtInputChanged = {},
                        onSelectBbtLocation = {},
                        onSave = {},
                        onRequestWritePermission = {},
                    )
                }
            }
        }
    }

    @Test
    fun createModeShowsAllSixSections() {
        setCard(
            CycleEntryUiState(
                isCheckingPermission = false,
                grantedKinds = CycleEntryKind.entries.toSet(),
            )
        )

        composeRule.onNodeWithText(string(R.string.cycle_entry_section_flow))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_entry_section_spotting))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_entry_section_sexual_activity))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_entry_section_ovulation))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_entry_section_mucus_appearance))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_entry_section_mucus_sensation))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_entry_bbt_location))
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tappingAFlowChipReportsTheSelection() {
        var selected: Int? = null
        setCard(
            CycleEntryUiState(
                isCheckingPermission = false,
                grantedKinds = CycleEntryKind.entries.toSet(),
            ),
            onSelectFlow = { selected = it },
        )

        composeRule.onNodeWithText(string(R.string.cycle_flow_medium))
            .performScrollTo().performClick()

        assertEquals(CycleRecordValues.FLOW_MEDIUM, selected)
    }

    @Test
    fun missingPermissionShowsTheGrantAffordance() {
        setCard(
            CycleEntryUiState(
                isCheckingPermission = false,
                grantedKinds = emptySet(),
            )
        )

        composeRule.onNodeWithText(string(R.string.cycle_entry_permission_needed))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_grant))
            .assertIsDisplayed()
    }

    @Test
    fun editModeRendersOnlyTheScopedSection() {
        setCard(
            CycleEntryUiState(
                isCheckingPermission = false,
                grantedKinds = CycleEntryKind.entries.toSet(),
                editKind = CycleEntryKind.OVULATION_TEST,
                editRecordId = "uid",
            )
        )

        composeRule.onNodeWithText(string(R.string.cycle_entry_section_ovulation))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_entry_section_flow))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.cycle_entry_section_spotting))
            .assertDoesNotExist()
    }
}
