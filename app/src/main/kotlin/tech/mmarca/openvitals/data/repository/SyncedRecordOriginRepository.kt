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
 * System of record for the original source apps of synced records, and
 * the single writer of [SyncedSourceOverlay]. Health Connect re-stamps
 * `dataOrigin` on write, so this keeps the attribution it cannot.
 */
@Singleton
class SyncedRecordOriginRepository @Inject constructor(
    private val dao: SyncedRecordOriginDao,
    private val dispatchers: DispatcherProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    /** Fire-and-forget hydration for app start. Until done, synced records show the raw attribution. */
    fun warmOverlay() {
        scope.launch { runCatching { hydrateOverlay() } }
    }

    /** Loads the table and publishes it as the display overlay snapshot. */
    suspend fun hydrateOverlay(): Map<String, String> = withContext(dispatchers.io) {
        val origins = dao.all().associate { it.clientRecordId to it.originPackage }
        SyncedSourceOverlay.update(origins)
        origins
    }

    /** All preserved origins, so a re-sent record carries its original origin through a chain. */
    suspend fun preservedOrigins(): Map<String, String> = hydrateOverlay()

    /** Persists origins for records that just landed and refreshes the overlay. */
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
