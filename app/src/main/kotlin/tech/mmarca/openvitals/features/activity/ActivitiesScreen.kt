package tech.mmarca.openvitals.features.activity

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.rememberMetricDetailSectionOrdering
import tech.mmarca.openvitals.domain.preferences.toWeekPeriodMode
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    viewModel: ActivitiesViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit = {},
    onStartPlannedWorkout: (String) -> Unit = {},
    onOpenCardioLoad: (() -> Unit)? = null,
    onOpenSteps: (() -> Unit)? = null,
    onOpenDistance: (() -> Unit)? = null,
    onOpenEnergyBurned: (() -> Unit)? = null,
    onOpenHrv: (() -> Unit)? = null,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sectionContext = rememberMetricDetailSectionOrdering(onSectionEditStateChanged)
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod(refreshCurrent = true)
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.ACTIVITIES,
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
            weekPeriodMode = state.activityWeekMode.toWeekPeriodMode(),
            syncPaused = hcUx.syncPaused,
            sectionListState = sectionContext.listState,
            periodOverride = {
                activityDisplayPeriod(
                    selectedRange = state.selectedRange,
                    selectedDate = state.selectedDate,
                    activityWeekMode = state.activityWeekMode,
                )
            },
            periodTitle = { period ->
                activityPeriodTitle(state.selectedRange, state.activityWeekMode, period)
            },
        ) { period ->
            renderActivitiesOrderedContent(
                sectionContext = sectionContext,
                state = state,
                period = period,
                chartDaySelection = chartDaySelection,
                selectedActivityType = state.selectedActivityType,
                availableActivityTypes = state.availableActivityTypes,
                onSelectActivityType = viewModel::selectActivityType,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenActivity = onOpenActivity,
                onEditActivity = onEditActivity,
                onDeleteActivity = viewModel::deleteActivityEntry,
                onStartPlannedWorkout = onStartPlannedWorkout,
                onOpenCardioLoad = onOpenCardioLoad,
                onOpenSteps = onOpenSteps,
                onOpenDistance = onOpenDistance,
                onOpenEnergyBurned = onOpenEnergyBurned,
                onOpenHrv = onOpenHrv,
                onDecreaseGoal = viewModel::decreaseDailyGoal,
                onIncreaseGoal = viewModel::increaseDailyGoal,
            )
        }
    }
}
