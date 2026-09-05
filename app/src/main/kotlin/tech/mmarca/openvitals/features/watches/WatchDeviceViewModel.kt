package tech.mmarca.openvitals.features.watches

import androidx.annotation.StringRes
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
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.devices.core.sync.AutoSyncInterval
import tech.mmarca.openvitals.devices.garmin.GarminAgpsImport
import tech.mmarca.openvitals.devices.garmin.GarminAgpsKind
import tech.mmarca.openvitals.devices.garmin.GarminAgpsState
import tech.mmarca.openvitals.devices.garmin.GarminAgpsStore
import tech.mmarca.openvitals.devices.garmin.GarminCapability
import tech.mmarca.openvitals.devices.garmin.GarminDeviceStateStore
import tech.mmarca.openvitals.devices.garmin.OnboardGarminWatchUseCase
import tech.mmarca.openvitals.devices.wearos.OnboardWearOsWatchUseCase
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.navigation.WATCH_DEVICE_ID_ARG
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator

/** Everything the device view shows about one watch. */
@Immutable
data class WatchDeviceUiState(
    /** Null while loading and after removal — the screen shows its no-data state. */
    val device: BleSensorDevice? = null,
    /** What the watch declared in its last handshake. Empty means unknown, not none; see [supports]. */
    val capabilities: Set<GarminCapability> = emptySet(),
    val sync: DeviceSyncUiState = DeviceSyncUiState(),
    val find: WatchFindUiState = WatchFindUiState(),
    /** The bike computer's broadcast-sensor detection, in flight. */
    val isDetectingSensors: Boolean = false,
    /** Companion mode: hold the link whenever the watch is in range. */
    val stayConnected: Boolean = false,
    /** How often the watch syncs on its own, or OFF for by-hand only. */
    val autoSync: AutoSyncInterval = AutoSyncInterval.OFF,
    /** Live readings streamed over that link. */
    val liveReadings: Boolean = false,
    /** The watch may read the phone's calendar. Off by default. */
    val calendarSync: Boolean = false,
    /** Calendar sync is on but the OS permission has been revoked. */
    val calendarPermissionMissing: Boolean = false,
    /** Live CoMaps guidance is shown on the watch, recording or not. */
    val navigationOnWatch: Boolean = false,
    /** Guidance on the watch is on but CoMaps' own permission has been declined. */
    val coMapsPermissionMissing: Boolean = false,
    /** The most recent live heart rate, while one is arriving. */
    val liveHeartRateBpm: Int? = null,
    /** The last detection connected and found nothing broadcasting. */
    val detectFoundNothing: Boolean = false,
    /** GPS ephemeris the user supplied, and what the watch has asked for. */
    val agps: GarminAgpsState = GarminAgpsState(),
    /** The result of the last ephemeris import, until the next one. */
    @StringRes val agpsMessage: Int? = null,
) {
    /** Whether the watch declared [capability]. Unknown means show: a never-synced watch has no list. */
    fun supports(capability: GarminCapability): Boolean =
        capabilities.isEmpty() || capability in capabilities
}

