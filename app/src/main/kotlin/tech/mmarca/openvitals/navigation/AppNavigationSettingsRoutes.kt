package tech.mmarca.openvitals.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.features.settings.SettingsScreen
import tech.mmarca.openvitals.features.settings.SettingsSection
import tech.mmarca.openvitals.features.settings.SettingsViewModel
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen

internal fun NavGraphBuilder.settingsRoutes(navController: NavHostController) {
    composable(Screen.Settings.route) {
        val settingsViewModel = hiltViewModel<SettingsViewModel>()
        SettingsScreen(
            viewModel = settingsViewModel,
            onOpenSection = { section ->
                navController.navigate(settingsSectionRoute(section)) {
                    launchSingleTop = true
                }
            },
        )
    }

    composable(Screen.SettingsDisplay.route) {
        SettingsSectionScreen(SettingsSection.DISPLAY)
    }

    composable(Screen.SettingsActivities.route) {
        SettingsSectionScreen(SettingsSection.ACTIVITIES)
    }

    composable(Screen.SettingsSensors.route) {
        SettingsSectionScreen(SettingsSection.SENSORS)
    }

    composable(Screen.SettingsNutrition.route) {
        SettingsSectionScreen(SettingsSection.NUTRITION)
    }

    composable(Screen.SettingsCalories.route) {
        SettingsSectionScreen(SettingsSection.NUTRITION)
    }

    composable(Screen.SettingsCaffeine.route) {
        SettingsSectionScreen(SettingsSection.NUTRITION)
    }

    composable(Screen.SettingsRecovery.route) {
        SettingsSectionScreen(SettingsSection.RECOVERY)
    }

    composable(Screen.SettingsSleep.route) {
        SettingsSectionScreen(SettingsSection.RECOVERY)
    }

    composable(Screen.SettingsBodyEnergy.route) {
        SettingsSectionScreen(SettingsSection.RECOVERY)
    }

    composable(Screen.SettingsDataImport.route) {
        val settingsViewModel = hiltViewModel<SettingsViewModel>()
        WithHealthConnectFeatureScreen(
            feature = HealthConnectFeature.DATA_IMPORT,
            showInlineSyncBanner = false,
        ) { _ ->
            SettingsScreen(
                viewModel = settingsViewModel,
                section = SettingsSection.DATA_IMPORT,
            )
        }
    }

    composable(Screen.SettingsHealthConnect.route) {
        SettingsSectionScreen(SettingsSection.HEALTH_CONNECT)
    }

    composable(Screen.SettingsPermissions.route) {
        SettingsSectionScreen(SettingsSection.HEALTH_CONNECT)
    }

    if (BuildConfig.OPENVITALS_DIAGNOSTICS) {
        composable(Screen.SettingsDebugDiagnostics.route) {
            SettingsSectionScreen(SettingsSection.DEBUG_DIAGNOSTICS)
        }
    }
}

private fun settingsSectionRoute(section: SettingsSection): String =
    when (section) {
        SettingsSection.DISPLAY -> Screen.SettingsDisplay.route
        SettingsSection.ACTIVITIES -> Screen.SettingsActivities.route
        SettingsSection.SENSORS -> Screen.SettingsSensors.route
        SettingsSection.NUTRITION -> Screen.SettingsNutrition.route
        SettingsSection.RECOVERY -> Screen.SettingsRecovery.route
        SettingsSection.DATA_IMPORT -> Screen.SettingsDataImport.route
        SettingsSection.HEALTH_CONNECT -> Screen.SettingsHealthConnect.route
        SettingsSection.DEBUG_DIAGNOSTICS -> Screen.SettingsDebugDiagnostics.route
    }

@Composable
private fun SettingsSectionScreen(section: SettingsSection) {
    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    SettingsScreen(
        viewModel = settingsViewModel,
        section = section,
    )
}
