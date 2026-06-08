package tech.mmarca.openvitals.features.manualentry

import android.location.Location
import android.net.Uri
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.preferences.UnitSystem
import tech.mmarca.openvitals.data.model.ActivityPauseInterval
import tech.mmarca.openvitals.data.model.ActivityWriteRequest
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.ExerciseRoutePoint
import tech.mmarca.openvitals.data.model.ExerciseRouteStatus
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.navigation.ACTIVITY_ENTRY_ID_ARG

private const val MilesToMeters = 1609.344
private const val FeetToMeters = 0.3048
private const val MaxActivityDurationMinutes = 7 * 24 * 60L
private const val MinRecordedRoutePoints = 2
private const val DefaultCalorieEstimateWeightKg = 70.0
private const val RestingMet = 1.0
private const val RunningKcalPerKgKm = 1.0
private const val WalkingKcalPerKgKm = 0.55

enum class ActivityEntryError {
    INVALID_VALUE,
    MISSING_WRITE_PERMISSION,
    ROUTE_IMPORT_FAILED,
    LOCATION_PERMISSION_NEEDED,
    NOTIFICATION_PERMISSION_NEEDED,
    RECORDING_FAILED,
    WRITE_FAILED,
}

enum class ActivityEntryMode {
    CHOOSE_SOURCE,
    MANUAL,
    ROUTE_IMPORT,
    RECORDING,
}

enum class ActivityEntryField {
    ACTIVITY_TYPE,
    START_DATE,
    START_TIME,
    DURATION,
    DISTANCE,
    ELEVATION,
    ACTIVE_CALORIES,
    TOTAL_CALORIES,
}

enum class ActivityEntryValidationError(
    val field: ActivityEntryField,
) {
    ACTIVITY_TYPE_DOES_NOT_SUPPORT_ROUTE(ActivityEntryField.ACTIVITY_TYPE),
    START_DATE_INVALID(ActivityEntryField.START_DATE),
    START_TIME_INVALID(ActivityEntryField.START_TIME),
    START_TIME_AFTER_ROUTE_START(ActivityEntryField.START_TIME),
    DURATION_INVALID(ActivityEntryField.DURATION),
    DISTANCE_INVALID(ActivityEntryField.DISTANCE),
    DISTANCE_UNSUPPORTED(ActivityEntryField.DISTANCE),
    ELEVATION_INVALID(ActivityEntryField.ELEVATION),
    ELEVATION_UNSUPPORTED(ActivityEntryField.ELEVATION),
    ACTIVE_CALORIES_INVALID(ActivityEntryField.ACTIVE_CALORIES),
    TOTAL_CALORIES_INVALID(ActivityEntryField.TOTAL_CALORIES),
    TOTAL_CALORIES_BELOW_ACTIVE(ActivityEntryField.TOTAL_CALORIES),
}

data class ActivityEntryUiState(
    val mode: ActivityEntryMode = ActivityEntryMode.CHOOSE_SOURCE,
    val activityTypes: List<ActivityEntryType> = DefaultActivityEntryTypes,
    val selectedActivityType: ActivityEntryType = DefaultActivityEntryTypes.first(),
    val titleText: String = "",
    val notesText: String = "",
    val startDateText: String = "",
    val startTimeText: String = "",
    val durationMinutesText: String = "30",
    val distanceText: String = "",
    val elevationText: String = "",
    val activeCaloriesText: String = "",
    val totalCaloriesText: String = "",
    val importedRoute: RouteFileImport? = null,
    val recordedPauseIntervals: List<ActivityPauseInterval> = emptyList(),
    val writePermissions: Set<String> = emptySet(),
    val canWrite: Boolean = false,
    val isCheckingPermission: Boolean = true,
    val isImportingRoute: Boolean = false,
    val isSavingEntry: Boolean = false,
    val entryError: ActivityEntryError? = null,
    val detailMessage: String? = null,
    val validationErrors: Set<ActivityEntryValidationError> = emptySet(),
    val editRecordId: String? = null,
    val isRecordingDraft: Boolean = false,
    val saveCompleted: Boolean = false,
) {
    val routePoints: List<ExerciseRoutePoint>
        get() = importedRoute?.points.orEmpty()

    val isEditMode: Boolean
        get() = editRecordId != null
}

