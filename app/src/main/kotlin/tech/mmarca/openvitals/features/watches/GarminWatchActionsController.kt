package tech.mmarca.openvitals.features.watches

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.garmin.GarminLocationFile
import tech.mmarca.openvitals.devices.garmin.GarminLog
import tech.mmarca.openvitals.devices.garmin.GarminSendFileResult
import tech.mmarca.openvitals.devices.garmin.GarminSendStage
import tech.mmarca.openvitals.devices.garmin.GarminUploadFileType
import tech.mmarca.openvitals.devices.garmin.GarminWatchSyncService
import tech.mmarca.openvitals.devices.garmin.GarminWaypoint
import tech.mmarca.openvitals.domain.model.BleSensorDevice

/** The find action, Garmin-only: gated on GarminCapability and speaking GFDI directly. */
data class WatchFindUiState(
    /** The watch currently being made to ring, or null. */
    val findingDeviceId: String? = null,
    /** The last find was refused. A flag; the wording is the screen's. */
    val findFailed: Boolean = false,
    val errorMessage: String? = null,
) {
    fun isFindingDevice(deviceId: String): Boolean = findingDeviceId == deviceId
}

/** Sending a point to the watch's saved locations. Garmin-only, like find. */
data class WatchPointSendUiState(
    /** The watch a point is going to, or null. */
    val sendingDeviceId: String? = null,
    val stage: GarminSendStage? = null,
    /** How the last send ended. The wording is the screen's. */
    val result: GarminSendFileResult? = null,
) {
    val isSending: Boolean get() = sendingDeviceId != null
}

/** Sends one point and reports the stages on the way. */
typealias SendWatchPoint = suspend (
    device: BleSensorDevice,
    point: GarminWaypoint,
    onStage: (GarminSendStage) -> Unit,
) -> GarminSendFileResult

/**
 * Drives the find toggle and the point send. Its own scope: both outlive the
 * screen that started them.
 */
@Singleton
class GarminWatchActionsController(
    private val deviceRepository: BleDeviceRepository,
    private val syncController: DeviceSyncController,
    private val findWatch: suspend (address: String, cancelled: CompletableDeferred<Unit>) -> Boolean,
    private val sendPoint: SendWatchPoint,
    private val scope: CoroutineScope,
) {

    @Inject
    constructor(
        deviceRepository: BleDeviceRepository,
        syncController: DeviceSyncController,
        syncService: GarminWatchSyncService,
    ) : this(
        deviceRepository,
        syncController,
        { address, cancelled -> syncService.findWatch(address, cancelled = cancelled) },
        { device, point, onStage ->
            val file = GarminLocationFile.build(point, Instant.now())
            syncService.uploadFile(device, GarminUploadFileType.LOCATION, file, onStage)
        },
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    private val _state = MutableStateFlow(WatchFindUiState())
    val state: StateFlow<WatchFindUiState> = _state.asStateFlow()

    private val _pointState = MutableStateFlow(WatchPointSendUiState())
    val pointState: StateFlow<WatchPointSendUiState> = _pointState.asStateFlow()

    private var findCancel: CompletableDeferred<Unit>? = null

    /**
     * Makes the watch ring, or stops it. A toggle: the alert runs for a
     * minute unless cancelled. Returns the running job, or null.
     */
    fun toggleFind(deviceId: String): Job? {
        if (_state.value.isFindingDevice(deviceId)) {
            // Stop stays enabled until the watch answers, so this can run twice.
            val cancel = findCancel
            if (cancel != null && !cancel.isCompleted) cancel.complete(Unit)
            return null
        }
        // One radio: no find during a sync, a send or another find.
        if (radioInUse) return null

        val device = deviceRepository.devices.firstOrNull { it.id == deviceId } ?: return null
        if (!device.isGarminGfdi) return null

        val cancel = CompletableDeferred<Unit>()
        findCancel = cancel
        _state.value = WatchFindUiState(findingDeviceId = deviceId)
        return scope.launch {
            try {
                val accepted = findWatch(device.address, cancel)
                _state.value = WatchFindUiState(findFailed = !accepted)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                GarminLog.log("[GARMIN-FIND] failed: $error")
                _state.value = WatchFindUiState(errorMessage = describe(error))
            } finally {
                findCancel = null
            }
        }
    }

    /**
     * Sends [point] to the watch's saved locations. Returns the running job,
     * or null when the radio is in use or the watch is not a Garmin.
     */
    fun sendPoint(deviceId: String, point: GarminWaypoint): Job? {
        if (radioInUse) return null
        val device = deviceRepository.devices.firstOrNull { it.id == deviceId } ?: return null
        if (!device.isGarminGfdi) return null

        _pointState.value = WatchPointSendUiState(sendingDeviceId = deviceId)
        return scope.launch {
            var result: GarminSendFileResult = GarminSendFileResult.Unreachable
            try {
                result = sendPoint(device, point) { stage ->
                    _pointState.value = _pointState.value.copy(stage = stage)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                GarminLog.log("[GARMIN-SEND] failed: $error")
            } finally {
                _pointState.value = WatchPointSendUiState(result = result)
            }
        }
    }

    /** Forgets the last result, so a reopened screen starts clean. */
    fun clearPointResult() {
        if (!_pointState.value.isSending) _pointState.value = WatchPointSendUiState()
    }

    private val radioInUse: Boolean
        get() = syncController.state.value.isSyncing ||
            _state.value.findingDeviceId != null ||
            _pointState.value.isSending

    private fun describe(error: Throwable): String {
        val text = error.message ?: error.toString()
        return text.ifBlank { "The watch could not be reached." }
    }
}
