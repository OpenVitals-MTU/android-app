package tech.mmarca.openvitals.features.readiness

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen

@Composable
fun DailyReadinessScreen(
    viewModel: DailyReadinessViewModel,
    onOpenBodyEnergyDetails: (LocalDate) -> Unit,
    onOpenTrainingReadinessDetails: (LocalDate) -> Unit,
    onOpenStressDetails: (LocalDate) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentDay()
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.READINESS,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { _ ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.insight != null,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.isLoading && state.insight == null -> FullScreenLoading()
                state.error != null && state.insight == null ->
                    ErrorMessage(state.error?.resolve() ?: stringResource(R.string.unknown_error))
                state.insight != null -> DailyReadinessContent(
                    state = state,
                    canGoForward = state.selectedDate.isBefore(LocalDate.now()),
                    onPreviousDay = viewModel::previousDay,
                    onNextDay = viewModel::nextDay,
                    onOpenCalendar = { showDatePicker = true },
                    onOpenBodyEnergyDetails = { onOpenBodyEnergyDetails(state.selectedDate) },
                    onOpenTrainingReadinessDetails = { onOpenTrainingReadinessDetails(state.selectedDate) },
                    onOpenStressDetails = { onOpenStressDetails(state.selectedDate) },
                )
                else -> ErrorMessage(stringResource(R.string.message_no_dashboard_data))
            }
        }

        if (showDatePicker) {
            HealthDatePickerDialog(
                selectedDate = state.selectedDate,
                onDismiss = { showDatePicker = false },
                onConfirm = { date ->
                    showDatePicker = false
                    viewModel.selectDate(date)
                },
            )
        }
    }
}
