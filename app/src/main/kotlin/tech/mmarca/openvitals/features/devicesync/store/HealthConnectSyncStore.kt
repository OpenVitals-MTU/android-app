package tech.mmarca.openvitals.features.devicesync.store

import android.util.Log
import androidx.health.connect.client.records.Record
import java.time.Instant
import kotlinx.coroutines.CancellationException
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.SyncedRecordOriginRepository
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
 *
 * ORIGINAL SOURCE PRESERVATION — Health Connect re-stamps every record this
 * phone writes with OpenVitals' own package, so each outgoing [SyncItem] also
 * carries the ORIGINAL source app ([resolveOriginalSource]: the local
 * `dataOrigin`, or the preserved origin when the record itself arrived by
 * sync, so chains pass the original through). Each incoming foreign origin is
 * persisted per fingerprint through [SyncedRecordOriginRepository] and only
 * ever used for display — it never enters the fingerprint or the payload, so
 * convergence with builds that predate the field is untouched.
 */
class HealthConnectSyncStore(
    private val healthConnectManager: HealthConnectManager,
    private val importRepository: AppleHealthImportRepository,
    private val originRepository: SyncedRecordOriginRepository,
    /** This phone's own package — origins matching it are not worth a row. */
    private val localPackageName: String,
    /** The inclusive sync window the user chose ("how far back"). */
    private val windowStart: Instant,
    private val windowEnd: Instant,
) : SyncRecordStore {

    override suspend fun readItems(types: Set<String>): List<SyncItem> {
        val preservedOrigins = originRepository.preservedOrigins()
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
                        originPackage = resolveOriginalSource(
                            clientRecordId = record.metadata.clientRecordId,
                            dataOriginPackage = record.metadata.dataOrigin.packageName,
                            preservedOrigins = preservedOrigins,
                        ),
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
        val originsByKey = mutableMapOf<String, String>()
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
            // Keyed on the RECOMPUTED fingerprint (what actually gets written
            // as the clientRecordId), not the peer-claimed item.key.
            val key = record.metadata.clientRecordId ?: item.key
            recordsByType.getOrPut(item.recordType) { mutableListOf() } += record
            keysByType.getOrPut(item.recordType) { mutableListOf() } += key
            persistableOrigin(item.originPackage, localPackageName)?.let { origin ->
                originsByKey[key] = origin
            }
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
        // Remember each landed record's original source app so the UI can keep
        // attributing it to Gadgetbridge (etc.) instead of OpenVitals.
        // Duplicate-converged keys are included on purpose: the mapping is an
        // upsert, and Health Connect already holds that exact fingerprint.
        // Best-effort — a failure here loses display attribution, not records.
        val landedOrigins = originsByKey.filterKeys { it in written }
        if (landedOrigins.isNotEmpty()) {
            try {
                originRepository.recordOrigins(landedOrigins)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "failed to persist ${landedOrigins.size} record origins: ${e.message}")
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

/**
 * The ORIGINAL source app to announce for an outgoing record.
 *
 * A record this phone itself received by sync carries a preserved origin under
 * its `sync_<hex>` clientRecordId — that wins, so an A→B→C chain forwards A's
 * Gadgetbridge, not B's re-stamped OpenVitals. Anything else (a native record,
 * or one synced before origins were carried) announces its local Health
 * Connect `dataOrigin`.
 */
internal fun resolveOriginalSource(
    clientRecordId: String?,
    dataOriginPackage: String,
    preservedOrigins: Map<String, String>,
): String = clientRecordId?.let(preservedOrigins::get) ?: dataOriginPackage

/**
 * The origin worth persisting for an incoming record, or null.
 *
 * Null (an old-version peer that does not carry origins), blank, and
 * [localPackageName] (a record genuinely authored in OpenVitals — the
 * receiver's default attribution is already right) all map to null.
 */
internal fun persistableOrigin(originPackage: String?, localPackageName: String): String? =
    originPackage?.takeIf { it.isNotBlank() && it != localPackageName }
