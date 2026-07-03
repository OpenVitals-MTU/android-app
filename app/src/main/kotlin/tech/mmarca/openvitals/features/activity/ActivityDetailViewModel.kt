package tech.mmarca.openvitals.features.activity

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.data.repository.ActivityMarkerRepository
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.domain.model.withSampleBackfilledMetrics
import tech.mmarca.openvitals.navigation.ACTIVITY_DETAIL_ID_ARG
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class ActivityDetailUiState(
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val workout: ExerciseData? = null,
    val heartRateSamples: List<HeartRateSample> = emptyList(),
    val speedSamples: List<SpeedSample> = emptyList(),
    val cadenceSamples: List<ActivityCadenceSample> = emptyList(),
    val markers: List<ActivityRecordingMarker> = emptyList(),
    val error: ScreenError? = null,
)

@HiltViewModel
class ActivityDetailViewModel(
    private val repository: ActivityRepository,
    private val activityId: String,
    private val heartRepository: HeartRepository? = null,
    private val markerRepository: ActivityMarkerRepository? = null,
) : ViewModel() {

    @Inject
    constructor(
        repository: ActivityRepository,
        heartRepository: HeartRepository,
        markerRepository: ActivityMarkerRepository,
        savedStateHandle: SavedStateHandle,
    ) : this(
        repository = repository,
        activityId = savedStateHandle[ACTIVITY_DETAIL_ID_ARG] ?: "",
        heartRepository = heartRepository,
        markerRepository = markerRepository,
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
                error = ScreenError.MissingArgument,
            )
            return
        }

        loadCoordinator.launch(viewModelScope) load@{
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { repository.loadWorkout(activityId) }
                .onSuccess { workout ->
                    if (!isCurrent) return@load
                    val heartRateSamples = if (workout != null) {
                        heartRepository?.loadHeartRateSamples(workout.startTime, workout.endTime)
                            .orEmpty()
                    } else {
                        emptyList()
                    }
                    val speedSamples = if (workout != null) {
                        repository.loadSpeedSamples(workout.startTime, workout.endTime)
                    } else {
                        emptyList()
                    }
                    val cadenceSamples = if (workout != null) {
                        repository.loadActivityCadenceSamples(workout.startTime, workout.endTime)
                    } else {
                        emptyList()
                    }
                    val backfilledWorkout = workout?.withSampleBackfilledMetrics(
                        heartRateSamples = heartRateSamples,
                        speedSamples = speedSamples,
                        cadenceSamples = cadenceSamples,
                    )
                    _uiState.value = ActivityDetailUiState(
                        isLoading = false,
                        workout = backfilledWorkout,
                        heartRateSamples = heartRateSamples,
                        speedSamples = speedSamples,
                        cadenceSamples = cadenceSamples,
                        markers = workout?.let { loadedWorkout ->
                            markerRepository?.markersForActivity(loadedWorkout.id).orEmpty()
                                .ifEmpty {
                                    loadedWorkout.clientRecordId
                                        ?.let { markerRepository?.markersForActivity(it) }
                                        .orEmpty()
                                }
                        }.orEmpty(),
                        error = if (workout == null) ScreenError.NotFound else null,
                    )
                }
                .onFailure {
                    if (!isCurrent) return@load
                    _uiState.value = ActivityDetailUiState(
                        isLoading = false,
                        error = it.toScreenError("Unable to load activity."),
                    )
                }
        }
    }

    fun deleteActivity(onDeleted: () -> Unit = {}) {
        val workout = _uiState.value.workout ?: return
        if (!workout.isOpenVitalsEntry || workout.id.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            runCatching {
                repository.deleteActivityEntry(workout.id)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isDeleting = false, workout = null)
                onDeleted()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = error.toScreenError("Unable to delete activity."),
                )
            }
        }
    }
}
