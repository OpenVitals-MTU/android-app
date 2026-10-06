package tech.mmarca.openvitals.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import tech.mmarca.openvitals.wear.features.dashboard.DashboardScreen
import tech.mmarca.openvitals.wear.features.dashboard.TileEditorScreen
import tech.mmarca.openvitals.wear.features.measure.MeasureListScreen
import tech.mmarca.openvitals.wear.features.measure.Measurement
import tech.mmarca.openvitals.wear.features.measure.MeasurementRoute
import tech.mmarca.openvitals.wear.features.metric.MetricDetailScreen
import tech.mmarca.openvitals.wear.features.quicklog.BloodPressureEntryScreen
import tech.mmarca.openvitals.wear.features.quicklog.BreatheScreen
import tech.mmarca.openvitals.wear.features.quicklog.EntryType
import tech.mmarca.openvitals.wear.features.quicklog.LoggedEntry
import tech.mmarca.openvitals.wear.features.quicklog.QuickLogScreen
import tech.mmarca.openvitals.wear.features.quicklog.WaterServingMl
import tech.mmarca.openvitals.wear.features.quicklog.WeightEntryScreen
import tech.mmarca.openvitals.wear.features.recording.ActivityPickerScreen
import tech.mmarca.openvitals.wear.features.recording.RecordingScreen
import tech.mmarca.openvitals.wear.features.recording.WorkoutSummaryScreen
import tech.mmarca.openvitals.wear.features.settings.SettingsScreen
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme

/** Starting weight on the weight stepper before anything was logged. */
private const val DefaultWeightKg = 70.0

/**
 * The whole watch UI: theme, scaffold and navigation. It renders [state] and
 * hands entries to [onLog]; nothing here reads a sensor except through a
 * screen's own ViewModel. The picked activity and the pause flag live here
 * until a recorder owns them, and tile choices are persisted via
 * [WearTilePreferences]. [deepLinkRoute] is a screen a tile asked for.
 */
