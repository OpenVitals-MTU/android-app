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
 * Maps decoded Garmin wellness FIT data onto Health Connect [Record]s — the
 * same write pipeline (`AppleHealthImportRepository.insertImportedRecords`)
 * the Apple Health importer uses. Port of the Flutter build's
 * `fit_wellness_import.dart`; every `clientRecordId` scheme is byte-identical
 * so records synced by the Flutter build dedup against these.
 */

/** Health Connect file type for a Garmin sleep FIT file (`file_id.type`). */
const val FIT_FILE_TYPE_SLEEP: Int = 49

private fun importMetadata(clientRecordId: String): Metadata =
    Metadata.manualEntry(
        clientRecordId = clientRecordId,
        device = Device(type = Device.TYPE_PHONE),
    )

/**
 * Turns a decoded [FitSleepSession] into a `SleepSessionRecord` import, or an
 * empty list if no stage mapped to a Health Connect stage.
 *
 * The `clientRecordId` is derived from the session start so a re-import of the
 * same export dedupes instead of duplicating the night (Health Connect keys
 * upserts on `clientRecordId`).
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

/**
 * Turns a decoded [FitHrvReading] into a `HeartRateVariabilityRmssdRecord`
 * import. Deterministic `clientRecordId` so a re-import dedupes.
 */
fun fitHrvImportRecords(reading: FitHrvReading): List<Record> = listOf(
    HeartRateVariabilityRmssdRecord(
        time = reading.time,
        zoneOffset = null,
        heartRateVariabilityMillis = reading.rmssdMillis,
        metadata = importMetadata("garmin_fit_hrv_${reading.time.toEpochMilli()}"),
    ),
)

/**
 * Turns the metrics file's VO2 max into a `Vo2MaxRecord` import.
 *
 * Only VO2 max: recovery time, training readiness and training load have no
 * Health Connect type and go to the app's own table instead.
 */
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
 * Turns a Health Snapshot's SpO2 and respiration samples into Health Connect
 * records. Its stress and Body Battery have no Health Connect type and go to
 * the app's own table instead.
 *
 * The `clientRecordId`s are keyed on the sample instant, so a re-import of the
 * same recording overwrites rather than duplicating — and they are namespaced
 * apart from the all-day series, which is a genuinely different measurement of
 * the same quantity and must not overwrite it.
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
 * Turns daytime naps into `SleepSessionRecord` imports.
 *
 * Deliberately stage-less: the nap message bounds the sleep but carries no
 * stage breakdown, and inventing one would put fabricated stages next to the
 * measured ones from a night.
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
                stages = emptyList(),
            ),
        )
    }
}

/** FIT `monitoring.distance` is in centimetres-of-a-metre (raw ÷ 100 = metres). */
private const val FitMonitoringDistanceScale = 100.0

/**
 * Turns a monitoring file (type 32) into its Health Connect records: the
 * one-per-file summaries (resting HR, BMR), the HR and respiration series
 * aggregated to **hourly**, and NOT the cumulative counters — see
 * [fitMonitoringCounterRecords], which cannot read them a file at a time.
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

    // HR — one series record per hour, samples packed in.
    //
    // Keyed on the bucket's FIRST SAMPLE, not on the hour. Keying on the hour
    // assumed one file per day, so no two files could ever touch the same
    // hour. A watch sync breaks that: it delivers a fresh file every few
    // minutes, so several files land in one hour and, sharing a
    // clientRecordId, each REPLACED the last — an hour of heart rate
    // collapsing to whichever sliver synced most recently. First-sample keying
    // stays idempotent for a re-imported file (same samples, same key) while
    // letting successive files coexist.
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

    // Respiration — one averaged reading per hour bucket, keyed and timed on
    // its first sample for the same reason as HR above. Stamping it at the top
    // of the hour additionally made every file in that hour claim the same
    // instant.
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
 * The cumulative step / distance / active-calorie counters a monitoring file
 * carried, kept apart from the rest so a caller can accumulate them across
 * every file of a sync before mapping.
 *
 * Everything else in a monitoring file reads a file at a time: a heart-rate
 * bucket is complete in the file that holds it. These are not — they are
 * day-cumulative, and what happened between the last snapshot of one file and
 * the first of the next lives in NEITHER file's own numbers, only in the
 * difference between them.
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
    /** By `yyyy-mm-dd` local day, for the caller to store and hand back next time. */
    val watermarks: Map<String, FitCounterWatermark>,
)

