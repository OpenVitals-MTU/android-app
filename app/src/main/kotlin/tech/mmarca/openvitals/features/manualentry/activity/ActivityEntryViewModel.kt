package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import android.location.Location
import android.net.Uri
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.PlannedExerciseStep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.data.repository.ActivityMarkerRepository
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.model.ActivityRecordingLap
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.ExerciseLapData
import tech.mmarca.openvitals.domain.model.PlannedExerciseBlockData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.PlannedExerciseWriteRequest
import tech.mmarca.openvitals.navigation.ACTIVITY_ENTRY_ID_ARG

@HiltViewModel
class ActivityEntryViewModel(
    private val repository: ActivityRepository,
    private val routeFileImporter: RouteFileImporter? = null,
    private val activityRecorder: ActivityRecordingController? = null,
    private val recordingDraftStore: ActivityRecordingDraftStore? = null,
    private val preferencesRepository: PreferencesRepository? = null,
    private val markerRepository: ActivityMarkerRepository? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val editActivityId: String? = null,
) : ViewModel() {

    @Inject
    constructor(
        repository: ActivityRepository,
        routeFileImporter: RouteFileImporter,
        activityRecorder: ActivityRecordingController,
        recordingDraftStore: ActivityRecordingDraftStore,
        preferencesRepository: PreferencesRepository,
        markerRepository: ActivityMarkerRepository,
        savedStateHandle: SavedStateHandle,
    ) : this(
        repository = repository,
        routeFileImporter = routeFileImporter,
        activityRecorder = activityRecorder,
        recordingDraftStore = recordingDraftStore,
        preferencesRepository = preferencesRepository,
        markerRepository = markerRepository,
        clock = Clock.systemDefaultZone(),
        editActivityId = savedStateHandle[ACTIVITY_ENTRY_ID_ARG],
    )

    private var editEntryLoaded = false

    private val _uiState = MutableStateFlow(
        initialState(recordingDraftStore?.restore())
    )
    val uiState: StateFlow<ActivityEntryUiState> = _uiState.asStateFlow()
    private val fallbackRecordingState = MutableStateFlow(ActivityRecordingState())
    val recordingState: StateFlow<ActivityRecordingState> =
        activityRecorder?.state ?: fallbackRecordingState.asStateFlow()

    init {
        refreshPermission()
        activityRecorder?.state
            ?.onEach { recording ->
                if (recording.isActive) {
                    applyRecordingProgress(recording)
                }
            }
            ?.launchIn(viewModelScope)
    }

    private fun initialState(recordingDraft: ActivityEntryUiState?): ActivityEntryUiState {
        if (editActivityId == null && recordingDraft?.isRecordingDraft == true) {
            return recordingDraft.copy(
                writePermissions = repository.activityWritePermissions(),
                canWrite = false,
                isCheckingPermission = true,
                isSavingEntry = false,
                isImportingRoute = false,
                entryError = null,
                detailMessage = null,
                validationErrors = emptySet(),
                editRecordId = null,
                saveCompleted = false,
            )
        }

        return initialActivityEntryState(clock, repository, preferredActivityType()).copy(
            mode = if (editActivityId == null) ActivityEntryMode.CHOOSE_SOURCE else ActivityEntryMode.MANUAL,
            editRecordId = editActivityId,
        )
    }

    override fun onCleared() {
        recordingDraftStore?.store(_uiState.value)
    }

    fun refreshPermission() {
        val permissions = currentRequiredPermissions()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingPermission = true,
                writePermissions = permissions,
                detailMessage = null,
            )
            runCatching {
                repository.hasActivityWritePermission()
            }.onSuccess { canWrite ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    canWrite = canWrite,
                    writePermissions = currentRequiredPermissions(),
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCheckingPermission = false,
                    canWrite = false,
                    entryError = ActivityEntryError.WRITE_FAILED,
                    detailMessage = error.message,
                    writePermissions = currentRequiredPermissions(),
                )
            }
        }
    }

    fun selectActivityType(type: ActivityEntryType) {
        val currentState = _uiState.value
        val retainedRoute = currentState.importedRoute?.takeIf { type.supportsGpsRoute }
        _uiState.value = currentState.copy(
            selectedActivityType = type,
            plannedWorkouts = emptyList(),
            selectedPlannedWorkoutId = null,
            selectedPlannedWorkoutActivityTypeId = null,
            distanceText = currentState.distanceText.takeIf { type.supportsDistance }.orEmpty(),
            elevationText = currentState.elevationText.takeIf { type.supportsElevation }.orEmpty(),
            importedRoute = retainedRoute,
            recordedPauseIntervals = if (retainedRoute == null) emptyList() else currentState.recordedPauseIntervals,
            recordedLaps = if (retainedRoute == null) emptyList() else currentState.recordedLaps,
            recordedMarkers = if (retainedRoute == null) emptyList() else currentState.recordedMarkers,
            mode = if (retainedRoute == null && currentState.mode == ActivityEntryMode.ROUTE_IMPORT) {
                ActivityEntryMode.MANUAL
            } else {
                currentState.mode
            },
            entryError = null,
            detailMessage = null,
            validationErrors = emptySet(),
        )
        refreshPermission()
        refreshPlannedWorkouts()
    }

    fun startManualEntry() {
        recordingDraftStore?.clear()
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.MANUAL,
            selectedPlannedWorkoutId = null,
            selectedPlannedWorkoutActivityTypeId = null,
            importedRoute = null,
            recordedPauseIntervals = emptyList(),
            recordedLaps = emptyList(),
            recordedMarkers = emptyList(),
            isRecordingDraft = false,
            entryError = null,
            detailMessage = null,
            validationErrors = emptySet(),
        )
        refreshPermission()
        refreshPlannedWorkouts()
    }

    fun startFromExistingPlan() {
        recordingDraftStore?.clear()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                mode = ActivityEntryMode.PLAN_ACTIVITY_PICKER,
                plannedWorkouts = emptyList(),
                selectedPlannedWorkoutId = null,
                selectedPlannedWorkoutActivityTypeId = null,
                isLoadingPlannedWorkouts = true,
                isRecordingDraft = false,
                entryError = null,
                detailMessage = null,
                validationErrors = emptySet(),
            )
            runCatching {
                repository.loadExistingPlannedWorkouts(LocalDate.now(clock))
            }.onSuccess { plans ->
                _uiState.value = _uiState.value.copy(
                    plannedWorkouts = plans,
                    isLoadingPlannedWorkouts = false,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    plannedWorkouts = emptyList(),
                    isLoadingPlannedWorkouts = false,
                    writePermissions = repository.plannedWorkoutWritePermissions(),
                    canWrite = false,
                    entryError = if (error is SecurityException) {
                        ActivityEntryError.MISSING_WRITE_PERMISSION
                    } else {
                        ActivityEntryError.WRITE_FAILED
                    },
                    detailMessage = error.message,
                )
            }
        }
    }

    fun selectPlannedWorkoutActivity(typeId: String) {
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.PLAN_PICKER,
            selectedPlannedWorkoutActivityTypeId = typeId,
            selectedPlannedWorkoutId = null,
            entryError = null,
            detailMessage = null,
        )
    }

    fun choosePlannedWorkoutActivity() {
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.PLAN_ACTIVITY_PICKER,
            selectedPlannedWorkoutActivityTypeId = null,
            selectedPlannedWorkoutId = null,
            entryError = null,
            detailMessage = null,
        )
    }

    fun chooseSource() {
        if (_uiState.value.isEditMode) return
        recordingDraftStore?.clear()
        _uiState.value = initialActivityEntryState(clock, repository, preferredActivityType()).copy(
            canWrite = _uiState.value.canWrite,
            isCheckingPermission = _uiState.value.isCheckingPermission,
            editRecordId = editActivityId,
        )
        refreshPermission()
    }

    fun updateTitle(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.TITLE)) {
            copy(titleText = text, entryError = null, detailMessage = null)
        }
    }

    fun updateNotes(text: String) {
        updateState { copy(notesText = text, entryError = null, detailMessage = null) }
    }

    fun updateStartDate(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.START_DATE, ActivityEntryField.START_TIME)) {
            copy(startDateText = text, selectedPlannedWorkoutId = null, entryError = null, detailMessage = null)
        }
        refreshPlannedWorkouts()
    }

    fun updateStartTime(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.START_TIME)) {
            copy(startTimeText = text, entryError = null, detailMessage = null)
        }
    }

    fun updateDurationMinutes(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.DURATION)) {
            copy(durationMinutesText = text, entryError = null, detailMessage = null)
        }
    }

    fun updateDistance(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.DISTANCE)) {
            copy(distanceText = text, entryError = null, detailMessage = null)
        }
    }

    fun updateElevation(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.ELEVATION)) {
            copy(elevationText = text, entryError = null, detailMessage = null)
        }
    }

    fun updateActiveCalories(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.ACTIVE_CALORIES, ActivityEntryField.TOTAL_CALORIES)) {
            copy(activeCaloriesText = text, entryError = null, detailMessage = null)
        }
    }

    fun updateTotalCalories(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.TOTAL_CALORIES)) {
            copy(totalCaloriesText = text, entryError = null, detailMessage = null)
        }
    }

    fun updateRepetitionMode(mode: ActivityRepetitionEntryMode) {
        updateState(clearFields = setOf(ActivityEntryField.REPETITIONS)) {
            copy(repetitionMode = mode, entryError = null, detailMessage = null)
        }
    }

    fun updateRepetitionTotal(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.REPETITIONS)) {
            copy(repetitionTotalText = text, entryError = null, detailMessage = null)
        }
    }

    fun updateRepetitionSetRepetitions(index: Int, text: String) {
        updateState(clearFields = setOf(ActivityEntryField.REPETITIONS)) {
            copy(
                repetitionSets = repetitionSets.mapIndexed { itemIndex, item ->
                    if (itemIndex == index) item.copy(repetitionsText = text) else item
                },
                entryError = null,
                detailMessage = null,
            )
        }
    }

    fun updateRepetitionSetRest(index: Int, text: String) {
        updateState(clearFields = setOf(ActivityEntryField.REPETITIONS)) {
            copy(
                repetitionSets = repetitionSets.mapIndexed { itemIndex, item ->
                    if (itemIndex == index) item.copy(restMinutesText = text) else item
                },
                entryError = null,
                detailMessage = null,
            )
        }
    }

    fun addRepetitionSet() {
        updateState(clearFields = setOf(ActivityEntryField.REPETITIONS)) {
            copy(
                repetitionMode = ActivityRepetitionEntryMode.SETS,
                repetitionSets = repetitionSets + ActivityRepetitionSetInput(),
                entryError = null,
                detailMessage = null,
            )
        }
    }

    fun removeRepetitionSet(index: Int) {
        updateState(clearFields = setOf(ActivityEntryField.REPETITIONS)) {
            copy(
                repetitionSets = repetitionSets
                    .filterIndexed { itemIndex, _ -> itemIndex != index }
                    .ifEmpty { listOf(ActivityRepetitionSetInput()) },
                entryError = null,
                detailMessage = null,
            )
        }
    }

    fun importRouteFile(uri: Uri, unitSystem: UnitSystem) {
        val importer = routeFileImporter
        if (importer == null) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.ROUTE_IMPORT_FAILED,
                detailMessage = "Route file import is not available.",
                validationErrors = emptySet(),
            )
            return
        }
        if (!_uiState.value.selectedActivityType.supportsGpsRoute) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.INVALID_VALUE,
                detailMessage = "Selected activity type does not support GPS routes.",
                validationErrors = setOf(ActivityEntryValidationError.ACTIVITY_TYPE_DOES_NOT_SUPPORT_ROUTE),
            )
            return
        }

        recordingDraftStore?.clear()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImportingRoute = true,
                isRecordingDraft = false,
                entryError = null,
                detailMessage = null,
                validationErrors = emptySet(),
            )
            runCatching { importer.import(uri) }
                .onSuccess { routeImport ->
                    applyRouteImport(routeImport, unitSystem)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isImportingRoute = false,
                        entryError = ActivityEntryError.ROUTE_IMPORT_FAILED,
                        detailMessage = error.message,
                        validationErrors = emptySet(),
                    )
                }
        }
    }

    fun refreshPlannedWorkouts() {
        val snapshot = _uiState.value
        if (!snapshot.selectedActivityType.supportsSetRepetitions) {
            _uiState.value = snapshot.copy(
                plannedWorkouts = emptyList(),
                selectedPlannedWorkoutId = null,
                isLoadingPlannedWorkouts = false,
            )
            return
        }
        val date = snapshot.startDateText.trim().let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPlannedWorkouts = true)
            runCatching {
                repository.loadPlannedWorkoutOptions(date, snapshot.selectedActivityType.exerciseType)
            }.onSuccess { plans ->
                val currentSelectedId = _uiState.value.selectedPlannedWorkoutId
                val selectedId = currentSelectedId?.takeIf { selected ->
                    plans.isEmpty() || plans.any { it.id == selected }
                }
                _uiState.value = _uiState.value.copy(
                    plannedWorkouts = plans,
                    selectedPlannedWorkoutId = selectedId,
                    isLoadingPlannedWorkouts = false,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    plannedWorkouts = emptyList(),
                    selectedPlannedWorkoutId = null,
                    isLoadingPlannedWorkouts = false,
                    detailMessage = error.message,
                )
            }
        }
    }

    fun applyPlannedWorkout(planId: String) {
        val plan = _uiState.value.plannedWorkouts.firstOrNull { it.id == planId } ?: return
        val sets = plan.toRepetitionSetInputs()
        if (sets.isEmpty()) return
        val activityType = plan.toActivityEntryType() ?: _uiState.value.selectedActivityType
        val planStart = plan.startTime.atZone(clock.zone)
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.MANUAL,
            selectedActivityType = activityType,
            selectedPlannedWorkoutId = plan.id,
            selectedPlannedWorkoutActivityTypeId = activityType.id,
            titleText = plan.title.orEmpty(),
            notesText = plan.notes.orEmpty(),
            startDateText = DateTimeFormatter.ISO_LOCAL_DATE.format(planStart),
            startTimeText = TimeFormatter.format(planStart.toLocalTime()),
            durationMinutesText = plan.durationMinutesText(),
            repetitionMode = ActivityRepetitionEntryMode.SETS,
            repetitionTotalText = "",
            repetitionSets = sets,
            entryError = null,
            detailMessage = null,
            validationErrors = emptySet(),
        )
    }

    fun createNewPlannedWorkout() {
        val current = _uiState.value
        _uiState.value = current.copy(
            selectedPlannedWorkoutId = null,
            selectedPlannedWorkoutActivityTypeId = current.selectedActivityType.id,
            titleText = "",
            notesText = "",
            durationMinutesText = "30",
            repetitionMode = ActivityRepetitionEntryMode.SETS,
            repetitionTotalText = "",
            repetitionSets = listOf(ActivityRepetitionSetInput()),
            entryError = null,
            detailMessage = null,
            validationErrors = emptySet(),
        )
    }

    fun saveCurrentAsPlannedWorkout(unitSystem: UnitSystem, updateSelected: Boolean = false) {
        val current = _uiState.value
        val validationErrors = validatePlannedExerciseWriteRequest(current, unitSystem)
        val request = buildPlannedExerciseWriteRequest(
            state = current,
            unitSystem = unitSystem,
            updateExistingId = current.selectedPlannedWorkoutId.takeIf { updateSelected },
        )
        if (validationErrors.isNotEmpty() || request == null) {
            _uiState.value = current.copy(
                entryError = ActivityEntryError.INVALID_VALUE,
                detailMessage = null,
                validationErrors = validationErrors,
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingPlannedWorkout = true,
                entryError = null,
                detailMessage = null,
            )
            runCatching {
                repository.writePlannedWorkout(request)
            }.onSuccess { savedPlanId ->
                _uiState.value = _uiState.value.copy(
                    selectedPlannedWorkoutId = savedPlanId,
                    isSavingPlannedWorkout = false,
                    detailMessage = null,
                )
                refreshPlannedWorkouts()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingPlannedWorkout = false,
                    writePermissions = repository.plannedWorkoutWritePermissions(),
                    canWrite = false,
                    entryError = if (error is SecurityException) {
                        ActivityEntryError.MISSING_WRITE_PERMISSION
                    } else {
                        ActivityEntryError.WRITE_FAILED
                    },
                    detailMessage = error.message,
                )
            }
        }
    }

    fun clearImportedRoute() {
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.MANUAL,
            importedRoute = null,
            recordedPauseIntervals = emptyList(),
            recordedLaps = emptyList(),
            recordedMarkers = emptyList(),
            entryError = null,
            detailMessage = null,
            validationErrors = emptySet(),
        )
        refreshPermission()
    }

    fun reportLocationPermissionNeeded() {
        _uiState.value = _uiState.value.copy(
            entryError = ActivityEntryError.LOCATION_PERMISSION_NEEDED,
            detailMessage = null,
            validationErrors = emptySet(),
        )
    }

    fun reportNotificationPermissionNeeded() {
        _uiState.value = _uiState.value.copy(
            entryError = ActivityEntryError.NOTIFICATION_PERMISSION_NEEDED,
            detailMessage = null,
            validationErrors = emptySet(),
        )
    }

    fun reportActivityRecognitionPermissionNeeded() {
        _uiState.value = _uiState.value.copy(
            entryError = ActivityEntryError.ACTIVITY_RECOGNITION_PERMISSION_NEEDED,
            detailMessage = null,
            validationErrors = emptySet(),
        )
    }

    fun prepareGpsRecording() {
        val currentState = _uiState.value

        recordingDraftStore?.clear()
        _uiState.value = currentState.copy(
            mode = ActivityEntryMode.RECORDING,
            selectedActivityType = preferredActivityType(requireLiveRecording = true),
            importedRoute = null,
            recordedPauseIntervals = emptyList(),
            recordedLaps = emptyList(),
            recordedMarkers = emptyList(),
            isRecordingDraft = false,
            distanceText = "",
            elevationText = "",
            entryError = null,
            detailMessage = null,
            validationErrors = emptySet(),
        )
        refreshPermission()
    }

    fun startGpsRecording(
        initialFix: Location? = null,
        repetitionRestSeconds: Long = 0L,
    ) {
        val recorder = activityRecorder
        if (recorder == null) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.RECORDING_FAILED,
                detailMessage = "GPS recording is not available.",
                validationErrors = emptySet(),
            )
            return
        }
        val currentState = _uiState.value
        if (!currentState.selectedActivityType.supportsLiveRecording) {
            _uiState.value = currentState.copy(
                entryError = ActivityEntryError.INVALID_VALUE,
                detailMessage = null,
                validationErrors = setOf(ActivityEntryValidationError.ACTIVITY_TYPE_DOES_NOT_SUPPORT_ROUTE),
            )
            return
        }

        recordingDraftStore?.clear()
        val now = LocalDateTime.now(clock).withSecond(0).withNano(0)
        _uiState.value = currentState.copy(
            mode = ActivityEntryMode.RECORDING,
            importedRoute = null,
            recordedPauseIntervals = emptyList(),
            recordedLaps = emptyList(),
            recordedMarkers = emptyList(),
            isRecordingDraft = false,
            startDateText = DateTimeFormatter.ISO_LOCAL_DATE.format(now),
            startTimeText = TimeFormatter.format(now.toLocalTime()),
            durationMinutesText = "1",
            distanceText = "",
            elevationText = "",
            entryError = null,
            detailMessage = null,
            validationErrors = emptySet(),
        )
        if (!recorder.startRecording(currentState.selectedActivityType, initialFix, repetitionRestSeconds)) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.RECORDING_FAILED,
                detailMessage = recorder.state.value.errorMessage,
                validationErrors = emptySet(),
            )
        }
    }

    fun pauseGpsRecording() {
        activityRecorder?.pauseRecording()
    }

    fun resumeGpsRecording() {
        activityRecorder?.resumeRecording()
    }

    fun addRecordingLap() {
        activityRecorder?.addManualLap()
    }

    fun addRecordingMarker() {
        activityRecorder?.addMarker()
    }

    fun updateRecordingMarker(marker: ActivityRecordingMarker) {
        activityRecorder?.updateMarker(marker)
    }

    fun deleteRecordingMarker(markerId: String) {
        activityRecorder?.deleteMarker(markerId)
    }

    fun adjustRepetitionRecording(delta: Long) {
        activityRecorder?.adjustRepetitionCount(delta)
    }

    fun endRepetitionSet() {
        activityRecorder?.endRepetitionSet()
    }

    fun startNextRepetitionSet() {
        activityRecorder?.startNextRepetitionSet()
    }

    fun discardGpsRecording() {
        activityRecorder?.discardRecording()
        recordingDraftStore?.clear()
        chooseSource()
    }

    fun discardRecordingDraft() {
        if (!_uiState.value.isRecordingDraft || _uiState.value.isEditMode) return
        recordingDraftStore?.clear()
        chooseSource()
    }

    fun finishGpsRecording(unitSystem: UnitSystem) {
        val snapshot = activityRecorder?.finishRecording()
        if (snapshot == null) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.RECORDING_FAILED,
                detailMessage = "No active activity recording was found.",
                validationErrors = emptySet(),
            )
            return
        }
        rememberLastActivityType(snapshot.exerciseType)

        if (snapshot.recordingKind == ActivityRecordingKind.GPS_ROUTE && snapshot.points.size >= MinRecordedRoutePoints) {
            applyRouteImport(
                RouteFileImport(
                    fileName = null,
                    points = snapshot.points,
                    distanceMeters = snapshot.distanceMeters,
                    elevationGainedMeters = snapshot.elevationGainedMeters,
                    startTime = snapshot.startTime,
                    endTime = snapshot.endTime,
                    hasRecordedTimestamps = true,
                    hasImportedTimeRange = true,
                    originalPointCount = snapshot.points.size,
                ),
                unitSystem,
            )
            _uiState.value = _uiState.value.copy(
                recordedPauseIntervals = snapshot.pauseIntervals,
                recordedLaps = snapshot.manualLaps.map { it.toExerciseLapData() },
                recordedMarkers = snapshot.markers,
                isRecordingDraft = true,
            )
        } else {
            applyRecordingWithoutRoute(snapshot)
        }
        recordingDraftStore?.store(_uiState.value)
    }

    fun loadEditEntry(unitSystem: UnitSystem) {
        val recordId = editActivityId ?: return
        if (editEntryLoaded) return
        editEntryLoaded = true
        viewModelScope.launch {
            runCatching {
                repository.loadWorkout(recordId)
            }.onSuccess { workout ->
                if (workout == null || !workout.isOpenVitalsEntry) {
                    _uiState.value = _uiState.value.copy(
                        entryError = ActivityEntryError.WRITE_FAILED,
                        detailMessage = "Only OpenVitals entries can be edited.",
                        validationErrors = emptySet(),
                    )
                    return@onSuccess
                }
                val current = _uiState.value
                _uiState.value = workout.toEditState(
                    unitSystem = unitSystem,
                    clock = clock,
                    repository = repository,
                    canWrite = current.canWrite,
                    isCheckingPermission = current.isCheckingPermission,
                ).copy(
                    recordedMarkers = markerRepository?.markersForActivity(recordId).orEmpty()
                        .ifEmpty {
                            workout.clientRecordId
                                ?.let { markerRepository?.markersForActivity(it) }
                                .orEmpty()
                        },
                )
                refreshPlannedWorkouts()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    entryError = ActivityEntryError.WRITE_FAILED,
                    detailMessage = error.message,
                    validationErrors = emptySet(),
                )
            }
        }
    }

    fun addEntry(unitSystem: UnitSystem) {
        if (_uiState.value.mode == ActivityEntryMode.CHOOSE_SOURCE) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.INVALID_VALUE,
                detailMessage = null,
                validationErrors = emptySet(),
            )
            return
        }

        val validationErrors = validateActivityEntry(_uiState.value, unitSystem)
        if (validationErrors.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.INVALID_VALUE,
                detailMessage = null,
                validationErrors = validationErrors,
            )
            return
        }

        val request = buildWriteRequest(_uiState.value, unitSystem)
        if (request == null) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.INVALID_VALUE,
                detailMessage = null,
                validationErrors = validationErrors,
            )
            return
        }
        val editRecordId = _uiState.value.editRecordId
        val wasRecordingDraft = _uiState.value.isRecordingDraft
        val markersToSave = _uiState.value.recordedMarkers
            .takeIf { _uiState.value.selectedActivityType.supportsGpsRoute }
            .orEmpty()
        val requestPermissions = repository.activityWritePermissions(request)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingEntry = true,
                entryError = null,
                detailMessage = null,
                validationErrors = emptySet(),
                writePermissions = requestPermissions,
            )
            val hasPermission = repository.hasActivityWritePermission(request)
            if (!hasPermission) {
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    canWrite = false,
                    entryError = ActivityEntryError.MISSING_WRITE_PERMISSION,
                    detailMessage = null,
                    validationErrors = emptySet(),
                )
                return@launch
            }

            runCatching {
                if (editRecordId == null) {
                    repository.writeActivityEntry(request)
                } else {
                    repository.updateActivityEntry(editRecordId, request)
                    editRecordId
                }
            }.onSuccess { savedActivityId ->
                markerRepository?.setMarkersForActivity(savedActivityId, markersToSave)
                recordingDraftStore?.clear()
                if (wasRecordingDraft) {
                    rememberLastActivityType(request.exerciseType)
                }
                if (editRecordId == null) {
                    _uiState.value = clearedAfterSaveState(clock, repository, preferredActivityType())
                        .copy(saveCompleted = true)
                    refreshPermission()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSavingEntry = false,
                        saveCompleted = true,
                        entryError = null,
                        detailMessage = null,
                        validationErrors = emptySet(),
                    )
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    entryError = ActivityEntryError.WRITE_FAILED,
                    detailMessage = error.message,
                    validationErrors = emptySet(),
                )
            }
        }
    }

    fun onSaveCompletedHandled() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    private fun applyRouteImport(routeImport: RouteFileImport, unitSystem: UnitSystem) {
        val currentState = _uiState.value
        val start = routeImport.startTime.atZone(clock.zone)
        val selectedActivityType = inferActivityType(routeImport, currentState.selectedActivityType)
        val routeDurationMinutes = if (routeImport.hasImportedTimeRange) {
            val routeDurationSeconds = Duration.between(routeImport.startTime, routeImport.endTime).seconds.coerceAtLeast(1)
            ceil((routeDurationSeconds + 1).toDouble() / 60.0)
                .toLong()
                .coerceIn(1, MaxActivityDurationMinutes)
                .toString()
        } else {
            currentState.durationMinutesText.ifBlank { "30" }
        }
        val calorieEstimate = activityCalorieEstimate(
            activityType = selectedActivityType,
            distanceMeters = routeImport.distanceMeters,
            durationMinutesText = routeDurationMinutes,
        ).takeIf {
            currentState.activeCaloriesText.isBlank() && currentState.totalCaloriesText.isBlank()
        }
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.ROUTE_IMPORT,
            selectedActivityType = selectedActivityType,
            titleText = currentState.titleText.ifBlank { routeImport.name.orEmpty() },
            notesText = currentState.notesText.ifBlank { routeImport.description.orEmpty() },
            distanceText = currentState.distanceText.ifBlank { routeDistanceInputText(routeImport, unitSystem) },
            elevationText = currentState.elevationText.ifBlank { routeElevationInputText(routeImport, unitSystem) },
            activeCaloriesText = calorieEstimate?.activeCaloriesText ?: currentState.activeCaloriesText,
            totalCaloriesText = calorieEstimate?.totalCaloriesText ?: currentState.totalCaloriesText,
            importedRoute = routeImport,
            recordedPauseIntervals = emptyList(),
            recordedLaps = emptyList(),
            recordedMarkers = emptyList(),
            isRecordingDraft = false,
            startDateText = if (routeImport.hasImportedTimeRange) {
                DateTimeFormatter.ISO_LOCAL_DATE.format(start)
            } else {
                currentState.startDateText
            },
            startTimeText = if (routeImport.hasImportedTimeRange) {
                TimeFormatter.format(start.toLocalTime())
            } else {
                currentState.startTimeText
            },
            durationMinutesText = routeDurationMinutes,
            isImportingRoute = false,
            entryError = null,
            detailMessage = null,
            validationErrors = emptySet(),
        )
        refreshPermission()
    }

    private fun applyRecordingProgress(recording: ActivityRecordingState) {
        val start = recording.startTime ?: return
        val startDateTime = start.atZone(clock.zone)
        val durationMinutes = ceil(
            recording.elapsedDuration(Instant.now(clock)).seconds
                .coerceAtLeast(1)
                .toDouble() / 60.0
        ).toLong().coerceIn(1, MaxActivityDurationMinutes)
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.RECORDING,
            importedRoute = null,
            startDateText = DateTimeFormatter.ISO_LOCAL_DATE.format(startDateTime),
            startTimeText = TimeFormatter.format(startDateTime.toLocalTime()),
            durationMinutesText = durationMinutes.toString(),
            entryError = recording.errorMessage?.let { ActivityEntryError.RECORDING_FAILED },
            detailMessage = recording.errorMessage,
            validationErrors = emptySet(),
        )
    }

    private fun applyRecordingWithoutRoute(snapshot: ActivityRecordingSnapshot) {
        val currentState = _uiState.value
        val start = snapshot.startTime.atZone(clock.zone)
        val durationMinutes = ceil(
            Duration.between(snapshot.startTime, snapshot.endTime).seconds
                .coerceAtLeast(1)
                .toDouble() / 60.0
        ).toLong().coerceIn(1, MaxActivityDurationMinutes)
        val selectedActivityType = activityEntryTypeById(snapshot.activityTypeId)
            ?: DefaultActivityEntryTypes.firstOrNull { it.exerciseType == snapshot.exerciseType && !it.isRepetitionLike }
            ?: DefaultActivityEntryTypes.firstOrNull { it.exerciseType == snapshot.exerciseType }
            ?: currentState.selectedActivityType
        val calorieEstimate = activityCalorieEstimate(
            activityType = selectedActivityType,
            distanceMeters = null,
            durationMinutesText = durationMinutes.toString(),
        ).takeIf {
            currentState.activeCaloriesText.isBlank() && currentState.totalCaloriesText.isBlank()
        }
        val recordedSets = snapshot.repetitionSets.map { set ->
            ActivityRepetitionSetInput(
                repetitionsText = set.repetitions.toString(),
                restMinutesText = set.restSeconds.takeIf { it > 0L }?.toString().orEmpty(),
            )
        }
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.MANUAL,
            selectedActivityType = selectedActivityType,
            importedRoute = null,
            recordedPauseIntervals = snapshot.pauseIntervals,
            recordedLaps = snapshot.manualLaps.map { it.toExerciseLapData() },
            recordedMarkers = snapshot.markers,
            isRecordingDraft = true,
            startDateText = DateTimeFormatter.ISO_LOCAL_DATE.format(start),
            startTimeText = TimeFormatter.format(start.toLocalTime()),
            durationMinutesText = durationMinutes.toString(),
            distanceText = "",
            elevationText = "",
            activeCaloriesText = calorieEstimate?.activeCaloriesText ?: currentState.activeCaloriesText,
            totalCaloriesText = calorieEstimate?.totalCaloriesText ?: currentState.totalCaloriesText,
            repetitionMode = if (recordedSets.isNotEmpty()) {
                ActivityRepetitionEntryMode.SETS
            } else {
                ActivityRepetitionEntryMode.TOTAL
            },
            repetitionTotalText = snapshot.repetitionCount.takeIf {
                selectedActivityType.isRepetitionLike && it > 0L && recordedSets.isEmpty()
            }?.toString().orEmpty(),
            repetitionSets = recordedSets.takeIf { it.isNotEmpty() } ?: listOf(ActivityRepetitionSetInput()),
            entryError = null,
            detailMessage = null,
            validationErrors = emptySet(),
        )
        refreshPermission()
    }

    private fun updateState(
        clearFields: Set<ActivityEntryField> = emptySet(),
        update: ActivityEntryUiState.() -> ActivityEntryUiState,
    ) {
        val previous = _uiState.value
        val updated = previous.update()
        val permissions = currentRequiredPermissions()
        _uiState.value = updated.copy(
            writePermissions = permissions,
            canWrite = updated.canWrite && permissions == previous.writePermissions,
            validationErrors = updated.validationErrors.filterNot { it.field in clearFields }.toSet(),
        )
    }

    private fun currentRequiredPermissions(): Set<String> =
        repository.activityWritePermissions()

    private fun preferredActivityType(
        requireGpsRoute: Boolean = false,
        requireLiveRecording: Boolean = false,
    ): ActivityEntryType {
        val activityTypes = DefaultActivityEntryTypes
            .filter { (!requireGpsRoute || it.supportsGpsRoute) && (!requireLiveRecording || it.supportsLiveRecording) }
            .ifEmpty { DefaultActivityEntryTypes }
        val preferredExerciseType = preferencesRepository
            ?.favoriteActivityExerciseType
            ?.takeIf { exerciseType -> activityTypes.any { it.exerciseType == exerciseType } }
            ?: preferencesRepository
                ?.lastActivityExerciseType
                ?.takeIf { exerciseType -> activityTypes.any { it.exerciseType == exerciseType } }
        return activityTypes.firstOrNull { it.exerciseType == preferredExerciseType }
            ?: activityTypes.first()
    }

    private fun rememberLastActivityType(exerciseType: Int) {
        preferencesRepository?.lastActivityExerciseType = exerciseType
    }
}

