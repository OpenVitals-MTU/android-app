package tech.mmarca.openvitals.features.manualentry.activity

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.isDarkTheme
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.rememberManualEntryWritePermissionRequester
import tech.mmarca.openvitals.sensors.ble.hasBluetoothConnectPermission
import tech.mmarca.openvitals.sensors.ble.hasBluetoothScanPermission
import tech.mmarca.openvitals.sensors.ble.recordingRuntimePermissionsToRequest
import tech.mmarca.openvitals.ui.components.AppBarAction
import tech.mmarca.openvitals.ui.components.DeclareAppBar
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.ScreenAppBar
import tech.mmarca.openvitals.ui.theme.recordingOutdoorAccentForAppTheme

@Composable
fun ActivityEntryScreen(
    viewModel: ActivityEntryViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider = DateTimeFormatterProvider(),
    savedWorkoutPlanId: String? = null,
    onSavedWorkoutPlanHandled: () -> Unit = {},
    onOpenWorkoutPlans: () -> Unit = {},
    onOpenWorkoutPlanBuilder: (String) -> Unit = {},
    pendingRouteImportUri: Uri? = null,
    pendingRouteImportRequestId: Long? = null,
    onPendingRouteImportHandled: (Long) -> Unit = {},
    onEntrySaved: () -> Unit = {},
    appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingSourceAction by remember { mutableStateOf<ActivityEntrySourceAction?>(null) }
    var isRecordingFocusMode by rememberSaveable { mutableStateOf(false) }
    var isRecordingOutdoorMode by rememberSaveable { mutableStateOf(false) }
    // The recording dashboard's own arrange toggle, hoisted to this screen so the whole
    // app bar is declared in one place.
    var recordingDashboardEdit by remember { mutableStateOf<RecordingDashboardEdit?>(null) }
    fun setRecordingFocusMode(enabled: Boolean) {
        isRecordingFocusMode = enabled
    }
    fun performSourceAction(action: ActivityEntrySourceAction) {
        when (action) {
            ActivityEntrySourceAction.Manual -> viewModel.startManualEntry()
            ActivityEntrySourceAction.Record -> viewModel.prepareGpsRecording()
            is ActivityEntrySourceAction.LogFromPlan -> viewModel.logFromPlan(action.planId)
            is ActivityEntrySourceAction.StartPlan -> viewModel.prepareGuidedPlan(action.planId)
            is ActivityEntrySourceAction.RepeatPlan -> viewModel.repeatPlan(action.planId)
        }
    }
    val requestRecordingSourcePermissions = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val hasNotificationPermission = grants[Manifest.permission.POST_NOTIFICATIONS] == true ||
            hasActivityRecordingNotificationPermission(context)
        val action = pendingSourceAction
        pendingSourceAction = null
        if (hasNotificationPermission && action != null) {
            performSourceAction(action)
        } else if (action?.needsRecordingPermissions == true) {
            viewModel.reportNotificationPermissionNeeded()
        }
    }
    val requestGpsLocationPermissions = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (!hasActivityRecordingPreciseLocationPermission(context)) {
            viewModel.reportLocationPermissionNeeded()
        }
    }
    val requestActivityRecognitionPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted || ActivityRecordingController.hasActivityRecognitionPermission(context)) {
            viewModel.openRecordingDashboard()
        } else {
            viewModel.reportActivityRecognitionPermissionNeeded()
        }
    }
    fun continueSourceActionAfterWritePermission(action: ActivityEntrySourceAction) {
        val missing = if (action.needsRecordingPermissions) {
            recordingRuntimePermissionsToRequest(
                sdkInt = Build.VERSION.SDK_INT,
                hasNotificationPermission = hasActivityRecordingNotificationPermission(context),
                hasBluetoothConnectPermission = hasBluetoothConnectPermission(context),
                hasBluetoothScanPermission = hasBluetoothScanPermission(context),
                hasSavedBleSensors = viewModel.hasSavedBleSensors,
            )
        } else {
            emptyList()
        }
        if (missing.isNotEmpty()) {
            pendingSourceAction = action
            requestRecordingSourcePermissions.launch(missing.toTypedArray())
        } else {
            performSourceAction(action)
        }
    }
    val requestWritePermissions = rememberManualEntryWritePermissionRequester { grantedPermissions ->
        viewModel.refreshPermission()
        val action = pendingSourceAction
        pendingSourceAction = null
        if (action != null && grantedPermissions.containsAll(state.writePermissions)) {
            continueSourceActionAfterWritePermission(action)
        }
    }
    fun performSourceActionAfterPermission(action: ActivityEntrySourceAction) {
        if (state.canWrite) {
            continueSourceActionAfterWritePermission(action)
        } else {
            pendingSourceAction = action
            requestWritePermissions.launch(state.writePermissions)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermission()
    }
    LaunchedEffect(ActivityEntryUnits.from(unitFormatter)) {
        viewModel.loadEditEntry(ActivityEntryUnits.from(unitFormatter))
    }
    LaunchedEffect(pendingRouteImportRequestId, pendingRouteImportUri, ActivityEntryUnits.from(unitFormatter)) {
        val requestId = pendingRouteImportRequestId
        val uri = pendingRouteImportUri
        if (requestId != null && uri != null) {
            viewModel.importRouteFile(uri, ActivityEntryUnits.from(unitFormatter))
            onPendingRouteImportHandled(requestId)
        }
    }
    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            viewModel.onSaveCompletedHandled()
            onEntrySaved()
        }
    }
    LaunchedEffect(state.pendingBuilderPlanId) {
        val planId = state.pendingBuilderPlanId ?: return@LaunchedEffect
        viewModel.onBuilderNavigationHandled()
        onOpenWorkoutPlanBuilder(planId)
    }
    // The builder hands back the saved plan's id. An edit keeps the id it had.
    LaunchedEffect(savedWorkoutPlanId) {
        val planId = savedWorkoutPlanId ?: return@LaunchedEffect
        onSavedWorkoutPlanHandled()
        viewModel.reapplyPlan(planId)
    }

    if (state.confirmRouteImportOverRecording) {
        AlertDialog(
            onDismissRequest = viewModel::keepRecordingInsteadOfRouteImport,
            title = { Text(stringResource(R.string.activity_entry_route_import_replace_title)) },
            text = { Text(stringResource(R.string.activity_entry_route_import_replace_body)) },
            confirmButton = {
                OpenVitalsTextButton(onClick = viewModel::confirmRouteImportOverRecording) {
                    Text(stringResource(R.string.activity_entry_route_import_replace_confirm))
                }
            },
            dismissButton = {
                OpenVitalsTextButton(onClick = viewModel::keepRecordingInsteadOfRouteImport) {
                    Text(stringResource(R.string.activity_entry_route_import_replace_keep))
                }
            },
        )
    }

    val isRecordingDashboardVisible =
        state.mode == ActivityEntryMode.RECORDING &&
        (recordingState.isActive || recordingState.activityTypeId != null)
    LaunchedEffect(isRecordingDashboardVisible) {
        if (!isRecordingDashboardVisible) {
            setRecordingFocusMode(false)
            isRecordingOutdoorMode = false
        }
    }

    // Outdoor mode repaints the chrome; focus mode takes the whole display.
    val outdoorModeAvailable = isRecordingDashboardVisible && !isRecordingFocusMode
    val outdoorAccent = recordingOutdoorAccentForAppTheme(appThemeMode)
    val editingTint = MaterialTheme.colorScheme.primary
    val recordingTitle = stringResource(R.string.activity_entry_recording_title)
    val outdoorBackground = if (appThemeMode.isDarkTheme(isSystemInDarkTheme())) Color.Black else Color.White
    DeclareAppBar(
        remember(
            isRecordingDashboardVisible,
            isRecordingFocusMode,
            outdoorModeAvailable,
            isRecordingOutdoorMode,
            recordingDashboardEdit,
            outdoorAccent,
            editingTint,
            recordingTitle,
            outdoorBackground,
        ) {
            ScreenAppBar(
                title = recordingTitle.takeIf { isRecordingDashboardVisible },
                actions = buildList {
                    if (outdoorModeAvailable) {
                        add(
                            AppBarAction(
                                icon = if (isRecordingOutdoorMode) Icons.Outlined.LightMode else Icons.Outlined.WbSunny,
                                contentDescription = R.string.cd_toggle_recording_outdoor_mode,
                                tint = if (isRecordingOutdoorMode) outdoorAccent else null,
                                onClick = { isRecordingOutdoorMode = !isRecordingOutdoorMode },
                            ),
                        )
                    }
                    recordingDashboardEdit?.let { edit ->
                        add(
                            AppBarAction(
                                icon = if (edit.isEditing) Icons.Outlined.Check else Icons.Outlined.Edit,
                                contentDescription = if (edit.isEditing) {
                                    R.string.cd_finish_recording_dashboard_editing
                                } else {
                                    R.string.cd_edit_recording_dashboard
                                },
                                tint = if (edit.isEditing) editingTint else null,
                                onClick = edit.onToggle,
                            ),
                        )
                    }
                },
                containerColor = outdoorBackground.takeIf { outdoorModeAvailable && isRecordingOutdoorMode },
                hidesAppBar = isRecordingFocusMode,
            )
        },
    )

    if (isRecordingDashboardVisible) {
        ActivityEntryRecordingContent(
            recordingState = recordingState,
            unitFormatter = unitFormatter,
            viewModel = viewModel,
            isFocusMode = isRecordingFocusMode,
            isOutdoorMode = isRecordingOutdoorMode,
            onFocusModeChanged = ::setRecordingFocusMode,
            onOutdoorModeChanged = { isRecordingOutdoorMode = it },
            onActivityRecordingEditStateChanged = { isAvailable, isEditing, onToggle ->
                recordingDashboardEdit = if (isAvailable) {
                    RecordingDashboardEdit(isEditing, onToggle)
                } else {
                    null
                }
            },
            appThemeMode = appThemeMode,
        )
    } else {
        ActivityEntryFormContent(
            state = state,
            recordingState = recordingState,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onPerformSourceActionAfterPermission = ::performSourceActionAfterPermission,
            onRequestGpsLocationPermissions = {
                requestGpsLocationPermissions.launch(activityRecordingLocationPermissions())
            },
            onRequestActivityRecognitionPermission = {
                requestActivityRecognitionPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            },
            onRequestWritePermissions = {
                requestWritePermissions.launch(state.writePermissions)
            },
            onOpenWorkoutPlans = onOpenWorkoutPlans,
            onOpenWorkoutPlanBuilder = onOpenWorkoutPlanBuilder,
            viewModel = viewModel,
        )
    }
}

/** What the start hub asked for; each goes through the write-permission gate first. */
sealed interface ActivityEntrySourceAction {
    data object Manual : ActivityEntrySourceAction
    data object Record : ActivityEntrySourceAction
    data class LogFromPlan(val planId: String) : ActivityEntrySourceAction
    data class StartPlan(val planId: String) : ActivityEntrySourceAction
    data class RepeatPlan(val planId: String) : ActivityEntrySourceAction

    /** Recording (plain or guided) runs a foreground service, so it needs the notification permission. */
    val needsRecordingPermissions: Boolean
        get() = this is Record || this is StartPlan || this is RepeatPlan
}

/** The recording dashboard's arrange toggle, as its screen root sees it. */
private data class RecordingDashboardEdit(
    val isEditing: Boolean,
    val onToggle: () -> Unit,
)
