package tech.mmarca.openvitals.data.local.vitalscache

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * One aggregated day of one cached metric. Sums with a count, not means,
 * so a recompute is exact. Blood pressure keeps diastolic in [secondarySum].
 */
@Entity(
    tableName = "vitals_daily_aggregates",
    primaryKeys = ["metric", "epoch_day"],
)
data class VitalsDailyAggregateEntity(
    @ColumnInfo(name = "metric") val metric: String,
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "value_sum") val valueSum: Double,
    @ColumnInfo(name = "secondary_sum") val secondarySum: Double?,
    @ColumnInfo(name = "sample_count") val sampleCount: Long,
)

/**
 * The sync cursor for one metric. A row with a token is the validity flag.
 * The key carries the cache-format version; bumping it forces a rebuild.
 */
@Entity(tableName = "vitals_sync_cursors")
data class VitalsSyncCursorEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "metric") val metric: String,
    @ColumnInfo(name = "changes_token") val changesToken: String?,
    @ColumnInfo(name = "last_full_sync_millis") val lastFullSyncMillis: Long?,
)
