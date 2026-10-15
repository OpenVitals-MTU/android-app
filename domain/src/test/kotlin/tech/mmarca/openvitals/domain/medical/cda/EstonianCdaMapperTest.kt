package tech.mmarca.openvitals.domain.medical.cda

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import tech.mmarca.openvitals.domain.medical.FhirPreflight
import tech.mmarca.openvitals.domain.medical.MedicalSourceSkipReason
import tech.mmarca.openvitals.domain.medical.PreparedResource

/** Estonian CDA documents become valid FHIR: the right record per entry, Estonian time, stable ids, no ID code. */
class EstonianCdaMapperTest {

    private fun map(xml: String): List<JsonObject> {
        val root = requireNotNull(parseCda(xml.toByteArray()))
        assertThat(EstonianCdaMapper.isEstonian(root)).isTrue()
        return (EstonianCdaMapper.map(root) as CdaMapResult.Mapped).entries.map { it.resource }
    }

    private fun List<JsonObject>.ofType(type: String) = filter { it.string("resourceType") == type }

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
    fun `every record of every document passes pre-flight and has no empty value`() {
        val all = listOf(EstonianCdaFixtures.epicrisis, EstonianCdaFixtures.immunisation, EstonianCdaFixtures.dental, EstonianCdaFixtures.referralResponse(), EstonianCdaFixtures.infectionNotice)
            .flatMap(::map)

        all.forEach { resource ->
            val prepared = PreparedResource(resource.string("resourceType")!!, resource.string("id")!!, resource)
            assertThat(FhirPreflight.check(prepared)).isNull()
            assertThat(hasEmptyValue(resource)).isFalse()
            // Health Connect refuses a no-break space in a FHIR string.
            assertThat(resource.toString()).doesNotContain("\u00A0")
        }
    }

    @Test
    fun `the patient keeps name, sex and birth date, and never the ID code, address or phone`() {
        val patient = map(EstonianCdaFixtures.epicrisis).ofType("Patient").single()

        assertThat(patient.string("id")).isEqualTo("self")
        assertThat(patient.first("name").string("family")).isEqualTo("Maasikas")
        assertThat(patient.string("gender")).isEqualTo("female")
        assertThat(patient.string("birthDate")).isEqualTo("1990-01-01")
        assertThat(patient.keys).containsNoneOf("identifier", "address", "telecom")
        assertThat(patient.toString()).doesNotContain("49001019999")
    }

    @Test
    fun `the clinic and the doctor are named by their registry codes`() {
        val records = map(EstonianCdaFixtures.epicrisis)

        assertThat(records.ofType("Organization").single().string("id")).isEqualTo("ee-org-90000001")
        val doctor = records.ofType("Practitioner").single()
        assertThat(doctor.string("id")).isEqualTo("ee-prac-D99999")
        assertThat(doctor.first("qualification").coding("code").string("code")).isEqualTo("DOCTOR")
    }

    @Test
    fun `a visit points at the clinic and the doctor, in Estonian time`() {
        val visit = map(EstonianCdaFixtures.epicrisis).ofType("Encounter").single()

        assertThat(visit.obj("class").string("code")).isEqualTo("AMB")
        assertThat(visit.obj("period").string("start")).isEqualTo("2024-03-15T10:30:00+02:00")
        assertThat(visit.obj("serviceProvider").string("reference")).isEqualTo("Organization/ee-org-90000001")
    }

    @Test
    fun `diagnoses are ICD-10 conditions of the visit, and a provisional one says so`() {
        val records = map(EstonianCdaFixtures.epicrisis)
        val visitId = records.ofType("Encounter").single().string("id")
        val (infection, cough) = records.ofType("Condition")

        assertThat(infection.coding("code").string("system")).isEqualTo("http://hl7.org/fhir/sid/icd-10")
        assertThat(infection.coding("code").string("code")).isEqualTo("J06.9")
        assertThat(infection.coding("verificationStatus").string("code")).isEqualTo("confirmed")
        assertThat(infection.obj("encounter").string("reference")).isEqualTo("Encounter/$visitId")
        assertThat(cough.coding("verificationStatus").string("code")).isEqualTo("provisional")
        assertThat(cough.string("recordedDate")).isEqualTo("2024-03-14")
        assertThat(cough.first("note").string("text")).isEqualTo("Köha kestnud nädal.")
    }

