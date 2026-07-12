package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.testing.AggregationResult
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Velocity
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KClass

/**
 * Google's fake, plus the three methods it refuses to implement.
 *
 *     "To use the aggregate method you must provide a fake response via
 *      overrides.aggregate."   -> IllegalStateException
 *
 * Fine for an app that aggregates rarely. This one aggregates *everywhere*: the
 * sibling-record session metrics (the entire walking-activity fix, and the power
 * read), the calorie fallback chain, daily steps, daily heart-rate summaries,
 * resting heart rate, hydration, nutrition. Hand-stubbing those responses would mean
 * the tests assert numbers WE made up — and would not have caught the bug where a
 * watch's steps and distance, written as separate records beside the session, were
 * never reattached to it at all.
 *
 * So everything delegates to Google's fake, and only these three are ours. ONE thing
 * in the suite can be wrong, rather than all of Health Connect.
 *
 * ## The two rules
 *
 * **Series records** (heart rate, speed, power) aggregate over the SAMPLES whose time
 * falls in the window — never over the record's own boundary. Well-founded, and the
 * entire reason aggregation is immune to the bug that hides a workout inside a
 * 17-hour record: `aggregate` slices by TIME, `readRecords` slices by RECORD.
 *
 * **Interval records** (steps, distance, calories, elevation) are PRO-RATED by how
 * much of the record overlaps the window: 600 steps over an hour, half inside the
 * window, contributes 300.
 *
 * ## What is NOT verified — read this before trusting a number
 *
 * The pro-rating rule is **UNCALIBRATED**. Health Connect's real behaviour for an
 * interval record straddling a window edge is undocumented and unmeasured. It is the
 * single largest unknown in this suite.
 *
 * The plan was to settle it with a probe against a real Health Connect. **That probe
 * cannot run in CI — the infrastructure has no emulator.** So it must be run ONCE, by
 * hand, on a device, and its answers frozen as fixed expectations in a JVM test, so
 * the calibration lives in CI even though the probe does not.
 *
 * Until then: an aggregate assertion here is NOT proof of what a device would say. It
 * is proof that our own arithmetic has not changed.
 */
