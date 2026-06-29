package tech.mmarca.openvitals.features.manualentry.hydration

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

class HydrationEntryFormTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hydrationEntryForm_rendersTrackerCard() {
        val state = HydrationEntryUiState(
            isCheckingPermission = false,
            canWriteHydration = true,
        )
        val unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        composeRule.setContent {
            OpenVitalsTheme {
                HydrationTrackerCard(
                    state = state,
                    unitFormatter = unitFormatter,
                    onSelectBeverage = {},
                    onSelectContainer = {},
                    onAddContainerEntry = {},
                    onAddSelectedEntry = {},
                    onAddCustomEntry = {},
                    onEntryTimeChanged = {},
                    onRequestWritePermission = {},
                    onUpdateContainerSize = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag("hydration_entry_tracker").assertExists()
    }
}
