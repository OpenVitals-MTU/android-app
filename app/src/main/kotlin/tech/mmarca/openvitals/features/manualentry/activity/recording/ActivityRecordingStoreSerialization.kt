package tech.mmarca.openvitals.features.manualentry.activity.recording

import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ActivityRecordingLap
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarkerType
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences

internal fun SharedPreferences.restoreRecordingState(): ActivityRecordingState {
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

internal fun SharedPreferences.storeRecordingMetadata(state: ActivityRecordingState) {
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

internal fun Instant.toPauseInterval(endTime: Instant): ActivityPauseInterval? =
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

internal fun List<ExerciseRoutePoint>.encodeRoutePoints(): String =
    joinToString(separator = "\n") { point -> point.encodeRoutePoint() }

internal fun ExerciseRoutePoint.encodeRoutePoint(): String =
    listOf(
        time.toEpochMilli().toString(),
        latitude.toString(),
        longitude.toString(),
        altitudeMeters?.toString().orEmpty(),
        horizontalAccuracyMeters?.toString().orEmpty(),
        verticalAccuracyMeters?.toString().orEmpty(),
    ).joinToString(separator = ",")

internal fun String.decodeRoutePoints(): List<ExerciseRoutePoint> =
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
