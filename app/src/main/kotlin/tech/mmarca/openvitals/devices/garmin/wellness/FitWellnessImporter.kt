package tech.mmarca.openvitals.devices.garmin.wellness

import androidx.health.connect.client.records.Record
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.contract.GarminWellnessRepository
import tech.mmarca.openvitals.devices.garmin.GarminCounterWatermarkStore
import tech.mmarca.openvitals.devices.garmin.GarminDownloadedFile
import tech.mmarca.openvitals.devices.garmin.GarminLog
import tech.mmarca.openvitals.domain.model.GarminWellnessMetric
import tech.mmarca.openvitals.domain.model.GarminWellnessSample
import tech.mmarca.openvitals.features.imports.applehealth.isDuplicateClientRecordFailure

/**
 * Runs the whole wellness import for one sync's downloaded FIT files:
 *
 *  1. Health Connect records (sleep, HRV, VO2 max, naps, monitoring
 *     summaries, Health Snapshot) through the same
 *     [AppleHealthImportRepository.insertImportedRecords] pipeline the Apple
 *     Health importer uses — deterministic `clientRecordId`s make a re-import
 *     upsert instead of duplicate.
 *  2. The cumulative counters, accumulated across EVERY file of the run and
 *     differenced once against the stored watermarks — a file only knows the
 *     minutes it holds, and an interval record needs the reading before its
 *     first one.
 *  3. The watch-only metrics (stress, Body Battery, sleep scores, …) into the
 *     app's own table via [GarminWellnessRepository] — Health Connect has no
 *     types for them.
 *
 * Mirrors the Flutter build's wellness half of `RouteBulkImportViewModel.
 * importRouteFiles` plus `GarminDeviceSyncPort._storeWatchOnlyMetrics`.
 * Activity-type files (4/5/6) are left alone here — their wellness passengers
 * (VO2 max, recovery time) are still extracted, but the exercise itself is not
 * imported in this milestone; the raw bytes stay in the Garmin file store.
 *
 * Throws when the write path itself is unavailable (no Health Connect,
 * permissions revoked mid-run) — the caller treats that as a failed sync and
 * re-downloads the files next time. A single undecodable FILE is tolerated and
 * skipped, like the bulk importer tolerates one bad file in a batch.
 */