/**
 * Turns the day-cumulative counters into INTRADAY Health Connect records: one
 * per step the counter actually took, spanning the minutes between the two
 * snapshots that bracket it.
 *
 * A single record per day is what a cumulative counter most obviously maps
 * to, and it is what this wrote first — but it says only how far you walked,
 * never when, so Health Connect drew a day's steps as one straight ramp from
 * midnight to now. The watch samples the counters about once a minute, so the
 * shape is there to be read; it just has to be read as differences.
 *
 * The rules that keep the total honest:
 *
 *  * The snapshots are the per-instant sums across activity types, so a total
 *    moved between buckets never shows up as a step taken.
 *  * Only forward differences are recorded. The counters roll over, and a
 *    rollover is not a walk backwards.
 *  * A day differences from where the day before it ended, NOT from zero —
 *    see [carryInto]. The watch does not roll its counters over at local
 *    midnight.
 *  * Nothing is written for a snapshot at or before the previous watermark:
 *    those minutes are already in Health Connect.
 *  * Records never OVERLAP, across syncs as well as within one. Health Connect
 *    discards the overlapping span when it aggregates, so two records that
 *    share a minute report less between them than either claims — a day read
 *    889 while its own records summed to 1007. A record's end follows the data
 *    and can run past later grid slots, so a run that resumes inside one of
 *    those slots starts its first record at the resume point rather than at the
 *    slot's edge.
 *  * A zero difference writes no record. Standing still is not an event, and
 *    a night of them would bury the day in empty entries.
 */
