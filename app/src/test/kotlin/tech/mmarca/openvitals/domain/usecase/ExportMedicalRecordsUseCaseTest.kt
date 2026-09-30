package tech.mmarca.openvitals.domain.usecase

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.FakeMedicalRecordsRepository
import tech.mmarca.openvitals.domain.medical.FhirFileParser
import tech.mmarca.openvitals.domain.medical.FhirImportAnalyzer
import tech.mmarca.openvitals.domain.medical.FhirParseResult
import tech.mmarca.openvitals.domain.medical.MedicalImportPlanner
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecordRef

/** An export holds what the app may read, says what it could not, and re-imports to the same sources and ids. */
class ExportMedicalRecordsUseCaseTest {

    private val repository = FakeMedicalRecordsRepository().apply {
        addSource("clinic", "Clinic", baseUri = "https://clinic.example/fhir")
        addSource("lab", "City Lab", baseUri = "https://lab.example/fhir")
        addSource("portal", "Portal", packageName = "com.example.portal", baseUri = "https://portal.example/fhir")
        // Two sources use the same id: each entry's fullUrl keeps them apart.
        add("clinic", "Immunization", "1", immunization("1", "Tetanus"))
        add("lab", "Immunization", "1", immunization("1", "Influenza"))
        add("clinic", "AllergyIntolerance", "a1", """{"resourceType":"AllergyIntolerance","id":"a1","code":{"text":"Peanut"},"patient":{"reference":"Patient/p1"}}""")
        add("portal", "Condition", "c1", """{"resourceType":"Condition","id":"c1","code":{"text":"Asthma"},"subject":{"reference":"Patient/p1"}}""")
    }

    private val export = ExportMedicalRecordsUseCase(repository)
    private val now = Instant.parse("2026-09-29T10:00:00Z")

    private fun immunization(id: String, vaccine: String) =
        """{"resourceType":"Immunization","id":"$id","status":"completed","vaccineCode":{"text":"$vaccine"},""" +
            """"patient":{"reference":"Patient/p1"},"occurrenceDateTime":"2023-10-02"}"""

    private fun fullUrls(json: String) =
        Json.parseToJsonElement(json).jsonObject.getValue("entry").jsonArray.map { it.jsonObject.getValue("fullUrl").jsonPrimitive.content }

    @Test
    fun `export all holds every readable record, other apps' included, each with its source's fullUrl`() = runTest {
        val result = export(MedicalExportScope.All, "2.12.0", now)

        assertThat(result.recordCount).isEqualTo(4)
        assertThat(fullUrls(result.bundleJson)).containsExactly(
            "https://clinic.example/fhir/Immunization/1",
            "https://lab.example/fhir/Immunization/1",
            "https://clinic.example/fhir/AllergyIntolerance/a1",
            "https://portal.example/fhir/Condition/c1",
        )
        assertThat(result.ownOnly).isEmpty()
        assertThat(result.leftOut).isEmpty()
    }

    @Test
    fun `a declined category holds only this app's records and says so, and no access at all leaves it out`() = runTest {
        repository.readable = MedicalCategory.entries.toSet() - MedicalCategory.CONDITIONS
        val ownOnly = export(MedicalExportScope.All, "2.12.0", now)

        repository.writable = false
        val noAccess = export(MedicalExportScope.All, "2.12.0", now)

        assertThat(ownOnly.ownOnly).containsExactly(MedicalCategory.CONDITIONS)
        assertThat(ownOnly.recordCount).isEqualTo(3)
        assertThat(noAccess.leftOut).containsExactly(MedicalCategory.CONDITIONS)
        assertThat(noAccess.recordCount).isEqualTo(3)
    }

    @Test
    fun `a category or a record exports alone`() = runTest {
        val category = export(MedicalExportScope.Category(MedicalCategory.ALLERGIES), "2.12.0", now)
        val record = export(MedicalExportScope.Record(MedicalRecordRef("lab", "Immunization", "1")), "2.12.0", now)

        assertThat(fullUrls(category.bundleJson)).containsExactly("https://clinic.example/fhir/AllergyIntolerance/a1")
        assertThat(fullUrls(record.bundleJson)).containsExactly("https://lab.example/fhir/Immunization/1")
    }

    @Test
    fun `an export re-imports into an empty phone with the same sources and ids, and again without duplicates`() = runTest {
        val bundle = export(MedicalExportScope.All, "2.12.0", now).bundleJson
        val target = FakeMedicalRecordsRepository()

        importInto(target, bundle)

        assertThat(target.sources.map { it.displayName to it.fhirBaseUri }).containsExactly(
            "Clinic" to "https://clinic.example/fhir",
            "City Lab" to "https://lab.example/fhir",
            "Portal" to "https://portal.example/fhir",
        )
        val bySource = target.records
            .groupBy({ record -> target.sources.first { it.id == record.ref.dataSourceId }.displayName }, { it.ref.resourceId })
            .mapValues { it.value.toSet() }
        assertThat(bySource).containsExactly("Clinic", setOf("1", "a1"), "City Lab", setOf("1"), "Portal", setOf("c1"))

        val again = importInto(target, bundle)
        assertThat(again.updated).isEqualTo(4)
        assertThat(target.sources).hasSize(3)
        assertThat(target.records).hasSize(4)
    }

    @Test
    fun `an origin in R4 and R4B comes back as two sources, and a re-import finds both`() = runTest {
        repository.addSource("clinic-r4b", "Clinic (FHIR 4.3.0)", baseUri = "https://clinic.example/fhir", fhirVersion = "4.3.0")
        repository.add("clinic-r4b", "Immunization", "2", immunization("2", "Measles"))
        val bundle = export(MedicalExportScope.Category(MedicalCategory.VACCINES), "2.12.0", now).bundleJson
        val target = FakeMedicalRecordsRepository()

        val first = importInto(target, bundle)
        val again = importInto(target, bundle)

        assertThat(first.rejected).isEqualTo(0)
        assertThat(target.sources.map { it.displayName to it.fhirVersion }).containsExactly(
            "Clinic" to "4.0.1",
            "Clinic (FHIR 4.3.0)" to "4.3.0",
            "City Lab" to "4.0.1",
        )
        assertThat(again.updated).isEqualTo(3)
        assertThat(target.records).hasSize(3)
    }

    private suspend fun importInto(target: FakeMedicalRecordsRepository, bundle: String) = run {
        val file = (FhirFileParser.parse(bundle) as FhirParseResult.Parsed).file
        val plans = MedicalImportPlanner.plan(FhirImportAnalyzer.analyze(file, "export.json"), target.ownSources(), emptyList())
        ImportMedicalRecordsUseCase(target)(plans, LocalDate.of(2026, 9, 29))
    }
}
