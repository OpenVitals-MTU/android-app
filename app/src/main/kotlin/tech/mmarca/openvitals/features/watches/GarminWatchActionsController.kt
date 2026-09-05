package tech.mmarca.openvitals.features.watches

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
import tech.mmarca.openvitals.devices.garmin.GarminLog
import tech.mmarca.openvitals.devices.garmin.GarminWatchSyncService

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

/** Drives the find toggle. Its own scope: the alert outlives the screen that started it. */
@Singleton
class GarminWatchActionsController(
    private val deviceRepository: BleDeviceRepository,
    private val syncController: DeviceSyncController,
    private val findWatch: suspend (address: String, cancelled: CompletableDeferred<Unit>) -> Boolean,
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
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    private val _state = MutableStateFlow(WatchFindUiState())
    val state: StateFlow<WatchFindUiState> = _state.asStateFlow()

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
        // One radio: no find during a sync or another find.
        if (syncController.state.value.isSyncing || _state.value.findingDeviceId != null) {
            return null
        }

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

    private fun describe(error: Throwable): String {
        val text = error.message ?: error.toString()
        return text.ifBlank { "The watch could not be reached." }
    }
}
