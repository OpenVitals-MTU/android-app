package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/** DSTU2 records become valid R4: fields renamed, statuses mapped, single values as lists, and nothing empty left behind. */
class Dstu2ToR4Test {

    private fun convert(json: String): JsonObject = requireNotNull(Dstu2ToR4.convert(Json.parseToJsonElement(json).jsonObject))

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.content

    private fun JsonObject.code(key: String): String? = getValue(key).jsonObject.getValue("coding").jsonArray.first().jsonObject.text("code")

    private fun hasEmptyValue(element: JsonElement): Boolean = when (element) {
        is JsonObject -> element.isEmpty() || element.values.any(::hasEmptyValue)
        is JsonArray -> element.isEmpty() || element.any(::hasEmptyValue)
        is JsonPrimitive -> false
    }

    @Test
    fun `an immunization takes its date, status, source and protocol in R4's shape`() {
        val given = convert(
            """{"resourceType":"Immunization","id":"i1","status":"completed","date":"2015-06-01","vaccineCode":{"text":"MMR"},
               "patient":{"reference":"Patient/p"},"wasNotGiven":false,"reported":false,"performer":{"display":"Dr A"},
               "requester":{"display":"Dr B"},"vaccinationProtocol":[{"doseSequence":2,"targetDisease":[{"text":"Measles"}]},{"description":"no dose"}],
               "text":{"status":"generated","div":"<div>old</div>"},"meta":{"profile":["http://fhir.org/guides/argonaut/StructureDefinition/argo-immunization"]}}""",
        )
        val refused = convert("""{"resourceType":"Immunization","id":"i2","status":"completed","date":"2016","vaccineCode":{"text":"Flu"},"patient":{"reference":"Patient/p"},"wasNotGiven":true}""")

        assertThat(given.text("occurrenceDateTime")).isEqualTo("2015-06-01")
        assertThat(given.text("primarySource")).isEqualTo("true")
        assertThat(given.getValue("performer").jsonArray.single().jsonObject.getValue("actor").jsonObject.text("display")).isEqualTo("Dr A")
        assertThat(given.getValue("protocolApplied").jsonArray.single().jsonObject.text("doseNumberPositiveInt")).isEqualTo("2")
        assertThat(given.keys).containsNoneOf("date", "wasNotGiven", "reported", "requester", "vaccinationProtocol", "text")
        assertThat(given.getValue("meta").jsonObject.keys).containsExactly("tag")
        assertThat(refused.text("status")).isEqualTo("not-done")
    }

    @Test
    fun `an allergy's one status becomes clinical and verification statuses, and its substance the code`() {
        val allergy = convert(
            """{"resourceType":"AllergyIntolerance","id":"a1","status":"confirmed","substance":{"text":"Peanut"},"patient":{"reference":"Patient/p"},
               "onset":"2001","category":"food","criticality":"CRITH","note":{"text":"carries adrenaline"},
               "reaction":[{"certainty":"likely","manifestation":[{"text":"Swelling"}],"note":{"text":"fast"}}]}""",
        )
        val wrong = convert("""{"resourceType":"AllergyIntolerance","id":"a2","status":"entered-in-error","substance":{"text":"x"},"patient":{"reference":"Patient/p"},"category":"other"}""")

        assertThat(allergy.getValue("code").jsonObject.text("text")).isEqualTo("Peanut")
        assertThat(allergy.code("clinicalStatus")).isEqualTo("active")
        assertThat(allergy.code("verificationStatus")).isEqualTo("confirmed")
        assertThat(allergy.text("onsetDateTime")).isEqualTo("2001")
        assertThat(allergy.getValue("category").jsonArray.single().jsonPrimitive.content).isEqualTo("food")
        assertThat(allergy.text("criticality")).isEqualTo("high")
        assertThat(allergy.getValue("note").jsonArray).hasSize(1)
        val reaction = allergy.getValue("reaction").jsonArray.single().jsonObject
        assertThat(reaction.keys).doesNotContain("certainty")
        assertThat(reaction.getValue("note").jsonArray).hasSize(1)
        assertThat(wrong.keys).containsNoneOf("clinicalStatus", "category")
        assertThat(wrong.code("verificationStatus")).isEqualTo("entered-in-error")
    }

