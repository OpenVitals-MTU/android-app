package tech.mmarca.openvitals.features.settings

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.sync.BodyEnergyChainSyncService
import tech.mmarca.openvitals.data.sync.DerivedMetricsResetService
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.domain.model.HeartRateThresholds
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem

@Immutable
data class BodySettingsUiState(
    /** With the latest Health Connect weight and height folded in. */
    val bodyProfile: BodyProfile = BodyProfile(),
    val bodyProfileWeightMeasured: Boolean = false,
    val bodyProfileHeightMeasured: Boolean = false,
    val canWriteBodyMeasurements: Boolean = false,
    /** What body weight displays in: the WEIGHT override, else the base unit. */
    val weightUnitSystem: UnitSystem = UnitSystem.METRIC,
    val caffeinePreferences: CaffeinePreferences = CaffeinePreferences(),
    val nightStartHour: Int = SleepWindow.Default.startHour,
    val nightEndHour: Int = SleepWindow.Default.endHour,
    val highHeartRateThresholdBpm: Int = PreferencesRepository.DEFAULT_HIGH_HEART_RATE_THRESHOLD_BPM,
    val lowHeartRateThresholdBpm: Int = PreferencesRepository.DEFAULT_LOW_HEART_RATE_THRESHOLD_BPM,
    val bodyEnergyCalibration: BodyEnergyCalibration = BodyEnergyCalibration.Automatic,
    val isResettingDerivedMetrics: Boolean = false,
)

/**
 * Body profile and Recovery: the settings the derived chain reads.
 *
 * The profile, the zones and the night window all feed Body Energy, so they share one
 * store and one rebuild trigger.
 */
