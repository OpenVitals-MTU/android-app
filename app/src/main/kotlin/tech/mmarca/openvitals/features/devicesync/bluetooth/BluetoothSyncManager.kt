package tech.mmarca.openvitals.features.devicesync.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.features.devicesync.protocol.SyncByteTransport

/** A device seen during discovery (or a bonded candidate), for the pairing UI. */
data class DiscoveredSyncDevice(
    val address: String,
    val name: String?,
    val bonded: Boolean,
)

/** High-level RFCOMM connection lifecycle for the UI. */
enum class SyncConnectionState { IDLE, CONNECTED, DISCONNECTED, CONNECT_FAILED }

/**
 * The Bluetooth side of phone-to-phone sync: discoverability, discovery, one
 * RFCOMM socket (server or client), and byte pumps — nothing about health
 * records or the wire protocol, which lives in `protocol/`.
 *
 * Kotlin absorption of the Flutter plugin (`BluetoothSyncNativePlugin` +
 * `bluetooth_sync_service.dart`): with no Pigeon bridge in the way, the plugin
 * layer collapses into this one class exposing suspend functions and Flows.
 * Callers must hold the runtime Bluetooth permissions (SCAN / CONNECT /
 * ADVERTISE) before scanning or connecting — adapter calls surface a missing
 * grant as an exception rather than crashing.
 *
 * Inbound bytes go into an UNLIMITED [Channel] created when the socket opens,
 * so bytes that arrive after the socket connects but before the SyncSession
 * attaches its reader are BUFFERED and replayed, never dropped — the native
 * reader starts pumping the moment the socket connects, seconds before the
 * session runs.
 */
