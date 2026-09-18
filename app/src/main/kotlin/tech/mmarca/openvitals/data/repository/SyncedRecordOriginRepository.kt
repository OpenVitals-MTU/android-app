package tech.mmarca.openvitals.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.local.syncorigin.SyncedRecordOriginDao
import tech.mmarca.openvitals.data.local.syncorigin.SyncedRecordOriginEntity
import tech.mmarca.openvitals.healthconnect.SyncedOriginIndex
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
    private val overlayMutex = Mutex()
    @Volatile private var hydrated = false

    /** Fire-and-forget hydration for app start. Until done, synced records show the raw attribution. */
    fun warmOverlay() {
        scope.launch { runCatching { hydrateOverlay() } }
    }

    /** Loads the table into the display overlay, once per process. */
    suspend fun hydrateOverlay() = overlayMutex.withLock { hydrateLocked() }

    /**
     * Lookup of preserved origins, so a re-sent record carries its original
     * origin through a chain. Backed by the overlay: no second copy.
     */
    suspend fun preservedOrigins(): (String) -> String? {
        hydrateOverlay()
        return SyncedSourceOverlay::originFor
    }

    /** Persists origins for records that just landed and adds them to the overlay. */
    suspend fun recordOrigins(originsByClientRecordId: Map<String, String>) {
        if (originsByClientRecordId.isEmpty()) return
        // Same lock as the load, so a load in flight cannot swap these rows away.
        overlayMutex.withLock {
            hydrateLocked()
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
            SyncedSourceOverlay.add(originsByClientRecordId)
        }
    }

    // Page by page into a packed index. Reading the table whole, after every received batch, ran out of memory.
    private suspend fun hydrateLocked() {
        if (hydrated) return
        withContext(dispatchers.io) {
            val index = SyncedOriginIndex(expectedRows = dao.count())
            var after = ""
            while (true) {
                val page = dao.pageAfter(after, HydrationPageSize)
                if (page.isEmpty()) break
                page.forEach { index.put(it.clientRecordId, it.originPackage) }
                after = page.last().clientRecordId
            }
            SyncedSourceOverlay.replace(index)
        }
        hydrated = true
    }

    private companion object {
        const val HydrationPageSize = 10_000
    }
}