    @Test
    fun `a prescription keeps the ATC code and its dosage as written`() {
        val prescription = map(EstonianCdaFixtures.epicrisis).ofType("MedicationRequest").single()

        assertThat(prescription.coding("medicationCodeableConcept").string("system")).isEqualTo("http://www.whocc.no/atc")
        assertThat(prescription.string("status")).isEqualTo("unknown")
        assertThat(prescription.string("intent")).isEqualTo("order")
        assertThat(prescription.first("dosageInstruction").string("text")).isEqualTo("1 TA · 3 PV · tablett")
    }

    @Test
    fun `lab results take their value from wherever the panel holds it, with ranges and flags`() {
        val results = map(EstonianCdaFixtures.epicrisis).ofType("Observation").associateBy { it.coding("code").string("code") }

        val hemoglobin = results.getValue("718-7")
        assertThat(hemoglobin.obj("valueQuantity").toString()).isEqualTo("""{"value":142,"unit":"g/l"}""")
        assertThat(hemoglobin.first("referenceRange").obj("low").string("value")).isEqualTo("117")
        // The specimen was taken at 09:00 in winter.
        assertThat(hemoglobin.string("effectiveDateTime")).isEqualTo("2024-03-15T09:00:00+02:00")
        assertThat(hemoglobin.first("category").first("coding").string("code")).isEqualTo("laboratory")

        val leukocytes = results.getValue("6690-2")
        assertThat(leukocytes.obj("valueQuantity").string("value")).isEqualTo("11.4")
        assertThat(leukocytes.first("interpretation").first("coding").string("code")).isEqualTo("H")
        assertThat(leukocytes.first("interpretation").first("coding").string("display")).isEqualTo("High")
        assertThat(leukocytes.first("referenceRange").string("text")).isEqualTo("3,5 .. 8,8")

        assertThat(results.getValue("1988-5").obj("valueRange").obj("high").string("value")).isEqualTo("5")
        assertThat(results.getValue("5811-5").string("valueString")).isEqualTo("1.015")
        // A result coded only by its role belongs to the procedure's test.
        assertThat(results.getValue("94500-6").coding("valueCodeableConcept").string("code")).isEqualTo("N")
    }

    @Test
    fun `a study done is one procedure with its description and its finding`() {
        val study = map(EstonianCdaFixtures.epicrisis).ofType("Procedure").single()

        assertThat(study.coding("code").string("code")).isEqualTo("7968")
        assertThat(study.obj("code").string("text")).isEqualTo("Kopsude röntgen (üks ülesvõte)")
        assertThat(study.getValue("note").jsonArray.map { it.jsonObject.string("text") }).containsExactly("Kopsuväljad puhtad.", "Järeldus: norm.")
    }

    @Test
    fun `a vaccination keeps the vaccine, lot, dose number and target disease`() {
        val vaccination = map(EstonianCdaFixtures.immunisation).ofType("Immunization").single()

        assertThat(vaccination.coding("vaccineCode").string("code")).isEqualTo("J07BX03")
        assertThat(vaccination.obj("vaccineCode").string("text")).isEqualTo("Näidisvaktsiin")
        assertThat(vaccination.string("lotNumber")).isEqualTo("AB1234")
        assertThat(vaccination.string("occurrenceDateTime")).isEqualTo("2021-06-01")
        val protocol = vaccination.first("protocolApplied")
        assertThat(protocol.getValue("doseNumberPositiveInt").jsonPrimitive.content).isEqualTo("2")
        assertThat(protocol.first("targetDisease").first("coding").string("code")).isEqualTo("101")
    }

    @Test
    fun `dental work and diagnoses name the tooth and belong to the dental visit`() {
        val records = map(EstonianCdaFixtures.dental)
        val visitId = records.ofType("Encounter").single().string("id")
        val caries = records.ofType("Condition").single()
        val filling = records.ofType("Procedure").single()

        assertThat(caries.first("bodySite").first("coding").string("code")).isEqualTo("16")
        assertThat(filling.first("bodySite").first("coding").string("code")).isEqualTo("16")
        assertThat(filling.obj("encounter").string("reference")).isEqualTo("Encounter/$visitId")
        assertThat(filling.string("performedDateTime")).isEqualTo("2023-09-10")
    }

