package tech.mmarca.openvitals.features.watches

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPhase
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPort
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncResult

/** Where a device sync has got to, for the row that started it. Device-agnostic. */
data class DeviceSyncUiState(
    /** The device id being synced, or null. Scoped so only one row shows a spinner. */
    val syncingDeviceId: String? = null,
    val phase: DeviceSyncPhase? = null,
    val filesTotal: Int = 0,
    val filesDone: Int = 0,
    /** Files downloaded and handed to the importer by the last completed run. */
    val lastFileCount: Int? = null,
    val errorMessage: String? = null,
) {
    val isSyncing: Boolean get() = syncingDeviceId != null

    fun isSyncingDevice(deviceId: String): Boolean = syncingDeviceId == deviceId
}

/**
 * Runs a device sync through the integration that owns the device. A
 * singleton with its own scope: the sync outlives the screen, and every
 * watch surface reads the same state.
 */
@Singleton
class DeviceSyncController(
    private val deviceRepository: BleDeviceRepository,
    private val syncPort: DeviceSyncPort,
    private val scope: CoroutineScope,
) {

    @Inject
    constructor(
        deviceRepository: BleDeviceRepository,
        syncPort: DeviceSyncPort,
    ) : this(
        deviceRepository,
        syncPort,
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    private val _state = MutableStateFlow(DeviceSyncUiState())
    val state: StateFlow<DeviceSyncUiState> = _state.asStateFlow()

    companion object {
        /**
         * How long a manual sync lingers after the pull. The watch runs its
         * own errands (weather) seconds after the link comes up.
         */
        val MANUAL_SYNC_LINGER: Duration = 20.seconds
    }

    /**
     * Syncs [deviceId], one at a time. A device no port claims is a no-op.
     * [listenAfter] is the diagnostic window held open after the sync.
     * [silent] is for the automatic schedule: a failure ends idle, no banner.
     * Returns a [Deferred] so a background caller can act on the outcome.
     */
    fun syncDevice(
        deviceId: String,
        listenAfter: Duration = Duration.ZERO,
        silent: Boolean = false,
    ): Deferred<DeviceSyncResult>? {
        if (_state.value.isSyncing) return null
        val device = deviceRepository.devices.firstOrNull { it.id == deviceId } ?: return null
        if (!syncPort.canSync(device)) return null

        // Set before the launch, so the radio reads busy at once.
        _state.value = DeviceSyncUiState(
            syncingDeviceId = deviceId,
            phase = DeviceSyncPhase.HANDSHAKE,
        )
        return scope.async {
            // The port owns the sequence. The catch is defensive: `async` holds an
            // uncaught exception, which would leave the radio reading busy forever.
            val result = try {
                syncPort.sync(
                    device,
                    listenAfter = listenAfter,
                    onProgress = { progress ->
                        _state.update { current ->
                            if (current.syncingDeviceId != deviceId) {
                                current
                            } else {
                                current.copy(
                                    phase = progress.phase,
                                    filesTotal = progress.filesTotal,
                                    filesDone = progress.filesDone,
                                )
                            }
                        }
                    },
                )
            } catch (error: CancellationException) {
                _state.value = DeviceSyncUiState()
                throw error
            } catch (error: Exception) {
                DeviceSyncResult.Failed(error.message ?: error.toString())
            }
            _state.value = when (result) {
                is DeviceSyncResult.Succeeded -> DeviceSyncUiState(
                    phase = DeviceSyncPhase.COMPLETE,
                    lastFileCount = result.fileCount,
                )

                is DeviceSyncResult.Failed -> if (silent) {
                    DeviceSyncUiState()
                } else {
                    DeviceSyncUiState(errorMessage = result.message)
                }
            }
            result
        }
    }

    /** Clears the finished/failed banner so the row goes back to normal. */
    fun clear() {
        if (_state.value.isSyncing) return
        _state.value = DeviceSyncUiState()
    }
}
