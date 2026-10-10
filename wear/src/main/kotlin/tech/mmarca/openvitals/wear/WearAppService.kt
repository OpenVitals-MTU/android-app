package tech.mmarca.openvitals.wear

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
 * The watch's end of the phone link, and the host of the heart rate and
 * sleep minute recorders.
 *
 * The link itself is [WearLinkServerHost] over `:wearlink`; this service
 * keeps it, the recorders and the trust store alive as a foreground
 * service (a plain background service is stopped about a minute after the
 * activity closes). Its type is `connectedDevice`, plus `health` while the
 * heart rate sensor is in use. Bluetooth off closes the listener; on again
 * reopens it; a bond change refreshes which trusted phones are still paired.
 *
 * The phone side is `app/.../devices/wearos/BluetoothWearOsNodePort`.
 */
class WearAppService : Service() {

    /** False when the system refused the foreground start: nothing may run then. */
    private var isForeground = false

    private lateinit var store: MetricStore
    private lateinit var recorder: HeartRateRecorder
    private lateinit var minuteRecorder: SleepMinuteRecorder
    private lateinit var ppgLogger: PpgRawLogger
    private lateinit var trust: WearTrustStore
    private lateinit var link: WearLinkServerHost

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_ON -> {
                        WearLinkState.update { it.copy(bluetoothOn = true) }
                        link.start()
                        refreshBondLost()
                    }
                    BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                        WearLinkState.update { it.copy(bluetoothOn = false) }
                        link.stop()
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> refreshBondLost()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        store = MetricStore(this)
        // Off the main thread: a week of heart rate is tens of thousands of rows.
        Thread({ LegacyStoreImport.run(this, store) }, "LegacyStoreImport").start()
        ppgLogger = PpgRawLogger(this)
        minuteRecorder = SleepMinuteRecorder(this, store, isHeartRateRecording = { recorder.isRunning })
        recorder = HeartRateRecorder(
            this,
            store,
            onSample = minuteRecorder::noteHeartRate,
            onContact = minuteRecorder::noteHeartRateContact,
        )
        trust = WearTrustStore(this).also { it.onPending = { pending -> WearPhoneRequests.notify(this, pending) } }
        link = WearLinkServerHost(
            adapter = { getSystemService(BluetoothManager::class.java)?.adapter },
            trust = trust,
            store = store,
            localName = ::localName,
        )
        isForeground = startInForeground()
        if (!isForeground) {
            stopSelf()
            return
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bluetoothReceiver, filter)
        }
        WearLinkState.update { it.copy(bluetoothOn = getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true) }
        link.start()
        refreshBondLost()
        startRecordingIfPermitted()
        // Needs no grant: the accelerometer is open to every app.
        minuteRecorder.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // onCreate already stopped a service the system refused; do not restart it.
        if (!isForeground) return START_NOT_STICKY
        if (intent?.action == ACTION_TOGGLE_PPG_LOG) {
            if (ppgLogger.isRunning) ppgLogger.stop() else ppgLogger.start()
            WearLinkState.update { it.copy(ppgLogging = ppgLogger.isRunning) }
        }
        link.start()
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
            NotificationChannel(CHANNEL_ID, getString(R.string.link_channel_name), NotificationManager.IMPORTANCE_MIN),
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

    /** The watch's Bluetooth name, as the phone will show it. */
    @SuppressLint("MissingPermission")
    private fun localName(): String {
        // The service only runs with the Bluetooth grant (startIfPermitted); the catch covers a revocation since.
        if (!WearPermissions.hasBluetooth(this)) return Build.MODEL
        return runCatching { getSystemService(BluetoothManager::class.java)?.adapter?.name }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: Build.MODEL
    }

    /** Trusted phones the watch is no longer bonded with: a bond the phone or the watch dropped. */
    @SuppressLint("MissingPermission")
    private fun refreshBondLost() {
        if (!WearPermissions.hasBluetooth(this)) return
        val bonded = runCatching {
            getSystemService(BluetoothManager::class.java)?.adapter?.bondedDevices.orEmpty().map { it.address.uppercase() }.toSet()
        }.getOrDefault(emptySet())
        val lost = trust.trusted().filter { it.address.uppercase() !in bonded }
        WearLinkState.update { it.copy(bondLost = lost) }
    }

    override fun onDestroy() {
        if (isForeground) {
            unregisterReceiver(bluetoothReceiver)
            recorder.stop()
            minuteRecorder.stop()
            ppgLogger.stop()
            WearLinkState.update { it.copy(ppgLogging = false) }
        }
        link.stop()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WearAppService"
        private const val CHANNEL_ID = "phone_link"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_TOGGLE_PPG_LOG = "tech.mmarca.openvitals.wear.TOGGLE_PPG_LOG"

        /** Starts or stops the raw PPG log (debuggable builds with the sensor only). */
        fun togglePpgLog(context: Context) {
            if (!WearPermissions.hasBluetooth(context)) return
            runCatching {
                context.startForegroundService(Intent(context, WearAppService::class.java).setAction(ACTION_TOGGLE_PPG_LOG))
            }.onFailure { Log.e(TAG, "Cannot toggle the PPG log", it) }
        }

        /** Starts the link once the Bluetooth grant is there; a no-op without it. */
        fun startIfPermitted(context: Context) {
            if (!WearPermissions.hasBluetooth(context)) return
            runCatching { context.startForegroundService(Intent(context, WearAppService::class.java)) }
                .onFailure { Log.e(TAG, "Cannot start the phone link", it) }
        }
    }
}
