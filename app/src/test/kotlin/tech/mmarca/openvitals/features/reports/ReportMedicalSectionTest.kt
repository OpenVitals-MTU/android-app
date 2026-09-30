package tech.mmarca.openvitals.features.reports

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.FakeMedicalRecordsRepository
import tech.mmarca.openvitals.domain.model.MedicalCategory

/** Standing facts show whole; dated events show when they fall in the range. Nothing is computed. */
class ReportMedicalSectionTest {

    private val repository = FakeMedicalRecordsRepository().apply {
        addSource("clinic", "City Clinic")
        add("clinic", "AllergyIntolerance", "a1", """{"resourceType":"AllergyIntolerance","id":"a1","code":{"text":"Peanut"},"clinicalStatus":{"coding":[{"code":"active"}]},"recordedDate":"2012-04-01"}""")
        add("clinic", "Condition", "c1", """{"resourceType":"Condition","id":"c1","code":{"text":"Asthma"},"clinicalStatus":{"coding":[{"code":"remission"}]},"onsetDateTime":"1998"}""")
        add("clinic", "MedicationStatement", "m1", """{"resourceType":"MedicationStatement","id":"m1","status":"stopped","medicationReference":{"reference":"Medication/med1"}}""")
        add("clinic", "Medication", "med1", """{"resourceType":"Medication","id":"med1","code":{"text":"Metformin"}}""")
        add("clinic", "Immunization", "i-old", """{"resourceType":"Immunization","id":"i-old","status":"completed","vaccineCode":{"text":"Tetanus"},"occurrenceDateTime":"2019-03-14"}""")
        add("clinic", "Immunization", "i-new", """{"resourceType":"Immunization","id":"i-new","status":"completed","vaccineCode":{"text":"Influenza"},"occurrenceDateTime":"2026-09-02"}""")
        add("clinic", "Immunization", "i-undated", """{"resourceType":"Immunization","id":"i-undated","status":"completed","vaccineCode":{"text":"Measles"}}""")
        add(
            "clinic", "Observation", "o1",
            """{"resourceType":"Observation","id":"o1","status":"final","category":[{"coding":[{"code":"laboratory"}]}],"code":{"text":"Glucose"},""" +
                """"effectiveDateTime":"2026-08-20","valueQuantity":{"value":7.2,"unit":"mmol/L"},"interpretation":[{"text":"High"}]}""",
            category = MedicalCategory.LAB_RESULTS,
        )
    }

    private val start = LocalDate.of(2026, 7, 1)
    private val end = LocalDate.of(2026, 9, 29)

    @Test
    fun `allergies, conditions and medications show whole, with their status and source`() = runTest {
        val section = ReportMedicalLoader(repository).load(start, end)

        assertThat(section.allergies.map { it.title to it.status }).containsExactly("Peanut" to "active")
        assertThat(section.conditions.map { it.title to it.date }).containsExactly("Asthma" to "1998")
        // A statement names its drug through a reference, and the row takes the drug's name.
        assertThat(section.medications.map { it.title to it.status }).containsExactly("Metformin" to "stopped")
        assertThat(section.allergies.single().source).isEqualTo("City Clinic")
    }

    @Test
    fun `vaccines and lab results keep only those dated in the range, labs with the value and flag the lab set`() = runTest {
        val section = ReportMedicalLoader(repository).load(start, end)

        assertThat(section.vaccines.map { it.title }).containsExactly("Influenza")
        assertThat(section.labResults.single().title).isEqualTo("Glucose")
        assertThat(section.labResults.single().value).contains("7.2")
        assertThat(section.labResults.single().flag).isEqualTo("High")
    }

    @Test
    fun `a declined category holds only this app's records, and with no access at all it is left out`() = runTest {
        repository.readable = MedicalCategory.entries.toSet() - MedicalCategory.ALLERGIES
        val declined = ReportMedicalLoader(repository).load(start, end)

        repository.writable = false
        val noAccess = ReportMedicalLoader(repository).load(start, end)

        assertThat(declined.ownOnly).containsExactly(MedicalCategory.ALLERGIES)
        assertThat(declined.allergies).hasSize(1)
        assertThat(noAccess.leftOut).containsExactly(MedicalCategory.ALLERGIES)
        assertThat(noAccess.allergies).isEmpty()
    }
}
