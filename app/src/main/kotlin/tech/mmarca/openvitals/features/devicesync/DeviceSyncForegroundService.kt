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
 * Inert keep-alive foreground service for a phone-to-phone sync transfer.
 *
 * The transfer itself runs in-process in the wizard's ViewModel — the RFCOMM
 * byte pumps live there, so this service does no work of its own. It exists
 * solely to promote the process to the foreground for the duration of a
 * transfer so the OS does not kill the app if the user glances away mid-sync.
 *
 * Foreground-slot policy (ported from the Flutter `device_sync_foreground.dart`):
 * the app treats the foreground slot as effectively single — recording, the
 * Apple Health import, and phone sync contend for it. So:
 *  - [start] is best-effort and starts nothing while an activity recording is
 *    live (the wizard refuses to sync then anyway); any start failure means
 *    the transfer simply proceeds in-process without a service.
 *  - [stop] only ever stops THIS service class, so a sync that ran without the
 *    slot can never tear down an unrelated foreground service on teardown.
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

        /**
         * Starts the keep-alive service. Returns false (and starts nothing) on
         * any failure — the caller then runs the transfer in-process. Callers
         * must have already ensured no activity recording is live.
         */
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
