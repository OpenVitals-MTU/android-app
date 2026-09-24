package tech.mmarca.openvitals.data.local.heartratecache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HeartRateDayCacheDao {

    @Query("SELECT * FROM heart_rate_days WHERE epoch_day BETWEEN :fromEpochDay AND :toEpochDay")
    suspend fun daysBetween(fromEpochDay: Long, toEpochDay: Long): List<HeartRateDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rows: List<HeartRateDayEntity>)
}
