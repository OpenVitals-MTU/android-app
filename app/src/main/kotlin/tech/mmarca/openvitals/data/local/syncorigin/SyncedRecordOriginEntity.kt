package tech.mmarca.openvitals.data.local.syncorigin

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The original source app of one synced record. Health Connect re-stamps
 * the writer, so the origin survives only here. Keyed on the `sync_<hex>`
 * fingerprint, so the row stays attached across re-syncs.
 */
@Entity(tableName = "synced_record_origins")
data class SyncedRecordOriginEntity(
    /** The record's `sync_<hex>` clientRecordId (its content fingerprint). */
    @PrimaryKey @ColumnInfo(name = "client_record_id") val clientRecordId: String,
    /** Package of the app that originally wrote the record on the sending phone. */
    @ColumnInfo(name = "origin_package") val originPackage: String,
)
