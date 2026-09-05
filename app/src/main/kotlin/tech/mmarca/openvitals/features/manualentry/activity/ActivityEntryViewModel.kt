package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import android.content.Context
import android.location.Location
import dagger.hilt.android.qualifiers.ApplicationContext
import android.net.Uri
import androidx.health.connect.client.records.ExerciseSegment
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
import tech.mmarca.openvitals.features.workoutplans.toCopyForDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.onScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.data.repository.ActivityMarkerRepository
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.CoMapsNavigationRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.model.ActivityRecordingLap
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline
import tech.mmarca.openvitals.domain.model.ExerciseLapData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardLayout
import tech.mmarca.openvitals.navigation.ACTIVITY_ENTRY_ID_ARG
import tech.mmarca.openvitals.navigation.ACTIVITY_ENTRY_MODE_ARG
import tech.mmarca.openvitals.navigation.ACTIVITY_ENTRY_PLAN_ID_ARG
import tech.mmarca.openvitals.navigation.ACTIVITY_ENTRY_TYPE_ARG
import tech.mmarca.openvitals.navigation.Screen
import tech.mmarca.openvitals.core.presentation.isPermissionFailure
import tech.mmarca.openvitals.features.activity.exerciseTypeLabel
import tech.mmarca.openvitals.features.workoutplans.WorkoutPlanGoalType
import tech.mmarca.openvitals.features.workoutplans.WorkoutPlanStepChoice
import tech.mmarca.openvitals.features.workoutplans.planRequestFromRows
import tech.mmarca.openvitals.features.workoutplans.toPlannedBlocks
import tech.mmarca.openvitals.features.workoutplans.toRepetitionSetInput
import tech.mmarca.openvitals.features.workoutplans.toRepetitionSetInputs
import tech.mmarca.openvitals.features.workoutplans.toPlanRunSteps

