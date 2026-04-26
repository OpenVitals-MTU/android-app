package tech.mmarca.openvitals.features.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActivityDetailUiState(
    val isLoading: Boolean = true,
    val workout: ExerciseData? = null,
    val error: String? = null,
)

class ActivityDetailViewModel(
    private val repository: ActivityRepository,
    private val activityId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityDetailUiState())
    val uiState: StateFlow<ActivityDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (activityId.isBlank()) {
            _uiState.value = ActivityDetailUiState(
                isLoading = false,
                error = "Missing activity id.",
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { repository.loadWorkout(activityId) }
                .onSuccess { workout ->
                    _uiState.value = ActivityDetailUiState(
                        isLoading = false,
                        workout = workout,
                        error = if (workout == null) "Activity not found." else null,
                    )
                }
                .onFailure {
                    _uiState.value = ActivityDetailUiState(
                        isLoading = false,
                        error = it.message ?: "Unable to load activity.",
                    )
                }
        }
    }
}
