package tech.mmarca.openvitals.devices.garmin.wellness

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Power
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import tech.mmarca.openvitals.devices.garmin.FitCounterWatermark

/**
 * Maps decoded Garmin wellness FIT data onto Health Connect [Record]s. Every
 * `clientRecordId` scheme matches the Flutter build, so old records dedup.
 */

/** Health Connect file type for a Garmin sleep FIT file (`file_id.type`). */
const val FIT_FILE_TYPE_SLEEP: Int = 49

private fun importMetadata(clientRecordId: String): Metadata =
    Metadata.manualEntry(
        clientRecordId = clientRecordId,
        device = Device(type = Device.TYPE_PHONE),
    )

/**
 * Turns a [FitSleepSession] into a `SleepSessionRecord` import, or an empty
 * list if no stage mapped. The `clientRecordId` comes from the start, so a
 * re-import dedupes.
 */
fun fitSleepImportRecords(session: FitSleepSession): List<Record> {
    val stages = session.stages.mapNotNull { stage ->
        val mapped = sleepStageFor(stage.level) ?: return@mapNotNull null
        SleepSessionRecord.Stage(
            startTime = stage.start,
            endTime = stage.end,
            stage = mapped,
        )
    }
    if (stages.isEmpty()) return emptyList()
    return listOf(
        SleepSessionRecord(
            startTime = session.start,
            startZoneOffset = null,
            endTime = session.end,
            endZoneOffset = null,
            metadata = importMetadata("garmin_fit_sleep_${session.start.toEpochMilli()}"),
            title = "Sleep",
            notes = null,
            stages = stages,
        ),
    )
}

/** Turns a [FitHrvReading] into an RMSSD record. Deterministic id, so re-imports dedupe. */
fun fitHrvImportRecords(reading: FitHrvReading): List<Record> = listOf(
    HeartRateVariabilityRmssdRecord(
        time = reading.time,
        zoneOffset = null,
        heartRateVariabilityMillis = reading.rmssdMillis,
        metadata = importMetadata("garmin_fit_hrv_${reading.time.toEpochMilli()}"),
    ),
)

/** Turns the metrics file's VO2 max into a record. The rest has no Health Connect type. */
fun fitMetricsImportRecords(metrics: FitMetricsSummary): List<Record> {
    val time = metrics.time ?: return emptyList()
    val vo2Max = metrics.vo2Max ?: return emptyList()
    return listOf(
        Vo2MaxRecord(
            time = time,
            zoneOffset = null,
            metadata = importMetadata("garmin_fit_vo2max_${time.toEpochMilli()}"),
            vo2MillilitersPerMinuteKilogram = vo2Max,
            measurementMethod = Vo2MaxRecord.MEASUREMENT_METHOD_OTHER,
        ),
    )
}

/**
 * Turns a Health Snapshot's SpO2 and respiration into records. Ids are keyed
 * on the sample instant and namespaced apart from the all-day series.
 */
fun fitHealthSnapshotImportRecords(snapshot: FitHealthSnapshot): List<Record> = buildList {
    for ((at, percent) in snapshot.spo2) {
        add(
            OxygenSaturationRecord(
                time = at,
                zoneOffset = null,
                percentage = Percentage(percent.toDouble()),
                metadata = importMetadata("garmin_fit_hsa_spo2_${at.toEpochMilli()}"),
            ),
        )
    }
    for ((at, rate) in snapshot.respiration) {
        add(
            RespiratoryRateRecord(
                time = at,
                zoneOffset = null,
                rate = rate,
                metadata = importMetadata("garmin_fit_hsa_rr_${at.toEpochMilli()}"),
            ),
        )
    }
}

/**
 * Turns naps into `SleepSessionRecord` imports. A nap has no stages, so the
 * whole span is one light-sleep stage, as Gadgetbridge does.
 */
