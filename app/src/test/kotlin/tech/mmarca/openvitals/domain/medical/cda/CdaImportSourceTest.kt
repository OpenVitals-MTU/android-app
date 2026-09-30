package tech.mmarca.openvitals.domain.medical.cda

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Test
import tech.mmarca.openvitals.domain.medical.FhirImportAnalyzer
import tech.mmarca.openvitals.domain.medical.MedicalImportSourceResult
import tech.mmarca.openvitals.domain.medical.MedicalSourceSkipReason
import tech.mmarca.openvitals.domain.medical.fhirId
import tech.mmarca.openvitals.domain.medical.string

/** The portal zip: records grouped by clinic, each PDF offered with its own records, and what is left out. */
class CdaImportSourceTest {

    private fun read(zip: ByteArray) = CdaImportSource.read { ByteArrayInputStream(zip) }

    private fun entries(zip: ByteArray) = read(zip) as MedicalImportSourceResult.Entries

    @Test
    fun `records are grouped by clinic, one source each, with one Patient per clinic`() {
        val result = entries(EstonianCdaFixtures.zip(EstonianCdaFixtures.all))
        val analysis = FhirImportAnalyzer.analyze(result.file, "export.zip")

        assertThat(analysis.groups.map { it.suggestedName }).containsExactly("Näidiskliinik OÜ", "Näidishambaravi OÜ")
        assertThat(analysis.groups.map { it.baseUri }).containsExactly("openvitals://ee-tis/90000001", "openvitals://ee-tis/90000002")
        assertThat(analysis.groups.all { it.versionStated && it.fhirVersion == "4.0.1" }).isTrue()
        assertThat(analysis.groups.flatMap { it.rejected }).isEmpty()
        assertThat(analysis.groups.flatMap { it.notes }).isEmpty()
        assertThat(analysis.groups.map { group -> group.ready.count { it.type == "Patient" } }).containsExactly(1, 1)
    }

    @Test
    fun `declarations are listed as skipped, with their title and file`() {
        val result = entries(EstonianCdaFixtures.zip(EstonianCdaFixtures.all))

        assertThat(result.skipped.map { it.reason }).containsExactly(MedicalSourceSkipReason.NOT_A_RECORD)
        assertThat(result.skipped.single().detail).isEqualTo("Tahteavaldus: tahteavaldus.xml")
    }

    @Test
    fun `each PDF is offered with its own document's records, never the shared ones`() {
        val result = entries(EstonianCdaFixtures.zip(EstonianCdaFixtures.all))
        val dental = result.documents.single { it.name == "hambaravi.pdf" }

        assertThat(result.documents.map { it.name }).containsExactly(
            "epikriis.pdf", "immuniseerimine.pdf", "hambaravi.pdf", "saatekirja-vastus.pdf", "nakkushaigus.pdf", "saatekiri.pdf",
            "tahteavaldus.pdf",
        )
        assertThat(dental.mimeType).isEqualTo("application/pdf")
        assertThat(dental.links!!.map { it.first }).containsExactly("Encounter", "Condition", "Procedure")
        assertThat(result.documents.flatMap { it.links!! }.map { it.first }).containsNoneOf("Patient", "Practitioner", "Organization")
    }

    @Test
    fun `a declaration's PDF is offered on its own, with no record and its clinic`() {
        val declaration = entries(EstonianCdaFixtures.zip(EstonianCdaFixtures.all)).documents.single { it.name == "tahteavaldus.pdf" }

        assertThat(declaration.keepWithoutRecords).isTrue()
        assertThat(declaration.links).isEmpty()
        assertThat(declaration.sourceName).isEqualTo("Näidiskliinik OÜ")
    }

    @Test
    fun `a zip without PDFs still imports, with nothing to keep`() {
        val result = entries(EstonianCdaFixtures.zip(EstonianCdaFixtures.all, withPdfs = false))

        assertThat(result.file.entries).isNotEmpty()
        assertThat(result.documents).isEmpty()
    }

    @Test
    fun `the newer version of a document replaces the older one`() {
        val zip = EstonianCdaFixtures.zip(
            mapOf(
                "v2" to EstonianCdaFixtures.referralResponse(version = 2, finding = "Parandatud leid."),
                "v1" to EstonianCdaFixtures.referralResponse(version = 1, finding = "Esialgne leid."),
            ),
        )
        val procedures = entries(zip).file.entries.filter { it.type == "Procedure" }

        assertThat(procedures).hasSize(1)
        assertThat(procedures.single().resource.toString()).contains("Parandatud leid.")
        assertThat(procedures.single().resource.fhirId).startsWith("ee-")
    }

    @Test
    fun `one XML file is one document, kept as the original, under its custodian`() {
        val result = CdaImportSource.read { CcdaFixtures.summary.byteInputStream() } as MedicalImportSourceResult.Entries
        val analysis = FhirImportAnalyzer.analyze(result.file, "summary.xml")

        assertThat(analysis.groups.single().suggestedName).isEqualTo("Sample Health Clinic")
        assertThat(analysis.groups.single().rejected).isEmpty()
        assertThat(result.documents.single().mimeType).isEqualTo("application/xml")
        assertThat(result.documents.single().links).isNull()
    }

    @Test
    fun `a standard document in a zip is read beside Estonian ones`() {
        val result = entries(EstonianCdaFixtures.zip(mapOf("summary" to CcdaFixtures.summary, "epikriis" to EstonianCdaFixtures.epicrisis)))

        assertThat(result.file.entries.mapNotNull { it.origin?.baseUri }.toSet())
            .containsExactly("openvitals://cda/2.16.840.1.113883.4.6/9999999999", "openvitals://ee-tis/90000001")
    }

    @Test
    fun `XML that is not a CDA document, and a file that is not XML, are not this source's`() {
        assertThat(CdaImportSource.read { "<note>hello</note>".byteInputStream() }).isEqualTo(MedicalImportSourceResult.NotSupported)
        assertThat(CdaImportSource.read { """{"resourceType":"Patient"}""".byteInputStream() }).isEqualTo(MedicalImportSourceResult.NotSupported)
    }

    @Test
    fun `a zip with no Estonian document is not this source's`() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("notes/readme.xml"))
            zip.write("""<note>hello</note>""".toByteArray())
            zip.closeEntry()
        }

        assertThat(read(output.toByteArray())).isEqualTo(MedicalImportSourceResult.NotSupported)
    }

    @Test
    fun `a broken document is listed as unreadable and the rest still import`() {
        val result = entries(EstonianCdaFixtures.zip(mapOf("broken" to "<ClinicalDocument", "epikriis" to EstonianCdaFixtures.epicrisis)))

        assertThat(result.skipped.single().reason).isEqualTo(MedicalSourceSkipReason.UNREADABLE_DOCUMENT)
        assertThat(result.file.entries.map { it.resource.string("resourceType") }).contains("Condition")
    }

    @Test
    fun `an export of declarations only has nothing to write but says why`() {
        val result = entries(EstonianCdaFixtures.zip(mapOf("tahteavaldus" to EstonianCdaFixtures.declaration)))

        assertThat(result.file.entries).isEmpty()
        assertThat(result.skipped.single().reason).isEqualTo(MedicalSourceSkipReason.NOT_A_RECORD)
    }
}
