package tech.mmarca.openvitals.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.local.syncorigin.SyncedRecordOriginDao
import tech.mmarca.openvitals.data.local.syncorigin.SyncedRecordOriginEntity
import tech.mmarca.openvitals.healthconnect.SyncedSourceOverlay

/**
 * System of record for the original source apps of phone-to-phone-synced
 * records (`synced_record_origins`), and the single writer of the in-memory
 * [SyncedSourceOverlay] the Health Connect readers substitute from.
 *
 * The table maps a synced record's `sync_<hex>` clientRecordId to the package
 * of the app that originally recorded it on the sending phone — the
 * attribution Health Connect itself cannot keep, because the receiver's write
 * re-stamps `dataOrigin` with OpenVitals' own package.
 */
@Singleton
class SyncedRecordOriginRepository @Inject constructor(
    private val dao: SyncedRecordOriginDao,
    private val dispatchers: DispatcherProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    /**
     * Fire-and-forget overlay hydration for app start. Until it completes,
     * synced records display their raw Health Connect attribution — the
     * pre-feature behavior, never an error.
     */
    fun warmOverlay() {
        scope.launch { runCatching { hydrateOverlay() } }
    }

    /** Loads the table and publishes it as the display overlay snapshot. */
    suspend fun hydrateOverlay(): Map<String, String> = withContext(dispatchers.io) {
        val origins = dao.all().associate { it.clientRecordId to it.originPackage }
        SyncedSourceOverlay.update(origins)
        origins
    }

    /**
     * All preserved origins, for the sync read path's pass-through: when a
     * record that itself arrived by sync is re-sent (an A→B→C chain), the wire
     * must carry the ORIGINAL origin, not this phone's re-stamped one.
     */
    suspend fun preservedOrigins(): Map<String, String> = hydrateOverlay()

    /**
     * Persists origins for records that just landed from a peer (fingerprint →
     * original package) and refreshes the overlay so the UI attributes them
     * immediately.
     */
    suspend fun recordOrigins(originsByClientRecordId: Map<String, String>) {
        if (originsByClientRecordId.isEmpty()) return
        withContext(dispatchers.io) {
            dao.upsertAll(
                originsByClientRecordId.map { (clientRecordId, originPackage) ->
                    SyncedRecordOriginEntity(
                        clientRecordId = clientRecordId,
                        originPackage = originPackage,
                    )
                },
            )
        }
        hydrateOverlay()
    }
}
