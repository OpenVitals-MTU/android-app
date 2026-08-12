package tech.mmarca.openvitals.data.local.garmin

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Watch-only wellness samples that Health Connect has no type for.
 *
 * Stress and Body Battery (and the other [tech.mmarca.openvitals.domain.model.GarminWellnessMetric]
 * series) are Garmin-proprietary measures with no Health Connect equivalent,
 * so unlike everything else the app reads, there is nowhere else to put them —
 * this table is their system of record, not a cache.
 *
 * One table with a [metric] discriminator rather than many near-identical
 * ones: all are plain `(instant, integer)` series and arrive from the same FIT
 * messages.
 *
 * The `(metric, time_millis)` primary key does the deduplication. A watch
 * re-offers the same monitoring window on successive syncs, so the same sample
 * arrives repeatedly; an upsert on that key makes a re-import idempotent, the
 * same guarantee `clientRecordId` gives the Health Connect records.
 *
 * Column-identical to the Flutter build's drift table so phase 5's preserved
 * drift file imports 1:1.
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
