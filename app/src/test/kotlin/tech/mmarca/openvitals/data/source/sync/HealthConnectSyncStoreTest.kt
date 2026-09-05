package tech.mmarca.openvitals.data.source.sync

import android.util.Log
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.local.syncorigin.SyncedRecordOriginDao
import tech.mmarca.openvitals.data.local.syncorigin.SyncedRecordOriginEntity
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.SyncedRecordOriginRepository
import tech.mmarca.openvitals.data.repository.TestDispatcherProvider
import tech.mmarca.openvitals.features.devicesync.protocol.SyncAborted
import tech.mmarca.openvitals.features.devicesync.protocol.SyncItem
import tech.mmarca.openvitals.features.devicesync.store.HealthConnectSyncStore
import tech.mmarca.openvitals.features.devicesync.store.encodeSyncRecordPayload
import tech.mmarca.openvitals.features.devicesync.store.syncFingerprint
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.SyncedSourceOverlay

/**
 * [FakeHealthConnect] models the import surface behind [HealthConnectManager] and
 * [AppleHealthImportRepository]: records keyed by clientRecordId, reads return them, writes upsert.
 */
class HealthConnectSyncStoreTest {

    /** The old materializing read over the streaming contract; these tests assert on content. */
    private suspend fun HealthConnectSyncStore.readItems(types: Set<String>): List<SyncItem> =
        readItemChunks(types, chunkSize = 500).toList().flatten()

    /** Models Health Connect's import surface in memory. */
    private class FakeHealthConnect {
        private val byClientId = linkedMapOf<String, Record>()

        /** Record types whose batch insert should be rejected. */
        var failTypes: Set<String> = emptySet()

        val count: Int get() = byClientId.size

        fun seed(record: Record) {
            // Health Connect assigns the sync fingerprint as clientRecordId, as the write path does.
            byClientId[syncFingerprint(record)] = record
        }

        val manager: HealthConnectManager = mockk<HealthConnectManager>().also { hc ->
            coEvery { hc.forEachSyncRecordPage(any(), any(), any(), any()) } coAnswers {
                val recordClass = firstArg<KClass<out Record>>()
                val action = arg<suspend (List<Record>) -> Unit>(3)
                val records = byClientId.values.filter { recordClass.isInstance(it) }
                if (records.isNotEmpty()) action(records)
            }
        }

        val repository: AppleHealthImportRepository = mockk<AppleHealthImportRepository>().also { repo ->
            coEvery { repo.insertImportedRecords(any()) } answers {
                val records = firstArg<List<Record>>()
                val type = records.firstOrNull()?.let { it::class.simpleName }
                if (records.isNotEmpty() && type in failTypes) {
                    error("insert rejected for $type")
                }
                records.forEach { record ->
                    byClientId[record.metadata.clientRecordId ?: error("no client id")] = record
                }
            }
        }
    }

    /** In-memory `synced_record_origins`, one per fake phone. */
    private class FakeOriginDao : SyncedRecordOriginDao {
        val rows = linkedMapOf<String, SyncedRecordOriginEntity>()

        override suspend fun all(): List<SyncedRecordOriginEntity> = rows.values.toList()

        override suspend fun upsertAll(origins: List<SyncedRecordOriginEntity>) {
            origins.forEach { rows[it.clientRecordId] = it }
        }
    }

    private val windowStart: Instant = Instant.parse("2025-01-01T00:00:00Z")
    private val windowEnd: Instant = Instant.parse("2027-01-01T00:00:00Z")

    private lateinit var hc: FakeHealthConnect
    private lateinit var store: HealthConnectSyncStore

    private fun storeOver(
        fake: FakeHealthConnect,
        originDao: FakeOriginDao = FakeOriginDao(),
        chunkPayloadByteCap: Int = Int.MAX_VALUE,
    ) = HealthConnectSyncStore(
        healthConnectManager = fake.manager,
        importRepository = fake.repository,
        originRepository = SyncedRecordOriginRepository(originDao, TestDispatcherProvider),
        localPackageName = "tech.mmarca.openvitals",
        windowStart = windowStart,
        windowEnd = windowEnd,
        chunkPayloadByteCap = chunkPayloadByteCap,
    )

    private fun weight(
        day: Int,
        kilograms: Double,
        clientRecordId: String = "ignored",
    ): WeightRecord = WeightRecord(
        time = Instant.parse("2026-01-%02dT00:00:00Z".format(day)),
        zoneOffset = null,
        weight = Mass.kilograms(kilograms),
        metadata = Metadata.manualEntry(device = Device(type = Device.TYPE_PHONE), clientRecordId = clientRecordId),
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        hc = FakeHealthConnect()
        store = storeOver(hc)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        // The repository hydrates the process-wide display overlay; reset it.
        SyncedSourceOverlay.update(emptyMap())
    }

    @Test
    fun `readItems keys each record by its content fingerprint`() = runTest {
        hc.seed(weight(1, 70.0))
        hc.seed(weight(2, 71.0))

        val items = store.readItems(setOf("WeightRecord"))

        assertEquals(2, items.size)
        items.forEach { item ->
            assertTrue(item.key.startsWith("sync_"))
            assertEquals("WeightRecord", item.recordType)
        }
    }

    @Test
    fun `chunks flush when the payload byte cap is reached`() = runTest {
        hc.seed(weight(1, 70.0))
        hc.seed(weight(2, 71.0))
        hc.seed(weight(3, 72.0))

        // A 1-byte cap forces a flush after every item.
        val chunks = storeOver(hc, chunkPayloadByteCap = 1)
            .readItemChunks(setOf("WeightRecord"), chunkSize = 500)
            .toList()

        assertEquals(3, chunks.size)
        assertTrue(chunks.all { it.size == 1 })
    }

