package tech.mmarca.openvitals.features.manualentry.activity.recording

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.mmarca.openvitals.MainActivity
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.repository.PreferencesRepository

@AndroidEntryPoint
class ActivityRecordingService : Service() {
    @Inject lateinit var controller: ActivityRecordingController
    @Inject lateinit var unitFormatter: UnitFormatter
    @Inject lateinit var preferencesRepository: PreferencesRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val locationManager: LocationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    private val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private var locationUpdatesStarted = false
    private var sensorUpdatesStarted = false
    private var pressureUpdatesStarted = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pushUpRecognizer: PushUpProximityRecognizer? = null
    private var stepRecognizer: StepDetectorRepetitionRecognizer? = null
    private var jumpRecognizer: JumpRepetitionRecognizer? = null
    private var pullUpRecognizer: PullUpRepetitionRecognizer? = null
    private var foregroundStarted = false
    private var voiceAnnouncer: ActivityRecordingVoiceAnnouncer? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            scheduleGpsLostTimeout()
            controller.acceptLocation(location)
        }

        override fun onProviderDisabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) {
                controller.reportGpsDisabled()
            }
        }
    }

    private val gpsLostRunnable = Runnable {
        controller.reportGpsLost()
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val now = System.currentTimeMillis()
            val recognized = when (event.sensor.type) {
                Sensor.TYPE_PROXIMITY -> pushUpRecognizer?.onProximity(event.values.firstOrNull() ?: return, now)
                Sensor.TYPE_STEP_DETECTOR -> stepRecognizer?.onStep(now)
                Sensor.TYPE_ACCELEROMETER -> {
                    val x = event.values.getOrNull(0) ?: return
                    val y = event.values.getOrNull(1) ?: return
                    val z = event.values.getOrNull(2) ?: return
                    jumpRecognizer?.onAcceleration(x, y, z, now)
                        ?: pullUpRecognizer?.onAcceleration(x, y, z, now)
                }
                else -> null
            }
            if (recognized != null) {
                controller.acceptRecognizedRepetition()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val pressureSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_PRESSURE) {
                event.values.firstOrNull()?.let(controller::acceptBarometerPressure)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
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
        stopSensorUpdates()
        stopPressureUpdates()
        voiceAnnouncer?.shutdown()
        voiceAnnouncer = null
        serviceScope.cancel()
        foregroundStarted = false
        super.onDestroy()
    }

    private fun observeRecordingState() {
        controller.state
            .onEach { state ->
                if (!state.isActive) {
                    stopLocationUpdates()
                    stopSensorUpdates()
                    stopPressureUpdates()
                    if (foregroundStarted) {
                        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    }
                    stopSelf()
                    return@onEach
                }

                ensureForeground(state)
                if (state.status == ActivityRecordingStatus.RECORDING && state.recordingKind == ActivityRecordingKind.GPS_ROUTE) {
                    stopSensorUpdates()
                    startLocationUpdates()
                    startPressureUpdates()
                } else if (state.status == ActivityRecordingStatus.RECORDING && state.recordingKind == ActivityRecordingKind.REPETITION) {
                    stopLocationUpdates()
                    stopPressureUpdates()
                    startSensorUpdates(state)
                } else {
                    stopLocationUpdates()
                    stopSensorUpdates()
                    stopPressureUpdates()
                }
                updateNotification(state)
                voiceAnnouncer()
                    .onRecordingState(
                        state = state,
                        preferences = preferencesRepository.activityRecordingPreferences(),
                    )
            }
            .launchIn(serviceScope)
    }

    private fun voiceAnnouncer(): ActivityRecordingVoiceAnnouncer =
        voiceAnnouncer ?: ActivityRecordingVoiceAnnouncer(this, unitFormatter).also {
            voiceAnnouncer = it
        }

    private fun ensureForeground(state: ActivityRecordingState) {
        if (foregroundStarted) return
        runCatching {
            ServiceCompat.startForeground(
                this,
                NotificationId,
                buildNotification(state),
                if (state.recordingKind == ActivityRecordingKind.GPS_ROUTE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else if (
                    state.recordingKind == ActivityRecordingKind.REPETITION &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                ) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
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

    private fun startSensorUpdates(state: ActivityRecordingState) {
        if (sensorUpdatesStarted) return
        val activityType = activityEntryTypeById(state.activityTypeId) ?: return
        if (
            activityType.recordingSensor == ActivityRecordingSensor.STEP_DETECTOR &&
            !ActivityRecordingController.hasActivityRecognitionPermission(this)
        ) {
            controller.reportRecordingError(getString(R.string.activity_recording_error_activity_recognition_permission))
            return
        }
        val sensorType = activityType.recordingSensor.toAndroidSensorType() ?: return
        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor == null) {
            controller.reportRecordingError(recordingSensorUnavailableMessage(activityType.recordingSensor))
            return
        }
        pushUpRecognizer = if (activityType.recordingSensor == ActivityRecordingSensor.PROXIMITY) {
            PushUpProximityRecognizer()
        } else {
            null
        }
        stepRecognizer = if (activityType.recordingSensor == ActivityRecordingSensor.STEP_DETECTOR) {
            StepDetectorRepetitionRecognizer()
        } else {
            null
        }
        jumpRecognizer = when (activityType.id) {
            "rope_skipping" -> JumpRepetitionRecognizer(maxJumpDurationMillis = 1_250L)
            "trampoline_jumping" -> JumpRepetitionRecognizer(maxJumpDurationMillis = 2_500L)
            else -> null
        }
        pullUpRecognizer = if (activityType.id == "pull_ups") PullUpRepetitionRecognizer() else null
        sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorUpdatesStarted = true
    }

    private fun stopSensorUpdates() {
        if (!sensorUpdatesStarted) return
        runCatching {
            sensorManager.unregisterListener(sensorListener)
        }
        sensorUpdatesStarted = false
        pushUpRecognizer = null
        stepRecognizer = null
        jumpRecognizer = null
        pullUpRecognizer = null
    }

    private fun startPressureUpdates() {
        if (pressureUpdatesStarted) return
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) ?: return
        sensorManager.registerListener(pressureSensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        pressureUpdatesStarted = true
    }

    private fun stopPressureUpdates() {
        if (!pressureUpdatesStarted) return
        runCatching {
            sensorManager.unregisterListener(pressureSensorListener)
        }
        pressureUpdatesStarted = false
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
            controller.reportGpsDisabled()
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
            scheduleGpsLostTimeout()
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
        mainHandler.removeCallbacks(gpsLostRunnable)
        locationUpdatesStarted = false
    }

    private fun scheduleGpsLostTimeout() {
        mainHandler.removeCallbacks(gpsLostRunnable)
        mainHandler.postDelayed(gpsLostRunnable, GpsLostTimeoutMillis)
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

        val contentText = notificationText(state)
        val builder = NotificationCompat.Builder(this, ChannelId)
            .setSmallIcon(R.drawable.ic_stat_activity_recording)
            .setContentTitle(getString(R.string.activity_recording_notification_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)
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
        return builder.build().apply {
            flags = flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        }
    }

    private fun notificationText(state: ActivityRecordingState): String {
        val now = Instant.now()
        val totalTime = formatNotificationElapsed(state.elapsedDuration(now))
        if (state.recordingKind == ActivityRecordingKind.REPETITION) {
            val activityType = activityEntryTypeById(state.activityTypeId)
            val unit = if (activityType?.repetitionUnit == ActivityRepetitionUnit.STEPS) {
                getString(R.string.unit_steps)
            } else {
                getString(R.string.unit_reps)
            }
            return when (state.status) {
                ActivityRecordingStatus.RECORDING -> getString(
                    R.string.activity_recording_notification_repetition_recording,
                    totalTime,
                    unitFormatter.count(state.repetitionCount),
                    unit,
                )
                ActivityRecordingStatus.PAUSED -> getString(
                    R.string.activity_recording_notification_repetition_paused,
                    totalTime,
                    unitFormatter.count(state.repetitionCount),
                    unit,
                )
                ActivityRecordingStatus.RESTING -> getString(
                    R.string.activity_recording_notification_repetition_resting,
                    totalTime,
                    formatNotificationElapsed(state.restRemainingDuration()),
                )
                ActivityRecordingStatus.IDLE -> getString(R.string.activity_recording_notification_title)
            }
        }
        val movingTime = formatNotificationElapsed(state.movingDuration(now))
        val distance = unitFormatter.distance(state.distanceMeters).text
        val gpsStatus = getString(state.gpsStatusLabelRes(now))
        return when (state.status) {
            ActivityRecordingStatus.RECORDING -> getString(
                R.string.activity_recording_notification_recording,
                totalTime,
                movingTime,
                distance,
                gpsStatus,
            )
            ActivityRecordingStatus.PAUSED -> getString(
                R.string.activity_recording_notification_paused,
                totalTime,
                movingTime,
                distance,
                getString(R.string.activity_entry_recording_paused),
            )
            ActivityRecordingStatus.RESTING -> getString(R.string.activity_recording_notification_title)
            ActivityRecordingStatus.IDLE -> getString(R.string.activity_recording_notification_title)
        }
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            intent(this, action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createNotificationChannel() {
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

private fun formatNotificationElapsed(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun ActivityRecordingState.gpsStatusLabelRes(now: Instant): Int =
    when {
        status == ActivityRecordingStatus.PAUSED -> R.string.activity_entry_recording_paused
        isAutoIdle(now) -> R.string.activity_entry_recording_idle
        gpsStatus == ActivityGpsStatus.FIX -> R.string.activity_entry_recording_gps_fix
        gpsStatus == ActivityGpsStatus.POOR_ACCURACY -> R.string.activity_entry_recording_gps_poor
        gpsStatus == ActivityGpsStatus.LOST -> R.string.activity_entry_recording_gps_lost
        gpsStatus == ActivityGpsStatus.DISABLED -> R.string.activity_entry_recording_gps_off
        ActivityGpsStatus.WAITING_FOR_FIX == gpsStatus -> R.string.activity_entry_recording_waiting_for_gps
        else -> R.string.activity_entry_recording_active
    }

private fun ActivityRecordingSensor.toAndroidSensorType(): Int? =
    when (this) {
        ActivityRecordingSensor.PROXIMITY -> Sensor.TYPE_PROXIMITY
        ActivityRecordingSensor.ACCELEROMETER -> Sensor.TYPE_ACCELEROMETER
        ActivityRecordingSensor.STEP_DETECTOR -> Sensor.TYPE_STEP_DETECTOR
        ActivityRecordingSensor.GPS,
        ActivityRecordingSensor.NONE -> null
    }

private fun Context.recordingSensorUnavailableMessage(sensor: ActivityRecordingSensor): String =
    when (sensor) {
        ActivityRecordingSensor.PROXIMITY -> getString(R.string.activity_recording_error_proximity_sensor)
        ActivityRecordingSensor.ACCELEROMETER -> getString(R.string.activity_recording_error_accelerometer)
        ActivityRecordingSensor.STEP_DETECTOR -> getString(R.string.activity_recording_error_step_detector)
        ActivityRecordingSensor.GPS,
        ActivityRecordingSensor.NONE -> getString(R.string.activity_recording_error_unsupported_type)
    }

private const val ChannelId = "activity_recording"
private const val NotificationId = 4027
private const val LocationSamplingIntervalMillis = 1_000L
private const val LocationSamplingDistanceMeters = 0f
private const val GpsLostTimeoutMillis = 30_000L
private const val RequestOpenApp = 10
private const val RequestPause = 11
private const val RequestResume = 12
private const val RequestDiscard = 13
