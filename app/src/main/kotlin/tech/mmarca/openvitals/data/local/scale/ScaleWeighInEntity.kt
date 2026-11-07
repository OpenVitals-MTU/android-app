package tech.mmarca.openvitals.data.local.scale

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * A weigh-in as the scale measured it. Health Connect has no type for body
 * impedance, so this table is its system of record. The weight and the
 * heart rate are held beside it for two reasons: a weigh-in heard in the
 * background must survive until Health Connect accepts it, and composition
 * can only be recomputed against the weight measured with that impedance.
 * The `(scale_timestamp, profile)` key makes a repeated broadcast rewrite
 * its own row.
 */
@Entity(
    tableName = "scale_weigh_ins",
    primaryKeys = ["scale_timestamp", "profile"],
)
data class ScaleWeighInEntity(
    /** Seconds on the scale's own clock. Identity only. */
    @ColumnInfo(name = "scale_timestamp") val scaleTimestamp: Long,
    /** The user slot the scale assigned the weigh-in to. */
    @ColumnInfo(name = "profile") val profile: Int,
    /** When the weigh-in happened, UTC milliseconds since the epoch. */
    @ColumnInfo(name = "time_millis") val timeMillis: Long,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    @ColumnInfo(name = "heart_rate_bpm") val heartRateBpm: Int?,
    /** The 50 kHz reading. */
    @ColumnInfo(name = "impedance_low_ohm") val impedanceLowOhm: Double?,
    /** The 250 kHz reading. */
    @ColumnInfo(name = "impedance_high_ohm") val impedanceHighOhm: Double?,
    /** Raised by every broadcast that added something. */
    @ColumnInfo(name = "updated_millis") val updatedMillis: Long,
    /** The [updatedMillis] that last reached Health Connect. Null while nothing has. */
    @ColumnInfo(name = "written_millis") val writtenMillis: Long?,
)
