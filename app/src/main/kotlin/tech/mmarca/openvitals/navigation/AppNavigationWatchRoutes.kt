package tech.mmarca.openvitals.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import tech.mmarca.openvitals.features.watches.WatchDataScreen
import tech.mmarca.openvitals.features.watches.WatchDeviceScreen
import tech.mmarca.openvitals.features.watches.WatchNotificationAppsScreen
import tech.mmarca.openvitals.features.watches.WatchSettingsScreen

/** The watch-facing destinations. Plain pushed screens. */
internal fun NavGraphBuilder.watchRoutes(
    navController: NavHostController,
    onWatchDeviceTitleChanged: (String?) -> Unit,
) {
    composable(Screen.WatchDevice.route) {
        WatchDeviceScreen(
            viewModel = hiltViewModel(),
            onOpenData = { deviceId ->
                navController.navigate(Screen.WatchData.createRoute(deviceId)) {
                    launchSingleTop = true
                }
            },
            onOpenNotifications = { deviceId ->
                navController.navigate(Screen.WatchNotifications.createRoute(deviceId)) {
                    launchSingleTop = true
                }
            },
            onOpenWatchSettings = { deviceId, screenId ->
                navController.navigate(Screen.WatchSettings.createRoute(deviceId, screenId)) {
                    launchSingleTop = true
                }
            },
            onRemoved = { navController.popBackStack() },
            onTitleChanged = onWatchDeviceTitleChanged,
        )
    }

    composable(Screen.WatchData.route) {
        WatchDataScreen(viewModel = hiltViewModel())
    }

    composable(Screen.WatchNotifications.route) {
        WatchNotificationAppsScreen(viewModel = hiltViewModel())
    }

    // A row that leads deeper pushes the same route with another screen id;
    // the screens share one held link.
    composable(Screen.WatchSettings.route) {
        WatchSettingsScreen(
            viewModel = hiltViewModel(),
            onOpenSubscreen = { deviceId, screenId ->
                navController.navigate(Screen.WatchSettings.createRoute(deviceId, screenId))
            },
            onClose = { navController.popBackStack() },
        )
    }
}
