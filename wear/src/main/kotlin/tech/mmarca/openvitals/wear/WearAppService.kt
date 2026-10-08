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
import java.io.BufferedWriter
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * The watch's end of the phone link, and the host of the heart rate and
 * sleep minute recorders.
 *
 * Listens for the phone's RFCOMM requests (`WearLinkProtocol`) so OpenVitals
 * on the phone can tell the app is alive and fetch what the watch recorded. A
 * foreground service: a plain background service is stopped about a minute
 * after the activity closes. Its type is `connectedDevice`, plus `health`
 * while the heart rate sensor is in use.
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

    private lateinit var store: HeartRateStore
    private lateinit var recorder: HeartRateRecorder
    private lateinit var minuteStore: SleepMinuteStore
    private lateinit var minuteRecorder: SleepMinuteRecorder

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
        store = HeartRateStore(this)
        minuteStore = SleepMinuteStore(this)
        minuteRecorder = SleepMinuteRecorder(this, minuteStore, isHeartRateRecording = { recorder.isRunning })
        recorder = HeartRateRecorder(
            this,
            store,
            onSample = minuteRecorder::noteHeartRate,
            onContact = minuteRecorder::noteHeartRateContact,
        )
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
        startRecordingIfPermitted()
        // Needs no grant: the accelerometer is open to every app.
        minuteRecorder.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // onCreate already stopped a service the system refused; do not restart it.
        if (!isForeground) return START_NOT_STICKY
        startRfcommListener()
        // A grant made after the start: the type set grows to include health.
        if (!recorder.isRunning && WearPermissions.hasHeartRate(this)) {
            startInForeground()
            startRecordingIfPermitted()
        }
        return START_STICKY
    }

    /**
     * False when the system refuses, e.g. BLUETOOTH_CONNECT not granted on
     * API 34+. Calling it again with the heart rate grant in place adds the
     * `health` type to the running service.
     */
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
        var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && WearPermissions.hasHeartRate(this)) {
            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        }
        return try {
            startForeground(NOTIFICATION_ID, notification, types)
            true
        } catch (e: Exception) {
            // SecurityException for the missing grant, or a start from the background.
            Log.e(TAG, "Cannot run the phone link in the foreground", e)
            false
        }
    }

    private fun startRecordingIfPermitted() {
        if (!WearPermissions.hasHeartRate(this)) {
            Log.i(TAG, "Heart rate not granted; link only")
            return
        }
        recorder.start()
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
                    adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, WearLinkProtocol.SERVICE_UUID)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Missing Bluetooth permission for RFCOMM server", e)
                    return@Thread
                }
                socket = server
                serverSocket = server
                Log.i(TAG, "RFCOMM server listening on ${WearLinkProtocol.SERVICE_UUID}")

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

    /** One request per connection. The phone closes after the reply; so do we. */
    private fun handleClientConnection(socket: BluetoothSocket) {
        Thread {
            try {
                val request = socket.inputStream.bufferedReader(Charsets.UTF_8).readLine() ?: return@Thread
                val out = socket.outputStream.bufferedWriter(Charsets.UTF_8)
                when {
                    WearLinkProtocol.isPing(request) -> {
                        out.write(WearLinkProtocol.PONG)
                        out.newLine()
                        Log.i(TAG, "Answered ping")
                    }
                    else -> {
                        val heartRate = WearLinkProtocol.parseHeartRateRequest(request)
                        val minutes = WearLinkProtocol.parseSleepMinutesRequest(request)
                        when {
                            heartRate != null -> sendHeartRate(out, heartRate)
                            minutes != null -> sendSleepMinutes(out, minutes)
                            else -> Log.w(TAG, "Unknown request: ${request.take(40)}")
                        }
                    }
                }
                out.flush()
            } catch (e: IOException) {
                Log.w(TAG, "Error handling client connection: ${e.message}")
            } finally {
                runCatching { socket.close() }
            }
        }.start()
    }

    private fun sendHeartRate(out: BufferedWriter, request: WearLinkProtocol.HeartRateRequest) {
        // One more than asked tells whether the limit cut the reply.
        val samples = store.since(request.sinceEpochMillis, request.limit + 1)
        val page = samples.take(request.limit)
        for (sample in page) {
            out.write(WearLinkProtocol.formatSample(sample.epochMillis, sample.bpm))
            out.newLine()
        }
        out.write(WearLinkProtocol.formatEnd(page.size, more = samples.size > page.size))
        out.newLine()
        Log.i(TAG, "Sent ${page.size} heart rate samples since ${request.sinceEpochMillis}")
    }

    private fun sendSleepMinutes(out: BufferedWriter, request: WearLinkProtocol.SleepMinutesRequest) {
        val minutes = minuteStore.since(request.sinceEpochMillis, request.limit + 1)
        val page = minutes.take(request.limit)
        for (minute in page) {
            out.write(WearLinkProtocol.formatSleepMinute(minute))
            out.newLine()
        }
        out.write(WearLinkProtocol.formatEnd(page.size, more = minutes.size > page.size))
        out.newLine()
        Log.i(TAG, "Sent ${page.size} sleep minutes since ${request.sinceEpochMillis}")
    }

    override fun onDestroy() {
        if (isForeground) {
            unregisterReceiver(bluetoothStateReceiver)
            recorder.stop()
            minuteRecorder.stop()
        }
        stopRfcommListener()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WearAppService"
        private const val SERVICE_NAME = "OpenVitalsWearApp"
        private const val CHANNEL_ID = "phone_link"
        private const val NOTIFICATION_ID = 1

        /** Starts the link once the Bluetooth grant is there; a no-op without it. */
        fun startIfPermitted(context: Context) {
            if (!WearPermissions.hasBluetooth(context)) return
            runCatching { context.startForegroundService(Intent(context, WearAppService::class.java)) }
                .onFailure { Log.e(TAG, "Cannot start the phone link", it) }
        }
    }
}
