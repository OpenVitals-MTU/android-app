package tech.mmarca.openvitals.features.sleep

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenSleepSession: (String) -> Unit,
    onOpenSleepScore: (() -> Unit)? = null,
    onOpenSleepEfficiency: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val display = state.display
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod()
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.SLEEP,
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
        ) { period ->
            if (!state.isLoading || state.sessions.isNotEmpty()) {
                sleepOverview(
                    summary = display.overviewSummary,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenSleepScore = onOpenSleepScore,
                    onOpenSleepEfficiency = onOpenSleepEfficiency,
                )
            }

            when {
                state.selectedRange == TimeRange.DAY && display.dailySummary != null -> {
                    sleepDayContent(
                        state = state,
                        display = display,
                        period = period,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        onOpenSleepSession = onOpenSleepSession,
                        onDecreaseGoal = viewModel::decreaseDailyGoal,
                        onIncreaseGoal = viewModel::increaseDailyGoal,
                    )
                }

                state.selectedRange != TimeRange.DAY && state.sessions.isNotEmpty() -> {
                    sleepPeriodContent(
                        state = state,
                        display = display,
                        period = period,
                        chartDaySelection = chartDaySelection,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        onOpenSleepSession = onOpenSleepSession,
                        onDecreaseGoal = viewModel::decreaseDailyGoal,
                        onIncreaseGoal = viewModel::increaseDailyGoal,
                    )
                }

                !state.isLoading -> {
                    sleepNoDataContent(selectedRange = state.selectedRange)
                }
            }
        }
    }
}
