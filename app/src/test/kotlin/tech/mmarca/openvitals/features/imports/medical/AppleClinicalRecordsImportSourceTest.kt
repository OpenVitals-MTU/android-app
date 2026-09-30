package tech.mmarca.openvitals.features.imports.medical

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import tech.mmarca.openvitals.domain.medical.Dstu2ToR4
import tech.mmarca.openvitals.domain.medical.FhirImportAnalyzer
import tech.mmarca.openvitals.domain.medical.MedicalImportSourceResult
import tech.mmarca.openvitals.domain.medical.MedicalSourceSkipReason
import tech.mmarca.openvitals.domain.medical.cda.CcdaFixtures
import tech.mmarca.openvitals.domain.medical.shc.SmartHealthCardFixtures
import tech.mmarca.openvitals.domain.medical.shc.SmartHealthCardImportSource
import tech.mmarca.openvitals.features.imports.medical.AppleExportZips.clinicalRecord
import tech.mmarca.openvitals.features.imports.medical.AppleExportZips.exportXml
import tech.mmarca.openvitals.features.imports.medical.AppleExportZips.immunization
import tech.mmarca.openvitals.features.imports.medical.AppleExportZips.zip

/** Apple's index names each record's provider and FHIR version. Two providers never share a source. */
class AppleClinicalRecordsImportSourceTest {

    private fun read(bytes: ByteArray) = AppleClinicalRecordsImportSource.read { bytes.inputStream() }

    private val hospitalA = "https://fhir.hospital-a.example/r4"
    private val hospitalB = "https://fhir.hospital-b.example/api/FHIR/R4"

    private val export = zip(
        exportXml = exportXml(
            clinicalRecord("A-1", "Hospital A", hospitalA, "4.0.1"),
            clinicalRecord("A-2", "Hospital A", hospitalA, "4.3.0"),
            clinicalRecord("B-1", "Hospital B", hospitalB, "4.0.1"),
            clinicalRecord("OLD-1", "Old Clinic", "https://old.example/dstu2", "1.0.2"),
            clinicalRecord("OLD-2", "Old Clinic", "https://old.example/dstu2", "1.0.2"),
            clinicalRecord("GONE-1", "Hospital B", hospitalB, "4.0.1"),
        ),
        clinical = mapOf(
            // Both hospitals use the id "imm-1": their own sources keep them apart.
            "A-1" to immunization("imm-1", "Tetanus"),
            "A-2" to immunization("imm-2", "Measles"),
            "B-1" to immunization("imm-1", "Influenza"),
            "OLD-1" to """{"resourceType":"Immunization","id":"old","status":"completed","date":"2010-01-01","vaccineCode":{"text":"Polio"},"patient":{"reference":"Patient/p1"},"wasNotGiven":false,"reported":true}""",
            "OLD-2" to """{"resourceType":"DiagnosticOrder","id":"order"}""",
            "STRAY-1" to immunization("stray"),
        ),
        before = setOf("A-1"),
    )

    @Test
    fun `every record comes out with its provider as origin, whatever the zip order, a DSTU2 one as R4`() {
        val result = read(export) as MedicalImportSourceResult.Entries

        val byOrigin = result.file.entries.groupBy { it.origin!!.baseUri to it.origin!!.fhirVersion }
        assertThat(byOrigin.keys).containsExactly(hospitalA to "4.0.1", hospitalA to "4.3.0", hospitalB to "4.0.1", "https://old.example/dstu2" to "4.0.1")
        assertThat(result.file.entries.map { it.origin!!.displayName }.toSet()).containsExactly("Hospital A", "Hospital B", "Old Clinic")
    }

    @Test
    fun `a DSTU2 record is converted to R4 and tagged as converted`() {
        val old = (read(export) as MedicalImportSourceResult.Entries).file.entries.single { it.origin!!.displayName == "Old Clinic" }.resource

        assertThat(old["occurrenceDateTime"].toString()).isEqualTo("\"2010-01-01\"")
        assertThat(old["primarySource"].toString()).isEqualTo("false")
        assertThat(old.keys).containsNoneOf("date", "wasNotGiven", "reported")
        assertThat(Dstu2ToR4.isConverted(old)).isTrue()
    }

