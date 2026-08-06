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
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.CoMapsNavigationRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.domain.insights.ActivitySplits
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryReading
import tech.mmarca.openvitals.domain.insights.buildActivitySplits
import tech.mmarca.openvitals.domain.insights.calculateHeartRateRecovery
import tech.mmarca.openvitals.domain.insights.heartRateRecoveryWindowFor
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseRouteStatus
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.domain.model.withSampleBackfilledMetrics
import tech.mmarca.openvitals.domain.preferences.ActivitySplitDistance
import tech.mmarca.openvitals.navigation.ACTIVITY_DETAIL_ID_ARG
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
internal data class ActivityDetailUiState(
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val workout: ExerciseData? = null,
    val heartRateSamples: List<HeartRateSample> = emptyList(),
    val speedSamples: List<SpeedSample> = emptyList(),
    val cadenceSamples: List<ActivityCadenceSample> = emptyList(),
    val markers: List<ActivityRecordingMarker> = emptyList(),

    /** CoMaps guidance saved beside the activity; app-local, empty for most. */
    val coMapsSamples: List<CoMapsNavigationSnapshot> = emptyList(),

    /** Splits cut against the user's split-distance preference. */
    val splits: ActivitySplits = ActivitySplits.none(),

    /** The distance the derived splits were cut at, for the card headers. */
    val splitDistanceMeters: Double = ActivitySplitDistance.defaultMeters,

    /**
     * The slowest and fastest split, in seconds per kilometre — the pace-bar
     * scale. Null when no split has a pace (which leaves the bars unpainted).
     */
    val slowestSplitPaceSeconds: Double? = null,
    val fastestSplitPaceSeconds: Double? = null,

    /** Speed rebuilt from the splits, for a session that recorded none. */
    val splitSpeedTrace: ActivitySplitSpeedTrace? = null,

    /** The height profile of the session, oldest first. */
    val elevationSamples: List<ActivityElevationSample> = emptyList(),

    /** How the heart rate fell after the effort stopped, for a session that carries a stop mark. */
    val heartRateRecovery: HeartRateRecoveryReading? = null,

    val error: ScreenError? = null,
)

