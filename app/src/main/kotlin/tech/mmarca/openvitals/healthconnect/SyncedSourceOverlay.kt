package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.metadata.Metadata

/**
 * Display-time substitution of a synced record's original source app.
 * Health Connect stamps `dataOrigin` with the writer, so the sync carries
 * the original and the receiver persists it per `clientRecordId`. A
 * process-wide object: the readers' mapping loops cannot take a dependency.
 * A not-yet-hydrated overlay degrades to the raw attribution.
 */
object SyncedSourceOverlay {

    @Volatile private var originsByClientRecordId: Map<String, String> = emptyMap()

    /** Replaces the snapshot; the single writer is the origin repository. */
    fun update(origins: Map<String, String>) {
        originsByClientRecordId = origins
    }

    /** The current snapshot (used when re-syncing to pass origins through). */
    fun snapshot(): Map<String, String> = originsByClientRecordId

    /** The preserved original source package for [clientRecordId], if any. */
    fun originFor(clientRecordId: String?): String? =
        clientRecordId?.let { originsByClientRecordId[it] }

    /** True when [clientRecordId] belongs to a record with a preserved origin. */
    fun isSyncedRecord(clientRecordId: String?): Boolean = originFor(clientRecordId) != null

    /** The source to display: the preserved original for a synced record, else `dataOrigin`. */
    fun displaySource(metadata: Metadata): String =
        originFor(metadata.clientRecordId) ?: metadata.dataOrigin.packageName
}
