package tech.mmarca.openvitals.data.local.syncorigin

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SyncedRecordOriginDao {
    @Query("SELECT * FROM synced_record_origins")
    suspend fun all(): List<SyncedRecordOriginEntity>

    @Upsert
    suspend fun upsertAll(origins: List<SyncedRecordOriginEntity>)
}
