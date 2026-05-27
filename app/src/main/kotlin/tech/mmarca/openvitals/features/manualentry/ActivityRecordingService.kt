package tech.mmarca.openvitals.features.manualentry

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.mmarca.openvitals.MainActivity
import tech.mmarca.openvitals.R

@AndroidEntryPoint
class ActivityRecordingService : Service() {
    @Inject lateinit var controller: ActivityRecordingController

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val locationManager: LocationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    private var locationUpdatesStarted = false
    private var foregroundStarted = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            controller.acceptLocation(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeRecordingState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ActionPause -> controller.pauseRecording()
            ActionResume -> controller.resumeRecording()
            ActionDiscard -> controller.discardRecording()
            ActionStop -> {
                stopLocationUpdates()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val state = controller.state.value
        if (state.isActive) {
            ensureForeground(state)
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopLocationUpdates()
        serviceScope.cancel()
        foregroundStarted = false
        super.onDestroy()
    }

    private fun observeRecordingState() {
        controller.state
            .onEach { state ->
                if (!state.isActive) {
                    stopLocationUpdates()
                    if (foregroundStarted) {
                        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    }
                    stopSelf()
                    return@onEach
                }

                ensureForeground(state)
                if (state.status == ActivityRecordingStatus.RECORDING) {
                    startLocationUpdates()
                } else {
                    stopLocationUpdates()
                }
                updateNotification(state)
            }
            .launchIn(serviceScope)
    }

    private fun ensureForeground(state: ActivityRecordingState) {
        if (foregroundStarted) return
        runCatching {
            ServiceCompat.startForeground(
                this,
                NotificationId,
                buildNotification(state),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    0
                },
            )
            foregroundStarted = true
        }.onFailure { error ->
            controller.reportRecordingError(error.message ?: getString(R.string.activity_recording_error_service))
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(state: ActivityRecordingState) {
        if (!foregroundStarted) return
        NotificationManagerCompat.from(this).notify(NotificationId, buildNotification(state))
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (locationUpdatesStarted) return
        if (!ActivityRecordingController.hasPreciseLocationPermission(this)) {
            controller.reportRecordingError(getString(R.string.activity_recording_error_precise_location_permission))
            return
        }

        if (!locationManager.isEnabled(LocationManager.GPS_PROVIDER)) {
            controller.reportRecordingError(getString(R.string.activity_recording_error_provider))
            return
        }

        runCatching {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LocationSamplingIntervalMillis,
                LocationSamplingDistanceMeters,
                locationListener,
                Looper.getMainLooper(),
            )
            locationUpdatesStarted = true
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?.let(controller::acceptLocation)
        }.onFailure { error ->
            controller.reportRecordingError(error.message ?: getString(R.string.activity_recording_error_provider))
        }
    }

    private fun stopLocationUpdates() {
        if (!locationUpdatesStarted) return
        runCatching {
            locationManager.removeUpdates(locationListener)
        }
        locationUpdatesStarted = false
    }

    private fun buildNotification(state: ActivityRecordingState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            RequestOpenApp,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, ChannelId)
            .setSmallIcon(R.drawable.ic_stat_activity_recording)
            .setContentTitle(getString(R.string.activity_recording_notification_title))
            .setContentText(notificationText(state))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setOnlyAlertOnce(true)
            .setOngoing(state.isActive)
            .setSilent(true)

        if (state.status == ActivityRecordingStatus.RECORDING) {
            builder.addAction(
                R.drawable.ic_stat_activity_recording,
                getString(R.string.action_pause),
                servicePendingIntent(ActionPause, RequestPause),
            )
        } else if (state.status == ActivityRecordingStatus.PAUSED) {
            builder.addAction(
                R.drawable.ic_stat_activity_recording,
                getString(R.string.action_resume),
                servicePendingIntent(ActionResume, RequestResume),
            )
        }
        builder.addAction(
            R.drawable.ic_stat_activity_recording,
            getString(R.string.action_discard),
            servicePendingIntent(ActionDiscard, RequestDiscard),
        )
        return builder.build()
    }

    private fun notificationText(state: ActivityRecordingState): String =
        when (state.status) {
            ActivityRecordingStatus.RECORDING -> getString(
                R.string.activity_recording_notification_recording,
                state.points.size,
            )
            ActivityRecordingStatus.PAUSED -> getString(
                R.string.activity_recording_notification_paused,
                state.points.size,
            )
            ActivityRecordingStatus.IDLE -> getString(R.string.activity_recording_notification_title)
        }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            intent(this, action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ChannelId,
            getString(R.string.activity_recording_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun LocationManager.isEnabled(provider: String): Boolean =
        runCatching { isProviderEnabled(provider) }.getOrDefault(false)

    companion object {
        const val ActionStart = "tech.mmarca.openvitals.action.START_ACTIVITY_RECORDING"
        const val ActionPause = "tech.mmarca.openvitals.action.PAUSE_ACTIVITY_RECORDING"
        const val ActionResume = "tech.mmarca.openvitals.action.RESUME_ACTIVITY_RECORDING"
        const val ActionDiscard = "tech.mmarca.openvitals.action.DISCARD_ACTIVITY_RECORDING"
        const val ActionStop = "tech.mmarca.openvitals.action.STOP_ACTIVITY_RECORDING"

        fun intent(context: Context, action: String): Intent =
            Intent(context, ActivityRecordingService::class.java).setAction(action)
    }
}

private const val ChannelId = "activity_recording"
private const val NotificationId = 4027
private const val LocationSamplingIntervalMillis = 1_000L
private const val LocationSamplingDistanceMeters = 0f
private const val RequestOpenApp = 10
private const val RequestPause = 11
private const val RequestResume = 12
private const val RequestDiscard = 13
