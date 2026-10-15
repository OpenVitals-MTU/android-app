package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import tech.mmarca.openvitals.domain.medical.FhirTestFiles.entry

/** Two origins never share a data source, or their ids could collide. */
class FhirSourceGrouperTest {

    @Test
    fun `absolute fullUrls group by their base`() {
        val groups = FhirSourceGrouper.group(FhirTestFiles.parsed("spike-sample-bundle.json"), "sample.json")

        assertThat(groups).hasSize(1)
        assertThat(groups.single().baseUri).isEqualTo("https://clinic.example/fhir")
        assertThat(groups.single().suggestedName).isEqualTo("clinic.example")
        assertThat(groups.single().entries).hasSize(13)
    }

    @Test
    fun `two bases are two groups even when ids repeat`() {
        val file = FhirFile(
            listOf(
                entry("""{"resourceType":"Immunization","id":"1"}""", "https://a.example/fhir/Immunization/1"),
                entry("""{"resourceType":"Immunization","id":"1"}""", "https://b.example/r4/Immunization/1"),
            ),
        )

        val groups = FhirSourceGrouper.group(file, "x.json")

        assertThat(groups.map { it.baseUri }).containsExactly("https://a.example/fhir", "https://b.example/r4")
    }

    @Test
    fun `meta source groups when there is no absolute fullUrl`() {
        val file = FhirFile(
            listOf(
                entry("""{"resourceType":"Condition","id":"1","meta":{"source":"https://portal.example/"}}"""),
                entry("""{"resourceType":"Condition","id":"2","meta":{"source":"https://portal.example"}}"""),
            ),
        )

        val group = FhirSourceGrouper.group(file, "x.json").single()

        assertThat(group.baseUri).isEqualTo("https://portal.example")
        assertThat(group.entries).hasSize(2)
    }

    @Test
    fun `records with no origin form one group the user names, prefilled from the file name`() {
        val group = FhirSourceGrouper.group(FhirTestFiles.parsed("ips-document-bundle.json"), "Downloads/summary-2024.json").single()

        assertThat(group.baseUri).isNull()
        assertThat(group.suggestedName).isEqualTo("summary-2024")
    }

    @Test
    fun `an index origin wins, one group per provider`() {
        val a = FhirOrigin("https://hospital-a.example/fhir", "Hospital A", "4.0.1")
        val b = FhirOrigin("https://hospital-b.example/fhir", "Hospital B", "4.0.1")
        val file = FhirFile(
            listOf(
                entry("""{"resourceType":"Immunization","id":"1"}""", origin = a),
                entry("""{"resourceType":"Immunization","id":"1"}""", origin = b),
            ),
        )

        val groups = FhirSourceGrouper.group(file, "export.zip")

        assertThat(groups.map { it.suggestedName }).containsExactly("Hospital A", "Hospital B")
        assertThat(groups.map { it.fhirVersion }).containsExactly("4.0.1", "4.0.1")
    }

    @Test
    fun `one provider in two FHIR versions is two groups, the second named with its version`() {
        val r4 = FhirOrigin("https://hospital.example/fhir", "Hospital", "4.0.1")
        val r4b = r4.copy(fhirVersion = "4.3.0")
        val file = FhirFile(
            listOf(
                entry("""{"resourceType":"Condition","id":"1"}""", origin = r4),
                entry("""{"resourceType":"Condition","id":"2"}""", origin = r4b),
            ),
        )

        val groups = FhirSourceGrouper.group(file, "export.zip")

        assertThat(groups.map { it.suggestedName }).containsExactly("Hospital", "Hospital (FHIR 4.3.0)").inOrder()
    }

    @Test
    fun `a fullUrl base is what comes before type and id`() {
        assertThat(FhirSourceGrouper.baseOf("https://x.example/fhir/Patient/p1", "Patient")).isEqualTo("https://x.example/fhir")
        assertThat(FhirSourceGrouper.baseOf("https://x.example/fhir/Patient/p1/_history/3", "Patient"))
            .isEqualTo("https://x.example/fhir")
        assertThat(FhirSourceGrouper.baseOf("openvitals://import/clinic/Condition/c1", "Condition"))
            .isEqualTo("openvitals://import/clinic")
        assertThat(FhirSourceGrouper.baseOf("urn:uuid:1b3f0a2e-0000-4000-8000-000000000001", "Patient")).isNull()
        assertThat(FhirSourceGrouper.baseOf("https://x.example/fhir/Patient/p1", "Condition")).isNull()
    }

    @Test
    fun `a named source's base URI is stable, plain and short`() {
        assertThat(FhirSourceGrouper.importBaseUri("Hospital Universitario de León"))
            .isEqualTo("openvitals://import/hospital-universitario-de-leon")
        assertThat(FhirSourceGrouper.importBaseUri("  ")).isEqualTo("openvitals://import/records")
        assertThat(FhirSourceGrouper.importBaseUri("x".repeat(100)).length)
            .isAtMost("openvitals://import/".length + 48)
    }
}