fun fitNapImportRecords(naps: List<FitNap>): List<Record> = buildList {
    for (nap in naps) {
        if (!nap.end.isAfter(nap.start)) continue
        add(
            SleepSessionRecord(
                startTime = nap.start,
                startZoneOffset = null,
                endTime = nap.end,
                endZoneOffset = null,
                metadata = importMetadata("garmin_fit_nap_${nap.start.toEpochMilli()}"),
                title = "Nap",
                notes = null,
                stages = listOf(
                    SleepSessionRecord.Stage(
                        startTime = nap.start,
                        endTime = nap.end,
                        stage = SleepSessionRecord.STAGE_TYPE_LIGHT,
                    ),
                ),
            ),
        )
    }
}

/** FIT `monitoring.distance` is raw / 100 metres. */
private const val FitMonitoringDistanceScale = 100.0

/**
 * Turns a monitoring file (type 32) into records: the per-file summaries and
 * the hourly HR and respiration series. Counters go through
 * [fitMonitoringCounterRecords], which needs every file of a sync.
 */
fun fitMonitoringImportRecords(m: FitMonitoringSummary): List<Record> = buildList {
    val rhrTime = m.restingHeartRateTime
    val rhrBpm = m.restingHeartRateBpm
    if (rhrTime != null && rhrBpm != null) {
        add(
            RestingHeartRateRecord(
                time = rhrTime,
                zoneOffset = null,
                beatsPerMinute = rhrBpm.toLong(),
                metadata = importMetadata("garmin_fit_resting_hr_${rhrTime.toEpochMilli()}"),
            ),
        )
    }
    val bmrTime = m.bmrTime
    val bmr = m.bmrKcalPerDay
    if (bmrTime != null && bmr != null) {
        add(
            BasalMetabolicRateRecord(
                time = bmrTime,
                zoneOffset = null,
                basalMetabolicRate = Power.kilocaloriesPerDay(bmr),
                metadata = importMetadata("garmin_fit_bmr_${bmrTime.toEpochMilli()}"),
            ),
        )
    }

    // One HR series record per hour, keyed on the bucket's first sample.
    // Keying on the hour let files synced minutes apart replace each other.
    for (bucket in bucketByHour(m.heartRateSamples) { it.first }.values) {
        val samples = bucket.sortedBy { it.first }
        val start = samples.first().first
        val last = samples.last().first
        val end = if (last.isAfter(start)) last else start.plusSeconds(1)
        add(
            HeartRateRecord(
                startTime = start,
                startZoneOffset = null,
                endTime = end,
                endZoneOffset = null,
                samples = samples.map { (at, bpm) ->
                    HeartRateRecord.Sample(time = at, beatsPerMinute = bpm.toLong())
                },
                metadata = importMetadata("garmin_fit_hr_${start.toEpochMilli()}"),
            ),
        )
    }

    // Respiration: one averaged reading per hour, keyed on its first sample.
    for (bucket in bucketByHour(m.respiration) { it.first }.values) {
        val readings = bucket.sortedBy { it.first }
        val avg = readings.sumOf { it.second } / readings.size
        val at = readings.first().first
        add(
            RespiratoryRateRecord(
                time = at,
                zoneOffset = null,
                rate = avg,
                metadata = importMetadata("garmin_fit_resp_${at.toEpochMilli()}"),
            ),
        )
    }
}

/**
 * The cumulative counters a monitoring file carried, kept apart so a caller
 * can accumulate them across every file of a sync. Movement between two files
 * lives only in the difference between them.
 */
class FitMonitoringCounters(
    val steps: List<FitMonitoringPoint> = emptyList(),
    val distance: List<FitMonitoringPoint> = emptyList(),
    val calories: List<FitMonitoringPoint> = emptyList(),
) {
    val isEmpty: Boolean
        get() = steps.isEmpty() && distance.isEmpty() && calories.isEmpty()

    fun merge(other: FitMonitoringCounters): FitMonitoringCounters =
        FitMonitoringCounters(
            steps = steps + other.steps,
            distance = distance + other.distance,
            calories = calories + other.calories,
        )
}

