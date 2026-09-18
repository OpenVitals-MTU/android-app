package tech.mmarca.openvitals.data.local.syncorigin

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SyncedRecordOriginDao {
    @Query("SELECT COUNT(*) FROM synced_record_origins")
    suspend fun count(): Int

    /** One page in key order. The table can hold a million rows, so nothing reads it whole. */
    @Query(
        "SELECT * FROM synced_record_origins WHERE client_record_id > :after " +
            "ORDER BY client_record_id LIMIT :limit",
    )
    suspend fun pageAfter(after: String, limit: Int): List<SyncedRecordOriginEntity>

    @Upsert
    suspend fun upsertAll(origins: List<SyncedRecordOriginEntity>)
}
