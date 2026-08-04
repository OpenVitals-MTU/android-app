package tech.mmarca.openvitals.data.local.vitalscache

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * One aggregated day of one cached metric.
 *
 * Values are stored as SUMS with a reading count, not as means: a per-day
 * recompute is then exact and idempotent — the mean reconstructs as
 * `valueSum / sampleCount`. Blood pressure keeps its diastolic sum in
 * [secondarySum]; the daily calories total is stored with `sampleCount = 1`.
 * Days with no readings have no row.
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
 * The sync cursor for one cached metric. The EXISTENCE of a row with a token is
 * the cache-validity flag: readers only trust the aggregate rows while a cursor
 * is present, and the sync services full-rebuild whenever it is missing. The
 * metric key carries the cache-format version (`.v2` style) — bumping it makes
 * the cursor lookup miss and forces the rebuild that rewrites stale rows.
 */
@Entity(tableName = "vitals_sync_cursors")
data class VitalsSyncCursorEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "metric") val metric: String,
    @ColumnInfo(name = "changes_token") val changesToken: String?,
    @ColumnInfo(name = "last_full_sync_millis") val lastFullSyncMillis: Long?,
)