    @Test
    fun `a DSTU2 type with no mapping, a missing file and a file with no index entry are left out and named`() {
        val result = read(export) as MedicalImportSourceResult.Entries

        assertThat(result.skipped.map { it.reason to it.detail }).containsExactly(
            MedicalSourceSkipReason.FHIR_DSTU2 to "clinical-records/old-2.json (DiagnosticOrder)",
            MedicalSourceSkipReason.FILE_MISSING to "clinical-records/gone-1.json",
            MedicalSourceSkipReason.NOT_INDEXED to "clinical-records/stray-1.json",
        )
    }

    @Test
    fun `the analyze step makes one source per provider and version, keeping each provider's ids`() {
        val entries = (read(export) as MedicalImportSourceResult.Entries).file

        val groups = FhirImportAnalyzer.analyze(entries, "export.zip").groups

        assertThat(groups.map { it.suggestedName }).containsExactly("Hospital A", "Hospital A (FHIR 4.3.0)", "Hospital B", "Old Clinic")
        assertThat(groups.map { it.fhirVersion }).containsExactly("4.0.1", "4.3.0", "4.0.1", "4.0.1")
        assertThat(groups.flatMap { group -> group.ready.map { it.id } }).containsExactly("imm-1", "imm-2", "imm-1", "old")
        assertThat(groups.all { it.notes.isEmpty() }).isTrue()
    }

    @Test
    fun `the copy to keep is the clinical records alone, packed as one zip`() {
        val document = (read(export) as MedicalImportSourceResult.Entries).documents.single()

        val names = java.util.zip.ZipInputStream(document.bytes.inputStream()).use { zip ->
            generateSequence { zip.nextEntry?.name }.toList()
        }
        assertThat(names).containsExactly(
            "clinical-records/a-1.json", "clinical-records/a-2.json", "clinical-records/b-1.json",
            "clinical-records/old-1.json", "clinical-records/old-2.json", "clinical-records/stray-1.json",
        )
        assertThat(document.mimeType).isEqualTo("application/zip")
        assertThat(document.fileName("export.zip")).isEqualTo("export-clinical-records.zip")
    }

    @Test
    fun `a provider with no URL gets a base from its name`() {
        val bytes = zip(exportXml(clinicalRecord("C-1", "Corner Clinic", url = null, version = "4.0.1")), mapOf("C-1" to immunization("c1")))

        val entry = (read(bytes) as MedicalImportSourceResult.Entries).file.entries.single()

        assertThat(entry.origin!!.baseUri).isEqualTo("openvitals://import/corner-clinic")
    }

    @Test
    fun `a zip with no export, or an export with no clinical records, has nothing to import`() {
        assertThat(read(zip(exportXml = null, clinical = mapOf("A-1" to immunization("a")))))
            .isEqualTo(MedicalImportSourceResult.NotSupported)
        assertThat(read(zip(exportXml(), clinical = emptyMap()))).isEqualTo(MedicalImportSourceResult.Empty)
    }

    @Test
    fun `a picked zip is read as an Apple export, XML as a CDA document, a card as a card, and anything else as a FHIR file`() {
        val picked = PickedFileImportSource(SmartHealthCardImportSource(SmartHealthCardFixtures.FixedRasterizer()))
        val apple = picked.read { export.inputStream() }
        val fhir = picked.read { immunization("x").byteInputStream() }
        val card = picked.read { SmartHealthCardFixtures.numeric(SmartHealthCardFixtures.jws()).byteInputStream() }
        val cda = picked.read { CcdaFixtures.summary.byteInputStream() }

        assertThat((apple as MedicalImportSourceResult.Entries).file.entries).hasSize(4)
        assertThat((fhir as MedicalImportSourceResult.Entries).file.entries.single().origin).isNull()
        assertThat((card as MedicalImportSourceResult.Entries).file.entries.first().origin?.baseUri).isEqualTo(SmartHealthCardFixtures.Issuer)
        assertThat((cda as MedicalImportSourceResult.Entries).file.entries.first().origin?.displayName).isEqualTo("Sample Health Clinic")
    }
}