@HiltViewModel
class BodySettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val bodyRepository: BodyRepository,
    private val bodyEnergyChainSyncService: BodyEnergyChainSyncService,
    private val derivedMetricsResetService: DerivedMetricsResetService,
) : ViewModel() {
    companion object {
        private const val TAG = "BodySettingsViewModel"
    }

    private val _uiState = MutableStateFlow(readPreferences(BodySettingsUiState()))
    val uiState: StateFlow<BodySettingsUiState> = _uiState.asStateFlow()

    init {
        resolveBodyProfileFromHealthConnect()
    }

    /** Permissions are granted in Health Connect, so a return to the screen re-reads. */
    fun refresh() {
        _uiState.value = readPreferences(_uiState.value)
        resolveBodyProfileFromHealthConnect()
    }

    private fun readPreferences(state: BodySettingsUiState): BodySettingsUiState =
        state.copy(
            bodyProfile = preferencesRepository.bodyProfile(),
            weightUnitSystem = preferencesRepository.unitOverride(UnitQuantity.WEIGHT)
                ?: preferencesRepository.unitSystem,
            caffeinePreferences = preferencesRepository.caffeinePreferences(),
            nightStartHour = preferencesRepository.nightStartHour,
            nightEndHour = preferencesRepository.nightEndHour,
            highHeartRateThresholdBpm = preferencesRepository.highHeartRateThresholdBpm,
            lowHeartRateThresholdBpm = preferencesRepository.lowHeartRateThresholdBpm,
            bodyEnergyCalibration = preferencesRepository.bodyEnergyCalibration(),
        )

    /** Folds the latest Health Connect weight and height into the card state. */
    private fun resolveBodyProfileFromHealthConnect() {
        viewModelScope.launch {
            val declared = preferencesRepository.bodyProfile()
            val resolved = runCatching { bodyRepository.resolveBodyProfile(declared) }
                .getOrElse { error ->
                    Log.w(TAG, "resolveBodyProfile failed", error)
                    return@launch
                }
            val canWrite = runCatching {
                bodyRepository.hasBodyWritePermission(BodyMeasurementType.WEIGHT)
            }.getOrDefault(false)
            _uiState.value = _uiState.value.copy(
                bodyProfile = resolved,
                bodyProfileWeightMeasured = resolved.weightKg != null && resolved.weightKg != declared.weightKg,
                bodyProfileHeightMeasured = resolved.heightCm != null && resolved.heightCm != declared.heightCm,
                canWriteBodyMeasurements = canWrite,
            )
        }
    }

    fun updateBodyProfile(profile: BodyProfile) {
        val previous = _uiState.value.bodyProfile
        val declared = preferencesRepository.bodyProfile()
        preferencesRepository.setBodyProfile(profile)
        val saved = preferencesRepository.bodyProfile()
        _uiState.value = _uiState.value.copy(bodyProfile = saved)
        if (saved.signature() != declared.signature()) rebuildBodyEnergyChain()
        if (!_uiState.value.canWriteBodyMeasurements) return
        // A changed weight or height is written to Health Connect as a real
        // measurement. Only on a real change, or every save adds a duplicate.
        viewModelScope.launch {
            val now = Instant.now()
            suspend fun write(type: BodyMeasurementType, value: Double?) {
                if (value == null) return
                runCatching {
                    bodyRepository.writeBodyMeasurementEntry(
                        BodyMeasurementWriteRequest(type = type, time = now, value = value),
                    )
                }.onFailure { error ->
                    Log.w(TAG, "Body measurement write failed type=$type", error)
                }
            }
            if (saved.weightKg != previous.weightKg) {
                write(BodyMeasurementType.WEIGHT, saved.weightKg)
            }
            if (saved.heightCm != previous.heightCm) {
                write(BodyMeasurementType.HEIGHT, saved.heightCm)
            }
        }
    }

    fun updateCaffeinePreferences(preferences: CaffeinePreferences) {
        preferencesRepository.setCaffeinePreferences(preferences)
        _uiState.value = _uiState.value.copy(caffeinePreferences = preferencesRepository.caffeinePreferences())
    }

    fun setNightStartHour(value: Int) {
        val hour = Math.floorMod(value, 24)
        preferencesRepository.nightStartHour = hour
        _uiState.value = _uiState.value.copy(nightStartHour = hour)
    }

    fun setNightEndHour(value: Int) {
        val hour = Math.floorMod(value, 24)
        preferencesRepository.nightEndHour = hour
        _uiState.value = _uiState.value.copy(nightEndHour = hour)
    }

    fun setHighHeartRateThresholdBpm(value: Int) {
        val current = _uiState.value
        val normalized = value
            .coerceAtLeast(current.lowHeartRateThresholdBpm + HeartRateThresholds.MINIMUM_GAP_BPM)
        preferencesRepository.highHeartRateThresholdBpm = normalized
        _uiState.value = current.copy(
            highHeartRateThresholdBpm = preferencesRepository.highHeartRateThresholdBpm,
        )
    }

    fun setLowHeartRateThresholdBpm(value: Int) {
        val current = _uiState.value
        val normalized = value
            .coerceAtMost(current.highHeartRateThresholdBpm - HeartRateThresholds.MINIMUM_GAP_BPM)
        preferencesRepository.lowHeartRateThresholdBpm = normalized
        _uiState.value = current.copy(
            lowHeartRateThresholdBpm = preferencesRepository.lowHeartRateThresholdBpm,
        )
    }

    /** Commits the zone ladder. [birthYear] is normally null: the Body profile card owns it. */
    fun updateBodyEnergyCalibration(calibration: BodyEnergyCalibration, birthYear: Int? = null) {
        if (birthYear != null) {
            updateBodyProfile(preferencesRepository.bodyProfile().copy(birthYear = birthYear))
        }
        val zonesChanged =
            preferencesRepository.bodyEnergyCalibration().zoneSignature() != calibration.zoneSignature()
        preferencesRepository.setBodyEnergyCalibration(calibration.copy(setupCompleted = true))
        _uiState.value = _uiState.value.copy(bodyEnergyCalibration = preferencesRepository.bodyEnergyCalibration())
        if (zonesChanged) rebuildBodyEnergyChain()
    }

    /** Zones and the profile are chain inputs: every stored Body Energy day is wrong now. */
    private fun rebuildBodyEnergyChain() {
        viewModelScope.launch {
            runCatching { bodyEnergyChainSyncService.syncAll(force = true) }
                .onFailure { error -> Log.w(TAG, "Body Energy chain rebuild failed", error) }
        }
    }

    /** Returns the learned gains to neutral and forgets the watch readings. Zone settings stay. */
    fun resetBodyEnergyPersonalTuning() {
        val current = preferencesRepository.bodyEnergyCalibration()
        updateBodyEnergyCalibration(
            current.copy(
                sleepChargeGain = 1.0,
                activityDrainGain = 1.0,
                basalDrainGain = 1.0,
                stressDrainGain = 1.0,
                watchObservationCount = 0,
            )
        )
    }

    /**
     * Wipes every derived metric kept outside Health Connect and kicks their
     * rebuild. [onComplete] fires once the wipe has landed.
     */
    fun resetDerivedMetrics(onComplete: (Boolean) -> Unit) {
        if (_uiState.value.isResettingDerivedMetrics) return
        _uiState.value = _uiState.value.copy(isResettingDerivedMetrics = true)
        viewModelScope.launch {
            val succeeded = try {
                derivedMetricsResetService.reset()
                true
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                Log.w(TAG, "Derived metrics reset failed", t)
                false
            }
            _uiState.value = _uiState.value.copy(
                isResettingDerivedMetrics = false,
                bodyEnergyCalibration = preferencesRepository.bodyEnergyCalibration(),
            )
            onComplete(succeeded)
        }
    }
}
