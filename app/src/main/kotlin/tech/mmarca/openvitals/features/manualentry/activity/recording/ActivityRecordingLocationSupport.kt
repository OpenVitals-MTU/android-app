package tech.mmarca.openvitals.features.manualentry.activity.recording

import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import java.time.Instant
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences

internal fun Location.toRoutePoint(timeOverride: Instant? = null): ExerciseRoutePoint =
    ExerciseRoutePoint(
        time = timeOverride ?: Instant.ofEpochMilli(time.takeIf { it > 0L } ?: System.currentTimeMillis()),
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = mslAltitudeMetersOrNull() ?: if (hasAltitude()) altitude else null,
        horizontalAccuracyMeters = if (hasAccuracy()) accuracy.toDouble() else null,
        verticalAccuracyMeters = mslAltitudeAccuracyMetersOrNull()
            ?: if (hasVerticalAccuracy()) {
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
    if (elapsedRealtimeNanos > 0L) {
        ((SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L)
            .coerceAtLeast(0L)
    } else {
        (System.currentTimeMillis() - time).coerceAtLeast(0L)
    }

internal fun ActivityRecordingState.withDroppedLocation(
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

internal fun ActivityRecordingState.withLocationMetadata(
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

internal fun ExerciseRoutePoint.recordingDistanceMetersTo(other: ExerciseRoutePoint): Double {
    val results = FloatArray(1)
    Location.distanceBetween(latitude, longitude, other.latitude, other.longitude, results)
    return results[0].toDouble()
}

internal fun ExerciseRoutePoint.elevationGainMetersTo(other: ExerciseRoutePoint): Double {
    val startAltitude = altitudeMeters ?: return 0.0
    val endAltitude = other.altitudeMeters ?: return 0.0
    return (endAltitude - startAltitude)
        .takeIf { it >= MinElevationGainIncrementMeters }
        ?: 0.0
}

internal fun ExerciseRoutePoint.elevationLossMetersTo(other: ExerciseRoutePoint): Double {
    val startAltitude = altitudeMeters ?: return 0.0
    val endAltitude = other.altitudeMeters ?: return 0.0
    return (startAltitude - endAltitude)
        .takeIf { it >= MinElevationGainIncrementMeters }
        ?: 0.0
}

internal fun isImplausibleJump(
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
