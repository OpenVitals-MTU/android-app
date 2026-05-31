package tech.mmarca.openvitals.features.manualentry

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.health.connect.client.records.ExerciseSessionRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.data.model.ActivityPauseInterval
import tech.mmarca.openvitals.data.model.ExerciseRoutePoint

enum class ActivityRecordingStatus {
    IDLE,
    RECORDING,
    PAUSED,
}

data class ActivityRecordingState(
    val status: ActivityRecordingStatus = ActivityRecordingStatus.IDLE,
    val exerciseType: Int? = null,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val pausedStartedAt: Instant? = null,
    val totalPausedMillis: Long = 0L,
    val pauseIntervals: List<ActivityPauseInterval> = emptyList(),
    val points: List<ExerciseRoutePoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val elevationGainedMeters: Double = 0.0,
    val lastAccuracyMeters: Double? = null,
    val lastLocationTime: Instant? = null,
    val droppedPointCount: Int = 0,
    val errorMessage: String? = null,
) {
    val isActive: Boolean
        get() = status == ActivityRecordingStatus.RECORDING || status == ActivityRecordingStatus.PAUSED
}

data class ActivityRecordingSnapshot(
    val exerciseType: Int,
    val startTime: Instant,
    val endTime: Instant,
    val points: List<ExerciseRoutePoint>,
    val pauseIntervals: List<ActivityPauseInterval>,
    val distanceMeters: Double,
    val elevationGainedMeters: Double,
)

