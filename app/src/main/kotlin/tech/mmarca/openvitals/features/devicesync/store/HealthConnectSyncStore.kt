package tech.mmarca.openvitals.features.devicesync.store

import android.util.Log
import androidx.health.connect.client.records.Record
import java.time.Instant
import kotlinx.coroutines.CancellationException
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.features.devicesync.protocol.SyncItem
import tech.mmarca.openvitals.features.devicesync.protocol.SyncRecordStore
import tech.mmarca.openvitals.features.imports.applehealth.isDuplicateClientRecordFailure
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/**
 * The Health Connect implementation of [SyncRecordStore], bridging the sync
 * protocol to real record reads, dedup, and writes.
 *
 * Reads become [SyncItem]s keyed by content fingerprint ([syncFingerprint]);
 * dedup happens in the session, which seeds its baseline from these same keys.
 * Writes reconstruct typed records (carrying the fingerprint as their
 * clientRecordId) and insert them via
 * [AppleHealthImportRepository.insertImportedRecords]. Because both phones
 * compute the same fingerprint and write it as the clientRecordId, re-syncs
 * converge and Health Connect upserts rather than duplicating.
 */
class HealthConnectSyncStore(
    private val healthConnectManager: HealthConnectManager,
    private val importRepository: AppleHealthImportRepository,
    /** The inclusive sync window the user chose ("how far back"). */
    private val windowStart: Instant,
    private val windowEnd: Instant,
) : SyncRecordStore {

    override suspend fun readItems(types: Set<String>): List<SyncItem> {
        val items = mutableListOf<SyncItem>()
        for (type in types) {
            val recordClass = syncRecordClassFor(type) ?: continue
            val records = healthConnectManager.readRecordsForSync(recordClass, windowStart, windowEnd)
            for (record in records) {
                // A record the codec cannot express (future provider fields,
                // an unexpected shape) is skipped rather than sinking the read.
                val item = runCatching {
                    SyncItem(
                        key = syncFingerprint(record),
                        recordType = type,
                        payload = encodeSyncRecordPayload(record),
                    )
                }.getOrNull() ?: continue
                items += item
            }
        }
        return items
    }

    override suspend fun writeItems(items: List<SyncItem>): Set<String> {
        // Group by record type and insert each group separately. A Health
        // Connect batch insert is atomic, so mixing types means one
        // unsupported/rejected type sinks the whole batch; isolating types
        // keeps the rest landing. A per-type failure is logged and swallowed so
        // the sync continues with the types that do write.
        val recordsByType = mutableMapOf<String, MutableList<Record>>()
        val keysByType = mutableMapOf<String, MutableList<String>>()
        for (item in items) {
            val record = try {
                ownedRecord(item)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // One malformed item from a buggy/hostile peer must not kill
                // the whole receive loop (which would stop acking and stall the
                // peer). Skip it — and, since it never lands, it is not among
                // the returned written keys.
                Log.w(TAG, "skipping undecodable ${item.recordType}: ${e.message}")
                continue
            }
            recordsByType.getOrPut(item.recordType) { mutableListOf() } += record
            keysByType.getOrPut(item.recordType) { mutableListOf() } +=
                record.metadata.clientRecordId ?: item.key
        }
        val written = mutableSetOf<String>()
        for ((type, records) in recordsByType) {
            val keys = keysByType.getValue(type)
            try {
                importRepository.insertImportedRecords(records)
                written += keys
                Log.i(TAG, "wrote ${records.size} $type records")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e.isDuplicateClientRecordFailure()) {
                    // The batch tripped on an already-present clientRecordId.
                    // Retry record-by-record so the fresh ones still land; a
                    // per-record duplicate means Health Connect already holds
                    // that exact fingerprint — converged, so count it written.
                    written += insertIndividually(type, records, keys)
                } else {
                    // Type batch rejected — its keys are NOT reported as
                    // written, so the session won't count them as imported.
                    Log.w(TAG, "WRITE FAILED for ${records.size} $type: ${e.message}")
                }
            }
        }
        return written
    }

    private suspend fun insertIndividually(
        type: String,
        records: List<Record>,
        keys: List<String>,
    ): Set<String> {
        val written = mutableSetOf<String>()
        records.forEachIndexed { index, record ->
            try {
                importRepository.insertImportedRecords(listOf(record))
                written += keys[index]
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e.isDuplicateClientRecordFailure()) {
                    written += keys[index]
                } else {
                    Log.w(TAG, "single write failed for $type: ${e.message}")
                }
            }
        }
        return written
    }

    /**
     * Decodes [item] and re-derives its clientRecordId from the decoded
     * *content* rather than trusting the peer-supplied [SyncItem.key].
     * Otherwise a hostile or buggy peer could set the key to an existing id
     * (e.g. an `apple_health_<hex>` we hold) and have Health Connect upsert
     * over — corrupt — an unrelated record. A content fingerprint can only
     * ever address the record the peer actually sent. In the honest case the
     * recomputed key equals the sent one, so the extra decode is skipped.
     */
    private fun ownedRecord(item: SyncItem): Record {
        val decoded = decodeSyncRecord(
            recordType = item.recordType,
            clientRecordId = item.key,
            payload = item.payload,
        )
        val ownKey = syncFingerprint(decoded)
        if (ownKey == item.key) return decoded
        return decodeSyncRecord(
            recordType = item.recordType,
            clientRecordId = ownKey,
            payload = item.payload,
        )
    }

    private companion object {
        const val TAG = "DeviceSync"
    }
}