class AggregatingFakeHealthConnectClient(
  private val inner: FakeHealthConnectClient,
) : HealthConnectClient by inner {

  override suspend fun aggregate(request: AggregateRequest): AggregationResult {
    val (start, end) = bounds(request.internalTimeRange())
    return compute(request.internalMetrics(), start, end)
  }

  override suspend fun aggregateGroupByDuration(
    request: AggregateGroupByDurationRequest,
  ): List<AggregationResultGroupedByDuration> {
    val metrics = request.internalMetrics()
    val (start, end) = bounds(request.internalTimeRange())
    val slice = request.internalSlicer()

    val out = mutableListOf<AggregationResultGroupedByDuration>()
    var bucketStart = start
    while (bucketStart.isBefore(end)) {
      val bucketEnd = minOf(bucketStart.plus(slice), end)
      val result = compute(metrics, bucketStart, bucketEnd)
      // Health Connect OMITS an empty bucket rather than returning a zero one, and
      // the app depends on it: "no data" and "zero" are different answers, and
      // several screens branch on exactly that difference.
      if (metrics.any { result.contains(it.erased()) }) {
        out.add(
          AggregationResultGroupedByDuration(
            result = result,
            startTime = bucketStart,
            endTime = bucketEnd,
            zoneOffset = ZoneOffset.UTC,
          ),
        )
      }
      bucketStart = bucketEnd
    }
    return out
  }

  override suspend fun aggregateGroupByPeriod(
    request: AggregateGroupByPeriodRequest,
  ): List<AggregationResultGroupedByPeriod> =
    // Deliberately not emulated. Period bucketing resolves its edges against each
    // RECORD's own zone offset — behaviour we have not measured and will not guess
    // at. A loud failure beats a confident wrong number. (The app moved its daily
    // steps read to aggregateGroupByDuration for related reasons.)
    throw NotImplementedError(
      "aggregateGroupByPeriod is not emulated: its bucket edges resolve against each " +
        "record's own zone offset, which is unmeasured. See the class doc.",
    )

  // ── the arithmetic ──────────────────────────────────────────────────────────

  private suspend fun compute(
    metrics: Set<AggregateMetric<*>>,
    start: Instant,
    end: Instant,
  ): AggregationResult {
    val values = mutableMapOf<AggregateMetric<Any>, Any>()
    val origins = mutableSetOf<DataOrigin>()

    for (metric in metrics) {
      val spec = SPECS[metric] ?: error(
        "No aggregation emulated for $metric. Add it to SPECS — otherwise a test is " +
          "silently asserting against a metric nobody computed.",
      )
      val records = readAll(spec.recordType)
      if (records.isEmpty()) continue

      val value = spec.compute(records, start, end) ?: continue
      values[metric.erased()] = value
      records.forEach { origins.add(it.metadata.dataOrigin) }
    }
    return AggregationResult(dataOrigins = origins, metrics = values)
  }

  /**
   * Every record of a type, regardless of window.
   *
   * Unbounded on purpose. Aggregation must NOT be limited by record boundaries —
   * that is the whole difference between it and `readRecords`, and the reason it can
   * see inside a 17-hour record that a windowed read cannot. Overlap is decided
   * below, on sample and interval TIMES.
   */
  private suspend fun <T : Record> readAll(type: KClass<T>): List<T> =
    inner.readRecords(
      ReadRecordsRequest(
        recordType = type,
        timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH),
      ),
    ).records

  private fun bounds(filter: TimeRangeFilter): Pair<Instant, Instant> =
    (filter.startTime ?: Instant.EPOCH) to (filter.endTime ?: Instant.now())

  // ── reaching into the request ───────────────────────────────────────────────
  //
  // AggregateRequest.metrics and .timeRangeFilter are `internal` to connect-client,
  // so a fake outside the library cannot read the request it is being asked to
  // answer. Kotlin's `internal` survives into the bytecode as a mangled public
  // getter (`getMetrics$connect_client`), which reflection can reach.
  //
  // This IS reaching into library internals, and it will break if androidx renames
  // them. That is why it throws with the name it looked for rather than returning
  // something plausible: a fake that silently aggregated the WRONG metrics would be
  // far worse than one that does not compile.

  @Suppress("UNCHECKED_CAST")
  private fun Any.internalMetrics(): Set<AggregateMetric<*>> =
    internal("getMetrics\$connect_client") as Set<AggregateMetric<*>>

  private fun Any.internalTimeRange(): TimeRangeFilter =
    internal("getTimeRangeFilter\$connect_client") as TimeRangeFilter

  private fun Any.internalSlicer(): Duration =
    internal("getTimeRangeSlicer\$connect_client") as Duration

  private fun Any.internal(getter: String): Any =
    runCatching { javaClass.getMethod(getter).invoke(this)!! }.getOrElse {
      throw IllegalStateException(
        "connect-client no longer exposes $getter on ${javaClass.simpleName}. The " +
          "aggregating fake reads the request through Kotlin's internal name " +
          "mangling; androidx has moved it. Fix this rather than working around it — " +
          "a fake that aggregates the wrong metrics is worse than one that fails.",
        it,
      )
    }

  @Suppress("UNCHECKED_CAST")
  private fun AggregateMetric<*>.erased(): AggregateMetric<Any> =
    this as AggregateMetric<Any>

  // ── the metric table ────────────────────────────────────────────────────────

  private class Spec(
    val recordType: KClass<out Record>,
    val compute: (List<Record>, Instant, Instant) -> Any?,
  )

  private companion object {

    /**
     * How much of an interval record's own span lies inside the window.
     *
     * `IntervalRecord` is internal to connect-client, so this takes the two instants
     * rather than the record — which is no loss: every caller knows its concrete type.
     */
    fun fraction(rStart: Instant, rEnd: Instant, start: Instant, end: Instant): Double {
      val span = rEnd.toEpochMilli() - rStart.toEpochMilli()
      if (span <= 0L) return 0.0
      val from = maxOf(rStart, start).toEpochMilli()
      val to = minOf(rEnd, end).toEpochMilli()
      return (to - from).coerceAtLeast(0L).toDouble() / span
    }

    /** Samples of a series record whose OWN time falls in the window. */
    inline fun <reified T : Record, S> samplesIn(
      records: List<Record>,
      start: Instant,
      end: Instant,
      samples: (T) -> List<S>,
      time: (S) -> Instant,
    ): List<S> = records.filterIsInstance<T>()
      .flatMap(samples)
      .filter { !time(it).isBefore(start) && time(it).isBefore(end) }

    private fun hr(rs: List<Record>, s: Instant, e: Instant): List<Long> =
      samplesIn<HeartRateRecord, HeartRateRecord.Sample>(
        rs, s, e, { it.samples }, { it.time },
      ).map { it.beatsPerMinute }

    val SPECS: Map<AggregateMetric<*>, Spec> = mapOf(
      StepsRecord.COUNT_TOTAL to Spec(StepsRecord::class) { rs, s, e ->
        rs.filterIsInstance<StepsRecord>()
          .sumOf { it.count * fraction(it.startTime, it.endTime, s, e) }
          .toLong()
          .takeIf { it > 0L }
      },
      DistanceRecord.DISTANCE_TOTAL to Spec(DistanceRecord::class) { rs, s, e ->
        rs.filterIsInstance<DistanceRecord>()
          .sumOf { it.distance.inMeters * fraction(it.startTime, it.endTime, s, e) }
          .takeIf { it > 0.0 }
          ?.let { Length.meters(it) }
      },
      ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL to
        Spec(ActiveCaloriesBurnedRecord::class) { rs, s, e ->
          rs.filterIsInstance<ActiveCaloriesBurnedRecord>()
            .sumOf {
              it.energy.inKilocalories * fraction(it.startTime, it.endTime, s, e)
            }
            .takeIf { it > 0.0 }
            ?.let { Energy.kilocalories(it) }
        },
      TotalCaloriesBurnedRecord.ENERGY_TOTAL to
        Spec(TotalCaloriesBurnedRecord::class) { rs, s, e ->
          rs.filterIsInstance<TotalCaloriesBurnedRecord>()
            .sumOf {
              it.energy.inKilocalories * fraction(it.startTime, it.endTime, s, e)
            }
            .takeIf { it > 0.0 }
            ?.let { Energy.kilocalories(it) }
        },
      ElevationGainedRecord.ELEVATION_GAINED_TOTAL to
        Spec(ElevationGainedRecord::class) { rs, s, e ->
          rs.filterIsInstance<ElevationGainedRecord>()
            .sumOf { it.elevation.inMeters * fraction(it.startTime, it.endTime, s, e) }
            .takeIf { it > 0.0 }
            ?.let { Length.meters(it) }
        },

      // Series: over the SAMPLES, never the record boundary.
      SpeedRecord.SPEED_AVG to Spec(SpeedRecord::class) { rs, s, e ->
        samplesIn<SpeedRecord, SpeedRecord.Sample>(rs, s, e, { it.samples }, { it.time })
          .map { it.speed.inMetersPerSecond }
          .ifEmpty { null }
          ?.average()
          ?.let { Velocity.metersPerSecond(it) }
      },
      PowerRecord.POWER_AVG to Spec(PowerRecord::class) { rs, s, e ->
        samplesIn<PowerRecord, PowerRecord.Sample>(rs, s, e, { it.samples }, { it.time })
          .map { it.power.inWatts }
          .ifEmpty { null }
          ?.average()
          ?.let { Power.watts(it) }
      },
      HeartRateRecord.BPM_AVG to Spec(HeartRateRecord::class) { rs, s, e ->
        hr(rs, s, e).ifEmpty { null }?.average()?.toLong()
      },
      HeartRateRecord.BPM_MIN to Spec(HeartRateRecord::class) { rs, s, e ->
        hr(rs, s, e).minOrNull()
      },
      HeartRateRecord.BPM_MAX to Spec(HeartRateRecord::class) { rs, s, e ->
        hr(rs, s, e).maxOrNull()
      },
      RestingHeartRateRecord.BPM_AVG to Spec(RestingHeartRateRecord::class) { rs, s, e ->
        rs.filterIsInstance<RestingHeartRateRecord>()
          .filter { !it.time.isBefore(s) && it.time.isBefore(e) }
          .map { it.beatsPerMinute }
          .ifEmpty { null }
          ?.average()
          ?.toLong()
      },
    )
  }
}