@Singleton
class ActivityRecordingController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val recordingStore: ActivityRecordingStore = ActivityRecordingStore(context),
) {
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(recordingStore.restore())
    val state: StateFlow<ActivityRecordingState> = _state.asStateFlow()

    fun startRecording(exerciseType: Int, initialFix: Location?): Boolean {
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
        if (initialFix?.activityGpsFixQuality()?.isPrecise != true) {
            updateAndPersist(
                _state.value.copy(
                    errorMessage = context.getString(R.string.activity_recording_error_waiting_for_gps),
                )
            )
            return false
        }

        val now = Instant.now()
        persistenceScope.coroutineContext.cancelChildren()
        recordingStore.clear()
        updateAndPersist(
            ActivityRecordingState(
                status = ActivityRecordingStatus.RECORDING,
                exerciseType = exerciseType,
                startTime = now,
                lastLocationTime = null,
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
        val snapshot = ActivityRecordingSnapshot(
            exerciseType = exerciseType,
            startTime = start,
            endTime = end,
            points = current.points,
            pauseIntervals = pauseIntervals,
            distanceMeters = current.distanceMeters,
            elevationGainedMeters = current.elevationGainedMeters,
        )
        clearRecording()
        stopRecordingService()
        return snapshot
    }

    fun acceptLocation(location: Location) {
        val current = _state.value
        if (current.status != ActivityRecordingStatus.RECORDING) return

        val fixQuality = location.activityGpsFixQuality(startTime = current.startTime)
        if (!fixQuality.isPrecise) {
            updateAndPersist(
                current.withDroppedLocation(
                    fixQuality.accuracyMeters,
                    fixQuality.locationTime,
                )
            )
            return
        }
        val accuracy = fixQuality.accuracyMeters ?: return
        val point = location.toRoutePoint()

        val lastPoint = current.points.lastOrNull()
        var distanceIncrement = 0.0
        var elevationIncrement = 0.0
        if (lastPoint != null) {
            if (!point.time.isAfter(lastPoint.time)) {
                updateAndPersist(current.withDroppedLocation(accuracy, point.time))
                return
            }

            val elapsedMillis = Duration.between(lastPoint.time, point.time)
                .toMillis()
                .coerceAtLeast(0L)
            val distanceMeters = lastPoint.distanceMetersTo(point)
            if (
                distanceMeters < current.minimumSampleDistanceMeters() ||
                elapsedMillis < MinSampleIntervalMillis
            ) {
                updateAndPersist(current.withLocationMetadata(accuracy, point.time))
                return
            }
            if (isImplausibleJump(lastPoint, point, distanceMeters, elapsedMillis, accuracy)) {
                updateAndPersist(current.withDroppedLocation(accuracy, point.time))
                return
            }

            distanceIncrement = distanceMeters
            elevationIncrement = lastPoint.elevationGainMetersTo(point)
        }

        updateAndPersist(
            current.copy(
                points = current.points + point,
                distanceMeters = current.distanceMeters + distanceIncrement,
                elevationGainedMeters = current.elevationGainedMeters + elevationIncrement,
                lastAccuracyMeters = accuracy,
                lastLocationTime = point.time,
                errorMessage = null,
            ),
            routePointToAppend = point,
        )
    }

    fun reportRecordingError(message: String) {
        updateAndPersist(_state.value.copy(errorMessage = message))
    }

    private fun clearRecording() {
        persistenceScope.coroutineContext.cancelChildren()
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

    private fun stopRecordingService() {
        context.stopService(Intent(context, ActivityRecordingService::class.java))
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

fun ActivityRecordingState.movingDuration(now: Instant = Instant.now()): Duration {
    val elapsedMillis = elapsedDuration(now).toMillis()
    val openPauseMillis = pausedStartedAt
        ?.takeIf { status == ActivityRecordingStatus.PAUSED }
        ?.let { Duration.between(it, now).toMillis().coerceAtLeast(0L) }
        ?: 0L
    val pausedMillis = totalPausedMillis + openPauseMillis
    return Duration.ofMillis((elapsedMillis - pausedMillis).coerceAtLeast(0L))
}

private fun Location.toRoutePoint(): ExerciseRoutePoint =
    ExerciseRoutePoint(
        time = Instant.ofEpochMilli(time.takeIf { it > 0L } ?: System.currentTimeMillis()),
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = if (hasAltitude()) altitude else null,
        horizontalAccuracyMeters = if (hasAccuracy()) accuracy.toDouble() else null,
        verticalAccuracyMeters = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && hasVerticalAccuracy()) {
            verticalAccuracyMeters.toDouble()
        } else {
            null
        },
    )

data class ActivityGpsFixQuality(
    val isPrecise: Boolean,
    val accuracyMeters: Double?,
    val locationTime: Instant?,
)

fun Location.activityGpsFixQuality(
    startTime: Instant? = null,
    now: Instant = Instant.now(),
): ActivityGpsFixQuality {
    val accuracy = if (hasAccuracy()) accuracy.toDouble() else null
    val locationTime = Instant.ofEpochMilli(time.takeIf { it > 0L } ?: System.currentTimeMillis())
    val isPrecise = provider == LocationManager.GPS_PROVIDER &&
        accuracy != null &&
        accuracy <= MaxAcceptedAccuracyMeters &&
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
): ActivityRecordingState =
    copy(
        lastAccuracyMeters = accuracyMeters ?: lastAccuracyMeters,
        lastLocationTime = locationTime ?: lastLocationTime,
        droppedPointCount = droppedPointCount + 1,
    )

private fun ActivityRecordingState.withLocationMetadata(
    accuracyMeters: Double?,
    locationTime: Instant,
): ActivityRecordingState =
    copy(
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

private fun ActivityRecordingState.minimumSampleDistanceMeters(): Double =
    when (exerciseType) {
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
        exerciseType = getInt(KeyExerciseType, MissingInt).takeIf { it != MissingInt },
        startTime = getLong(KeyStartTime, MissingLong).toInstantOrNull(),
        endTime = getLong(KeyEndTime, MissingLong).toInstantOrNull(),
        pausedStartedAt = getLong(KeyPausedStartedAt, MissingLong).toInstantOrNull(),
        totalPausedMillis = getLong(KeyTotalPausedMillis, 0L),
        pauseIntervals = getString(KeyPauseIntervals, null).orEmpty().decodePauseIntervals(),
        points = getString(KeyPoints, null).orEmpty().decodeRoutePoints(),
        distanceMeters = getFloat(KeyDistanceMeters, 0f).toDouble(),
        elevationGainedMeters = getFloat(KeyElevationMeters, 0f).toDouble(),
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
        .putInt(KeyExerciseType, state.exerciseType ?: MissingInt)
        .putLong(KeyStartTime, state.startTime?.toEpochMilli() ?: MissingLong)
        .putLong(KeyEndTime, state.endTime?.toEpochMilli() ?: MissingLong)
        .putLong(KeyPausedStartedAt, state.pausedStartedAt?.toEpochMilli() ?: MissingLong)
        .putLong(KeyTotalPausedMillis, state.totalPausedMillis)
        .putString(KeyPauseIntervals, state.pauseIntervals.encodePauseIntervals())
        .putFloat(KeyDistanceMeters, state.distanceMeters.toFloat())
        .putFloat(KeyElevationMeters, state.elevationGainedMeters.toFloat())
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
private const val MaxAcceptedAccuracyMeters = 30.0
private const val MaxLocationAgeMillis = 10_000L
private const val MaxLocationFutureSkewSeconds = 5L
private const val MaxPlausibleSpeedMetersPerSecond = 55.0
private const val MinSampleIntervalMillis = 500L
private const val MinElevationGainIncrementMeters = 1.0
private const val MissingInt = Int.MIN_VALUE
private const val MissingLong = Long.MIN_VALUE
private const val MissingFloat = -1f
private const val KeyStatus = "status"
private const val KeyExerciseType = "exercise_type"
private const val KeyStartTime = "start_time"
private const val KeyEndTime = "end_time"
private const val KeyPausedStartedAt = "paused_started_at"
private const val KeyTotalPausedMillis = "total_paused_millis"
private const val KeyPauseIntervals = "pause_intervals"
private const val KeyPoints = "points"
private const val KeyDistanceMeters = "distance_meters"
private const val KeyElevationMeters = "elevation_meters"
private const val KeyLastAccuracyMeters = "last_accuracy_meters"
private const val KeyLastLocationTime = "last_location_time"
private const val KeyDroppedPointCount = "dropped_point_count"
private const val KeyErrorMessage = "error_message"
