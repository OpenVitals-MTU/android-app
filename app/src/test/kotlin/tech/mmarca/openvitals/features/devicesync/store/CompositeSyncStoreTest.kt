package tech.mmarca.openvitals.features.devicesync.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.features.devicesync.protocol.SyncItem
import tech.mmarca.openvitals.features.devicesync.protocol.SyncRecordStore

/** Each record type reaches the one store that owns it; a type nobody owns goes nowhere. */
class CompositeSyncStoreTest {

    private class RecordingStore(private val name: String, private val accept: Boolean = true) : SyncRecordStore {
        val keysAskedFor = mutableListOf<Set<String>>()
        val written = mutableListOf<SyncItem>()

        override fun readKeys(types: Set<String>): Flow<String> = flow {
            keysAskedFor += types
            types.sorted().forEach { emit("$name:$it") }
        }

        override fun readItemChunks(types: Set<String>, chunkSize: Int): Flow<List<SyncItem>> = flow {
            emit(types.sorted().map { SyncItem(key = "$name:$it", recordType = it, payload = ByteArray(0)) })
        }

        override fun accepts(item: SyncItem): Boolean = accept

        override suspend fun writeItems(items: List<SyncItem>): Set<String> {
            written += items
            return items.map { it.key }.toSet()
        }
    }

    private val health = RecordingStore("hc")
    private val journal = RecordingStore("journal", accept = false)
    private val composite = CompositeSyncStore(
        listOf(
            setOf("StepsRecord", "WeightRecord") to health,
            setOf("CycleJournalEntry") to journal,
        ),
    )

    @Test
    fun `keys and chunks come from every store that owns a chosen type`() = runTest {
        val types = setOf("StepsRecord", "CycleJournalEntry", "Nobody")

        val keys = composite.readKeys(types).toList()
        val items = composite.readItemChunks(types, chunkSize = 10).toList().flatten()

        assertEquals(listOf("hc:StepsRecord", "journal:CycleJournalEntry"), keys)
        assertEquals(listOf(setOf("StepsRecord")), health.keysAskedFor)
        assertEquals(listOf(setOf("CycleJournalEntry")), journal.keysAskedFor)
        assertEquals(listOf("hc:StepsRecord", "journal:CycleJournalEntry"), items.map { it.key })
    }

    @Test
    fun `acceptance and writes are delegated by type, and an unowned type is refused`() = runTest {
        val steps = SyncItem(key = "s", recordType = "StepsRecord", payload = ByteArray(0))
        val entry = SyncItem(key = "e", recordType = "CycleJournalEntry", payload = ByteArray(0))
        val stranger = SyncItem(key = "x", recordType = "Nobody", payload = ByteArray(0))

        assertTrue(composite.accepts(steps))
        assertFalse(composite.accepts(entry))
        assertFalse(composite.accepts(stranger))

        val written = composite.writeItems(listOf(steps, entry, stranger))

        assertEquals(setOf("s", "e"), written)
        assertEquals(listOf(steps), health.written)
        assertEquals(listOf(entry), journal.written)
    }
}