@Composable
fun OpenVitalsWearApp(
    state: WearUiState,
    onLog: (LoggedEntry) -> Unit = {},
    deepLinkRoute: String? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    // The nav graph is built once, so every destination reads the latest
    // state through this holder instead of a value captured at build time.
    val currentState by rememberUpdatedState(state)

    OpenVitalsWearTheme {
        // AppScaffold draws the time once, above every screen.
        AppScaffold {
            val context = LocalContext.current
            val tilePrefs = remember(context) { WearTilePreferences(context) }
            val navController = rememberSwipeDismissableNavController()
            var activityType by rememberSaveable { mutableStateOf(state.recording.activityType) }
            var paused by rememberSaveable { mutableStateOf(state.recording.paused) }
            var chosenTiles by remember { mutableStateOf(tilePrefs.loadTiles()) }
            fun updateTiles(updated: List<WearMetric>) {
                chosenTiles = updated
                tilePrefs.saveTiles(updated)
            }
            // A chosen tile whose sensor is missing stays chosen but is not shown.
            fun visibleTiles() = chosenTiles.filter { it in currentState.availableMetrics }
            fun recording() = currentState.recording.copy(activityType = activityType, paused = paused)
            fun workoutFields() = currentState.capabilities.workouts[activityType].orEmpty()
            fun log(type: EntryType, value: Double, value2: Double? = null) =
                onLog(LoggedEntry(type, System.currentTimeMillis(), value, value2))

            LaunchedEffect(deepLinkRoute) {
                val route = deepLinkRoute ?: return@LaunchedEffect
                if (WearRoutes.isDeepLinkable(route)) {
                    navController.navigate(route) { popUpTo(WearRoutes.DASHBOARD) }
                }
                onDeepLinkHandled()
            }

            SwipeDismissableNavHost(
                navController = navController,
                startDestination = WearRoutes.DASHBOARD,
            ) {
                composable(WearRoutes.DASHBOARD) {
                    DashboardScreen(
                        tiles = visibleTiles(),
                        metrics = currentState.metrics,
                        unitSystem = currentState.preferences.unitSystem,
                        canMeasure = currentState.capabilities.measurements.isNotEmpty(),
                        onOpenMetric = { navController.navigate(WearRoutes.metric(it)) },
                        onMeasure = { navController.navigate(WearRoutes.MEASURE) },
                        onQuickLog = { navController.navigate(WearRoutes.QUICK_LOG) },
                        onEditTiles = { navController.navigate(WearRoutes.TILE_EDITOR) },
                        onStartActivity = { navController.navigate(WearRoutes.ACTIVITY_PICKER) },
                        onOpenSettings = { navController.navigate(WearRoutes.SETTINGS) },
                    )
                }
                composable(WearRoutes.METRIC) { entry ->
                    val metric = WearMetric.valueOf(checkNotNull(entry.arguments?.getString(WearRoutes.METRIC_ARG)))
                    val measurement = metric.spotMeasurement?.takeIf { it in currentState.capabilities.measurements }
                    MetricDetailScreen(
                        metric = metric,
                        state = currentState.metrics[metric] ?: MetricUiState(),
                        unitSystem = currentState.preferences.unitSystem,
                        maxHeartRate = currentState.preferences.maxHeartRate,
                        restingHeartRate = currentState.metrics[WearMetric.RESTING_HEART_RATE]?.current,
                        sleepStages = currentState.sleepStages,
                        onMeasure = measurement?.let { { navController.navigate(WearRoutes.measurement(it)) } },
                    )
                }
                composable(WearRoutes.TILE_EDITOR) {
                    TileEditorScreen(
                        available = WearMetric.entries.filter { it in currentState.availableMetrics },
                        tiles = visibleTiles(),
                        onToggle = { metric, checked ->
                            updateTiles(if (checked) chosenTiles + metric else chosenTiles - metric)
                        },
                        onMove = { moved, target -> updateTiles(chosenTiles.withTileMoved(moved, target)) },
                        onReset = { chosenTiles = tilePrefs.resetTiles() },
                    )
                }
                composable(WearRoutes.MEASURE) {
                    MeasureListScreen(
                        capabilities = currentState.capabilities,
                        onMeasure = { navController.navigate(WearRoutes.measurement(it)) },
                    )
                }
                composable(WearRoutes.MEASUREMENT) { entry ->
                    val name = entry.arguments?.getString(WearRoutes.MEASUREMENT_ARG)
                    val measurement = Measurement.entries.firstOrNull { it.name == name } ?: Measurement.HEART_RATE
                    val capabilities = currentState.capabilities
                    MeasurementRoute(
                        measurement = measurement,
                        supported = if (capabilities.probed) measurement in capabilities.measurements else null,
                        unitSystem = currentState.preferences.unitSystem,
                        onDone = { navController.popBackStack() },
                    )
                }
                composable(WearRoutes.QUICK_LOG) {
                    QuickLogScreen(
                        state = currentState,
                        onAddWater = { log(EntryType.WATER, WaterServingMl) },
                        onLogWeight = { navController.navigate(WearRoutes.LOG_WEIGHT) },
                        onLogBloodPressure = { navController.navigate(WearRoutes.LOG_BLOOD_PRESSURE) },
                        onBreathe = { navController.navigate(WearRoutes.BREATHE) },
                    )
                }
                composable(WearRoutes.LOG_WEIGHT) {
                    WeightEntryScreen(
                        initialKg = currentState.entries.lastOrNull { it.type == EntryType.WEIGHT }?.value
                            ?: currentState.metrics[WearMetric.WEIGHT]?.current
                            ?: DefaultWeightKg,
                        unitSystem = currentState.preferences.unitSystem,
                        onSave = { kilograms ->
                            log(EntryType.WEIGHT, kilograms)
                            navController.popBackStack()
                        },
                    )
                }
                composable(WearRoutes.LOG_BLOOD_PRESSURE) {
                    val last = currentState.entries.lastOrNull { it.type == EntryType.BLOOD_PRESSURE }
                    BloodPressureEntryScreen(
                        initialSystolic = last?.value?.toInt() ?: 120,
                        initialDiastolic = last?.value2?.toInt() ?: 80,
                        onSave = { systolic, diastolic ->
                            log(EntryType.BLOOD_PRESSURE, systolic.toDouble(), diastolic.toDouble())
                            navController.popBackStack()
                        },
                    )
                }
                composable(WearRoutes.BREATHE) {
                    BreatheScreen(
                        onFinished = { log(EntryType.MINDFULNESS, 1.0) },
                        onClose = { navController.popBackStack() },
                    )
                }
                composable(WearRoutes.ACTIVITY_PICKER) {
                    ActivityPickerScreen(
                        capabilities = currentState.capabilities,
                        onPick = { picked ->
                            activityType = picked
                            paused = false
                            navController.navigate(WearRoutes.RECORDING) {
                                // Stopping returns to the dashboard, not to the picker.
                                popUpTo(WearRoutes.DASHBOARD)
                            }
                        },
                    )
                }
                composable(WearRoutes.RECORDING) {
                    RecordingScreen(
                        state = recording(),
                        fields = workoutFields(),
                        unitSystem = currentState.preferences.unitSystem,
                        maxHeartRate = currentState.preferences.maxHeartRate,
                        onTogglePause = { paused = !paused },
                        onStop = {
                            navController.navigate(WearRoutes.WORKOUT_SUMMARY) {
                                popUpTo(WearRoutes.DASHBOARD)
                            }
                        },
                    )
                }
                composable(WearRoutes.WORKOUT_SUMMARY) {
                    WorkoutSummaryScreen(
                        state = recording(),
                        fields = workoutFields(),
                        unitSystem = currentState.preferences.unitSystem,
                        onDone = { navController.popBackStack() },
                    )
                }
                composable(WearRoutes.SETTINGS) {
                    SettingsScreen(
                        state = currentState.settings,
                        unitSystem = currentState.preferences.unitSystem,
                        onPhoneClick = {},
                        onSensorAccessClick = {},
                        onUnitsClick = {},
                    )
                }
            }
        }
    }
}

/** The spot measurement that refreshes this metric, if there is one. */
private val WearMetric.spotMeasurement: Measurement?
    get() = when (this) {
        WearMetric.HEART_RATE -> Measurement.HEART_RATE
        WearMetric.HRV -> Measurement.HRV
        else -> null
    }
