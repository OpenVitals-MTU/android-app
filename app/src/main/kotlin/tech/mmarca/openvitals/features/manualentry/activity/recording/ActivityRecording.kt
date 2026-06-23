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
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.health.connect.client.records.ExerciseSessionRecord
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
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ActivityRecordingLap
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarkerType
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences

enum class ActivityRecordingStatus {
    IDLE,
    RECORDING,
    RESTING,
    PAUSED,
}

enum class ActivityRecordingKind {
    GPS_ROUTE,
    REPETITION,
}

enum class ActivityGpsStatus {
    WAITING_FOR_FIX,
    FIX,
    POOR_ACCURACY,
    LOST,
    DISABLED,
}

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
) {
    val isActive: Boolean
        get() = status == ActivityRecordingStatus.RECORDING ||
            status == ActivityRecordingStatus.RESTING ||
            status == ActivityRecordingStatus.PAUSED
}

data class ActivityRecordedRepetitionSet(
    val repetitions: Long,
    val restSeconds: Long,
    val activeMillis: Long,
)

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
)

@Singleton
class ActivityRecordingController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val recordingStore: ActivityRecordingStore = ActivityRecordingStore(context),
) {
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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

    private fun startGpsRecording(activityType: ActivityEntryType, initialFix: Location?): Boolean {
        val recordingPreferences = preferencesRepository.activityRecordingPreferences()
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
                autoIdleEnabled = recordingPreferences.autoIdleEnabled,
                autoIdleTimeoutMillis = recordingPreferences.autoIdleTimeoutSeconds * 1_000L,
                lastMovementAt = now,
                lastAccuracyMeters = initialFixQuality.accuracyMeters,
                lastLocationTime = now,
            ),
            replaceRoutePoints = true,
        )
        acceptLocation(Location(lockedFix).apply { time = now.toEpochMilli() })
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
                currentSetStartedAt = now,
                repetitionRestSeconds = repetitionRestSeconds.coerceAtLeast(0L),
            ),
            replaceRoutePoints = true,
        )
        ContextCompat.startForegroundService(
            context,
            ActivityRecordingService.intent(context, ActivityRecordingService.ActionStart),
        )
        return true
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
            val distanceMeters = lastPoint.distanceMetersTo(point)
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
        if (current.status != ActivityRecordingStatus.RECORDING ||
            current.recordingKind != ActivityRecordingKind.REPETITION
        ) {
            return
        }
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
        val restEnd = state.restEndTime() ?: return
        val now = Instant.now()
        val delayMillis = Duration.between(now, restEnd).toMillis().coerceAtLeast(0L)
        restCompletionJob = persistenceScope.launch {
            delay(delayMillis)
            val current = _state.value
            if (current.status == ActivityRecordingStatus.RESTING &&
                current.restStartedAt == state.restStartedAt
            ) {
                startNextRepetitionSet(current, Instant.now())
            }
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
        preferences.edit().putString(KeyPoints, points.encodeRoutePoints()).apply()
    }

    fun appendRoutePoint(point: ExerciseRoutePoint) {
        routePointsFile.parentFile?.mkdirs()
        routePointsFile.appendText(point.encodeRoutePoint() + "\n")
    }

    fun clear() {
        preferences.edit().clear().apply()
        routePointsFile.delete()
    }
}

fun ActivityRecordingState.elapsedDuration(now: Instant = Instant.now()): Duration {
    val start = startTime ?: return Duration.ZERO
    val effectiveEnd = endTime
        ?: if (status == ActivityRecordingStatus.PAUSED) pausedStartedAt ?: now else now
    return Duration.ofMillis((effectiveEnd.toEpochMilli() - start.toEpochMilli()).coerceAtLeast(0L))
}

fun ActivityRecordingState.restDuration(now: Instant = Instant.now()): Duration =
    Duration.ofMillis((accumulatedRestMillis + openRestMillis(now)).coerceAtLeast(0L))

