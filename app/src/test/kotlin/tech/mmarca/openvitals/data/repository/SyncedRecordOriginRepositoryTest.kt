package tech.mmarca.openvitals.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.data.local.syncorigin.SyncedRecordOriginDao
import tech.mmarca.openvitals.data.local.syncorigin.SyncedRecordOriginEntity
import tech.mmarca.openvitals.healthconnect.SyncedSourceOverlay

/** Persisting fingerprint-to-origin rows and keeping the display overlay in step. */
class SyncedRecordOriginRepositoryTest {

    private class FakeDao : SyncedRecordOriginDao {
        val rows = linkedMapOf<String, SyncedRecordOriginEntity>()

        override suspend fun all(): List<SyncedRecordOriginEntity> = rows.values.toList()

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
    fun `recordOrigins persists rows and refreshes the overlay`() = runTest {
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

        assertEquals(mapOf("sync_c" to "com.fitbit.FitbitMobile"), preserved)
        // Loading also hydrates the display overlay (app-start warm path).
        assertEquals("com.fitbit.FitbitMobile", SyncedSourceOverlay.originFor("sync_c"))
        assertNull(SyncedSourceOverlay.originFor("sync_unknown"))
    }
}
