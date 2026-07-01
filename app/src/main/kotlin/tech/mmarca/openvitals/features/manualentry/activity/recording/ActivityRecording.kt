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
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.altitude.AltitudeConverter
import android.media.MediaPlayer
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.compose.runtime.Immutable
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ActivityRecordingLap
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarkerType
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.BleDeviceConnectionStatus
import tech.mmarca.openvitals.domain.model.BleRecordingMetrics
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardLayout
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator

enum class ActivityRecordingStatus {
    IDLE,
    RECORDING,
    RESTING,
    PAUSED,
}

enum class ActivityRecordingKind {
    GPS_ROUTE,
    REPETITION,
    TIMED,
}

enum class ActivityGpsStatus {
    WAITING_FOR_FIX,
    FIX,
    POOR_ACCURACY,
    LOST,
    DISABLED,
}

@Immutable
data class ActivityRecordingState(
    val status: ActivityRecordingStatus = ActivityRecordingStatus.IDLE,
    val recordingKind: ActivityRecordingKind = ActivityRecordingKind.GPS_ROUTE,
    val activityTypeId: String? = null,
    val exerciseType: Int? = null,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val pausedStartedAt: Instant? = null,
    val totalPausedMillis: Long = 0L,
    val pauseIntervals: List<ActivityPauseInterval> = emptyList(),
    val points: List<ExerciseRoutePoint> = emptyList(),
    val routeBreakIndexes: List<Int> = emptyList(),
    val manualLaps: List<ActivityRecordingLap> = emptyList(),
    val markers: List<ActivityRecordingMarker> = emptyList(),
    val latestUiPoint: ExerciseRoutePoint? = null,
    val distanceMeters: Double = 0.0,
    val elevationGainedMeters: Double = 0.0,
    val elevationLostMeters: Double = 0.0,
    val barometerElevationGainedMeters: Double = 0.0,
    val barometerElevationLostMeters: Double = 0.0,
    val hasBarometerElevation: Boolean = false,
    val lastBarometerAltitudeMeters: Double? = null,
    val currentSpeedMetersPerSecond: Double = 0.0,
    val maxSpeedMetersPerSecond: Double = 0.0,
    val gpsStatus: ActivityGpsStatus = ActivityGpsStatus.WAITING_FOR_FIX,
    val keepScreenOnDuringRecording: Boolean = ActivityRecordingPreferences.DefaultKeepScreenOnDuringRecording,
    val autoIdleEnabled: Boolean = ActivityRecordingPreferences.DefaultAutoIdleEnabled,
    val autoIdleTimeoutMillis: Long = ActivityRecordingPreferences.DefaultAutoIdleTimeoutSeconds * 1_000L,
    val lastMovementAt: Instant? = null,
    val totalIdleMillis: Long = 0L,
    val repetitionCount: Long = 0L,
    val currentSetRepetitionCount: Long = 0L,
    val repetitionSets: List<ActivityRecordedRepetitionSet> = emptyList(),
    val repetitionRestSeconds: Long = 0L,
    val currentSetStartedAt: Instant? = null,
    val restStartedAt: Instant? = null,
    val accumulatedRestMillis: Long = 0L,
    val lastAccuracyMeters: Double? = null,
    val lastLocationTime: Instant? = null,
    val droppedPointCount: Int = 0,
    val errorMessage: String? = null,
    val currentHeartRateBpm: Long? = null,
    val currentCyclingCadenceRpm: Long? = null,
    val currentPowerWatts: Double? = null,
    val currentSensorSpeedMetersPerSecond: Double? = null,
    val currentRunningCadenceRpm: Long? = null,
    val bleHeartRateNoSignal: Boolean = false,
    val bleDeviceStatuses: List<BleDeviceConnectionStatus> = emptyList(),
    val dashboardLayout: ActivityRecordingDashboardLayout = ActivityRecordingDashboardLayout(),
) {
    val isActive: Boolean
        get() = status == ActivityRecordingStatus.RECORDING ||
            status == ActivityRecordingStatus.RESTING ||
            status == ActivityRecordingStatus.PAUSED
}

@Immutable
data class ActivityRecordedRepetitionSet(
    val repetitions: Long,
    val restSeconds: Long,
    val activeMillis: Long,
)

@Immutable
data class ActivityRecordingSnapshot(
    val exerciseType: Int,
    val recordingKind: ActivityRecordingKind = ActivityRecordingKind.GPS_ROUTE,
    val activityTypeId: String? = null,
    val startTime: Instant,
    val endTime: Instant,
    val points: List<ExerciseRoutePoint>,
    val pauseIntervals: List<ActivityPauseInterval>,
    val routeBreakIndexes: List<Int> = emptyList(),
    val manualLaps: List<ActivityRecordingLap> = emptyList(),
    val markers: List<ActivityRecordingMarker> = emptyList(),
    val distanceMeters: Double,
    val elevationGainedMeters: Double,
    val repetitionCount: Long = 0L,
    val repetitionSets: List<ActivityRecordedRepetitionSet> = emptyList(),
    val bleSamples: BleRecordingSampleBuffer = BleRecordingSampleBuffer(),
)