fun ActivityRecordingState.movingDuration(now: Instant = Instant.now()): Duration {
    val elapsedMillis = elapsedDuration(now).toMillis()
    val openPauseMillis = pausedStartedAt
        ?.takeIf { status == ActivityRecordingStatus.PAUSED }
        ?.let { Duration.between(it, now).toMillis().coerceAtLeast(0L) }
        ?: 0L
    val pausedMillis = totalPausedMillis + openPauseMillis
    val idleMillis = totalIdleMillis + openIdleMillis(now)
    val restMillis = restDuration(now).toMillis().takeIf {
        recordingKind == ActivityRecordingKind.REPETITION
    } ?: 0L
    return Duration.ofMillis((elapsedMillis - pausedMillis - idleMillis - restMillis).coerceAtLeast(0L))
}

fun ActivityRecordingState.restRemainingDuration(now: Instant = Instant.now()): Duration {
    val restEnd = restEndTime() ?: return Duration.ZERO
    return Duration.ofMillis(Duration.between(now, restEnd).toMillis().coerceAtLeast(0L))
}

private fun ActivityRecordingState.restEndTime(): Instant? =
    restStartedAt
        ?.takeIf { status == ActivityRecordingStatus.RESTING && repetitionRestSeconds > 0L }
        ?.plusSeconds(repetitionRestSeconds)

private fun ActivityRecordingState.openRestMillis(now: Instant): Long =
    restStartedAt
        ?.takeIf { status == ActivityRecordingStatus.RESTING }
        ?.let { Duration.between(it, now).toMillis().coerceAtLeast(0L) }
        ?: 0L

private fun ActivityRecordingState.recordedRepetitionSets(end: Instant): List<ActivityRecordedRepetitionSet> {
    val sets = if (status == ActivityRecordingStatus.RESTING) {
        repetitionSets.withLastRestSeconds((openRestMillis(end) / 1_000L).coerceAtLeast(0L))
    } else {
        repetitionSets
    }
    if (status != ActivityRecordingStatus.RECORDING || currentSetRepetitionCount <= 0L) return sets
    val activeMillis = Duration.between(currentSetStartedAt ?: startTime ?: end, end)
        .toMillis()
        .coerceAtLeast(1L)
    return sets + ActivityRecordedRepetitionSet(
        repetitions = currentSetRepetitionCount,
        restSeconds = 0L,
        activeMillis = activeMillis,
    )
}

private fun List<ActivityRecordedRepetitionSet>.withLastRestSeconds(restSeconds: Long): List<ActivityRecordedRepetitionSet> =
    if (isEmpty()) {
        this
    } else {
        dropLast(1) + last().copy(restSeconds = restSeconds.coerceAtLeast(0L))
    }

fun ActivityRecordingState.displayElevationGainedMeters(): Double =
    if (hasBarometerElevation) barometerElevationGainedMeters else elevationGainedMeters

fun ActivityRecordingState.closedManualLaps(endTime: Instant): List<ActivityRecordingLap> {
    if (manualLaps.isEmpty()) return emptyList()
    val openStart = manualLaps.maxByOrNull { it.endTime }?.endTime ?: startTime ?: return manualLaps
    val finalLap = if (openStart.isBefore(endTime)) {
        ActivityRecordingLap(
            startTime = openStart,
            endTime = endTime,
            distanceMeters = activityRecordingRouteDistanceMeters(
                points = points,
                routeBreakIndexes = routeBreakIndexes,
                startTime = openStart,
                endTime = endTime,
            ).takeIf { it > 0.0 },
        )
    } else {
        null
    }
    return manualLaps + listOfNotNull(finalLap)
}

fun ActivityRecordingState.effectiveCurrentSpeedMetersPerSecond(now: Instant = Instant.now()): Double =
    if (
        status != ActivityRecordingStatus.RECORDING ||
        isAutoIdle(now) ||
        gpsStatus == ActivityGpsStatus.POOR_ACCURACY ||
        gpsStatus == ActivityGpsStatus.LOST ||
        gpsStatus == ActivityGpsStatus.DISABLED
    ) {
        0.0
    } else {
        currentSpeedMetersPerSecond
    }