    @Test
    fun `a failing read aborts the stream instead of silently truncating`() = runTest {
        coEvery {
            hc.manager.forEachSyncRecordPage(any(), any(), any(), any())
        } throws IllegalStateException("quota has been exceeded")

        val result = runCatching { store.readItemChunks(setOf("WeightRecord"), 500).toList() }

        val error = result.exceptionOrNull()
        assertTrue(error is SyncAborted)
        assertTrue(error!!.message.orEmpty().contains("WeightRecord"))
    }

    @Test
    fun `writeItems reconstructs typed records under the fingerprint id`() = runTest {
        // Build items on a source phone, then write them into a fresh target.
        val source = FakeHealthConnect().apply { seed(weight(3, 80.0)) }
        val items = storeOver(source).readItems(setOf("WeightRecord"))

        store.writeItems(items)
        assertEquals(1, hc.count)

        // The written record re-fingerprints to the same key, so a re-sync reads as a duplicate.
        val written = store.readItems(setOf("WeightRecord"))
        assertEquals(items.single().key, written.single().key)
    }

    @Test
    fun `writing the same items twice upserts rather than duplicating`() = runTest {
        val source = FakeHealthConnect().apply {
            seed(weight(4, 60.0))
            seed(weight(5, 61.0))
        }
        val items = storeOver(source).readItems(setOf("WeightRecord"))

        store.writeItems(items)
        assertEquals(2, hc.count)

        // The same fingerprint ids, so Health Connect upserts and the count stays put.
        store.writeItems(items)
        assertEquals(2, hc.count)
        val after = store.readItems(setOf("WeightRecord"))
        assertEquals(items.map { it.key }.toSet(), after.map { it.key }.toSet())
    }

    @Test
    fun `writeItems ignores a peer-chosen key and writes under the content fingerprint`() = runTest {
        val record = weight(6, 65.0)
        val honestKey = syncFingerprint(record)
        // A hostile peer sets the key to an existing id it wants to clobber. The store must not trust it.
        val hostile = SyncItem(
            key = "apple_health_deadbeef",
            recordType = "WeightRecord",
            payload = encodeSyncRecordPayload(record),
        )

        store.writeItems(listOf(hostile))

        val written = store.readItems(setOf("WeightRecord"))
        // Written under the recomputed fingerprint, never the peer's key.
        assertEquals(honestKey, written.single().key)
        assertNotEquals("apple_health_deadbeef", written.single().key)
    }

    @Test
    fun `writeItems returns the keys that actually landed`() = runTest {
        val source = FakeHealthConnect().apply { seed(weight(8, 72.0)) }
        val items = storeOver(source).readItems(setOf("WeightRecord"))

        val written = store.writeItems(items)

        assertEquals(setOf(items.single().key), written)
    }

    @Test
    fun `readItems passes a preserved origin through instead of the local attribution`() = runTest {
        // Phone B holds the record from A (Gadgetbridge) under its fingerprint.
        // When B re-sends toward C, the item must announce Gadgetbridge, not B.
        val fingerprint = syncFingerprint(weight(10, 74.0))
        hc.seed(weight(10, 74.0, clientRecordId = fingerprint))
        val dao = FakeOriginDao().apply {
            rows[fingerprint] = SyncedRecordOriginEntity(
                clientRecordId = fingerprint,
                originPackage = "com.espruino.gadgetbridge.banglejs",
            )
        }

        val items = storeOver(hc, dao).readItems(setOf("WeightRecord"))

        assertEquals("com.espruino.gadgetbridge.banglejs", items.single().originPackage)
    }

    @Test
    fun `writeItems persists a foreign origin for each landed record`() = runTest {
        val record = weight(11, 75.0)
        val dao = FakeOriginDao()
        val item = SyncItem(
            key = syncFingerprint(record),
            recordType = "WeightRecord",
            payload = encodeSyncRecordPayload(record),
            originPackage = "com.espruino.gadgetbridge.banglejs",
        )

        val written = storeOver(hc, dao).writeItems(listOf(item))

        assertEquals(setOf(item.key), written)
        assertEquals(
            "com.espruino.gadgetbridge.banglejs",
            dao.rows[item.key]?.originPackage,
        )
    }

    @Test
    fun `writeItems does not persist an origin that is our own package or absent`() = runTest {
        val ownRecord = weight(12, 76.0)
        val legacyRecord = weight(13, 77.0)
        val dao = FakeOriginDao()
        val items = listOf(
            // A record genuinely authored in OpenVitals on the sender.
            SyncItem(
                key = syncFingerprint(ownRecord),
                recordType = "WeightRecord",
                payload = encodeSyncRecordPayload(ownRecord),
                originPackage = "tech.mmarca.openvitals",
            ),
            // A record from a peer build that predates the origin field.
            SyncItem(
                key = syncFingerprint(legacyRecord),
                recordType = "WeightRecord",
                payload = encodeSyncRecordPayload(legacyRecord),
            ),
        )

        val written = storeOver(hc, dao).writeItems(items)

        assertEquals(items.map { it.key }.toSet(), written)
        assertEquals(0, dao.rows.size)
    }

    @Test
    fun `writeItems excludes a rejected type from its written-keys result`() = runTest {
        hc.failTypes = setOf("WeightRecord")
        val source = FakeHealthConnect().apply { seed(weight(9, 73.0)) }
        val items = storeOver(source).readItems(setOf("WeightRecord"))

        val written = store.writeItems(items)

        // The batch was rejected, so its key is not reported as written.
        assertEquals(emptySet<String>(), written)
        assertEquals(0, hc.count)
    }
}
