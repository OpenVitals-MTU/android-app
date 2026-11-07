package tech.mmarca.openvitals.devices.xiaomi

import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.FakePreferences
import tech.mmarca.openvitals.data.repository.contract.FakeScaleWeighInRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.domain.model.ScaleReading
import tech.mmarca.openvitals.domain.preferences.BiologicalSex
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.HealthConnectSyncDisabledException

/**
 * From the scale's broadcasts to Room and Health Connect, with a real
 * weigh-in: the two frames a scale sent, captured for xiaomi-ble's tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScaleWeighInIngestTest {

    private val address = "8C:D0:B2:F6:BE:EF"
    private val key = "0728974d657a4b60964c1b1677f35f7c".hexToByteArray()
    private val scaleTimestamp = 1744250605L

    /** 69.9 kg, 92 bpm, 543.2 ohm at 50 kHz. */
    private val weightFrame = advert("4859d53b0abc078ff2348c844138e930220000009e538599")

    /** 497.6 ohm at 250 kHz, same weigh-in. */
    private val secondFrame = advert("4859d53b0bd6ef0b25db72785e7e2f46d6000000d8642df6")

    private val store = XiaomiScaleStore(FakeSharedPreferences()).also { it.setUp(address, "Scale", key) }
    private val weighIns = FakeScaleWeighInRepository()

    private val written = mutableListOf<List<Record>>()
    private var writeFailure: Exception? = null
    private val importRepository = mockk<AppleHealthImportRepository> {
        coEvery { insertImportedRecords(any()) } answers {
            writeFailure?.let { throw it }
            written += firstArg<List<Record>>()
        }
        coEvery { deleteImportedRecordsByClientIds(any(), any()) } returns Unit
    }
    private var granted = ScaleWritePermissions
    private val healthRepository = mockk<HealthRepository> {
        coEvery { grantedPermissions() } answers { granted }
    }
    private val writer = ScaleWeighInWriter(
        weighIns = weighIns,
        importRepository = importRepository,
        healthRepository = healthRepository,
        bodyProfilePreferences = FakePreferences(
            initialProfile = BodyProfile(birthYear = 1990, heightCm = 175.0, sex = BiologicalSex.MALE),
        ),
        bodyRepository = mockk<BodyRepository>(),
        // In the background Health Connect shows the app its own records only: the stored profile is used.
        hc = mockk<HealthConnectManager> { coEvery { readsOtherAppsDataNow() } returns false },
        store = store,
    )

    private fun advert(frame: String, from: String = address) = ScaleAdvert(from, frame.hexToByteArray())

    private fun TestScope.ingest() = ScaleWeighInIngest(
        store = store,
        weighIns = weighIns,
        writer = writer,
        scope = backgroundScope,
        // Twenty seconds after the scale stamped the weigh-in.
        now = { Instant.ofEpochSecond(scaleTimestamp + 20) },
    )

    @Test
    fun `the first frame the key opens says which of the scale's users this is, and saves the weigh-in`() = runTest {
        ingest().ingest(listOf(weightFrame))

        assertEquals(1, store.config.value.profile)
        assertEquals(ScaleReading(69.9, 92, impedanceLowOhm = 543.2), weighIns.all.single().reading)
        assertEquals(Instant.ofEpochSecond(scaleTimestamp), weighIns.all.single().time)
        assertEquals(
            listOf("weight", "heart_rate", "body_fat", "lean_mass", "body_water")
                .map { "xiaomi_s400_${it}_${scaleTimestamp}_p1" },
            written.single().map { it.metadata.clientRecordId },
        )
        assertFalse(weighIns.all.single().isPending)
    }

    @Test
    fun `a frame that adds to the weigh-in rewrites it under a higher version`() = runTest {
        val ingest = ingest()

        ingest.ingest(listOf(weightFrame))
        ingest.ingest(listOf(secondFrame))

        assertEquals(ScaleReading(69.9, 92, 543.2, 497.6), weighIns.all.single().reading)
        val versions = written.map { batch -> batch.first().metadata.clientRecordVersion }
        assertEquals(2, versions.size)
        assertTrue("Health Connect ignores an equal version", versions[1] > versions[0])
    }

    @Test
    fun `the scale repeating itself writes nothing more`() = runTest {
        val ingest = ingest()

        ingest.ingest(listOf(weightFrame, weightFrame))
        ingest.ingest(listOf(weightFrame))

        assertEquals(1, written.size)
    }

    @Test
    fun `another scale and another person leave nothing behind`() = runTest {
        // This scale, but the person in its second user slot.
        store.setProfile(2)
        val ingest = ingest()

        ingest.ingest(listOf(advert(weightFrame.serviceData.toHexString(), from = "84:46:93:64:A5:E6")))
        assertNull("another scale is not even looked at", store.config.value.ignoredProfile)

        ingest.ingest(listOf(weightFrame))

        assertEquals(emptyList<Any>(), weighIns.all)
        assertEquals(emptyList<Any>(), written)
        // Which slot it was is kept, so the screen can offer "that was me". The measurement is not.
        assertEquals(1, store.config.value.ignoredProfile?.profile)
    }

    @Test
    fun `a key that stopped fitting is flagged until a frame opens again`() = runTest {
        val ingest = ingest()
        store.changeKey(ByteArray(16) { 7 })

        ingest.ingest(listOf(weightFrame))
        assertTrue(store.config.value.keyRejected)
        assertEquals(emptyList<Any>(), weighIns.all)

        store.changeKey(key)
        store.setKeyRejected(true)
        ingest.ingest(listOf(secondFrame))

        assertFalse(store.config.value.keyRejected)
    }

    @Test
    fun `the user slot is learned once, not moved by a later weigh-in`() = runTest {
        val ingest = ingest()
        ingest.ingest(listOf(weightFrame))

        // Someone else steps on this scale: it files them under slot 2. Built for this test with the scale's key.
        ingest.ingest(listOf(advert("4859d53b209a029d137973bdf79cc0351f0000000f3d98df")))

        assertEquals(1, store.config.value.profile)
        assertEquals(2, store.config.value.ignoredProfile?.profile)
        assertEquals(1, weighIns.all.size)
    }

    @Test
    fun `a weigh-in Health Connect refuses waits in Room, with the reason, until a retry lands it`() = runTest {
        val refusals = mapOf(
            SecurityException("no grant") to ScaleWriteFailure.PERMISSION,
            HealthConnectSyncDisabledException() to ScaleWriteFailure.SYNC_PAUSED,
            IllegalStateException("Health Connect is updating") to ScaleWriteFailure.OTHER,
        )
        val ingest = ingest()
        writeFailure = refusals.keys.first()
        ingest.ingest(listOf(weightFrame))

        assertEquals(
            refusals.values.toList(),
            refusals.keys.map { failure ->
                writeFailure = failure
                writer.writePending()
                store.config.value.writeFailure
            },
        )
        assertTrue(weighIns.all.single().isPending)

        writeFailure = null
        writer.writePending()

        assertFalse(weighIns.all.single().isPending)
        assertNull(store.config.value.writeFailure)
        assertEquals(1, written.size)
    }

    @Test
    fun `without the weight grant nothing is written and the weigh-in waits`() = runTest {
        granted = setOf(ScaleRecordKind.BODY_FAT.writePermission)

        ingest().ingest(listOf(weightFrame))

        assertEquals(emptyList<Any>(), written)
        assertEquals(ScaleWriteFailure.PERMISSION, store.config.value.writeFailure)
        assertTrue(weighIns.all.single().isPending)

        granted = setOf(ScaleRecordKind.WEIGHT.writePermission, ScaleRecordKind.HEART_RATE.writePermission)
        writer.writePending()

        assertEquals(listOf(WeightRecord::class, HeartRateRecord::class), written.single().map { it::class })
    }

    @Test
    fun `a slow write frees the broadcast at the budget and still lands`() = runTest {
        val healthConnect = CompletableDeferred<Unit>()
        coEvery { importRepository.insertImportedRecords(any()) } coAnswers {
            healthConnect.await()
            written += firstArg<List<Record>>()
        }
        var broadcastEnded = false

        ingest().onAdverts(listOf(weightFrame), budgetMillis = 8_000) { broadcastEnded = true }
        advanceTimeBy(7_999)
        assertFalse(broadcastEnded)
        advanceTimeBy(2)
        assertTrue(broadcastEnded)
        assertTrue("the reading is safe in Room meanwhile", weighIns.all.single().isPending)

        healthConnect.complete(Unit)
        runCurrent()

        assertEquals(1, written.size)
        assertFalse(weighIns.all.single().isPending)
    }

    @Test
    fun `a deleted weigh-in is gone from both stores and the scale cannot bring it back`() = runTest {
        val ingest = ingest()
        ingest.ingest(listOf(weightFrame))

        writer.delete(weighIns.all.single())

        assertEquals(emptyList<Any>(), weighIns.all)
        coVerify(exactly = 1) {
            importRepository.deleteImportedRecordsByClientIds(
                BodyFatRecord::class,
                listOf("xiaomi_s400_body_fat_${scaleTimestamp}_p1"),
            )
        }
        coVerify(exactly = ScaleRecordKind.entries.size) {
            importRepository.deleteImportedRecordsByClientIds(any(), any())
        }

        // The scale is still showing the result and broadcasting it.
        ingest.ingest(listOf(secondFrame))

        assertEquals(emptyList<Any>(), weighIns.all)
    }
}
