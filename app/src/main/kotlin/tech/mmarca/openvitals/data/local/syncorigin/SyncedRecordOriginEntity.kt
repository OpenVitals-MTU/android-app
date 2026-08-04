package tech.mmarca.openvitals.data.local.syncorigin

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The original source app of one record received through phone-to-phone sync.
 *
 * Health Connect re-stamps every record the receiver writes with OpenVitals'
 * own package, so the app that ORIGINALLY recorded the data (Gadgetbridge, a
 * watch companion, …) survives only here. Keyed on the record's
 * `clientRecordId` — the deterministic `sync_<hex>` content fingerprint both
 * phones compute — so the row stays attached to the record across convergent
 * re-syncs, the same way the Apple Health importer's `apple_health_<hex>` ids
 * mark ITS provenance. That prefix idiom can say a record was imported but not
 * from WHICH app, hence this table.
 */
@Entity(tableName = "synced_record_origins")
data class SyncedRecordOriginEntity(
    /** The record's `sync_<hex>` clientRecordId (its content fingerprint). */
    @PrimaryKey @ColumnInfo(name = "client_record_id") val clientRecordId: String,
    /** Package of the app that originally wrote the record on the sending phone. */
    @ColumnInfo(name = "origin_package") val originPackage: String,
)