    @Test
    fun `radiology is a procedure with its finding, and a pathology diagnosis is a condition`() {
        val records = map(EstonianCdaFixtures.referralResponse())
        val xray = records.ofType("Procedure").single()
        val pathology = records.ofType("Condition").single()

        assertThat(xray.obj("category").string("text")).isEqualTo("Röntgenuuring")
        assertThat(xray.first("note").string("text")).isEqualTo("Leid: kopsud puhtad.")
        assertThat(xray.string("performedDateTime")).isEqualTo("2024-04-01T10:00:00+03:00")
        assertThat(pathology.coding("code").string("system")).isEqualTo("urn:oid:1.3.6.1.4.1.28284.6.2.1.275.8")
        assertThat(pathology.first("bodySite").first("coding").string("code")).isEqualTo("23456789")
    }

    @Test
    fun `a notifiable disease is a condition, and the place of the bite stays out`() {
        val records = map(EstonianCdaFixtures.infectionNotice)

        assertThat(records.ofType("Condition").single().coding("code").string("code")).isEqualTo("A69.2")
        assertThat(records.toString()).doesNotContain("Näidisküla")
    }

    @Test
    fun `a referral gives only its diagnoses, all provisional, noting the referral`() {
        val records = map(EstonianCdaFixtures.referral)
        val conditions = records.ofType("Condition")

        assertThat(records.map { it.string("resourceType") }.toSet()).containsExactly("Patient", "Organization", "Practitioner", "Condition")
        assertThat(conditions.map { it.coding("verificationStatus").string("code") }).containsExactly("provisional", "provisional")
        assertThat(conditions.first().first("note").string("text")).isEqualTo("Saatekiri uuringule")
        assertThat(conditions.none { "encounter" in it }).isTrue()
    }

    @Test
    fun `declarations are left out with their reason`() {
        val declaration = EstonianCdaMapper.map(parseCda(EstonianCdaFixtures.declaration.toByteArray())!!)

        assertThat((declaration as CdaMapResult.Skipped).reason).isEqualTo(MedicalSourceSkipReason.NOT_A_RECORD)
    }

    @Test
    fun `the same document gives the same ids, and another document other ids`() {
        val first = map(EstonianCdaFixtures.epicrisis).map { it.string("id") }
        val again = map(EstonianCdaFixtures.epicrisis).map { it.string("id") }
        val other = map(EstonianCdaFixtures.referralResponse()).ofType("Procedure").map { it.string("id") }

        assertThat(again).isEqualTo(first)
        assertThat(first).containsNoneIn(other)
        assertThat(first.filterNotNull().all { it.length <= 64 && Regex("[A-Za-z0-9.-]+").matches(it) }).isTrue()
    }

    @Test
    fun `a time with no zone gets Estonian winter or summer time, and a date stays a date`() {
        val tallinn = CdaProfile.Estonian.zone
        assertThat(fhirDateTime("20240115083000", tallinn)).isEqualTo("2024-01-15T08:30:00+02:00")
        assertThat(fhirDateTime("20240715083000", tallinn)).isEqualTo("2024-07-15T08:30:00+03:00")
        assertThat(fhirDateTime("202407150830+0100", tallinn)).isEqualTo("2024-07-15T08:30:00+01:00")
        assertThat(fhirDateTime("20240715", tallinn)).isEqualTo("2024-07-15")
        assertThat(fhirDateTime("202407", tallinn)).isEqualTo("2024-07")
        assertThat(fhirDateTime("20241345", tallinn)).isNull()
        // With no zone known, a time keeps only its date: FHIR refuses a time with no zone.
        assertThat(fhirDateTime("20240715083000", null)).isEqualTo("2024-07-15")
        assertThat(fhirDateTime("20240715083000-0500", null)).isEqualTo("2024-07-15T08:30:00-05:00")
    }

    @Test
    fun `a document that is not Estonian CDA is not read as one`() {
        val other = parseCda("""<ClinicalDocument xmlns="urn:hl7-org:v3"><templateId root="2.16.840.1.113883.10.20.22.1.1"/></ClinicalDocument>""".toByteArray())!!

        assertThat(EstonianCdaMapper.isEstonian(other)).isFalse()
        assertThat(parseCda("not xml".toByteArray())).isNull()
    }
}
