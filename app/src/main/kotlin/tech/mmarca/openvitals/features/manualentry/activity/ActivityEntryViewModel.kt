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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.Instant
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
        super.onCleared()
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
        val retainedRoute = _uiState.value.importedRoute?.takeIf { type.supportsGpsRoute }
        _uiState.value = _uiState.value.copy(
            selectedActivityType = type,
            importedRoute = retainedRoute,
            recordedPauseIntervals = if (retainedRoute == null) emptyList() else _uiState.value.recordedPauseIntervals,
            recordedLaps = if (retainedRoute == null) emptyList() else _uiState.value.recordedLaps,
            recordedMarkers = if (retainedRoute == null) emptyList() else _uiState.value.recordedMarkers,
            mode = if (retainedRoute == null && _uiState.value.mode == ActivityEntryMode.ROUTE_IMPORT) {
                ActivityEntryMode.MANUAL
            } else {
                _uiState.value.mode
            },
            entryError = null,
            detailMessage = null,
            validationErrors = emptySet(),
        )
        refreshPermission()
    }

    fun startManualEntry() {
        recordingDraftStore?.clear()
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.MANUAL,
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
        updateState { copy(titleText = text, entryError = null, detailMessage = null) }
    }

    fun updateNotes(text: String) {
        updateState { copy(notesText = text, entryError = null, detailMessage = null) }
    }

    fun updateStartDate(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.START_DATE, ActivityEntryField.START_TIME)) {
            copy(startDateText = text, entryError = null, detailMessage = null)
        }
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

    fun startGpsRecording(initialFix: Location? = null) {
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
        if (!recorder.startRecording(currentState.selectedActivityType, initialFix)) {
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
            repetitionMode = ActivityRepetitionEntryMode.TOTAL,
            repetitionTotalText = snapshot.repetitionCount.takeIf {
                selectedActivityType.isRepetitionLike && it > 0L
            }?.toString().orEmpty(),
            repetitionSets = listOf(ActivityRepetitionSetInput()),
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
