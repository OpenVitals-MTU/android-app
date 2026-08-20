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

    /**
     * Takes in an ephemeris file the user downloaded. What it is comes from
     * its contents, so there is nothing to ask them about — either it is
     * usable or the message says why it is not.
     */
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

    /**
     * Companion mode. The bridge owns the behaviour — the pref, presence
     * observation and the held link all change together there.
     */
    fun setLiveReadings(enabled: Boolean) {
        notificationBridge.onLiveReadingsChanged(deviceId, enabled)
        localState.update { it.copy(liveReadings = enabled) }
    }

    /**
     * Turns the calendar glance on or off. The screen asks for READ_CALENDAR
     * first when it needs to; a toggle set with the permission denied still
     * sticks, and the state shows the missing grant instead of lying.
     */
    fun setCalendarSync(enabled: Boolean) {
        stateStore.setCalendarSync(deviceId, enabled)
        localState.update { it.copy(calendarSync = enabled) }
    }

    /**
     * Shows or stops showing live CoMaps guidance on the watch. Nothing here
     * asks about activity recording: this switch is the whole of the feature,
     * and it works whether or not a session is running.
     */
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

    /**
     * Re-checks CoMaps' grant after its dialog closes, and re-opens the feed
     * with it: the observer registered before the grant was refused, and stays
     * refused.
     */
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
        // The combine re-reads the pref on its next emission; poke it so the
        // switch reflects the change immediately rather than on the next
        // device event.
        localState.update { it.copy(stayConnected = enabled) }
    }

    /**
     * Picks how often the watch syncs on its own. The scheduler owns both
     * halves — the stored choice and the periodic work — so this cannot leave
     * one saying something the other does not.
     */
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
        // Before the early return: a schedule left running against a watch the
        // registry no longer knows would wake the radio for nothing.
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