    @Test
    fun `a condition's statuses become coded, and its patient the subject`() {
        val condition = convert(
            """{"resourceType":"Condition","id":"c1","patient":{"reference":"Patient/p"},"code":{"text":"Asthma"},"clinicalStatus":"relapse",
               "verificationStatus":"confirmed","category":{"text":"problem"},"dateRecorded":"2012-03-04","notes":"since childhood"}""",
        )

        assertThat(condition.getValue("subject").jsonObject.text("reference")).isEqualTo("Patient/p")
        assertThat(condition.code("clinicalStatus")).isEqualTo("recurrence")
        assertThat(condition.code("verificationStatus")).isEqualTo("confirmed")
        assertThat(condition.getValue("category").jsonArray).hasSize(1)
        assertThat(condition.text("recordedDate")).isEqualTo("2012-03-04")
        assertThat(condition.getValue("note").jsonArray.single().jsonObject.text("text")).isEqualTo("since childhood")
    }

    @Test
    fun `a medication order becomes a medication request, with its contained medication and its dose converted`() {
        val request = convert(
            """{"resourceType":"MedicationOrder","id":"m1","status":"active","dateWritten":"2018-01-02","patient":{"reference":"Patient/p"},
               "prescriber":{"display":"Dr A"},"medicationReference":{"reference":"#med"},"reasonCodeableConcept":{"text":"Pain"},
               "contained":[{"resourceType":"Medication","id":"med","code":{"text":"Ibuprofen"},"isBrand":false,"product":{"form":{"text":"tablet"}}}],
               "dosageInstruction":[{"text":"1 tablet","additionalInstructions":{"text":"with food"},"doseQuantity":{"value":1},
                 "timing":{"repeat":{"frequency":3,"period":1,"periodUnits":"d"}}}],
               "dispenseRequest":{"medicationCodeableConcept":{"text":"Ibuprofen"},"numberOfRepeatsAllowed":2}}""",
        )
        val dosage = request.getValue("dosageInstruction").jsonArray.single().jsonObject
        val medication = request.getValue("contained").jsonArray.single().jsonObject

        assertThat(request.text("resourceType")).isEqualTo("MedicationRequest")
        assertThat(request.text("intent")).isEqualTo("order")
        assertThat(request.text("authoredOn")).isEqualTo("2018-01-02")
        assertThat(request.keys).containsAtLeast("subject", "requester", "reasonCode")
        assertThat(dosage.getValue("doseAndRate").jsonArray.single().jsonObject.keys).containsExactly("doseQuantity")
        assertThat(dosage.getValue("additionalInstruction").jsonArray).hasSize(1)
        assertThat(dosage.getValue("timing").jsonObject.getValue("repeat").jsonObject.text("periodUnit")).isEqualTo("d")
        assertThat(medication.getValue("form").jsonObject.text("text")).isEqualTo("tablet")
        assertThat(medication.keys).containsNoneOf("isBrand", "product")
        assertThat(request.getValue("dispenseRequest").jsonObject.keys).containsExactly("numberOfRepeatsAllowed")
    }

    @Test
    fun `a medication statement not taken says so in its status`() {
        val statement = convert(
            """{"resourceType":"MedicationStatement","id":"s1","status":"completed","patient":{"reference":"Patient/p"},"wasNotTaken":true,
               "medicationCodeableConcept":{"text":"Aspirin"},"reasonForUseCodeableConcept":{"text":"Headache"},"note":"stopped",
               "dosage":[{"quantityQuantity":{"value":2}}]}""",
        )

        assertThat(statement.text("status")).isEqualTo("not-taken")
        assertThat(statement.getValue("reasonCode").jsonArray).hasSize(1)
        assertThat(statement.getValue("dosage").jsonArray.single().jsonObject.getValue("doseAndRate").jsonArray.single().jsonObject.keys).containsExactly("doseQuantity")
        assertThat(statement.getValue("note").jsonArray.single().jsonObject.text("text")).isEqualTo("stopped")
    }

