package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** A zip of FHIR files, as CommonHealth exports: every file read, whatever the folders, and the rest named. */
class FhirZipImportSourceTest {

    private fun read(files: Map<String, String>) = FhirZipImportSource.read { zipOf(files.mapValues { it.value.toByteArray() }).inputStream() }

    private fun immunization(id: String) =
        """{"resourceType":"Immunization","id":"$id","status":"completed","vaccineCode":{"text":"Influenza"},"patient":{"reference":"Patient/p1"},"occurrenceDateTime":"2023-10-02"}"""

    private val observation = """{"resourceType":"Observation","id":"o1","status":"final","code":{"text":"Glucose"}}"""

    @Test
    fun `every FHIR file is read, and a file that is not FHIR is left out and named with what it holds`() {
        val result = read(
            mapOf(
                "export/Patient.json" to """{"resourceType":"Patient","id":"p1","name":[{"family":"Doe"}]}""",
                "export/labs/observations.ndjson" to "$observation\n${observation.replace("o1", "o2")}\n",
                "export/vaccines.json" to """{"resourceType":"Bundle","type":"collection","entry":[{"resource":${immunization("i1")}}]}""",
                "export/Observation-list.json" to "[$observation]",
                "export/summary.pdf" to "%PDF-1.7",
                "__MACOSX/export/._Patient.json" to "\u0000\u0005",
                "export/.DS_Store" to "\u0000",
            ),
        ) as MedicalImportSourceResult.Entries

        assertThat(result.file.entries.map { it.type }).containsExactly("Patient", "Observation", "Observation", "Immunization")
        assertThat(result.skipped).containsExactly(
            MedicalSourceSkip(MedicalSourceSkipReason.UNREADABLE, "export/Observation-list.json: a JSON array of 1: 1 record: Observation 1"),
            MedicalSourceSkip(MedicalSourceSkipReason.UNREADABLE, "export/summary.pdf"),
        )
        assertThat(result.documents.single().fileName("commonhealth.zip")).isEqualTo("commonhealth-fhir.zip")
    }

    @Test
    fun `a DSTU2 Bundle converts its own records, not those of the other files`() {
        val dstu2 = """{"resourceType":"Bundle","meta":{"profile":["http://hl7.org/fhir/DSTU2/StructureDefinition/Bundle"]},""" +
            """"entry":[{"resource":{"resourceType":"Immunization","id":"old","status":"completed","date":"2010-01-01","vaccineCode":{"text":"Polio"},"patient":{"reference":"Patient/p1"},"wasNotGiven":false}}]}"""

        val result = read(mapOf("old.json" to dstu2, "new.json" to immunization("new"))) as MedicalImportSourceResult.Entries

        val (old, new) = result.file.entries.map { it.resource }
        assertThat(Dstu2ToR4.isConverted(old)).isTrue()
        assertThat(Dstu2ToR4.isConverted(new)).isFalse()
        assertThat(result.file.bundleProfiles).isEmpty()
    }

    @Test
    fun `a zip with no FHIR file in it is not this source's`() {
        assertThat(read(mapOf("notes.txt" to "hello", "scan.pdf" to "%PDF-1.7"))).isEqualTo(MedicalImportSourceResult.NotSupported)
    }
}
