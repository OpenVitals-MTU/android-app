package tech.mmarca.openvitals.features.manualentry.cycle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
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
 * The cycle day-log shows one section at a time. Edit mode scopes to the record being edited,
 * or it would write records the user never meant to touch.
 */
class CycleEntryContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setCard(
        state: CycleEntryUiState,
        onSelectFlow: (Int?) -> Unit = {},
        onSelectSection: (CycleEntryKind) -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    CycleEntryCard(
                        state = state,
                        unitSystem = UnitSystem.METRIC,
                        onDateChanged = {},
                        onEntryTimeChanged = {},
                        onSelectSection = onSelectSection,
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
    fun createModeShowsTheCategoryPickerAndOnlyTheSelectedSection() {
        setCard(
            CycleEntryUiState(
                isCheckingPermission = false,
                grantedKinds = CycleEntryKind.entries.toSet(),
            )
        )

        // All six categories are offered...
        composeRule.onNodeWithText(string(R.string.cycle_observation_intermenstrual_bleeding))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_observation_sexual_activity))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_observation_ovulation_test))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_observation_cervical_mucus))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_observation_basal_body_temperature))
            .performScrollTo().assertIsDisplayed()
        // Only the selected category's input renders, as a collapsed dropdown. Exactly one.
        composeRule.onAllNodesWithText(string(R.string.option_not_specified))
            .assertCountEquals(1)
        composeRule.onNodeWithText(string(R.string.cycle_entry_section_spotting))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.cycle_entry_section_mucus_appearance))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.cycle_entry_bbt_location))
            .assertDoesNotExist()
    }

    @Test
    fun tappingACategoryChipReportsTheSelection() {
        var selected: CycleEntryKind? = null
        setCard(
            CycleEntryUiState(
                isCheckingPermission = false,
                grantedKinds = CycleEntryKind.entries.toSet(),
            ),
            onSelectSection = { selected = it },
        )

        composeRule.onNodeWithText(string(R.string.cycle_observation_ovulation_test))
            .performScrollTo().performClick()

        assertEquals(CycleEntryKind.OVULATION_TEST, selected)
    }

    @Test
    fun aSelectedCategoryRendersItsOwnSection() {
        setCard(
            CycleEntryUiState(
                isCheckingPermission = false,
                grantedKinds = CycleEntryKind.entries.toSet(),
                selectedSection = CycleEntryKind.BASAL_BODY_TEMPERATURE,
            )
        )

        composeRule.onNodeWithText(string(R.string.cycle_entry_bbt_location))
            .performScrollTo().assertIsDisplayed()
        // "Period flow" appears once, as the chip. If the flow section rendered too, it would be two.
        composeRule.onAllNodesWithText(string(R.string.cycle_entry_section_flow))
            .assertCountEquals(1)
    }

    @Test
    fun pickingAFlowOptionFromTheDropdownReportsTheSelection() {
        var selected: Int? = null
        setCard(
            CycleEntryUiState(
                isCheckingPermission = false,
                grantedKinds = CycleEntryKind.entries.toSet(),
            ),
            onSelectFlow = { selected = it },
        )

        composeRule.onNodeWithText(string(R.string.option_not_specified))
            .performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.cycle_flow_medium)).performClick()

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
        composeRule.onNodeWithText(string(R.string.action_grant_permission))
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
        // No category picker while editing: the record's kind is not a choice.
        composeRule.onNodeWithText(string(R.string.cycle_observation_basal_body_temperature))
            .assertDoesNotExist()
    }
}
