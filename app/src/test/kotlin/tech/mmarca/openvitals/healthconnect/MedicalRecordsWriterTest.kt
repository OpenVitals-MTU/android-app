package tech.mmarca.openvitals.healthconnect

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.healthconnect.MedicalRecordsTestSupport.condition
import tech.mmarca.openvitals.healthconnect.MedicalRecordsTestSupport.immunization

/** FHIR records have no clientRecordId: source, type and id identify them, and a rewrite updates. */
class MedicalRecordsWriterTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = MedicalRecordsTestSupport.dispatchers(dispatcher)
    private val client = FakeMedicalRecordsClient()
    private var syncEnabled = true
    private var available = true
    private val reader = MedicalRecordsTestSupport.reader(client, dispatchers)
    private val writer = MedicalRecordsWriter(
        client = client,
        requireSyncEnabled = { if (!syncEnabled) throw HealthConnectSyncDisabledException() },
        isAvailable = { available },
        dispatchers = dispatchers,
    )

    @Before
    fun setUp() = MedicalRecordsTestSupport.mockLog()

    @After
    fun tearDown() = MedicalRecordsTestSupport.unmockLog()

    @Test
    fun `writing the same record again updates it`() = runTest(dispatcher) {
        val source = writer.createDataSource("https://clinic.example/fhir", "Clinic", "4.0.1")
        writer.upsert(source.id, "4.0.1", listOf(immunization("imm-1", vaccine = "Tetanus")))

        writer.upsert(source.id, "4.0.1", listOf(immunization("imm-1", vaccine = "Tetanus booster")))

        val records = reader.readPage(MedicalCategory.VACCINES, pageSize = 10).records
        assertThat(records).hasSize(1)
        assertThat(records.single().json).contains("Tetanus booster")
    }

    @Test
    fun `one bad record fails its whole batch`() = runTest(dispatcher) {
        val source = writer.createDataSource("https://clinic.example/fhir", "Clinic", "4.0.1")
        val contained = """{"resourceType":"Condition","id":"c2","contained":[{"resourceType":"Patient","id":"p"}]}"""

        val failure = runCatching {
            writer.upsert(source.id, "4.0.1", listOf(condition("c1"), contained))
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(reader.count(MedicalCategory.CONDITIONS)).isEqualTo(0)
    }

    @Test
    fun `deleting records removes those named and nothing else`() = runTest(dispatcher) {
        val source = writer.createDataSource("https://clinic.example/fhir", "Clinic", "4.0.1")
        val written = writer.upsert(source.id, "4.0.1", listOf(immunization("imm-1"), immunization("imm-2")))

        writer.delete(listOf(written.first().ref))

        assertThat(reader.readPage(MedicalCategory.VACCINES, pageSize = 10).records.map { it.ref.resourceId })
            .containsExactly("imm-2")
    }

    @Test
    fun `deleting a source takes its records with it`() = runTest(dispatcher) {
        val clinic = writer.createDataSource("https://clinic.example/fhir", "Clinic", "4.0.1")
        val lab = writer.createDataSource("https://lab.example/fhir", "Lab", "4.0.1")
        writer.upsert(clinic.id, "4.0.1", listOf(immunization("imm-1")))
        writer.upsert(lab.id, "4.0.1", listOf(immunization("imm-2")))

        writer.deleteDataSource(clinic.id)

        assertThat(reader.ownDataSources().map { it.id }).containsExactly(lab.id)
        assertThat(reader.readPage(MedicalCategory.VACCINES, pageSize = 10).records.map { it.ref.dataSourceId })
            .containsExactly(lab.id)
    }

    @Test
    fun `nothing is written while Health Connect sync is paused`() = runTest(dispatcher) {
        val source = writer.createDataSource("https://clinic.example/fhir", "Clinic", "4.0.1")
        client.calls.clear()
        syncEnabled = false

        val failure = runCatching { writer.upsert(source.id, "4.0.1", listOf(immunization("imm-1"))) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(HealthConnectSyncDisabledException::class.java)
        assertThat(client.calls).isEmpty()
    }

    @Test
    fun `an unavailable feature throws before Health Connect is called`() = runTest(dispatcher) {
        available = false

        val failure = runCatching { writer.createDataSource("https://clinic.example/fhir", "Clinic", "4.0.1") }
            .exceptionOrNull()

        assertThat(failure).isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(client.calls).isEmpty()
    }

    @Test
    fun `an empty batch makes no call`() = runTest(dispatcher) {
        val source = writer.createDataSource("https://clinic.example/fhir", "Clinic", "4.0.1")
        client.calls.clear()

        assertThat(writer.upsert(source.id, "4.0.1", emptyList())).isEmpty()
        writer.delete(emptyList())
        assertThat(client.calls).isEmpty()
    }
}
