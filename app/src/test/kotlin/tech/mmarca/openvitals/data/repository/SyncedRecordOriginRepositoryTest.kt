package tech.mmarca.openvitals.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.data.local.syncorigin.SyncedRecordOriginDao
import tech.mmarca.openvitals.data.local.syncorigin.SyncedRecordOriginEntity
import tech.mmarca.openvitals.healthconnect.SyncedSourceOverlay

/** Persisting fingerprint-to-origin rows and keeping the display overlay in step. */
class SyncedRecordOriginRepositoryTest {

    private class FakeDao : SyncedRecordOriginDao {
        val rows = linkedMapOf<String, SyncedRecordOriginEntity>()

        override suspend fun count(): Int = rows.size

        override suspend fun pageAfter(after: String, limit: Int): List<SyncedRecordOriginEntity> {
            pageReads++
            return rows.values.filter { it.clientRecordId > after }.sortedBy { it.clientRecordId }.take(limit)
        }

        var pageReads = 0

        override suspend fun upsertAll(origins: List<SyncedRecordOriginEntity>) {
            origins.forEach { rows[it.clientRecordId] = it }
        }
    }

    private val dao = FakeDao()
    private val repository = SyncedRecordOriginRepository(dao, TestDispatcherProvider)

    @After
    fun tearDown() {
        // The overlay is process-wide; leave nothing behind for other tests.
        SyncedSourceOverlay.update(emptyMap())
    }

    @Test
    fun `recordOrigins persists rows and adds them to the overlay`() = runTest {
        repository.recordOrigins(mapOf("sync_a" to "com.gadgetbridge"))

        assertEquals(
            SyncedRecordOriginEntity("sync_a", "com.gadgetbridge"),
            dao.rows["sync_a"],
        )
        assertEquals("com.gadgetbridge", SyncedSourceOverlay.originFor("sync_a"))
    }

    @Test
    fun `a re-sync upserts rather than duplicating or failing`() = runTest {
        repository.recordOrigins(mapOf("sync_a" to "com.gadgetbridge"))
        repository.recordOrigins(mapOf("sync_a" to "com.gadgetbridge", "sync_b" to "com.polar"))

        assertEquals(2, dao.rows.size)
        assertEquals("com.polar", SyncedSourceOverlay.originFor("sync_b"))
    }

    @Test
    fun `an empty mapping writes nothing`() = runTest {
        repository.recordOrigins(emptyMap())

        assertEquals(0, dao.rows.size)
    }

    @Test
    fun `preservedOrigins loads the table for the sync read path`() = runTest {
        dao.upsertAll(listOf(SyncedRecordOriginEntity("sync_c", "com.fitbit.FitbitMobile")))

        val preserved = repository.preservedOrigins()

        assertEquals("com.fitbit.FitbitMobile", preserved("sync_c"))
        assertNull(preserved("sync_unknown"))
        // Loading also hydrates the display overlay (app-start warm path).
        assertEquals("com.fitbit.FitbitMobile", SyncedSourceOverlay.originFor("sync_c"))
        assertNull(SyncedSourceOverlay.originFor("sync_unknown"))
    }

    @Test
    fun `the table is read once, however many batches land`() = runTest {
        // It used to be re-read whole after every received batch.
        dao.upsertAll(listOf(SyncedRecordOriginEntity("sync_c", "com.fitbit.FitbitMobile")))

        repeat(50) { batch -> repository.recordOrigins(mapOf("sync_batch_$batch" to "com.polar")) }
        val readsAfterBatches = dao.pageReads
        repository.preservedOrigins()

        assertEquals(readsAfterBatches, dao.pageReads)
        assertEquals("com.fitbit.FitbitMobile", SyncedSourceOverlay.originFor("sync_c"))
        assertEquals("com.polar", SyncedSourceOverlay.originFor("sync_batch_49"))
        assertEquals(51, SyncedSourceOverlay.size)
    }

    @Test
    fun `a table larger than one page loads completely`() = runTest {
        val ids = (0 until 25_000).map { "sync_%032x".format(it) }
        dao.upsertAll(ids.map { SyncedRecordOriginEntity(it, "com.gadgetbridge") })

        repository.hydrateOverlay()

        assertEquals(25_000, SyncedSourceOverlay.size)
        assertEquals("com.gadgetbridge", SyncedSourceOverlay.originFor(ids.first()))
        assertEquals("com.gadgetbridge", SyncedSourceOverlay.originFor(ids.last()))
        assertTrue(dao.pageReads >= 3)
    }
}
