package tech.mmarca.openvitals.domain.medical.cda

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import tech.mmarca.openvitals.domain.medical.FhirEntry
import tech.mmarca.openvitals.domain.medical.FhirPreflight
import tech.mmarca.openvitals.domain.medical.PreparedResource

/** A standard C-CDA becomes valid FHIR: one record per entry, absences left out, and no identifiers of the person. */
class CcdaMapperTest {

    private fun entries(xml: String, contentHash: String = "hash"): List<FhirEntry> {
        val root = requireNotNull(parseCda(xml.toByteArray()))
        assertThat(isClinicalDocument(root)).isTrue()
        assertThat(EstonianCdaMapper.isEstonian(root)).isFalse()
        return (CcdaMapper.map(root, contentHash) as CdaMapResult.Mapped).entries
    }

    private val records by lazy { entries(CcdaFixtures.summary).map { it.resource } }

    private fun ofType(type: String) = records.filter { it.string("resourceType") == type }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.content

    private fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

    private fun JsonObject.first(key: String): JsonObject = getValue(key).jsonArray.first().jsonObject

    private fun JsonObject.coding(key: String): JsonObject = obj(key).first("coding")

    private fun hasEmptyValue(element: JsonElement): Boolean = when (element) {
        is JsonObject -> element.isEmpty() || element.values.any(::hasEmptyValue)
        is JsonArray -> element.isEmpty() || element.any(::hasEmptyValue)
        is JsonPrimitive -> element.isString && element.content.isBlank()
    }

    @Test
    fun `every record passes pre-flight, has no empty value, and a valid id`() {
        records.forEach { resource ->
            assertThat(FhirPreflight.check(PreparedResource(resource.string("resourceType")!!, resource.string("id")!!, resource))).isNull()
            assertThat(hasEmptyValue(resource)).isFalse()
        }
        assertThat(records.map { it.string("id") }.distinct()).hasSize(records.size)
    }

    @Test
    fun `the patient keeps name, sex and birth date, and no identifier, address or phone`() {
        val patient = ofType("Patient").single()

        assertThat(patient.string("id")).isEqualTo("patient")
        assertThat(patient.string("gender")).isEqualTo("female")
        assertThat(patient.string("birthDate")).isEqualTo("1990-01-01")
        assertThat(patient.keys).containsNoneOf("identifier", "address", "telecom")
        assertThat(records.toString()).doesNotContain("999-99-9999")
    }

    @Test
    fun `the source is the custodian, by its id`() {
        val origin = entries(CcdaFixtures.summary).first().origin

        assertThat(origin?.baseUri).isEqualTo("openvitals://cda/2.16.840.1.113883.4.6/9999999999")
        assertThat(origin?.displayName).isEqualTo("Sample Health Clinic")
        assertThat(ofType("Organization").single().string("name")).isEqualTo("Sample Health Clinic")
        assertThat(ofType("Practitioner").single().string("id")).isEqualTo("cda-prac-1234567893")
    }

    @Test
    fun `problems are problem list conditions, active or resolved, named from the narrative when not coded`() {
        val (asthma, ankle) = ofType("Condition")

        assertThat(asthma.coding("code").string("system")).isEqualTo("http://snomed.info/sct")
        assertThat(asthma.coding("clinicalStatus").string("code")).isEqualTo("active")
        assertThat(asthma.first("category").first("coding").string("code")).isEqualTo("problem-list-item")
        assertThat(asthma.string("onsetDateTime")).isEqualTo("2019-04-01")
        assertThat(ankle.obj("code").string("text")).isEqualTo("Sprained ankle")
        assertThat(ankle.coding("clinicalStatus").string("code")).isEqualTo("resolved")
        assertThat(ankle.string("abatementDateTime")).isEqualTo("2020-01-25")
    }

    @Test
    fun `an allergy names the substance and its reaction, and no known allergies makes no record`() {
        val allergy = ofType("AllergyIntolerance").single()

        assertThat(allergy.coding("code").string("system")).isEqualTo("http://www.nlm.nih.gov/research/umls/rxnorm")
        assertThat(allergy.coding("code").string("display")).isEqualTo("Penicillin G")
        assertThat(allergy.coding("clinicalStatus").string("code")).isEqualTo("active")
        assertThat(allergy.string("onsetDateTime")).isEqualTo("2015")
        assertThat(allergy.first("reaction").first("manifestation").first("coding").string("display")).isEqualTo("Hives")
    }

    @Test
    fun `a medication keeps its status, period, dose, route and the written instructions`() {
        val medication = ofType("MedicationStatement").single()
        val dosage = medication.first("dosage")

        assertThat(medication.string("status")).isEqualTo("active")
        assertThat(medication.coding("medicationCodeableConcept").string("code")).isEqualTo("745679")
        assertThat(medication.obj("effectivePeriod").string("start")).isEqualTo("2023-01-01")
        assertThat(dosage.string("text")).isEqualTo("2 puffs every 12 hours")
        assertThat(dosage.first("doseAndRate").obj("doseQuantity").string("value")).isEqualTo("2")
        assertThat(dosage.obj("route").string("text")).isEqualTo("Respiratory (inhalation)")
    }

