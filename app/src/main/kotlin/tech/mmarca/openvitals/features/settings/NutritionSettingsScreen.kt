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
import tech.mmarca.openvitals.ui.theme.Spacing

/** Nutrition: calories, averages, the drinking goal and the caffeine model. */
@Composable
fun NutritionSettingsScreen(viewModel: NutritionSettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The caffeine model reads the Health Connect weight, which can change while the app is away.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    SettingsSectionList {
        item { SectionHeader(stringResource(SettingsSection.NUTRITION.titleRes)) }
        item {
            CalorieDataSourceCard(
                enabled = state.showOpenVitalsCalculatedCalories,
                onEnabledChange = viewModel::setShowOpenVitalsCalculatedCalories,
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
            )
        }
        item { SettingsCardSpacer() }
        item {
            NutritionAverageBasisCard(
                loggedDaysOnly = state.nutritionAverageLoggedDaysOnly,
                onLoggedDaysOnlyChange = viewModel::setNutritionAverageLoggedDaysOnly,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
        }
        item { SettingsCardSpacer() }
        item {
            HydrationGoalCard(
                goalLiters = state.hydrationDailyGoalLiters,
                unitSystem = state.hydrationUnitSystem,
                onGoalChange = viewModel::setHydrationDailyGoalLiters,
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
            )
        }
        item { SettingsCardSpacer() }
        item {
            CaffeinePreferencesCard(
                preferences = state.caffeinePreferences,
                bodyProfile = state.bodyProfile,
                onSave = viewModel::updateCaffeinePreferences,
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
            )
        }
    }
}
