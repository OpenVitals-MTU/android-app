package tech.mmarca.openvitals.devices.xiaomi

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.R

/**
 * What lets a closed app hear a weigh-in. Android scans for a background app
 * a twentieth of the time, and the scale's result is on the air for two
 * seconds; only an app with a foreground service gets the radio full time.
 * A port, so the listener's timing is testable without a service.
 */
interface ScaleForeground {

    /**
     * Brings the app to the foreground for a weigh-in. False when it did not
     * need to (the app is on screen, or a recording or a transfer already
     * holds the foreground slot) or was not allowed to.
     */
    fun raise(): Boolean

    /** Ends what [raise] started. Safe when it started nothing. */
    fun lower()
}

@Singleton
class ScaleListeningForeground @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ScaleForeground {

    override fun raise(): Boolean {
        // The app's one foreground slot is someone else's, or the app is on screen. Either way the
        // scan already runs full time, and a second service would only be a second notification.
        if (isForegroundAlready()) return false
        return ScaleListeningService.start(context)
    }

    override fun lower() = ScaleListeningService.stop(context)

    private fun isForegroundAlready(): Boolean {
        val state = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(state)
        return state.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
    }
}

/**
 * Inert foreground service for the seconds a weigh-in takes. It runs no
 * logic: [XiaomiScaleListener] starts it when Android reports the scale
 * awake and stops it when the result is in, or after its time limit.
 *
 * Starting it from the background is allowed because the scale is an
 * associated companion device. Nobody tapped anything, but someone stepped
 * on a scale, which is why this is the one service the app starts unasked.
 */
class ScaleListeningService : Service() {

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.scale_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val started = runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        }
        if (started.isFailure) {
            Log.w(TAG, "startForeground failed: ${started.exceptionOrNull()?.message}")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Silent, and Android holds a new service's notification back for its first ten seconds.
    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_activity_recording)
            .setContentTitle(getString(R.string.scale_notification_title))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        private const val TAG = "ScaleListening"
        private const val CHANNEL_ID = "scale_weigh_ins"
        private const val NOTIFICATION_ID = 2051

        /** Starts the service. False on any failure; the weigh-in is then heard at background speed. */
        fun start(context: Context): Boolean = runCatching {
            context.startForegroundService(Intent(context, ScaleListeningService::class.java))
            true
        }.getOrElse { error ->
            Log.w(TAG, "foreground service start failed: ${error.message}")
            false
        }

        /** Stops this service class only. Safe to call when it never started. */
        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, ScaleListeningService::class.java)) }
        }
    }
}
