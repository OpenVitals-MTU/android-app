package tech.mmarca.openvitals.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import tech.mmarca.openvitals.features.settings.SettingsScreen
import tech.mmarca.openvitals.features.settings.SettingsSection
import tech.mmarca.openvitals.features.settings.SettingsViewModel

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
        val settingsViewModel = hiltViewModel<SettingsViewModel>()
        SettingsScreen(
            viewModel = settingsViewModel,
            section = SettingsSection.DISPLAY,
        )
    }

    composable(Screen.SettingsActivities.route) {
        val settingsViewModel = hiltViewModel<SettingsViewModel>()
        SettingsScreen(
            viewModel = settingsViewModel,
            section = SettingsSection.ACTIVITIES,
        )
    }

    composable(Screen.SettingsCalories.route) {
        val settingsViewModel = hiltViewModel<SettingsViewModel>()
        SettingsScreen(
            viewModel = settingsViewModel,
            section = SettingsSection.CALORIES,
        )
    }

    composable(Screen.SettingsSleep.route) {
        val settingsViewModel = hiltViewModel<SettingsViewModel>()
        SettingsScreen(
            viewModel = settingsViewModel,
            section = SettingsSection.SLEEP,
        )
    }

    composable(Screen.SettingsCycle.route) {
        val settingsViewModel = hiltViewModel<SettingsViewModel>()
        SettingsScreen(
            viewModel = settingsViewModel,
            section = SettingsSection.CYCLE,
        )
    }

    composable(Screen.SettingsDataImport.route) {
        val settingsViewModel = hiltViewModel<SettingsViewModel>()
        SettingsScreen(
            viewModel = settingsViewModel,
            section = SettingsSection.DATA_IMPORT,
        )
    }

    composable(Screen.SettingsHealthConnect.route) {
        val settingsViewModel = hiltViewModel<SettingsViewModel>()
        SettingsScreen(
            viewModel = settingsViewModel,
            section = SettingsSection.HEALTH_CONNECT,
        )
    }

    composable(Screen.SettingsPermissions.route) {
        val settingsViewModel = hiltViewModel<SettingsViewModel>()
        SettingsScreen(
            viewModel = settingsViewModel,
            section = SettingsSection.PERMISSIONS,
        )
    }
}

private fun settingsSectionRoute(section: SettingsSection): String =
    when (section) {
        SettingsSection.DISPLAY -> Screen.SettingsDisplay.route
        SettingsSection.ACTIVITIES -> Screen.SettingsActivities.route
        SettingsSection.CALORIES -> Screen.SettingsCalories.route
        SettingsSection.SLEEP -> Screen.SettingsSleep.route
        SettingsSection.CYCLE -> Screen.SettingsCycle.route
        SettingsSection.DATA_IMPORT -> Screen.SettingsDataImport.route
        SettingsSection.HEALTH_CONNECT -> Screen.SettingsHealthConnect.route
        SettingsSection.PERMISSIONS -> Screen.SettingsPermissions.route
    }
