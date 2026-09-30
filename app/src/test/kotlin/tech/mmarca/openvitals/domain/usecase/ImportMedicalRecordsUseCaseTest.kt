package tech.mmarca.openvitals.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.MedicalRecordsRepositoryImpl
import tech.mmarca.openvitals.domain.medical.FhirFile
import tech.mmarca.openvitals.domain.medical.FhirImportAnalyzer
import tech.mmarca.openvitals.domain.medical.FhirTestFiles
import tech.mmarca.openvitals.domain.medical.MedicalImportGroupPlan
import tech.mmarca.openvitals.domain.medical.MedicalImportPlanner
import tech.mmarca.openvitals.domain.medical.MedicalImportProgress
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.healthconnect.FakeMedicalRecordsClient
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.MedicalCategoryMapping
import tech.mmarca.openvitals.healthconnect.MedicalRecordsTestSupport
import tech.mmarca.openvitals.healthconnect.MedicalRecordsWriter

/** Runs against the real repository over the fake client, which enforces the documented write checks. */
class ImportMedicalRecordsUseCaseTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = MedicalRecordsTestSupport.dispatchers(dispatcher)
    private val client = FakeMedicalRecordsClient()
    private val hc = mockk<HealthConnectManager>()
    private val repository = MedicalRecordsRepositoryImpl(hc)
    private val useCase = ImportMedicalRecordsUseCase(repository)
    private val today = LocalDate.of(2026, 9, 29)

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

    private suspend fun plans(file: FhirFile = FhirTestFiles.parsed("spike-sample-bundle.json")): List<MedicalImportGroupPlan> =
        MedicalImportPlanner.plan(FhirImportAnalyzer.analyze(file, "sample.json"), repository.ownSources(), emptyList())

    @Test
    fun `a first import writes everything into a new source`() = runTest(dispatcher) {
        val progress = mutableListOf<MedicalImportProgress>()

        val result = useCase(plans(), today) { progress += it }

        assertThat(result.written).isEqualTo(12)
        assertThat(result.updated).isEqualTo(0)
        assertThat(result.skipped).isEqualTo(1)
        assertThat(result.stoppedBy).isNull()
        assertThat(repository.ownSources().map { it.fhirBaseUri }).containsExactly("https://clinic.example/fhir")
        assertThat(progress.last()).isEqualTo(MedicalImportProgress(12, 12))
    }

    @Test
    fun `importing the same file again updates the same records and makes no new source`() = runTest(dispatcher) {
        useCase(plans(), today)

        val again = useCase(plans(), today)

        assertThat(again.written).isEqualTo(0)
        assertThat(again.updated).isEqualTo(12)
        assertThat(repository.ownSources()).hasSize(1)
        assertThat(repository.count(MedicalCategory.VACCINES)).isEqualTo(2)
    }

    @Test
    fun `one refused record fails its batch, which is retried one by one`() = runTest(dispatcher) {
        // Pre-flight lets a LOINC-coded Observation through. This fake, like a real phone for an unknown code, refuses it.
        val file = FhirFile(
            listOf(
                FhirTestFiles.entry("""{"resourceType":"Condition","id":"c1","code":{"text":"Asthma"}}"""),
                FhirTestFiles.entry(
                    """{"resourceType":"Observation","id":"o1","status":"final",""" +
                        """"code":{"coding":[{"system":"http://loinc.org","code":"0000-0"}]}}""",
                ),
                FhirTestFiles.entry("""{"resourceType":"Condition","id":"c2","code":{"text":"Hay fever"}}"""),
            ),
        )

        val result = useCase(plans(file), today)

        assertThat(result.written).isEqualTo(2)
        val refused = result.groups.single().rejected.single()
        assertThat(refused.type to refused.id).isEqualTo("Observation" to "o1")
        assertThat(refused.reason).contains("Cannot classify")
        assertThat(refused.json).contains("0000-0")
    }

    @Test
    fun `a group the user left out is not written`() = runTest(dispatcher) {
        val result = useCase(plans().map { it.copy(include = false) }, today)

        assertThat(result.written).isEqualTo(0)
        assertThat(result.excluded).hasSize(1)
        assertThat(repository.ownSources()).isEmpty()
    }

    @Test
    fun `a new source whose name is taken gets the date`() = runTest(dispatcher) {
        repository.createSource("https://other.example/fhir", "clinic.example", "4.0.1")

        useCase(plans(), today)

        assertThat(repository.ownSources().map { it.displayName }).contains("clinic.example (2026-09-29)")
    }

    @Test
    fun `a lost permission stops the import and says why`() = runTest(dispatcher) {
        val planned = plans()
        client.failWrites = SecurityException("Caller doesn't have android.permission.health.WRITE_MEDICAL_DATA")

        val result = useCase(planned, today)

        assertThat(result.stoppedBy).contains("WRITE_MEDICAL_DATA")
        assertThat(result.written).isEqualTo(0)
    }
}