/** The counters [m] carried, for accumulating across a sync. */
fun fitMonitoringCounters(m: FitMonitoringSummary): FitMonitoringCounters =
    FitMonitoringCounters(
        steps = m.stepPoints,
        distance = m.distancePoints,
        calories = m.caloriePoints,
    )

/** The counter records, and the watermarks the caller must persist. */
class FitCounterImport(
    val records: List<Record>,
    /** By local `yyyy-mm-dd`, for the caller to store and hand back. */
    val watermarks: Map<String, FitCounterWatermark>,
)

/**
 * Turns the day-cumulative counters into intraday records, one per step the
 * counter took, spanning the snapshots that bracket it.
 *
 * Rules that keep the total honest: snapshots are summed across types; only
 * forward differences count; a day differences from where the day before
 * ended ([carryInto]); nothing is written at or before the watermark; records
 * never overlap, because Health Connect drops overlaps when it aggregates; a
 * zero difference writes nothing.
 */
fun fitMonitoringCounterRecords(
    counters: FitMonitoringCounters,
    previous: Map<String, FitCounterWatermark> = emptyMap(),
    zone: ZoneId = ZoneId.systemDefault(),
): FitCounterImport {
    val records = mutableListOf<Record>()
    val watermarks = mutableMapOf<String, FitCounterWatermark>()

    // Where the previous day left the counters, to carry across midnight.
    var carry = CounterCarry()

    for (day in counterDays(counters, zone)) {
        val mark = previous[day.key]
        val steps = dayTypedPoints(counters.steps, day, zone)
        val distance = dayTypedPoints(counters.distance, day, zone)
        val calories = dayTypedPoints(counters.calories, day, zone)
        val carried = carryInto(day, carry, previous)

        // Every instant any counter reported, so the three share a timeline.
        val instants = buildSet {
            steps.forEach { add(it.time) }
            distance.forEach { add(it.time) }
            calories.forEach { add(it.time) }
        }.sorted()
        if (instants.isEmpty()) continue

        // Per-type memory of the walk. A type never seen is adopted: with its
        // full value on a fresh day, silently under a pre-map watermark.
        val start: Instant
        val adoptSilently: Boolean
        val stepsContext: MutableMap<Int, Int>
        val distanceContext: MutableMap<Int, Int>
        val caloriesContext: MutableMap<Int, Int>
        if (mark != null) {
            start = mark.time
            adoptSilently = mark.stepsByType == null
            stepsContext = mark.stepsByType.orEmpty().toMutableMap()
            distanceContext = mark.distanceByType.orEmpty().toMutableMap()
            caloriesContext = mark.caloriesByType.orEmpty().toMutableMap()
        } else {
            start = day.start
            adoptSilently = carried.isLegacy
            stepsContext = dayStartContext(carried.stepsByType, steps)
            distanceContext = dayStartContext(carried.distanceByType, distance)
            caloriesContext = dayStartContext(carried.caloriesByType, calories)
        }

        val stepsAt = byInstant(steps)
        val distanceAt = byInstant(distance)
        val caloriesAt = byInstant(calories)

        // Deltas on a fixed grid from local midnight, so ids depend only on the clock.
        val buckets = mutableMapOf<Long, CounterDeltas>()
        var from = start

        // The bucket the previous sync stopped in was written half-filled.
        // Seed it from Health Connect so the upsert replaces the whole bucket.
        var seededBucket: Long? = null
        if (mark != null &&
            (mark.openBucketSteps > 0 || mark.openBucketDistance > 0 || mark.openBucketCalories > 0)
        ) {
            val seed = counterBucketStart(mark.time, day.start)
            seededBucket = seed
            // Re-opened at the instant the stored record claims, not the grid position.
            buckets[seed] = CounterDeltas(
                mark.openBucketStart ?: Instant.ofEpochMilli(seed),
            ).apply {
                this.steps = mark.openBucketSteps
                this.distance = mark.openBucketDistance
                this.calories = mark.openBucketCalories
                stretchTo(mark.time)
            }
        }

        for (at in instants) {
            // Already imported. Every sync re-reads the file it was halfway through.
            if (!at.isAfter(from)) continue

            // Movement over [from, at) belongs to the bucket the interval started in.
            val bucket = counterBucketStart(from, day.start)
            // The grid fixes the id; the start is the later of the grid position and
            // where this run resumed, so it cannot overlap the previous sync's record.
            buckets.getOrPut(bucket) {
                CounterDeltas(maxOf(Instant.ofEpochMilli(bucket), start))
            }.add(
                steps = instantDelta(stepsAt[at], stepsContext, adoptSilently),
                distance = instantDelta(distanceAt[at], distanceContext, adoptSilently),
                calories = instantDelta(caloriesAt[at], caloriesContext, adoptSilently),
                until = at,
            )

            from = at
        }

        // Clamp each bucket's end to the next occupied bucket so records never overlap.
        val ordered = buckets.keys.sorted()
        for (i in 0 until ordered.size - 1) {
            buckets.getValue(ordered[i]).clampEndTo(Instant.ofEpochMilli(ordered[i + 1]))
        }

        // The open bucket is emitted too; the next sync recomputes it from the seed.
        val openBucket = counterBucketStart(from, day.start)

        // One bucket per day goes out under the legacy day-keyed id, overwriting the
        // old whole-day record. Only a bucket never written under a grid id may.
        // See [FitCounterWatermark.legacyRetired].
        val emitted = buckets.keys.sorted()
        var legacyRetired = mark?.legacyRetired ?: false
        val closed = emitted.filter { it != openBucket && it != seededBucket }
        val retiringWith = if (legacyRetired || closed.isEmpty()) null else closed.first()

        for (bucket in emitted) {
            val key = if (bucket == retiringWith) day.key else bucket.toString()
            records.addAll(buckets.getValue(bucket).toRecords(key))
        }
        if (retiringWith != null) legacyRetired = true

        val open = buckets[openBucket]
        watermarks[day.key] = FitCounterWatermark(
            time = from,
            steps = contextSum(stepsContext),
            distance = contextSum(distanceContext),
            calories = contextSum(caloriesContext),
            stepsByType = stepsContext.toMap(),
            distanceByType = distanceContext.toMap(),
            caloriesByType = caloriesContext.toMap(),
            openBucketSteps = open?.steps ?: 0,
            openBucketDistance = open?.distance ?: 0,
            openBucketCalories = open?.calories ?: 0,
            openBucketStart = open?.start,
            legacyRetired = legacyRetired,
        )

        // A counter this day never reported keeps its carried value.
        carry = CounterCarry(
            day = day.localDate,
            stepsByType = if (steps.isEmpty()) carried.stepsByType else stepsContext.toMap(),
            distanceByType = if (distance.isEmpty()) carried.distanceByType else distanceContext.toMap(),
            caloriesByType = if (calories.isEmpty()) carried.caloriesByType else caloriesContext.toMap(),
        )
    }

    return FitCounterImport(records = records, watermarks = watermarks)
}

