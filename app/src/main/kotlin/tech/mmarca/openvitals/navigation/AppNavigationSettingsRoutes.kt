package tech.mmarca.openvitals.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.features.devicesync.DeviceSyncScreen
import tech.mmarca.openvitals.features.imports.csv.CsvImportScreen
import tech.mmarca.openvitals.features.reports.ReportBuilderScreen
import tech.mmarca.openvitals.features.settings.ActivitiesSettingsScreen
import tech.mmarca.openvitals.features.settings.BodyProfileSettingsScreen
import tech.mmarca.openvitals.features.settings.DataImportScreen
import tech.mmarca.openvitals.features.settings.DisplaySettingsScreen
import tech.mmarca.openvitals.features.settings.NutritionSettingsScreen
import tech.mmarca.openvitals.features.settings.RecoverySettingsScreen
import tech.mmarca.openvitals.features.settings.SensorsSettingsScreen
import tech.mmarca.openvitals.features.settings.SettingsScreen
import tech.mmarca.openvitals.features.settings.SettingsSection
import tech.mmarca.openvitals.features.watches.WatchesSettingsScreen

/**
 * Each section builds only its own ViewModel. The root, Health Connect, Vitals and
 * Diagnostics share [tech.mmarca.openvitals.features.settings.SettingsViewModel].
 */
internal fun NavGraphBuilder.settingsRoutes(
    navController: NavHostController,
    onImportRouteFile: (Uri) -> Unit = {},
    onImportFitFile: (Uri) -> Unit = {},
    onRouteFilesImported: () -> Unit = {},
) {
    composable(Screen.Settings.route) {
        SettingsScreen(
            viewModel = hiltViewModel(),
            onOpenSection = { section ->
                navController.navigate(settingsSectionRoute(section)) {
                    launchSingleTop = true
                }
            },
        )
    }

    composable(Screen.SettingsDisplay.route) {
        DisplaySettingsScreen(viewModel = hiltViewModel())
    }

    composable(Screen.SettingsActivities.route) {
        ActivitiesSettingsScreen(viewModel = hiltViewModel())
    }

    composable(Screen.SettingsSensors.route) {
        SensorsSettingsScreen()
    }

    // A bespoke screen: paired-watch rows plus the add flow.
    composable(Screen.SettingsWatches.route) {
        WatchesSettingsScreen(
            viewModel = hiltViewModel(),
            onOpenWatch = { deviceId ->
                navController.navigate(Screen.WatchDevice.createRoute(deviceId)) {
                    launchSingleTop = true
                }
            },
        )
    }

    // Three routes, one screen: the metric screens link to the section by what they show.
    composable(Screen.SettingsNutrition.route) {
        NutritionSettingsScreen(viewModel = hiltViewModel())
    }

    composable(Screen.SettingsCalories.route) {
        NutritionSettingsScreen(viewModel = hiltViewModel())
    }

    composable(Screen.SettingsCaffeine.route) {
        NutritionSettingsScreen(viewModel = hiltViewModel())
    }

    composable(Screen.SettingsBodyProfile.route) {
        BodyProfileSettingsScreen(viewModel = hiltViewModel())
    }

    composable(Screen.SettingsVitals.route) {
        SettingsSectionScreen(SettingsSection.VITALS)
    }

    composable(Screen.SettingsRecovery.route) {
        RecoverySettingsScreen(viewModel = hiltViewModel())
    }

    composable(Screen.SettingsSleep.route) {
        RecoverySettingsScreen(viewModel = hiltViewModel())
    }

    composable(Screen.SettingsBodyEnergy.route) {
        RecoverySettingsScreen(viewModel = hiltViewModel())
    }

    composable(Screen.SettingsDataImport.route) {
        DataImportScreen(
            viewModel = hiltViewModel(),
            onImportRouteFileSelected = onImportRouteFile,
            onImportFitFileSelected = onImportFitFile,
            onRouteFilesImported = onRouteFilesImported,
            onOpenCsvImport = {
                navController.navigate(Screen.SettingsCsvImport.route) {
                    launchSingleTop = true
                }
            },
            onOpenReportExport = {
                navController.navigate(Screen.SettingsReportExport.route) {
                    launchSingleTop = true
                }
            },
        )
    }

    composable(Screen.SettingsCsvImport.route) {
        CsvImportScreen(onDone = { navController.popBackStack() })
    }

    composable(Screen.SettingsReportExport.route) {
        ReportBuilderScreen(onDone = { navController.popBackStack() })
    }

    // A bespoke wizard, not a settings card list.
    composable(Screen.SettingsDeviceSync.route) {
        DeviceSyncScreen(onDone = { navController.popBackStack() })
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
        SettingsSection.WATCHES -> Screen.SettingsWatches.route
        SettingsSection.NUTRITION -> Screen.SettingsNutrition.route
        SettingsSection.BODY_PROFILE -> Screen.SettingsBodyProfile.route
        SettingsSection.VITALS -> Screen.SettingsVitals.route
        SettingsSection.RECOVERY -> Screen.SettingsRecovery.route
        SettingsSection.DATA_IMPORT -> Screen.SettingsDataImport.route
        SettingsSection.DEVICE_SYNC -> Screen.SettingsDeviceSync.route
        SettingsSection.HEALTH_CONNECT -> Screen.SettingsHealthConnect.route
        SettingsSection.DEBUG_DIAGNOSTICS -> Screen.SettingsDebugDiagnostics.route
    }

/** The sections that still share the root's ViewModel. */
@Composable
private fun SettingsSectionScreen(section: SettingsSection) {
    SettingsScreen(
        viewModel = hiltViewModel(),
        section = section,
    )
}
