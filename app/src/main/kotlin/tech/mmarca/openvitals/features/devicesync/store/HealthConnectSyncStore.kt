package tech.mmarca.openvitals.features.devicesync.store

import android.util.Log
import androidx.health.connect.client.records.Record
import java.time.Instant
import kotlin.reflect.KClass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.SyncedRecordOriginRepository
import tech.mmarca.openvitals.features.devicesync.protocol.SyncAborted
import tech.mmarca.openvitals.features.devicesync.protocol.SyncItem
import tech.mmarca.openvitals.features.devicesync.protocol.SyncRecordStore
import tech.mmarca.openvitals.features.imports.applehealth.isDuplicateClientRecordFailure
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/**
 * The Health Connect implementation of [SyncRecordStore]. Reads become
 * [SyncItem]s keyed by [syncFingerprint]; writes insert typed records under
 * that fingerprint as clientRecordId, so re-syncs converge.
 *
 * Health Connect re-stamps written records with this app's package, so each
 * outgoing item also carries the original source app. Incoming origins are
 * persisted per fingerprint for display only.
 */
class HealthConnectSyncStore(
    private val healthConnectManager: HealthConnectManager,
    private val importRepository: AppleHealthImportRepository,
    private val originRepository: SyncedRecordOriginRepository,
    /** This phone's package; origins matching it are not stored. */
    private val localPackageName: String,
    /** The inclusive sync window the user chose ("how far back"). */
    private val windowStart: Instant,
    private val windowEnd: Instant,
    /** Overridable so a test can hit the byte cap without megabyte fixtures. */
    private val chunkPayloadByteCap: Int = ChunkPayloadByteCap,
) : SyncRecordStore {

    override fun readKeys(types: Set<String>): Flow<String> = flow {
        // Keys only: the baseline must stay record-light.
        for (type in types) {
            val recordClass = syncRecordClassFor(type) ?: continue
            forEachRecordPageOrAbort(recordClass, type) { page ->
                for (record in page) {
                    val key = runCatching { syncFingerprint(record) }.getOrNull() ?: continue
                    emit(key)
                }
            }
        }
    }

    /**
     * Streams pages for [recordClass], turning a read failure into a clean
     * abort. A stream that quietly ended once reported "completed".
     */
    private suspend fun forEachRecordPageOrAbort(
        recordClass: KClass<out Record>,
        type: String,
        action: suspend (List<Record>) -> Unit,
    ) {
        try {
            healthConnectManager.forEachSyncRecordPage(recordClass, windowStart, windowEnd, action)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SyncAborted) {
            throw e
        } catch (e: Exception) {
            throw SyncAborted("reading $type from Health Connect failed: ${e.message}")
        }
    }

    override fun readItemChunks(types: Set<String>, chunkSize: Int): Flow<List<SyncItem>> = flow {
        val preservedOrigins = originRepository.preservedOrigins()
        val chunk = mutableListOf<SyncItem>()
        var chunkPayloadBytes = 0
        for (type in types) {
            val recordClass = syncRecordClassFor(type) ?: continue
            forEachRecordPageOrAbort(recordClass, type) { page ->
                for (record in page) {
                    // A record the codec cannot express is skipped, not fatal.
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
                    chunk += item
                    chunkPayloadBytes += item.payload.size
                    // Capped by bytes too: 500 series records made multi-megabyte
                    // batches that outlived the peer's ack timeout.
                    if (chunk.size >= chunkSize || chunkPayloadBytes >= chunkPayloadByteCap) {
                        emit(chunk.toList())
                        chunk.clear()
                        chunkPayloadBytes = 0
                    }
                }
            }
        }
        if (chunk.isNotEmpty()) emit(chunk.toList())
    }

    override suspend fun writeItems(items: List<SyncItem>): Set<String> {
        // Insert per type: a batch is atomic, so one rejected type must not sink the rest.
        val recordsByType = mutableMapOf<String, MutableList<Record>>()
        val keysByType = mutableMapOf<String, MutableList<String>>()
        val originsByKey = mutableMapOf<String, String>()
        for (item in items) {
            val record = try {
                ownedRecord(item)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // One malformed item must not stall the receive loop. Skipped, so not written.
                Log.w(TAG, "skipping undecodable ${item.recordType}: ${e.message}")
                continue
            }
            // Keyed on the recomputed fingerprint, not the peer-claimed key.
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
                    // A duplicate clientRecordId tripped the batch. Retry one by one;
                    // a per-record duplicate is already converged, so count it written.
                    written += insertIndividually(type, records, keys)
                } else {
                    // Rejected: its keys are not reported as written.
                    Log.w(TAG, "WRITE FAILED for ${records.size} $type: ${e.message}")
                }
            }
        }
        // Remember each landed record's original source app, for display.
        // Converged duplicates included: the mapping is an upsert. Best-effort.
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
     * Decodes [item] and re-derives its clientRecordId from the content, so
     * a hostile peer cannot address an unrelated record through the key.
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
 * The original source app for an outgoing record: the preserved origin when
 * the record itself arrived by sync, else its local `dataOrigin`.
 */
internal fun resolveOriginalSource(
    clientRecordId: String?,
    dataOriginPackage: String,
    preservedOrigins: Map<String, String>,
): String = clientRecordId?.let(preservedOrigins::get) ?: dataOriginPackage

/** The origin worth persisting, or null for absent, blank, or [localPackageName]. */
internal fun persistableOrigin(originPackage: String?, localPackageName: String): String? =
    originPackage?.takeIf { it.isNotBlank() && it != localPackageName }

/** Payload-byte ceiling per chunk, so a batch crosses a slow RFCOMM link inside the ack timeout. */
private const val ChunkPayloadByteCap = 256 * 1024
