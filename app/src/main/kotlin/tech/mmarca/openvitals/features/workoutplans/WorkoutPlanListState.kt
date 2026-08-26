package tech.mmarca.openvitals.features.workoutplans

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.model.PlannedExerciseData

enum class WorkoutPlanGroup {
    TODAY,
    UPCOMING,
    PAST,
}

enum class WorkoutPlanListMessage {
    DELETED,
    COPIED_TO_TODAY,
    EXPORTED,
    IMPORTED,
    ACTION_FAILED,
}

@Immutable
data class WorkoutPlanListItem(
    val plan: PlannedExerciseData,
    val isOwnedByApp: Boolean,
    val group: WorkoutPlanGroup,
) {
    val isCompleted: Boolean get() = plan.completedExerciseSessionId != null
    val canStart: Boolean get() = !isCompleted
    val canEdit: Boolean get() = isOwnedByApp
    val canDelete: Boolean get() = isOwnedByApp
}

@Immutable
data class WorkoutPlanListUiState(
    val isLoading: Boolean = true,
    val isAvailable: Boolean = true,
    val items: List<WorkoutPlanListItem> = emptyList(),
    val error: ScreenError? = null,
    val pendingDeleteId: String? = null,
    val message: WorkoutPlanListMessage? = null,
    /** One-shot: a plan just copied for today that the screen should start. */
    val pendingStartPlanId: String? = null,
    val importedCount: Int = 0,
) {
    fun items(group: WorkoutPlanGroup): List<WorkoutPlanListItem> = items.filter { it.group == group }
}
