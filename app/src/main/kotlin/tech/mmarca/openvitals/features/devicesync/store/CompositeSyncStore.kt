package tech.mmarca.openvitals.features.devicesync.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import tech.mmarca.openvitals.features.devicesync.protocol.SyncItem
import tech.mmarca.openvitals.features.devicesync.protocol.SyncRecordStore

/**
 * One [SyncRecordStore] for the session, made of one per data home: Health
 * Connect records in one, the cycle journal in another. Each type belongs to
 * exactly one store; a type nobody owns is refused.
 */
class CompositeSyncStore(
    private val routes: List<Pair<Set<String>, SyncRecordStore>>,
) : SyncRecordStore {

    private fun storeFor(recordType: String): SyncRecordStore? =
        routes.firstOrNull { (owned, _) -> recordType in owned }?.second

    override fun readKeys(types: Set<String>): Flow<String> = flow {
        for ((owned, store) in routes) {
            val mine = types intersect owned
            if (mine.isNotEmpty()) emitAll(store.readKeys(mine))
        }
    }

    override fun readItemChunks(types: Set<String>, chunkSize: Int): Flow<List<SyncItem>> = flow {
        for ((owned, store) in routes) {
            val mine = types intersect owned
            if (mine.isNotEmpty()) emitAll(store.readItemChunks(mine, chunkSize))
        }
    }

    override fun accepts(item: SyncItem): Boolean = storeFor(item.recordType)?.accepts(item) ?: false

    override suspend fun writeItems(items: List<SyncItem>): Set<String> {
        val written = mutableSetOf<String>()
        for ((_, group) in items.groupBy { storeFor(it.recordType) }) {
            val store = storeFor(group.first().recordType) ?: continue
            written += store.writeItems(group)
        }
        return written
    }
}