@HiltViewModel
class ActivityEntryViewModel(
    private val repository: ActivityRepository,
    private val routeFileImporter: RouteFileImporter? = null,
    private val activityRecorder: ActivityRecordingController? = null,
    private val recordingDraftStore: ActivityRecordingDraftStore? = null,
    private val preferencesRepository: PreferencesRepository? = null,
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
        savedStateHandle: SavedStateHandle,
    ) : this(
        repository = repository,
        routeFileImporter = routeFileImporter,
        activityRecorder = activityRecorder,
        recordingDraftStore = recordingDraftStore,
        preferencesRepository = preferencesRepository,
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

    fun prepareGpsRecording() {
        val currentState = _uiState.value

        recordingDraftStore?.clear()
        _uiState.value = currentState.copy(
            mode = ActivityEntryMode.RECORDING,
            selectedActivityType = preferredActivityType(requireGpsRoute = true),
            importedRoute = null,
            recordedPauseIntervals = emptyList(),
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
        if (!currentState.selectedActivityType.supportsGpsRoute) {
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
        if (!recorder.startRecording(currentState.selectedActivityType.exerciseType, initialFix)) {
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
                detailMessage = "No active GPS recording was found.",
                validationErrors = emptySet(),
            )
            return
        }
        rememberLastActivityType(snapshot.exerciseType)

        if (snapshot.points.size >= MinRecordedRoutePoints) {
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

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingEntry = true,
                entryError = null,
                detailMessage = null,
                validationErrors = emptySet(),
                writePermissions = repository.activityWritePermissions(),
            )
            val hasPermission = repository.hasActivityWritePermission()
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
                }
            }.onSuccess {
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
        val selectedActivityType = DefaultActivityEntryTypes
            .firstOrNull { it.exerciseType == snapshot.exerciseType }
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
            isRecordingDraft = true,
            startDateText = DateTimeFormatter.ISO_LOCAL_DATE.format(start),
            startTimeText = TimeFormatter.format(start.toLocalTime()),
            durationMinutesText = durationMinutes.toString(),
            distanceText = "",
            elevationText = "",
            activeCaloriesText = calorieEstimate?.activeCaloriesText ?: currentState.activeCaloriesText,
            totalCaloriesText = calorieEstimate?.totalCaloriesText ?: currentState.totalCaloriesText,
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

    private fun preferredActivityType(requireGpsRoute: Boolean = false): ActivityEntryType {
        val activityTypes = DefaultActivityEntryTypes
            .filter { !requireGpsRoute || it.supportsGpsRoute }
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

internal fun buildWriteRequest(
    state: ActivityEntryUiState,
    unitSystem: UnitSystem,
): ActivityWriteRequest? {
    if (validateActivityEntry(state, unitSystem).isNotEmpty()) return null

    val startDate = state.startDateText.trim().let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: return null
    val startTime = state.startTimeText.trim().let { runCatching { LocalTime.parse(it, TimeFormatter) }.getOrNull() }
        ?: return null
    val durationMinutes = state.durationMinutesText.trim().toLongOrNull()
        ?.takeIf { it in 1..MaxActivityDurationMinutes }
        ?: return null
    val zone = ZoneId.systemDefault()
    val start = LocalDateTime.of(startDate, startTime).atZone(zone).toInstant()
    var end = start.plus(Duration.ofMinutes(durationMinutes))
    val importedRoute = state.importedRoute
    var routePoints = importedRoute?.points.orEmpty()
    if (routePoints.isNotEmpty()) {
        if (!state.selectedActivityType.supportsGpsRoute) return null
        if (importedRoute?.hasRecordedTimestamps == false) {
            routePoints = routePoints.withActivityTimeRange(start, end)
        } else {
            val firstPoint = routePoints.first()
            val lastPoint = routePoints.last()
            if (firstPoint.time.isBefore(start)) return null
            if (!lastPoint.time.isBefore(end)) {
                end = lastPoint.time.plusSeconds(1)
            }
        }
    }

    val distanceMeters = when {
        state.distanceText.isNotBlank() && importedRoute != null &&
            state.distanceText.trim() == routeDistanceInputText(importedRoute, unitSystem) -> {
            importedRoute.distanceMeters.takeIf { it > 0.0 }
        }
        state.distanceText.isNotBlank() -> parseDistanceMeters(state.distanceText, unitSystem) ?: return null
        routePoints.isNotEmpty() -> state.importedRoute?.distanceMeters?.takeIf { it > 0.0 }
        else -> null
    }
    val elevationMeters = when {
        state.elevationText.isNotBlank() && importedRoute != null &&
            state.elevationText.trim() == routeElevationInputText(importedRoute, unitSystem) -> {
            importedRoute.elevationGainedMeters.takeIf { it > 0.0 }
        }
        state.elevationText.isNotBlank() -> parseElevationMeters(state.elevationText, unitSystem) ?: return null
        routePoints.isNotEmpty() -> state.importedRoute?.elevationGainedMeters?.takeIf { it > 0.0 }
        else -> null
    }
    val activeCalories = if (state.activeCaloriesText.isBlank()) {
        null
    } else {
        state.activeCaloriesText.toPositiveDoubleOrNull() ?: return null
    }
    val totalCalories = if (state.totalCaloriesText.isBlank()) {
        null
    } else {
        state.totalCaloriesText.toPositiveDoubleOrNull() ?: return null
    }
    if (activeCalories != null && totalCalories != null && totalCalories < activeCalories) return null

    return ActivityWriteRequest(
        exerciseType = state.selectedActivityType.exerciseType,
        startTime = start,
        endTime = end,
        title = state.titleText.trim().takeIf { it.isNotBlank() },
        notes = state.notesText.trim().takeIf { it.isNotBlank() },
        routePoints = routePoints,
        pauseIntervals = state.recordedPauseIntervals.insideActivityRange(start, end),
        distanceMeters = distanceMeters,
        elevationGainedMeters = elevationMeters,
        activeCaloriesKcal = activeCalories,
        totalCaloriesKcal = totalCalories,
    )
}

internal fun validateActivityEntry(
    state: ActivityEntryUiState,
    unitSystem: UnitSystem,
): Set<ActivityEntryValidationError> {
    val errors = mutableSetOf<ActivityEntryValidationError>()
    val startDate = state.startDateText.trim()
        .let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val startTime = state.startTimeText.trim()
        .let { runCatching { LocalTime.parse(it, TimeFormatter) }.getOrNull() }
    val durationMinutes = state.durationMinutesText.trim().toLongOrNull()
        ?.takeIf { it in 1..MaxActivityDurationMinutes }

    if (startDate == null) errors += ActivityEntryValidationError.START_DATE_INVALID
    if (startTime == null) errors += ActivityEntryValidationError.START_TIME_INVALID
    if (durationMinutes == null) errors += ActivityEntryValidationError.DURATION_INVALID

    val importedRoute = state.importedRoute
    val routePoints = importedRoute?.points.orEmpty()
    if (routePoints.isNotEmpty() && !state.selectedActivityType.supportsGpsRoute) {
        errors += ActivityEntryValidationError.ACTIVITY_TYPE_DOES_NOT_SUPPORT_ROUTE
    }
    if (
        routePoints.isNotEmpty() &&
        importedRoute?.hasRecordedTimestamps != false &&
        startDate != null &&
        startTime != null
    ) {
        val start = LocalDateTime.of(startDate, startTime).atZone(ZoneId.systemDefault()).toInstant()
        if (routePoints.first().time.isBefore(start)) {
            errors += ActivityEntryValidationError.START_TIME_AFTER_ROUTE_START
        }
    }

    if (state.distanceText.isNotBlank()) {
        when {
            !state.selectedActivityType.supportsDistance -> {
                errors += ActivityEntryValidationError.DISTANCE_UNSUPPORTED
            }
            importedRoute != null &&
                state.distanceText.trim() == routeDistanceInputText(importedRoute, unitSystem) -> Unit
            parseDistanceMeters(state.distanceText, unitSystem) == null -> {
                errors += ActivityEntryValidationError.DISTANCE_INVALID
            }
        }
    }

    if (state.elevationText.isNotBlank()) {
        when {
            !state.selectedActivityType.supportsElevation -> {
                errors += ActivityEntryValidationError.ELEVATION_UNSUPPORTED
            }
            importedRoute != null &&
                state.elevationText.trim() == routeElevationInputText(importedRoute, unitSystem) -> Unit
            parseElevationMeters(state.elevationText, unitSystem) == null -> {
                errors += ActivityEntryValidationError.ELEVATION_INVALID
            }
        }
    }

    val activeCalories = if (state.activeCaloriesText.isBlank()) {
        null
    } else {
        state.activeCaloriesText.toPositiveDoubleOrNull()
            ?: run {
                errors += ActivityEntryValidationError.ACTIVE_CALORIES_INVALID
                null
            }
    }
    val totalCalories = if (state.totalCaloriesText.isBlank()) {
        null
    } else {
        state.totalCaloriesText.toPositiveDoubleOrNull()
            ?: run {
                errors += ActivityEntryValidationError.TOTAL_CALORIES_INVALID
                null
            }
    }
    if (activeCalories != null && totalCalories != null && totalCalories < activeCalories) {
        errors += ActivityEntryValidationError.TOTAL_CALORIES_BELOW_ACTIVE
    }

    return errors
}

private fun initialActivityEntryState(
    clock: Clock,
    repository: ActivityRepository,
    selectedActivityType: ActivityEntryType = DefaultActivityEntryTypes.first(),
): ActivityEntryUiState {
    val now = LocalDateTime.now(clock).withSecond(0).withNano(0)
    return ActivityEntryUiState(
        selectedActivityType = selectedActivityType,
        startDateText = DateTimeFormatter.ISO_LOCAL_DATE.format(now),
        startTimeText = TimeFormatter.format(now.toLocalTime()),
        writePermissions = repository.activityWritePermissions(),
    )
}

private fun clearedAfterSaveState(
    clock: Clock,
    repository: ActivityRepository,
    selectedType: ActivityEntryType,
): ActivityEntryUiState =
    initialActivityEntryState(clock, repository, selectedType)

private fun ExerciseData.toEditState(
    unitSystem: UnitSystem,
    clock: Clock,
    repository: ActivityRepository,
    canWrite: Boolean,
    isCheckingPermission: Boolean,
): ActivityEntryUiState {
    val selectedType = DefaultActivityEntryTypes
        .firstOrNull { it.exerciseType == exerciseType }
        ?: DefaultActivityEntryTypes.first()
    val routeImport = route.takeIf { it.status == ExerciseRouteStatus.DATA && it.points.isNotEmpty() }
        ?.let { routeData ->
            RouteFileImport(
                fileName = null,
                points = routeData.points,
                distanceMeters = totalDistanceMeters ?: 0.0,
                elevationGainedMeters = elevationGainedMeters ?: 0.0,
                startTime = startTime,
                endTime = endTime,
                name = title,
                description = notes,
                hasRecordedTimestamps = true,
                hasImportedTimeRange = true,
                originalPointCount = routeData.points.size,
            )
        }
    val start = startTime.atZone(clock.zone)
    val durationMinutes = ceil(
        Duration.between(startTime, endTime).seconds.coerceAtLeast(1).toDouble() / 60.0
    ).toLong().coerceIn(1, MaxActivityDurationMinutes)
    return ActivityEntryUiState(
        mode = if (routeImport == null) ActivityEntryMode.MANUAL else ActivityEntryMode.ROUTE_IMPORT,
        selectedActivityType = selectedType,
        titleText = title.orEmpty(),
        notesText = notes.orEmpty(),
        startDateText = DateTimeFormatter.ISO_LOCAL_DATE.format(start),
        startTimeText = TimeFormatter.format(start.toLocalTime()),
        durationMinutesText = durationMinutes.toString(),
        distanceText = totalDistanceMeters?.takeIf { it > 0.0 }?.toDistanceInputText(unitSystem).orEmpty(),
        elevationText = elevationGainedMeters?.takeIf { it > 0.0 }?.toElevationInputText(unitSystem).orEmpty(),
        activeCaloriesText = activeCaloriesKcal?.takeIf { it > 0.0 }?.toInputText(maxFractionDigits = 1).orEmpty(),
        totalCaloriesText = totalCaloriesKcal?.takeIf { it > 0.0 }?.toInputText(maxFractionDigits = 1).orEmpty(),
        importedRoute = routeImport,
        recordedPauseIntervals = segments
            .filter { it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE }
            .map { ActivityPauseInterval(startTime = it.startTime, endTime = it.endTime) },
        writePermissions = repository.activityWritePermissions(),
        canWrite = canWrite,
        isCheckingPermission = isCheckingPermission,
        editRecordId = id,
    )
}

private fun Double.toDistanceInputText(unitSystem: UnitSystem): String {
    val value = when (unitSystem) {
        UnitSystem.METRIC -> this / 1000.0
        UnitSystem.IMPERIAL -> this / MilesToMeters
    }
    return value.toInputText(maxFractionDigits = 2)
}

private fun Double.toElevationInputText(unitSystem: UnitSystem): String {
    val value = when (unitSystem) {
        UnitSystem.METRIC -> this
        UnitSystem.IMPERIAL -> this / FeetToMeters
    }
    return value.toInputText(maxFractionDigits = 1)
}

private fun inferActivityType(
    routeImport: RouteFileImport,
    currentType: ActivityEntryType,
): ActivityEntryType {
    val sourceText = listOfNotNull(routeImport.type, routeImport.name, routeImport.fileName)
        .joinToString(separator = " ")
        .lowercase()
    val exerciseType = when {
        sourceText.containsAny("snowboard") -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING
        sourceText.containsAny("snowshoe") -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING
        sourceText.containsAny("ski") -> ExerciseSessionRecord.EXERCISE_TYPE_SKIING
        sourceText.containsAny("hike", "hiking") -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
        sourceText.containsAny("run", "running", "jog") -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        sourceText.containsAny("bike", "biking", "bicycle", "cycling", "cycle", "ride") -> {
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING
        }
        sourceText.containsAny("walk", "walking") -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
        sourceText.containsAny("wheelchair") -> ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
        sourceText.containsAny("row", "rowing") -> ExerciseSessionRecord.EXERCISE_TYPE_ROWING
        sourceText.containsAny("paddle", "kayak", "canoe") -> ExerciseSessionRecord.EXERCISE_TYPE_PADDLING
        sourceText.containsAny("skate", "skating") -> ExerciseSessionRecord.EXERCISE_TYPE_SKATING
        sourceText.containsAny("sail", "sailing") -> ExerciseSessionRecord.EXERCISE_TYPE_SAILING
        sourceText.containsAny("surf", "surfing") -> ExerciseSessionRecord.EXERCISE_TYPE_SURFING
        sourceText.containsAny("swim", "swimming") -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER
        sourceText.containsAny("golf") -> ExerciseSessionRecord.EXERCISE_TYPE_GOLF
        else -> null
    }
    return DefaultActivityEntryTypes
        .firstOrNull { it.exerciseType == exerciseType && it.supportsGpsRoute }
        ?: currentType.takeIf { it.supportsGpsRoute }
        ?: DefaultActivityEntryTypes.first()
}

private fun String.containsAny(vararg values: String): Boolean =
    values.any(::contains)

private data class ActivityCalorieEstimate(
    val activeCaloriesText: String,
    val totalCaloriesText: String,
)

private fun activityCalorieEstimate(
    activityType: ActivityEntryType,
    distanceMeters: Double?,
    durationMinutesText: String,
): ActivityCalorieEstimate? {
    if (!activityType.supportsGpsRoute) return null
    val durationMinutes = durationMinutesText.trim().toLongOrNull()
        ?.takeIf { it in 1..MaxActivityDurationMinutes }
        ?: return null
    val hours = durationMinutes / 60.0
    val met = activityMet(activityType.exerciseType) ?: return null
    val restingCalories = DefaultCalorieEstimateWeightKg * hours * RestingMet
    val activeByMet = (met - RestingMet)
        .coerceAtLeast(0.0) * DefaultCalorieEstimateWeightKg * hours
    val activeByDistance = distanceBasedActiveCalories(
        exerciseType = activityType.exerciseType,
        distanceMeters = distanceMeters,
    ) ?: 0.0
    val activeCalories = maxOf(activeByMet, activeByDistance).takeIf { it > 0.0 } ?: return null

    return ActivityCalorieEstimate(
        activeCaloriesText = activeCalories.toCaloriesInputText(),
        totalCaloriesText = (activeCalories + restingCalories).toCaloriesInputText(),
    )
}

private fun activityMet(exerciseType: Int): Double? =
    when (exerciseType) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> 9.8
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> 7.5
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> 3.5
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> 6.0
        ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR -> 4.0
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
        ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> 7.0
        ExerciseSessionRecord.EXERCISE_TYPE_SKIING -> 7.0
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING -> 5.3
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING -> 8.0
        ExerciseSessionRecord.EXERCISE_TYPE_SKATING -> 7.0
        ExerciseSessionRecord.EXERCISE_TYPE_SAILING -> 3.0
        ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> 3.0
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> 8.0
        ExerciseSessionRecord.EXERCISE_TYPE_GOLF -> 4.8
        else -> null
    }

private fun distanceBasedActiveCalories(
    exerciseType: Int,
    distanceMeters: Double?,
): Double? {
    val distanceKm = distanceMeters?.takeIf { it > 0.0 }?.div(1000.0) ?: return null
    val kcalPerKgKm = when (exerciseType) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING -> RunningKcalPerKgKm
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR -> WalkingKcalPerKgKm
        else -> return null
    }
    return DefaultCalorieEstimateWeightKg * distanceKm * kcalPerKgKm
}

private fun parseDistanceMeters(text: String, unitSystem: UnitSystem): Double? {
    val value = text.toPositiveDoubleOrNull() ?: return null
    return when (unitSystem) {
        UnitSystem.METRIC -> value * 1000.0
        UnitSystem.IMPERIAL -> value * MilesToMeters
    }
}

private fun parseElevationMeters(text: String, unitSystem: UnitSystem): Double? {
    val value = text.toPositiveDoubleOrNull() ?: return null
    return when (unitSystem) {
        UnitSystem.METRIC -> value
        UnitSystem.IMPERIAL -> value * FeetToMeters
    }
}

private fun routeDistanceInputText(routeImport: RouteFileImport, unitSystem: UnitSystem): String {
    val distance = routeImport.distanceMeters.takeIf { it > 0.0 } ?: return ""
    val value = when (unitSystem) {
        UnitSystem.METRIC -> distance / 1000.0
        UnitSystem.IMPERIAL -> distance / MilesToMeters
    }
    return value.toInputText(maxFractionDigits = 2)
}

private fun routeElevationInputText(routeImport: RouteFileImport, unitSystem: UnitSystem): String {
    val elevation = routeImport.elevationGainedMeters.takeIf { it > 0.0 } ?: return ""
    val value = when (unitSystem) {
        UnitSystem.METRIC -> elevation
        UnitSystem.IMPERIAL -> elevation / FeetToMeters
    }
    return value.toInputText(maxFractionDigits = 1)
}

private fun List<ExerciseRoutePoint>.withActivityTimeRange(
    start: java.time.Instant,
    end: java.time.Instant,
): List<ExerciseRoutePoint> {
    if (isEmpty()) return emptyList()
    val totalMillis = Duration.between(start, end)
        .toMillis()
        .coerceAtLeast(size.toLong())
    val lastOffset = (totalMillis - 1).coerceAtLeast(0L)
    return mapIndexed { index, point ->
        val offset = if (size == 1) {
            0L
        } else {
            lastOffset * index / (size - 1)
        }
        point.copy(time = start.plusMillis(offset))
    }
}

private fun List<ActivityPauseInterval>.insideActivityRange(
    start: java.time.Instant,
    end: java.time.Instant,
): List<ActivityPauseInterval> =
    sortedBy { it.startTime }
        .filter { interval ->
            !interval.startTime.isBefore(start) &&
                interval.startTime.isBefore(interval.endTime) &&
                !interval.endTime.isAfter(end)
        }

private fun Double.toInputText(maxFractionDigits: Int): String =
    "%.${maxFractionDigits}f"
        .format(Locale.US, this)
        .trimEnd('0')
        .trimEnd('.')

private fun Double.toCaloriesInputText(): String =
    roundToInt()
        .coerceAtLeast(1)
        .toString()

private fun String.toPositiveDoubleOrNull(): Double? =
    trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it > 0.0 }

private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
