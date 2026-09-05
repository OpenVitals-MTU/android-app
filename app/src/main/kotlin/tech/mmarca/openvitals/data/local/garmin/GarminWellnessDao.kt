package tech.mmarca.openvitals.data.local.garmin

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GarminWellnessDao {

    /** Upserts a batch; an overlapping window rewrites the same rows. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSamples(samples: List<GarminWellnessSampleEntity>)

    /** Samples for [metric] in `[fromMillis, toMillis)`, oldest first. */
    @Query(
        "SELECT * FROM garmin_wellness_samples " +
            "WHERE metric = :metric AND time_millis >= :fromMillis AND time_millis < :toMillis " +
            "ORDER BY time_millis",
    )
    suspend fun samplesBetween(metric: String, fromMillis: Long, toMillis: Long): List<GarminWellnessSampleEntity>

    /** The most recent sample for [metric], or null when none has been synced. */
    @Query(
        "SELECT * FROM garmin_wellness_samples WHERE metric = :metric " +
            "ORDER BY time_millis DESC LIMIT 1",
    )
    suspend fun latest(metric: String): GarminWellnessSampleEntity?

    /** Total rows held, for diagnostics. */
    @Query("SELECT COUNT(time_millis) FROM garmin_wellness_samples WHERE metric = :metric")
    suspend fun countFor(metric: String): Long
}