@Singleton
class BluetoothSyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var server: RfcommServer? = null

    @Volatile private var channel: RfcommByteChannel? = null

    @Volatile private var inbound: Channel<ByteArray> = Channel(Channel.UNLIMITED)

    private val _connectionState = MutableStateFlow(SyncConnectionState.IDLE)
    val connectionState: StateFlow<SyncConnectionState> = _connectionState.asStateFlow()

    private fun adapter(): BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    fun isBluetoothSupported(): Boolean = adapter() != null

    fun isBluetoothEnabled(): Boolean = adapter()?.isEnabled == true

    /**
     * The system dialog intent asking to make this phone discoverable for
     * [seconds]. Launch through an ActivityResult launcher; the result code IS
     * the granted discoverable window in seconds (RESULT_CANCELED = declined).
     */
    fun requestDiscoverableIntent(seconds: Int): Intent =
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds)

    /**
     * Opens the RFCOMM server socket and returns once it is listening; a
     * background job then blocks on accept for ONE inbound connection, which
     * lands on [connectionState]. Throws if Bluetooth is off or SCAN/CONNECT
     * is missing.
     */
    suspend fun startListening() {
        val adapter = adapter() ?: throw IOException("Bluetooth unavailable")
        stopListening()
        val rfcommServer = RfcommServer(adapter)
        server = rfcommServer
        withContext(Dispatchers.IO) { rfcommServer.listen() }
        Log.i(SyncBluetooth.TAG, "server: listening, waiting to accept")
        scope.launch {
            val socket = try {
                rfcommServer.accept()
            } catch (e: Exception) {
                Log.w(SyncBluetooth.TAG, "server: accept failed: ${e.message}")
                null
            }
            if (server === rfcommServer) server = null
            if (socket != null) {
                Log.i(SyncBluetooth.TAG, "server: accepted a connection")
                onSocketConnected(socket)
            } else {
                emitState(SyncConnectionState.DISCONNECTED)
            }
        }
    }

    /** Cancels a pending accept. Idempotent. */
    fun stopListening() {
        server?.cancel()
        server = null
    }

    /**
     * Runs one discovery scan, emitting devices as they are found; the flow
     * COMPLETES when the scan window closes (~12 s) — the UI's cue to offer a
     * rescan. Cancelling the collection cancels the scan.
     */
    fun startDiscovery(): Flow<DiscoveredSyncDevice> = callbackFlow {
        val adapter = adapter()
        if (adapter == null) {
            close(IOException("Bluetooth unavailable"))
            return@callbackFlow
        }
        val receiver = BluetoothDiscoveryReceiver(
            onFound = { device -> trySend(device.toDiscovered()) },
            onFinished = { close() },
        )
        context.registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            },
        )
        try {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            if (!adapter.startDiscovery()) {
                close(IOException("Failed to start discovery"))
            }
        } catch (e: SecurityException) {
            close(e)
        }
        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
            try {
                if (adapter.isDiscovering) adapter.cancelDiscovery()
            } catch (_: SecurityException) {
                // missing BLUETOOTH_SCAN; nothing to cancel
            }
        }
    }

    /**
     * Already-bonded devices as scan-list seeds — the fallback for OEMs whose
     * discoverable UX is unreliable: a previously-paired peer connects without
     * a fresh scan finding it.
     */
    fun bondedCandidates(): List<DiscoveredSyncDevice> = try {
        adapter()?.bondedDevices.orEmpty().map { it.toDiscovered() }
    } catch (_: SecurityException) {
        emptyList()
    }

    /**
     * Dials [address] (guest role) and returns once the socket is open —
     * triggering the OS pairing dialog on a first-time peer. Throws on failure.
     */
    suspend fun connect(address: String) {
        val adapter = adapter() ?: throw IOException("Bluetooth unavailable")
        // Guard against a re-entrant connect while one socket is already open —
        // a second RFCOMM connect to the same UUID fails with "already opened".
        if (channel != null) {
            Log.w(SyncBluetooth.TAG, "connect: ignoring, a connection is already open")
            return
        }
        Log.i(SyncBluetooth.TAG, "connect: dialing $address")
        val socket = try {
            withContext(Dispatchers.IO) { RfcommClient(adapter).connect(address) }
        } catch (e: Exception) {
            Log.w(SyncBluetooth.TAG, "connect: failed: ${e.message}")
            emitState(SyncConnectionState.CONNECT_FAILED)
            throw e
        }
        Log.i(SyncBluetooth.TAG, "connect: socket open")
        onSocketConnected(socket)
    }

    /**
     * The [SyncByteTransport] over the live connection. Valid while
     * [connectionState] is [SyncConnectionState.CONNECTED]; its inbound channel
     * has been buffering since the socket opened.
     */
    fun transport(): SyncByteTransport = TransportImpl()

    /** Closes the socket and any pending server. Idempotent. */
    fun disconnect() {
        channel?.close()
        channel = null
        stopListening()
        inbound.close()
    }

    /**
     * Full teardown between wizard runs: closes everything and re-arms a fresh
     * inbound buffer + IDLE state so the next session starts clean.
     */
    fun reset() {
        disconnect()
        inbound = Channel(Channel.UNLIMITED)
        _connectionState.value = SyncConnectionState.IDLE
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private fun onSocketConnected(socket: BluetoothSocket) {
        // Fresh buffer per connection, armed BEFORE the reader starts so no
        // byte can slip past it.
        val connectionInbound = Channel<ByteArray>(Channel.UNLIMITED)
        inbound = connectionInbound
        val byteChannel = try {
            RfcommByteChannel(
                socket = socket,
                onBytes = { bytes -> connectionInbound.trySend(bytes) },
                onClosed = {
                    Log.i(SyncBluetooth.TAG, "socket closed by peer/link")
                    channel = null
                    // A dropped link MUST end the inbound byte stream — a
                    // SyncSession parked in its receive loop learns of a dead
                    // link only from its inbound closing.
                    connectionInbound.close()
                    emitState(SyncConnectionState.DISCONNECTED)
                },
            )
        } catch (e: IOException) {
            Log.w(SyncBluetooth.TAG, "onSocketConnected: stream open failed: ${e.message}")
            emitState(SyncConnectionState.CONNECT_FAILED)
            return
        }
        channel = byteChannel
        // Publish 'connected' before starting the reader so no observer can
        // see bytes before the connection state.
        emitState(SyncConnectionState.CONNECTED)
        byteChannel.start()
    }

    private fun emitState(state: SyncConnectionState) {
        Log.i(SyncBluetooth.TAG, "connectionState -> $state")
        _connectionState.value = state
    }

    private fun BluetoothDevice.toDiscovered(): DiscoveredSyncDevice {
        val deviceName = try {
            name
        } catch (_: SecurityException) {
            null
        }
        val isBonded = try {
            bondState == BluetoothDevice.BOND_BONDED
        } catch (_: SecurityException) {
            false
        }
        return DiscoveredSyncDevice(address = address, name = deviceName, bonded = isBonded)
    }

    /**
     * Binds the transport to the manager's CURRENT connection. Outbound writes
     * run on IO under a mutex so the session's concurrent sender/receiver/ack
     * writes stay ordered on the one socket.
     */
    private inner class TransportImpl : SyncByteTransport {
        private val writeMutex = Mutex()

        override val inbound: ReceiveChannel<ByteArray> get() = this@BluetoothSyncManager.inbound

        override suspend fun send(bytes: ByteArray) {
            val active = channel ?: throw IOException("No open connection")
            writeMutex.withLock {
                withContext(Dispatchers.IO) { active.write(bytes) }
            }
        }

        override fun close() {
            this@BluetoothSyncManager.disconnect()
        }
    }
}
