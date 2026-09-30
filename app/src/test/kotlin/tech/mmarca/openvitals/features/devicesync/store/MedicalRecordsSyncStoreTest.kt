package tech.mmarca.openvitals.features.devicesync.store

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.FakeMedicalRecordsRepository
import tech.mmarca.openvitals.features.devicesync.protocol.SYNC_NONCE_BYTES
import tech.mmarca.openvitals.features.devicesync.protocol.SyncPipe
import tech.mmarca.openvitals.features.devicesync.protocol.SyncReport
import tech.mmarca.openvitals.features.devicesync.protocol.SyncRole
import tech.mmarca.openvitals.features.devicesync.protocol.SyncSession
import tech.mmarca.openvitals.features.devicesync.protocol.SyncSessionConfig
import tech.mmarca.openvitals.features.devicesync.protocol.SyncTypeSummary

/** Medical records between two phones: the import pipeline on arrival, content keys, and the Patient check. */
class MedicalRecordsSyncStoreTest {

    private val phoneA = FakeMedicalRecordsRepository().apply { addSource("clinic", "Clinic") }
    private val phoneB = FakeMedicalRecordsRepository()

    private val ana = """{"resourceType":"Patient","id":"p1","name":[{"given":["Ana"],"family":"Silva"}],"birthDate":"1990-04-02"}"""
    private val bruno = """{"resourceType":"Patient","id":"p9","name":[{"given":["Bruno"],"family":"Costa"}],"birthDate":"1985-11-20"}"""
    private val vaccine = """{"resourceType":"Immunization","id":"i1","status":"completed","vaccineCode":{"text":"Tetanus"},""" +
        """"patient":{"reference":"Patient/p1"},"occurrenceDateTime":"2021-05-01"}"""
    private val allergy = """{"resourceType":"AllergyIntolerance","id":"a1","code":{"text":"Penicillin"},"patient":{"reference":"Patient/p1"}}"""

    private class Side(val store: MedicalRecordsSyncStore, val report: SyncReport) {
        val medical: SyncTypeSummary get() = report.typeSummaries.single { it.recordType == MedicalRecordsSyncTypes.RECORD }
    }

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun config(role: SyncRole) = SyncSessionConfig(
        role = role,
        confirmCode = { true },
        deviceName = role.name,
        supportedTypes = MedicalRecordsSyncTypes.all,
        nonce = ByteArray(SYNC_NONCE_BYTES) { if (role == SyncRole.HOST) 0x11 else 0x22 },
        handshakeTimeoutMillis = 5_000,
        confirmTimeoutMillis = 5_000,
        batchTimeoutMillis = 5_000,
        batchSize = 2,
    )

    /** One real session between the phones, over an in-memory link. */
    private suspend fun CoroutineScope.sync(): Pair<Side, Side> {
        val (pipeA, pipeB) = SyncPipe.create()
        val storeA = MedicalRecordsSyncStore(phoneA, today = { LocalDate.of(2026, 9, 29) })
        val storeB = MedicalRecordsSyncStore(phoneB, today = { LocalDate.of(2026, 9, 29) })
        val reports = awaitAll(
            async { SyncSession(transport = pipeA, store = storeA, config = config(SyncRole.HOST)).run() },
            async { SyncSession(transport = pipeB, store = storeB, config = config(SyncRole.GUEST)).run() },
        )
        return Side(storeA, reports[0]) to Side(storeB, reports[1])
    }

    private fun FakeMedicalRecordsRepository.refs() = records.map { "${it.ref.resourceType}/${it.ref.resourceId}" }.toSet()

    @Test
    fun `records reach an empty phone in a source of the same origin, and a second sync finds them present`() = runTest {
        phoneA.add("clinic", "Patient", "p1", ana)
        phoneA.add("clinic", "Immunization", "i1", vaccine)
        phoneA.add("clinic", "AllergyIntolerance", "a1", allergy)

        val (_, first) = sync()

        val source = phoneB.ownSources().single()
        assertEquals("https://clinic.example/fhir", source.fhirBaseUri)
        assertEquals("Clinic", source.displayName)
        assertEquals(setOf("Patient/p1", "Immunization/i1", "AllergyIntolerance/a1"), phoneB.refs())
        assertEquals(vaccine, phoneB.records.single { it.ref.resourceId == "i1" }.json)
        assertEquals(3, first.medical.imported)

        val (againA, againB) = sync()

        assertEquals(0, againB.medical.imported)
        assertEquals(3, againB.medical.duplicateSkipped)
        // The copies phone B sends back are phone A's own records.
        assertEquals(3, againA.medical.duplicateSkipped)
        assertEquals(3, phoneB.records.size)
    }