@Singleton
class ActivityRecordingController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val bleSensorCoordinator: BleSensorCoordinator,
    private val recordingStore: ActivityRecordingStore = ActivityRecordingStore(context),
) {
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bleMetricsScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val locationProcessingDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val locationProcessingScope = CoroutineScope(SupervisorJob() + locationProcessingDispatcher)
    private val altitudeConverter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        AltitudeConverter()
    } else {
        null
    }
    private val _state = MutableStateFlow(recordingStore.restore())
    private var recordingGeneration = 0L
    private var restCompletionJob: Job? = null
    val state: StateFlow<ActivityRecordingState> = _state.asStateFlow()

    init {
        scheduleRestCompletion(_state.value)
        bleSensorCoordinator.metrics
            .onEach { metrics -> acceptBleMetrics(metrics) }
            .launchIn(bleMetricsScope)
    }

    fun startRecording(activityType: ActivityEntryType, initialFix: Location?): Boolean =
        startRecording(activityType, initialFix, repetitionRestSeconds = 0L)

    fun startRecording(
        activityType: ActivityEntryType,
        initialFix: Location?,
        repetitionRestSeconds: Long,
    ): Boolean =
        if (activityType.supportsGpsRoute) {
            startGpsRecording(activityType, initialFix)
        } else if (activityType.isRepetitionLike) {
            startRepetitionRecording(activityType, repetitionRestSeconds)
        } else if (activityType.recordingSensor == ActivityRecordingSensor.BLE) {
            startTimedRecording(activityType)
        } else {
            updateAndPersist(
                _state.value.copy(
                    errorMessage = context.getString(R.string.activity_recording_error_unsupported_type),
                )
            )
            false
        }

    fun startRecording(exerciseType: Int, initialFix: Location?): Boolean {
        val activityType = DefaultActivityEntryTypes.firstOrNull { it.exerciseType == exerciseType && it.supportsGpsRoute }
            ?: return false
        return startRecording(activityType, initialFix)
    }

    fun prepareRecordingDashboard(activityType: ActivityEntryType) {
        if (_state.value.isActive) return
        val recordingKind = activityType.recordingKind()
        persistenceScope.coroutineContext.cancelChildren()
        recordingGeneration += 1
        updateAndPersist(
            ActivityRecordingState(
                status = ActivityRecordingStatus.IDLE,
                recordingKind = recordingKind,
                activityTypeId = activityType.id,
                exerciseType = activityType.exerciseType,
                dashboardLayout = preferencesRepository.activityRecordingDashboardLayout(activityType.id),
            ),
            replaceRoutePoints = true,
        )
        previewBleConnections()
    }

    fun updateDashboardLayout(layout: ActivityRecordingDashboardLayout) {
        val current = _state.value
        val activityTypeId = current.activityTypeId ?: return
        if (current.status == ActivityRecordingStatus.RECORDING) return
        val normalized = layout.normalized()
        preferencesRepository.setActivityRecordingDashboardLayout(activityTypeId, normalized)
        updateAndPersist(current.copy(dashboardLayout = normalized, errorMessage = null))
    }

    fun clearPreparedRecording() {
        if (_state.value.isActive) return
        clearRecording()
    }

    private fun startGpsRecording(activityType: ActivityEntryType, initialFix: Location?): Boolean {
        val recordingPreferences = preferencesRepository.activityRecordingPreferences()
        val dashboardLayout = preferencesRepository.activityRecordingDashboardLayout(activityType.id)
        if (!hasPreciseLocationPermission(context)) {
            updateAndPersist(
                _state.value.copy(
                    errorMessage = context.getString(R.string.activity_recording_error_precise_location_permission),
                )
            )
            return false
        }
        if (!hasNotificationPermission(context)) {
            updateAndPersist(
                _state.value.copy(
                    errorMessage = context.getString(R.string.activity_recording_error_notification_permission),
                )
            )
            return false
        }
        val now = Instant.now()
        val lockedFix = initialFix
        val initialFixQuality = lockedFix?.activityGpsFixQuality(
            now = now,
            requiredAccuracyMeters = recordingPreferences.requiredGpsAccuracyMeters.toDouble(),
        )
        if (lockedFix == null || initialFixQuality?.isPrecise != true) {
            updateAndPersist(
                _state.value.copy(
                    errorMessage = context.getString(R.string.activity_recording_error_waiting_for_gps),
                )
            )
            return false
        }

        persistenceScope.coroutineContext.cancelChildren()
        recordingStore.clear()
        recordingGeneration += 1
        updateAndPersist(
            ActivityRecordingState(
                status = ActivityRecordingStatus.RECORDING,
                recordingKind = ActivityRecordingKind.GPS_ROUTE,
                activityTypeId = activityType.id,
                exerciseType = activityType.exerciseType,
                startTime = now,
                gpsStatus = ActivityGpsStatus.FIX,
                keepScreenOnDuringRecording = recordingPreferences.keepScreenOnDuringRecording,
                autoIdleEnabled = recordingPreferences.autoIdleEnabled,
                autoIdleTimeoutMillis = recordingPreferences.autoIdleTimeoutSeconds * 1_000L,
                lastMovementAt = now,
                lastAccuracyMeters = initialFixQuality.accuracyMeters,
                lastLocationTime = now,
                dashboardLayout = dashboardLayout,
            ),
            replaceRoutePoints = true,
        )
        acceptLocation(Location(lockedFix).apply { time = now.toEpochMilli() })
        bleSensorCoordinator.startRecording()
        acceptBleMetrics(bleSensorCoordinator.metrics.value)
        ContextCompat.startForegroundService(
            context,
            ActivityRecordingService.intent(context, ActivityRecordingService.ActionStart),
        )
        return true
    }

    private fun startRepetitionRecording(
        activityType: ActivityEntryType,
        repetitionRestSeconds: Long,
    ): Boolean {
        if (!hasNotificationPermission(context)) {
            updateAndPersist(
                _state.value.copy(
                    errorMessage = context.getString(R.string.activity_recording_error_notification_permission),
                )
            )
            return false
        }

        val now = Instant.now()
        val recordingPreferences = preferencesRepository.activityRecordingPreferences()
        val dashboardLayout = preferencesRepository.activityRecordingDashboardLayout(activityType.id)
        persistenceScope.coroutineContext.cancelChildren()
        recordingStore.clear()
        recordingGeneration += 1
        updateAndPersist(
            ActivityRecordingState(
                status = ActivityRecordingStatus.RECORDING,
                recordingKind = ActivityRecordingKind.REPETITION,
                activityTypeId = activityType.id,
                exerciseType = activityType.exerciseType,
                startTime = now,
                keepScreenOnDuringRecording = recordingPreferences.keepScreenOnDuringRecording,
                currentSetStartedAt = now,
                repetitionRestSeconds = repetitionRestSeconds.coerceAtLeast(0L),
                dashboardLayout = dashboardLayout,
            ),
            replaceRoutePoints = true,
        )
        bleSensorCoordinator.startRecording()
        acceptBleMetrics(bleSensorCoordinator.metrics.value)
        ContextCompat.startForegroundService(
            context,
            ActivityRecordingService.intent(context, ActivityRecordingService.ActionStart),
        )
        return true
    }

    private fun startTimedRecording(activityType: ActivityEntryType): Boolean {
        if (!hasNotificationPermission(context)) {
            updateAndPersist(
                _state.value.copy(
                    errorMessage = context.getString(R.string.activity_recording_error_notification_permission),
                )
            )
            return false
        }

        val now = Instant.now()
        val recordingPreferences = preferencesRepository.activityRecordingPreferences()
        val dashboardLayout = preferencesRepository.activityRecordingDashboardLayout(activityType.id)
        persistenceScope.coroutineContext.cancelChildren()
        recordingStore.clear()
        recordingGeneration += 1
        updateAndPersist(
            ActivityRecordingState(
                status = ActivityRecordingStatus.RECORDING,
                recordingKind = ActivityRecordingKind.TIMED,
                activityTypeId = activityType.id,
                exerciseType = activityType.exerciseType,
                startTime = now,
                keepScreenOnDuringRecording = recordingPreferences.keepScreenOnDuringRecording,
                dashboardLayout = dashboardLayout,
            ),
            replaceRoutePoints = true,
        )
        bleSensorCoordinator.startRecording()
        acceptBleMetrics(bleSensorCoordinator.metrics.value)
        ContextCompat.startForegroundService(
            context,
            ActivityRecordingService.intent(context, ActivityRecordingService.ActionStart),
        )
        return true
    }

    fun previewBleConnections() {
        if (_state.value.isActive) return
        bleSensorCoordinator.refreshConnections()
        acceptBleMetrics(bleSensorCoordinator.metrics.value)
    }

    fun stopBlePreview() {
        if (_state.value.isActive) return
        bleSensorCoordinator.disconnectAll()
    }

    fun acceptBleMetrics(metrics: BleRecordingMetrics) {
        val current = _state.value
        updateAndPersist(
            current.copy(
                currentHeartRateBpm = metrics.heartRateBpm,
                currentCyclingCadenceRpm = metrics.cyclingCadenceRpm,
                currentPowerWatts = metrics.powerWatts,
                currentSensorSpeedMetersPerSecond = metrics.cyclingSpeedMetersPerSecond
                    ?: metrics.runningSpeedMetersPerSecond,
                currentRunningCadenceRpm = metrics.runningCadenceRpm,
                bleHeartRateNoSignal = metrics.heartRateNoSignal && metrics.heartRateBpm == null,
                bleDeviceStatuses = metrics.deviceStatuses.ifEmpty { current.bleDeviceStatuses },
            ),
        )
    }

    fun pauseRecording() {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING) return
        updateAndPersist(
            current.copy(
                status = ActivityRecordingStatus.PAUSED,
                pausedStartedAt = Instant.now(),
                errorMessage = null,
            )
        )
    }

    fun resumeRecording() {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.PAUSED) return
        val now = Instant.now()
        val pausedMillis = current.pausedStartedAt
            ?.let { Duration.between(it, now).toMillis().coerceAtLeast(0L) }
            ?: 0L
        val closedPause = current.pausedStartedAt?.toPauseInterval(now)
        updateAndPersist(
            current.copy(
                status = ActivityRecordingStatus.RECORDING,
                pausedStartedAt = null,
                totalPausedMillis = current.totalPausedMillis + pausedMillis,
                pauseIntervals = current.pauseIntervals + listOfNotNull(closedPause),
                errorMessage = null,
            )
        )
    }

    fun addManualLap() {
        val current = _state.value
        if (!current.isActive || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        val start = current.manualLaps.maxByOrNull { it.endTime }?.endTime
            ?: current.startTime
            ?: return
        val end = Instant.now()
        if (!start.isBefore(end)) return
        val distanceMeters = activityRecordingRouteDistanceMeters(
            points = current.points,
            routeBreakIndexes = current.routeBreakIndexes,
            startTime = start,
            endTime = end,
        ).takeIf { it > 0.0 }
        updateAndPersist(
            current.copy(
                manualLaps = current.manualLaps + ActivityRecordingLap(
                    startTime = start,
                    endTime = end,
                    distanceMeters = distanceMeters,
                ),
                errorMessage = null,
            )
        )
    }

    fun addMarker() {
        val current = _state.value
        if (!current.isActive || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        val point = current.latestUiPoint ?: current.points.lastOrNull() ?: return
        val markerNumber = current.markers.size + 1
        updateAndPersist(
            current.copy(
                markers = current.markers + ActivityRecordingMarker(
                    id = UUID.randomUUID().toString(),
                    time = point.time,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    altitudeMeters = point.altitudeMeters,
                    name = context.getString(R.string.activity_entry_recording_marker_default_name, markerNumber),
                    type = ActivityRecordingMarkerType.Generic.value,
                ),
                errorMessage = null,
            )
        )
    }

    fun updateMarker(marker: ActivityRecordingMarker) {
        val current = _state.value
        if (!current.isActive || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        updateAndPersist(
            current.copy(
                markers = current.markers.map { existing ->
                    if (existing.id == marker.id) marker else existing
                },
                errorMessage = null,
            )
        )
    }

    fun deleteMarker(markerId: String) {
        val current = _state.value
        if (!current.isActive || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        updateAndPersist(
            current.copy(
                markers = current.markers.filterNot { it.id == markerId },
                errorMessage = null,
            )
        )
    }

    fun discardRecording() {
        bleSensorCoordinator.stopRecording()
        clearRecording()
        stopRecordingService()
    }

    fun finishRecording(): ActivityRecordingSnapshot? {
        val current = _state.value
        val start = current.startTime ?: return null
        val exerciseType = current.exerciseType ?: return null
        if (!current.isActive) return null

        val end = Instant.now().takeIf { it.isAfter(start) } ?: start.plusSeconds(1)
        val pauseIntervals = current.pauseIntervals +
            listOfNotNull(current.pausedStartedAt?.toPauseInterval(end))
        val manualLaps = current.closedManualLaps(end)
        val repetitionSets = current.recordedRepetitionSets(end)
        val bleSamples = bleSensorCoordinator.stopRecording()
        val snapshot = ActivityRecordingSnapshot(
            exerciseType = exerciseType,
            recordingKind = current.recordingKind,
            activityTypeId = current.activityTypeId,
            startTime = start,
            endTime = end,
            points = current.points,
            pauseIntervals = pauseIntervals,
            routeBreakIndexes = current.routeBreakIndexes,
            manualLaps = manualLaps,
            markers = current.markers,
            distanceMeters = current.distanceMeters,
            elevationGainedMeters = current.elevationGainedMeters,
            repetitionCount = current.repetitionCount,
            repetitionSets = repetitionSets,
            bleSamples = bleSamples,
        )
        clearRecording()
        stopRecordingService()
        return snapshot
    }

    fun acceptLocation(location: Location) {
        val generation = recordingGeneration
        updateGpsStatus(location)
        locationProcessingScope.launch {
            acceptConvertedLocation(
                location = location.withMslAltitude(),
                generation = generation,
                recordingPreferences = preferencesRepository.activityRecordingPreferences(),
            )
        }
    }

    private fun acceptConvertedLocation(
        location: Location,
        generation: Long,
        recordingPreferences: ActivityRecordingPreferences,
    ) {
        if (generation != recordingGeneration) return
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING) return
        if (current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return

        val fixQuality = location.activityGpsFixQuality(
            startTime = current.startTime,
            requiredAccuracyMeters = recordingPreferences.requiredGpsAccuracyMeters.toDouble(),
        )
        val point = location.toRoutePoint(fixQuality.locationTime)
        if (!fixQuality.isPrecise) {
            updateAndPersist(
                current.withDroppedLocation(
                    fixQuality.accuracyMeters,
                    fixQuality.locationTime,
                    gpsStatus = if (fixQuality.accuracyMeters == null) {
                        ActivityGpsStatus.WAITING_FOR_FIX
                    } else {
                        ActivityGpsStatus.POOR_ACCURACY
                    },
                ).copy(latestUiPoint = point)
            )
            return
        }
        val accuracy = fixQuality.accuracyMeters ?: return

        val lastPoint = current.points.lastOrNull()
        var distanceIncrement = 0.0
        var elevationIncrement = 0.0
        var elevationLossIncrement = 0.0
        var currentSpeedMetersPerSecond = 0.0
        var routeBreakIndexes = current.routeBreakIndexes
        var lastMovementAt = current.lastMovementAt ?: current.startTime ?: point.time
        var totalIdleMillis = current.totalIdleMillis
        if (lastPoint != null) {
            if (!point.time.isAfter(lastPoint.time)) {
                updateAndPersist(current.withDroppedLocation(accuracy, point.time, ActivityGpsStatus.FIX))
                return
            }

            val elapsedMillis = Duration.between(lastPoint.time, point.time)
                .toMillis()
                .coerceAtLeast(0L)
            val distanceMeters = lastPoint.recordingDistanceMetersTo(point)
            if (
                distanceMeters < current.minimumSampleDistanceMeters(recordingPreferences) ||
                elapsedMillis < recordingPreferences.recordingTimeIntervalMillis.toLong()
            ) {
                updateAndPersist(
                    current.withLocationMetadata(
                        accuracyMeters = accuracy,
                        locationTime = point.time,
                        gpsStatus = ActivityGpsStatus.FIX,
                        recordingPreferences = recordingPreferences,
                    ).copy(latestUiPoint = point)
                )
                return
            }
            val startsNewRouteSegment = recordingPreferences.routeGapMeters
                ?.let { distanceMeters > it.toDouble() }
                ?: false
            if (startsNewRouteSegment) {
                routeBreakIndexes = routeBreakIndexes + current.points.size
            } else {
                if (isImplausibleJump(lastPoint, point, distanceMeters, elapsedMillis, accuracy)) {
                    updateAndPersist(current.withDroppedLocation(accuracy, point.time, ActivityGpsStatus.FIX))
                    return
                }
                distanceIncrement = distanceMeters
                elevationIncrement = lastPoint.elevationGainMetersTo(point)
                elevationLossIncrement = lastPoint.elevationLossMetersTo(point)
                currentSpeedMetersPerSecond = distanceMeters / (elapsedMillis / 1_000.0)
                if (current.autoIdleEnabled) {
                    val idleStartedAt = lastMovementAt.plusMillis(current.autoIdleTimeoutMillis)
                    if (point.time.isAfter(idleStartedAt)) {
                        totalIdleMillis += Duration.between(idleStartedAt, point.time)
                            .toMillis()
                            .coerceAtLeast(0L)
                    }
                }
                lastMovementAt = point.time
            }
        }

        updateAndPersist(
            current.copy(
                points = current.points + point,
                routeBreakIndexes = routeBreakIndexes,
                latestUiPoint = point,
                distanceMeters = current.distanceMeters + distanceIncrement,
                elevationGainedMeters = current.elevationGainedMeters + elevationIncrement,
                elevationLostMeters = current.elevationLostMeters + elevationLossIncrement,
                currentSpeedMetersPerSecond = currentSpeedMetersPerSecond,
                maxSpeedMetersPerSecond = maxOf(current.maxSpeedMetersPerSecond, currentSpeedMetersPerSecond),
                gpsStatus = ActivityGpsStatus.FIX,
                keepScreenOnDuringRecording = recordingPreferences.keepScreenOnDuringRecording,
                autoIdleEnabled = recordingPreferences.autoIdleEnabled,
                autoIdleTimeoutMillis = recordingPreferences.autoIdleTimeoutSeconds * 1_000L,
                lastMovementAt = lastMovementAt,
                totalIdleMillis = totalIdleMillis,
                lastAccuracyMeters = accuracy,
                lastLocationTime = point.time,
                errorMessage = null,
            ),
            routePointToAppend = point,
        )
    }

    fun acceptRecognizedRepetition() {
        adjustRepetitionCount(1)
    }

    fun adjustRepetitionCount(delta: Long) {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING) {
            return
        }
        if (current.recordingKind == ActivityRecordingKind.GPS_ROUTE) {
            if (activityEntryTypeById(current.activityTypeId)?.supportsStepCounting != true) return
            val nextCount = (current.repetitionCount + delta).coerceAtLeast(0L)
            updateAndPersist(
                current.copy(
                    currentSetRepetitionCount = nextCount,
                    repetitionCount = nextCount,
                    errorMessage = null,
                )
            )
            return
        }
        if (current.recordingKind != ActivityRecordingKind.REPETITION) return

        val nextCurrentSetCount = (current.currentSetRepetitionCount + delta).coerceAtLeast(0L)
        val completedCount = current.repetitionSets.sumOf { it.repetitions }
        updateAndPersist(
            current.copy(
                currentSetRepetitionCount = nextCurrentSetCount,
                repetitionCount = completedCount + nextCurrentSetCount,
                errorMessage = null,
            )
        )
    }

    fun endRepetitionSet() {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING ||
            current.recordingKind != ActivityRecordingKind.REPETITION ||
            current.currentSetRepetitionCount <= 0L
        ) {
            return
        }
        val now = Instant.now()
        val activeMillis = Duration.between(current.currentSetStartedAt ?: current.startTime ?: now, now)
            .toMillis()
            .coerceAtLeast(1L)
        val completedSet = ActivityRecordedRepetitionSet(
            repetitions = current.currentSetRepetitionCount,
            restSeconds = current.repetitionRestSeconds,
            activeMillis = activeMillis,
        )
        val nextState = if (current.repetitionRestSeconds > 0L) {
            current.copy(
                status = ActivityRecordingStatus.RESTING,
                repetitionSets = current.repetitionSets + completedSet,
                currentSetRepetitionCount = 0L,
                restStartedAt = now,
                currentSetStartedAt = null,
                errorMessage = null,
            )
        } else {
            current.copy(
                repetitionSets = current.repetitionSets + completedSet,
                currentSetRepetitionCount = 0L,
                currentSetStartedAt = now,
                errorMessage = null,
            )
        }
        updateAndPersist(nextState)
    }

    fun startNextRepetitionSet() {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RESTING ||
            current.recordingKind != ActivityRecordingKind.REPETITION
        ) {
            return
        }
        startNextRepetitionSet(current, Instant.now())
    }

    fun reportRecordingError(message: String) {
        updateAndPersist(_state.value.copy(errorMessage = message))
    }

    fun reportGpsDisabled() {
        val current = _state.value
        if (!current.isActive || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        updateAndPersist(current.copy(gpsStatus = ActivityGpsStatus.DISABLED))
    }

    fun reportGpsLost() {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        updateAndPersist(current.copy(gpsStatus = ActivityGpsStatus.LOST))
    }

    fun acceptBarometerPressure(pressureHpa: Float) {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        if (!preferencesRepository.activityRecordingPreferences().barometerClimbEnabled) return

        val altitudeMeters = android.hardware.SensorManager.getAltitude(
            android.hardware.SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
            pressureHpa,
        ).toDouble()
        val smoothedAltitude = current.lastBarometerAltitudeMeters?.let { previous ->
            previous + ((altitudeMeters - previous) * BarometerSmoothingAlpha)
        } ?: altitudeMeters
        val previousAltitude = current.lastBarometerAltitudeMeters
        if (previousAltitude == null) {
            updateAndPersist(
                current.copy(
                    hasBarometerElevation = true,
                    lastBarometerAltitudeMeters = smoothedAltitude,
                )
            )
            return
        }

        val delta = smoothedAltitude - previousAltitude
        val gainedMeters = if (delta >= MinBarometerElevationStepMeters) delta else 0.0
        val lostMeters = if (delta <= -MinBarometerElevationStepMeters) -delta else 0.0
        updateAndPersist(
            current.copy(
                hasBarometerElevation = true,
                barometerElevationGainedMeters = current.barometerElevationGainedMeters + gainedMeters,
                barometerElevationLostMeters = current.barometerElevationLostMeters + lostMeters,
                lastBarometerAltitudeMeters = if (gainedMeters > 0.0 || lostMeters > 0.0) {
                    smoothedAltitude
                } else {
                    previousAltitude
                },
            )
        )
    }

    private fun updateGpsStatus(location: Location) {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING || current.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        val recordingPreferences = preferencesRepository.activityRecordingPreferences()
        val fixQuality = location.activityGpsFixQuality(
            startTime = current.startTime,
            requiredAccuracyMeters = recordingPreferences.requiredGpsAccuracyMeters.toDouble(),
        )
        val gpsStatus = when {
            fixQuality.isPrecise -> ActivityGpsStatus.FIX
            fixQuality.accuracyMeters == null -> ActivityGpsStatus.WAITING_FOR_FIX
            else -> ActivityGpsStatus.POOR_ACCURACY
        }
        updateAndPersist(
            current.copy(
                gpsStatus = gpsStatus,
                latestUiPoint = fixQuality.locationTime?.let { location.toRoutePoint(it) } ?: current.latestUiPoint,
                keepScreenOnDuringRecording = recordingPreferences.keepScreenOnDuringRecording,
                autoIdleEnabled = recordingPreferences.autoIdleEnabled,
                autoIdleTimeoutMillis = recordingPreferences.autoIdleTimeoutSeconds * 1_000L,
                lastAccuracyMeters = fixQuality.accuracyMeters ?: current.lastAccuracyMeters,
                lastLocationTime = fixQuality.locationTime ?: current.lastLocationTime,
            )
        )
    }

    private fun clearRecording() {
        persistenceScope.coroutineContext.cancelChildren()
        restCompletionJob?.cancel()
        recordingGeneration += 1
        _state.value = ActivityRecordingState()
        recordingStore.clear()
    }

    private fun updateAndPersist(
        state: ActivityRecordingState,
        routePointToAppend: ExerciseRoutePoint? = null,
        replaceRoutePoints: Boolean = false,
    ) {
        _state.value = state
        recordingStore.storeMetadata(state)
        scheduleRestCompletion(state)
        if (replaceRoutePoints) {
            persistenceScope.launch {
                recordingStore.replaceRoutePoints(state.points)
            }
        }
        routePointToAppend?.let { point ->
            persistenceScope.launch {
                recordingStore.appendRoutePoint(point)
            }
        }
    }

    private fun scheduleRestCompletion(state: ActivityRecordingState) {
        restCompletionJob?.cancel()
        restCompletionJob = null
        val restEnd = state.restEndTime() ?: return
        val now = Instant.now()
        val delayMillis = Duration.between(now, restEnd).toMillis().coerceAtLeast(0L)
        restCompletionJob = persistenceScope.launch {
            delay(delayMillis)
            val current = _state.value
            if (current.status == ActivityRecordingStatus.RESTING &&
                current.restStartedAt == state.restStartedAt
            ) {
                restCompletionJob = null
                playRestTimerBellIfEnabled()
                startNextRepetitionSet(current, Instant.now())
            }
        }
    }

    private fun playRestTimerBellIfEnabled() {
        if (!preferencesRepository.activityRecordingPreferences().restTimerBellEnabled) return
        val player = runCatching { MediaPlayer.create(context, R.raw.bowl_struck) }.getOrNull()
            ?: return
        player.setOnCompletionListener { completedPlayer ->
            completedPlayer.release()
        }
        player.setOnErrorListener { errorPlayer, _, _ ->
            errorPlayer.release()
            true
        }
        runCatching {
            player.setVolume(RestTimerBellVolume, RestTimerBellVolume)
            player.start()
        }.onFailure {
            player.release()
        }
    }

    private fun startNextRepetitionSet(state: ActivityRecordingState, now: Instant) {
        val actualRestMillis = state.openRestMillis(now)
        val updatedSets = state.repetitionSets.withLastRestSeconds((actualRestMillis / 1_000L).coerceAtLeast(0L))
        val nextState = state.copy(
            status = ActivityRecordingStatus.RECORDING,
            repetitionSets = updatedSets,
            accumulatedRestMillis = state.accumulatedRestMillis + actualRestMillis,
            restStartedAt = null,
            currentSetStartedAt = now,
            errorMessage = null,
        )
        updateAndPersist(nextState)
    }

    private fun stopRecordingService() {
        context.stopService(Intent(context, ActivityRecordingService::class.java))
    }

    @SuppressLint("NewApi")
    private fun Location.withMslAltitude(): Location {
        val converted = Location(this)
        val converter = altitudeConverter ?: return converted
        if (!converted.hasAltitude()) return converted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching {
                converter.addMslAltitudeToLocation(context, converted)
            }
        }
        return converted
    }

    companion object {
        fun hasPreciseLocationPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

        fun hasLocationPermission(context: Context): Boolean =
            hasPreciseLocationPermission(context) ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED

        fun hasNotificationPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED

        fun hasActivityRecognitionPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION,
                ) == PackageManager.PERMISSION_GRANTED
    }
}

