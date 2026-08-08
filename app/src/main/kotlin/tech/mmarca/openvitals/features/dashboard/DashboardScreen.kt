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
    // One collection, seeded from the flow's CURRENT value: coming back from a
    // detail screen must render the dashboard on its first frame. Mapped flows
    // with placeholder initial values put a loading screen there instead, and
    // that single frame threw away every saveable below it — the carousel page
    // and the scroll position both snapped back to the top.
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = state.isLoading
    val error = state.error
    val display = state.display
    val selectedDate = state.selectedDate
    val loadedData = state.data
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
        // The dashboard body renders the sync banner itself, inside its list —
        // the shell rendering a second one on top is how the screen ended up
        // with two "Syncing…" banners for a single load.
        showInlineSyncBanner = false,
    ) { hcUx ->
        // One reload, one indicator: the spinner answers the pull gesture,
        // and every refresh the user did not pull for speaks through the
        // banner inside the list instead.
        var pullRefreshRequested by remember { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(state.isRefreshing) {
            if (!state.isRefreshing) pullRefreshRequested = false
        }
        PullToRefreshBox(
            isRefreshing = pullRefreshRequested && state.isRefreshing && loadedData != null,
            onRefresh = {
                pullRefreshRequested = true
                viewModel.refresh()
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                isLoading && loadedData == null -> FullScreenLoading()
                error != null && loadedData == null ->
                    ErrorMessage(error?.resolve() ?: stringResource(R.string.unknown_error))
                loadedData != null -> DashboardContent(
                    data = loadedData,
                    selectedDate = selectedDate,
                    display = display,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    canGoForward = selectedDate.isBefore(LocalDate.now()),
                    isRefreshing = state.isRefreshing && !pullRefreshRequested,
                    syncPaused = hcUx.syncPaused,
                    dashboardWidgets = state.dashboardWidgets,
                    isEditingDashboard = state.isEditingDashboard,
                    sortEmptyTilesLast = state.sortEmptyTilesLast,
                    onPreviousDay = viewModel::previousDay,
                    onNextDay = viewModel::nextDay,
                    onOpenCalendar = { showDatePicker = true },
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
                    onToggleDashboardEdit = viewModel::toggleDashboardEdit,
                    placedWidgetIds = state.placedDashboardWidgets,
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
