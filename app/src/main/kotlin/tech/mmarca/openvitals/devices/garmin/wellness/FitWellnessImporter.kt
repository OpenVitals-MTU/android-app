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
 * Runs the wellness import for one sync's FIT files: Health Connect records
 * through the shared insert pipeline, the cumulative counters accumulated
 * across every file and differenced once against the watermarks, and the
 * watch-only metrics into [GarminWellnessRepository]. Activity files are
 * left to the activity importer.
 *
 * Throws when the write path is unavailable; a single bad file is skipped.
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
                // A rejected file must not sink the files beside it.
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

            // Counters are mapped once at the end: an interval needs the reading before it.
            wellness.monitoring?.let { counters = counters.merge(fitMonitoringCounters(it)) }

            watchOnly += watchOnlySamples(wellness)
        }

        writeRecords(records)
        writeCounters(counters)

        // After the Health Connect writes, like the Flutter build. A failure still fails the sync.
        wellnessRepository.upsert(watchOnly)
        if (watchOnly.isNotEmpty()) {
            GarminLog.log("[GARMIN-IMPORT] stored ${watchOnly.size} watch-only samples")
        }
    }

    /** One batched insert. Duplicate-id rejections are retried one by one and count as success. */
    private suspend fun writeRecords(records: List<Record>) {
        // Several files can restate the same record; the last occurrence wins.
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
        // The batch is atomic; retry one by one so new records still land.
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
            // Nothing new, but the cursor moved. Persisting it keeps a re-sync
            // from re-walking the same minutes, and nothing is lost.
            watermarkStore.save(mapped.watermarks)
            return
        }
        writeRecords(mapped.records)
        // Only once the records are in, or those minutes would be skipped for good.
        watermarkStore.save(mapped.watermarks)
        GarminLog.log(
            "[GARMIN-IMPORT] wrote ${mapped.records.size} intraday counter " +
                "records across ${mapped.watermarks.size} day(s)",
        )
    }

    /** The watch-only metrics one decoded file carried. */
    private fun watchOnlySamples(wellness: FitWellness): List<GarminWellnessSample> = buildList {
        fun add(metric: GarminWellnessMetric, at: Instant, value: Long) {
            add(GarminWellnessSample(metric = metric, time = at, value = value))
        }

        // VO2 max is absent on purpose: Health Connect has a type for it.
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

        // daily_sleep arrives in the metrics file. Keyed to the night's end.
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

        // Health Snapshot stress and Body Battery: the same quantity as the all-day
        // series, measured deliberately. The (metric, time) key keeps them apart.
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
