package tech.mmarca.openvitals.features.dashboard

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.rememberHealthConnectPermissionLauncher
import tech.mmarca.openvitals.ui.components.shouldShowDashboardHealthConnectPromo
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    refreshRequest: Int = 0,
    onOpenMetric: (DashboardWidgetId) -> Unit,
    onOpenActivities: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit = {},
    onOpenLog: () -> Unit,
    onStartActivity: () -> Unit,
    onOpenDeviceStatus: () -> Unit,
    onSensorStatusVisibilityChanged: (Boolean) -> Unit = {},
) {
    val uiState = viewModel.uiState
    val isLoading by remember(viewModel) { uiState.map { it.isLoading } }
        .collectAsStateWithLifecycle(initialValue = true)
    val error by remember(viewModel) { uiState.map { it.error } }
        .collectAsStateWithLifecycle(initialValue = null)
    val display by remember(viewModel) { uiState.map { it.display } }
        .collectAsStateWithLifecycle(initialValue = DashboardDisplayState())
    val dashboardData by remember(viewModel) { uiState.map { it.data } }
        .collectAsStateWithLifecycle(initialValue = null)
    val selectedDate by remember(viewModel) { uiState.map { it.selectedDate } }
        .collectAsStateWithLifecycle(initialValue = LocalDate.now())
    val state by uiState.collectAsStateWithLifecycle()
    val loadedData = dashboardData
    var showDatePicker by remember { mutableStateOf(false) }
    val showPromo = shouldShowDashboardHealthConnectPromo(
        availability = state.healthConnectAvailability,
        syncEnabled = state.healthConnectSyncEnabled,
        minimumPermissionsGranted = state.minimumPermissionsGranted,
    )
    var permissionReloadKey by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberHealthConnectPermissionLauncher(
        onResult = {
            permissionReloadKey++
            viewModel.refresh()
        },
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentDay()
    }
    androidx.compose.runtime.LaunchedEffect(refreshRequest) {
        if (refreshRequest > 0) {
            viewModel.refresh()
        }
    }
    val context = LocalContext.current
    val errorMessage = error?.resolve()
    androidx.compose.runtime.LaunchedEffect(errorMessage) {
        // Refresh failures (e.g. Health Connect rate limiting) are shown as a transient
        // toast rather than the full-screen ErrorMessage, since the dashboard already
        // has data to display and shouldn't be replaced by an error state.
        if (errorMessage != null && loadedData != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    androidx.compose.runtime.LaunchedEffect(state.sensorStatus.hasDevices) {
        onSensorStatusVisibilityChanged(state.sensorStatus.hasDevices)
    }
    DisposableEffect(Unit) {
        onDispose { onSensorStatusVisibilityChanged(false) }
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.DASHBOARD,
        isLoading = isLoading && loadedData != null,
        refreshKey = refreshRequest to permissionReloadKey,
    ) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing && loadedData != null,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                isLoading && loadedData == null -> FullScreenLoading()
                error != null && loadedData == null ->
                    ErrorMessage(error?.resolve() ?: stringResource(R.string.unknown_error))
                loadedData != null -> DashboardContent(
                    data = loadedData,
                    display = display,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    canGoForward = selectedDate.isBefore(LocalDate.now()),
                    unacknowledgedWidgetPermissions = state.unacknowledgedWidgetPermissions,
                    showHealthConnectPromo = showPromo,
                    healthConnectAvailability = state.healthConnectAvailability,
                    healthConnectSyncEnabled = state.healthConnectSyncEnabled,
                    dashboardWidgets = state.dashboardWidgets,
                    sensorStatus = state.sensorStatus,
                    isEditingDashboard = state.isEditingDashboard,
                    onPreviousDay = viewModel::previousDay,
                    onNextDay = viewModel::nextDay,
                    onOpenCalendar = { showDatePicker = true },
                    onGrantWidgetPermissions = {
                        permissionLauncher.launch(state.unacknowledgedWidgetPermissions)
                    },
                    onDismissWidgetPermissions = viewModel::acknowledgeWidgetMissingPermissions,
                    onMoveWidgetToTarget = viewModel::moveDashboardWidgetToTarget,
                    onRemoveWidget = viewModel::removeDashboardWidget,
                    onAddWidget = viewModel::addDashboardWidget,
                    onOpenMetric = onOpenMetric,
                    onOpenActivities = onOpenActivities,
                    onOpenActivity = onOpenActivity,
                    onEditActivity = onEditActivity,
                    onDeleteActivity = viewModel::deleteActivityEntry,
                    onOpenLog = onOpenLog,
                    onStartActivity = onStartActivity,
                    onOpenDeviceStatus = onOpenDeviceStatus,
                    onToggleDashboardEdit = viewModel::toggleDashboardEdit,
                    onHealthConnectPromoAction = {
                        permissionLauncher.launch(viewModel.minimumOnboardingPermissions)
                    },
                )
                else -> ErrorMessage(stringResource(R.string.message_no_dashboard_data))
            }
        }
    }

    if (showDatePicker) {
        HealthDatePickerDialog(
            selectedDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                showDatePicker = false
                viewModel.selectDate(date)
            },
        )
    }
}