@Singleton
class FitWellnessImporter @Inject constructor(
    private val importRepository: AppleHealthImportRepository,
    private val wellnessRepository: GarminWellnessRepository,
    private val watermarkStore: GarminCounterWatermarkStore,
) {

    suspend fun import(files: List<GarminDownloadedFile>) {
        if (files.isEmpty()) return

        val records = mutableListOf<Record>()
        var counters = FitMonitoringCounters()
        val watchOnly = mutableListOf<GarminWellnessSample>()

        for (file in files) {
            val wellness = try {
                parseGarminWellness(file.bytes, fileName = file.entry.type.label)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // A file the decoder rejects must not sink the night's data in
                // the files beside it.
                GarminLog.log(
                    "[GARMIN-IMPORT] undecodable ${file.entry.type.label} " +
                        "index=${file.entry.fileIndex}: $error",
                )
                continue
            }

            wellness.sleep?.let { records += fitSleepImportRecords(it) }
            wellness.hrv?.let { records += fitHrvImportRecords(it) }
            wellness.monitoring?.let { records += fitMonitoringImportRecords(it) }
            wellness.metrics?.let { records += fitMetricsImportRecords(it) }
            records += fitNapImportRecords(wellness.naps)
            wellness.healthSnapshot?.let { records += fitHealthSnapshotImportRecords(it) }

            // The cumulative counters are held back and mapped ONCE at the end
            // of the run: a file only knows the minutes it holds, and an
            // interval record needs the reading before its first one.
            wellness.monitoring?.let { counters = counters.merge(fitMonitoringCounters(it)) }

            watchOnly += watchOnlySamples(wellness)
        }

        writeRecords(records)
        writeCounters(counters)

        // Stored after the Health Connect writes, like the Flutter build: the
        // app's own table has no quota and its upsert is idempotent, so its
        // order matters less — but a failure here still fails the sync, so the
        // files are re-fetched rather than silently losing the series.
        wellnessRepository.upsert(watchOnly)
        if (watchOnly.isNotEmpty()) {
            GarminLog.log("[GARMIN-IMPORT] stored ${watchOnly.size} watch-only samples")
        }
    }

    /**
     * One batched insert; duplicate-clientRecordId rejections are retried
     * record by record and counted as success — the record is already there,
     * which is exactly what a deterministic id promises.
     */
    private suspend fun writeRecords(records: List<Record>) {
        // Several files can restate the same record (the watch re-offers a
        // file whose archive flag did not stick); the LAST occurrence wins,
        // matching the order Health Connect would have applied the upserts in.
        val deduped = records
            .associateBy { it.metadata.clientRecordId ?: it }
            .values
            .toList()
        if (deduped.isEmpty()) return
        try {
            importRepository.insertImportedRecords(deduped)
            GarminLog.log("[GARMIN-IMPORT] wrote ${deduped.size} Health Connect records")
            return
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (!error.isDuplicateClientRecordFailure()) throw error
        }
        // The batch is atomic, so one duplicate rejected all of it. Retry one
        // by one so the genuinely new records still land.
        var written = 0
        for (record in deduped) {
            try {
                importRepository.insertImportedRecords(listOf(record))
                written += 1
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!error.isDuplicateClientRecordFailure()) throw error
            }
        }
        GarminLog.log(
            "[GARMIN-IMPORT] wrote $written/${deduped.size} records (rest already present)",
        )
    }

    private suspend fun writeCounters(counters: FitMonitoringCounters) {
        if (counters.isEmpty) return
        val mapped = fitMonitoringCounterRecords(counters, previous = watermarkStore.load())
        if (mapped.records.isEmpty()) {
            // Nothing new to write, but the cursor still moved: the run may
            // have read past the previous watermark and found only movement
            // inside the still-filling bucket. Persisting it keeps an
            // idempotent re-sync from re-walking the same minutes, and it
            // cannot lose data because there was none to lose.
            watermarkStore.save(mapped.watermarks)
            return
        }
        writeRecords(mapped.records)
        // Stored only once the records are IN. A watermark for records that
        // were never written would skip those minutes for good — the next sync
        // would difference from a reading Health Connect never received.
        watermarkStore.save(mapped.watermarks)
        GarminLog.log(
            "[GARMIN-IMPORT] wrote ${mapped.records.size} intraday counter " +
                "records across ${mapped.watermarks.size} day(s)",
        )
    }

    /**
     * The watch-only metrics one decoded file carried, for the app's own
     * table. Port of the Flutter build's
     * `GarminDeviceSyncPort._storeWatchOnlyMetrics` inner loop.
     */
    private fun watchOnlySamples(wellness: FitWellness): List<GarminWellnessSample> = buildList {
        fun add(metric: GarminWellnessMetric, at: Instant, value: Long) {
            add(GarminWellnessSample(metric = metric, time = at, value = value))
        }

        // The metrics file: one snapshot, several unrelated numbers. VO2 max
        // is absent on purpose — Health Connect has a type for it, so it goes
        // down the import path with everything else it can hold.
        val metrics = wellness.metrics
        val metricsAt = metrics?.time
        if (metrics != null && metricsAt != null) {
            metrics.recoveryTimeMinutes?.let {
                add(GarminWellnessMetric.RECOVERY_TIME, metricsAt, it.toLong())
            }
            metrics.trainingReadiness?.let {
                add(GarminWellnessMetric.TRAINING_READINESS, metricsAt, it.toLong())
            }
            metrics.trainingLoadAcute?.let {
                add(GarminWellnessMetric.TRAINING_LOAD_ACUTE, metricsAt, it.toLong())
            }
            metrics.trainingLoadChronic?.let {
                add(GarminWellnessMetric.TRAINING_LOAD_CHRONIC, metricsAt, it.toLong())
            }
        }

        // daily_sleep: the watch's own nightly summary, which arrives in the
        // METRICS file rather than the sleep file. Keyed to the night's end,
        // which is the only instant the message carries.
        val daily = wellness.dailySleep
        val dailyAt = daily?.endTime
        if (daily != null && dailyAt != null) {
            daily.score?.let { add(GarminWellnessMetric.SLEEP_SCORE, dailyAt, it.toLong()) }
            daily.awakeDuration?.let { awake ->
                add(GarminWellnessMetric.SLEEP_AWAKE_SECONDS, dailyAt, awake.seconds)
                GarminLog.log(
                    "[FIT-SLEEP] watch awake_duration=${awake.toMinutes()}m " +
                        "for the night ending $dailyAt",
                )
            }
            daily.pressure?.let { add(GarminWellnessMetric.SLEEP_PRESSURE, dailyAt, it.toLong()) }
        }

        // Sleep Coach.
        val demand = wellness.sleepDemand
        val demandAt = demand?.time
        if (demand != null && demandAt != null) {
            demand.normal?.let {
                add(GarminWellnessMetric.SLEEP_NEED_NORMAL_MINUTES, demandAt, it.toMinutes())
            }
            demand.demand?.let {
                add(GarminWellnessMetric.SLEEP_NEED_MINUTES, demandAt, it.toMinutes())
            }
        }

        // The watch's own verdict on a night, keyed to when the night began.
        val sleep = wellness.sleep
        if (sleep != null) {
            sleep.overallScore?.let {
                add(GarminWellnessMetric.SLEEP_SCORE, sleep.start, it.toLong())
            }
            sleep.awakeningsCount?.let {
                add(GarminWellnessMetric.SLEEP_AWAKENINGS, sleep.start, it.toLong())
            }
        }

        // Health Snapshot stress / Body Battery. Stored under the same metrics
        // as the all-day series: they are the same quantity on the same scale,
        // just measured deliberately rather than passively, and the
        // (metric, time) key keeps them from colliding.
        val snapshot = wellness.healthSnapshot
        if (snapshot != null) {
            for ((at, value) in snapshot.stress) {
                add(GarminWellnessMetric.STRESS, at, value.toLong())
            }
            for ((at, value) in snapshot.bodyEnergy) {
                add(GarminWellnessMetric.BODY_ENERGY, at, value.toLong())
            }
        }

        val monitoring = wellness.monitoring ?: return@buildList
        for ((at, value) in monitoring.stress) {
            add(GarminWellnessMetric.STRESS, at, value.toLong())
        }
        for ((at, value) in monitoring.bodyEnergy) {
            add(GarminWellnessMetric.BODY_ENERGY, at, value.toLong())
        }
        for ((at, value) in monitoring.moderateMinutes) {
            add(GarminWellnessMetric.MODERATE_MINUTES, at, value.toLong())
        }
        for ((at, value) in monitoring.vigorousMinutes) {
            add(GarminWellnessMetric.VIGOROUS_MINUTES, at, value.toLong())
        }
    }
}
