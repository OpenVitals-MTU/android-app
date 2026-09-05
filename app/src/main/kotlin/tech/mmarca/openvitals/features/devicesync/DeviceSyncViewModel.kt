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
 * View-model for the "Sync with another phone" wizard. Drives the Bluetooth
 * manager and the [SyncSession] over RFCOMM. The screen handles permissions;
 * [startHosting] and [startScanning] assume they were granted.
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

    // Bumped on reset so a session tearing down cannot write over a fresh wizard.
    private var generation = 0

    private var foregroundStartedByUs = false

    init {
        refreshHealthPermissions()
        loadStoredReport()
    }

    // Permission plumbing, driven by the screen's launchers.

    /**
     * The Health Connect permissions the wizard asks for: read and write for
     * every syncable type. The guest must be able to write received records.
     */
    fun healthPermissionsToRequest(): Set<String> = buildSet {
        for ((type, suffix) in syncableTypePermissionSuffix) {
            when (type) {
                "MindfulnessSessionRecord" ->
                    if (!healthConnectManager.isMindfulnessSessionAvailable()) continue
                "PlannedExerciseSessionRecord" ->
                    if (!healthConnectManager.isPlannedExerciseAvailable()) continue
                "SkinTemperatureRecord" -> {
                    // WRITE_SKIN_TEMPERATURE is not in the manifest, so only the read half is requestable.
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
            // Syncable here only with both a read and a write grant.
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

    // Step 1: role.

    /** Host role, with the granted discoverable window (0 = declined). Opens the RFCOMM server. */
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
                // Bonded phones seed the list, for OEMs whose discoverable window is flaky.
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
            // The scan window closed itself; consume it or the UI spins forever.
            if (_uiState.value.step == DeviceSyncStep.GUEST_SCANNING) {
                _uiState.update { it.copy(scanning = false) }
            }
        }
    }

    // Step 2 (guest): select and code.

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

    // Steps 3-4: range and types.

    fun setRange(range: SyncRange) = _uiState.update { it.copy(range = range) }

    fun toggleType(recordType: String) {
        _uiState.update { state ->
            val next = state.selectedTypes.toMutableSet()
            if (!next.remove(recordType)) next += recordType
            state.copy(selectedTypes = next)
        }
    }

    fun goToTypes() = _uiState.update { it.copy(step = DeviceSyncStep.TYPES) }

    // Step 5: sync.

    fun startSync() {
        val state = _uiState.value
        val role = state.role ?: return
        if (syncJob?.isActive == true) return
        // A live recording holds the foreground slot and the radio. Refuse.
        if (recordingController.state.value.isActive) {
            _uiState.update { it.copy(error = DeviceSyncError.RECORDING_ACTIVE) }
            return
        }
        val gen = generation
        Log.i(TAG, "startSync role=$role types=${state.selectedTypes.size} range=${state.range}")
        _uiState.update { it.copy(step = DeviceSyncStep.SYNCING, error = null, progress = null) }

        syncJob = viewModelScope.launch {
            // Wait for the socket to connect before the handshake.
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
                    // Bigger batches cut stop-and-wait round trips; the store also caps
                    // by bytes. The timeout rides out a Health Connect rate-limit pause.
                    batchSize = 500,
                    batchTimeoutMillis = 300_000,
                ),
            )
            val progressJob = launch {
                session.progress.collect { progress ->
                    _uiState.update { it.copy(progress = progress) }
                }
            }
            // Keep the process foregrounded so the OS does not kill it. Best-effort.
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
                // Records just arrived; the widgets must not stay on pre-sync numbers.
                runCatching { refreshPlacedHomeWidgets(context) }
                if (gen != generation) return@launch
                _uiState.update { it.copy(step = DeviceSyncStep.REPORT, report = report) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "session threw: ${e.message}")
                if (gen != generation) return@launch
                // Even an ugly death gets a report with the last counters and the exception.
                val partial = partialReport(e)
                persistReport(partial)
                // Leave the syncing step so the UI can render the failure.
                _uiState.update {
                    it.copy(
                        step = DeviceSyncStep.REPORT,
                        report = partial,
                        error = DeviceSyncError.SYNC_FAILED,
                    )
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

    /** A best-effort report for a session that died with an exception. Per-type tallies are lost. */
    private fun partialReport(cause: Exception): SyncReport {
        val progress = _uiState.value.progress
        return SyncReport(
            completed = false,
            peerDeviceName = "unknown",
            negotiatedTypes = emptyList(),
            itemsSent = progress?.itemsSent ?: 0,
            itemsReceived = progress?.itemsReceived ?: 0,
            imported = progress?.itemsWritten ?: 0,
            duplicateSkipped = 0,
            typeSummaries = emptyList(),
            abortReason = "unexpected error: ${cause.message ?: cause.javaClass.simpleName}",
        )
    }

    private fun loadStoredReport() {
        viewModelScope.launch {
            val stored = reportStore.readReport()
            if (stored.isNotEmpty()) {
                _uiState.update { it.copy(lastReportText = stored) }
            }
        }
    }

    // Reset and teardown.

    /**
     * Cancels the sync and returns to the start. Tearing the manager down
     * ends any session; the bumped generation keeps its result off the wizard.
     */
    fun cancel() = reset()

    fun reset() {
        generation += 1
        teardown()
        _uiState.value = DeviceSyncState()
        refreshHealthPermissions()
        loadStoredReport()
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

    // Internals.

    private fun observeConnection() {
        if (connectionJob?.isActive == true) return
        connectionJob = viewModelScope.launch {
            bluetooth.connectionState.collect { connection ->
                Log.i(TAG, "connectionState=$connection step=${_uiState.value.step}")
                if (connection == SyncConnectionState.CONNECTED) {
                    // Advance the host from its waiting screen into the picker, so it
                    // runs its half of the session.
                    val state = _uiState.value
                    if (state.role == SyncRole.HOST && state.step == DeviceSyncStep.HOST_WAITING) {
                        _uiState.update { it.copy(step = DeviceSyncStep.RANGE) }
                    }
                }
            }
        }
    }

    private fun stopForegroundIfOurs() {
        // Only stop a service we started. A GPS recording can never be torn down from here.
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
