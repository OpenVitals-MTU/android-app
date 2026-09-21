package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.LayoutMetrics

/** Body profile: the declared body and the caffeine clearance factors. Shares [BodySettingsViewModel] with Recovery. */
@Composable
fun BodyProfileSettingsScreen(viewModel: BodySettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The write permission is granted in Health Connect, so it is re-read on the way back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    SettingsSectionList {
        item { SectionHeader(stringResource(SettingsSection.BODY_PROFILE.titleRes)) }
        item {
            BodyProfileCard(
                profile = state.bodyProfile,
                // The card's only unit-sensitive input is body weight.
                unitSystem = state.weightUnitSystem,
                onSave = viewModel::updateBodyProfile,
                weightMeasured = state.bodyProfileWeightMeasured,
                heightMeasured = state.bodyProfileHeightMeasured,
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
            )
        }
        item { SettingsCardSpacer() }
        item {
            MetabolismCard(
                preferences = state.caffeinePreferences,
                onSave = viewModel::updateCaffeinePreferences,
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
            )
        }
    }
}
