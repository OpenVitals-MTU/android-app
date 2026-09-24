package tech.mmarca.openvitals.data.local.heartratecache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One local day's heart-rate average, from raw samples as the day view reads
 * it. The [signature] stamps the hourly aggregates it was read under. When
 * they change, the day is read again and this row is overwritten.
 */
@Entity(tableName = "heart_rate_days")
data class HeartRateDayEntity(
    @PrimaryKey
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "signature") val signature: String,
    @ColumnInfo(name = "average_bpm") val averageBpm: Double,
)
