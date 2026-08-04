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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.features.devicesync.protocol.SyncItem
import tech.mmarca.openvitals.features.devicesync.store.HealthConnectSyncStore
import tech.mmarca.openvitals.features.devicesync.store.encodeSyncRecordPayload
import tech.mmarca.openvitals.features.devicesync.store.syncFingerprint
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/**
 * Port of the Flutter `health_connect_sync_store_test.dart` suite.
 *
 * The Dart tests drive a fake `HealthDataSource`; the Kotlin store reads
 * through [HealthConnectManager] and writes through
 * [AppleHealthImportRepository], so [FakeHealthConnect] models the same
 * in-memory Health Connect import surface behind those two collaborators:
 * records live keyed by clientRecordId, reads return them, writes upsert.
 */
class HealthConnectSyncStoreTest {

    /** Models Health Connect's import surface in memory. */
    private class FakeHealthConnect {
        private val byClientId = linkedMapOf<String, Record>()

        /** Record types whose batch insert should be rejected. */
        var failTypes: Set<String> = emptySet()

        val count: Int get() = byClientId.size

        fun seed(record: Record) {
            // Emulate Health Connect assigning the sync fingerprint as
            // clientRecordId, as the write path does.
            byClientId[syncFingerprint(record)] = record
        }

        val manager: HealthConnectManager = mockk<HealthConnectManager>().also { hc ->
            coEvery { hc.readRecordsForSync(any(), any(), any()) } answers {
                val recordClass = firstArg<KClass<out Record>>()
                byClientId.values.filter { recordClass.isInstance(it) }
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

    private val windowStart: Instant = Instant.parse("2025-01-01T00:00:00Z")
    private val windowEnd: Instant = Instant.parse("2027-01-01T00:00:00Z")

    private lateinit var hc: FakeHealthConnect
    private lateinit var store: HealthConnectSyncStore

    private fun storeOver(fake: FakeHealthConnect) = HealthConnectSyncStore(
        healthConnectManager = fake.manager,
        importRepository = fake.repository,
        windowStart = windowStart,
        windowEnd = windowEnd,
    )

    private fun weight(day: Int, kilograms: Double): WeightRecord = WeightRecord(
        time = Instant.parse("2026-01-%02dT00:00:00Z".format(day)),
        zoneOffset = null,
        weight = Mass.kilograms(kilograms),
        metadata = Metadata.manualEntry(device = Device(type = Device.TYPE_PHONE), clientRecordId = "ignored"),
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
    fun `writeItems reconstructs typed records under the fingerprint id`() = runTest {
        // Build items on a source phone, then write them into a fresh target.
        val source = FakeHealthConnect().apply { seed(weight(3, 80.0)) }
        val items = storeOver(source).readItems(setOf("WeightRecord"))

        store.writeItems(items)
        assertEquals(1, hc.count)

        // The written record is typed and re-fingerprints to the same key, so
        // it reappears from readItems under that key — the session's dedup
        // baseline (seeded from readItems) then recognises a re-sync as a
        // duplicate.
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

        // Writing them again keys on the same fingerprint clientRecordIds, so
        // Health Connect upserts and the count stays put.
        store.writeItems(items)
        assertEquals(2, hc.count)
        val after = store.readItems(setOf("WeightRecord"))
        assertEquals(items.map { it.key }.toSet(), after.map { it.key }.toSet())
    }

    @Test
    fun `writeItems ignores a peer-chosen key and writes under the content fingerprint`() = runTest {
        val record = weight(6, 65.0)
        val honestKey = syncFingerprint(record)
        // A hostile peer sets the SyncItem key to an existing id it wants to
        // clobber (e.g. an apple_health_* record we hold). The store must NOT
        // trust it.
        val hostile = SyncItem(
            key = "apple_health_deadbeef",
            recordType = "WeightRecord",
            payload = encodeSyncRecordPayload(record),
        )

        store.writeItems(listOf(hostile))

        val written = store.readItems(setOf("WeightRecord"))
        // Written under the recomputed content fingerprint, never the peer's
        // key — so the peer can only ever address the record it actually sent.
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
    fun `writeItems excludes a rejected type from its written-keys result`() = runTest {
        hc.failTypes = setOf("WeightRecord")
        val source = FakeHealthConnect().apply { seed(weight(9, 73.0)) }
        val items = storeOver(source).readItems(setOf("WeightRecord"))

        val written = store.writeItems(items)

        // The batch was rejected, so its key is NOT reported as written — the
        // session therefore won't count it as imported.
        assertEquals(emptySet<String>(), written)
        assertEquals(0, hc.count)
    }
}