private fun ActivityRecordingLap.toExerciseLapData(): ExerciseLapData =
    ExerciseLapData(
        startTime = startTime,
        endTime = endTime,
        lengthMeters = distanceMeters,
    )

internal fun buildPlannedExerciseWriteRequest(
    state: ActivityEntryUiState,
    unitSystem: UnitSystem,
    updateExistingId: String? = null,
): PlannedExerciseWriteRequest? {
    if (!state.selectedActivityType.supportsSetRepetitions) return null
    if (validatePlannedExerciseWriteRequest(state, unitSystem).isNotEmpty()) return null
    val activityRequest = buildWriteRequest(state, unitSystem) ?: return null
    val title = state.titleText.trim().takeIf { it.isNotEmpty() } ?: return null
    val segmentType = state.selectedActivityType.segmentType ?: ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT
    val steps = when (state.repetitionMode) {
        ActivityRepetitionEntryMode.TOTAL -> {
            val repetitions = state.repetitionTotalText.trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
            listOf(repetitionPlanStep(segmentType, repetitions, setNumber = 1))
        }
        ActivityRepetitionEntryMode.SETS -> {
            state.repetitionSets.flatMapIndexed { index, set ->
                val repetitions = set.repetitionsText.trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
                buildList {
                    add(repetitionPlanStep(segmentType, repetitions, setNumber = index + 1))
                    val restSeconds = set.restMinutesText.trim().toLongOrNull()?.takeIf { it > 0L }
                    if (restSeconds != null) {
                        add(restPlanStep(restSeconds))
                    }
                }
            }
        }
    }
    if (steps.isEmpty()) return null
    return PlannedExerciseWriteRequest(
        id = updateExistingId,
        exerciseType = activityRequest.exerciseType,
        startTime = activityRequest.startTime,
        endTime = activityRequest.endTime,
        title = title,
        notes = activityRequest.notes,
        blocks = listOf(
            PlannedExerciseBlockData(
                repetitions = 1,
                description = title,
                steps = steps,
            )
        ),
    )
}

