package tech.mmarca.openvitals.features.devicesync

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.mmarca.openvitals.features.homewidgets.refreshPlacedHomeWidgets
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.SyncedRecordOriginRepository
import tech.mmarca.openvitals.features.devicesync.bluetooth.BluetoothSyncManager
import tech.mmarca.openvitals.features.devicesync.bluetooth.DiscoveredSyncDevice
import tech.mmarca.openvitals.features.devicesync.bluetooth.SyncConnectionState
import tech.mmarca.openvitals.features.devicesync.protocol.PAIRING_CODE_DIGITS
import tech.mmarca.openvitals.features.devicesync.protocol.SyncReport
import tech.mmarca.openvitals.features.devicesync.protocol.SyncRole
import tech.mmarca.openvitals.features.devicesync.protocol.SyncSession
import tech.mmarca.openvitals.features.devicesync.protocol.SyncSessionConfig
import tech.mmarca.openvitals.features.devicesync.protocol.buildSyncReportText
import tech.mmarca.openvitals.features.devicesync.protocol.generatePairingCode
import tech.mmarca.openvitals.features.devicesync.store.DeviceSyncReportStore
import tech.mmarca.openvitals.features.devicesync.store.HealthConnectSyncStore
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingController
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.Instant

/**
 * View-model for the "Sync with another phone" wizard.
 *
 * Orchestrates the Bluetooth manager (discoverability / discovery / connect)
 * and the pure-Kotlin [SyncSession] over the live RFCOMM transport, reading
 * and writing Health Connect through [HealthConnectSyncStore]. One state flow
 * drives the whole state-machine flow the screen renders.
 *
 * Permission choreography lives in the screen (ActivityResult launchers must):
 * Bluetooth runtime grants → Health Connect grants → (host) the discoverable
 * dialog. The VM entry points [startHosting] / [startScanning] assume those
 * already ran.
 */