/** One watch, and everything about it. */
@HiltViewModel
class WatchDeviceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: BleDeviceRepository,
    private val stateStore: GarminDeviceStateStore,
    private val syncController: DeviceSyncController,
    private val autoSyncScheduler: WatchAutoSyncScheduler,
    private val actionsController: GarminWatchActionsController,
    private val sensorCoordinator: BleSensorCoordinator,
    private val onboardGarminWatch: OnboardGarminWatchUseCase,
    private val onboardWearOsWatch: OnboardWearOsWatchUseCase,
    private val notificationBridge: tech.mmarca.openvitals.devices.garmin.GarminNotificationBridge,
    private val realtimeStore: tech.mmarca.openvitals.devices.garmin.GarminRealtimeStore,
    private val agpsStore: GarminAgpsStore,
    private val calendarSource: tech.mmarca.openvitals.devices.garmin.GarminCalendarSource,
    private val navigationRelay: tech.mmarca.openvitals.devices.garmin.GarminNavigationRelay,
    private val coMapsNavigationRepository:
        tech.mmarca.openvitals.data.repository.contract.CoMapsNavigationRepository,
    private val coMapsGuidanceFeed: tech.mmarca.openvitals.comaps.CoMapsGuidanceFeed,
) : ViewModel() {

    val deviceId: String = savedStateHandle.get<String>(WATCH_DEVICE_ID_ARG).orEmpty()

    /** Outlives the view-model: the unbond after a removal must not die with the screen. */
    private val housekeepingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val localState = MutableStateFlow(WatchDeviceUiState())

    val uiState: StateFlow<WatchDeviceUiState> = combine(
        deviceRepository.devicesFlow,
        syncController.state,
        actionsController.state,
        localState,
        realtimeStore.readings,
    ) { devices, sync, find, local, live ->
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
            stayConnected = stateStore.stayConnected(deviceId),
            autoSync = autoSyncScheduler.interval(deviceId),
            liveReadings = stateStore.liveReadings(deviceId),
            calendarSync = stateStore.calendarSync(deviceId),
            calendarPermissionMissing = stateStore.calendarSync(deviceId) &&
                !calendarSource.hasPermission(),
            navigationOnWatch = stateStore.navigationOnWatch(deviceId),
            coMapsPermissionMissing = stateStore.navigationOnWatch(deviceId) &&
                !coMapsNavigationRepository.hasPermission(),
            liveHeartRateBpm = live.freshHeartRate(),
        )
    }
        .combine(agpsStore.agps) { state, agps -> state.copy(agps = agps) }
        .stateInViewModel(initial = WatchDeviceUiState())

    /** Takes in an ephemeris file. Its contents say what it is; the message says why not. */
    fun importAgps(uri: android.net.Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { agpsStore.import(uri) }
            localState.update {
                it.copy(
                    agpsMessage = when (result) {
                        is GarminAgpsImport.Imported -> R.string.settings_watch_agps_imported
                        GarminAgpsImport.NotEphemeris -> R.string.settings_watch_agps_not_ephemeris
                        GarminAgpsImport.Stale -> R.string.settings_watch_agps_stale
                        GarminAgpsImport.Unreadable -> R.string.settings_watch_agps_unreadable
                    },
                )
            }
        }
    }

    fun forgetAgps(kind: GarminAgpsKind) {
        agpsStore.forget(kind)
        localState.update { it.copy(agpsMessage = null) }
    }

    /** Companion mode. The bridge owns the pref, presence observation and the held link together. */
    fun setLiveReadings(enabled: Boolean) {
        notificationBridge.onLiveReadingsChanged(deviceId, enabled)
        localState.update { it.copy(liveReadings = enabled) }
    }

    /** Turns the calendar glance on or off. A toggle set with the permission denied still sticks. */
    fun setCalendarSync(enabled: Boolean) {
        stateStore.setCalendarSync(deviceId, enabled)
        localState.update { it.copy(calendarSync = enabled) }
    }

    /** Shows live CoMaps guidance on the watch. Works whether or not a session is running. */
    fun setNavigationOnWatch(enabled: Boolean) {
        navigationRelay.onEnabledChanged(deviceId, enabled)
        localState.update {
            it.copy(
                navigationOnWatch = enabled,
                coMapsPermissionMissing = enabled && !coMapsNavigationRepository.hasPermission(),
            )
        }
    }

    /** The flavour-specific CoMaps permission to request, null without a CoMaps installed. */
    fun coMapsPermissionName(): String? = coMapsNavigationRepository.permissionName()

    /** Re-checks CoMaps' grant after its dialog and re-opens the feed: a refused observer stays refused. */
    fun refreshCoMapsPermission() {
        coMapsGuidanceFeed.refresh()
        localState.update {
            it.copy(
                coMapsPermissionMissing = stateStore.navigationOnWatch(deviceId) &&
                    !coMapsNavigationRepository.hasPermission(),
            )
        }
    }

    /** Re-checks the OS grant after the permission dialog closes. */
    fun refreshCalendarPermission() {
        localState.update { it.copy(calendarSync = stateStore.calendarSync(deviceId)) }
    }

    fun setStayConnected(enabled: Boolean) {
        notificationBridge.onStayConnectedChanged(deviceId, enabled)
        // Poke the combine so the switch reflects the change immediately.
        localState.update { it.copy(stayConnected = enabled) }
    }

    /** Picks the auto-sync interval. The scheduler owns the stored choice and the periodic work. */
    fun setAutoSync(interval: AutoSyncInterval) {
        autoSyncScheduler.setInterval(deviceId, interval)
        localState.update { it.copy(autoSync = interval) }
    }

    fun syncNow() {
        syncController.syncDevice(deviceId, listenAfter = DeviceSyncController.MANUAL_SYNC_LINGER)
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
     * Probes a bike computer for the GATT services it is broadcasting now and
     * persists them as capabilities. From the device card: broadcast mode is
     * usually only on during a ride.
     */
    fun detectBroadcastSensors() {
        val device = deviceRepository.devices.firstOrNull { it.id == deviceId } ?: return
        if (!device.isLiveSensorCapable || localState.value.isDetectingSensors) return
        localState.update { it.copy(isDetectingSensors = true, detectFoundNothing = false) }
        viewModelScope.launch {
            val found = sensorCoordinator.discoverCapabilities(device.address)
            // The assigned capabilities are the fallback when the connect finds nothing.
            val capabilities = found.ifEmpty { device.capabilities }
            if (capabilities != device.capabilities) {
                deviceRepository.updateDevice(deviceId = deviceId, capabilities = capabilities)
            }
            localState.update {
                it.copy(isDetectingSensors = false, detectFoundNothing = found.isEmpty())
            }
        }
    }

    /** Removes the watch: registry row, Garmin state, and the OS-level bond, fire and forget. */
    fun removeDevice() {
        // Read the device before forgetting it: the OS cleanup needs its address.
        val device = deviceRepository.devices.firstOrNull { it.id == deviceId }
        deviceRepository.removeDevice(deviceId)
        // Before the early return: a schedule left running would wake the radio for nothing.
        autoSyncScheduler.forget(deviceId)
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
