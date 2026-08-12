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

/**
 * The Garmin-only watch action that deliberately does NOT go through the sync
 * seam: making the watch ring (find). It is gated on GarminCapability and
 * speaks GFDI directly, so it stays Garmin-typed rather than pretending to be
 * device-agnostic. Port of the Flutter build's
 * `garmin_watch_actions_view_model.dart`.
 */
data class WatchFindUiState(
    /** The watch currently being made to ring, or null. */
    val findingDeviceId: String? = null,
    /**
     * The last find was refused by the watch. A flag, not a message: the
     * wording is the screen's job, and this layer has no localizations.
     */
    val findFailed: Boolean = false,
    val errorMessage: String? = null,
) {
    fun isFindingDevice(deviceId: String): Boolean = findingDeviceId == deviceId
}

/**
 * Drives the find/ring toggle. A singleton with its own scope for the same
 * reason as [DeviceSyncController]: the alert runs for up to a minute, and the
 * screen that started it may be gone before it ends — the cancel discipline in
 * [GarminWatchSyncService.findWatch] must keep running regardless.
 */
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
     * Makes the watch ring, and stops it.
     *
     * A toggle rather than a fire-and-forget: the protocol alerts for a
     * minute unless cancelled, so the same control has to be able to stop it
     * — and the link stays open for the duration, which is why this cannot
     * share the sync path that closes it a second in.
     *
     * Returns the running job, or null for a stop/no-op.
     */
    fun toggleFind(deviceId: String): Job? {
        if (_state.value.isFindingDevice(deviceId)) {
            // Stop stays enabled until the watch answers the cancel — a full
            // round trip — so this branch is reachable twice, and completing
            // a completed deferred must stay harmless.
            val cancel = findCancel
            if (cancel != null && !cancel.isCompleted) cancel.complete(Unit)
            return null
        }
        // One radio: a find cannot start while a sync is running, nor while
        // another find is in flight.
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
