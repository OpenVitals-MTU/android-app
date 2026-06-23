package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Locale

internal data class AppleInterval(
    val start: AppleDateTime,
    val end: AppleDateTime,
)

internal fun interval(start: AppleDateTime, end: AppleDateTime): AppleInterval {
    val adjustedEnd =
        if (end.instant.isAfter(start.instant)) {
            end
        } else {
            end.copy(instant = start.instant.plusSeconds(1), offset = end.offset ?: start.offset)
        }
    return AppleInterval(start = start, end = adjustedEnd)
}

internal data class SleepStageCandidate(
    val record: AppleRecord,
    val start: AppleDateTime,
    val end: AppleDateTime,
    val stage: Int,
    val inBedOnly: Boolean,
)

internal data class AppleWorkoutOverlapCandidate(
    val type: String,
    val sourceName: String?,
    val startDate: AppleDateTime?,
    val endDate: AppleDateTime?,
)

internal data class BoundedWorkoutOverlapCandidates(
    val candidates: List<AppleWorkoutOverlapCandidate>,
    val limitReached: Boolean,
)

internal fun List<SleepStageCandidate>.splitSleepSessions(): List<List<SleepStageCandidate>> {
    val sessions = mutableListOf<MutableList<SleepStageCandidate>>()
    forEach { candidate ->
        val current = sessions.lastOrNull()
        if (current == null || Duration.between(current.maxOf { it.end.instant }, candidate.start.instant) > SleepSessionGap) {
            sessions += mutableListOf(candidate)
        } else {
            current += candidate
        }
    }
    return sessions
}

internal fun appleMetadata(record: AppleRecord, targetType: String, sourceFingerprint: String): Metadata =
    appleMetadata(targetType, record.stableClientRecordId(targetType.toStableIdSegment(), sourceFingerprint))

internal fun appleMetadata(targetType: String, fingerprint: String): Metadata =
    Metadata.manualEntry(
        device = Device(type = Device.TYPE_PHONE),
        clientRecordId = fingerprint.ifBlank {
            buildStableClientRecordId(targetType.toStableIdSegment(), listOf(targetType, Instant.now().toString()))
        },
    )

internal fun buildStableClientRecordId(prefix: String, parts: Any): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(parts.toString().toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
        .take(32)
    return "apple_health_${prefix.toStableIdSegment()}_$digest"
}

internal fun AppleRecord.stableClientRecordId(prefix: String, extra: Any = stableParts()): String =
    buildStableClientRecordId(prefix, extra)

internal val AppleRecord.sourceFingerprint: String
    get() = stableParts()

internal fun AppleRecord.stableParts(): String =
    listOf(
        type,
        sourceName.orEmpty(),
        sourceVersion.orEmpty(),
        device.orEmpty(),
        creationDate?.instant?.toString().orEmpty(),
        startDate?.instant?.toString().orEmpty(),
        endDate?.instant?.toString().orEmpty(),
        unit.orEmpty(),
        rawValue.orEmpty(),
        correlationType.orEmpty(),
        metadata.toSortedMap().entries.joinToString(";") { "${it.key}=${it.value}" },
    ).joinToString("|")

internal fun AppleWorkout.stableParts(): String =
    listOf(
        workoutActivityType,
        sourceName.orEmpty(),
        sourceVersion.orEmpty(),
        device.orEmpty(),
        creationDate?.instant?.toString().orEmpty(),
        startDate?.instant?.toString().orEmpty(),
        endDate?.instant?.toString().orEmpty(),
        duration?.toString().orEmpty(),
        durationUnit.orEmpty(),
        totalDistance?.toString().orEmpty(),
        totalDistanceUnit.orEmpty(),
        totalEnergyBurned?.toString().orEmpty(),
        totalEnergyBurnedUnit.orEmpty(),
        metadata.toSortedMap().entries.joinToString(";") { "${it.key}=${it.value}" },
    ).joinToString("|")

internal fun AppleCorrelation.stableParts(): String =
    listOf(
        type,
        sourceName.orEmpty(),
        sourceVersion.orEmpty(),
        device.orEmpty(),
        creationDate?.instant?.toString().orEmpty(),
        startDate?.instant?.toString().orEmpty(),
        endDate?.instant?.toString().orEmpty(),
        records.joinToString(";") { it.stableParts() },
    ).joinToString("|")

internal fun AppleRecord.timeRangeOrNull(): AppleImportTimeRange? {
    val start = startDate?.instant ?: return null
    val end = endDate?.instant ?: start
    return AppleImportTimeRange(start, end)
}

internal fun AppleCorrelation.timeRangeOrNull(): AppleImportTimeRange? {
    val start = startDate?.instant ?: records.mapNotNull { it.startDate?.instant }.minOrNull() ?: return null
    val end = endDate?.instant ?: records.mapNotNull { it.endDate?.instant ?: it.startDate?.instant }.maxOrNull() ?: start
    return AppleImportTimeRange(start, end)
}

internal fun AppleRecord.toWorkoutOverlapCandidate(): AppleWorkoutOverlapCandidate? =
    if (type in AppleDistanceTypes || type == AppleActiveEnergyBurned) {
        AppleWorkoutOverlapCandidate(
            type = type,
            sourceName = sourceName,
            startDate = startDate,
            endDate = endDate,
        )
    } else {
        null
    }

internal fun List<AppleRecord>.toBoundedWorkoutOverlapCandidates(): BoundedWorkoutOverlapCandidates {
    val candidates = ArrayList<AppleWorkoutOverlapCandidate>(minOf(size, MaxWorkoutOverlapCandidates))
    var limitReached = false
    for (record in this) {
        val candidate = record.toWorkoutOverlapCandidate() ?: continue
        if (candidates.size < MaxWorkoutOverlapCandidates) {
            candidates += candidate
        } else {
            limitReached = true
            break
        }
    }
    return BoundedWorkoutOverlapCandidates(candidates, limitReached)
}

internal fun List<AppleWorkoutOverlapCandidate>.hasOverlapping(workout: AppleWorkout, types: Set<String>): Boolean {
    val workoutStart = workout.startDate?.instant ?: return false
    val workoutEnd = workout.endDate?.instant ?: return false
    return any { record ->
        record.type in types &&
            (record.sourceName == null || workout.sourceName == null || record.sourceName == workout.sourceName) &&
            record.startDate?.instant?.isBefore(workoutEnd) == true &&
            (record.endDate?.instant ?: record.startDate.instant).isAfter(workoutStart)
    }
}

internal fun String.toStableIdSegment(): String =
    lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "record" }

private val SleepSessionGap: Duration = Duration.ofHours(2)
