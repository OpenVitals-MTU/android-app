package tech.mmarca.openvitals.data.local.cycle

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per local day the pill was marked as taken, keyed by its ISO date. */
@Entity(tableName = "pill_intakes")
data class PillIntakeEntity(
    @PrimaryKey val date: String,
    @ColumnInfo(name = "taken_at_millis") val takenAtMillis: Long,
)