@HiltViewModel
internal class ActivityDetailViewModel(
    private val repository: ActivityRepository,
    private val activityId: String,
    private val heartRepository: HeartRepository? = null,
    private val markerRepository: ActivityMarkerRepository? = null,
    private val preferencesRepository: PreferencesRepository? = null,
    private val coMapsNavigationRepository: CoMapsNavigationRepository? = null,
) : ViewModel() {

    @Inject
    constructor(
        repository: ActivityRepository,
        heartRepository: HeartRepository,
        markerRepository: ActivityMarkerRepository,
        preferencesRepository: PreferencesRepository,
        coMapsNavigationRepository: CoMapsNavigationRepository,
        savedStateHandle: SavedStateHandle,
    ) : this(
        repository = repository,
        activityId = savedStateHandle[ACTIVITY_DETAIL_ID_ARG] ?: "",
        heartRepository = heartRepository,
        markerRepository = markerRepository,
        preferencesRepository = preferencesRepository,
        coMapsNavigationRepository = coMapsNavigationRepository,
    )

    private val _uiState = MutableStateFlow(ActivityDetailUiState())
    val uiState: StateFlow<ActivityDetailUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        load()
        observeSplitDistance()
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
                    // Speed, cadence, markers and the recovery each degrade to
                    // empty on failure: a missing permission costs one card,
                    // never the screen. The session and the heart-rate read do
                    // not degrade — without them there is nothing to show.
                    val speedSamples = if (workout != null) {
                        runCatching {
                            repository.loadSpeedSamples(workout.startTime, workout.endTime)
                        }.getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                    val cadenceSamples = if (workout != null) {
                        runCatching {
                            repository.loadActivityCadenceSamples(
                                workout.startTime,
                                workout.endTime,
                            )
                        }.getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                    val markers = workout?.let { runCatching { loadMarkers(it) }.getOrNull() }
                        .orEmpty()
                    val coMapsSamples = workout
                        ?.let { runCatching { loadCoMapsSamples(it) }.getOrNull() }
                        .orEmpty()
                    val heartRateRecovery = workout?.let {
                        runCatching { loadHeartRateRecovery(it) }.getOrNull()
                    }
                    val backfilledWorkout = workout?.withSampleBackfilledMetrics(
                        heartRateSamples = heartRateSamples,
                        speedSamples = speedSamples,
                        cadenceSamples = cadenceSamples,
                    )
                    val splitDistanceMeters = currentSplitDistanceMeters()
                    _uiState.value = ActivityDetailUiState(
                        isLoading = false,
                        workout = backfilledWorkout,
                        heartRateSamples = heartRateSamples,
                        speedSamples = speedSamples,
                        cadenceSamples = cadenceSamples,
                        markers = markers,
                        coMapsSamples = coMapsSamples,
                        elevationSamples = backfilledWorkout
                            ?.let { elevationProfile(it.route) }
                            .orEmpty(),
                        heartRateRecovery = heartRateRecovery,
                        error = if (workout == null) ScreenError.NotFound else null,
                    ).withSplitsCutAt(splitDistanceMeters)
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

    /** The recording marks of [workout], looked up by id and then by client id. */
    private suspend fun loadMarkers(workout: ExerciseData): List<ActivityRecordingMarker> =
        markerRepository?.markersForActivity(workout.id).orEmpty()
            .ifEmpty {
                workout.clientRecordId
                    ?.let { markerRepository?.markersForActivity(it) }
                    .orEmpty()
            }

    /** The saved CoMaps guidance of [workout], looked up like the markers are. */
    private fun loadCoMapsSamples(workout: ExerciseData): List<CoMapsNavigationSnapshot> =
        coMapsNavigationRepository?.loadSamples(workout.id).orEmpty()
            .ifEmpty {
                workout.clientRecordId
                    ?.let { coMapsNavigationRepository?.loadSamples(it) }
                    .orEmpty()
            }

    /**
     * The recovery reading for [workout], or null when the session carries no
     * qualifying stop mark — in which case no heart-rate read is issued at all:
     * the session window read already happened, and a second one would be spent
     * on a number that cannot exist.
     */
    private suspend fun loadHeartRateRecovery(workout: ExerciseData): HeartRateRecoveryReading? {
        val heartRepository = heartRepository ?: return null
        val window = heartRateRecoveryWindowFor(workout) ?: return null
        val samples = heartRepository.loadHeartRateSamples(window.readStart, window.readEnd)
        val profile = preferencesRepository?.bodyProfile()
        return calculateHeartRateRecovery(
            recoveryStart = window.recoveryStart,
            samples = samples,
            restingHeartRateBpm = profile?.restingHeartRateBpm,
            ageYears = profile?.ageYears(),
            // The 90-day observed maximum is a trend-screen concern; on the card,
            // the explicit profile maximum plus the Tanaka estimate cover it.
            observedMaxHeartRateBpm = null,
            explicitMaxHeartRateBpm = profile?.maxHeartRateBpm,
        )
    }

    private fun currentSplitDistanceMeters(): Double =
        ActivitySplitDistance.normalize(
            preferencesRepository?.activitySplitDistanceMeters
                ?: ActivitySplitDistance.defaultMeters,
        )

    /**
     * Re-cut the splits when the preference changes while the screen is open —
     * a state update only. The Health Connect data on screen did not change,
     * so nothing is reloaded.
     */
    private fun observeSplitDistance() {
        val preferences = preferencesRepository ?: return
        viewModelScope.launch {
            preferences.activitySplitDistanceMetersFlow.collect { meters ->
                val normalized = ActivitySplitDistance.normalize(meters)
                val state = _uiState.value
                if (state.splitDistanceMeters == normalized && state.workout == null) return@collect
                _uiState.value = state.withSplitsCutAt(normalized)
            }
        }
    }

    /**
     * The state with its splits (and everything derived from them) re-cut at
     * [splitDistanceMeters], from the samples the state already holds.
     */
    private fun ActivityDetailUiState.withSplitsCutAt(
        splitDistanceMeters: Double,
    ): ActivityDetailUiState {
        val workout = workout ?: return copy(
            splitDistanceMeters = splitDistanceMeters,
            splits = ActivitySplits.none(),
            slowestSplitPaceSeconds = null,
            fastestSplitPaceSeconds = null,
            splitSpeedTrace = null,
        )
        val routePoints = workout.route
            .takeIf { it.status == ExerciseRouteStatus.DATA }
            ?.points
            .orEmpty()
        val splits = buildActivitySplits(
            workout = workout,
            routePoints = routePoints,
            speedSamples = speedSamples,
            heartRateSamples = heartRateSamples,
            splitDistanceMeters = splitDistanceMeters,
        )
        return copy(
            splitDistanceMeters = splitDistanceMeters,
            splits = splits,
            slowestSplitPaceSeconds = slowestSplitPaceSeconds(splits),
            fastestSplitPaceSeconds = fastestSplitPaceSeconds(splits),
            splitSpeedTrace = splitSpeedTrace(recordedSpeed = speedSamples, splits = splits),
        )
    }
}
