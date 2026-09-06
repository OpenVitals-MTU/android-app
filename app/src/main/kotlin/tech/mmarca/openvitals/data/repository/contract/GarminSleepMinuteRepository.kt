package tech.mmarca.openvitals.data.repository.contract

import java.time.Instant
import tech.mmarca.openvitals.domain.model.GarminSleepMinute

/**
 * Per-minute sleep input from watches that do not stage sleep. A thin seam
 * over Room: Health Connect has no record type for per-minute movement, and
 * the estimator needs a whole night, which arrives across several syncs.
 */
interface GarminSleepMinuteRepository {

    /** Upserts a batch. The minute is the key, so a re-import is idempotent. */
    suspend fun upsert(minutes: List<GarminSleepMinute>)

    /** Minutes in `[from, to)`, oldest first. */
    suspend fun minutesBetween(from: Instant, to: Instant): List<GarminSleepMinute>

    /** Drops minutes older than [before]. */
    suspend fun pruneBefore(before: Instant)
}
