package tech.mmarca.openvitals.features.settings

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.geo.HgtTileKey
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.CoMapsNavigationRepository
import tech.mmarca.openvitals.data.sync.StepDistanceBackfillService
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.domain.preferences.ActivitySplitDistance
import tech.mmarca.openvitals.domain.preferences.StrideLength
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.activity.elevation.ElevationTile
import tech.mmarca.openvitals.features.activity.elevation.ElevationTileRepository
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportPhase
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportProgress
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportResult
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportWorkController
import tech.mmarca.openvitals.features.activity.maps.OfflineMapPack
import tech.mmarca.openvitals.features.activity.maps.OfflineMapPackFormat
import tech.mmarca.openvitals.features.activity.maps.OfflineMapRepository

@Immutable
data class ActivitiesSettingsUiState(
    val favoriteActivityExerciseType: Int? = null,
    val activitySplitDistanceMeters: Double = ActivitySplitDistance.defaultMeters,
    /** What distances display in: the DISTANCE override, else the base unit. */
    val distanceUnitSystem: UnitSystem = UnitSystem.METRIC,
    val stepDistanceBackfillEnabled: Boolean = false,
    val strideLengthMeters: Double = StrideLength.defaultMeters,
    val activityRecordingPreferences: ActivityRecordingPreferences = ActivityRecordingPreferences(),
    val offlineMapPacks: List<OfflineMapPack> = emptyList(),
    val activeOfflineMapFormat: OfflineMapPackFormat? = null,
    val isImportingOfflineMap: Boolean = false,
    val offlineMapImportProgress: OfflineMapImportProgress? = null,
    val offlineMapImportResult: OfflineMapImportResult? = null,
    val offlineMapImportError: String? = null,
    val elevationCorrectionEnabled: Boolean = true,
    val elevationTiles: List<ElevationTile> = emptyList(),
    val isImportingElevationTile: Boolean = false,
    val elevationTileImportResult: ElevationTile? = null,
    val elevationTileImportError: String? = null,
)

