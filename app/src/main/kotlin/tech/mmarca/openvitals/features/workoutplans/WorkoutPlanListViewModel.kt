package tech.mmarca.openvitals.features.workoutplans

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import tech.mmarca.openvitals.domain.model.PlannedExerciseWriteRequest
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.healthconnect.isOpenVitalsRecord

@HiltViewModel
class WorkoutPlanListViewModel(
    private val repository: ActivityRepository,
    private val appPackageName: String,
    private val clock: Clock,
) : ViewModel() {

    @Inject
    constructor(
        repository: ActivityRepository,
        @ApplicationContext context: Context,
    ) : this(
        repository = repository,
        appPackageName = context.packageName,
        clock = Clock.systemDefaultZone(),
    )

    private val _uiState = MutableStateFlow(WorkoutPlanListUiState())
    val uiState: StateFlow<WorkoutPlanListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val isAvailable = repository.plannedWorkoutWritePermissions().isNotEmpty()
            if (!isAvailable) {
                _uiState.update { it.copy(isLoading = false, isAvailable = false, items = emptyList()) }
                return@launch
            }
            val today = LocalDate.now(clock)
            runCatching {
                repository.loadPlannedWorkouts(today.minusYears(1), today.plusYears(1))
            }.onSuccess { plans ->
                _uiState.update {
                    it.copy(isLoading = false, isAvailable = true, items = plans.map { plan -> plan.toListItem(today) })
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, isAvailable = true, error = throwable.toScreenError()) }
            }
        }
    }

    fun requestDelete(planId: String) = _uiState.update { it.copy(pendingDeleteId = planId) }

    fun cancelDelete() = _uiState.update { it.copy(pendingDeleteId = null) }

    fun confirmDelete() {
        val planId = _uiState.value.pendingDeleteId ?: return
        _uiState.update { it.copy(pendingDeleteId = null) }
        viewModelScope.launch {
            runCatching { repository.deletePlannedWorkout(planId) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            items = state.items.filterNot { it.plan.id == planId },
                            message = WorkoutPlanListMessage.DELETED,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(error = throwable.toScreenError(), message = WorkoutPlanListMessage.ACTION_FAILED) }
                }
        }
    }

    fun copyToToday(planId: String) {
        val plan = _uiState.value.items.firstOrNull { it.plan.id == planId }?.plan ?: return
        viewModelScope.launch {
            runCatching {
                repository.writePlannedWorkout(plan.copyForToday())
            }.onSuccess {
                _uiState.update { it.copy(message = WorkoutPlanListMessage.COPIED_TO_TODAY) }
                refresh()
            }.onFailure { throwable ->
                _uiState.update { it.copy(error = throwable.toScreenError(), message = WorkoutPlanListMessage.ACTION_FAILED) }
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    /** Copy to today and hand the fresh plan to the screen to start. */
    fun repeatPlan(planId: String) {
        val plan = _uiState.value.items.firstOrNull { it.plan.id == planId }?.plan ?: return
        viewModelScope.launch {
            runCatching { repository.writePlannedWorkout(plan.copyForToday()) }
                .onSuccess { newId -> _uiState.update { it.copy(pendingStartPlanId = newId) } }
                .onFailure { throwable ->
                    _uiState.update { it.copy(error = throwable.toScreenError(), message = WorkoutPlanListMessage.ACTION_FAILED) }
                }
        }
    }

    fun onStartPlanHandled() = _uiState.update { it.copy(pendingStartPlanId = null) }

    /** Every plan the list knows, as the export file's text; null when there is nothing to export. */
    fun exportJson(): String? =
        _uiState.value.items.map { it.plan }.takeIf { it.isNotEmpty() }?.toExportJson(Instant.now(clock))

    /** Writes each plan in an export file to Health Connect as a new plan. */
    fun importJson(text: String) {
        val requests = parseWorkoutPlanExport(text)
        if (requests.isNullOrEmpty()) {
            _uiState.update { it.copy(message = WorkoutPlanListMessage.ACTION_FAILED) }
            return
        }
        viewModelScope.launch {
            runCatching { requests.forEach { repository.writePlannedWorkout(it) } }
                .onSuccess {
                    _uiState.update { it.copy(message = WorkoutPlanListMessage.IMPORTED, importedCount = requests.size) }
                    refresh()
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(error = throwable.toScreenError(), message = WorkoutPlanListMessage.ACTION_FAILED) }
                }
        }
    }

    fun onExported() = _uiState.update { it.copy(message = WorkoutPlanListMessage.EXPORTED) }

    /**
     * Today's copy keeps the plan's clock time unless that has already gone by,
     * in which case it is scheduled for now — a plan copied to "earlier today"
     * would sit in the past before it was ever started.
     */
    private fun PlannedExerciseData.copyForToday(): PlannedExerciseWriteRequest {
        val today = LocalDate.now(clock)
        val now = LocalTime.now(clock).withSecond(0).withNano(0)
        val original = startTime.atZone(clock.zone).toLocalTime()
        return toCopyForDate(today, clock.zone, startTimeOfDay = if (original.isBefore(now)) now else original)
    }

    private fun PlannedExerciseData.toListItem(today: LocalDate): WorkoutPlanListItem {
        val date = startTime.atZone(startZoneOffset ?: clock.zone).toLocalDate()
        val group = when {
            completedExerciseSessionId != null -> WorkoutPlanGroup.PAST
            date.isEqual(today) -> WorkoutPlanGroup.TODAY
            date.isAfter(today) -> WorkoutPlanGroup.UPCOMING
            else -> WorkoutPlanGroup.PAST
        }
        return WorkoutPlanListItem(
            plan = this,
            isOwnedByApp = isOpenVitalsRecord(dataOriginPackage, appPackageName),
            group = group,
        )
    }
}