/**
 * One counter's net movement at one instant. Netted across types first, then
 * clamped, so a total moved between types does not count twice.
 */
private fun instantDelta(
    restated: List<FitMonitoringPoint>?,
    context: MutableMap<Int, Int>,
    adoptSilently: Boolean,
): Int {
    if (restated == null) return 0
    var net = 0
    for (point in restated) {
        val before = context[point.activityType]
        if (before != null) {
            net += point.value - before
        } else if (!adoptSilently) {
            net += point.value
        }
        context[point.activityType] = point.value
    }
    return max(0, net)
}

/**
 * What a day with no watermark starts from, per type. The watch resets after
 * local midnight, not at it: a type that starts below yesterday has reset, a
 * type absent from the first readings has not spoken yet.
 */
private fun dayStartContext(
    carried: Map<Int, Int>?,
    points: List<FitMonitoringPoint>,
): MutableMap<Int, Int> {
    if (carried == null) return mutableMapOf()
    val context = carried.toMutableMap()
    val seen = mutableSetOf<Int>()
    for (point in points) {
        if (!seen.add(point.activityType)) continue
        val before = context[point.activityType]
        if (before != null && point.value < before) context[point.activityType] = 0
    }
    return context
}

private fun contextSum(context: Map<Int, Int>): Int = context.values.sum()