    @Test
    fun `a vaccination given is completed, and one refused is not-done`() {
        val (flu, refused) = ofType("Immunization")

        assertThat(flu.coding("vaccineCode").string("system")).isEqualTo("http://hl7.org/fhir/sid/cvx")
        assertThat(flu.string("status")).isEqualTo("completed")
        assertThat(flu.string("lotNumber")).isEqualTo("LOT42")
        assertThat(flu.string("occurrenceDateTime")).isEqualTo("2023-10-15")
        assertThat(refused.string("status")).isEqualTo("not-done")
    }

    @Test
    fun `results, vital signs and social history are observations in their own categories`() {
        val byCode = ofType("Observation").associateBy { it.coding("code").string("code") }
        fun category(code: String) = byCode.getValue(code).first("category").first("coding").string("code")

        val hemoglobin = byCode.getValue("718-7")
        assertThat(category("718-7")).isEqualTo("laboratory")
        assertThat(hemoglobin.obj("valueQuantity").toString()).isEqualTo("""{"value":13.2,"unit":"g/dL"}""")
        assertThat(hemoglobin.string("effectiveDateTime")).isEqualTo("2024-03-01T08:30:00-05:00")
        assertThat(hemoglobin.first("interpretation").first("coding").string("display")).isEqualTo("Normal")
        assertThat(hemoglobin.first("referenceRange").obj("high").string("value")).isEqualTo("15.5")
        assertThat(byCode.getValue("5778-6").string("valueString")).isEqualTo("Yellow")
        assertThat(byCode.getValue("5778-6").string("effectiveDateTime")).isEqualTo("2024-03-02")
        assertThat(category("8480-6")).isEqualTo("vital-signs")
        assertThat(category("72166-2")).isEqualTo("social-history")
        assertThat(byCode.getValue("72166-2").coding("valueCodeableConcept").string("display")).isEqualTo("Never smoked tobacco")
    }

    @Test
    fun `a procedure keeps its date and body site`() {
        val procedure = ofType("Procedure").single()

        assertThat(procedure.string("status")).isEqualTo("completed")
        assertThat(procedure.string("performedDateTime")).isEqualTo("2010-11-20")
        assertThat(procedure.first("bodySite").first("coding").string("display")).isEqualTo("Appendix")
    }

    @Test
    fun `a visit states its class only when the document codes one, and a time with no zone keeps its date`() {
        val (coded, office) = ofType("Encounter")

        assertThat(coded.obj("class").string("code")).isEqualTo("AMB")
        assertThat(coded.obj("period").string("end")).isEqualTo("2024-03-15T09:30:00-05:00")
        assertThat(office.obj("class").string("code")).isEqualTo("UNK")
        assertThat(office.first("type").first("coding").string("code")).isEqualTo("99213")
        assertThat(office.obj("period").string("start")).isEqualTo("2023-06-01")
    }

    @Test
    fun `a document with no custodian and no id lets the user name the source, and takes its ids from the content`() {
        val first = entries(CcdaFixtures.bare, contentHash = "aaa")
        val again = entries(CcdaFixtures.bare, contentHash = "aaa")
        val other = entries(CcdaFixtures.bare, contentHash = "bbb")

        assertThat(first.map { it.origin }.toSet()).containsExactly(null)
        assertThat(again.map { it.resource.string("id") }).isEqualTo(first.map { it.resource.string("id") })
        assertThat(other.filter { it.type == "Condition" }.map { it.resource.string("id") })
            .containsNoneIn(first.filter { it.type == "Condition" }.map { it.resource.string("id") })
    }

    @Test
    fun `a document written by an app, not a person, gives no vital signs`() {
        val byApp = CcdaFixtures.summary.replace(
            "<assignedPerson><name><given>Sample</given><family>Doctor</family></name></assignedPerson>",
            "<assignedAuthoringDevice><softwareName>Some Health App</softwareName></assignedAuthoringDevice>",
        )
        val categories = entries(byApp).map { it.resource }.filter { it.string("resourceType") == "Observation" }
            .map { it.first("category").first("coding").string("code") }

        assertThat(categories).doesNotContain("vital-signs")
        assertThat(categories).contains("laboratory")
    }

    @Test
    fun `sections it does not know, and narrative, make no record`() {
        assertThat(records.toString()).doesNotContain("History of present illness")
        assertThat(records.map { it.string("resourceType") }.toSet()).containsExactly(
            "Patient", "Organization", "Practitioner", "Condition", "AllergyIntolerance", "MedicationStatement",
            "Immunization", "Observation", "Procedure", "Encounter",
        )
    }
}
