package tech.mmarca.openvitals.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.features.settings.SettingsScreen
import tech.mmarca.openvitals.features.settings.SettingsSection
import tech.mmarca.openvitals.features.settings.SettingsViewModel

internal fun NavGraphBuilder.settingsRoutes(
    navController: NavHostController,
    onImportFitFile: (Uri) -> Unit = {},
) {
    composable(Screen.Settings.route) {
        val settingsViewModel = hiltViewModel<SettingsViewModel>()
        SettingsScreen(
            viewModel = settingsViewModel,
            onImportFitFileSelected = onImportFitFile,
            onOpenSection = { section ->
                navController.navigate(settingsSectionRoute(section)) {
                    launchSingleTop = true
                }
            },
        )
    }

    composable(Screen.SettingsDisplay.route) {
        SettingsSectionScreen(SettingsSection.DISPLAY, onImportFitFile)
    }

    composable(Screen.SettingsActivities.route) {
        SettingsSectionScreen(SettingsSection.ACTIVITIES, onImportFitFile)
    }

    composable(Screen.SettingsSensors.route) {
        SettingsSectionScreen(SettingsSection.SENSORS, onImportFitFile)
    }

    composable(Screen.SettingsNutrition.route) {
        SettingsSectionScreen(SettingsSection.NUTRITION, onImportFitFile)
    }

    composable(Screen.SettingsCalories.route) {
        SettingsSectionScreen(SettingsSection.NUTRITION, onImportFitFile)
    }

    composable(Screen.SettingsCaffeine.route) {
        SettingsSectionScreen(SettingsSection.NUTRITION, onImportFitFile)
    }

    composable(Screen.SettingsRecovery.route) {
        SettingsSectionScreen(SettingsSection.RECOVERY, onImportFitFile)
    }

    composable(Screen.SettingsSleep.route) {
        SettingsSectionScreen(SettingsSection.RECOVERY, onImportFitFile)
    }

    composable(Screen.SettingsBodyEnergy.route) {
        SettingsSectionScreen(SettingsSection.RECOVERY, onImportFitFile)
    }

    composable(Screen.SettingsDataImport.route) {
        SettingsSectionScreen(SettingsSection.DATA_IMPORT, onImportFitFile)
    }

    composable(Screen.SettingsHealthConnect.route) {
        SettingsSectionScreen(SettingsSection.HEALTH_CONNECT, onImportFitFile)
    }

    composable(Screen.SettingsPermissions.route) {
        SettingsSectionScreen(SettingsSection.HEALTH_CONNECT, onImportFitFile)
    }

    if (BuildConfig.OPENVITALS_DIAGNOSTICS) {
        composable(Screen.SettingsDebugDiagnostics.route) {
            SettingsSectionScreen(SettingsSection.DEBUG_DIAGNOSTICS, onImportFitFile)
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
private fun SettingsSectionScreen(
    section: SettingsSection,
    onImportFitFile: (Uri) -> Unit,
) {
    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    SettingsScreen(
        viewModel = settingsViewModel,
        section = section,
        onImportFitFileSelected = onImportFitFile,
    )
}
