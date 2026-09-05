package tech.mmarca.openvitals.data.local.bodyenergy

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import tech.mmarca.openvitals.data.local.vitalscache.VitalsSyncCursorEntity

/**
 * The Body Energy chain: day summaries, buckets, and the cursor row under
 * [BodyEnergyChainCursorKey]. No "most recent day before D" query: validity
 * depends on a per-date signature computed in Kotlin, so [daysBetween]
 * returns the window and the repository walks it.
 */
@Dao
interface BodyEnergyTimelineDao {

    @Query("SELECT * FROM body_energy_days WHERE epoch_day = :epochDay LIMIT 1")
    suspend fun day(epochDay: Long): BodyEnergyDayEntity?

    /** Summaries for `[startEpochDay, endEpochDay]`, oldest first. One query, no buckets. */
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

    /** How many buckets a day still has. A count: the caller only asks whether to protect it. */
    @Query("SELECT COUNT(*) FROM body_energy_buckets WHERE epoch_day = :epochDay")
    suspend fun countBucketsForDay(epochDay: Long): Int

    /**
     * Replaces one day atomically, so a crash cannot leave a summary that
     * disagrees with its last bucket. A full rewrite: cheap next to the reads.
     */
    @Transaction
    suspend fun upsertDay(summary: BodyEnergyDayEntity, buckets: List<BodyEnergyBucketEntity>) {
        deleteBucketsForDay(summary.epochDay)
        insertDay(summary)
        if (buckets.isNotEmpty()) insertBuckets(buckets)
    }

    /** Forward ripple: drop `[startEpochDay, endEpochDay]`. Their seeds no longer hold. */
    @Transaction
    suspend fun deleteDays(startEpochDay: Long, endEpochDay: Long) {
        if (endEpochDay < startEpochDay) return
        deleteBucketsBetween(startEpochDay, endEpochDay)
        deleteDaysBetween(startEpochDay, endEpochDay)
    }

    /** Retention: drop buckets before [epochDay], keeping the summaries. */
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

    /** Upserts the bookkeeping. Null fields are left untouched. */
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
