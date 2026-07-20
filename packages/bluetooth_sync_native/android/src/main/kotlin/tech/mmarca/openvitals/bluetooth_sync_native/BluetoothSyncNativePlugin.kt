package tech.mmarca.openvitals.bluetooth_sync_native

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Flutter plugin bridging to Android Bluetooth Classic (RFCOMM) for
 * phone-to-phone Health Connect sync.
 *
 * Moves BYTES ONLY. Discoverability, discovery, one RFCOMM socket (server or
 * client), and byte pumps — nothing about health records or the wire protocol,
 * which lives in pure Dart. Each `@async` host call runs its blocking Bluetooth
 * work on [Dispatchers.IO]; inbound events are pushed up through
 * [BluetoothSyncFlutterApi] on the main thread.
 *
 * The plugin assumes the Dart layer has already obtained the runtime Bluetooth
 * permissions (SCAN / CONNECT / ADVERTISE) via permission_handler before calling
 * — adapter calls are wrapped so a missing grant surfaces as a Pigeon error
 * rather than crashing.
 */
class BluetoothSyncNativePlugin :
    FlutterPlugin,
    ActivityAware,
    BluetoothSyncHostApi {

    private var applicationContext: Context? = null
    private var activity: Activity? = null
    private var flutterApi: BluetoothSyncFlutterApi? = null

    // Blocking Bluetooth work runs on IO; events post back through mainScope.
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var server: RfcommServer? = null
    private var channel: RfcommByteChannel? = null

    private var discoveryReceiver: BluetoothDiscoveryReceiver? = null

    /** Launcher + pending callback for the ACTION_REQUEST_DISCOVERABLE result. */
    private var discoverableLauncher: ActivityResultLauncher<Intent>? = null
    private var pendingDiscoverableCallback: ((Result<Long>) -> Unit)? = null

    private fun adapter(): BluetoothAdapter? {
        val context = applicationContext ?: return null
        val manager = context.getSystemService(BluetoothManager::class.java)
        return manager?.adapter
    }

    // ---------------------------------------------------------------------------
    // FlutterPlugin
    // ---------------------------------------------------------------------------

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        flutterApi = BluetoothSyncFlutterApi(binding.binaryMessenger)
        BluetoothSyncHostApi.setUp(binding.binaryMessenger, this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        BluetoothSyncHostApi.setUp(binding.binaryMessenger, null)
        disconnect()
        stopDiscoveryReceiver()
        flutterApi = null
        applicationContext = null
        ioScope.cancel()
        mainScope.cancel()
    }

    // ---------------------------------------------------------------------------
    // ActivityAware — needed for the discoverable request.
    // ---------------------------------------------------------------------------

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        registerDiscoverableLauncher(binding.activity)
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) =
        onAttachedToActivity(binding)

    override fun onDetachedFromActivityForConfigChanges() = onDetachedFromActivity()

    override fun onDetachedFromActivity() {
        discoverableLauncher?.unregister()
        discoverableLauncher = null
        activity = null
    }

    private fun registerDiscoverableLauncher(activity: Activity) {
        val componentActivity = activity as? ComponentActivity
        if (componentActivity == null) {
            Log.w(SyncBluetooth.TAG, "activity is not a ComponentActivity; discoverable unavailable")
            return
        }
        discoverableLauncher?.unregister()
        discoverableLauncher =
            componentActivity.activityResultRegistry.register(
                "tech.mmarca.openvitals.bluetooth_sync_native.discoverable",
                ActivityResultContracts.StartActivityForResult(),
            ) { result: ActivityResult ->
                val callback = pendingDiscoverableCallback
                pendingDiscoverableCallback = null
                // For ACTION_REQUEST_DISCOVERABLE the result code IS the granted
                // discoverable window in seconds; RESULT_CANCELED (0) means the
                // user declined.
                val seconds =
                    if (result.resultCode == Activity.RESULT_CANCELED) 0L
                    else result.resultCode.toLong()
                callback?.invoke(Result.success(seconds))
            }
    }

    // ---------------------------------------------------------------------------
    // BluetoothSyncHostApi
    // ---------------------------------------------------------------------------

    override fun isBluetoothSupported(): Boolean = adapter() != null

    override fun isBluetoothEnabled(): Boolean = adapter()?.isEnabled == true

    override fun requestDiscoverable(seconds: Long, callback: (Result<Long>) -> Unit) {
        val launcher = discoverableLauncher
        if (launcher == null) {
            callback(Result.failure(IllegalStateException("No activity to request discoverability")))
            return
        }
        if (pendingDiscoverableCallback != null) {
            callback(Result.failure(IllegalStateException("A discoverable request is already in flight")))
            return
        }
        pendingDiscoverableCallback = callback
        val intent =
            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds.toInt())
            }
        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            pendingDiscoverableCallback = null
            callback(Result.failure(e))
        }
    }

    override fun startServer(callback: (Result<Unit>) -> Unit) {
        val adapter = adapter()
        if (adapter == null) {
            callback(Result.failure(IllegalStateException("Bluetooth unavailable")))
            return
        }
        val rfcommServer = RfcommServer(adapter)
        server = rfcommServer
        ioScope.launch {
            try {
                rfcommServer.listen()
            } catch (e: Exception) {
                server = null
                withContext(Dispatchers.Main) { callback(Result.failure(e)) }
                return@launch
            }
            // The socket is now listening — resolve before the blocking accept.
            withContext(Dispatchers.Main) { callback(Result.success(Unit)) }
            val socket =
                try {
                    rfcommServer.accept()
                } catch (_: Exception) {
                    null
                }
            server = null
            if (socket != null) onSocketConnected(socket)
            else emitState(SyncConnectionStateMsg.DISCONNECTED)
        }
    }

    override fun stopServer() {
        server?.cancel()
        server = null
    }

    override fun startDiscovery(callback: (Result<Unit>) -> Unit) {
        val adapter = adapter()
        val context = applicationContext
        if (adapter == null || context == null) {
            callback(Result.failure(IllegalStateException("Bluetooth unavailable")))
            return
        }
        startDiscoveryReceiver(context)
        try {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            val started = adapter.startDiscovery()
            if (started) {
                callback(Result.success(Unit))
            } else {
                stopDiscoveryReceiver()
                callback(Result.failure(IllegalStateException("Failed to start discovery")))
            }
        } catch (e: SecurityException) {
            stopDiscoveryReceiver()
            callback(Result.failure(e))
        }
    }

    override fun cancelDiscovery() {
        try {
            adapter()?.let { if (it.isDiscovering) it.cancelDiscovery() }
        } catch (_: SecurityException) {
            // missing BLUETOOTH_SCAN; nothing to cancel
        }
        stopDiscoveryReceiver()
    }

    override fun connect(address: String, callback: (Result<Unit>) -> Unit) {
        val adapter = adapter()
        if (adapter == null) {
            callback(Result.failure(IllegalStateException("Bluetooth unavailable")))
            return
        }
        // A running discovery starves an RFCOMM connect.
        cancelDiscovery()
        ioScope.launch {
            val socket =
                try {
                    RfcommClient(adapter).connect(address)
                } catch (e: Exception) {
                    emitState(SyncConnectionStateMsg.CONNECT_FAILED)
                    withContext(Dispatchers.Main) { callback(Result.failure(e)) }
                    return@launch
                }
            onSocketConnected(socket)
            withContext(Dispatchers.Main) { callback(Result.success(Unit)) }
        }
    }

    override fun sendBytes(chunk: ByteArray, callback: (Result<Unit>) -> Unit) {
        val active = channel
        if (active == null) {
            callback(Result.failure(IllegalStateException("No open connection")))
            return
        }
        ioScope.launch {
            try {
                active.write(chunk)
                withContext(Dispatchers.Main) { callback(Result.success(Unit)) }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) { callback(Result.failure(e)) }
            }
        }
    }

    override fun disconnect() {
        channel?.close()
        channel = null
        server?.cancel()
        server = null
    }

    // ---------------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------------

    private fun onSocketConnected(socket: BluetoothSocket) {
        val byteChannel =
            try {
                RfcommByteChannel(
                    socket = socket,
                    onBytes = { bytes -> emitBytes(bytes) },
                    onClosed = {
                        channel = null
                        emitState(SyncConnectionStateMsg.DISCONNECTED)
                    },
                )
            } catch (_: IOException) {
                emitState(SyncConnectionStateMsg.CONNECT_FAILED)
                return
            }
        channel = byteChannel
        // Emit 'connected' before starting the reader so Dart never sees bytes
        // before the connection event (main-thread posts stay FIFO).
        emitState(SyncConnectionStateMsg.CONNECTED)
        byteChannel.start()
    }

    private fun startDiscoveryReceiver(context: Context) {
        stopDiscoveryReceiver()
        val receiver =
            BluetoothDiscoveryReceiver(
                onFound = { device ->
                    val name =
                        try {
                            device.name
                        } catch (_: SecurityException) {
                            null
                        }
                    val bonded =
                        try {
                            device.bondState == android.bluetooth.BluetoothDevice.BOND_BONDED
                        } catch (_: SecurityException) {
                            false
                        }
                    emitDevice(SyncDeviceMsg(address = device.address, name = name, bonded = bonded))
                },
                onFinished = {
                    stopDiscoveryReceiver()
                    emitDiscoveryFinished()
                },
            )
        discoveryReceiver = receiver
        val filter =
            IntentFilter().apply {
                addAction(android.bluetooth.BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
        context.registerReceiver(receiver, filter)
    }

    private fun stopDiscoveryReceiver() {
        val receiver = discoveryReceiver ?: return
        discoveryReceiver = null
        try {
            applicationContext?.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // already unregistered
        }
    }

    // --- FlutterApi event emission (always on the main thread) ------------------

    private fun emitDevice(device: SyncDeviceMsg) {
        mainScope.launch { flutterApi?.onDeviceDiscovered(device) {} }
    }

    private fun emitDiscoveryFinished() {
        mainScope.launch { flutterApi?.onDiscoveryFinished {} }
    }

    private fun emitState(state: SyncConnectionStateMsg) {
        mainScope.launch { flutterApi?.onConnectionStateChanged(state) {} }
    }

    private fun emitBytes(bytes: ByteArray) {
        mainScope.launch { flutterApi?.onBytesReceived(bytes) {} }
    }
}
