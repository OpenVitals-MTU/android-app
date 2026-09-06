package tech.mmarca.openvitals.data.local.garmin

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-minute rows from a Garmin watch that does not stage sleep. Health
 * Connect has no type for per-minute movement, and a night arrives across
 * several syncs, so the rows wait here until the estimator writes the night
 * as a session. Pruned after 45 days.
 */
@Entity(tableName = "garmin_sleep_minutes")
data class GarminSleepMinuteEntity(
    /** Minute instant, UTC milliseconds since the epoch. One row per minute. */
    @PrimaryKey @ColumnInfo(name = "time_millis") val timeMillis: Long,
    /** A [tech.mmarca.openvitals.domain.model.SleepMinuteKind.storageName]. */
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "heart_rate") val heartRate: Double?,
    @ColumnInfo(name = "movement") val movement: Double?,
    @ColumnInfo(name = "activity") val activity: Double?,
    /** The watch's UTC offset at that minute, in seconds. */
    @ColumnInfo(name = "offset_seconds") val offsetSeconds: Int,
    /** The ten raw features as little-endian float32, or null for non-raw rows. */
    @ColumnInfo(name = "features", typeAffinity = ColumnInfo.BLOB) val features: ByteArray?,
)
