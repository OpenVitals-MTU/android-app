package tech.mmarca.openvitals.features.nutrition

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sectionContext = rememberMetricDetailSectionOrdering(onSectionEditStateChanged)
    val chartDaySelection = rememberChartDaySelection(
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        key = "nutrition",
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod()
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
            )
        }
    }
}

@Composable
fun CaloriesInScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    NutritionMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = NutritionMetric.CALORIES_IN,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun ProteinScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    NutritionMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = NutritionMetric.PROTEIN,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun CarbsScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    NutritionMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = NutritionMetric.CARBS,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun FatScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    NutritionMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = NutritionMetric.FAT,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun NutritionMetricScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: NutritionMetric,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sectionContext = rememberMetricDetailSectionOrdering(onSectionEditStateChanged)
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate, metric)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod()
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
            )
        }
    }
}
