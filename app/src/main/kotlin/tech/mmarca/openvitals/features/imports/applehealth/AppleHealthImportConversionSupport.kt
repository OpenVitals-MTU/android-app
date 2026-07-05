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

internal data class AppleAdditiveOverlapCandidate(
    val record: AppleRecord,
    val start: Instant,
    val end: Instant,
    val sourceName: String?,
    val sourcePriority: Int,
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
    val bytes = MessageDigest.getInstance("SHA-256")
        .digest(parts.toString().toByteArray(Charsets.UTF_8))
    // 16 bytes → 32 hex chars; manual hex encoding avoids a Formatter allocation per byte on
    // this hot path (multiple digests per imported record).
    val digest = buildString(32) {
        for (index in 0 until 16) {
            val byte = bytes[index].toInt() and 0xFF
            append(HexDigits[byte ushr 4])
            append(HexDigits[byte and 0x0F])
        }
    }
    return "apple_health_${prefix.toStableIdSegment()}_$digest"
}

private const val HexDigits = "0123456789abcdef"

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

internal fun AppleRecord.toAdditiveOverlapCandidate(): AppleAdditiveOverlapCandidate? {
    if (type !in AppleAdditiveOverlapSensitiveTypes) return null
    val start = startDate?.instant ?: return null
    val rawEnd = endDate?.instant ?: start
    val end = if (rawEnd.isAfter(start)) rawEnd else start.plusSeconds(1)
    return AppleAdditiveOverlapCandidate(
        record = this,
        start = start,
        end = end,
        sourceName = sourceName,
        sourcePriority = additiveSourcePriority(),
    )
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

internal class AppleAdditiveOverlapIndex {
    private val rangesByTypeAndSource = linkedMapOf<String, MutableMap<String, MutableList<AppleInstantRange>>>()

    fun isMostlyCovered(candidate: AppleAdditiveOverlapCandidate): Boolean {
        val source = candidate.sourceName.orEmpty()
        val sourceRanges = rangesByTypeAndSource[candidate.record.type] ?: return false
        val overlaps = mutableListOf<AppleInstantRange>()
        sourceRanges.forEach { (acceptedSource, ranges) ->
            if (acceptedSource != source) {
                ranges.collectOverlaps(candidate.start, candidate.end, overlaps)
            }
        }
        if (overlaps.isEmpty()) return false

        overlaps.sortBy { it.start }
        var coveredSeconds = 0L
        var currentStart = overlaps.first().start
        var currentEnd = overlaps.first().end
        for (index in 1 until overlaps.size) {
            val range = overlaps[index]
            if (range.start <= currentEnd) {
                currentEnd = maxOf(currentEnd, range.end)
            } else {
                coveredSeconds += Duration.between(currentStart, currentEnd).seconds
                currentStart = range.start
                currentEnd = range.end
            }
        }
        coveredSeconds += Duration.between(currentStart, currentEnd).seconds

        val durationSeconds = Duration.between(candidate.start, candidate.end).seconds.coerceAtLeast(1)
        return coveredSeconds.toDouble() / durationSeconds >= AdditiveOverlapCoverageThreshold
    }

    fun add(candidate: AppleAdditiveOverlapCandidate) {
        val ranges = rangesByTypeAndSource
            .getOrPut(candidate.record.type) { linkedMapOf() }
            .getOrPut(candidate.sourceName.orEmpty()) { mutableListOf() }
        ranges.addMerged(candidate.start, candidate.end)
    }
}

private data class AppleInstantRange(
    val start: Instant,
    var end: Instant,
)

private fun MutableList<AppleInstantRange>.collectOverlaps(
    start: Instant,
    end: Instant,
    destination: MutableList<AppleInstantRange>,
) {
    var index = indexOfFirstEndingAfter(start)
    while (index < size) {
        val range = this[index]
        if (range.start >= end) break
        val overlapStart = maxOf(range.start, start)
        val overlapEnd = minOf(range.end, end)
        if (overlapEnd.isAfter(overlapStart)) {
            destination += AppleInstantRange(overlapStart, overlapEnd)
        }
        index += 1
    }
}

private fun MutableList<AppleInstantRange>.indexOfFirstEndingAfter(start: Instant): Int {
    var low = 0
    var high = size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (this[mid].end <= start) {
            low = mid + 1
        } else {
            high = mid
        }
    }
    return low
}

private fun MutableList<AppleInstantRange>.addMerged(start: Instant, end: Instant) {
    if (isEmpty()) {
        add(AppleInstantRange(start, end))
        return
    }

    val last = last()
    if (start >= last.start) {
        if (start <= last.end) {
            last.end = maxOf(last.end, end)
        } else {
            add(AppleInstantRange(start, end))
        }
        return
    }

    add(AppleInstantRange(start, end))
    sortBy { it.start }
    var writeIndex = 0
    for (readIndex in 1 until size) {
        val current = this[readIndex]
        val merged = this[writeIndex]
        if (current.start <= merged.end) {
            merged.end = maxOf(merged.end, current.end)
        } else {
            writeIndex += 1
            this[writeIndex] = current
        }
    }
    subList(writeIndex + 1, size).clear()
}

private fun AppleRecord.additiveSourcePriority(): Int {
    val source = sourceName?.lowercase(Locale.US).orEmpty()
    return when {
        "watch" in source -> 0
        source.containsAny(WorkoutAppSourceHints) -> 1
        "iphone" in source || "ipad" in source -> 2
        "apple" in source -> 3
        else -> 4
    }
}

private fun String.containsAny(values: Set<String>): Boolean = values.any { it in this }

internal fun List<AppleWorkoutOverlapCandidate>.hasOverlapping(workout: AppleWorkout, types: Set<String>): Boolean {
    val workoutStart = workout.startDate?.instant ?: return false
    val workoutEnd = workout.endDate?.instant ?: return false
    return any { record ->
        record.type in types &&
            record.startDate?.instant?.isBefore(workoutEnd) == true &&
            (record.endDate?.instant ?: record.startDate.instant).isAfter(workoutStart)
    }
}

internal fun String.toStableIdSegment(): String =
    lowercase(Locale.US)
        .replace(StableIdSegmentRegex, "_")
        .trim('_')
        .ifBlank { "record" }

private val StableIdSegmentRegex = Regex("[^a-z0-9]+")

private val SleepSessionGap: Duration = Duration.ofHours(2)
private const val AdditiveOverlapCoverageThreshold = 0.8
private val WorkoutAppSourceHints = setOf(
    "strava",
    "garmin",
    "polar",
    "fitbit",
    "wahoo",
    "zwift",
    "runkeeper",
    "komoot",
    "trainingpeaks",
)
