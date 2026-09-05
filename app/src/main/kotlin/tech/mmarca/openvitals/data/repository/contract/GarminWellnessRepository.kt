package tech.mmarca.openvitals.data.repository.contract

import java.time.Instant
import tech.mmarca.openvitals.domain.model.GarminWellnessMetric
import tech.mmarca.openvitals.domain.model.GarminWellnessSample

/**
 * Watch-only Garmin wellness series, held in Room as their system of
 * record. Thin over the DAO: no windowing or aggregation here.
 */
interface GarminWellnessRepository {

    /** Upserts a batch. The `(metric, time)` key makes a re-import idempotent. */
    suspend fun upsert(samples: List<GarminWellnessSample>)

    /** Samples for [metric] in `[from, to)`, oldest first. */
    suspend fun samplesBetween(metric: GarminWellnessMetric, from: Instant, to: Instant): List<GarminWellnessSample>

    /** The most recent sample for [metric], or null when none has been synced. */
    suspend fun latest(metric: GarminWellnessMetric): GarminWellnessSample?

    /** Total rows held for [metric], for diagnostics. */
    suspend fun countFor(metric: GarminWellnessMetric): Long
}
