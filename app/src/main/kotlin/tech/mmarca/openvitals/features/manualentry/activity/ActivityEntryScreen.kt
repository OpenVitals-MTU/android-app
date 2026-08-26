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
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.AppThemeMode

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
        if (action.needsRecordingPermissions && needsActivityRecordingRuntimePermission(context)) {
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
    // The builder saves by delete-then-insert, so the plan comes back under a
    // new id; re-prefilling from it keeps the session linked to a live record.
    LaunchedEffect(savedWorkoutPlanId) {
        val planId = savedWorkoutPlanId ?: return@LaunchedEffect
        onSavedWorkoutPlanHandled()
        viewModel.reapplyPlan(planId)
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