@HiltViewModel
class ActivityEntryViewModel(
    private val repository: ActivityRepository,
    private val heartRepository: HeartRepository? = null,
    private val routeFileImporter: RouteFileImporter? = null,
    private val activityRecorder: ActivityRecordingController? = null,
    private val recordingDraftStore: ActivityRecordingDraftStore? = null,
    private val preferencesRepository: PreferencesRepository? = null,
    private val markerRepository: ActivityMarkerRepository? = null,
    private val coMapsNavigationRepository: CoMapsNavigationRepository? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val editActivityId: String? = null,
    private val launchMode: String? = null,
    private val launchPlanId: String? = null,
    private val launchActivityTypeId: String? = null,
    private val appContext: Context? = null,
) : ViewModel() {

    @Inject
    constructor(
        repository: ActivityRepository,
        heartRepository: HeartRepository,
        routeFileImporter: RouteFileImporter,
        activityRecorder: ActivityRecordingController,
        recordingDraftStore: ActivityRecordingDraftStore,
        preferencesRepository: PreferencesRepository,
        markerRepository: ActivityMarkerRepository,
        coMapsNavigationRepository: CoMapsNavigationRepository,
        savedStateHandle: SavedStateHandle,
        @ApplicationContext context: Context,
    ) : this(
        repository = repository,
        heartRepository = heartRepository,
        routeFileImporter = routeFileImporter,
        activityRecorder = activityRecorder,
        recordingDraftStore = recordingDraftStore,
        preferencesRepository = preferencesRepository,
        markerRepository = markerRepository,
        coMapsNavigationRepository = coMapsNavigationRepository,
        clock = Clock.systemDefaultZone(),
        editActivityId = savedStateHandle[ACTIVITY_ENTRY_ID_ARG],
        launchMode = savedStateHandle[ACTIVITY_ENTRY_MODE_ARG],
        launchPlanId = savedStateHandle[ACTIVITY_ENTRY_PLAN_ID_ARG],
        launchActivityTypeId = savedStateHandle[ACTIVITY_ENTRY_TYPE_ARG],
        appContext = context.applicationContext,
    )

    private var editEntryLoaded = false

    private val _uiState = MutableStateFlow(
        initialState(recordingDraftStore?.restore())
    )
    val uiState: StateFlow<ActivityEntryUiState> = _uiState.asStateFlow()
    private val fallbackRecordingState = MutableStateFlow(ActivityRecordingState())
    val recordingState: StateFlow<ActivityRecordingState> =
        activityRecorder?.state ?: fallbackRecordingState.asStateFlow()

    private val fallbackCoMapsNavigation =
        MutableStateFlow<CoMapsNavigationState>(CoMapsNavigationState.Disabled)
    val coMapsNavigation: StateFlow<CoMapsNavigationState> =
        activityRecorder?.coMapsNavigation ?: fallbackCoMapsNavigation.asStateFlow()

    private val fallbackCoMapsRoute = MutableStateFlow<CoMapsRoutePolyline?>(null)
    val coMapsRoute: StateFlow<CoMapsRoutePolyline?> =
        activityRecorder?.coMapsRoute ?: fallbackCoMapsRoute.asStateFlow()

    fun refreshCoMapsGuidance() {
        activityRecorder?.refreshCoMapsGuidance()
    }

    fun planInCoMaps() {
        activityRecorder?.planInCoMaps()
    }

    fun coMapsPermissionName(): String? = activityRecorder?.coMapsPermissionName()

    fun setCoMapsPrestartWatch(active: Boolean) {
        activityRecorder?.setCoMapsPrestartWatch(active)
    }

    init {
        refreshPermission()
        activityRecorder?.state
            ?.onEach { recording ->
                if (recording.isActive) {
                    applyRecordingProgress(recording)
                }
            }
            ?.launchIn(viewModelScope)
        applyLaunchIntent()
    }

    /** Applies the navigation arguments' intent. Fresh entries only; an edit or a draft wins. */
    private fun applyLaunchIntent() {
        if (editActivityId != null) return
        if (_uiState.value.isRecordingDraft) return

        launchActivityTypeId
            ?.let { typeId -> DefaultActivityEntryTypes.firstOrNull { it.id == typeId } }
            ?.let { type -> _uiState.value = _uiState.value.copy(selectedActivityType = type) }

        when {
            launchPlanId != null && launchMode == Screen.ActivityEntryMode.RECORD -> prepareGuidedPlan(launchPlanId)
            launchPlanId != null -> startWithPlan(launchPlanId)
            launchMode == Screen.ActivityEntryMode.RECORD -> prepareGpsRecording()
            launchMode == Screen.ActivityEntryMode.MANUAL -> startManualEntry()
            // The bare route and the legacy PLAN intent both land on the hub.
            else -> loadHubPlans()
        }
    }

    /** Opens the plan into the prefilled form. A missing plan falls back to the hub. */
    fun startWithPlan(planId: String) {
        recordingDraftStore?.clear()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                mode = ActivityEntryMode.START_HUB,
                isLoadingHubPlans = true,
                isRecordingDraft = false,
                entryError = null,
                detailError = null,
                validationErrors = emptySet(),
            )
            runCatching { repository.loadPlannedWorkout(planId) }
                .onSuccess { plan ->
                    if (plan != null) {
                        applyPlannedWorkout(plan)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoadingHubPlans = false,
                            entryError = ActivityEntryError.PLAN_NOT_FOUND,
                        )
                        loadHubPlans()
                    }
                }
                .onFailure { error ->
                    showStartHub()
                    _uiState.value = _uiState.value.copy(hubPlansError = error.toScreenError())
                }
        }
    }

    /** The start hub: today's and upcoming plans, then record / log manually. */
    fun showStartHub() {
        if (_uiState.value.isEditMode) return
        recordingDraftStore?.clear()
        activityRecorder?.stopBlePreview()
        activityRecorder?.clearPreparedRecording()
        _uiState.value = initialActivityEntryState(clock, repository, preferredActivityType()).copy(
            mode = ActivityEntryMode.START_HUB,
            canWrite = _uiState.value.canWrite,
            isCheckingPermission = _uiState.value.isCheckingPermission,
            hubPlans = _uiState.value.hubPlans,
            hubPlansAvailable = _uiState.value.hubPlansAvailable,
            editRecordId = editActivityId,
        )
        refreshPermission()
        loadHubPlans()
    }

    fun loadHubPlans() {
        val available = repository.plannedWorkoutWritePermissions().isNotEmpty()
        if (!available) {
            _uiState.value = _uiState.value.copy(hubPlansAvailable = false, hubPlans = emptyList(), isLoadingHubPlans = false)
            return
        }
        val today = LocalDate.now(clock)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHubPlans = true, hubPlansError = null, hubPlansAvailable = true)
            runCatching { repository.loadExistingPlannedWorkouts(today) }
                .onSuccess { plans ->
                    // A missing permission costs the Repeat row, never the hub.
                    val recent = runCatching {
                        repository.loadPlannedWorkouts(today.minusDays(RecentPlanLookbackDays), today)
                    }.getOrDefault(emptyList())
                        .filter { it.completedExerciseSessionId != null }
                        .sortedByDescending { it.startTime }
                        .distinctBy { it.title ?: it.id }
                        .take(MaxRecentPlans)
                    _uiState.value = _uiState.value.copy(
                        isLoadingHubPlans = false,
                        hubPlans = plans
                            .filter { !it.startTime.atZone(it.startZoneOffset ?: clock.zone).toLocalDate().isBefore(today) }
                            .sortedBy { it.startTime },
                        recentPlans = recent,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingHubPlans = false,
                        hubPlans = emptyList(),
                        hubPlansError = error.toScreenError(),
                    )
                }
        }
    }

    /** A hub row's Start. A plan the recorder cannot walk falls back to the form. */
    fun prepareGuidedPlan(planId: String) {
        val cached = _uiState.value.hubPlans.firstOrNull { it.id == planId }
        if (cached != null) {
            showGuidedPlanSetup(cached)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHubPlans = true, entryError = null, detailError = null)
            runCatching { repository.loadPlannedWorkout(planId) }
                .onSuccess { plan ->
                    if (plan != null) {
                        showGuidedPlanSetup(plan)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            mode = ActivityEntryMode.START_HUB,
                            isLoadingHubPlans = false,
                            entryError = ActivityEntryError.PLAN_NOT_FOUND,
                        )
                        loadHubPlans()
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isLoadingHubPlans = false, hubPlansError = error.toScreenError())
                }
        }
    }

    private fun showGuidedPlanSetup(plan: PlannedExerciseData) {
        val activityType = plan.toActivityEntryType()
        val steps = plan.toPlanRunSteps(localizedTitle = { appContext?.getString(it.labelRes) })
        if (activityType == null || !activityType.isRepetitionLike || steps.isEmpty()) {
            recordingDraftStore?.clear()
            applyPlannedWorkout(plan)
            return
        }
        recordingDraftStore?.clear()
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.RECORDING,
            guidedPlan = ActivityGuidedPlan(plan, activityType, steps),
            selectedActivityType = activityType,
            linkedPlan = ActivityLinkedPlan(plan.id, plan.title),
            titleText = plan.title.orEmpty(),
            notesText = plan.notes.orEmpty(),
            importedRoute = null,
            recordedPauseIntervals = emptyList(),
            recordedLaps = emptyList(),
            recordedMarkers = emptyList(),
            isRecordingDraft = false,
            isLoadingHubPlans = false,
            distanceText = "",
            elevationText = "",
            entryError = null,
            detailError = null,
            validationErrors = emptySet(),
        )
        refreshPermission()
        activityRecorder?.clearPreparedRecording()
        activityRecorder?.previewBleConnections()
    }

    /** Setup's Start for a guided plan. */
    fun startPlanRecording() {
        val guided = _uiState.value.guidedPlan ?: return
        val recorder = activityRecorder
        if (recorder == null) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.RECORDING_FAILED,
                detailError = ScreenError.Message("Recording is not available."),
                validationErrors = emptySet(),
            )
            return
        }
        recordingDraftStore?.clear()
        val now = LocalDateTime.now(clock).withSecond(0).withNano(0)
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.RECORDING,
            selectedActivityType = guided.activityType,
            startDateText = DateTimeFormatter.ISO_LOCAL_DATE.format(now),
            startTimeText = TimeFormatter.format(now.toLocalTime()),
            durationMinutesText = "1",
            recordedRecoveryStartTime = null,
            entryError = null,
            detailError = null,
            validationErrors = emptySet(),
        )
        if (!recorder.startPlanRecording(guided.plan, guided.activityType)) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.RECORDING_FAILED,
                detailError = recorder.state.value.errorMessage?.let(ScreenError::Message),
                validationErrors = emptySet(),
            )
        }
    }

    fun completePlanStep() {
        activityRecorder?.completeCurrentPlanStep()
    }

    fun skipPlanStep() {
        activityRecorder?.skipPlanStep()
    }

    fun undoPlanStep() {
        activityRecorder?.undoPlanStep()
    }

    /** A Repeat row: today's copy of a plan done before, straight into the guided setup. */
    fun repeatPlan(planId: String) {
        val plan = _uiState.value.recentPlans.firstOrNull { it.id == planId } ?: return
        val today = LocalDate.now(clock)
        val now = LocalTime.now(clock).withSecond(0).withNano(0)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHubPlans = true, entryError = null, detailError = null)
            runCatching { repository.writePlannedWorkout(plan.toCopyForDate(today, clock.zone, startTimeOfDay = now)) }
                .onSuccess { newId -> prepareGuidedPlan(newId) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingHubPlans = false,
                        writePermissions = repository.plannedWorkoutWritePermissions(),
                        canWrite = false,
                        entryError = if (error.isPermissionFailure()) {
                            ActivityEntryError.MISSING_WRITE_PERMISSION
                        } else {
                            ActivityEntryError.WRITE_FAILED
                        },
                        detailError = error.toScreenError(),
                    )
                }
        }
    }

    /** A hub row: log the session from that plan. */
    fun logFromPlan(planId: String) {
        val plan = _uiState.value.hubPlans.firstOrNull { it.id == planId } ?: return
        recordingDraftStore?.clear()
        applyPlannedWorkout(plan)
    }

    /** After the builder saved the linked plan (under a new id): pick the edited version up again. */
    fun reapplyPlan(planId: String) {
        viewModelScope.launch {
            runCatching { repository.loadPlannedWorkout(planId) }
                .onSuccess { plan -> if (plan != null) applyPlannedWorkout(plan) }
                .onFailure { error -> _uiState.value = _uiState.value.copy(detailError = error.toScreenError()) }
        }
    }

    fun clearLinkedPlan() {
        _uiState.value = _uiState.value.copy(linkedPlan = null)
    }

    private fun applyPlannedWorkout(plan: PlannedExerciseData) {
        val activityType = plan.toActivityEntryType() ?: _uiState.value.selectedActivityType
        val sets = plan.toRepetitionSetInputs(activityType.segmentType)
        val selectedAt = LocalDateTime.now(clock).withSecond(0).withNano(0)
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.MANUAL,
            selectedActivityType = activityType,
            linkedPlan = ActivityLinkedPlan(plan.id, plan.title),
            titleText = plan.title.orEmpty(),
            notesText = plan.notes.orEmpty(),
            startDateText = DateTimeFormatter.ISO_LOCAL_DATE.format(selectedAt),
            startTimeText = TimeFormatter.format(selectedAt.toLocalTime()),
            durationMinutesText = plan.durationMinutesText(),
            repetitionMode = ActivityRepetitionEntryMode.SETS,
            repetitionTotalText = "",
            repetitionSets = sets.ifEmpty { listOf(ActivityRepetitionSetInput()) },
            isLoadingHubPlans = false,
            isRecordingDraft = false,
            entryError = null,
            detailError = null,
            validationErrors = emptySet(),
        )
        refreshPermission()
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
                detailError = null,
                validationErrors = emptySet(),
                editRecordId = null,
                saveCompleted = false,
            )
        }

        return initialActivityEntryState(clock, repository, preferredActivityType()).copy(
            mode = if (editActivityId == null) ActivityEntryMode.START_HUB else ActivityEntryMode.MANUAL,
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
                detailError = null,
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
                    detailError = error.toScreenError(),
                    writePermissions = currentRequiredPermissions(),
                )
            }
        }
    }

    fun selectActivityType(type: ActivityEntryType) {
        val currentState = _uiState.value
        val retainedRoute = currentState.importedRoute?.takeIf { import ->
            import.points.isEmpty() || type.supportsGpsRoute
        }
        _uiState.value = currentState.copy(
            selectedActivityType = type,
            // A mixed-exercise type has no single "total": its rows each name an exercise.
            repetitionMode = if (type.supportsMixedExercises) ActivityRepetitionEntryMode.SETS else currentState.repetitionMode,
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
            detailError = null,
            validationErrors = emptySet(),
        )
        refreshPermission()
    }

    fun startManualEntry() {
        recordingDraftStore?.clear()
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.MANUAL,
            linkedPlan = null,
            importedRoute = null,
            recordedPauseIntervals = emptyList(),
            recordedLaps = emptyList(),
            recordedMarkers = emptyList(),
            isRecordingDraft = false,
            entryError = null,
            detailError = null,
            validationErrors = emptySet(),
        )
        refreshPermission()
    }

    /** Kept for the recording screens' "choose another method" affordance. */
    fun chooseSource() = showStartHub()

    fun updateTitle(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.TITLE)) {
            copy(titleText = text, entryError = null, detailError = null)
        }
    }

    fun updateNotes(text: String) {
        updateState { copy(notesText = text, entryError = null, detailError = null) }
    }

    fun updateFeeling(feeling: ActivityEntryFeeling?) {
        updateState { copy(selectedFeeling = feeling, entryError = null, detailError = null) }
    }

    fun updateStartDate(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.START_DATE, ActivityEntryField.START_TIME)) {
            copy(startDateText = text, entryError = null, detailError = null)
        }
    }

    fun updateStartTime(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.START_TIME)) {
            copy(startTimeText = text, entryError = null, detailError = null)
        }
    }

    fun updateDurationMinutes(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.DURATION)) {
            copy(durationMinutesText = text, entryError = null, detailError = null)
        }
    }

    fun updateDistance(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.DISTANCE)) {
            copy(distanceText = text, entryError = null, detailError = null)
        }
    }

    fun updateElevation(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.ELEVATION)) {
            copy(elevationText = text, entryError = null, detailError = null)
        }
    }

    fun updateActiveCalories(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.ACTIVE_CALORIES, ActivityEntryField.TOTAL_CALORIES)) {
            copy(activeCaloriesText = text, entryError = null, detailError = null)
        }
    }

    fun updateTotalCalories(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.TOTAL_CALORIES)) {
            copy(totalCaloriesText = text, entryError = null, detailError = null)
        }
    }

    fun updateRepetitionMode(mode: ActivityRepetitionEntryMode) {
        updateState(clearFields = setOf(ActivityEntryField.REPETITIONS)) {
            copy(repetitionMode = mode, entryError = null, detailError = null)
        }
    }

    fun updateRepetitionTotal(text: String) {
        updateState(clearFields = setOf(ActivityEntryField.REPETITIONS)) {
            copy(repetitionTotalText = text, entryError = null, detailError = null)
        }
    }

    fun updateRepetitionSetRepetitions(index: Int, text: String) {
        updateState(clearFields = setOf(ActivityEntryField.REPETITIONS)) {
            copy(
                repetitionSets = repetitionSets.mapIndexed { itemIndex, item ->
                    if (itemIndex == index) item.copy(repetitionsText = text) else item
                },
                entryError = null,
                detailError = null,
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
                detailError = null,
            )
        }
    }

    /** "Add set": another round of whatever the last row was doing. */
    fun addRepetitionSet() {
        updateState(clearFields = setOf(ActivityEntryField.REPETITIONS)) {
            val template = repetitionSets.lastOrNull()
            copy(
                repetitionMode = ActivityRepetitionEntryMode.SETS,
                repetitionSets = repetitionSets + (template?.copy() ?: ActivityRepetitionSetInput()),
                entryError = null,
                detailError = null,
            )
        }
    }

    /** "Add exercise": a new row for a picked exercise, with the picker's default goal. */
    fun addExerciseStep(choice: WorkoutPlanStepChoice) {
        updateState(clearFields = setOf(ActivityEntryField.REPETITIONS)) {
            val blankOnly = repetitionSets.size == 1 && repetitionSets.single().repetitionsText.isBlank()
            copy(
                repetitionMode = ActivityRepetitionEntryMode.SETS,
                repetitionSets = (if (blankOnly) emptyList() else repetitionSets) + choice.toRepetitionSetInput(),
                entryError = null,
                detailError = null,
            )
        }
    }

    fun updateRepetitionSetExercise(index: Int, choice: WorkoutPlanStepChoice) {
        updateState(clearFields = setOf(ActivityEntryField.REPETITIONS)) {
            copy(
                repetitionSets = repetitionSets.mapIndexed { itemIndex, item ->
                    if (itemIndex != index) return@mapIndexed item
                    val goalChanges = item.repetitionsText.isBlank()
                    item.copy(
                        segmentType = choice.segmentType,
                        label = choice.description,
                        isDuration = if (goalChanges) choice.defaultGoal == WorkoutPlanGoalType.DURATION else item.isDuration,
                    )
                },
                entryError = null,
                detailError = null,
            )
        }
    }

    fun updateRepetitionSetGoalType(index: Int, isDuration: Boolean) {
        updateState(clearFields = setOf(ActivityEntryField.REPETITIONS)) {
            copy(
                repetitionSets = repetitionSets.mapIndexed { itemIndex, item ->
                    if (itemIndex == index) item.copy(isDuration = isDuration) else item
                },
                entryError = null,
                detailError = null,
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
                detailError = null,
            )
        }
    }

    fun importRouteFile(uri: Uri, units: ActivityEntryUnits) {
        val importer = routeFileImporter
        if (importer == null) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.ROUTE_IMPORT_FAILED,
                detailError = ScreenError.Message("Activity file import is not available."),
                validationErrors = emptySet(),
            )
            return
        }
        recordingDraftStore?.clear()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImportingRoute = true,
                isRecordingDraft = false,
                entryError = null,
                detailError = null,
                validationErrors = emptySet(),
            )
            runCatching { importer.import(uri) }
                .onSuccess { routeImport ->
                    applyRouteImport(routeImport, units)
                }
                .onScreenError(
                    logTag = TAG,
                    logMessage = "Activity file import failed",
                ) { screenError ->
                    _uiState.value = _uiState.value.copy(
                        isImportingRoute = false,
                        entryError = ActivityEntryError.ROUTE_IMPORT_FAILED,
                        detailError = screenError,
                        validationErrors = emptySet(),
                    )
                }
        }
    }

    /** Hands the steps to the plan builder: writes a one-block plan, links it, opens the builder. */
    fun saveAsPlan(units: ActivityEntryUnits) {
        val current = _uiState.value
        if (current.isSavingAsPlan) return
        val validationErrors = validateActivityEntry(current, units)
        val range = activityEntrySessionRange(current)
        val ownSegmentType = current.selectedActivityType.segmentType ?: ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT
        val blocks = when (current.repetitionMode) {
            ActivityRepetitionEntryMode.TOTAL -> listOf(ActivityRepetitionSetInput(repetitionsText = current.repetitionTotalText))
            ActivityRepetitionEntryMode.SETS -> current.repetitionSets
        }.toPlannedBlocks(ownSegmentType)
        if (validationErrors.isNotEmpty() || range == null || blocks == null) {
            _uiState.value = current.copy(
                entryError = ActivityEntryError.INVALID_VALUE,
                detailError = null,
                validationErrors = validationErrors,
            )
            return
        }
        val title = current.titleText.trim().ifEmpty {
            current.selectedActivityType.defaultTitle
                ?: appContext?.let { exerciseTypeLabel(it, current.selectedActivityType.exerciseType) }
                ?: ""
        }
        val request = planRequestFromRows(
            exerciseType = current.selectedActivityType.exerciseType,
            title = title,
            notes = current.activitySaveNotes(),
            startTime = range.first,
            endTime = range.second,
            blocks = blocks,
        )
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingAsPlan = true, entryError = null, detailError = null)
            runCatching { repository.writePlannedWorkout(request) }
                .onSuccess { savedPlanId ->
                    _uiState.value = _uiState.value.copy(
                        isSavingAsPlan = false,
                        linkedPlan = ActivityLinkedPlan(savedPlanId, title),
                        pendingBuilderPlanId = savedPlanId,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSavingAsPlan = false,
                        writePermissions = repository.plannedWorkoutWritePermissions(),
                        canWrite = false,
                        entryError = if (error.isPermissionFailure()) {
                            ActivityEntryError.MISSING_WRITE_PERMISSION
                        } else {
                            ActivityEntryError.WRITE_FAILED
                        },
                        detailError = error.toScreenError(),
                    )
                }
        }
    }

    fun onBuilderNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingBuilderPlanId = null)
    }

    fun clearImportedRoute() {
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.MANUAL,
            importedRoute = null,
            recordedPauseIntervals = emptyList(),
            recordedLaps = emptyList(),
            recordedMarkers = emptyList(),
            entryError = null,
            detailError = null,
            validationErrors = emptySet(),
        )
        refreshPermission()
    }

    fun reportLocationPermissionNeeded() {
        _uiState.value = _uiState.value.copy(
            entryError = ActivityEntryError.LOCATION_PERMISSION_NEEDED,
            detailError = null,
            validationErrors = emptySet(),
        )
    }

    fun reportNotificationPermissionNeeded() {
        _uiState.value = _uiState.value.copy(
            entryError = ActivityEntryError.NOTIFICATION_PERMISSION_NEEDED,
            detailError = null,
            validationErrors = emptySet(),
        )
    }

    fun reportActivityRecognitionPermissionNeeded() {
        _uiState.value = _uiState.value.copy(
            entryError = ActivityEntryError.ACTIVITY_RECOGNITION_PERMISSION_NEEDED,
            detailError = null,
            validationErrors = emptySet(),
        )
    }

    fun prepareGpsRecording() {
        val currentState = _uiState.value

        recordingDraftStore?.clear()
        // The setup screen offers today's plan as a shortcut.
        if (currentState.hubPlans.isEmpty()) loadHubPlans()
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
            detailError = null,
            validationErrors = emptySet(),
        )
        refreshPermission()
        activityRecorder?.clearPreparedRecording()
        activityRecorder?.previewBleConnections()
    }

    fun openRecordingDashboard(repetitionRestSeconds: Long = 0L) {
        val recorder = activityRecorder
        if (recorder == null) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.RECORDING_FAILED,
                detailError = ScreenError.Message("GPS recording is not available."),
                validationErrors = emptySet(),
            )
            return
        }
        val currentState = _uiState.value
        if (!currentState.selectedActivityType.supportsLiveRecording) {
            _uiState.value = currentState.copy(
                entryError = ActivityEntryError.INVALID_VALUE,
                detailError = null,
                validationErrors = setOf(ActivityEntryValidationError.ACTIVITY_TYPE_DOES_NOT_SUPPORT_ROUTE),
            )
            return
        }
        if (!currentState.selectedActivityType.supportsGpsRoute) {
            startGpsRecording(null, repetitionRestSeconds)
            return
        }

        recordingDraftStore?.clear()
        _uiState.value = currentState.copy(
            mode = ActivityEntryMode.RECORDING,
            importedRoute = null,
            recordedPauseIntervals = emptyList(),
            recordedLaps = emptyList(),
            recordedMarkers = emptyList(),
            isRecordingDraft = false,
            distanceText = "",
            elevationText = "",
            entryError = null,
            detailError = null,
            validationErrors = emptySet(),
        )
        recorder.prepareRecordingDashboard(currentState.selectedActivityType)
    }

    fun startGpsRecording(
        initialFix: Location? = null,
        repetitionRestSeconds: Long = 0L,
        withoutGps: Boolean = false,
    ) {
        val recorder = activityRecorder
        if (recorder == null) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.RECORDING_FAILED,
                detailError = ScreenError.Message("GPS recording is not available."),
                validationErrors = emptySet(),
            )
            return
        }
        val currentState = _uiState.value
        if (!currentState.selectedActivityType.supportsLiveRecording) {
            _uiState.value = currentState.copy(
                entryError = ActivityEntryError.INVALID_VALUE,
                detailError = null,
                validationErrors = setOf(ActivityEntryValidationError.ACTIVITY_TYPE_DOES_NOT_SUPPORT_ROUTE),
            )
            return
        }

        // With CoMaps guidance on, Start arms the dashboard so a route can be set
        // up first. The dashboard's own Start lands here too and passes.
        if (!withoutGps &&
            currentState.selectedActivityType.recordingKind() == ActivityRecordingKind.GPS_ROUTE &&
            recorder.state.value.activityTypeId == null &&
            preferencesRepository?.activityRecordingPreferences()
                ?.coMapsNavigationContextEnabled == true
        ) {
            recordingDraftStore?.clear()
            _uiState.value = currentState.copy(
                mode = ActivityEntryMode.RECORDING,
                entryError = null,
                detailError = null,
                validationErrors = emptySet(),
            )
            recorder.prepareRecordingDashboard(currentState.selectedActivityType)
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
            recordedRecoveryStartTime = null,
            entryError = null,
            detailError = null,
            validationErrors = emptySet(),
        )
        if (
            !recorder.startRecording(
                currentState.selectedActivityType,
                initialFix,
                repetitionRestSeconds,
                withoutGps = withoutGps,
            )
        ) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.RECORDING_FAILED,
                detailError = recorder.state.value.errorMessage?.let(ScreenError::Message),
                validationErrors = emptySet(),
            )
        }
    }

    /** Starts a guided heart-rate-recovery test: warm up, go hard, stop dead. */
    fun startHeartRateRecoveryTest(config: HeartRateRecoveryTestConfig) {
        val recorder = activityRecorder
        if (recorder == null) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.RECORDING_FAILED,
                detailError = ScreenError.Message("Recording is not available."),
                validationErrors = emptySet(),
            )
            return
        }
        val currentState = _uiState.value
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
            recordedRecoveryStartTime = null,
            entryError = null,
            detailError = null,
            validationErrors = emptySet(),
        )
        if (!recorder.startHeartRateRecoveryTest(currentState.selectedActivityType, config)) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.RECORDING_FAILED,
                detailError = recorder.state.value.errorMessage?.let(ScreenError::Message),
                validationErrors = emptySet(),
            )
        }
    }

    fun endHeartRateRecoveryEffort() {
        activityRecorder?.endHeartRateRecoveryEffort()
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

    fun updateRecordingDashboardLayout(layout: ActivityRecordingDashboardLayout) {
        activityRecorder?.updateDashboardLayout(layout)
    }

    fun discardGpsRecording() {
        activityRecorder?.discardRecording()
        activityRecorder?.stopBlePreview()
        recordingDraftStore?.clear()
        showStartHub()
    }

    fun discardRecordingDraft() {
        if (!_uiState.value.isRecordingDraft || _uiState.value.isEditMode) return
        recordingDraftStore?.clear()
        showStartHub()
    }

    fun finishGpsRecording(units: ActivityEntryUnits) {
        val snapshot = activityRecorder?.finishRecording()
        if (snapshot == null) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.RECORDING_FAILED,
                detailError = ScreenError.Message("No active activity recording was found."),
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
                units,
            )
            _uiState.value = _uiState.value.copy(
                recordedPauseIntervals = snapshot.pauseIntervals,
                recordedLaps = snapshot.manualLaps.map { it.toExerciseLapData() },
                recordedMarkers = snapshot.markers,
                repetitionMode = ActivityRepetitionEntryMode.TOTAL,
                repetitionTotalText = snapshot.repetitionCount.takeIf {
                    activityEntryTypeById(snapshot.activityTypeId)?.supportsStepCounting == true && it > 0L
                }?.toString().orEmpty(),
                isRecordingDraft = true,
                recordedBleSamples = snapshot.bleSamples,
                recordedCoMapsSamples = snapshot.coMapsNavigationSamples,
            )
        } else {
            applyRecordingWithoutRoute(snapshot)
        }
        recordingDraftStore?.store(_uiState.value)
    }

    fun loadEditEntry(units: ActivityEntryUnits) {
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
                        detailError = ScreenError.Message("Only OpenVitals entries can be edited."),
                        validationErrors = emptySet(),
                    )
                    return@onSuccess
                }
                val heartRateSamples = heartRepository?.loadHeartRateSamples(workout.startTime, workout.endTime)
                    .orEmpty()
                // The link keeps the plan marked completed; the title is decoration.
                val linkedPlan = workout.plannedExerciseSessionId?.let { planId ->
                    ActivityLinkedPlan(planId, runCatching { repository.loadPlannedWorkout(planId) }.getOrNull()?.title)
                }
                val current = _uiState.value
                _uiState.value = workout.toEditState(
                    units = units,
                    clock = clock,
                    repository = repository,
                    canWrite = current.canWrite,
                    isCheckingPermission = current.isCheckingPermission,
                ).copy(
                    linkedPlan = linkedPlan,
                    sessionHeartRateSamples = heartRateSamples,
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
                    detailError = error.toScreenError(),
                    validationErrors = emptySet(),
                )
            }
        }
    }

    fun addEntry(units: ActivityEntryUnits) {
        if (_uiState.value.mode == ActivityEntryMode.START_HUB) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.INVALID_VALUE,
                detailError = null,
                validationErrors = emptySet(),
            )
            return
        }

        val validationErrors = validateActivityEntry(_uiState.value, units)
        if (validationErrors.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.INVALID_VALUE,
                detailError = null,
                validationErrors = validationErrors,
            )
            return
        }

        val request = buildWriteRequest(_uiState.value, units)
        if (request == null) {
            _uiState.value = _uiState.value.copy(
                entryError = ActivityEntryError.INVALID_VALUE,
                detailError = null,
                validationErrors = validationErrors,
            )
            return
        }
        val editRecordId = _uiState.value.editRecordId
        val wasRecordingDraft = _uiState.value.isRecordingDraft
        val markersToSave = _uiState.value.recordedMarkers
        val coMapsSamplesToSave = _uiState.value.recordedCoMapsSamples
            .takeIf { _uiState.value.selectedActivityType.supportsGpsRoute }
            .orEmpty()
        val requestPermissions = repository.activityWritePermissions(request)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingEntry = true,
                entryError = null,
                detailError = null,
                validationErrors = emptySet(),
                writePermissions = requestPermissions,
            )
            val hasPermission = repository.hasActivityWritePermission(request)
            if (!hasPermission) {
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    canWrite = false,
                    entryError = ActivityEntryError.MISSING_WRITE_PERMISSION,
                    detailError = null,
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
                if (coMapsSamplesToSave.isNotEmpty()) {
                    coMapsNavigationRepository?.saveSamples(savedActivityId, coMapsSamplesToSave)
                }
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
                        detailError = null,
                        validationErrors = emptySet(),
                    )
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    entryError = ActivityEntryError.WRITE_FAILED,
                    detailError = error.toScreenError(),
                    validationErrors = emptySet(),
                )
            }
        }
    }

    fun onSaveCompletedHandled() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    private fun applyRouteImport(routeImport: RouteFileImport, units: ActivityEntryUnits) {
        _uiState.value = _uiState.value.withRouteImport(routeImport, units, clock)
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
            detailError = recording.errorMessage?.let(ScreenError::Message),
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
                repetitionsText = if (set.isDuration) {
                    (set.activeMillis / 1_000L).coerceAtLeast(1L).toString()
                } else {
                    set.repetitions.toString()
                },
                restMinutesText = set.restSeconds.takeIf { it > 0L }?.toString().orEmpty(),
                segmentType = set.segmentType?.takeIf { it != selectedActivityType.segmentType },
                label = set.label,
                isDuration = set.isDuration,
            )
        }
        val linkedPlan = snapshot.planId?.let { ActivityLinkedPlan(it, snapshot.planTitle) } ?: currentState.linkedPlan
        _uiState.value = _uiState.value.copy(
            mode = ActivityEntryMode.MANUAL,
            selectedActivityType = selectedActivityType,
            linkedPlan = linkedPlan,
            guidedPlan = null,
            titleText = currentState.titleText.ifBlank { snapshot.planTitle.orEmpty() },
            importedRoute = null,
            recordedPauseIntervals = snapshot.pauseIntervals,
            recordedLaps = snapshot.manualLaps.map { it.toExerciseLapData() },
            recordedMarkers = snapshot.markers,
            isRecordingDraft = true,
            startDateText = DateTimeFormatter.ISO_LOCAL_DATE.format(start),
            startTimeText = TimeFormatter.format(start.toLocalTime()),
            durationMinutesText = durationMinutes.toString(),
            // Distance is the one thing a GPS-less recording cannot know.
            distanceText = "",
            // Elevation came from the barometer, which never needed a position.
            elevationText = if (selectedActivityType.supportsElevation && snapshot.elevationGainedMeters > 0.0) {
                elevationInputText(
                    snapshot.elevationGainedMeters,
                    preferencesRepository?.let { prefs ->
                        prefs.unitOverride(UnitQuantity.ELEVATION) ?: prefs.unitSystem
                    } ?: UnitSystem.METRIC,
                )
            } else {
                ""
            },
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
            recordedBleSamples = snapshot.bleSamples,
            recordedRecoveryStartTime = snapshot.hrrEffortEndedAt,
            entryError = null,
            detailError = null,
            validationErrors = emptySet(),
        )
        refreshPermission()
    }

    private companion object {
        private const val TAG = "ActivityEntryViewModel"
        private const val RecentPlanLookbackDays = 30L
        private const val MaxRecentPlans = 3
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

private fun PlannedExerciseData.durationMinutesText(): String {
    val minutes = Duration.ofMillis(durationMs).toMinutes().coerceAtLeast(1L)
    return minutes.coerceIn(1, MaxActivityDurationMinutes).toString()
}
