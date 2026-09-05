package tech.mmarca.openvitals.data.local.bodyenergy

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per local day of the Body Energy chain. Keyed by [epochDay]
 * alone: the [signature] is a validity stamp, so a recompute overwrites in
 * place. [endScore] seeds the next day and is readable without touching the
 * buckets, which is why they are a separate table.
 */
@Entity(tableName = "body_energy_days")
data class BodyEnergyDayEntity(
    @PrimaryKey
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    /** The per-day signature this row was computed under, compared against this row's own date. */
    @ColumnInfo(name = "signature") val signature: String,
    @ColumnInfo(name = "start_score") val startScore: Int,
    @ColumnInfo(name = "end_score") val endScore: Int,
    @ColumnInfo(name = "charged") val charged: Int,
    @ColumnInfo(name = "drained") val drained: Int,
    @ColumnInfo(name = "confidence") val confidence: String,
    @ColumnInfo(name = "confidence_reason") val confidenceReason: String,
    @ColumnInfo(name = "generated_at_millis") val generatedAtMillis: Long,
    // The input summary, one column per field.
    @ColumnInfo(name = "algorithm_version") val algorithmVersion: Int,
    @ColumnInfo(name = "bucket_minutes") val bucketMinutes: Long,
    @ColumnInfo(name = "heart_rate_sample_count") val heartRateSampleCount: Int,
    @ColumnInfo(name = "hrv_sample_count") val hrvSampleCount: Int,
    @ColumnInfo(name = "sleep_session_count") val sleepSessionCount: Int,
    @ColumnInfo(name = "workout_count") val workoutCount: Int,
    @ColumnInfo(name = "respiratory_sample_count") val respiratorySampleCount: Int,
    @ColumnInfo(name = "has_resting_heart_rate") val hasRestingHeartRate: Boolean,
    @ColumnInfo(name = "has_baseline_resting_heart_rate") val hasBaselineRestingHeartRate: Boolean,
    @ColumnInfo(name = "has_observed_max_heart_rate") val hasObservedMaxHeartRate: Boolean,
    @ColumnInfo(name = "has_hrv_baseline") val hasHrvBaseline: Boolean,
    @ColumnInfo(name = "has_respiratory_baseline") val hasRespiratoryBaseline: Boolean,
    @ColumnInfo(name = "previous_end_score") val previousEndScore: Int?,
    @ColumnInfo(name = "carry_over_floor_applied") val carryOverFloorApplied: Boolean,
    @ColumnInfo(name = "seed_source") val seedSource: String,
    @ColumnInfo(name = "calibration_mode") val calibrationMode: String,
)

/**
 * The 5-minute buckets behind each day. [epochDay] is the local day, stored
 * explicitly: deriving it from the UTC instant would split a day across two.
 * Enum columns hold the enum's `name`.
 */
@Entity(
    tableName = "body_energy_buckets",
    primaryKeys = ["epoch_day", "time_millis"],
)
data class BodyEnergyBucketEntity(
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    /** Bucket start, UTC milliseconds since the epoch. */
    @ColumnInfo(name = "time_millis") val timeMillis: Long,
    @ColumnInfo(name = "score") val score: Int,
    @ColumnInfo(name = "delta") val delta: Double,
    @ColumnInfo(name = "state") val state: String,
    @ColumnInfo(name = "confidence") val confidence: String,
    @ColumnInfo(name = "charge") val charge: Double,
    @ColumnInfo(name = "intensity_drain") val intensityDrain: Double,
    @ColumnInfo(name = "activity_energy_drain") val activityEnergyDrain: Double,
    @ColumnInfo(name = "basal_drain") val basalDrain: Double,
    @ColumnInfo(name = "stress_drain") val stressDrain: Double,
    @ColumnInfo(name = "recovery_debt_drain") val recoveryDebtDrain: Double,
    @ColumnInfo(name = "primary_influence") val primaryInfluence: String,
)

/** Days of buckets kept. The summary row survives, so the chain stays intact. */
const val BodyEnergyBucketRetentionDays = 120L

/**
 * The `vitals_sync_cursors` key for the chain. `changes_token` holds the
 * global signature, `last_full_sync_millis` the warm service's last pass.
 */
const val BodyEnergyChainCursorKey = "bodyEnergyChain.v1"
