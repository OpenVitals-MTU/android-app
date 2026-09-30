package tech.mmarca.openvitals.healthconnect

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.healthconnect.MedicalRecordsTestSupport.condition
import tech.mmarca.openvitals.healthconnect.MedicalRecordsTestSupport.immunization

/** Reads must throw rather than return an empty list: "no records" would be a lie. */
class MedicalRecordsHealthReaderTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = MedicalRecordsTestSupport.dispatchers(dispatcher)
    private val client = FakeMedicalRecordsClient()
    private var available = true
    private val reader = MedicalRecordsTestSupport.reader(client, dispatchers) { available }

    @Before
    fun setUp() = MedicalRecordsTestSupport.mockLog()

    @After
    fun tearDown() = MedicalRecordsTestSupport.unmockLog()

    @Test
    fun `a category reads page by page and says how many remain`() = runTest(dispatcher) {
        val source = client.createDataSource("https://clinic.example/fhir", "Clinic", "4.0.1")
        client.upsert(source.id, "4.0.1", (1..5).map { immunization("imm-$it") } + condition("cond-1"))

        val first = reader.readPage(MedicalCategory.VACCINES, pageSize = 2)
        val second = reader.readPage(MedicalCategory.VACCINES, pageSize = 2, pageToken = first.nextPageToken)
        val last = reader.readPage(MedicalCategory.VACCINES, pageSize = 2, pageToken = second.nextPageToken)

        assertThat(first.records.map { it.ref.resourceId }).containsExactly("imm-1", "imm-2").inOrder()
        assertThat(first.remainingCount).isEqualTo(3)
        assertThat(second.remainingCount).isEqualTo(1)
        assertThat(last.records.map { it.ref.resourceId }).containsExactly("imm-5")
        assertThat(last.nextPageToken).isNull()
    }

    @Test
    fun `a count reads one record and adds the rest`() = runTest(dispatcher) {
        val source = client.createDataSource("https://clinic.example/fhir", "Clinic", "4.0.1")
        client.upsert(source.id, "4.0.1", (1..5).map { immunization("imm-$it") } + condition("cond-1"))

        assertThat(reader.count(MedicalCategory.VACCINES)).isEqualTo(5)
        assertThat(reader.count(MedicalCategory.CONDITIONS)).isEqualTo(1)
        assertThat(reader.count(MedicalCategory.ALLERGIES)).isEqualTo(0)
    }

    @Test
    fun `records come back by id`() = runTest(dispatcher) {
        val source = client.createDataSource("https://clinic.example/fhir", "Clinic", "4.0.1")
        client.upsert(source.id, "4.0.1", listOf(immunization("imm-1"), immunization("imm-2")))
        val ref = MedicalRecordRef(source.id, "Immunization", "imm-2")

        assertThat(reader.readByIds(listOf(ref)).map { it.ref }).containsExactly(ref)
        assertThat(reader.readByIds(emptyList())).isEmpty()
    }

    @Test
    fun `own sources are this app's only, and an empty package list means every app`() = runTest(dispatcher) {
        val own = client.createDataSource("https://clinic.example/fhir", "Clinic", "4.0.1")
        client.addForeignSource("com.example.portal", "https://portal.example", "Portal")

        assertThat(reader.ownDataSources().map { it.id }).containsExactly(own.id)
        assertThat(reader.dataSources(emptyList()).map { it.displayName }).containsExactly("Clinic", "Portal")
    }

    @Test
    fun `a source filter narrows the read`() = runTest(dispatcher) {
        val clinic = client.createDataSource("https://clinic.example/fhir", "Clinic", "4.0.1")
        val lab = client.createDataSource("https://lab.example/fhir", "Lab", "4.0.1")
        client.upsert(clinic.id, "4.0.1", listOf(immunization("same-id")))
        client.upsert(lab.id, "4.0.1", listOf(immunization("same-id")))

        val page = reader.readPage(MedicalCategory.VACCINES, pageSize = 10, sourceIds = setOf(lab.id))

        assertThat(page.records.map { it.ref.dataSourceId }).containsExactly(lab.id)
        assertThat(reader.count(MedicalCategory.VACCINES)).isEqualTo(2)
    }

    @Test
    fun `an unavailable feature throws before Health Connect is called`() = runTest(dispatcher) {
        available = false

        val failure = runCatching { reader.count(MedicalCategory.VACCINES) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(client.calls).isEmpty()
    }

    @Test
    fun `a missing read permission reaches the caller instead of an empty list`() = runTest(dispatcher) {
        client.deniedCategories = setOf(MedicalCategory.VACCINES)

        val failure = runCatching { reader.readPage(MedicalCategory.VACCINES, pageSize = 10) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(SecurityException::class.java)
    }
}
