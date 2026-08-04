package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.metadata.Metadata

/**
 * Display-time substitution of a synced record's ORIGINAL source app.
 *
 * Health Connect stamps `dataOrigin` with the package of the app that WROTE a
 * record, so every record received through phone-to-phone sync is attributed to
 * OpenVitals on the receiving phone — the platform offers no way to write a
 * record on another app's behalf. The sync protocol therefore carries the
 * original source package alongside each record, the receiving phone persists
 * it per `clientRecordId` (`synced_record_origins` in Room, via
 * [tech.mmarca.openvitals.data.repository.SyncedRecordOriginRepository]), and
 * this overlay swaps it back in wherever the app derives a record's `source`
 * from Health Connect metadata. The mapping is keyed on the sync fingerprint
 * (`sync_<hex>`) the receiver wrote as the record's `clientRecordId` — the
 * same convergence key the re-sync dedup uses.
 *
 * A process-wide object rather than an injected dependency on purpose: the
 * substitution happens inside the Health Connect readers' per-record mapping
 * loops and in top-level mapper functions, where threading a constructor
 * dependency through every reader and file-level helper would balloon the
 * change for what is one immutable snapshot map with a single writer (the
 * repository hydrates it at app start and after each sync). Readers only ever
 * observe a complete snapshot; a not-yet-hydrated overlay degrades to the raw
 * Health Connect attribution, never to an error.
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

    /**
     * The source package to DISPLAY for a record: the preserved original source
     * if this record arrived via phone-to-phone sync, otherwise the Health
     * Connect `dataOrigin` as before.
     */
    fun displaySource(metadata: Metadata): String =
        originFor(metadata.clientRecordId) ?: metadata.dataOrigin.packageName
}
