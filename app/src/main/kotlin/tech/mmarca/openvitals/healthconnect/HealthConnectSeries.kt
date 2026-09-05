package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Reading Health Connect series records (heart rate, speed, power, cadence).
 * Health Connect filters by the record's bounds, never the samples', so a
 * day-long record misses a workout window and a returned record leaks
 * samples outside it. Read a window widened both ways, keep only samples
 * inside. Widening is unconditional: an edge overlap comes back non-empty
 * and short.
 */

/** A mapped sample with its time, so the read can clip and order it. */
internal data class TimedSample<out T>(val time: Instant, val value: T)

/**
 * How far either side of a window to look for an overlapping record. Must
 * exceed the longest record any writer produces; this app wrote a 17.5-hour
 * one. Fixed, so it cannot grow with the range.
 */
private val SeriesRecordLookaround: Duration = Duration.ofHours(30)

/** The samples of [recordType] inside `[start, end)`, in time order, however they were grouped. */
internal suspend fun <R : Record, T> HealthConnectClient.readSeriesSamples(
    recordType: KClass<R>,
    start: Instant,
    end: Instant,
    lookaround: Duration = SeriesRecordLookaround,
    flatten: (R) -> List<TimedSample<T>>,
): List<T> {
    if (!end.isAfter(start)) return emptyList()

    // Read wide, keep narrow.
    return readSeriesSamplesClipped(
        recordType = recordType,
        readStart = start.minus(lookaround),
        readEnd = end.plus(lookaround),
        clipStart = start,
        clipEnd = end,
        flatten = flatten,
    )
}

/** Reads across `[readStart, readEnd]`, keeping only samples inside `[clipStart, clipEnd)`. */
private suspend fun <R : Record, T> HealthConnectClient.readSeriesSamplesClipped(
    recordType: KClass<R>,
    readStart: Instant,
    readEnd: Instant,
    clipStart: Instant,
    clipEnd: Instant,
    flatten: (R) -> List<TimedSample<T>>,
): List<T> =
    readRecordsPaged(
        recordType = recordType,
        timeRangeFilter = TimeRangeFilter.between(readStart, readEnd),
        ascendingOrder = true,
        pageSize = 500,
    ).flatMap(flatten)
        .filter { !it.time.isBefore(clipStart) && it.time.isBefore(clipEnd) }
        .sortedBy { it.time }
        .map { it.value }
