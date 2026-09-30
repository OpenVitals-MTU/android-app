package tech.mmarca.openvitals.data.repository

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.healthconnect.FakeMedicalRecordsClient
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.MedicalCategoryMapping
import tech.mmarca.openvitals.healthconnect.MedicalRecordsTestSupport
import tech.mmarca.openvitals.healthconnect.MedicalRecordsWriter

class MedicalRecordsRepositoryImplTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = MedicalRecordsTestSupport.dispatchers(dispatcher)
    private val client = FakeMedicalRecordsClient()
    private val hc = mockk<HealthConnectManager>()
    private val repository = MedicalRecordsRepositoryImpl(hc)

    @Before
    fun setUp() {
        MedicalRecordsTestSupport.mockLog()
        every { hc.medicalRecordsReader } returns MedicalRecordsTestSupport.reader(client, dispatchers)
        every { hc.medicalRecordsWriter } returns
            MedicalRecordsWriter(client, requireSyncEnabled = {}, isAvailable = { true }, dispatchers = dispatchers)
        every { hc.medicalRecordsPermissions } returns MedicalCategoryMapping.allPermissions
    }

    @After
    fun tearDown() = MedicalRecordsTestSupport.unmockLog()

    @Test
    fun `a whole category is read across pages`() = runTest(dispatcher) {
        val source = repository.createSource("https://clinic.example/fhir", "Clinic", "4.0.1")
        (1..2_500).chunked(500).forEach { ids ->
            repository.upsert(source.id, "4.0.1", ids.map { MedicalRecordsTestSupport.immunization("imm-$it") })
        }
        client.calls.clear()

        val records = repository.readCategory(MedicalCategory.VACCINES)

        assertThat(records.map { it.ref.resourceId }.toSet()).hasSize(2_500)
        assertThat(client.calls.count { it == "readPage" }).isEqualTo(3)
    }

    @Test
    fun `readable categories and write access come from the grants, not from permission strings`() = runTest(dispatcher) {
        coEvery { hc.grantedPermissions() } returns setOf(
            MedicalCategoryMapping.readPermission(MedicalCategory.VACCINES),
            MedicalCategoryMapping.readPermission(MedicalCategory.PREGNANCY),
        )

        assertThat(repository.readableCategories()).containsExactly(MedicalCategory.VACCINES, MedicalCategory.PREGNANCY)
        assertThat(repository.canWrite()).isFalse()
    }

    @Test
    fun `granted permissions are the medical ones only`() = runTest(dispatcher) {
        val vaccines = MedicalCategoryMapping.readPermission(MedicalCategory.VACCINES)
        coEvery { hc.grantedPermissions() } returns setOf(
            vaccines,
            MedicalCategoryMapping.WRITE_PERMISSION,
            "android.permission.health.READ_STEPS",
        )

        assertThat(repository.grantedPermissions()).containsExactly(vaccines, MedicalCategoryMapping.WRITE_PERMISSION)
    }
}
