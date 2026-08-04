package tech.mmarca.openvitals.features.watches

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.garmin.GarminCapability
import tech.mmarca.openvitals.devices.garmin.GarminDeviceStateStore
import tech.mmarca.openvitals.devices.garmin.OnboardGarminWatchUseCase
import tech.mmarca.openvitals.devices.wearos.OnboardWearOsWatchUseCase
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.navigation.WATCH_DEVICE_ID_ARG
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator

/**
 * Everything the device view shows about one watch: the registry row, its
 * declared GFDI capabilities, and the shared sync/find state.
 */
@Immutable
data class WatchDeviceUiState(
    /** Null while loading and after removal — the screen shows its no-data state. */
    val device: BleSensorDevice? = null,
    /**
     * What the watch declared in its last handshake. Empty for a watch that
     * has never synced — which the screen must read as "unknown", not "none":
     * see [supports].
     */
    val capabilities: Set<GarminCapability> = emptySet(),
    val sync: DeviceSyncUiState = DeviceSyncUiState(),
    val find: WatchFindUiState = WatchFindUiState(),
    /** The bike computer's broadcast-sensor detection, in flight. */
    val isDetectingSensors: Boolean = false,
    /** The last detection connected and found nothing broadcasting. */
    val detectFoundNothing: Boolean = false,
) {
    /**
     * Whether the watch declared [capability]. Unknown means SHOW, not hide:
     * capabilities arrive in a handshake, so a watch that has never synced
     * would otherwise look feature-less.
     */
    fun supports(capability: GarminCapability): Boolean =
        capabilities.isEmpty() || capability in capabilities
}

/**
 * One watch, and everything about it. Port of the Flutter build's
 * `watch_device_screen.dart` state wiring (`ble_devices_view_model` +
 * `device_sync_view_model` + `garmin_watch_actions_view_model` slices).
 */
@HiltViewModel
class WatchDeviceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: BleDeviceRepository,
    private val stateStore: GarminDeviceStateStore,
    private val syncController: DeviceSyncController,
    private val actionsController: GarminWatchActionsController,
    private val sensorCoordinator: BleSensorCoordinator,
    private val onboardGarminWatch: OnboardGarminWatchUseCase,
    private val onboardWearOsWatch: OnboardWearOsWatchUseCase,
) : ViewModel() {

    val deviceId: String = savedStateHandle.get<String>(WATCH_DEVICE_ID_ARG).orEmpty()

    /**
     * Deliberately outlives the view-model (never cancelled in onCleared):
     * the OS-level unbond after a removal is housekeeping that must not die
     * with the screen that popped right after asking for it.
     */
    private val housekeepingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val localState = MutableStateFlow(WatchDeviceUiState())

    val uiState: StateFlow<WatchDeviceUiState> = combine(
        deviceRepository.devicesFlow,
        syncController.state,
        actionsController.state,
        localState,
    ) { devices, sync, find, local ->
        val device = devices.firstOrNull { it.id == deviceId }
        local.copy(
            device = device,
            capabilities = if (device?.isGarminGfdi == true) {
                stateStore.capabilities(deviceId)
            } else {
                emptySet()
            },
            sync = sync,
            find = find,
        )
    }.stateInViewModel(initial = WatchDeviceUiState())

    fun syncNow() {
        syncController.syncDevice(deviceId)
    }

    fun toggleFind() {
        actionsController.toggleFind(deviceId)
    }

    /** Renames the watch without touching anything else it holds. */
    fun rename(displayName: String) {
        val trimmed = displayName.trim()
        if (trimmed.isEmpty()) return
        deviceRepository.updateDevice(deviceId = deviceId, displayName = trimmed)
    }

    fun setEnabled(enabled: Boolean) {
        deviceRepository.setDeviceEnabled(deviceId, enabled)
    }

    /**
     * Probes a live-sensor-capable device (an Edge bike computer) for the
     * standard GATT services it is broadcasting RIGHT NOW, and persists the
     * result as its capabilities so the recording coordinator will connect to
     * it. Run from the device card, not onboarding: broadcast mode is usually
     * only on during a ride.
     */
    fun detectBroadcastSensors() {
        val device = deviceRepository.devices.firstOrNull { it.id == deviceId } ?: return
        if (!device.isLiveSensorCapable || localState.value.isDetectingSensors) return
        localState.update { it.copy(isDetectingSensors = true, detectFoundNothing = false) }
        viewModelScope.launch {
            val found = sensorCoordinator.discoverCapabilities(device.address)
            // The already-assigned capabilities are the fallback when the
            // connect finds nothing (e.g. broadcast mode turned off
            // mid-detect) — mirroring the Flutter discovery use case.
            val capabilities = found.ifEmpty { device.capabilities }
            if (capabilities != device.capabilities) {
                deviceRepository.updateDevice(deviceId = deviceId, capabilities = capabilities)
            }
            localState.update {
                it.copy(isDetectingSensors = false, detectFoundNothing = found.isEmpty())
            }
        }
    }

    /**
     * Removes the watch: registry row, Garmin per-device state, and — fire
     * and forget — the OS-level bond/association through whichever
     * integration owns it. Mirrors the Flutter `removeDevice` branches.
     */
    fun removeDevice() {
        // Read the device BEFORE forgetting it — the OS-level cleanup needs
        // its address, which the registry is about to stop holding.
        val device = deviceRepository.devices.firstOrNull { it.id == deviceId }
        deviceRepository.removeDevice(deviceId)
        if (device == null) return
        if (device.isGarminGfdi) {
            stateStore.clear(deviceId)
            housekeepingScope.launch {
                runCatching { onboardGarminWatch.forget(device.address) }
            }
        } else if (device.isWearosWatch) {
            housekeepingScope.launch {
                runCatching { onboardWearOsWatch.forget(device.address) }
            }
        }
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateInViewModel(initial: T): StateFlow<T> {
        val state = MutableStateFlow(initial)
        viewModelScope.launch {
            collect { value -> state.value = value }
        }
        return state.asStateFlow()
    }
}