    @Test
    fun `a newer edit replaces the other phone's copy, and the older copy does not come back`() = runTest {
        phoneA.add("clinic", "Immunization", "i1", vaccine)
        sync()
        val edited = vaccine.replace(""""id":"i1",""", """"id":"i1","meta":{"lastUpdated":"2026-09-29T10:00:00Z"},""")
            .replace("Tetanus", "Tetanus booster")
        phoneA.upsert("clinic", "4.0.1", listOf(edited))

        val (a, b) = sync()

        assertEquals(1, b.medical.imported)
        assertEquals(listOf(edited), phoneB.records.map { it.json })
        assertEquals(1, a.medical.refused)
        assertEquals(listOf(edited), phoneA.records.map { it.json })
    }

    @Test
    fun `two versions with no edit time stay apart, each phone keeping its own`() = runTest {
        phoneA.add("clinic", "Immunization", "i1", vaccine)
        sync()
        val edited = vaccine.replace("Tetanus", "Tetanus booster")
        phoneA.upsert("clinic", "4.0.1", listOf(edited))

        val (a, b) = sync()

        assertEquals(1, a.medical.refused)
        assertEquals(1, b.medical.refused)
        assertEquals(listOf(edited), phoneA.records.map { it.json })
        assertEquals(listOf(vaccine), phoneB.records.map { it.json })
    }

    @Test
    fun `a source another app holds here is skipped, but its copy of a record counts as present`() = runTest {
        phoneA.add("clinic", "Immunization", "i1", vaccine)
        phoneA.add("clinic", "AllergyIntolerance", "a1", allergy)
        phoneB.addSource("hospital", "Hospital", packageName = "org.hospital.app", baseUri = "https://clinic.example/fhir")
        phoneB.add("hospital", "Immunization", "i1", vaccine)

        val (_, b) = sync()

        assertEquals(1, b.medical.duplicateSkipped)
        assertEquals(1, b.medical.refused)
        assertEquals(0, b.medical.imported)
        assertTrue(phoneB.ownSources().isEmpty())
    }

    @Test
    fun `Patient records naming someone else hold back every medical record, on both phones`() = runTest {
        phoneA.add("clinic", "Patient", "p1", ana)
        phoneA.add("clinic", "Immunization", "i1", vaccine)
        phoneB.addSource("mine", "Mine")
        phoneB.add("mine", "Patient", "p9", bruno)

        val (a, b) = sync()

        assertEquals(MedicalHeldBack.OTHER_PERSON, a.store.heldBack)
        assertEquals(MedicalHeldBack.OTHER_PERSON, b.store.heldBack)
        assertEquals(2, b.medical.refused)
        assertEquals(setOf("Patient/p9"), phoneB.refs())
        assertTrue(medicalSyncReportText(b.report, b.store.heldBack).contains("name someone else"))
    }

    @Test
    fun `records naming two people hold back all of them, the first Patient too`() = runTest {
        phoneA.addSource("lab", "Lab")
        phoneA.add("clinic", "Patient", "p1", ana)
        phoneA.add("lab", "Patient", "p9", bruno)
        phoneA.add("clinic", "Immunization", "i1", vaccine)

        val (_, b) = sync()

        assertEquals(MedicalHeldBack.SEVERAL_PEOPLE, b.store.heldBack)
        assertTrue(phoneB.records.isEmpty())
    }

    @Test
    fun `the same person on both phones passes, and a record pre-flight holds back counts as not added`() = runTest {
        phoneA.add("clinic", "Patient", "p1", ana)
        phoneA.add("clinic", "AllergyIntolerance", "a2", """{"resourceType":"AllergyIntolerance","id":"a2","note":[]}""")
        phoneB.addSource("mine", "Mine")
        phoneB.add("mine", "Patient", "p5", ana.replace("\"p1\"", "\"p5\""))

        val (_, b) = sync()

        assertNull(b.store.heldBack)
        assertEquals(1, b.medical.imported)
        assertTrue(medicalSyncReportText(b.report, null).contains("Written 1, already present 0, skipped 0, rejected 1"))
    }

    @Test
    fun `the key ignores key order, spacing and the base's case, but not the content, the source, or the version`() {
        fun key(base: String, version: String, json: String) =
            medicalRecordKey(RecordIdentity.of(base, version, "Immunization", "i1"), Json.parseToJsonElement(json).jsonObject)
        val plain = """{"id":"i1","resourceType":"Immunization"}"""
        val reference = key("https://clinic.example/fhir/", "4.0.1", plain)

        assertEquals(reference, key("https://CLINIC.example/fhir", "4.0.1", """{ "resourceType": "Immunization", "id": "i1" }"""))
        assertNotEquals(reference, key("https://clinic.example/fhir", "4.0.1", """{"id":"i1","resourceType":"Immunization","status":"completed"}"""))
        assertNotEquals(reference, key("https://lab.example/fhir", "4.0.1", plain))
        assertNotEquals(reference, key("https://clinic.example/fhir", "4.3.0", plain))
    }
}