fun fitMonitoringCounterRecords(
    counters: FitMonitoringCounters,
    previous: Map<String, FitCounterWatermark> = emptyMap(),
    zone: ZoneId = ZoneId.systemDefault(),
): FitCounterImport {
    val records = mutableListOf<Record>()
    val watermarks = mutableMapOf<String, FitCounterWatermark>()

    // Where the walk left the counters on the day just mapped, so the next one
    // can carry across midnight. Days come out of [counterDays] in order,
    // which is what makes this the day before the one being mapped.
    var carry = CounterCarry()

    for (day in counterDays(counters, zone)) {
        val mark = previous[day.key]
        val steps = dayTypedPoints(counters.steps, day, zone)
        val distance = dayTypedPoints(counters.distance, day, zone)
        val calories = dayTypedPoints(counters.calories, day, zone)
        val carried = carryInto(day, carry, previous)

        // The instants any counter reported, so the three stay on one timeline.
        val instants = buildSet {
            steps.forEach { add(it.time) }
            distance.forEach { add(it.time) }
            calories.forEach { add(it.time) }
        }.sorted()
        if (instants.isEmpty()) continue

        // The walk's memory of each counter, by activity type. A restated
        // type's delta is its value against what the map holds; a type the map
        // has never seen is ADOPTED — with its full value where nothing was
        // ever counted before it (a fresh day, a lost watermark), and silently
        // where a watermark from before the maps existed makes "already
        // counted or not" unknowable. Silent adoption loses at most the
        // minutes since that type's last restatement, once; counting it could
        // re-write the whole day.
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

        // Deltas folded onto a fixed grid anchored at local midnight, so a
        // record's identity is a pure function of its wall clock.
        val buckets = mutableMapOf<Long, CounterDeltas>()
        var from = start

        // The bucket the previous sync stopped inside was written half-filled
        // (see [FitCounterWatermark.openBucketSteps]). Seed it with what is
        // already in Health Connect, so the deltas this run folds in produce
        // the WHOLE bucket and the upsert replaces the half rather than losing
        // it.
        var seededBucket: Long? = null
        if (mark != null &&
            (mark.openBucketSteps > 0 || mark.openBucketDistance > 0 || mark.openBucketCalories > 0)
        ) {
            val seed = counterBucketStart(mark.time, day.start)
            seededBucket = seed
            // Re-opened at the instant the record already in Health Connect
            // claims, not at the grid position: that record may itself have
            // begun mid-bucket, and re-writing it from the grid would widen it
            // back over its predecessor.
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
            // Already imported. Not an error — every sync re-reads the file it
            // was halfway through, and the watch re-offers a file whose
            // archive flag did not stick.
            if (!at.isAfter(from)) continue

            // The movement accrued over [from, at), so it belongs to the
            // bucket the interval STARTED in — not the one it ended in, which
            // would push a walk forward by up to a bucket every time.
            val bucket = counterBucketStart(from, day.start)
            // The grid fixes the record's ID; its START is the later of the
            // grid position and where this run resumed. Only the first bucket
            // of a run can differ, and it is exactly the one that must: the
            // previous sync's last record ran to [start], so beginning this one
            // at the grid slot CONTAINING [start] would overlap it — and Health
            // Connect drops the overlap when it aggregates, silently shortening
            // the day by up to a bucket per sync.
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

        // An interval that starts in one bucket can end in the next, so a
        // bucket's data-driven end can run past its successor's start. Clamp
        // each to the next OCCUPIED bucket: that keeps records non-overlapping
        // without shortening the sparse case, where the gap to the next bucket
        // is real.
        val ordered = buckets.keys.sorted()
        for (i in 0 until ordered.size - 1) {
            buckets.getValue(ordered[i]).clampEndTo(Instant.ofEpochMilli(ordered[i + 1]))
        }

        // The bucket the walk stopped inside stays open: the next sync
        // recomputes it in full from the watermark's seed. Every bucket is
        // emitted, the open one included — that is what saves the final bucket
        // of a day, which no later sync would ever come back to close.
        val openBucket = counterBucketStart(from, day.start)

        // One bucket per day is written under the legacy day-keyed id, so that
        // it OVERWRITES the pre-intraday whole-day record instead of stacking
        // beside it. Only a bucket never yet written under its grid id can
        // retire it: the open bucket goes out under a grid id this run, and
        // the seeded one did last run, so day-keying either would leave the
        // grid-id record standing beside the day-keyed one — the double count
        // this exists to prevent. The id is handed out once, latched — see
        // [FitCounterWatermark.legacyRetired].
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

        // A counter this day never reported keeps whatever was carried into it.
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
 * One counter's net movement at one instant, against [context].
 *
 * Netted across every type restated at the instant, THEN clamped: the watch
 * moves a total from one type to another and zeroes the one it left, and only
 * same-instant netting keeps a transfer from counting twice. A negative net —
 * the day-close rollover — clamps to nothing, and the context still adopts the
 * new lows so what follows counts from there.
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
 * What a day with no watermark of its own starts from, per type.
 *
 * The watch resets its counters when it closes the monitoring day, some time
 * after local midnight — not at it. So a type whose first restatement of the
 * day is BELOW where yesterday left it has been reset, and its readings are
 * the day's own accrual; a type merely absent from the first readings has
 * said nothing yet, and yesterday's value stands (delta-neutral until it
 * speaks). This per-type distinction is what tells a real rollover from a
 * partial first reading — comparing summed totals could not, and turned
 * yesterday's steps into today's.
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
 * What the counters read at the end of the day before the one being mapped.
 *
 * Null maps mean there is nothing typed to carry: either no history at all
 * (the first day of a run — the first reading IS the day's accrual), or a
 * watermark from before the per-type maps existed (see the adopt rule).
 */
private class CounterCarry(
    /**
     * The local day these came off, so a carry can only be spent on the day
     * that actually follows it.
     */
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
 * What [day] should difference its first readings against.
 *
 * The counters do NOT roll over at local midnight — the watch closes its
 * monitoring day after it has finalised the night — so a morning sync carries
 * messages timestamped today whose counters are still yesterday's running
 * totals. A day therefore starts from where the day before it ended: this
 * run's own walk when it mapped that day, and otherwise the watermark that
 * day was left at. Only the immediately preceding day counts — across a gap
 * the counter has certainly rolled over.
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
 * The grid one counter record covers.
 *
 * Records are written on a fixed grid from local midnight rather than on the
 * intervals a particular sync happened to see. The interval boundaries depend
 * on which files the watch offered and where the last sync stopped, so an id
 * derived from them changes between runs: a re-sync re-partitions the day and
 * every record after the first lands BESIDE the previous run's rather than
 * replacing it. A grid position is a property of the clock, so the same
 * minutes always produce the same id and Health Connect upserts.
 *
 * Fifteen minutes: the watch reports about once a minute, so per-instant
 * records would be ~1440 a day per counter, while an hour is coarse enough to
 * smear a walk across a lunch break.
 */
private val CounterBucket = Duration.ofMinutes(15)

private fun counterBucketStart(at: Instant, dayStart: Instant): Long {
    val elapsed = at.toEpochMilli() - dayStart.toEpochMilli()
    val size = CounterBucket.toMillis()
    val aligned = (if (elapsed < 0) 0 else elapsed / size) * size
    return dayStart.toEpochMilli() + aligned
}

/**
 * One grid bucket's accumulated counter movement.
 *
 * The grid fixes the record's IDENTITY and its start; the span still follows
 * the data, running to the end of the last interval folded in. Pinning the
 * end to the grid too would claim a first-sync-of-the-day reading of 8,000
 * steps happened in the first quarter hour after midnight. Intervals are
 * contiguous and each lands wholly in the bucket it started in, so
 * consecutive buckets still cannot overlap.
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

    /**
     * Pulls the end back to [limit] when the last interval folded in ran past
     * the next occupied bucket. Never pushes it forward, and never before the
     * start.
     */
    fun clampEndTo(limit: Instant) {
        if (end.isAfter(limit)) end = if (limit.isAfter(start)) limit else start
    }

    /**
     * Extends the end to [until] without adding movement — for re-seeding a
     * half-written bucket with the span its record already claims.
     */
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
    /**
     * Stable `yyyy-mm-dd`, so every sync of the same day writes the same
     * `clientRecordId` and Health Connect upserts instead of accumulating.
     */
    val key: String,
    /** The local calendar day. */
    val localDate: LocalDate,
    /**
     * Local midnight. The counter is the whole day's running total, so the
     * record has to span the whole day or Health Connect would attribute the
     * day's steps to whatever few minutes the file happened to cover.
     */
    val start: Instant,
    /** The last sample seen for the day — the total is only known up to here. */
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
            // An interval record must not be empty: a file whose only sample
            // sits at local midnight would otherwise produce start == end.
            val end = if (last.isAfter(start)) last else start.plus(1, ChronoUnit.MINUTES)
            MonitoringDay(key = dayKey(day), localDate = day, start = start, end = end)
        }
        .sortedBy { it.key }
}

/**
 * A local day as `yyyy-mm-dd` — the watermark key, and what a day's records
 * are identified by.
 */
private fun dayKey(day: LocalDate): String =
    "%04d-%02d-%02d".format(day.year, day.monthValue, day.dayOfMonth)

/**
 * One counter's points of [day], in time order, with the untyped rule applied:
 * a counter naming no activity beside typed ones is the same day's total under
 * a name of its own — counting it beside them counts those steps twice — but
 * when the file declared no type anywhere, the untyped counter IS the total
 * and stays.
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

/**
 * Garmin `sleep_level` → Health Connect `SleepSessionRecord.Stage`.
 * `unmeasurable` has no Health Connect stage, so it is dropped (the gap
 * between stages simply carries no classification).
 */
private fun sleepStageFor(level: FitSleepLevel): Int? = when (level) {
    FitSleepLevel.AWAKE -> SleepSessionRecord.STAGE_TYPE_AWAKE
    FitSleepLevel.LIGHT -> SleepSessionRecord.STAGE_TYPE_LIGHT
    FitSleepLevel.DEEP -> SleepSessionRecord.STAGE_TYPE_DEEP
    FitSleepLevel.REM -> SleepSessionRecord.STAGE_TYPE_REM
    FitSleepLevel.UNMEASURABLE -> null
}
