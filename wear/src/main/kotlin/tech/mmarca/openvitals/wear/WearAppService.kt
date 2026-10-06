package tech.mmarca.openvitals.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Listens for the phone's RFCOMM ping so OpenVitals on the phone can tell the
 * app is alive. A `connectedDevice` foreground service: a plain background
 * service is stopped about a minute after the activity closes.
 *
 * The phone side is `app/.../devices/wearos/BluetoothWearOsNodePort`.
 */
class WearAppService : Service() {

    @Volatile
    private var serverSocket: BluetoothServerSocket? = null
    private val isListening = AtomicBoolean(false)

    /** Bumped per listener start, so a thread that is shutting down cannot stop its successor. */
    private val listenerGeneration = AtomicInteger(0)

    /** False when the system refused the foreground start: nothing may listen then. */
    private var isForeground = false

    /** Bluetooth off closes the server socket; on again reopens it. */
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> startRfcommListener()
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> stopRfcommListener()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isForeground = startInForeground()
        if (!isForeground) {
            stopSelf()
            return
        }
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bluetoothStateReceiver, filter)
        }
        startRfcommListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // onCreate already stopped a service the system refused; do not restart it.
        if (!isForeground) return START_NOT_STICKY
        startRfcommListener()
        return START_STICKY
    }

    /** False when the system refuses, e.g. BLUETOOTH_CONNECT not granted on API 34+. */
    private fun startInForeground(): Boolean {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.link_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            ),
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.link_notification_text))
            .setOngoing(true)
            .build()
        return try {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
            true
        } catch (e: Exception) {
            // SecurityException for the missing grant, or a start from the background.
            Log.e(TAG, "Cannot run the phone link in the foreground", e)
            false
        }
    }

    private fun startRfcommListener() {
        if (isListening.getAndSet(true)) return
        val generation = listenerGeneration.incrementAndGet()

        Thread {
            var socket: BluetoothServerSocket? = null
            try {
                val adapter = getSystemService(BluetoothManager::class.java)?.adapter
                if (adapter == null || !adapter.isEnabled) {
                    // The state receiver starts it again once Bluetooth is on.
                    Log.w(TAG, "Bluetooth unavailable or disabled")
                    return@Thread
                }
                val server = try {
                    adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, OPENVITALS_WEAR_APP_UUID)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Missing Bluetooth permission for RFCOMM server", e)
                    return@Thread
                }
                socket = server
                serverSocket = server
                Log.i(TAG, "RFCOMM server listening on $OPENVITALS_WEAR_APP_UUID")

                while (isListening.get() && listenerGeneration.get() == generation) {
                    val client: BluetoothSocket = try {
                        server.accept()
                    } catch (e: IOException) {
                        if (isListening.get()) Log.e(TAG, "Socket accept failed", e)
                        break
                    }
                    handleClientConnection(client)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error starting RFCOMM listener", e)
            } finally {
                // Only this thread's socket: a restart may already hold a new one.
                runCatching { socket?.close() }
                if (serverSocket === socket) serverSocket = null
                // A newer listener owns the flag now; leave it alone.
                if (listenerGeneration.get() == generation) isListening.set(false)
            }
        }.apply {
            name = "WearAppService-rfcomm"
            start()
        }
    }

    private fun stopRfcommListener() {
        isListening.set(false)
        // Unblocks accept(); the listener thread then exits.
        runCatching { serverSocket?.close() }
    }

    private fun handleClientConnection(socket: BluetoothSocket) {
        Thread {
            try {
                val request = socket.inputStream.bufferedReader(Charsets.UTF_8).readLine()
                if (request?.trim() == PING) {
                    socket.outputStream.write("$PONG\n".toByteArray(Charsets.UTF_8))
                    socket.outputStream.flush()
                    Log.i(TAG, "Responded PONG to ping request")
                }
            } catch (e: IOException) {
                Log.w(TAG, "Error handling client connection: ${e.message}")
            } finally {
                runCatching { socket.close() }
            }
        }.start()
    }

    override fun onDestroy() {
        if (isForeground) unregisterReceiver(bluetoothStateReceiver)
        stopRfcommListener()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WearAppService"
        private const val SERVICE_NAME = "OpenVitalsWearApp"
        private const val CHANNEL_ID = "phone_link"
        private const val NOTIFICATION_ID = 1

        /** Must match `BluetoothWearOsNodePort` on the phone; `WearOsLinkParityTest` holds them together. */
        val OPENVITALS_WEAR_APP_UUID: UUID = UUID.fromString("4838d728-6e5a-4b95-a29d-a60032338301")
        private const val PING = "PING"
        private const val PONG = "PONG"

        /** The link needs BLUETOOTH_CONNECT from API 31; before that it is install-time. */
        fun hasBluetoothPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

        /** Starts the listener once the grant is there; a no-op without it. */
        fun startIfPermitted(context: Context) {
            if (!hasBluetoothPermission(context)) return
            runCatching { context.startForegroundService(Intent(context, WearAppService::class.java)) }
                .onFailure { Log.e(TAG, "Cannot start the phone link", it) }
        }
    }
}
