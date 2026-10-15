package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FhirFileParserTest {

    @Test
    fun `a single resource is one entry`() {
        val result = FhirFileParser.parse("""{"resourceType":"Condition","id":"c1"}""")

        val file = (result as FhirParseResult.Parsed).file
        assertThat(file.entries.map { it.type }).containsExactly("Condition")
        assertThat(file.entries.single().fullUrl).isNull()
    }

    @Test
    fun `a bundle of any type is unwrapped with each entry's fullUrl`() {
        val file = FhirTestFiles.parsed("ips-document-bundle.json")

        assertThat(file.entries).hasSize(8)
        assertThat(file.entries.first().type).isEqualTo("Composition")
        assertThat(file.entries.first().fullUrl).isEqualTo("urn:uuid:1b3f0a2e-0000-4000-8000-000000000001")
        assertThat(file.isOpenVitalsExport).isFalse()
    }

    @Test
    fun `NDJSON reads one resource per line and skips blank lines`() {
        val text = """{"resourceType":"Patient","id":"a"}""" + "\n\n" + """{"resourceType":"Condition","id":"b"}""" + "\n"

        val file = (FhirFileParser.parse(text) as FhirParseResult.Parsed).file

        assertThat(file.entries.map { it.type }).containsExactly("Patient", "Condition").inOrder()
    }

    @Test
    fun `an OpenVitals export's markers are read back`() {
        val text = """
            {"resourceType":"Bundle","type":"collection",
             "meta":{"tag":[{"system":"${OpenVitalsFhir.TagSystem}","code":"${OpenVitalsFhir.ExportTag}"}]},
             "entry":[{"fullUrl":"https://lab.example/fhir/Observation/o1",
               "extension":[{"url":"${OpenVitalsFhir.SourceExtensionUrl}","extension":[
                 {"url":"displayName","valueString":"City Lab"},{"url":"packageName","valueString":"com.example.lab"},
                 {"url":"fhirVersion","valueString":"4.3.0"}]}],
               "resource":{"resourceType":"Observation","id":"o1"}}]}
        """.trimIndent()

        val file = (FhirFileParser.parse(text) as FhirParseResult.Parsed).file

        assertThat(file.isOpenVitalsExport).isTrue()
        assertThat(file.entries.single().exportedSourceName).isEqualTo("City Lab")
        assertThat(file.entries.single().exportedSourcePackage).isEqualTo("com.example.lab")
        assertThat(file.entries.single().exportedFhirVersion).isEqualTo("4.3.0")
    }

    @Test
    fun `anything that is not FHIR JSON is refused`() {
        listOf(
            "date,steps\n2024-01-01,100",
            """[{"resourceType":"Patient"}]""",
            """{"name":"no resource type"}""",
            """{"resourceType":"Patient"}""" + "\nnot json",
            "",
        ).forEach { text ->
            assertThat(FhirFileParser.parse(text)).isEqualTo(FhirParseResult.NotFhir)
        }
    }
}
