package tech.mmarca.openvitals.data.local.vitalscache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface VitalsDailyCacheDao {

    @Query(
        "SELECT * FROM vitals_daily_aggregates " +
            "WHERE metric = :metric AND epoch_day BETWEEN :fromEpochDay AND :toEpochDay " +
            "ORDER BY epoch_day",
    )
    suspend fun aggregatesBetween(metric: String, fromEpochDay: Long, toEpochDay: Long): List<VitalsDailyAggregateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDay(row: VitalsDailyAggregateEntity)

    @Query("DELETE FROM vitals_daily_aggregates WHERE metric = :metric AND epoch_day = :epochDay")
    suspend fun deleteDay(metric: String, epochDay: Long)

    /** Atomic wipe-and-reload of one metric's rows, for a full rebuild. */
    @Transaction
    suspend fun replaceMetric(metric: String, rows: List<VitalsDailyAggregateEntity>) {
        deleteMetricRows(metric)
        insertRows(rows)
    }

    /** Drops one metric's rows AND cursor — used to purge a legacy cache key. */
    @Transaction
    suspend fun purgeMetric(metric: String) {
        deleteMetricRows(metric)
        deleteCursor(metric)
    }

    @Query("SELECT * FROM vitals_sync_cursors WHERE metric = :metric")
    suspend fun cursor(metric: String): VitalsSyncCursorEntity?

    /** Records a completed full rebuild: the fresh token plus its wall-clock stamp. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun writeFullSync(cursor: VitalsSyncCursorEntity)

    /** Advances only the token, preserving the last full-sync stamp. */
    @Transaction
    suspend fun writeToken(metric: String, token: String) {
        val updated = updateToken(metric, token)
        if (updated == 0) {
            writeFullSync(VitalsSyncCursorEntity(metric = metric, changesToken = token, lastFullSyncMillis = null))
        }
    }

    @Query("DELETE FROM vitals_daily_aggregates WHERE metric = :metric")
    suspend fun deleteMetricRows(metric: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRows(rows: List<VitalsDailyAggregateEntity>)

    @Query("DELETE FROM vitals_sync_cursors WHERE metric = :metric")
    suspend fun deleteCursor(metric: String)

    @Query("UPDATE vitals_sync_cursors SET changes_token = :token WHERE metric = :metric")
    suspend fun updateToken(metric: String, token: String): Int
}
