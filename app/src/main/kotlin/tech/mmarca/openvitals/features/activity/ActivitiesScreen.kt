package tech.mmarca.openvitals.features.activity

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.rememberMetricDetailSectionOrdering
import tech.mmarca.openvitals.domain.model.ExerciseData
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
    onManageWorkoutPlans: () -> Unit = {},
    onOpenCardioLoad: (() -> Unit)? = null,
    onOpenSteps: (() -> Unit)? = null,
    onOpenDistance: (() -> Unit)? = null,
    onOpenEnergyBurned: (() -> Unit)? = null,
    onOpenHrv: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sectionContext = rememberMetricDetailSectionOrdering()
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate)
    // A swipe only asks. The dashboard already did; this list deleted at once, with no undo.
    var workoutToDelete by remember { mutableStateOf<ExerciseData?>(null) }

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
            onSelectDay = viewModel::selectDay,
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
                onDeleteActivity = { id -> workoutToDelete = state.workouts.firstOrNull { it.id == id } },
                onStartPlannedWorkout = onStartPlannedWorkout,
                onManageWorkoutPlans = onManageWorkoutPlans,
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

    workoutToDelete?.let { workout ->
        DeleteActivityConfirmationDialog(
            workout = workout,
            onDismiss = { workoutToDelete = null },
            onConfirm = {
                workoutToDelete = null
                viewModel.deleteActivityEntry(workout.id)
            },
        )
    }
}
