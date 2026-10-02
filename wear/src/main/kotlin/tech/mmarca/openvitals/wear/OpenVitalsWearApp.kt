package tech.mmarca.openvitals.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import tech.mmarca.openvitals.wear.features.dashboard.DashboardScreen
import tech.mmarca.openvitals.wear.features.dashboard.TileEditorScreen
import tech.mmarca.openvitals.wear.features.metric.MetricDetailScreen
import tech.mmarca.openvitals.wear.features.recording.ActivityPickerScreen
import tech.mmarca.openvitals.wear.features.recording.RecordingScreen
import tech.mmarca.openvitals.wear.features.settings.SettingsScreen
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme

private object Routes {
    const val DASHBOARD = "dashboard"
    const val TILE_EDITOR = "tile_editor"
    const val METRIC_ARG = "metric"
    const val METRIC = "metric/{$METRIC_ARG}"
    fun metric(metric: WearMetric) = "metric/${metric.name}"
    const val ACTIVITY_PICKER = "activity_picker"
    const val RECORDING = "recording"
    const val SETTINGS = "settings"
}

/**
 * The whole watch UI: theme, scaffold and navigation. It only renders
 * [state]; nothing here reads a sensor or talks to the phone yet. The
 * picked activity and the pause flag live here until a recorder owns them,
 * and the tile selection until something persists it.
 */
@Composable
fun OpenVitalsWearApp(state: WearUiState) {
    OpenVitalsWearTheme {
        // AppScaffold draws the time once, above every screen.
        AppScaffold {
            val navController = rememberSwipeDismissableNavController()
            var activityType by rememberSaveable { mutableStateOf(state.recording.activityType) }
            var paused by rememberSaveable { mutableStateOf(state.recording.paused) }
            var chosenTiles by rememberSaveable { mutableStateOf(WearMetric.DefaultTiles) }
            // A chosen tile whose sensor is missing stays chosen but is not shown.
            val tiles = chosenTiles.filter { it in state.availableMetrics }

            SwipeDismissableNavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        tiles = tiles,
                        metrics = state.metrics,
                        unitSystem = state.preferences.unitSystem,
                        onOpenMetric = { navController.navigate(Routes.metric(it)) },
                        onEditTiles = { navController.navigate(Routes.TILE_EDITOR) },
                        onStartActivity = { navController.navigate(Routes.ACTIVITY_PICKER) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
                composable(Routes.METRIC) { entry ->
                    val metric = WearMetric.valueOf(checkNotNull(entry.arguments?.getString(Routes.METRIC_ARG)))
                    MetricDetailScreen(
                        metric = metric,
                        state = state.metrics[metric] ?: MetricUiState(),
                        unitSystem = state.preferences.unitSystem,
                    )
                }
                composable(Routes.TILE_EDITOR) {
                    TileEditorScreen(
                        available = WearMetric.entries.filter { it in state.availableMetrics },
                        tiles = tiles,
                        onToggle = { metric, checked ->
                            chosenTiles = if (checked) chosenTiles + metric else chosenTiles - metric
                        },
                    )
                }
                composable(Routes.ACTIVITY_PICKER) {
                    ActivityPickerScreen(
                        onPick = { picked ->
                            activityType = picked
                            paused = false
                            navController.navigate(Routes.RECORDING) {
                                // Stopping returns to the dashboard, not to the picker.
                                popUpTo(Routes.DASHBOARD)
                            }
                        },
                    )
                }
                composable(Routes.RECORDING) {
                    RecordingScreen(
                        state = state.recording.copy(activityType = activityType, paused = paused),
                        unitSystem = state.preferences.unitSystem,
                        onTogglePause = { paused = !paused },
                        onStop = { navController.popBackStack() },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        state = state.settings,
                        unitSystem = state.preferences.unitSystem,
                        onPhoneClick = {},
                        onSensorAccessClick = {},
                        onUnitsClick = {},
                    )
                }
            }
        }
    }
}