/** The Activities section: recording, the step-distance backfill, offline maps and elevation tiles. */
@HiltViewModel
class ActivitiesSettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val stepDistanceBackfillService: StepDistanceBackfillService,
    private val offlineMapRepository: OfflineMapRepository,
    private val offlineMapImportWorkController: OfflineMapImportWorkController,
    private val elevationTileRepository: ElevationTileRepository,
    private val coMapsNavigationRepository: CoMapsNavigationRepository,
) : ViewModel() {
    companion object {
        private const val TAG = "ActivitiesSettingsViewModel"
    }

    private val _uiState = MutableStateFlow(readPreferences(ActivitiesSettingsUiState()))
    val uiState: StateFlow<ActivitiesSettingsUiState> = _uiState.asStateFlow()

    init {
        observeOfflineMaps()
        observeElevationTiles()
        observeOfflineMapImportWork()
    }

    /** Re-reads the stored settings; the map and tile libraries are observed, not re-read. */
    fun refresh() {
        _uiState.value = readPreferences(_uiState.value)
    }

    private fun readPreferences(state: ActivitiesSettingsUiState): ActivitiesSettingsUiState =
        state.copy(
            favoriteActivityExerciseType = preferencesRepository.favoriteActivityExerciseType,
            activitySplitDistanceMeters = preferencesRepository.activitySplitDistanceMeters,
            distanceUnitSystem = preferencesRepository.unitOverride(UnitQuantity.DISTANCE)
                ?: preferencesRepository.unitSystem,
            stepDistanceBackfillEnabled = preferencesRepository.stepDistanceBackfillEnabled,
            strideLengthMeters = preferencesRepository.strideLengthMeters,
            activityRecordingPreferences = preferencesRepository.activityRecordingPreferences(),
            elevationCorrectionEnabled = preferencesRepository.elevationCorrectionEnabled,
        )

    fun selectFavoriteActivity(exerciseType: Int?) {
        preferencesRepository.favoriteActivityExerciseType = exerciseType
        _uiState.value = _uiState.value.copy(favoriteActivityExerciseType = exerciseType)
    }

    fun setActivitySplitDistance(meters: Double) {
        val normalized = ActivitySplitDistance.normalize(meters)
        preferencesRepository.activitySplitDistanceMeters = normalized
        _uiState.value = _uiState.value.copy(activitySplitDistanceMeters = normalized)
    }

    fun saveStepDistanceBackfill(enabled: Boolean, strideMeters: Double) {
        val normalized = StrideLength.normalize(strideMeters)
        val wasEnabled = preferencesRepository.stepDistanceBackfillEnabled
        preferencesRepository.strideLengthMeters = normalized
        preferencesRepository.stepDistanceBackfillEnabled = enabled
        _uiState.value = _uiState.value.copy(
            stepDistanceBackfillEnabled = enabled,
            strideLengthMeters = normalized,
        )
        viewModelScope.launch {
            if (enabled) {
                stepDistanceBackfillService.syncNow()
            } else if (wasEnabled) {
                stepDistanceBackfillService.purgeDerivedRecords()
            }
        }
    }

    fun updateActivityRecordingPreferences(preferences: ActivityRecordingPreferences) {
        val normalized = preferences.normalized()
        preferencesRepository.setActivityRecordingPreferences(normalized)
        _uiState.value = _uiState.value.copy(activityRecordingPreferences = normalized)
    }

    /** The flavour-specific CoMaps permission to request, null without a CoMaps installed. */
    fun coMapsPermissionName(): String? = coMapsNavigationRepository.permissionName()

    fun onCoMapsPermissionChanged() {
        coMapsNavigationRepository.onPermissionChanged()
    }

    fun importOfflineMap(uri: Uri) {
        if (_uiState.value.isImportingOfflineMap) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImportingOfflineMap = true,
                offlineMapImportProgress = OfflineMapImportProgress(phase = OfflineMapImportPhase.QUEUED),
                offlineMapImportResult = null,
                offlineMapImportError = null,
            )

            runCatching { offlineMapImportWorkController.enqueue(uri) }
                .onFailure { error ->
                    Log.e(TAG, "Offline map import enqueue failed type=${error::class.java.simpleName}")
                    _uiState.value = _uiState.value.copy(
                        isImportingOfflineMap = false,
                        offlineMapImportProgress = null,
                        offlineMapImportResult = null,
                        offlineMapImportError = error.localizedMessage
                            ?: "Offline map import failed.",
                    )
                }
        }
    }

    fun selectOfflineMapFormat(format: OfflineMapPackFormat?) {
        offlineMapRepository.setActiveFormat(format)
        val libraryState = offlineMapRepository.state.value
        _uiState.value = _uiState.value.copy(
            offlineMapPacks = libraryState.mapPacks,
            activeOfflineMapFormat = libraryState.activeFormat,
        )
    }

    fun deleteOfflineMap(id: String) {
        viewModelScope.launch {
            runCatching { offlineMapRepository.deleteMap(id) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        offlineMapImportError = error.localizedMessage
                            ?: "Unable to delete offline map.",
                    )
                }
        }
    }

    fun setElevationCorrectionEnabled(enabled: Boolean) {
        preferencesRepository.elevationCorrectionEnabled = enabled
        _uiState.value = _uiState.value.copy(elevationCorrectionEnabled = enabled)
    }

    fun importElevationTile(uri: Uri) {
        if (_uiState.value.isImportingElevationTile) return
        _uiState.value = _uiState.value.copy(
            isImportingElevationTile = true,
            elevationTileImportResult = null,
            elevationTileImportError = null,
        )
        viewModelScope.launch {
            runCatching { elevationTileRepository.importTile(uri) }
                .onSuccess { tile ->
                    Log.d(TAG, "Elevation tile import completed tile=${tile.displayName}")
                    _uiState.value = _uiState.value.copy(
                        isImportingElevationTile = false,
                        elevationTileImportResult = tile,
                    )
                }
                .onFailure { error ->
                    Log.e(TAG, "Elevation tile import failed type=${error::class.java.simpleName}")
                    _uiState.value = _uiState.value.copy(
                        isImportingElevationTile = false,
                        elevationTileImportError = error.localizedMessage
                            ?: "Elevation tile import failed.",
                    )
                }
        }
    }

    fun deleteElevationTile(key: HgtTileKey) {
        viewModelScope.launch {
            runCatching { elevationTileRepository.deleteTile(key) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        elevationTileImportError = error.localizedMessage
                            ?: "Unable to delete elevation tile.",
                    )
                }
        }
    }

    private fun observeOfflineMaps() {
        viewModelScope.launch {
            offlineMapRepository.state.collect { libraryState ->
                _uiState.value = _uiState.value.copy(
                    offlineMapPacks = libraryState.mapPacks,
                    activeOfflineMapFormat = libraryState.activeFormat,
                )
            }
        }
    }

    private fun observeElevationTiles() {
        viewModelScope.launch {
            elevationTileRepository.state.collect { libraryState ->
                _uiState.value = _uiState.value.copy(elevationTiles = libraryState.tiles)
            }
        }
    }

    private fun observeOfflineMapImportWork() {
        viewModelScope.launch {
            offlineMapImportWorkController.workInfos.collect { workInfos ->
                val workInfo = workInfos.firstOrNull() ?: return@collect
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.BLOCKED,
                    WorkInfo.State.RUNNING,
                    -> {
                        _uiState.value = _uiState.value.copy(
                            isImportingOfflineMap = true,
                            offlineMapImportProgress = offlineMapImportWorkController.progressFor(workInfo)
                                ?: OfflineMapImportProgress(phase = OfflineMapImportPhase.QUEUED),
                            offlineMapImportResult = null,
                            offlineMapImportError = null,
                        )
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val result = offlineMapImportWorkController.resultFor(workInfo)
                        offlineMapRepository.refresh()
                        Log.d(TAG, "Offline map import completed mapId=${result?.mapId.orEmpty()}")
                        _uiState.value = _uiState.value.copy(
                            isImportingOfflineMap = false,
                            offlineMapImportProgress = null,
                            offlineMapImportResult = result,
                            offlineMapImportError = null,
                        )
                    }
                    WorkInfo.State.FAILED -> {
                        val error = offlineMapImportWorkController.errorFor(workInfo)
                            ?: "Offline map import failed."
                        Log.e(TAG, "Offline map import failed")
                        offlineMapRepository.refresh()
                        _uiState.value = _uiState.value.copy(
                            isImportingOfflineMap = false,
                            offlineMapImportProgress = null,
                            offlineMapImportResult = null,
                            offlineMapImportError = error,
                        )
                    }
                    WorkInfo.State.CANCELLED -> {
                        if (_uiState.value.isImportingOfflineMap) {
                            _uiState.value = _uiState.value.copy(
                                isImportingOfflineMap = false,
                                offlineMapImportProgress = null,
                            )
                        }
                    }
                }
            }
        }
    }
}
