package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import java.time.Duration
import java.time.Instant
import tech.mmarca.openvitals.domain.model.ActivityRecordSource
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryType
import tech.mmarca.openvitals.features.manualentry.activity.MaxActivityDurationMinutes
import tech.mmarca.openvitals.features.manualentry.activity.activityCalorieEstimate
import tech.mmarca.openvitals.features.manualentry.activity.inferActivityType

/**
 * The write request for a file imported with nobody to review it: a watch sync, a bulk
 * folder import.
 *
 * These used to be pushed through the entry form: the file became form text and the text
 * was parsed back. The start lost its seconds, the duration was rounded up to a minute,
 * the records were stored as a manual entry from a phone, and every client id was random,
 * so importing a file twice stored the workout twice.
 *
 * Null when the file cannot stand alone as an activity: no time range, a span past the
 * limit, or a route on a type that takes none. A caller that has a form can still fall
 * back to it.
 */
internal fun RouteFileImport.toImportWriteRequest(
    fallbackType: ActivityEntryType,
    source: ActivityRecordSource,
): ActivityWriteRequest? {
    if (!hasImportedTimeRange) return null
    val key = contentKey ?: return null
    val type = inferActivityType(this, fallbackType)
    if (points.isNotEmpty() && !type.supportsGpsRoute) return null

    // The session must contain everything written beside it.
    val firstTimes = listOfNotNull(startTime, points.firstOrNull()?.time, bleSamples.firstSampleTime())
    val lastTimes = listOfNotNull(
        endTime,
        points.lastOrNull()?.time?.plusSeconds(1),
        bleSamples.lastSampleTime()?.plusSeconds(1),
    )
    val start = firstTimes.min()
    val end = lastTimes.max()
    if (!start.isBefore(end)) return null
    val minutes = Duration.between(start, end).toMinutes()
    if (minutes > MaxActivityDurationMinutes) return null

    val active = activeCaloriesKcal?.takeIf { it > 0.0 }
    val total = totalCaloriesKcal?.takeIf { it > 0.0 }
    // As the form does: estimate only for a file that measured no calories at all.
    val estimate = if (active == null && total == null) {
        activityCalorieEstimate(
            activityType = type,
            distanceMeters = distanceMeters,
            durationMinutesText = minutes.coerceAtLeast(1).toString(),
        )
    } else {
        null
    }
    val activeKcal = active ?: estimate?.activeCaloriesText?.toDoubleOrNull()
    val totalKcal = total ?: estimate?.totalCaloriesText?.toDoubleOrNull()

    return ActivityWriteRequest(
        exerciseType = type.exerciseType,
        startTime = start,
        endTime = end,
        title = name ?: fileName?.substringBeforeLast('.', missingDelimiterValue = fileName) ?: type.defaultTitle,
        notes = description?.trim()?.takeIf { it.isNotEmpty() },
        routePoints = points,
        distanceMeters = distanceMeters.takeIf { type.supportsDistance && it > 0.0 },
        elevationGainedMeters = elevationGainedMeters.takeIf { type.supportsElevation && it > 0.0 },
        // Health Connect refuses a total below the active part.
        activeCaloriesKcal = activeKcal?.takeIf { totalKcal == null || it <= totalKcal },
        totalCaloriesKcal = totalKcal,
        bleSamples = bleSamples.within(start, end),
        source = source,
        importKey = key,
    )
}

private fun BleRecordingSampleBuffer.within(start: Instant, end: Instant): BleRecordingSampleBuffer {
    fun Instant.isInside(): Boolean = !isBefore(start) && isBefore(end)
    return BleRecordingSampleBuffer(
        heartRateSamples = heartRateSamples.filter { it.time.isInside() },
        powerSamples = powerSamples.filter { it.time.isInside() },
        cyclingCadenceSamples = cyclingCadenceSamples.filter { it.time.isInside() },
        speedSamples = speedSamples.filter { it.time.isInside() },
        stepsCadenceSamples = stepsCadenceSamples.filter { it.time.isInside() },
    )
}
