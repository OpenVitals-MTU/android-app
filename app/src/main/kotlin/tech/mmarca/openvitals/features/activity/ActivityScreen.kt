package tech.mmarca.openvitals.features.activity

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionOrderViewModel
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState

enum class ActivityMetric {
    STEPS,
    DISTANCE,
    CALORIES_BURNED,
    ACTIVE_CALORIES,
    FLOORS,
    ELEVATION,
    WHEELCHAIR_PUSHES,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    ActivityMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = ActivityMetric.STEPS,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun DistanceScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    ActivityMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = ActivityMetric.DISTANCE,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun CaloriesOutScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    ActivityMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = ActivityMetric.CALORIES_BURNED,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun ActiveCaloriesScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    ActivityMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = ActivityMetric.ACTIVE_CALORIES,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun FloorsScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    ActivityMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = ActivityMetric.FLOORS,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun ElevationScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    ActivityMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = ActivityMetric.ELEVATION,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun WheelchairPushesScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    ActivityMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = ActivityMetric.WHEELCHAIR_PUSHES,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ActivityMetricScreen(
    viewModel: ActivityViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: ActivityMetric,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sectionOrderViewModel = hiltViewModel<MetricDetailSectionOrderViewModel>()
    val sectionOrder by sectionOrderViewModel.sectionOrder.collectAsStateWithLifecycle()
    val isEditingSections by sectionOrderViewModel.isEditingSections.collectAsStateWithLifecycle()
    val sectionListState = rememberMetricDetailSectionListState()
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate, metric)
    val sectionContext = ActivityMetricSectionContext(
        listState = sectionListState,
        order = sectionOrder,
        isEditingSections = isEditingSections,
        onMoveSectionToTarget = sectionOrderViewModel::moveSectionToTarget,
        onMoveSection = sectionOrderViewModel::moveSection,
    )

    LaunchedEffect(isEditingSections) {
        onSectionEditStateChanged(isEditingSections, sectionOrderViewModel::toggleSectionEdit)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isEditingSections) {
                sectionOrderViewModel.toggleSectionEdit()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod()
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.ACTIVITY,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { hcUx ->
        MetricDetailScaffold(
            isLoading = state.isLoading,
            selectedRange = state.selectedRange,
            selectedDate = state.selectedDate,
            screenError = state.error,
            onRefresh = viewModel::load,
            onSelectRange = viewModel::selectRange,
            onPreviousPeriod = viewModel::previousPeriod,
            onNextPeriod = viewModel::nextPeriod,
            onSelectDate = viewModel::selectDate,
            weekPeriodMode = state.weekPeriodMode,
            syncPaused = hcUx.syncPaused,
            sectionListState = sectionListState,
        ) { period ->
            when (metric) {
                ActivityMetric.STEPS -> stepsContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    chartDaySelection,
                    sectionContext,
                    viewModel::decreaseDailyGoal,
                    viewModel::increaseDailyGoal,
                )
                ActivityMetric.DISTANCE -> distanceContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    chartDaySelection,
                    sectionContext,
                    viewModel::decreaseDailyGoal,
                    viewModel::increaseDailyGoal,
                )
                ActivityMetric.CALORIES_BURNED -> caloriesContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    chartDaySelection,
                    sectionContext,
                    viewModel::decreaseDailyGoal,
                    viewModel::increaseDailyGoal,
                )
                ActivityMetric.ACTIVE_CALORIES -> activeCaloriesContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    chartDaySelection,
                    sectionContext,
                    viewModel::decreaseDailyGoal,
                    viewModel::increaseDailyGoal,
                )
                ActivityMetric.FLOORS -> floorsContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    chartDaySelection,
                    sectionContext,
                    viewModel::decreaseDailyGoal,
                    viewModel::increaseDailyGoal,
                )
                ActivityMetric.ELEVATION -> elevationContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    chartDaySelection,
                    sectionContext,
                    viewModel::decreaseDailyGoal,
                    viewModel::increaseDailyGoal,
                )
                ActivityMetric.WHEELCHAIR_PUSHES -> wheelchairPushesContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    chartDaySelection,
                    sectionContext,
                    viewModel::decreaseDailyGoal,
                    viewModel::increaseDailyGoal,
                )
            }
        }
    }
}
