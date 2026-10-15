package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import tech.mmarca.openvitals.domain.medical.FhirTestFiles.entry
import tech.mmarca.openvitals.domain.medical.FhirTestFiles.references

/** Health Connect needs an id on every record. Repairs must keep every reference pointing at the right one. */
class FhirIdAssignerTest {

    private val scope = FhirSourceGrouper.ImportScheme

    private fun ips(): FhirIdResult {
        val supported = FhirTestFiles.parsed("ips-document-bundle.json").entries.filter { it.type in FhirResourceTypes.supported }
        return FhirIdAssigner.assign(supported, scope)
    }

    @Test
    fun `an entry with a urn uuid fullUrl and no id takes the uuid`() {
        val patient = ips().resources.single { it.type == "Patient" }

        assertThat(patient.id).isEqualTo("1b3f0a2e-0000-4000-8000-000000000002")
        assertThat(patient.json.fhirId).isEqualTo(patient.id)
    }

    @Test
    fun `every urn uuid reference is rewritten to type and id`() {
        val result = ips()
        val targets = result.resources.map { "${it.type}/${it.id}" }.toSet()

        val refs = result.resources.flatMap { references(it.json) }.toSet()

        assertThat(refs.filter { it.startsWith("urn:uuid:") || it.startsWith("#") }).isEmpty()
        assertThat(targets).containsAtLeastElementsIn(refs)
    }

    @Test
    fun `an invalid id is replaced by a hash, and references to it follow`() {
        val result = ips()
        val immunization = result.resources.single { it.type == "Immunization" }

        assertThat(immunization.id).matches("ov-[0-9a-f]{40}")
        assertThat(result.notes).contains(FhirIdNote.IdAssigned("Immunization", "not a valid id", immunization.id))
    }

    @Test
    fun `a contained resource of a supported type is lifted and its reference follows`() {
        val result = ips()
        val statement = result.resources.single { it.type == "MedicationStatement" }
        val medication = result.resources.single { it.type == "Medication" }

        assertThat(medication.id).isEqualTo("${statement.id}-med1")
        assertThat(statement.json).doesNotContainKey("contained")
        assertThat(references(statement.json)).contains("Medication/${medication.id}")
    }

    @Test
    fun `a contained resource of a refused type is dropped, and its reference keeps a display`() {
        val result = ips()
        val medication = result.resources.single { it.type == "Medication" }
        val itemReference = medication.json.objects("ingredient").single().obj("itemReference")!!

        assertThat(itemReference.string("reference")).isNull()
        assertThat(itemReference.string("display")).isEqualTo("Metformin hydrochloride")
        assertThat(result.notes.filterIsInstance<FhirIdNote.ContainedDropped>().single().type).isEqualTo("Substance")
    }

    @Test
    fun `the same file always yields the same ids`() {
        assertThat(ips().resources.map { it.id }).isEqualTo(ips().resources.map { it.id })
    }

    @Test
    fun `a hash depends on the source, so two sources never share one by accident`() {
        val entries = listOf(entry("""{"resourceType":"Condition","code":{"text":"Asthma"}}"""))

        val a = FhirIdAssigner.assign(entries, "https://a.example").resources.single().id
        val b = FhirIdAssigner.assign(entries, "https://b.example").resources.single().id

        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `a valid id is kept and absolute references to entries in the file are rewritten`() {
        val entries = listOf(
            entry("""{"resourceType":"Patient","id":"p1"}""", "https://x.example/fhir/Patient/p1"),
            entry(
                """{"resourceType":"Condition","id":"c1","subject":{"reference":"https://x.example/fhir/Patient/p1"},""" +
                    """"asserter":{"reference":"https://elsewhere.example/Practitioner/9"}}""",
                "https://x.example/fhir/Condition/c1",
            ),
        )

        val condition = FhirIdAssigner.assign(entries, "https://x.example/fhir").resources.single { it.type == "Condition" }

        assertThat(condition.id).isEqualTo("c1")
        assertThat(references(condition.json)).containsExactly("Patient/p1", "https://elsewhere.example/Practitioner/9")
    }

    @Test
    fun `a contained resource can point back at its container`() {
        val entries = listOf(
            entry(
                """{"resourceType":"Condition","id":"c1","contained":[""" +
                    """{"resourceType":"Observation","id":"o1","focus":[{"reference":"#"}]}]}""",
            ),
        )

        val lifted = FhirIdAssigner.assign(entries, scope).resources.single { it.type == "Observation" }

        assertThat(references(lifted.json)).containsExactly("Condition/c1")
    }

    @Test
    fun `two different resources with one type and id keep the later one and say so`() {
        val entries = listOf(
            entry("""{"resourceType":"Condition","id":"c1","code":{"text":"Asthma"}}"""),
            entry("""{"resourceType":"Condition","id":"c1","code":{"text":"Hay fever"}}"""),
            entry("""{"resourceType":"Condition","id":"c2","code":{"text":"Same"}}"""),
            entry("""{"resourceType":"Condition","id":"c2","code":{"text":"Same"}}"""),
        )

        val result = FhirIdAssigner.assign(entries, scope)

        assertThat(result.resources.map { it.id }).containsExactly("c1", "c2")
        assertThat(result.resources.first().json.obj("code")!!.string("text")).isEqualTo("Hay fever")
        assertThat(result.notes).containsExactly(FhirIdNote.Duplicate("Condition", "c1"))
    }

    @Test
    fun `an entry with no id takes the one its absolute fullUrl names`() {
        val entries = listOf(entry("""{"resourceType":"Condition","code":{"text":"x"}}""", "https://x.example/fhir/Condition/c9"))

        assertThat(FhirIdAssigner.assign(entries, "https://x.example/fhir").resources.single().id).isEqualTo("c9")
    }

    @Test
    fun `the id comes right after the resource type`() {
        val prepared = FhirIdAssigner.assign(listOf(entry("""{"resourceType":"Condition","code":{"text":"x"}}""")), scope)

        assertThat(prepared.resources.single().json.keys.take(2)).containsExactly("resourceType", "id").inOrder()
    }
}