@Singleton
class ActivityRecordingStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(RecordingPreferencesName, Context.MODE_PRIVATE)
    private val routePointsFile = File(context.filesDir, RecordingRoutePointsFileName)

    fun restore(): ActivityRecordingState {
        val restored = preferences.restoreRecordingState()
        val filePoints = routePointsFile
            .takeIf { it.exists() }
            ?.readText()
            .orEmpty()
            .decodeRoutePoints()
        return if (filePoints.isNotEmpty()) {
            restored.copy(points = filePoints)
        } else {
            restored
        }
    }

    fun storeMetadata(state: ActivityRecordingState) {
        if (state.status == ActivityRecordingStatus.IDLE) {
            clear()
        } else {
            preferences.storeRecordingMetadata(state)
        }
    }

    fun replaceRoutePoints(points: List<ExerciseRoutePoint>) {
        if (points.isEmpty()) {
            routePointsFile.delete()
        } else {
            routePointsFile.writeText(points.encodeRoutePoints())
        }
        preferences.edit {
            putString(KeyPoints, points.encodeRoutePoints())
        }
    }

    fun appendRoutePoint(point: ExerciseRoutePoint) {
        routePointsFile.parentFile?.mkdirs()
        routePointsFile.appendText(point.encodeRoutePoint() + "\n")
    }

    fun clear() {
        preferences.edit {
            clear()
        }
        routePointsFile.delete()
    }
}

