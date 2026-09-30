package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.MedicalRecordRef

/** A list shows events. Places, organisations and drug definitions show inside what points at them. */
class MedicalRecordRowsTest {

    private fun record(source: String, type: String, id: String, json: String) =
        MedicalRecord(MedicalRecordRef(source, type, id), MedicalCategory.VISITS, "4.0.1", json)

    private val encounter = record(
        "s1", "Encounter", "e1",
        """{"resourceType":"Encounter","id":"e1","serviceProvider":{"reference":"Organization/o1"},""" +
            """"location":[{"location":{"reference":"Location/l1"}}]}""",
    )
    private val organisation = record("s1", "Organization", "o1", """{"resourceType":"Organization","id":"o1"}""")
    private val location = record("s1", "Location", "l1", """{"resourceType":"Location","id":"l1"}""")

    @Test
    fun `a definition that an event points at is not a row`() {
        val rows = MedicalRecordRows.rows(listOf(encounter, organisation, location))

        assertThat(rows).containsExactly(encounter)
    }

    @Test
    fun `a definition nothing points at is a row`() {
        val lonely = record("s1", "Organization", "o2", """{"resourceType":"Organization","id":"o2"}""")

        assertThat(MedicalRecordRows.rows(listOf(encounter, lonely))).containsExactly(encounter, lonely)
    }

    @Test
    fun `a reference resolves inside its own source only`() {
        val otherSourceOrganisation = record("s2", "Organization", "o1", """{"resourceType":"Organization","id":"o1"}""")

        val rows = MedicalRecordRows.rows(listOf(encounter, otherSourceOrganisation))

        assertThat(rows).containsExactly(encounter, otherSourceOrganisation)
    }

    @Test
    fun `only a relative type and id reference becomes a ref`() {
        assertThat(MedicalRecordRows.refOf("s1", "Patient/p1")).isEqualTo(MedicalRecordRef("s1", "Patient", "p1"))
        assertThat(MedicalRecordRows.refOf("s1", "https://x.example/fhir/Patient/p1")).isNull()
        assertThat(MedicalRecordRows.refOf("s1", "#local")).isNull()
        assertThat(MedicalRecordRows.refOf("s1", null)).isNull()
    }
}