    @Test
    fun `an observation's category moves to R4's code system, so Health Connect can sort it`() {
        val observation = convert(
            """{"resourceType":"Observation","id":"o1","status":"final","code":{"text":"Glucose"},"subject":{"reference":"Patient/p"},
               "category":{"coding":[{"system":"http://hl7.org/fhir/observation-category","code":"laboratory"}]},
               "valueQuantity":{"value":5.4,"unit":"mmol/L"},"comments":"fasting","interpretation":{"text":"normal"},
               "referenceRange":[{"low":{"value":3.9},"meaning":{"text":"normal"}}],
               "related":[{"type":"has-member","target":{"reference":"Observation/x"}},{"type":"derived-from","target":{"reference":"Observation/y"}}]}""",
        )

        assertThat(FhirResourceTypes.likelyCategory(observation)).isEqualTo(tech.mmarca.openvitals.domain.model.MedicalCategory.LAB_RESULTS)
        assertThat(observation.getValue("interpretation").jsonArray).hasSize(1)
        assertThat(observation.getValue("note").jsonArray.single().jsonObject.text("text")).isEqualTo("fasting")
        assertThat(observation.getValue("referenceRange").jsonArray.single().jsonObject.keys).containsExactly("low", "type")
        assertThat(observation.getValue("hasMember").jsonArray).hasSize(1)
        assertThat(observation.getValue("derivedFrom").jsonArray).hasSize(1)
        assertThat(observation.keys).containsNoneOf("comments", "related")
    }

    @Test
    fun `a procedure not performed, or aborted, takes R4's status`() {
        val notDone = convert("""{"resourceType":"Procedure","id":"p1","status":"completed","notPerformed":true,"subject":{"reference":"Patient/p"},"code":{"text":"X-ray"},"reasonCodeableConcept":{"text":"pain"},"notes":[{"text":"declined"}]}""")
        val aborted = convert("""{"resourceType":"Procedure","id":"p2","status":"aborted","subject":{"reference":"Patient/p"},"code":{"text":"Scan"},"performer":[{"actor":{"display":"Dr A"},"role":{"text":"surgeon"}}]}""")

        assertThat(notDone.text("status")).isEqualTo("not-done")
        assertThat(notDone.getValue("reasonCode").jsonArray).hasSize(1)
        assertThat(notDone.getValue("note").jsonArray).hasSize(1)
        assertThat(aborted.text("status")).isEqualTo("stopped")
        assertThat(aborted.getValue("performer").jsonArray.single().jsonObject.keys).containsExactly("actor", "function")
    }

    @Test
    fun `a patient's several family names become one, and every converted record is tagged and free of empty values`() {
        val patient = convert("""{"resourceType":"Patient","id":"p","name":[{"family":["Maasikas","Tamm"],"given":["Mari"]}],"careProvider":[{"display":"Dr A"}],"contained":[],"extension":[]}""")

        assertThat(patient.getValue("name").jsonArray.single().jsonObject.text("family")).isEqualTo("Maasikas Tamm")
        assertThat(patient.keys).contains("generalPractitioner")
        assertThat(Dstu2ToR4.isConverted(patient)).isTrue()
        assertThat(hasEmptyValue(patient)).isFalse()
    }

    @Test
    fun `a type with no mapping is not converted`() {
        assertThat(Dstu2ToR4.convert(Json.parseToJsonElement("""{"resourceType":"DiagnosticOrder","id":"d"}""").jsonObject)).isNull()
    }
}
