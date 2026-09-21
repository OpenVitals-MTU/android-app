package tech.mmarca.openvitals.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import tech.mmarca.openvitals.features.watches.WatchAlarmsScreen
import tech.mmarca.openvitals.features.watches.WatchDataScreen
import tech.mmarca.openvitals.features.watches.WatchDeviceScreen
import tech.mmarca.openvitals.features.watches.WatchNotificationAppsScreen
import tech.mmarca.openvitals.features.watches.WatchSendPointScreen
import tech.mmarca.openvitals.features.watches.WatchSettingsScreen

/** The watch-facing destinations. Plain pushed screens. */
internal fun NavGraphBuilder.watchRoutes(
    navController: NavHostController,
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
            onOpenSendPoint = { deviceId ->
                navController.navigate(Screen.WatchSendPoint.createRoute(watchDeviceId = deviceId)) {
                    launchSingleTop = true
                }
            },
            onOpenAlarms = { deviceId ->
                navController.navigate(Screen.WatchAlarms.createRoute(deviceId)) {
                    launchSingleTop = true
                }
            },
            onRemoved = { navController.popBackStack() },
        )
    }

    composable(Screen.WatchData.route) {
        WatchDataScreen(viewModel = hiltViewModel())
    }

    composable(Screen.WatchNotifications.route) {
        WatchNotificationAppsScreen(viewModel = hiltViewModel())
    }

    composable(Screen.WatchAlarms.route) {
        WatchAlarmsScreen(viewModel = hiltViewModel())
    }

    // Strings, so an absent position stays absent: a number argument cannot be null.
    composable(
        route = Screen.WatchSendPoint.route,
        arguments = listOf(
            WATCH_DEVICE_ID_ARG,
            WATCH_POINT_LATITUDE_ARG,
            WATCH_POINT_LONGITUDE_ARG,
            WATCH_POINT_NAME_ARG,
            WATCH_POINT_UNREADABLE_ARG,
        ).map { name ->
            navArgument(name) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        },
    ) {
        WatchSendPointScreen(
            viewModel = hiltViewModel(),
            onOpenWatches = {
                navController.navigate(Screen.SettingsWatches.route) { launchSingleTop = true }
            },
        )
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
