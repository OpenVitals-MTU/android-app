package tech.mmarca.openvitals.features.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.navigation.ACTIVITY_DETAIL_ID_ARG
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActivityDetailUiState(
    val isLoading: Boolean = true,
    val workout: ExerciseData? = null,
    val error: String? = null,
)

@HiltViewModel
class ActivityDetailViewModel(
    private val repository: ActivityRepository,
    private val activityId: String,
) : ViewModel() {

    @Inject
    constructor(
        repository: ActivityRepository,
        savedStateHandle: SavedStateHandle,
    ) : this(
        repository = repository,
        activityId = savedStateHandle[ACTIVITY_DETAIL_ID_ARG] ?: "",
    )

    private val _uiState = MutableStateFlow(ActivityDetailUiState())
    val uiState: StateFlow<ActivityDetailUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

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

        loadCoordinator.launch(viewModelScope) load@{
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { repository.loadWorkout(activityId) }
                .onSuccess { workout ->
                    if (!isCurrent) return@load
                    _uiState.value = ActivityDetailUiState(
                        isLoading = false,
                        workout = workout,
                        error = if (workout == null) "Activity not found." else null,
                    )
                }
                .onFailure {
                    if (!isCurrent) return@load
                    _uiState.value = ActivityDetailUiState(
                        isLoading = false,
                        error = it.message ?: "Unable to load activity.",
                    )
                }
        }
    }
}
