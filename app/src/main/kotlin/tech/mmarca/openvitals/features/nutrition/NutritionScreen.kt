package tech.mmarca.openvitals.features.nutrition

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.rememberMetricDetailSectionOrdering
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection

enum class NutritionMetric {
    CALORIES_IN,
    PROTEIN,
    CARBS,
    FAT,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditNutritionEntry: (String) -> Unit = {},
    onEditHydrationEntry: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sectionContext = rememberMetricDetailSectionOrdering()
    val chartDaySelection = rememberChartDaySelection(
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        key = "nutrition",
    )

    // Reloads on return, so an entry edited on another screen shows its new values.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod(refreshCurrent = true)
    }
    LaunchedEffect(state.pendingEdit) {
        val target = state.pendingEdit ?: return@LaunchedEffect
        viewModel.onEditHandled()
        when (target) {
            is NutritionEditTarget.TypedEntry -> onEditNutritionEntry(target.nutritionRecordId)
            is NutritionEditTarget.Drink -> onEditHydrationEntry(target.hydrationRecordId)
        }
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.NUTRITION,
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
            onSelectDay = viewModel::selectDay,
            weekPeriodMode = state.weekPeriodMode,
            syncPaused = hcUx.syncPaused,
            sectionListState = sectionContext.listState,
        ) { period ->
            nutritionContent(
                sectionContext = sectionContext,
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                onDeleteEntry = viewModel::deleteNutritionEntry,
                onEditEntry = viewModel::editEntry,
            )
        }
    }
}

@Composable
fun CaloriesInScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditNutritionEntry: (String) -> Unit = {},
    onEditHydrationEntry: (String) -> Unit = {},
) {
    NutritionMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = NutritionMetric.CALORIES_IN,
        onEditNutritionEntry = onEditNutritionEntry,
        onEditHydrationEntry = onEditHydrationEntry,
    )
}

@Composable
fun ProteinScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditNutritionEntry: (String) -> Unit = {},
    onEditHydrationEntry: (String) -> Unit = {},
) {
    NutritionMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = NutritionMetric.PROTEIN,
        onEditNutritionEntry = onEditNutritionEntry,
        onEditHydrationEntry = onEditHydrationEntry,
    )
}

@Composable
fun CarbsScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditNutritionEntry: (String) -> Unit = {},
    onEditHydrationEntry: (String) -> Unit = {},
) {
    NutritionMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = NutritionMetric.CARBS,
        onEditNutritionEntry = onEditNutritionEntry,
        onEditHydrationEntry = onEditHydrationEntry,
    )
}

@Composable
fun FatScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditNutritionEntry: (String) -> Unit = {},
    onEditHydrationEntry: (String) -> Unit = {},
) {
    NutritionMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = NutritionMetric.FAT,
        onEditNutritionEntry = onEditNutritionEntry,
        onEditHydrationEntry = onEditHydrationEntry,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun NutritionMetricScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: NutritionMetric,
    onEditNutritionEntry: (String) -> Unit = {},
    onEditHydrationEntry: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sectionContext = rememberMetricDetailSectionOrdering()
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate, metric)

    // Reloads on return, so an entry edited on another screen shows its new values.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod(refreshCurrent = true)
    }
    LaunchedEffect(state.pendingEdit) {
        val target = state.pendingEdit ?: return@LaunchedEffect
        viewModel.onEditHandled()
        when (target) {
            is NutritionEditTarget.TypedEntry -> onEditNutritionEntry(target.nutritionRecordId)
            is NutritionEditTarget.Drink -> onEditHydrationEntry(target.hydrationRecordId)
        }
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.NUTRITION,
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
            onSelectDay = viewModel::selectDay,
            weekPeriodMode = state.weekPeriodMode,
            syncPaused = hcUx.syncPaused,
            sectionListState = sectionContext.listState,
        ) { period ->
            nutritionMetricContent(
                sectionContext = sectionContext,
                metric = metric,
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                onDecreaseGoal = viewModel::decreaseDailyGoal,
                onIncreaseGoal = viewModel::increaseDailyGoal,
                onDeleteEntry = viewModel::deleteNutritionEntry,
                onEditEntry = viewModel::editEntry,
            )
        }
    }
}
