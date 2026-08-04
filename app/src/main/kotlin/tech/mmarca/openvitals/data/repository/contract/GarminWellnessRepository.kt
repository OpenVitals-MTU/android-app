package tech.mmarca.openvitals.data.repository.contract

import java.time.Instant
import tech.mmarca.openvitals.domain.model.GarminWellnessMetric
import tech.mmarca.openvitals.domain.model.GarminWellnessSample

/**
 * Watch-only Garmin wellness series (stress, Body Battery, sleep score, …) —
 * the metrics Health Connect has no type for, held in the app's own Room table
 * as their system of record.
 *
 * The clean seam between the Garmin sync pipeline (which writes via [upsert]
 * after decoding FIT wellness files) and the readers that chart or derive from
 * these series. Deliberately thin over the DAO: no windowing, aggregation or
 * interpretation happens here.
 */
interface GarminWellnessRepository {

    /**
     * Upserts a batch. Re-syncing an overlapping window rewrites the same rows
     * rather than duplicating them — the `(metric, time)` primary key makes a
     * re-import idempotent.
     */
    suspend fun upsert(samples: List<GarminWellnessSample>)

    /** Samples for [metric] in `[from, to)`, oldest first. */
    suspend fun samplesBetween(metric: GarminWellnessMetric, from: Instant, to: Instant): List<GarminWellnessSample>

    /** The most recent sample for [metric], or null when none has been synced. */
    suspend fun latest(metric: GarminWellnessMetric): GarminWellnessSample?

    /** Total rows held for [metric], for diagnostics. */
    suspend fun countFor(metric: GarminWellnessMetric): Long
}