@HiltViewModel
class DeviceSyncViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bluetooth: BluetoothSyncManager,
    private val healthConnectManager: HealthConnectManager,
    private val importRepository: AppleHealthImportRepository,
    private val originRepository: SyncedRecordOriginRepository,
    private val reportStore: DeviceSyncReportStore,
    private val recordingController: ActivityRecordingController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceSyncState())
    val uiState: StateFlow<DeviceSyncState> = _uiState.asStateFlow()

    private var discoveryJob: Job? = null
    private var connectionJob: Job? = null
    private var syncJob: Job? = null

    // Bumped on reset()/cancel() so a session tearing down in the background
    // can't write its result over a freshly-reset wizard.
    private var generation = 0

    private var foregroundStartedByUs = false

    init {
        refreshHealthPermissions()
    }

    // ── Permission plumbing (driven by the screen's launchers) ───────────────

    /**
     * The Health Connect permissions the wizard asks for: READ + WRITE for
     * every syncable type this device's provider + manifest can grant — the
     * guest must be able to WRITE received records (an unpermitted write throws
     * and, since a batch insert is atomic, drops the whole batch).
     */
    fun healthPermissionsToRequest(): Set<String> = buildSet {
        for ((type, suffix) in syncableTypePermissionSuffix) {
            when (type) {
                "MindfulnessSessionRecord" ->
                    if (!healthConnectManager.isMindfulnessSessionAvailable()) continue
                "PlannedExerciseSessionRecord" ->
                    if (!healthConnectManager.isPlannedExerciseAvailable()) continue
                "SkinTemperatureRecord" -> {
                    // WRITE_SKIN_TEMPERATURE is not declared in the manifest, so
                    // only the read half is requestable — and without the write
                    // half the type is not syncable on this device anyway.
                    if (healthConnectManager.isSkinTemperatureAvailable()) {
                        add(healthReadPermission(suffix))
                    }
                    continue
                }
            }
            add(healthReadPermission(suffix))
            add(healthWritePermission(suffix))
        }
    }

    /** Re-reads granted Health Connect permissions; call after a request returns. */
    fun refreshHealthPermissions() {
        viewModelScope.launch {
            val granted = runCatching { healthConnectManager.grantedPermissions() }
                .getOrNull() ?: return@launch
            // A type is syncable on THIS device only if it holds both a read
            // (to send) and a write (to receive) grant.
            val available = syncableTypePermissionSuffix
                .filterValues { suffix ->
                    healthReadPermission(suffix) in granted &&
                        healthWritePermission(suffix) in granted
                }
                .keys
            _uiState.update { state ->
                state.copy(
                    availableTypes = available,
                    selectedTypes =
                    if (state.selectedTypes.isEmpty()) available
                    else state.selectedTypes intersect available,
                )
            }
        }
    }

    fun onBluetoothPermissionsDenied() {
        _uiState.update { it.copy(error = DeviceSyncError.PERMISSION_DENIED) }
    }

    /** The system discoverable dialog for the screen's launcher (host role). */
    fun discoverableIntent(): Intent =
        bluetooth.requestDiscoverableIntent(DISCOVERABLE_SECONDS)

    // ── Step 1: role ─────────────────────────────────────────────────────────

    /**
     * Host role, called with the granted discoverable window (0 = declined).
     * Generates the pairing code and opens the RFCOMM server.
     */
    fun startHosting(grantedSeconds: Int) {
        if (!bluetooth.isBluetoothEnabled()) {
            _uiState.update { it.copy(bluetoothUnavailable = true) }
            return
        }
        if (grantedSeconds <= 0) {
            _uiState.update { it.copy(error = DeviceSyncError.DISCOVERABLE_DECLINED) }
            return
        }
        val code = generatePairingCode()
        observeConnection()
        viewModelScope.launch {
            try {
                bluetooth.startListening()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "host: listen failed: ${e.message}")
                _uiState.update { it.copy(error = DeviceSyncError.SYNC_FAILED) }
                return@launch
            }
            Log.i(TAG, "host: discoverable ${grantedSeconds}s, server listening")
            _uiState.update {
                it.copy(
                    role = SyncRole.HOST,
                    code = code,
                    step = DeviceSyncStep.HOST_WAITING,
                    error = null,
                    bluetoothUnavailable = false,
                )
            }
        }
    }

    /** Guest role: starts scanning for nearby phones. */
    fun startScanning() {
        if (!bluetooth.isBluetoothEnabled()) {
            _uiState.update { it.copy(bluetoothUnavailable = true) }
            return
        }
        observeConnection()
        _uiState.update {
            it.copy(
                role = SyncRole.GUEST,
                step = DeviceSyncStep.GUEST_SCANNING,
                // Bonded phones seed the list — the fallback when an OEM's
                // discoverable window is flaky and the scan misses the peer.
                devices = bluetooth.bondedCandidates(),
                scanning = true,
                error = null,
                bluetoothUnavailable = false,
            )
        }
        launchDiscovery()
    }

    /** Restarts discovery after a scan window closed (the Rescan affordance). */
    fun rescan() {
        if (_uiState.value.step != DeviceSyncStep.GUEST_SCANNING) return
        _uiState.update { it.copy(devices = bluetooth.bondedCandidates(), scanning = true) }
        launchDiscovery()
    }

    private fun launchDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            try {
                bluetooth.startDiscovery().collect { device ->
                    _uiState.update { state ->
                        // Dedup by address; newest name wins.
                        state.copy(
                            devices = state.devices.filter { it.address != device.address } + device,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "discovery failed: ${e.message}")
            }
            // The scan window closed itself (~12 s); without consuming this the
            // UI would spin forever with no "no devices found" affordance.
            if (_uiState.value.step == DeviceSyncStep.GUEST_SCANNING) {
                _uiState.update { it.copy(scanning = false) }
            }
        }
    }

    // ── Step 2 (guest): select + code ────────────────────────────────────────

    fun selectDevice(device: DiscoveredSyncDevice) {
        discoveryJob?.cancel()
        _uiState.update {
            it.copy(
                selectedDevice = device,
                step = DeviceSyncStep.GUEST_CODE,
                codeEntry = "",
                codeError = false,
                scanning = false,
            )
        }
    }

    fun enterDigit(digit: Char) {
        _uiState.update { state ->
            if (state.codeEntry.length >= PAIRING_CODE_DIGITS) state
            else state.copy(codeEntry = state.codeEntry + digit, codeError = false)
        }
    }

    fun deleteDigit() {
        _uiState.update { state ->
            if (state.codeEntry.isEmpty()) state
            else state.copy(codeEntry = state.codeEntry.dropLast(1))
        }
    }

    fun submitCode() {
        val state = _uiState.value
        val device = state.selectedDevice ?: return
        if (state.codeEntry.length != PAIRING_CODE_DIGITS) return
        viewModelScope.launch {
            try {
                Log.i(TAG, "guest: connecting to ${device.address}")
                bluetooth.connect(device.address)
                Log.i(TAG, "guest: connected, advancing to range")
                _uiState.update {
                    it.copy(code = it.codeEntry, step = DeviceSyncStep.RANGE, error = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "guest: connect failed: ${e.message}")
                _uiState.update { it.copy(error = DeviceSyncError.CONNECT_FAILED) }
            }
        }
    }

    // ── Steps 3-4: range + types ─────────────────────────────────────────────

    fun setRange(range: SyncRange) = _uiState.update { it.copy(range = range) }

    fun toggleType(recordType: String) {
        _uiState.update { state ->
            val next = state.selectedTypes.toMutableSet()
            if (!next.remove(recordType)) next += recordType
            state.copy(selectedTypes = next)
        }
    }

    fun goToTypes() = _uiState.update { it.copy(step = DeviceSyncStep.TYPES) }

    // ── Step 5: sync ─────────────────────────────────────────────────────────

    fun startSync() {
        val state = _uiState.value
        val role = state.role ?: return
        if (syncJob?.isActive == true) return
        // A live activity recording holds the app's foreground slot AND the
        // radio discipline — refuse to sync until it is finished or discarded.
        if (recordingController.state.value.isActive) {
            _uiState.update { it.copy(error = DeviceSyncError.RECORDING_ACTIVE) }
            return
        }
        val gen = generation
        Log.i(TAG, "startSync role=$role types=${state.selectedTypes.size} range=${state.range}")
        _uiState.update { it.copy(step = DeviceSyncStep.SYNCING, error = null, progress = null) }

        syncJob = viewModelScope.launch {
            // Wait until the RFCOMM socket is actually connected before the
            // handshake (the host may still be waiting for the guest to dial).
            val connected = withTimeoutOrNull(CONNECT_WAIT_MILLIS) {
                bluetooth.connectionState.first { it == SyncConnectionState.CONNECTED }
            }
            if (gen != generation) return@launch
            if (connected == null) {
                _uiState.update {
                    it.copy(step = DeviceSyncStep.REPORT, error = DeviceSyncError.CONNECT_TIMEOUT)
                }
                return@launch
            }

            val window = state.range.window()
            val store = HealthConnectSyncStore(
                healthConnectManager = healthConnectManager,
                importRepository = importRepository,
                originRepository = originRepository,
                localPackageName = context.packageName,
                windowStart = window.first,
                windowEnd = window.second,
            )
            val session = SyncSession(
                transport = bluetooth.transport(),
                store = store,
                config = SyncSessionConfig(
                    role = role,
                    code = state.code,
                    deviceName = deviceName(),
                    supportedTypes = state.availableTypes.toList(),
                    selectedTypes = state.selectedTypes.toList(),
                    // Real datasets can be large (a CGM alone is ~100k
                    // readings/year). Bigger batches cut the number of
                    // stop-and-wait round-trips, and a generous ack timeout
                    // tolerates the slow side writing a big batch to Health
                    // Connect.
                    batchSize = 500,
                    batchTimeoutMillis = 180_000,
                ),
            )
            val progressJob = launch {
                session.progress.collect { progress ->
                    _uiState.update { it.copy(progress = progress) }
                }
            }
            // Keep the process foregrounded for the duration of the transfer so
            // the OS does not kill the app if the user switches away
            // (best-effort; the transfer itself stays in-process).
            foregroundStartedByUs = DeviceSyncForegroundService.start(context)
            try {
                val report = session.run()
                Log.i(
                    TAG,
                    "session done: completed=${report.completed} sent=${report.itemsSent} " +
                        "received=${report.itemsReceived} imported=${report.imported} " +
                        "abort=${report.abortReason}",
                )
                if (gen != generation) return@launch
                if (!report.completed && report.abortReason?.contains("code") == true) {
                    // Wrong code — back to code entry with an error.
                    _uiState.update {
                        it.copy(step = DeviceSyncStep.GUEST_CODE, codeError = true, codeEntry = "")
                    }
                    return@launch
                }
                persistReport(report)
                // The tiles read stored data, so records that just arrived from
                // the other phone must not leave the home screen on its
                // pre-sync numbers until the system's next periodic tick.
                runCatching { refreshPlacedHomeWidgets(context) }
                if (gen != generation) return@launch
                _uiState.update { it.copy(step = DeviceSyncStep.REPORT, report = report) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "session threw: ${e.message}")
                if (gen != generation) return@launch
                // Move OFF the syncing step so the UI leaves the progress
                // spinner and can render the failure.
                _uiState.update {
                    it.copy(step = DeviceSyncStep.REPORT, error = DeviceSyncError.SYNC_FAILED)
                }
            } finally {
                progressJob.cancel()
                stopForegroundIfOurs()
            }
        }
    }

    private suspend fun persistReport(report: SyncReport) {
        val text = buildSyncReportText(report, generatedAt = Instant.now())
        _uiState.update { it.copy(reportText = text) }
        reportStore.writeReport(text)
    }

    // ── Reset / teardown ─────────────────────────────────────────────────────

    /**
     * Cancels an in-flight or pending sync and returns to the start. Tearing
     * the Bluetooth manager down closes the transport, which ends any running
     * SyncSession (its inbound closes → abort), and the bumped generation
     * stops that session's result from landing on the reset wizard.
     */
    fun cancel() = reset()

    fun reset() {
        generation += 1
        teardown()
        _uiState.value = DeviceSyncState()
        refreshHealthPermissions()
    }

    override fun onCleared() {
        generation += 1
        teardown()
        super.onCleared()
    }

    private fun teardown() {
        discoveryJob?.cancel()
        discoveryJob = null
        connectionJob?.cancel()
        connectionJob = null
        syncJob?.cancel()
        syncJob = null
        bluetooth.reset()
        stopForegroundIfOurs()
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private fun observeConnection() {
        if (connectionJob?.isActive == true) return
        connectionJob = viewModelScope.launch {
            bluetooth.connectionState.collect { connection ->
                Log.i(TAG, "connectionState=$connection step=${_uiState.value.step}")
                if (connection == SyncConnectionState.CONNECTED) {
                    // The host sits on a static "waiting" screen until a peer
                    // connects. Advance it into the range/type picker so it
                    // runs its own half of the (bidirectional) session —
                    // without this the host never starts a session and the
                    // guest's handshake finds no peer.
                    val state = _uiState.value
                    if (state.role == SyncRole.HOST && state.step == DeviceSyncStep.HOST_WAITING) {
                        _uiState.update { it.copy(step = DeviceSyncStep.RANGE) }
                    }
                }
            }
        }
    }

    private fun stopForegroundIfOurs() {
        // Only stop a service WE started; DeviceSyncForegroundService.stop only
        // ever addresses its own class, so an unrelated foreground service
        // (e.g. a GPS recording) can never be torn down from here.
        if (!foregroundStartedByUs) return
        foregroundStartedByUs = false
        DeviceSyncForegroundService.stop(context)
    }

    private fun deviceName(): String =
        listOfNotNull(Build.MANUFACTURER, Build.MODEL)
            .joinToString(" ")
            .ifBlank { "OpenVitals phone" }

    private companion object {
        const val TAG = "DeviceSync"
        const val DISCOVERABLE_SECONDS = 120
        const val CONNECT_WAIT_MILLIS = 30_000L
    }
}
