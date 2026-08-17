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

/**
 * Where a device sync has got to, for the row that started it. Port of the
 * Flutter build's `DeviceSyncState` (`device_sync_view_model.dart`, the watch
 * one in `features/settings/application`).
 *
 * Device-agnostic: any integration's sync drives this same state through the
 * [DeviceSyncPort] seam.
 */
data class DeviceSyncUiState(
    /**
     * The device id being synced, or null when idle. Scoped rather than a
     * bare flag because the screen can list several watches and only one row
     * should show a spinner.
     */
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
 * Runs a device sync through whichever integration owns the device, and feeds
 * the row's state off the port's progress and outcome. Port of the Flutter
 * build's `DeviceSyncViewModel`.
 *
 * A singleton with its OWN scope rather than a screen view-model: the sync
 * outlives the screen that started it (Flutter's provider was app-lifetime),
 * so backing out of the device view must not kill a download mid-file — and
 * every watch surface reads the same state, so two screens cannot disagree
 * about whether the radio is busy.
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
         * How long a MANUAL sync lingers after the files are pulled.
         *
         * The watch runs its own on-connection errands — fetching weather
         * through the HTTP proxy, notably — a few seconds after the link
         * comes up, on a rate limit of its own. A sync that disconnects the
         * moment the files land hangs up before those errands run, which is
         * why the weather glance stayed empty with everything else working.
         * Manual syncs only: background ones stay short, this costs radio.
         */
        val MANUAL_SYNC_LINGER: Duration = 20.seconds
    }

    /**
     * Syncs [deviceId], one sync at a time — the radio is a single resource,
     * and two sessions against one watch would fight over its handles.
     *
     * A device no [DeviceSyncPort.canSync] claims (a live sensor, an unknown
     * id) is a no-op. Returns the running sync, or null when nothing started.
     * [listenAfter] is the diagnostic listen window held open after the sync.
     *
     * [silent] is for a sync nobody asked for (the automatic schedule): the
     * run drives the same progress state, so the screens still show the radio
     * as busy and a second sync cannot start, but a failure ends idle instead
     * of throwing a red banner onto a screen the user never touched. A watch
     * out of range at 3am is not an error to report; the last-sync time says
     * everything there is to say.
     *
     * The result comes back through a [Deferred] rather than a bare [Job] so a
     * background caller can act on the outcome. Nothing has to await it —
     * [DeviceSyncPort.sync] never throws, so an ignored one cannot swallow a
     * failure that mattered.
     */
    fun syncDevice(
        deviceId: String,
        listenAfter: Duration = Duration.ZERO,
        silent: Boolean = false,
    ): Deferred<DeviceSyncResult>? {
        if (_state.value.isSyncing) return null
        val device = deviceRepository.devices.firstOrNull { it.id == deviceId } ?: return null
        if (!syncPort.canSync(device)) return null

        // Set BEFORE the launch, so the instant the tap returns the radio
        // already reads as busy to anything else that asks.
        _state.value = DeviceSyncUiState(
            syncingDeviceId = deviceId,
            phase = DeviceSyncPhase.HANDSHAKE,
        )
        return scope.async {
            // The port owns the pull → import → store → stamp sequence; this
            // controller only drives the row's state off its progress/outcome.
            //
            // The catch is defensive, not expected: the seam's contract is
            // that a sync never throws. It matters because `async` holds an
            // uncaught exception until somebody awaits it, so a port that
            // broke the contract would otherwise leave the radio reading busy
            // for the rest of the process's life and silently refuse every
            // later sync.
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