fun ActivityRecordingState.isAutoIdle(now: Instant = Instant.now()): Boolean =
    status == ActivityRecordingStatus.RECORDING &&
        autoIdleEnabled &&
        lastMovementAt?.plusMillis(autoIdleTimeoutMillis)?.let { !now.isBefore(it) } == true

private fun ActivityRecordingState.openIdleMillis(now: Instant): Long {
    if (status != ActivityRecordingStatus.RECORDING || !autoIdleEnabled) return 0L
    val movementAt = lastMovementAt ?: return 0L
    val idleStartedAt = movementAt.plusMillis(autoIdleTimeoutMillis)
    return if (now.isAfter(idleStartedAt)) {
        Duration.between(idleStartedAt, now).toMillis().coerceAtLeast(0L)
    } else {
        0L
    }
}

private fun Location.toRoutePoint(timeOverride: Instant? = null): ExerciseRoutePoint =
    ExerciseRoutePoint(
        time = timeOverride ?: Instant.ofEpochMilli(time.takeIf { it > 0L } ?: System.currentTimeMillis()),
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = mslAltitudeMetersOrNull() ?: if (hasAltitude()) altitude else null,
        horizontalAccuracyMeters = if (hasAccuracy()) accuracy.toDouble() else null,
        verticalAccuracyMeters = mslAltitudeAccuracyMetersOrNull()
            ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasVerticalAccuracy()) {
                verticalAccuracyMeters.toDouble()
            } else {
                null
            },
    )

private fun Location.mslAltitudeMetersOrNull(): Double? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && hasMslAltitude()) {
        mslAltitudeMeters
    } else {
        null
    }

private fun Location.mslAltitudeAccuracyMetersOrNull(): Double? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && hasMslAltitudeAccuracy()) {
        mslAltitudeAccuracyMeters.toDouble()
    } else {
        null
    }

data class ActivityGpsFixQuality(
    val isPrecise: Boolean,
    val accuracyMeters: Double?,
    val locationTime: Instant?,
)

fun Location.activityGpsFixQuality(
    startTime: Instant? = null,
    now: Instant = Instant.now(),
    requiredAccuracyMeters: Double = ActivityRecordingPreferences.DefaultRequiredGpsAccuracyMeters.toDouble(),
): ActivityGpsFixQuality {
    val accuracy = if (hasAccuracy()) accuracy.toDouble() else null
    val locationTime = Instant.ofEpochMilli(time.takeIf { it > 0L } ?: System.currentTimeMillis())
    val isPrecise = provider == LocationManager.GPS_PROVIDER &&
        accuracy != null &&
        accuracy <= requiredAccuracyMeters &&
        locationAgeMillis() <= MaxLocationAgeMillis &&
        startTime?.let { locationTime.isAfter(it) } != false &&
        !locationTime.isAfter(now.plusSeconds(MaxLocationFutureSkewSeconds))
    return ActivityGpsFixQuality(
        isPrecise = isPrecise,
        accuracyMeters = accuracy,
        locationTime = locationTime,
    )
}