private fun ActivityEntryType.recordingKind(): ActivityRecordingKind =
    when {
        supportsGpsRoute -> ActivityRecordingKind.GPS_ROUTE
        isRepetitionLike -> ActivityRecordingKind.REPETITION
        else -> ActivityRecordingKind.TIMED
    }

internal const val RecordingPreferencesName = "activity_recording"
internal const val RecordingRoutePointsFileName = "activity_recording_points.csv"
internal const val MaxLocationAgeMillis = 10_000L
internal const val MaxLocationFutureSkewSeconds = 5L
internal const val MaxPlausibleSpeedMetersPerSecond = 55.0
internal const val MinSampleIntervalMillis = 500L
internal const val MinElevationGainIncrementMeters = 1.0
internal const val BarometerSmoothingAlpha = 0.3
internal const val MinBarometerElevationStepMeters = 3.0
internal const val RestTimerBellVolume = 0.42f
internal const val MissingInt = Int.MIN_VALUE
internal const val MissingLong = Long.MIN_VALUE
internal const val MissingFloat = -1f
internal const val KeyStatus = "status"
internal const val KeyRecordingKind = "recording_kind"
internal const val KeyActivityTypeId = "activity_type_id"
internal const val KeyExerciseType = "exercise_type"
internal const val KeyStartTime = "start_time"
internal const val KeyEndTime = "end_time"
internal const val KeyPausedStartedAt = "paused_started_at"
internal const val KeyTotalPausedMillis = "total_paused_millis"
internal const val KeyPauseIntervals = "pause_intervals"
internal const val KeyPoints = "points"
internal const val KeyRouteBreakIndexes = "route_break_indexes"
internal const val KeyManualLaps = "manual_laps"
internal const val KeyMarkers = "markers"
internal const val KeyDistanceMeters = "distance_meters"
internal const val KeyElevationMeters = "elevation_meters"
internal const val KeyElevationLostMeters = "elevation_lost_meters"
internal const val KeyBarometerElevationGainedMeters = "barometer_elevation_gained_meters"
internal const val KeyBarometerElevationLostMeters = "barometer_elevation_lost_meters"
internal const val KeyHasBarometerElevation = "has_barometer_elevation"
internal const val KeyLastBarometerAltitudeMeters = "last_barometer_altitude_meters"
internal const val KeyCurrentSpeedMetersPerSecond = "current_speed_meters_per_second"
internal const val KeyMaxSpeedMetersPerSecond = "max_speed_meters_per_second"
internal const val KeyGpsStatus = "gps_status"
internal const val KeyKeepScreenOnDuringRecording = "keep_screen_on_during_recording"
internal const val KeyAutoIdleEnabled = "auto_idle_enabled"
internal const val KeyAutoIdleTimeoutMillis = "auto_idle_timeout_millis"
internal const val KeyLastMovementAt = "last_movement_at"
internal const val KeyTotalIdleMillis = "total_idle_millis"
internal const val KeyRepetitionCount = "repetition_count"
internal const val KeyCurrentSetRepetitionCount = "current_set_repetition_count"
internal const val KeyRepetitionSets = "repetition_sets"
internal const val KeyRepetitionRestSeconds = "repetition_rest_seconds"
internal const val KeyCurrentSetStartedAt = "current_set_started_at"
internal const val KeyRestStartedAt = "rest_started_at"
internal const val KeyAccumulatedRestMillis = "accumulated_rest_millis"
internal const val KeyLastAccuracyMeters = "last_accuracy_meters"
internal const val KeyLastLocationTime = "last_location_time"
internal const val KeyDroppedPointCount = "dropped_point_count"
internal const val KeyErrorMessage = "error_message"
internal const val KeyDashboardTemplate = "dashboard_template"
internal const val KeyDashboardFields = "dashboard_fields"
