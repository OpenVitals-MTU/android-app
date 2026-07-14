package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Reading Health Connect *series* records — the record types that carry a nested
 * list of timestamped samples: heart rate, speed, power, and the two cadences.
 *
 * These need their own read path because Health Connect filters a series record
 * by the boundary of the RECORD, never by the times of the samples inside it.
 * Two things follow, and both were live bugs:
 *
 *  - **Samples go missing.** A writer is free to group a whole day of beats into
 *    one HeartRateRecord. Ask for a 09:05-09:41 workout and Health Connect looks
 *    at a record that runs 03:34-21:03, decides it is not in the window, and
 *    hands back nothing — successfully. The workout had a heart rate the whole
 *    time; the activity screen said "Not available", and nothing anywhere looked
 *    like a failure. The same read backs the 1 km splits (via SpeedRecord) and
 *    the cadence traces, so those silently degraded to estimates too.
 *
 *  - **Samples leak in.** The mirror image: when such a record IS returned, the
 *    naive `flatMap { it.samples }` takes every sample in it, including the
 *    hours sitting outside the window that was asked for.
 *
 * So: ask for a window widened far enough to catch any record that OVERLAPS the
 * one that was wanted, and keep only the samples that actually fall inside it.
 *
 * The widening is unconditional, and that is the fix for the second bug above's
 * quieter half. This used to ask for the exact window first and only widen when
 * that came back EMPTY — which catches a record that swallows the window whole,
 * and misses one that overlaps only its edge. A hike starting at 11:50 whose
 * watch had been recording since 11:48 read back fine: the records from 12:44
 * onwards begin inside the window, so the read was full, healthy-looking, and
 * short by the first fifty-three minutes. Nothing is empty, so nothing asked
 * again. Emptiness is not the symptom; a record boundary crossing the window's is.
 *
 * Widening BOTH ends is deliberate. Health Connect's interval filtering appears
 * to key on a record's start, but that is observed behaviour rather than a
 * documented promise (the FakeHealthConnectClient the tests run against demands
 * full containment instead) — a symmetric window is correct either way, and is
 * not worth making fragile to save a few records off a bounded read.
 */

/**
 * A mapped sample, still carrying the time it happened, so the read below can
 * clip and order a series it knows nothing else about.
 *
 * The caller flattens the record rather than the read doing it generically:
 * Kotlin cannot infer a sample type out of `SeriesRecord<T>`, and the honest
 * lambda is better than making every call site spell both types out.
 */
internal data class TimedSample<out T>(val time: Instant, val value: T)

/**
 * How far either side of a window to look for a record that overlaps it.
 *
 * It has to be at least as long as the longest record any writer might produce, or
 * that record's overlap is invisible again. Comfortably past a day: the record that
 * prompted the original fix — a real one, written by this very app — ran 17.5 hours.
 *
 * This is now paid on every read rather than only on one that came back empty, which
 * is what makes it correct. It is affordable because every window it is asked for is
 * bounded and small: one day (the intraday charts) or one workout. The margin is a
 * fixed 30 hours, not a multiple of the window, so it cannot grow with the range.
 */
private val SeriesRecordLookaround: Duration = Duration.ofHours(30)

/**
 * The samples of [recordType] that fall inside `[start, end)`, in time order,
 * however the writer chose to group them into records.
 */
internal suspend fun <R : Record, T> HealthConnectClient.readSeriesSamples(
  recordType: KClass<R>,
  start: Instant,
  end: Instant,
  lookaround: Duration = SeriesRecordLookaround,
  flatten: (R) -> List<TimedSample<T>>,
): List<T> {
  if (!end.isAfter(start)) return emptyList()

  // Read wide, keep narrow. A record is fetched if it could possibly overlap the
  // window; a SAMPLE is kept only if it actually falls inside it.
  //
  // Deliberately unconditional. Asking for the exact window first and widening only
  // when it came back empty is what hid a record overlapping the START of a workout:
  // the rest of the workout answers the narrow read, so it is never empty, and the
  // front of the trace goes missing without a single symptom.
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