private fun byInstant(points: List<FitMonitoringPoint>): Map<Instant, List<FitMonitoringPoint>> =
    points.groupBy { it.time }

/**
 * The counters at the end of the previous day. Null maps mean nothing typed
 * to carry: no history, or a pre-map watermark.
 */
private class CounterCarry(
    /** The local day these came off. A carry is only spent on the day that follows. */
    val day: LocalDate? = null,
    val stepsByType: Map<Int, Int>? = null,
    val distanceByType: Map<Int, Int>? = null,
    val caloriesByType: Map<Int, Int>? = null,
) {
    /** A day WAS carried but its watermark predates the per-type maps. */
    val isLegacy: Boolean
        get() = day != null && stepsByType == null
}

/**
 * What [day] differences its first readings against: the day before it, from
 * this run's walk or its watermark. The watch does not roll over at local
 * midnight. Only the immediately preceding day counts.
 */
private fun carryInto(
    day: MonitoringDay,
    running: CounterCarry,
    previous: Map<String, FitCounterWatermark>,
): CounterCarry {
    val yesterday = day.localDate.minusDays(1)
    if (running.day == yesterday) return running
    val mark = previous[dayKey(yesterday)] ?: return CounterCarry()
    return CounterCarry(
        day = yesterday,
        stepsByType = mark.stepsByType,
        distanceByType = mark.distanceByType,
        caloriesByType = mark.caloriesByType,
    )
}

/**
 * The grid one counter record covers. A fixed grid from local midnight keeps
 * ids stable across syncs, so Health Connect upserts. Fifteen minutes: per
 * instant is ~1440 records a day, an hour smears a walk.
 */
private val CounterBucket = Duration.ofMinutes(15)

private fun counterBucketStart(at: Instant, dayStart: Instant): Long {
    val elapsed = at.toEpochMilli() - dayStart.toEpochMilli()
    val size = CounterBucket.toMillis()
    val aligned = (if (elapsed < 0) 0 else elapsed / size) * size
    return dayStart.toEpochMilli() + aligned
}

/**
 * One grid bucket's movement. The grid fixes the id and start; the end
 * follows the data. Intervals land wholly in their starting bucket, so
 * consecutive buckets cannot overlap.
 */
private class CounterDeltas(val start: Instant) {
    var end: Instant = start
    var steps: Int = 0
    var distance: Int = 0
    var calories: Int = 0

    fun add(steps: Int, distance: Int, calories: Int, until: Instant) {
        this.steps += steps
        this.distance += distance
        this.calories += calories
        if (until.isAfter(end)) end = until
    }

    /** Pulls the end back to [limit]. Never forward, never before the start. */
    fun clampEndTo(limit: Instant) {
        if (end.isAfter(limit)) end = if (limit.isAfter(start)) limit else start
    }

    /** Extends the end to [until] without adding movement, for re-seeding. */
    fun stretchTo(until: Instant) {
        if (until.isAfter(end)) end = until
    }

    fun toRecords(key: String): List<Record> = buildList {
        if (steps > 0) {
            add(
                StepsRecord(
                    startTime = start,
                    startZoneOffset = null,
                    endTime = end,
                    endZoneOffset = null,
                    count = steps.toLong(),
                    metadata = importMetadata("garmin_fit_steps_$key"),
                ),
            )
        }
        if (distance > 0) {
            add(
                DistanceRecord(
                    startTime = start,
                    startZoneOffset = null,
                    endTime = end,
                    endZoneOffset = null,
                    distance = Length.meters(distance / FitMonitoringDistanceScale),
                    metadata = importMetadata("garmin_fit_distance_$key"),
                ),
            )
        }
        if (calories > 0) {
            add(
                ActiveCaloriesBurnedRecord(
                    startTime = start,
                    startZoneOffset = null,
                    endTime = end,
                    endZoneOffset = null,
                    energy = Energy.kilocalories(calories.toDouble()),
                    metadata = importMetadata("garmin_fit_active_cal_$key"),
                ),
            )
        }
    }
}

