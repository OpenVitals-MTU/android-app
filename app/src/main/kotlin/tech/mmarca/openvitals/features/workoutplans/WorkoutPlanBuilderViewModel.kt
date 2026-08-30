package tech.mmarca.openvitals.features.workoutplans

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.healthconnect.isOpenVitalsRecord
import tech.mmarca.openvitals.navigation.WORKOUT_PLAN_ID_ARG

@HiltViewModel
class WorkoutPlanBuilderViewModel(
    private val repository: ActivityRepository,
    private val appPackageName: String,
    private val clock: Clock,
    private val planId: String?,
) : ViewModel() {

    @Inject
    constructor(
        repository: ActivityRepository,
        @ApplicationContext context: Context,
        savedStateHandle: SavedStateHandle,
    ) : this(
        repository = repository,
        appPackageName = context.packageName,
        clock = Clock.systemDefaultZone(),
        planId = savedStateHandle[WORKOUT_PLAN_ID_ARG],
    )

    private val _uiState = MutableStateFlow(WorkoutPlanBuilderUiState(planId = planId, isLoading = true))
    val uiState: StateFlow<WorkoutPlanBuilderUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val writePermissions = repository.plannedWorkoutWritePermissions()
            val isAvailable = writePermissions.isNotEmpty()
            if (planId == null) {
                val form = workoutPlanFormForNewPlan(LocalDateTime.now(clock))
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAvailable = isAvailable,
                        writePermissions = writePermissions,
                        form = form,
                        baseline = form,
                        error = null,
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, error = null, writePermissions = writePermissions, isAvailable = isAvailable) }
            runCatching { repository.loadPlannedWorkout(planId) }
                .onSuccess { plan ->
                    if (plan == null) {
                        _uiState.update { it.copy(isLoading = false, error = ScreenError.NotFound) }
                        return@onSuccess
                    }
                    val form = plan.toWorkoutPlanForm(clock.zone)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOwnedByApp = isOpenVitalsRecord(plan.dataOriginPackage, appPackageName),
                            form = form,
                            baseline = form,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false, error = throwable.toScreenError()) }
                }
        }
    }

    // --- session fields --------------------------------------------------

    fun updateTitle(text: String) = updateForm { copy(titleText = text) }
    fun updateNotes(text: String) = updateForm { copy(notesText = text) }
    fun updateSessionType(exerciseType: Int) = updateForm { copy(sessionExerciseType = exerciseType) }
    fun updateStartDate(text: String) = updateForm { copy(startDateText = text) }
    fun updateStartTime(text: String) = updateForm { copy(startTimeText = text) }
    fun updateDurationMinutes(text: String) = updateForm { copy(durationMinutesText = text) }

    // --- blocks ----------------------------------------------------------

    fun addBlock() = updateForm { copy(blocks = blocks + WorkoutPlanBlockInput()) }

    fun removeBlock(blockId: String) = updateForm { copy(blocks = blocks.filterNot { it.id == blockId }) }

    fun moveBlock(blockId: String, delta: Int) = updateForm {
        val index = blocks.indexOfFirst { it.id == blockId }
        val target = index + delta
        if (index < 0 || target !in blocks.indices) return@updateForm this
        copy(blocks = blocks.toMutableList().apply { add(target, removeAt(index)) })
    }

    fun updateBlockName(blockId: String, text: String) = updateBlock(blockId) { copy(nameText = text) }

    fun updateBlockRounds(blockId: String, text: String) = updateBlock(blockId) { copy(roundsText = text) }

    // --- steps -----------------------------------------------------------

    fun addStep(blockId: String, choice: WorkoutPlanStepChoice) =
        updateBlock(blockId) { copy(steps = steps + WorkoutPlanStepInput.active(choice)) }

    fun addRestStep(blockId: String) =
        updateBlock(blockId) { copy(steps = steps + WorkoutPlanStepInput.rest()) }

    fun removeStep(blockId: String, stepId: String) =
        updateBlock(blockId) { copy(steps = steps.filterNot { it.id == stepId }) }

    fun moveStep(blockId: String, fromIndex: Int, toIndex: Int) = updateBlock(blockId) {
        if (fromIndex !in steps.indices || toIndex !in steps.indices) return@updateBlock this
        copy(steps = steps.toMutableList().apply { add(toIndex, removeAt(fromIndex)) })
    }

    fun updateStepGoalType(blockId: String, stepId: String, goalType: WorkoutPlanGoalType) =
        updateStep(blockId, stepId) { copy(goalType = goalType) }

    fun updateStepGoalValue(blockId: String, stepId: String, text: String) =
        updateStep(blockId, stepId) { copy(goalValueText = text) }

    fun updateStepDescription(blockId: String, stepId: String, text: String) =
        updateStep(blockId, stepId) { copy(descriptionText = text) }

    // --- save ------------------------------------------------------------

    fun save() {
        val current = _uiState.value
        if (current.isSaving || !current.canEdit) return
        val errors = validateWorkoutPlan(current.form)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }
        val request = current.form.toWriteRequest(clock.zone, existingId = planId) ?: return
        _uiState.update { it.copy(isSaving = true, validationErrors = emptySet(), error = null) }
        viewModelScope.launch {
            runCatching { repository.writePlannedWorkout(request) }
                .onSuccess { savedId ->
                    _uiState.update { it.copy(isSaving = false, baseline = it.form, savedPlanId = savedId) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isSaving = false, error = throwable.toScreenError()) }
                }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    // --- helpers ---------------------------------------------------------

    private fun updateForm(transform: WorkoutPlanFormInput.() -> WorkoutPlanFormInput) {
        _uiState.update { state ->
            val form = state.form.transform()
            state.copy(
                form = form,
                // Re-validate live only once the user has seen errors; before that the
                // form stays quiet until the first save attempt.
                validationErrors = if (state.validationErrors.isEmpty()) emptySet() else validateWorkoutPlan(form),
            )
        }
    }

    private fun updateBlock(blockId: String, transform: WorkoutPlanBlockInput.() -> WorkoutPlanBlockInput) =
        updateForm { copy(blocks = blocks.map { if (it.id == blockId) it.transform() else it }) }

    private fun updateStep(
        blockId: String,
        stepId: String,
        transform: WorkoutPlanStepInput.() -> WorkoutPlanStepInput,
    ) = updateBlock(blockId) { copy(steps = steps.map { if (it.id == stepId) it.transform() else it }) }

    internal val zone: ZoneId get() = clock.zone
}