internal fun validatePlannedExerciseWriteRequest(
    state: ActivityEntryUiState,
    unitSystem: UnitSystem,
): Set<ActivityEntryValidationError> =
    buildSet {
        addAll(validateActivityEntry(state, unitSystem))
        if (state.titleText.isBlank()) {
            add(ActivityEntryValidationError.TRAINING_PLAN_TITLE_REQUIRED)
        }
    }

private fun repetitionPlanStep(segmentType: Int, repetitions: Int, setNumber: Int): PlannedExerciseStepData =
    PlannedExerciseStepData(
        exerciseType = segmentType,
        exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
        description = "Set $setNumber",
        completion = PlannedExerciseCompletion.Repetitions(repetitions),
    )

private fun restPlanStep(seconds: Long): PlannedExerciseStepData =
    PlannedExerciseStepData(
        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
        exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_REST,
        description = "Rest",
        completion = PlannedExerciseCompletion.DurationSeconds(seconds),
    )

internal fun PlannedExerciseData.toRepetitionSetInputs(): List<ActivityRepetitionSetInput> {
    val sets = mutableListOf<ActivityRepetitionSetInput>()
    blocks.forEach { block ->
        repeat(block.repetitions.coerceAtLeast(1)) {
            block.steps.forEach { step ->
                when (val completion = step.completion) {
                    is PlannedExerciseCompletion.Repetitions -> {
                        sets += ActivityRepetitionSetInput(
                            repetitionsText = completion.repetitions.toString(),
                        )
                    }
                    is PlannedExerciseCompletion.DurationSeconds -> {
                        val last = sets.lastOrNull() ?: return@forEach
                        sets[sets.lastIndex] = last.copy(restMinutesText = completion.seconds.toString())
                    }
                    PlannedExerciseCompletion.Manual,
                    PlannedExerciseCompletion.Unknown -> Unit
                }
            }
        }
    }
    return sets
}

internal fun PlannedExerciseData.toActivityEntryType(): ActivityEntryType? {
    val activeSegmentType = blocks
        .asSequence()
        .flatMap { it.steps.asSequence() }
        .firstOrNull { step ->
            step.exercisePhase == PlannedExerciseStep.EXERCISE_PHASE_ACTIVE &&
                step.completion is PlannedExerciseCompletion.Repetitions
        }
        ?.exerciseType

    return DefaultActivityEntryTypes.firstOrNull { type ->
        type.exerciseType == exerciseType && type.segmentType != null && type.segmentType == activeSegmentType
    } ?: DefaultActivityEntryTypes.firstOrNull { type ->
        type.exerciseType == exerciseType && type.supportsSetRepetitions
    } ?: DefaultActivityEntryTypes.firstOrNull { type ->
        type.exerciseType == exerciseType
    }
}

private fun PlannedExerciseData.durationMinutesText(): String {
    val minutes = Duration.ofMillis(durationMs).toMinutes().coerceAtLeast(1L)
    return minutes.coerceIn(1, MaxActivityDurationMinutes).toString()
}