/** One local day a monitoring file touched, and the span to record it over. */
private class MonitoringDay(
    /** Stable `yyyy-mm-dd`, so every sync of the day writes the same id. */
    val key: String,
    /** The local calendar day. */
    val localDate: LocalDate,
    /** Local midnight. The record must span the whole day. */
    val start: Instant,
    /** The last sample seen for the day. */
    val end: Instant,
)

/** The local days the accumulated counters carried readings for. */
private fun counterDays(counters: FitMonitoringCounters, zone: ZoneId): List<MonitoringDay> =
    daysOf(listOf(counters.steps, counters.distance, counters.calories), zone)

private fun daysOf(series: List<List<FitMonitoringPoint>>, zone: ZoneId): List<MonitoringDay> {
    val lastByDay = mutableMapOf<LocalDate, Instant>()
    for (points in series) {
        for (point in points) {
            val day = point.time.atZone(zone).toLocalDate()
            val seen = lastByDay[day]
            if (seen == null || point.time.isAfter(seen)) lastByDay[day] = point.time
        }
    }

    return lastByDay.entries
        .map { (day, last) ->
            val start = day.atStartOfDay(zone).toInstant()
            // An interval record must not be empty.
            val end = if (last.isAfter(start)) last else start.plus(1, ChronoUnit.MINUTES)
            MonitoringDay(key = dayKey(day), localDate = day, start = start, end = end)
        }
        .sortedBy { it.key }
}

/** A local day as `yyyy-mm-dd`: the watermark key. */
private fun dayKey(day: LocalDate): String =
    "%04d-%02d-%02d".format(day.year, day.monthValue, day.dayOfMonth)

/**
 * One counter's points of [day], in time order. An untyped counter beside
 * typed ones is the day's total and is dropped; alone, it is the total and stays.
 */
private fun dayTypedPoints(
    points: List<FitMonitoringPoint>,
    day: MonitoringDay,
    zone: ZoneId,
): List<FitMonitoringPoint> {
    val ofDay = mutableListOf<FitMonitoringPoint>()
    var sawDeclaredType = false
    for (point in points) {
        if (point.time.atZone(zone).toLocalDate() != day.localDate) continue
        ofDay.add(point)
        if (point.activityType != UNKNOWN_FIT_ACTIVITY_TYPE) sawDeclaredType = true
    }
    ofDay.sortBy { it.time }
    if (!sawDeclaredType) return ofDay
    return ofDay.filter { it.activityType != UNKNOWN_FIT_ACTIVITY_TYPE }
}

/** Groups items into UTC-hour buckets keyed by the hour's epoch-ms. */
private fun <T> bucketByHour(items: List<T>, timeOf: (T) -> Instant): Map<Long, List<T>> =
    items.groupBy { timeOf(it).truncatedTo(ChronoUnit.HOURS).toEpochMilli() }

/** Garmin `sleep_level` to Health Connect stage. `unmeasurable` is dropped. */
private fun sleepStageFor(level: FitSleepLevel): Int? = when (level) {
    FitSleepLevel.AWAKE -> SleepSessionRecord.STAGE_TYPE_AWAKE
    FitSleepLevel.LIGHT -> SleepSessionRecord.STAGE_TYPE_LIGHT
    FitSleepLevel.DEEP -> SleepSessionRecord.STAGE_TYPE_DEEP
    FitSleepLevel.REM -> SleepSessionRecord.STAGE_TYPE_REM
    FitSleepLevel.UNMEASURABLE -> null
}
