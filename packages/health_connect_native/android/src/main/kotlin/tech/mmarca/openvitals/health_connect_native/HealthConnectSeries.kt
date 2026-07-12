package tech.mmarca.openvitals.health_connect_native

import android.util.Log
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
 * So: ask for the window; if it comes back empty, ask again with both ends
 * widened far enough to catch a record that merely CONTAINS the window; and in
 * either case keep only the samples that actually fall inside it.
 *
 * Widening BOTH ends is deliberate. Health Connect's interval filtering appears
 * to key on a record's start, but that is observed behaviour rather than a
 * documented promise — a symmetric window is correct whether the filter tests
 * the start or demands full containment, and this is not worth making fragile to
 * save one query on a path that is already the cold one.
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
 * How far either side of a window to look for a record that swallowed it.
 *
 * Comfortably past a day: the record that prompted this — a real one, written by
 * this very app — ran 17.5 hours. Only ever paid on a window that came back
 * empty, so a generous bound costs nothing on the healthy path.
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

  val direct = readSeriesSamplesClipped(recordType, start, end, start, end, flatten)
  if (direct.isNotEmpty()) return direct

  // Empty means either "no data" or "every sample sits inside a record whose own
  // boundary falls outside the window". Only the second is recoverable, and only
  // a second query tells the two apart.
  val recovered = readSeriesSamplesClipped(
    recordType = recordType,
    readStart = start.minus(lookaround),
    readEnd = end.plus(lookaround),
    clipStart = start,
    clipEnd = end,
    flatten = flatten,
  )
  if (recovered.isNotEmpty()) {
    Log.d(
      "OpenVitalsPerf",
      "healthConnect.readSeriesSamples type=${recordType.simpleName} " +
        "recovered=${recovered.size} samples the windowed read could not see",
    )
  }
  return recovered
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
