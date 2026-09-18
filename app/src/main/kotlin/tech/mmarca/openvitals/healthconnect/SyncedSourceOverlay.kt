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

    @Volatile private var index = SyncedOriginIndex()

    /** Replaces everything; the single writer is the origin repository. */
    fun update(origins: Map<String, String>) {
        replace(SyncedOriginIndex(origins.size).also { it.putAll(origins) })
    }

    /** Swaps in an index built elsewhere, so a reload never shows a half-filled one. */
    internal fun replace(newIndex: SyncedOriginIndex) {
        index = newIndex
    }

    /** Adds rows that just landed. No reload: the table can hold a million rows. */
    fun add(origins: Map<String, String>) {
        index.putAll(origins)
    }

    /** How many origins are held. */
    val size: Int get() = index.size

    /** The preserved original source package for [clientRecordId], if any. */
    fun originFor(clientRecordId: String?): String? =
        clientRecordId?.let { index[it] }

    /** True when [clientRecordId] belongs to a record with a preserved origin. */
    fun isSyncedRecord(clientRecordId: String?): Boolean = originFor(clientRecordId) != null

    /** The source to display: the preserved original for a synced record, else `dataOrigin`. */
    fun displaySource(metadata: Metadata): String =
        originFor(metadata.clientRecordId) ?: metadata.dataOrigin.packageName
}
