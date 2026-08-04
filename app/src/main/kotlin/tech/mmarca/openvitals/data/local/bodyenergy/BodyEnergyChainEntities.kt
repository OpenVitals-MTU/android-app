package tech.mmarca.openvitals.data.local.bodyenergy

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per local calendar day of the Body Energy chain: the day's headline
 * numbers plus everything its input summary carries.
 *
 * Keyed by [epochDay] ALONE, deliberately. The [signature] is a *validity
 * stamp*, not a discriminator: there is exactly one true timeline per day at any
 * moment. Keying by `(day, signature)` would accumulate an orphan row for every
 * calibration edit and force each chain read to filter; keying by day means a
 * recompute overwrites in place and the table can never exceed one row per day
 * the user has lived. A signature mismatch on read is simply a miss.
 *
 * [endScore] is what seeds the *next* day — the reason this table exists. It is
 * readable without touching [BodyEnergyBucketEntity], which is why the buckets
 * are a separate table rather than an encoded column here: the chain walk-back
 * asks this question up to a fortnight's worth of times per screen open and must
 * not decode 288 points to answer it.
 */
@Entity(tableName = "body_energy_days")
data class BodyEnergyDayEntity(
    @PrimaryKey
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    /**
     * The per-day signature (`v11|<zones+profile hash>|<permission hash>|<gain
     * hash>`) this row was computed under, compared on read against the
     * signature built for THIS row's own date — the body profile's signature
     * varies by date, so one built for day D can never validate day D-1.
     */
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
 * The 5-minute buckets behind each [BodyEnergyDayEntity] — ~288 for a full day.
 *
 * [epochDay] is the LOCAL calendar day and is stored explicitly rather than
 * derived from [timeMillis]: for most of the world a bucket's UTC instant falls
 * on a different UTC day than its local date, so deriving it would scatter one
 * day's buckets across two partitions.
 *
 * Enum-valued columns hold the enum's `name` — greppable in a `sqlite3` dump and
 * immune to a Kotlin enum being reordered.
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

/**
 * How many days of 5-minute buckets are kept. Past this the day's summary row
 * survives — so the chain, and any long-range daily-score chart, stay intact —
 * but its buckets are dropped. Buckets are ~99% of the chain's bytes and nothing
 * reads them more than a few weeks back.
 */
const val BodyEnergyBucketRetentionDays = 120L

/**
 * The `vitals_sync_cursors` key the Body Energy chain keeps its bookkeeping
 * under. That table is generic per-key sync state, so the chain reuses it rather
 * than cloning a two-column table.
 *
 * `changes_token` holds the GLOBAL signature — algorithm version, zones and
 * permissions, without the per-day profile component — so a calibration edit can
 * purge the whole chain in one comparison. `last_full_sync_millis` is the warm
 * service's last completed pass.
 */
const val BodyEnergyChainCursorKey = "bodyEnergyChain.v1"
