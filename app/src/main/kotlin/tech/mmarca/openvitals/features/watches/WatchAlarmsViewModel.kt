package tech.mmarca.openvitals.features.watches

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.garmin.GarminAlarm
import tech.mmarca.openvitals.devices.garmin.GarminAlarmsFile
import tech.mmarca.openvitals.devices.garmin.GarminDeviceStateStore
import tech.mmarca.openvitals.devices.garmin.GarminSendFileResult
import tech.mmarca.openvitals.devices.garmin.GarminSendStage
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.navigation.WATCH_DEVICE_ID_ARG

data class WatchAlarmsUiState(
    /** Null while loading, and when the watch is gone or is not a Garmin. */
    val device: BleSensorDevice? = null,
    val alarms: List<GarminAlarm> = emptyList(),
    /** The list differs from the one the watch last accepted. */
    val unsent: Boolean = false,
    val sendingStage: GarminSendStage? = null,
    val isSending: Boolean = false,
    val result: GarminSendFileResult? = null,
    /** A sync, a find or a point send holds the radio. */
    val radioBusy: Boolean = false,
) {
    val canAdd: Boolean get() = alarms.size < GarminAlarmsFile.MaxAlarms && !isSending

    val canSend: Boolean get() = device != null && unsent && !isSending && !radioBusy
}

/**
 * The alarms of a watch with no settings tree. The list lives on the phone:
 * the watch's own alarms cannot be read, and a send replaces them all.
 */
@HiltViewModel
class WatchAlarmsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    deviceRepository: BleDeviceRepository,
    private val stateStore: GarminDeviceStateStore,
    private val actionsController: GarminWatchActionsController,
    syncController: DeviceSyncController,
) : ViewModel() {

    private val deviceId: String = checkNotNull(savedStateHandle[WATCH_DEVICE_ID_ARG])

    private val alarms = MutableStateFlow(stateStore.alarms(deviceId))

    init {
        // A result from an earlier visit says nothing about this list.
        actionsController.clearAlarmsResult()
    }

    private val radioBusy = combine(
        actionsController.state,
        actionsController.pointState,
        syncController.state,
    ) { find, point, sync -> sync.isSyncing || find.findingDeviceId != null || point.isSending }

    val uiState: StateFlow<WatchAlarmsUiState> = combine(
        deviceRepository.devicesFlow,
        alarms,
        actionsController.alarmsState,
        radioBusy,
    ) { devices, alarms, send, radioBusy ->
        WatchAlarmsUiState(
            device = devices.firstOrNull { it.id == deviceId && it.enabled && it.isGarminGfdi },
            alarms = alarms,
            // Read on every change: a finished send has just recorded its list.
            unsent = alarms != stateStore.sentAlarms(deviceId).orEmpty(),
            sendingStage = send.stage,
            isSending = send.isSending,
            result = send.result,
            radioBusy = radioBusy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchAlarmsUiState())

    /** Adds [alarm], or replaces the one at [index]. The list stays in time order. */
    fun save(index: Int?, alarm: GarminAlarm) {
        val current = alarms.value
        val next = if (index == null) current + alarm else current.replaced(index, alarm)
        if (next.size > GarminAlarmsFile.MaxAlarms) return
        update(next.sortedBy { it.minuteOfDay })
    }

    fun delete(index: Int) {
        update(alarms.value.filterIndexed { position, _ -> position != index })
    }

    fun setEnabled(index: Int, enabled: Boolean) {
        val alarm = alarms.value.getOrNull(index) ?: return
        update(alarms.value.replaced(index, alarm.copy(enabled = enabled)))
    }

    fun send() {
        if (!uiState.value.canSend) return
        actionsController.sendAlarms(deviceId, alarms.value)
    }

    private fun update(next: List<GarminAlarm>) {
        if (actionsController.alarmsState.value.isSending) return
        stateStore.setAlarms(deviceId, next)
        alarms.value = next
        actionsController.clearAlarmsResult()
    }
}

private fun List<GarminAlarm>.replaced(index: Int, alarm: GarminAlarm): List<GarminAlarm> =
    mapIndexed { position, existing -> if (position == index) alarm else existing }