private fun Location.locationAgeMillis(): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && elapsedRealtimeNanos > 0L) {
        ((SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L)
            .coerceAtLeast(0L)
    } else {
        (System.currentTimeMillis() - time).coerceAtLeast(0L)
    }

private fun ActivityRecordingState.withDroppedLocation(
    accuracyMeters: Double?,
    locationTime: Instant? = null,
    gpsStatus: ActivityGpsStatus = this.gpsStatus,
): ActivityRecordingState =
    copy(
        gpsStatus = gpsStatus,
        lastAccuracyMeters = accuracyMeters ?: lastAccuracyMeters,
        lastLocationTime = locationTime ?: lastLocationTime,
        droppedPointCount = droppedPointCount + 1,
    )

private fun ActivityRecordingState.withLocationMetadata(
    accuracyMeters: Double?,
    locationTime: Instant,
    gpsStatus: ActivityGpsStatus = this.gpsStatus,
    recordingPreferences: ActivityRecordingPreferences? = null,
): ActivityRecordingState =
    copy(
        gpsStatus = gpsStatus,
        autoIdleEnabled = recordingPreferences?.autoIdleEnabled ?: autoIdleEnabled,
        autoIdleTimeoutMillis = recordingPreferences?.autoIdleTimeoutSeconds?.times(1_000L) ?: autoIdleTimeoutMillis,
        lastAccuracyMeters = accuracyMeters ?: lastAccuracyMeters,
        lastLocationTime = locationTime,
        errorMessage = null,
    )

private fun ExerciseRoutePoint.distanceMetersTo(other: ExerciseRoutePoint): Double {
    val results = FloatArray(1)
    Location.distanceBetween(latitude, longitude, other.latitude, other.longitude, results)
    return results[0].toDouble()
}

private fun ExerciseRoutePoint.elevationGainMetersTo(other: ExerciseRoutePoint): Double {
    val startAltitude = altitudeMeters ?: return 0.0
    val endAltitude = other.altitudeMeters ?: return 0.0
    return (endAltitude - startAltitude)
        .takeIf { it >= MinElevationGainIncrementMeters }
        ?: 0.0
}

private fun ExerciseRoutePoint.elevationLossMetersTo(other: ExerciseRoutePoint): Double {
    val startAltitude = altitudeMeters ?: return 0.0
    val endAltitude = other.altitudeMeters ?: return 0.0
    return (startAltitude - endAltitude)
        .takeIf { it >= MinElevationGainIncrementMeters }
        ?: 0.0
}

private fun ActivityRecordingState.minimumSampleDistanceMeters(
    recordingPreferences: ActivityRecordingPreferences,
): Double =
    recordingPreferences.recordingDistanceIntervalMeters?.toDouble() ?: when (exerciseType) {
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_SKIING,
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING,
        ExerciseSessionRecord.EXERCISE_TYPE_SAILING -> 10.0
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
        ExerciseSessionRecord.EXERCISE_TYPE_PADDLING,
        ExerciseSessionRecord.EXERCISE_TYPE_SKATING,
        ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> 7.0
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> 4.0
        else -> 5.0
    }

private fun isImplausibleJump(
    lastPoint: ExerciseRoutePoint,
    point: ExerciseRoutePoint,
    distanceMeters: Double,
    elapsedMillis: Long,
    accuracyMeters: Double,
): Boolean {
    if (elapsedMillis <= 0L) return true
    val metersPerSecond = distanceMeters / (elapsedMillis / 1_000.0)
    val lastAccuracyMeters = lastPoint.horizontalAccuracyMeters ?: accuracyMeters
    val combinedAccuracyMeters = lastAccuracyMeters + accuracyMeters
    return metersPerSecond > MaxPlausibleSpeedMetersPerSecond &&
        distanceMeters > combinedAccuracyMeters
}

private fun SharedPreferences.restoreRecordingState(): ActivityRecordingState {
    val status = getString(KeyStatus, null)
        ?.let { runCatching { ActivityRecordingStatus.valueOf(it) }.getOrNull() }
        ?: ActivityRecordingStatus.IDLE
    if (status == ActivityRecordingStatus.IDLE) return ActivityRecordingState()

    return ActivityRecordingState(
        status = status,
        recordingKind = getString(KeyRecordingKind, null)
            ?.let { runCatching { ActivityRecordingKind.valueOf(it) }.getOrNull() }
            ?: ActivityRecordingKind.GPS_ROUTE,
        activityTypeId = getString(KeyActivityTypeId, null),
        exerciseType = getInt(KeyExerciseType, MissingInt).takeIf { it != MissingInt },
        startTime = getLong(KeyStartTime, MissingLong).toInstantOrNull(),
        endTime = getLong(KeyEndTime, MissingLong).toInstantOrNull(),
        pausedStartedAt = getLong(KeyPausedStartedAt, MissingLong).toInstantOrNull(),
        totalPausedMillis = getLong(KeyTotalPausedMillis, 0L),
        pauseIntervals = getString(KeyPauseIntervals, null).orEmpty().decodePauseIntervals(),
        points = getString(KeyPoints, null).orEmpty().decodeRoutePoints(),
        routeBreakIndexes = getString(KeyRouteBreakIndexes, null).orEmpty().decodeIntList(),
        manualLaps = getString(KeyManualLaps, null).orEmpty().decodeRecordingLaps(),
        markers = getString(KeyMarkers, null).orEmpty().decodeRecordingMarkers(),
        distanceMeters = getFloat(KeyDistanceMeters, 0f).toDouble(),
        elevationGainedMeters = getFloat(KeyElevationMeters, 0f).toDouble(),
        elevationLostMeters = getFloat(KeyElevationLostMeters, 0f).toDouble(),
        barometerElevationGainedMeters = getFloat(KeyBarometerElevationGainedMeters, 0f).toDouble(),
        barometerElevationLostMeters = getFloat(KeyBarometerElevationLostMeters, 0f).toDouble(),
        hasBarometerElevation = getBoolean(KeyHasBarometerElevation, false),
        lastBarometerAltitudeMeters = getFloat(KeyLastBarometerAltitudeMeters, MissingFloat)
            .takeIf { it != MissingFloat }
            ?.toDouble(),
        currentSpeedMetersPerSecond = getFloat(KeyCurrentSpeedMetersPerSecond, 0f).toDouble(),
        maxSpeedMetersPerSecond = getFloat(KeyMaxSpeedMetersPerSecond, 0f).toDouble(),
        gpsStatus = getString(KeyGpsStatus, null)
            ?.let { runCatching { ActivityGpsStatus.valueOf(it) }.getOrNull() }
            ?: ActivityGpsStatus.WAITING_FOR_FIX,
        autoIdleEnabled = getBoolean(
            KeyAutoIdleEnabled,
            ActivityRecordingPreferences.DefaultAutoIdleEnabled,
        ),
        autoIdleTimeoutMillis = getLong(
            KeyAutoIdleTimeoutMillis,
            ActivityRecordingPreferences.DefaultAutoIdleTimeoutSeconds * 1_000L,
        ),
        lastMovementAt = getLong(KeyLastMovementAt, MissingLong).toInstantOrNull(),
        totalIdleMillis = getLong(KeyTotalIdleMillis, 0L),
        repetitionCount = getLong(KeyRepetitionCount, 0L),
        currentSetRepetitionCount = getLong(KeyCurrentSetRepetitionCount, 0L),
        repetitionSets = getString(KeyRepetitionSets, null).orEmpty().decodeRecordedRepetitionSets(),
        repetitionRestSeconds = getLong(KeyRepetitionRestSeconds, 0L),
        currentSetStartedAt = getLong(KeyCurrentSetStartedAt, MissingLong).toInstantOrNull(),
        restStartedAt = getLong(KeyRestStartedAt, MissingLong).toInstantOrNull(),
        accumulatedRestMillis = getLong(KeyAccumulatedRestMillis, 0L),
        lastAccuracyMeters = getFloat(KeyLastAccuracyMeters, MissingFloat)
            .takeIf { it != MissingFloat }
            ?.toDouble(),
        lastLocationTime = getLong(KeyLastLocationTime, MissingLong).toInstantOrNull(),
        droppedPointCount = getInt(KeyDroppedPointCount, 0),
        errorMessage = getString(KeyErrorMessage, null),
    )
}

private fun SharedPreferences.storeRecordingMetadata(state: ActivityRecordingState) {
    edit()
        .putString(KeyStatus, state.status.name)
        .putString(KeyRecordingKind, state.recordingKind.name)
        .putString(KeyActivityTypeId, state.activityTypeId)
        .putInt(KeyExerciseType, state.exerciseType ?: MissingInt)
        .putLong(KeyStartTime, state.startTime?.toEpochMilli() ?: MissingLong)
        .putLong(KeyEndTime, state.endTime?.toEpochMilli() ?: MissingLong)
        .putLong(KeyPausedStartedAt, state.pausedStartedAt?.toEpochMilli() ?: MissingLong)
        .putLong(KeyTotalPausedMillis, state.totalPausedMillis)
        .putString(KeyPauseIntervals, state.pauseIntervals.encodePauseIntervals())
        .putString(KeyRouteBreakIndexes, state.routeBreakIndexes.encodeIntList())
        .putString(KeyManualLaps, state.manualLaps.encodeRecordingLaps())
        .putString(KeyMarkers, state.markers.encodeRecordingMarkers())
        .putFloat(KeyDistanceMeters, state.distanceMeters.toFloat())
        .putFloat(KeyElevationMeters, state.elevationGainedMeters.toFloat())
        .putFloat(KeyElevationLostMeters, state.elevationLostMeters.toFloat())
        .putFloat(KeyBarometerElevationGainedMeters, state.barometerElevationGainedMeters.toFloat())
        .putFloat(KeyBarometerElevationLostMeters, state.barometerElevationLostMeters.toFloat())
        .putBoolean(KeyHasBarometerElevation, state.hasBarometerElevation)
        .putFloat(KeyLastBarometerAltitudeMeters, state.lastBarometerAltitudeMeters?.toFloat() ?: MissingFloat)
        .putFloat(KeyCurrentSpeedMetersPerSecond, state.currentSpeedMetersPerSecond.toFloat())
        .putFloat(KeyMaxSpeedMetersPerSecond, state.maxSpeedMetersPerSecond.toFloat())
        .putString(KeyGpsStatus, state.gpsStatus.name)
        .putBoolean(KeyAutoIdleEnabled, state.autoIdleEnabled)
        .putLong(KeyAutoIdleTimeoutMillis, state.autoIdleTimeoutMillis)
        .putLong(KeyLastMovementAt, state.lastMovementAt?.toEpochMilli() ?: MissingLong)
        .putLong(KeyTotalIdleMillis, state.totalIdleMillis)
        .putLong(KeyRepetitionCount, state.repetitionCount)
        .putLong(KeyCurrentSetRepetitionCount, state.currentSetRepetitionCount)
        .putString(KeyRepetitionSets, state.repetitionSets.encodeRecordedRepetitionSets())
        .putLong(KeyRepetitionRestSeconds, state.repetitionRestSeconds)
        .putLong(KeyCurrentSetStartedAt, state.currentSetStartedAt?.toEpochMilli() ?: MissingLong)
        .putLong(KeyRestStartedAt, state.restStartedAt?.toEpochMilli() ?: MissingLong)
        .putLong(KeyAccumulatedRestMillis, state.accumulatedRestMillis)
        .putFloat(KeyLastAccuracyMeters, state.lastAccuracyMeters?.toFloat() ?: MissingFloat)
        .putLong(KeyLastLocationTime, state.lastLocationTime?.toEpochMilli() ?: MissingLong)
        .putInt(KeyDroppedPointCount, state.droppedPointCount)
        .putString(KeyErrorMessage, state.errorMessage)
        .apply()
}

private fun Instant.toPauseInterval(endTime: Instant): ActivityPauseInterval? =
    takeIf { it.isBefore(endTime) }?.let { startTime ->
        ActivityPauseInterval(startTime = startTime, endTime = endTime)
    }

private fun List<ActivityPauseInterval>.encodePauseIntervals(): String =
    joinToString(separator = "\n") { interval ->
        "${interval.startTime.toEpochMilli()},${interval.endTime.toEpochMilli()}"
    }

private fun String.decodePauseIntervals(): List<ActivityPauseInterval> =
    lineSequence()
        .mapNotNull { line ->
            val parts = line.split(',')
            if (parts.size < 2) return@mapNotNull null
            val startTime = parts[0].toLongOrNull()?.let(Instant::ofEpochMilli) ?: return@mapNotNull null
            val endTime = parts[1].toLongOrNull()?.let(Instant::ofEpochMilli) ?: return@mapNotNull null
            ActivityPauseInterval(startTime = startTime, endTime = endTime)
                .takeIf { it.startTime.isBefore(it.endTime) }
        }
        .toList()

private fun List<ActivityRecordedRepetitionSet>.encodeRecordedRepetitionSets(): String =
    joinToString(separator = "\n") { set ->
        listOf(
            set.repetitions.toString(),
            set.restSeconds.toString(),
            set.activeMillis.toString(),
        ).joinToString(separator = ",")
    }

private fun String.decodeRecordedRepetitionSets(): List<ActivityRecordedRepetitionSet> =
    lineSequence()
        .mapNotNull { line ->
            val parts = line.split(',')
            if (parts.size < 3) return@mapNotNull null
            ActivityRecordedRepetitionSet(
                repetitions = parts[0].toLongOrNull()?.coerceAtLeast(0L) ?: return@mapNotNull null,
                restSeconds = parts[1].toLongOrNull()?.coerceAtLeast(0L) ?: return@mapNotNull null,
                activeMillis = parts[2].toLongOrNull()?.coerceAtLeast(1L) ?: return@mapNotNull null,
            )
        }
        .toList()

private fun List<ActivityRecordingLap>.encodeRecordingLaps(): String =
    joinToString(separator = "\n") { lap ->
        listOf(
            lap.startTime.toEpochMilli().toString(),
            lap.endTime.toEpochMilli().toString(),
            lap.distanceMeters?.toString().orEmpty(),
        ).joinToString(separator = ",")
    }

private fun String.decodeRecordingLaps(): List<ActivityRecordingLap> =
    lineSequence()
        .mapNotNull { line ->
            val parts = line.split(',')
            if (parts.size < 3) return@mapNotNull null
            val startTime = parts[0].toLongOrNull()?.let(Instant::ofEpochMilli) ?: return@mapNotNull null
            val endTime = parts[1].toLongOrNull()?.let(Instant::ofEpochMilli) ?: return@mapNotNull null
            ActivityRecordingLap(
                startTime = startTime,
                endTime = endTime,
                distanceMeters = parts[2].toDoubleOrNull(),
            ).takeIf { it.startTime.isBefore(it.endTime) }
        }
        .toList()

private fun List<ActivityRecordingMarker>.encodeRecordingMarkers(): String =
    joinToString(separator = "\n") { marker ->
        listOf(
            marker.id,
            marker.time.toEpochMilli().toString(),
            marker.latitude.toString(),
            marker.longitude.toString(),
            marker.altitudeMeters?.toString().orEmpty(),
            marker.name.encodeCompactText(),
            marker.note.encodeCompactText(),
            marker.type.encodeCompactText(),
        ).joinToString(separator = ",")
    }

private fun String.decodeRecordingMarkers(): List<ActivityRecordingMarker> =
    lineSequence()
        .mapNotNull { line ->
            val parts = line.split(',')
            if (parts.size < 8) return@mapNotNull null
            ActivityRecordingMarker(
                id = parts[0].takeIf { it.isNotBlank() } ?: return@mapNotNull null,
                time = parts[1].toLongOrNull()?.let(Instant::ofEpochMilli) ?: return@mapNotNull null,
                latitude = parts[2].toDoubleOrNull() ?: return@mapNotNull null,
                longitude = parts[3].toDoubleOrNull() ?: return@mapNotNull null,
                altitudeMeters = parts[4].toDoubleOrNull(),
                name = parts[5].decodeCompactText().ifBlank { "Marker" },
                note = parts[6].decodeCompactText(),
                type = parts[7].decodeCompactText().ifBlank { ActivityRecordingMarkerType.Generic.value },
            )
        }
        .toList()

private fun String.encodeCompactText(): String =
    Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(toByteArray(StandardCharsets.UTF_8))

private fun String.decodeCompactText(): String =
    runCatching {
        String(Base64.getUrlDecoder().decode(this), StandardCharsets.UTF_8)
    }.getOrDefault("")

private fun List<Int>.encodeIntList(): String =
    joinToString(separator = ",")

private fun String.decodeIntList(): List<Int> =
    split(',')
        .mapNotNull { value -> value.toIntOrNull()?.takeIf { it > 0 } }

private fun List<ExerciseRoutePoint>.encodeRoutePoints(): String =
    joinToString(separator = "\n") { point -> point.encodeRoutePoint() }

private fun ExerciseRoutePoint.encodeRoutePoint(): String =
    listOf(
        time.toEpochMilli().toString(),
        latitude.toString(),
        longitude.toString(),
        altitudeMeters?.toString().orEmpty(),
        horizontalAccuracyMeters?.toString().orEmpty(),
        verticalAccuracyMeters?.toString().orEmpty(),
    ).joinToString(separator = ",")

private fun String.decodeRoutePoints(): List<ExerciseRoutePoint> =
    lineSequence()
        .mapNotNull { line ->
            val parts = line.split(',')
            if (parts.size < 6) return@mapNotNull null
            val time = parts[0].toLongOrNull()?.let(Instant::ofEpochMilli) ?: return@mapNotNull null
            val latitude = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            val longitude = parts[2].toDoubleOrNull() ?: return@mapNotNull null
            ExerciseRoutePoint(
                time = time,
                latitude = latitude,
                longitude = longitude,
                altitudeMeters = parts[3].toDoubleOrNull(),
                horizontalAccuracyMeters = parts[4].toDoubleOrNull(),
                verticalAccuracyMeters = parts[5].toDoubleOrNull(),
            )
        }
        .toList()

private fun Long.toInstantOrNull(): Instant? =
    takeIf { it != MissingLong }?.let(Instant::ofEpochMilli)

private const val RecordingPreferencesName = "activity_recording"
private const val RecordingRoutePointsFileName = "activity_recording_points.csv"
private const val MaxLocationAgeMillis = 10_000L
private const val MaxLocationFutureSkewSeconds = 5L
private const val MaxPlausibleSpeedMetersPerSecond = 55.0
private const val MinSampleIntervalMillis = 500L
private const val MinElevationGainIncrementMeters = 1.0
private const val BarometerSmoothingAlpha = 0.3
private const val MinBarometerElevationStepMeters = 3.0
private const val MissingInt = Int.MIN_VALUE
private const val MissingLong = Long.MIN_VALUE
private const val MissingFloat = -1f
private const val KeyStatus = "status"
private const val KeyRecordingKind = "recording_kind"
private const val KeyActivityTypeId = "activity_type_id"
private const val KeyExerciseType = "exercise_type"
private const val KeyStartTime = "start_time"
private const val KeyEndTime = "end_time"
private const val KeyPausedStartedAt = "paused_started_at"
private const val KeyTotalPausedMillis = "total_paused_millis"
private const val KeyPauseIntervals = "pause_intervals"
private const val KeyPoints = "points"
private const val KeyRouteBreakIndexes = "route_break_indexes"
private const val KeyManualLaps = "manual_laps"
private const val KeyMarkers = "markers"
private const val KeyDistanceMeters = "distance_meters"
private const val KeyElevationMeters = "elevation_meters"
private const val KeyElevationLostMeters = "elevation_lost_meters"
private const val KeyBarometerElevationGainedMeters = "barometer_elevation_gained_meters"
private const val KeyBarometerElevationLostMeters = "barometer_elevation_lost_meters"
private const val KeyHasBarometerElevation = "has_barometer_elevation"
private const val KeyLastBarometerAltitudeMeters = "last_barometer_altitude_meters"
private const val KeyCurrentSpeedMetersPerSecond = "current_speed_meters_per_second"
private const val KeyMaxSpeedMetersPerSecond = "max_speed_meters_per_second"
private const val KeyGpsStatus = "gps_status"
private const val KeyAutoIdleEnabled = "auto_idle_enabled"
private const val KeyAutoIdleTimeoutMillis = "auto_idle_timeout_millis"
private const val KeyLastMovementAt = "last_movement_at"
private const val KeyTotalIdleMillis = "total_idle_millis"
private const val KeyRepetitionCount = "repetition_count"
private const val KeyCurrentSetRepetitionCount = "current_set_repetition_count"
private const val KeyRepetitionSets = "repetition_sets"
private const val KeyRepetitionRestSeconds = "repetition_rest_seconds"
private const val KeyCurrentSetStartedAt = "current_set_started_at"
private const val KeyRestStartedAt = "rest_started_at"
private const val KeyAccumulatedRestMillis = "accumulated_rest_millis"
private const val KeyLastAccuracyMeters = "last_accuracy_meters"
private const val KeyLastLocationTime = "last_location_time"
private const val KeyDroppedPointCount = "dropped_point_count"
private const val KeyErrorMessage = "error_message"
