package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordResponse
import androidx.health.connect.client.testing.AggregationResult
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Volume
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KClass

/**
 * Google's fake plus the aggregation it refuses to implement. Everything else delegates to it.
 *
 * Series records aggregate over the samples inside the window. Interval records are
 * pro-rated by how much of the record overlaps the window.
 *
 * The pro-rating rule is uncalibrated: real Health Connect behaviour at a window edge is
 * unmeasured. An aggregate assertion here proves our arithmetic has not changed, not what
 * a device would say.
 */
class AggregatingFakeHealthConnectClient(
    private val inner: FakeHealthConnectClient,
) : HealthConnectClient by inner {

    override suspend fun aggregate(request: AggregateRequest): AggregationResult {
        val (start, end) = bounds(request.internalTimeRange())
        return compute(request.internalMetrics(), start, end)
    }

    /** Bounds of every aggregateGroupByDuration request, in call order. */
    val groupByDurationRequestRanges = mutableListOf<Pair<Instant, Instant>>()

    override suspend fun aggregateGroupByDuration(
        request: AggregateGroupByDurationRequest,
    ): List<AggregationResultGroupedByDuration> {
        val metrics = request.internalMetrics()
        val (start, end) = bounds(request.internalTimeRange())
        val slice = request.internalSlicer()
        groupByDurationRequestRanges += start to end

        // One store read per record type per request, not per bucket.
        // Per-bucket reads made the corpus tests time out.
        val recordsByType = mutableMapOf<KClass<out Record>, List<Record>>()
        val recordsOf: suspend (KClass<out Record>) -> List<Record> = { type ->
            recordsByType.getOrPut(type) { readAll(type) }
        }

        val out = mutableListOf<AggregationResultGroupedByDuration>()
        var bucketStart = start
        while (bucketStart.isBefore(end)) {
            val bucketEnd = minOf(bucketStart.plus(slice), end)
            val result = compute(metrics, bucketStart, bucketEnd, recordsOf)
            // Health Connect omits an empty bucket rather than returning zero, and screens branch on that.
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

    /**
     * Read a record by the id the fake assigned it.
     * Google's fake keys `idsToRecords` by the pre-insert id, so `readRecord` by inserted id throws.
     */
    override suspend fun <T : Record> readRecord(
        recordType: KClass<T>,
        recordId: String,
    ): ReadRecordResponse<T> {
        val record = readAll(recordType).firstOrNull { it.metadata.id == recordId }
            ?: throw IllegalStateException(
                "No ${recordType.simpleName} with id $recordId. Health Connect answers a " +
                    "read of an id it does not hold with a failure, not an empty record.",
            )
        return ReadRecordResponse(record)
    }

    override suspend fun aggregateGroupByPeriod(
        request: AggregateGroupByPeriodRequest,
    ): List<AggregationResultGroupedByPeriod> =
        // Not emulated: period bucketing uses each record's zone offset, which we have not measured.
        // A loud failure beats a wrong number.
        throw NotImplementedError(
            "aggregateGroupByPeriod is not emulated: its bucket edges resolve against each " +
                "record's own zone offset, which is unmeasured. See the class doc.",
        )

    // The arithmetic.

    private suspend fun compute(
        metrics: Set<AggregateMetric<*>>,
        start: Instant,
        end: Instant,
        recordsOf: suspend (KClass<out Record>) -> List<Record> = { readAll(it) },
    ): AggregationResult {
        val values = mutableMapOf<AggregateMetric<Any>, Any>()
        val origins = mutableSetOf<DataOrigin>()

        for (metric in metrics) {
            val spec = SPECS[metric] ?: error(
                "No aggregation emulated for $metric. Add it to SPECS — otherwise a test is " +
                    "silently asserting against a metric nobody computed.",
            )
            val records = recordsOf(spec.recordType)
            if (records.isEmpty()) continue

            val value = spec.compute(records, start, end) ?: continue
            values[metric.erased()] = value
            records.forEach { origins.add(it.metadata.dataOrigin) }
        }
        return AggregationResult(dataOrigins = origins, metrics = values)
    }

    /** Every record of a type, unbounded on purpose. Overlap is decided on sample and interval times. */
    private suspend fun <T : Record> readAll(type: KClass<T>): List<T> =
        inner.readRecords(
            ReadRecordsRequest(
                recordType = type,
                timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH),
            ),
        ).records

    private fun bounds(filter: TimeRangeFilter): Pair<Instant, Instant> =
        (filter.startTime ?: Instant.EPOCH) to (filter.endTime ?: Instant.now())

    // AggregateRequest.metrics and .timeRangeFilter are `internal`, reachable only through the
    // mangled getter (`getMetrics$connect_client`). This throws with the name it looked for
    // if androidx renames them.

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

    // The metric table.

    private class Spec(
        val recordType: KClass<out Record>,
        val compute: (List<Record>, Instant, Instant) -> Any?,
    )

    private companion object {

        /** How much of an interval record lies inside the window. Takes instants because `IntervalRecord` is internal. */
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

            FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL to
                Spec(FloorsClimbedRecord::class) { rs, s, e ->
                    rs.filterIsInstance<FloorsClimbedRecord>()
                        .sumOf { it.floors * fraction(it.startTime, it.endTime, s, e) }
                        .takeIf { it > 0.0 }
                },
            WheelchairPushesRecord.COUNT_TOTAL to
                Spec(WheelchairPushesRecord::class) { rs, s, e ->
                    rs.filterIsInstance<WheelchairPushesRecord>()
                        .sumOf { it.count * fraction(it.startTime, it.endTime, s, e) }
                        .toLong()
                        .takeIf { it > 0L }
                },
            HydrationRecord.VOLUME_TOTAL to Spec(HydrationRecord::class) { rs, s, e ->
                rs.filterIsInstance<HydrationRecord>()
                    .sumOf { it.volume.inLiters * fraction(it.startTime, it.endTime, s, e) }
                    .takeIf { it > 0.0 }
                    ?.let { Volume.liters(it) }
            },

            // Series: over the samples. Speed hands back a raw Double because
            // connect-testing's AggregationResult() builder silently drops Velocity.
            SpeedRecord.SPEED_AVG to Spec(SpeedRecord::class) { rs, s, e ->
                samplesIn<SpeedRecord, SpeedRecord.Sample>(rs, s, e, { it.samples }, { it.time })
                    .map { it.speed.inMetersPerSecond }
                    .ifEmpty { null }
                    ?.average()
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
