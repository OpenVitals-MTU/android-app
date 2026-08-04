package tech.mmarca.openvitals.data.local.bodyenergy

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import tech.mmarca.openvitals.data.local.vitalscache.VitalsSyncCursorEntity

/**
 * The Body Energy chain: day summaries, their 5-minute buckets, and the shared
 * sync cursor row keyed by [BodyEnergyChainCursorKey].
 *
 * Deliberately no SQL "most recent stored day at or before D": whether a row is
 * usable depends on the signature computed for *that row's own date*, which is a
 * Kotlin-side calculation, so such a query would routinely hand back rows the
 * caller must reject and loop straight back into SQL. [daysBetween] answers the
 * whole lookback window in one query and the walk lives in the repository, where
 * the signature knowledge is.
 */
@Dao
interface BodyEnergyTimelineDao {

    @Query("SELECT * FROM body_energy_days WHERE epoch_day = :epochDay LIMIT 1")
    suspend fun day(epochDay: Long): BodyEnergyDayEntity?

    /**
     * Summaries for `[startEpochDay, endEpochDay]`, oldest first. The chain
     * walk-back reads its whole lookback window with this in ONE query and never
     * touches a bucket.
     */
    @Query(
        "SELECT * FROM body_energy_days " +
            "WHERE epoch_day BETWEEN :startEpochDay AND :endEpochDay " +
            "ORDER BY epoch_day",
    )
    suspend fun daysBetween(startEpochDay: Long, endEpochDay: Long): List<BodyEnergyDayEntity>

    @Query("SELECT * FROM body_energy_buckets WHERE epoch_day = :epochDay ORDER BY time_millis")
    suspend fun bucketsForDay(epochDay: Long): List<BodyEnergyBucketEntity>

    @Query("SELECT COUNT(*) FROM body_energy_days")
    suspend fun countDays(): Int

    /**
     * How many buckets a day still has — a count rather than a read, because the
     * only caller is asking whether a stored day is worth protecting from an
     * empty recompute, not what is in it.
     */
    @Query("SELECT COUNT(*) FROM body_energy_buckets WHERE epoch_day = :epochDay")
    suspend fun countBucketsForDay(epochDay: Long): Int

    /**
     * Replace one day atomically: its old buckets go, the new ones land, and the
     * summary is upserted — all in one transaction, so a crash mid-write can
     * never leave a summary whose `end_score` disagrees with its last bucket.
     *
     * A full rewrite even when only the tail changed, which recomputing today
     * mostly is. That is deliberate: a whole 288-bucket day costs a couple of
     * milliseconds against the ~8 Health Connect reads that had to happen first
     * to produce those buckets. Writing only the changed tail would have to diff
     * against what is stored, and would trade a transaction that cannot
     * half-apply for one that can.
     */
    @Transaction
    suspend fun upsertDay(summary: BodyEnergyDayEntity, buckets: List<BodyEnergyBucketEntity>) {
        deleteBucketsForDay(summary.epochDay)
        insertDay(summary)
        if (buckets.isNotEmpty()) insertBuckets(buckets)
    }

    /**
     * Forward ripple: drop `[startEpochDay, endEpochDay]` from both tables.
     * Recomputing a day changes the seed of every day after it, so those days'
     * stored scores are claims about a chain that no longer exists.
     */
    @Transaction
    suspend fun deleteDays(startEpochDay: Long, endEpochDay: Long) {
        if (endEpochDay < startEpochDay) return
        deleteBucketsBetween(startEpochDay, endEpochDay)
        deleteDaysBetween(startEpochDay, endEpochDay)
    }

    /**
     * Retention: drop buckets strictly before [epochDay], keeping the summaries
     * so the chain and any long-range daily chart survive.
     */
    @Query("DELETE FROM body_energy_buckets WHERE epoch_day < :epochDay")
    suspend fun purgeBucketsBefore(epochDay: Long)

    /** Everything, plus the cursor — the algorithm/calibration-change reset. */
    @Transaction
    suspend fun purgeAll() {
        deleteAllBuckets()
        deleteAllDays()
        deleteCursor(BodyEnergyChainCursorKey)
    }

    @Query("SELECT * FROM vitals_sync_cursors WHERE metric = :metric LIMIT 1")
    suspend fun cursor(metric: String): VitalsSyncCursorEntity?

    /**
     * Upsert the chain's bookkeeping. Each field is left untouched when null, so
     * recording a completed pass cannot clear the stored signature.
     */
    @Transaction
    suspend fun writeChainCursor(globalSignature: String?, lastPassMillis: Long?) {
        val existing = cursor(BodyEnergyChainCursorKey)
        insertCursor(
            VitalsSyncCursorEntity(
                metric = BodyEnergyChainCursorKey,
                changesToken = globalSignature ?: existing?.changesToken,
                lastFullSyncMillis = lastPassMillis ?: existing?.lastFullSyncMillis,
            ),
        )
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: BodyEnergyDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuckets(buckets: List<BodyEnergyBucketEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCursor(cursor: VitalsSyncCursorEntity)

    @Query("DELETE FROM body_energy_buckets WHERE epoch_day = :epochDay")
    suspend fun deleteBucketsForDay(epochDay: Long)

    @Query("DELETE FROM body_energy_buckets WHERE epoch_day BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun deleteBucketsBetween(startEpochDay: Long, endEpochDay: Long)

    @Query("DELETE FROM body_energy_days WHERE epoch_day BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun deleteDaysBetween(startEpochDay: Long, endEpochDay: Long)

    @Query("DELETE FROM body_energy_buckets")
    suspend fun deleteAllBuckets()

    @Query("DELETE FROM body_energy_days")
    suspend fun deleteAllDays()

    @Query("DELETE FROM vitals_sync_cursors WHERE metric = :metric")
    suspend fun deleteCursor(metric: String)
}
