package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.AppThemeMode

@Composable
fun ActivityEntryScreen(
    viewModel: ActivityEntryViewModel,
    unitFormatter: UnitFormatter,
    pendingRouteImportUri: Uri? = null,
    pendingRouteImportRequestId: Long? = null,
    onPendingRouteImportHandled: (Long) -> Unit = {},
    onEntrySaved: () -> Unit = {},
    onActivityRecordingTitleChanged: (Int?) -> Unit = {},
    onActivityRecordingEditStateChanged: (Boolean, Boolean, () -> Unit) -> Unit = { _, _, _ -> },
    onActivityRecordingFocusModeChanged: (Boolean) -> Unit = {},
    onActivityRecordingOutdoorModeStateChanged: (Boolean, Boolean, () -> Unit) -> Unit = { _, _, _ -> },
    appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingSourceAction by remember { mutableStateOf<ActivityEntrySourceAction?>(null) }
    var isRecordingFocusMode by rememberSaveable { mutableStateOf(false) }
    var isRecordingOutdoorMode by rememberSaveable { mutableStateOf(false) }
    fun setRecordingFocusMode(enabled: Boolean) {
        isRecordingFocusMode = enabled
    }
    LaunchedEffect(isRecordingFocusMode) {
        onActivityRecordingFocusModeChanged(isRecordingFocusMode)
    }
    fun performSourceAction(action: ActivityEntrySourceAction) {
        when (action) {
            ActivityEntrySourceAction.MANUAL -> viewModel.startManualEntry()
            ActivityEntrySourceAction.EXISTING_PLAN -> viewModel.startFromExistingPlan()
            ActivityEntrySourceAction.RECORD_GPS -> viewModel.prepareGpsRecording()
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
        } else if (action == ActivityEntrySourceAction.RECORD_GPS) {
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
        if (action == ActivityEntrySourceAction.RECORD_GPS && needsActivityRecordingRuntimePermission(context)) {
            pendingSourceAction = action
            requestRecordingSourcePermissions.launch(activityRecordingRuntimePermissions())
        } else {
            performSourceAction(action)
        }
    }
    val requestWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { grantedPermissions ->
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
    LaunchedEffect(unitFormatter.unitSystem()) {
        viewModel.loadEditEntry(unitFormatter.unitSystem())
    }
    LaunchedEffect(pendingRouteImportRequestId, pendingRouteImportUri, unitFormatter.unitSystem()) {
        val requestId = pendingRouteImportRequestId
        val uri = pendingRouteImportUri
        if (requestId != null && uri != null) {
            viewModel.importRouteFile(uri, unitFormatter.unitSystem())
            onPendingRouteImportHandled(requestId)
        }
    }
    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            viewModel.onSaveCompletedHandled()
            onEntrySaved()
        }
    }

    val isRecordingDashboardVisible =
        state.mode == ActivityEntryMode.RECORDING &&
        (recordingState.isActive || recordingState.activityTypeId != null)
    LaunchedEffect(isRecordingDashboardVisible, isRecordingFocusMode, isRecordingOutdoorMode) {
        onActivityRecordingOutdoorModeStateChanged(
            isRecordingDashboardVisible && !isRecordingFocusMode,
            isRecordingOutdoorMode,
        ) {
            isRecordingOutdoorMode = !isRecordingOutdoorMode
        }
    }
    LaunchedEffect(isRecordingDashboardVisible) {
        if (!isRecordingDashboardVisible) {
            setRecordingFocusMode(false)
            isRecordingOutdoorMode = false
            onActivityRecordingTitleChanged(null)
            onActivityRecordingEditStateChanged(false, false) {}
            onActivityRecordingOutdoorModeStateChanged(false, false) {}
        }
    }

    if (isRecordingDashboardVisible) {
        ActivityEntryRecordingContent(
            recordingState = recordingState,
            unitFormatter = unitFormatter,
            viewModel = viewModel,
            isFocusMode = isRecordingFocusMode,
            isOutdoorMode = isRecordingOutdoorMode,
            onFocusModeChanged = ::setRecordingFocusMode,
            onOutdoorModeChanged = { isRecordingOutdoorMode = it },
            onActivityRecordingTitleChanged = onActivityRecordingTitleChanged,
            onActivityRecordingEditStateChanged = onActivityRecordingEditStateChanged,
            appThemeMode = appThemeMode,
        )
    } else {
        ActivityEntryFormContent(
            state = state,
            recordingState = recordingState,
            unitFormatter = unitFormatter,
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
            viewModel = viewModel,
        )
    }
}
