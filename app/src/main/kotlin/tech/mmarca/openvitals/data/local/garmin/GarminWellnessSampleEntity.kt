package tech.mmarca.openvitals.data.local.garmin

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Watch-only wellness samples with no Health Connect type: this table is
 * their system of record. One table with a [metric] discriminator; the
 * `(metric, time_millis)` key makes re-imports idempotent. Column-identical
 * to the Flutter drift table.
 */
@Entity(
    tableName = "garmin_wellness_samples",
    primaryKeys = ["metric", "time_millis"],
)
data class GarminWellnessSampleEntity(
    /** A [tech.mmarca.openvitals.domain.model.GarminWellnessMetric.storageName]. */
    @ColumnInfo(name = "metric") val metric: String,
    /** Sample instant, UTC milliseconds since the epoch. */
    @ColumnInfo(name = "time_millis") val timeMillis: Long,
    /** Stored raw, uninterpreted, in the metric's own units. */
    @ColumnInfo(name = "value") val value: Long,
)
