package tech.mmarca.openvitals.data.local.scale

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScaleWeighInDao {
    @Query("SELECT * FROM scale_weigh_ins WHERE scale_timestamp = :scaleTimestamp AND profile = :profile LIMIT 1")
    suspend fun find(scaleTimestamp: Long, profile: Int): ScaleWeighInEntity?

    @Upsert
    suspend fun upsert(weighIn: ScaleWeighInEntity)

    /** Weigh-ins with a weight that Health Connect has not seen in their current state, oldest first. */
    @Query(
        "SELECT * FROM scale_weigh_ins " +
            "WHERE weight_kg IS NOT NULL AND (written_millis IS NULL OR written_millis < updated_millis) " +
            "ORDER BY time_millis ASC",
    )
    suspend fun pending(): List<ScaleWeighInEntity>

    @Query(
        "SELECT COUNT(*) FROM scale_weigh_ins " +
            "WHERE weight_kg IS NOT NULL AND (written_millis IS NULL OR written_millis < updated_millis)",
    )
    fun pendingCount(): Flow<Int>

    /** Stamps the state that was written. A row that moved on since stays pending. */
    @Query(
        "UPDATE scale_weigh_ins SET written_millis = :writtenMillis " +
            "WHERE scale_timestamp = :scaleTimestamp AND profile = :profile",
    )
    suspend fun markWritten(scaleTimestamp: Long, profile: Int, writtenMillis: Long)

    @Query("SELECT * FROM scale_weigh_ins WHERE weight_kg IS NOT NULL ORDER BY time_millis DESC LIMIT 1")
    fun latest(): Flow<ScaleWeighInEntity?>

    @Query("DELETE FROM scale_weigh_ins WHERE scale_timestamp = :scaleTimestamp AND profile = :profile")
    suspend fun delete(scaleTimestamp: Long, profile: Int)

    /** Drops weigh-ins whose weight frame was never heard. Their other frame alone says nothing. */
    @Query("DELETE FROM scale_weigh_ins WHERE weight_kg IS NULL AND time_millis < :beforeMillis")
    suspend fun deleteWithoutWeightBefore(beforeMillis: Long)
}
