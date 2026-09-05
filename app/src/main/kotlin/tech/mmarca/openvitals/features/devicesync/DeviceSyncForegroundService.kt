package tech.mmarca.openvitals.features.devicesync

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
import tech.mmarca.openvitals.R

/**
 * Inert keep-alive foreground service for a phone-to-phone transfer. The
 * transfer runs in the ViewModel; this only keeps the process foregrounded.
 * [start] is best-effort and never during a recording; [stop] only stops
 * this class.
 */
class DeviceSyncForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
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

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_activity_recording)
            .setContentTitle(getString(R.string.device_sync_notification_title))
            .setContentText(getString(R.string.device_sync_notification_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.device_sync_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "DeviceSync"
        private const val CHANNEL_ID = "openvitals_device_sync"
        private const val NOTIFICATION_ID = 2041

        /** Starts the service. False on any failure; the transfer then runs without it. */
        fun start(context: Context): Boolean = runCatching {
            context.startForegroundService(Intent(context, DeviceSyncForegroundService::class.java))
            true
        }.getOrElse { error ->
            Log.w(TAG, "foreground service start failed: ${error.message}")
            false
        }

        /** Stops this service class only. Safe to call when it never started. */
        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, DeviceSyncForegroundService::class.java))
            }
        }
    }
}
