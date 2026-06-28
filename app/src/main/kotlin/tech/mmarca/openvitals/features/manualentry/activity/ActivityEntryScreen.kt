package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    val importRouteFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importRouteFile(uri, unitFormatter.unitSystem())
        }
    }
    fun performSourceAction(action: ActivityEntrySourceAction) {
        when (action) {
            ActivityEntrySourceAction.MANUAL -> viewModel.startManualEntry()
            ActivityEntrySourceAction.EXISTING_PLAN -> viewModel.startFromExistingPlan()
            ActivityEntrySourceAction.IMPORT_ROUTE_FILE -> importRouteFile.launch(RouteImportMimeTypes)
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
        ActivityRecordingScreen(
            state = recordingState,
            unitFormatter = unitFormatter,
            onStartRecording = viewModel::startGpsRecording,
            onPauseRecording = viewModel::pauseGpsRecording,
            onResumeRecording = viewModel::resumeGpsRecording,
            onAddLap = viewModel::addRecordingLap,
            onAddMarker = viewModel::addRecordingMarker,
            onUpdateMarker = viewModel::updateRecordingMarker,
            onDeleteMarker = viewModel::deleteRecordingMarker,
            onUpdateDashboardLayout = viewModel::updateRecordingDashboardLayout,
            onChooseSource = viewModel::chooseSource,
            onAdjustRepetitionCount = viewModel::adjustRepetitionRecording,
            onEndRepetitionSet = viewModel::endRepetitionSet,
            onStartNextRepetitionSet = viewModel::startNextRepetitionSet,
            onFinishRecording = {
                viewModel.finishGpsRecording(unitFormatter.unitSystem())
            },
            onActivityRecordingTitleChanged = onActivityRecordingTitleChanged,
            onDashboardEditStateChanged = onActivityRecordingEditStateChanged,
            isFocusMode = isRecordingFocusMode,
            onFocusModeChanged = ::setRecordingFocusMode,
            isOutdoorMode = isRecordingOutdoorMode,
            onOutdoorModeChanged = { isRecordingOutdoorMode = it },
            appThemeMode = appThemeMode,
            modifier = if (isRecordingFocusMode || isRecordingOutdoorMode) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            },
        )
    } else {
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            item {
                if (state.mode == ActivityEntryMode.RECORDING) {
                    ActivityRecordingSetupScreen(
                        state = state,
                        recordingState = recordingState,
                        unitFormatter = unitFormatter,
                        onSelectActivityType = viewModel::selectActivityType,
                        onStartRecording = { _, restSeconds ->
                            viewModel.openRecordingDashboard(restSeconds)
                        },
                        onRequestLocationPermission = {
                            requestGpsLocationPermissions.launch(activityRecordingLocationPermissions())
                        },
                        onRequestActivityRecognitionPermission = {
                            requestActivityRecognitionPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        },
                        onChooseSource = viewModel::chooseSource,
                        onRequestWritePermission = {
                            requestWritePermissions.launch(state.writePermissions)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else if (state.mode == ActivityEntryMode.CHOOSE_SOURCE) {
                    ActivityEntrySourceCard(
                        state = state,
                        onStartManualEntry = {
                            performSourceActionAfterPermission(ActivityEntrySourceAction.MANUAL)
                        },
                        onCreateFromExistingPlan = {
                            performSourceActionAfterPermission(ActivityEntrySourceAction.EXISTING_PLAN)
                        },
                        onImportRouteFile = {
                            performSourceActionAfterPermission(ActivityEntrySourceAction.IMPORT_ROUTE_FILE)
                        },
                        onRecordGpsActivity = {
                            performSourceActionAfterPermission(ActivityEntrySourceAction.RECORD_GPS)
                        },
                        onRequestWritePermission = {
                            requestWritePermissions.launch(state.writePermissions)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else if (state.mode == ActivityEntryMode.PLAN_ACTIVITY_PICKER) {
                    ActivityPlanActivityPickerCard(
                        state = state,
                        onSelectActivity = viewModel::selectPlannedWorkoutActivity,
                        onChooseSource = viewModel::chooseSource,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else if (state.mode == ActivityEntryMode.PLAN_PICKER) {
                    ActivityPlanPickerCard(
                        state = state,
                        onSelectPlan = viewModel::applyPlannedWorkout,
                        onChooseActivity = viewModel::choosePlannedWorkoutActivity,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    ActivityEntryCard(
                        state = state,
                        unitFormatter = unitFormatter,
                        onSelectActivityType = viewModel::selectActivityType,
                        onTitleChanged = viewModel::updateTitle,
                        onFeelingChanged = viewModel::updateFeeling,
                        onNotesChanged = viewModel::updateNotes,
                        onStartDateChanged = viewModel::updateStartDate,
                        onStartTimeChanged = viewModel::updateStartTime,
                        onDurationChanged = viewModel::updateDurationMinutes,
                        onRepetitionModeChanged = viewModel::updateRepetitionMode,
                        onRepetitionTotalChanged = viewModel::updateRepetitionTotal,
                        onRepetitionSetRepetitionsChanged = viewModel::updateRepetitionSetRepetitions,
                        onRepetitionSetRestChanged = viewModel::updateRepetitionSetRest,
                        onAddRepetitionSet = viewModel::addRepetitionSet,
                        onRemoveRepetitionSet = viewModel::removeRepetitionSet,
                        onCreateNewPlannedWorkout = viewModel::createNewPlannedWorkout,
                        onApplyPlannedWorkout = viewModel::applyPlannedWorkout,
                        onSavePlannedWorkout = {
                            viewModel.saveCurrentAsPlannedWorkout(unitFormatter.unitSystem())
                        },
                        onUpdatePlannedWorkout = {
                            viewModel.saveCurrentAsPlannedWorkout(unitFormatter.unitSystem(), updateSelected = true)
                        },
                        onDistanceChanged = viewModel::updateDistance,
                        onElevationChanged = viewModel::updateElevation,
                        onActiveCaloriesChanged = viewModel::updateActiveCalories,
                        onTotalCaloriesChanged = viewModel::updateTotalCalories,
                        onClearRoute = viewModel::clearImportedRoute,
                        onChooseSource = viewModel::chooseSource,
                        onRequestWritePermission = {
                            requestWritePermissions.launch(state.writePermissions)
                        },
                        onAddEntry = {
                            viewModel.addEntry(unitFormatter.unitSystem())
                        },
                        onDiscardRecordingDraft = viewModel::discardRecordingDraft,
                        isEditMode = state.isEditMode,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
