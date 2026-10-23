package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.medical.FhirTestFiles.json

/** Manual entries are written as valid FHIR: required fields filled, empty ones left out, other fields kept on edit. */
class ManualFhirWriterTest {

    private val today = LocalDate.of(2026, 9, 29)

    private fun write(draft: ManualRecordDraft, base: JsonObject? = null) = ManualFhirWriter.resource(draft, "id-1", today, base)

    private fun hasEmptyValue(element: JsonElement): Boolean = when (element) {
        is JsonObject -> element.isEmpty() || element.values.any(::hasEmptyValue)
        is JsonArray -> element.isEmpty() || element.any(::hasEmptyValue)
        is JsonPrimitive -> element.isString && element.content.isEmpty()
    }

    @Test
    fun `a save stamps its time, to the second, beside the source`() {
        val record = ManualFhirWriter.resource(
            ManualRecordDraft(ManualRecordKind.ALLERGY, name = "Pollen"),
            "id-1",
            today,
            savedAt = Instant.parse("2026-09-29T10:15:30.123Z"),
        )

        assertThat(record.obj("meta")?.string("lastUpdated")).isEqualTo("2026-09-29T10:15:30Z")
        assertThat(record.obj("meta")?.string("source")).isEqualTo(ManualFhirWriter.SourceBaseUri)
    }

    @Test
    fun `every kind passes pre-flight, lands in its category, and has no empty value`() {
        ManualRecordKind.entries.forEach { kind ->
            val record = write(ManualRecordDraft(kind, name = "Something", date = today, result = "1"))

            assertThat(FhirPreflight.check(PreparedResource(kind.resourceType, "id-1", record))).isNull()
            assertThat(FhirResourceTypes.likelyCategory(record)).isEqualTo(kind.category)
            // A doctor or a clinic is not a record about the owner.
            assertThat(record.toString().contains("\"reference\":\"Patient/self\"")).isEqualTo(!kind.isDirectory)
            assertThat(record.obj("meta")?.string("source")).isEqualTo(ManualFhirWriter.SourceBaseUri)
            assertThat(hasEmptyValue(record)).isFalse()
        }
    }

    @Test
    fun `every kind writes only fields its FHIR R4 resource has, and every one it requires`() {
        val doctor = ManualReference(ManualReference.Practitioner, "Dr Rivera", "p-1")
        val clinic = ManualReference(ManualReference.Organization, "City Clinic", "o-1")
        ManualRecordKind.entries.forEach { kind ->
            val record = write(
                ManualRecordDraft(
                    kind,
                    name = "Something",
                    date = today,
                    detail = "Detail",
                    criticality = "high",
                    code = ManualCode("http://snomed.info/sct", "1"),
                    note = "A note",
                    doctor = doctor,
                    organization = clinic,
                    result = "5",
                    unit = "mg",
                    interpretation = "H",
                    referenceRange = "1-2",
                    contact = "+34 600 000 000",
                    place = ManualReference(ManualReference.Location, "Room 4", "l-1"),
                    familyName = "Moss",
                    gender = "female",
                ),
            )
            val elements = R4Elements.getValue(kind.resourceType)

            assertThat(record.keys - CommonElements).isNotEmpty()
            assertThat(record.keys - CommonElements - elements).isEmpty()
            assertThat(record.keys).containsAtLeastElementsIn(R4Required.getValue(kind.resourceType))
        }
    }

    @Test
    fun `a vaccine carries its status, date, name, lot number and code`() {
        val record = write(
            ManualRecordDraft(
                ManualRecordKind.VACCINE,
                name = "Influenza",
                date = LocalDate.of(2025, 10, 2),
                detail = "AB12",
                code = ManualCode("http://hl7.org/fhir/sid/cvx", "140"),
            ),
        )

        assertThat(record.string("status")).isEqualTo("completed")
        assertThat(record.string("occurrenceDateTime")).isEqualTo("2025-10-02")
        assertThat(record.string("lotNumber")).isEqualTo("AB12")
        assertThat(record.obj("vaccineCode")).isEqualTo(
            json("""{"coding":[{"system":"http://hl7.org/fhir/sid/cvx","code":"140"}],"text":"Influenza"}"""),
        )
    }

    @Test
    fun `an allergy and a condition carry a coded clinical status and the day they were recorded`() {
        val allergy = write(ManualRecordDraft(ManualRecordKind.ALLERGY, name = "Peanut", criticality = "high", detail = "Hives"))
        val condition = write(ManualRecordDraft(ManualRecordKind.CONDITION, name = "Asthma", status = "remission"))

        assertThat(allergy.obj("clinicalStatus")?.objects("coding")?.single()?.string("code")).isEqualTo("active")
        assertThat(allergy.string("criticality")).isEqualTo("high")
        assertThat(allergy.objects("reaction").single().objects("manifestation").single().string("text")).isEqualTo("Hives")
        assertThat(allergy.string("recordedDate")).isEqualTo("2026-09-29")
        assertThat(condition.obj("clinicalStatus")?.objects("coding")?.single()?.string("system"))
            .isEqualTo("http://terminology.hl7.org/CodeSystem/condition-clinical")
        assertThat(condition.obj("clinicalStatus")?.objects("coding")?.single()?.string("code")).isEqualTo("remission")
    }

    @Test
    fun `a medication without a start date or dosage leaves both out`() {
        val record = write(ManualRecordDraft(ManualRecordKind.MEDICATION, name = "Metformin"))

        assertThat(record.string("status")).isEqualTo("active")
        assertThat(record.keys).containsNoneOf("effectivePeriod", "dosage", "note")
        assertThat(record.obj("medicationCodeableConcept")).isEqualTo(json("""{"text":"Metformin"}"""))
    }

    @Test
    fun `an edit replaces what the form holds and keeps every other field`() {
        val stored = json(
            """{"resourceType":"Condition","id":"id-1","meta":{"source":"openvitals://manual","versionId":"3"},
               "clinicalStatus":{"coding":[{"system":"x","code":"active"}]},"code":{"text":"Asthma"},
               "subject":{"reference":"Patient/self"},"recordedDate":"2024-01-01","severity":{"text":"Mild"},
               "note":[{"text":"Old note"}]}""",
        )

        val edited = write(ManualRecordDraft(ManualRecordKind.CONDITION, name = "Asthma, allergic", status = "resolved"), base = stored)

        assertThat(edited.obj("code")?.string("text")).isEqualTo("Asthma, allergic")
        assertThat(edited.obj("severity")).isEqualTo(json("""{"text":"Mild"}"""))
        assertThat(edited.string("recordedDate")).isEqualTo("2024-01-01")
        assertThat(edited.obj("meta")?.string("versionId")).isEqualTo("3")
        assertThat(edited.keys).doesNotContain("note")
    }

    @Test
    fun `a stored record reads back into the same form`() {
        val draft = ManualRecordDraft(
            ManualRecordKind.ALLERGY,
            name = "Penicillin",
            status = "inactive",
            date = LocalDate.of(2010, 5, 1),
            detail = "Rash",
            criticality = "low",
            code = ManualCode("http://snomed.info/sct", "91936005"),
            note = "Seen once",
        )

        assertThat(ManualFhirWriter.draftOf(ManualRecordKind.ALLERGY, write(draft))).isEqualTo(draft)
    }

    @Test
    fun `a visit carries its class, what it was, its day, the reason, the doctor and the clinic`() {
        val record = write(
            ManualRecordDraft(
                ManualRecordKind.VISIT,
                name = "Cardiology check-up",
                date = LocalDate.of(2026, 9, 1),
                setting = "VR",
                detail = "Palpitations",
                doctor = ManualReference(ManualReference.Practitioner, "Dr Rivera", "p-1"),
                organization = ManualReference(ManualReference.Organization, "Heart Centre", "o-1"),
            ),
        )

        assertThat(record.string("status")).isEqualTo("finished")
        assertThat(record.obj("class")).isEqualTo(
            json("""{"system":"http://terminology.hl7.org/CodeSystem/v3-ActCode","code":"VR"}"""),
        )
        assertThat(record.objects("type").single()).isEqualTo(json("""{"text":"Cardiology check-up"}"""))
        assertThat(record.obj("period")).isEqualTo(json("""{"start":"2026-09-01"}"""))
        assertThat(record.objects("reasonCode").single().string("text")).isEqualTo("Palpitations")
        assertThat(record.objects("participant").single())
            .isEqualTo(json("""{"individual":{"reference":"Practitioner/p-1","display":"Dr Rivera"}}"""))
        assertThat(record.obj("serviceProvider")).isEqualTo(json("""{"reference":"Organization/o-1","display":"Heart Centre"}"""))
    }

    @Test
    fun `a procedure carries its code, its day, and the doctor acting for the clinic`() {
        val clinic = ManualReference(ManualReference.Organization, "City Hospital", "o-1")
        val record = write(
            ManualRecordDraft(
                ManualRecordKind.PROCEDURE,
                name = "Colonoscopy",
                date = LocalDate.of(2026, 3, 4),
                code = ManualCode("http://snomed.info/sct", "73761001"),
                doctor = ManualReference(ManualReference.Practitioner, "Dr Okafor", "p-1"),
                organization = clinic,
            ),
        )
        val clinicOnly = write(ManualRecordDraft(ManualRecordKind.PROCEDURE, name = "X-ray", date = today, organization = clinic))

        assertThat(record.string("status")).isEqualTo("completed")
        assertThat(record.string("performedDateTime")).isEqualTo("2026-03-04")
        assertThat(record.obj("code")?.string("text")).isEqualTo("Colonoscopy")
        assertThat(record.objects("performer").single()).isEqualTo(
            json(
                """{"actor":{"reference":"Practitioner/p-1","display":"Dr Okafor"},
                   "onBehalfOf":{"reference":"Organization/o-1","display":"City Hospital"}}""",
            ),
        )
        assertThat(record.keys).doesNotContain("reasonCode")
        assertThat(clinicOnly.objects("performer").single())
            .isEqualTo(json("""{"actor":{"reference":"Organization/o-1","display":"City Hospital"}}"""))
    }

    @Test
    fun `a lab result is filed as laboratory, a number as a quantity and words as text`() {
        val numeric = write(
            ManualRecordDraft(
                ManualRecordKind.LAB_RESULT,
                name = "C-reactive protein",
                date = today,
                result = "<0,50",
                unit = "mg/L",
                interpretation = "N",
                referenceRange = "< 5 mg/L",
                organization = ManualReference(ManualReference.Organization, "Central Lab", "o-1"),
            ),
        )
        val worded = write(ManualRecordDraft(ManualRecordKind.LAB_RESULT, name = "COVID-19 PCR", date = today, result = "Negative"))

        assertThat(FhirResourceTypes.likelyCategory(numeric)).isEqualTo(MedicalCategory.LAB_RESULTS)
        assertThat(numeric.string("status")).isEqualTo("final")
        assertThat(numeric.string("effectiveDateTime")).isEqualTo("2026-09-29")
        // The number stays a JSON number, with the precision the report printed.
        assertThat(numeric.obj("valueQuantity").toString()).isEqualTo("""{"value":0.50,"comparator":"<","unit":"mg/L"}""")
        assertThat(numeric.objects("interpretation").single()).isEqualTo(
            json("""{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation","code":"N"}]}"""),
        )
        assertThat(numeric.objects("referenceRange").single().string("text")).isEqualTo("< 5 mg/L")
        assertThat(numeric.objects("performer").single()).isEqualTo(json("""{"reference":"Organization/o-1","display":"Central Lab"}"""))
        assertThat(worded.string("valueString")).isEqualTo("Negative")
        assertThat(worded.keys).containsNoneOf("valueQuantity", "interpretation", "referenceRange", "performer")
    }

    @Test
    fun `a stored visit, procedure, lab result, doctor and clinic read back into the same form`() {
        val doctor = ManualReference(ManualReference.Practitioner, "Dr Okafor", "p-1")
        val clinic = ManualReference(ManualReference.Organization, "St. Mary's", "o-1")
        val drafts = listOf(
            ManualRecordDraft(
                ManualRecordKind.VISIT,
                name = "Emergency visit",
                status = "cancelled",
                date = LocalDate.of(2025, 1, 2),
                setting = "EMER",
                detail = "Fall",
                doctor = doctor,
                organization = clinic,
            ),
            ManualRecordDraft(
                ManualRecordKind.PROCEDURE,
                name = "Appendectomy",
                status = "in-progress",
                date = LocalDate.of(2025, 2, 3),
                code = ManualCode("http://www.ama-assn.org/go/cpt", "44950"),
                detail = "Appendicitis",
                doctor = doctor,
                organization = clinic,
                note = "Keyhole",
            ),
            ManualRecordDraft(
                ManualRecordKind.LAB_RESULT,
                name = "HbA1c",
                status = "preliminary",
                date = LocalDate.of(2025, 3, 4),
                code = ManualCode("http://loinc.org", "4548-4"),
                result = ">=6.5",
                unit = "%",
                interpretation = "H",
                referenceRange = "4.0-5.6 %",
                organization = ManualReference(ManualReference.Organization, "Central Lab", "o-2"),
            ),
            ManualRecordDraft(ManualRecordKind.PRACTITIONER, name = "Dr Okafor", detail = "MD", contact = "okafor@example.org"),
            ManualRecordDraft(ManualRecordKind.ORGANIZATION, name = "St. Mary's", detail = "1 Main St", contact = "+44 20 0000 0000"),
        )

        drafts.forEach { draft -> assertThat(ManualFhirWriter.draftOf(draft.kind, write(draft))).isEqualTo(draft) }
    }

    @Test
    fun `a doctor or clinic typed as a new name is written beside the record, and a picked one is not`() {
        val draft = ManualRecordDraft(
            ManualRecordKind.VISIT,
            name = "Check-up",
            date = today,
            doctor = ManualReference(ManualReference.Practitioner, " Dr Lee "),
            organization = ManualReference(ManualReference.Organization, "Clinic", "o-9"),
        )
        val ids = ArrayDeque(listOf("new-1"))

        val (linked, created) = ManualFhirWriter.linkNewReferences(draft, today, { ids.removeFirst() })

        assertThat(linked.doctor).isEqualTo(ManualReference(ManualReference.Practitioner, "Dr Lee", "new-1"))
        assertThat(linked.organization).isEqualTo(draft.organization)
        assertThat(created.single()).isEqualTo(
            json("""{"resourceType":"Practitioner","id":"new-1","meta":{"source":"openvitals://manual"},"name":[{"text":"Dr Lee"}]}"""),
        )
    }

    @Test
    fun `a typed name that matches a saved doctor points at that doctor`() {
        val saved = ManualReference(ManualReference.Practitioner, "Dr Lee", "p-1")
        val directory = ManualDirectory(practitioners = listOf(saved))

        assertThat(directory.resolve(ManualReference(ManualReference.Practitioner, "dr lee "))).isEqualTo(saved)
        assertThat(directory.resolve(ManualReference(ManualReference.Practitioner, "Dr Kim")))
            .isEqualTo(ManualReference(ManualReference.Practitioner, "Dr Kim"))
        assertThat(directory.resolve(ManualReference(ManualReference.Practitioner, "  "))).isNull()
    }

    @Test
    fun `a vital sign is filed by its code and category, with its value in the unit it was typed in`() {
        val record = write(
            ManualRecordDraft(
                ManualRecordKind.VITAL_SIGN,
                name = "Peso",
                date = today,
                measure = ObservationMeasure.BODY_WEIGHT,
                result = "172,4",
                unit = "[lb_av]",
            ),
        )

        assertThat(FhirResourceTypes.likelyCategory(record)).isEqualTo(MedicalCategory.VITAL_SIGNS)
        assertThat(record.obj("code")).isEqualTo(
            json("""{"coding":[{"system":"http://loinc.org","code":"29463-7","display":"Body weight"}],"text":"Peso"}"""),
        )
        assertThat(record.obj("valueQuantity").toString())
            .isEqualTo("""{"value":172.4,"unit":"lb","system":"http://unitsofmeasure.org","code":"[lb_av]"}""")
        assertThat(record.objects("category").single().objects("coding").single().string("code")).isEqualTo("vital-signs")
    }

    @Test
    fun `a blood pressure is the systolic and diastolic components of one panel`() {
        val record = write(
            ManualRecordDraft(ManualRecordKind.VITAL_SIGN, date = today, measure = ObservationMeasure.BLOOD_PRESSURE, result = "128", secondResult = "82"),
        )

        assertThat(record.obj("code")?.objects("coding")?.single()?.string("code")).isEqualTo("85354-9")
        assertThat(record.keys).containsNoneOf("valueQuantity", "valueString")
        val components = record.objects("component").associate { part ->
            part.obj("code")!!.objects("coding").single().string("code") to part.obj("valueQuantity")!!.primitive("value")!!.content
        }
        assertThat(components).containsExactly("8480-6", "128", "8462-4", "82")
    }

    @Test
    fun `smoking status is a SNOMED answer with the words the user saw, under social history`() {
        val smoking = write(
            ManualRecordDraft(
                ManualRecordKind.SOCIAL_HISTORY,
                name = "Tabaquismo",
                date = today,
                measure = ObservationMeasure.SMOKING_STATUS,
                answer = "8517006",
                result = "Exfumador",
            ),
        )
        val other = write(
            ManualRecordDraft(
                ManualRecordKind.SOCIAL_HISTORY,
                name = "Occupation",
                date = today,
                measure = ObservationMeasure.OTHER_SOCIAL_HISTORY,
                result = "Night shifts",
            ),
        )

        assertThat(FhirResourceTypes.likelyCategory(smoking)).isEqualTo(MedicalCategory.SOCIAL_HISTORY)
        assertThat(smoking.obj("valueCodeableConcept")).isEqualTo(
            json("""{"coding":[{"system":"http://snomed.info/sct","code":"8517006","display":"Ex-smoker"}],"text":"Exfumador"}"""),
        )
        // With no code of its own, free social history is filed by its category alone.
        assertThat(FhirResourceTypes.likelyCategory(other)).isEqualTo(MedicalCategory.SOCIAL_HISTORY)
        assertThat(other.obj("code")).isEqualTo(json("""{"text":"Occupation"}"""))
        assertThat(other.string("valueString")).isEqualTo("Night shifts")
    }

    @Test
    fun `a pregnancy record is filed by its code alone, and a due date may lie ahead`() {
        val due = write(
            ManualRecordDraft(ManualRecordKind.PREGNANCY, date = today, measure = ObservationMeasure.DELIVERY_DATE, valueDate = today.plusMonths(5)),
        )
        val births = ManualRecordDraft(ManualRecordKind.PREGNANCY, date = today, measure = ObservationMeasure.BIRTHS_LIVE, result = "2")

        assertThat(FhirResourceTypes.likelyCategory(due)).isEqualTo(MedicalCategory.PREGNANCY)
        assertThat(due.keys).doesNotContain("category")
        assertThat(due.string("valueDateTime")).isEqualTo("2027-02-28")
        assertThat(write(births).primitive("valueInteger")?.content).isEqualTo("2")
        assertThat(ManualRecordDraft(ManualRecordKind.PREGNANCY, date = today, measure = ObservationMeasure.DELIVERY_DATE, valueDate = today.plusMonths(5)).isComplete(today)).isTrue()
        assertThat(births.copy(result = "-1").isComplete(today)).isFalse()
        assertThat(births.copy(result = "").isComplete(today)).isFalse()
    }

    @Test
    fun `every measure reads back into the same form`() {
        ObservationMeasure.entries.forEach { measure ->
            val draft = ManualRecordDraft(
                measure.kind,
                name = measure.display,
                status = "amended",
                date = today,
                measure = measure,
                result = when (measure.value) {
                    MeasureValue.QUANTITY, MeasureValue.BLOOD_PRESSURE -> "12.5"
                    MeasureValue.INTEGER -> "3"
                    MeasureValue.CODED -> "In words"
                    MeasureValue.TEXT -> "Free text"
                    MeasureValue.DATE -> ""
                },
                secondResult = if (measure.value == MeasureValue.BLOOD_PRESSURE) "80" else "",
                unit = measure.units.lastOrNull()?.ucum.orEmpty(),
                answer = measure.answers.lastOrNull()?.snomed,
                valueDate = today.plusDays(30).takeIf { measure.value == MeasureValue.DATE },
                note = "Checked twice",
            )

            assertWithMessage(measure.name).that(ManualFhirWriter.draftOf(measure.kind, write(draft))).isEqualTo(draft)
            assertWithMessage(measure.name).that(FhirResourceTypes.likelyCategory(write(draft))).isEqualTo(measure.kind.category)
        }
    }

    @Test
    fun `a prescription is an order, with who prescribed it and how to take it`() {
        val record = write(
            ManualRecordDraft(
                ManualRecordKind.PRESCRIPTION,
                name = "Amoxicillin 500 mg",
                date = LocalDate.of(2026, 9, 20),
                detail = "One capsule three times a day for 7 days",
                doctor = ManualReference(ManualReference.Practitioner, "Dr Lee", "p-1"),
            ),
        )

        assertThat(record.string("status")).isEqualTo("active")
        assertThat(record.string("intent")).isEqualTo("order")
        assertThat(record.string("authoredOn")).isEqualTo("2026-09-20")
        assertThat(record.obj("requester")).isEqualTo(json("""{"reference":"Practitioner/p-1","display":"Dr Lee"}"""))
        assertThat(record.objects("dosageInstruction").single().string("text")).isEqualTo("One capsule three times a day for 7 days")
    }

    @Test
    fun `a place has one address, a clinic a list of them, and a doctor's role needs its doctor`() {
        val place = write(ManualRecordDraft(ManualRecordKind.LOCATION, name = "Imaging wing", detail = "2 Hill Rd"))
        val clinic = write(ManualRecordDraft(ManualRecordKind.ORGANIZATION, name = "City Clinic", detail = "2 Hill Rd"))
        val role = ManualRecordDraft(
            ManualRecordKind.PRACTITIONER_ROLE,
            name = "Cardiologist",
            detail = "Electrophysiology",
            doctor = ManualReference(ManualReference.Practitioner, "Dr Lee", "p-1"),
            organization = ManualReference(ManualReference.Organization, "City Clinic", "o-1"),
            place = ManualReference(ManualReference.Location, "Imaging wing", "l-1"),
        )

        assertThat(place.obj("address")).isEqualTo(json("""{"text":"2 Hill Rd"}"""))
        assertThat(clinic.objects("address").single()).isEqualTo(json("""{"text":"2 Hill Rd"}"""))
        assertThat(write(role).obj("practitioner")?.string("reference")).isEqualTo("Practitioner/p-1")
        assertThat(write(role).objects("location").single().string("reference")).isEqualTo("Location/l-1")
        assertThat(role.isComplete(today)).isTrue()
        assertThat(role.copy(doctor = null).isComplete(today)).isFalse()
    }

    @Test
    fun `a doctor's role, a place, a prescription and the owner read back into the same form`() {
        val drafts = listOf(
            ManualRecordDraft(
                ManualRecordKind.PRESCRIPTION,
                name = "Amoxicillin",
                status = "completed",
                date = LocalDate.of(2026, 9, 20),
                code = ManualCode("http://www.whocc.no/atc", "J01CA04"),
                detail = "500 mg every 8 h",
                doctor = ManualReference(ManualReference.Practitioner, "Dr Lee", "p-1"),
                note = "Finished the course",
            ),
            ManualRecordDraft(
                ManualRecordKind.PRACTITIONER_ROLE,
                name = "Cardiologist",
                detail = "Electrophysiology",
                doctor = ManualReference(ManualReference.Practitioner, "Dr Lee", "p-1"),
                organization = ManualReference(ManualReference.Organization, "City Clinic", "o-1"),
                place = ManualReference(ManualReference.Location, "Imaging wing", "l-1"),
                contact = "+44 20 0000 0000",
            ),
            ManualRecordDraft(ManualRecordKind.LOCATION, name = "Imaging wing", detail = "2 Hill Rd", contact = "wing@example.org"),
            ManualRecordDraft(
                ManualRecordKind.PATIENT,
                name = "Robin Ann",
                familyName = "Moss",
                date = LocalDate.of(1990, 1, 1),
                gender = "female",
                detail = "1 Main St",
                contact = "+44 20 1111 1111",
                doctor = ManualReference(ManualReference.Practitioner, "Dr Lee", "p-1"),
            ),
        )

        drafts.forEach { draft -> assertThat(ManualFhirWriter.draftOf(draft.kind, write(draft))).isEqualTo(draft) }
    }

    @Test
    fun `the owner's record leaves out what is not known`() {
        val full = ManualFhirWriter.patient(PatientIdentity(listOf("Robin", "Ann"), listOf("Moss"), "1990-01-01"))
        val nameOnly = ManualFhirWriter.patient(PatientIdentity(emptyList(), listOf("Moss"), null))

        assertThat(full).isEqualTo(
            json(
                """{"resourceType":"Patient","id":"self","meta":{"source":"openvitals://manual"},
                   "name":[{"use":"usual","given":["Robin","Ann"],"family":"Moss"}],"birthDate":"1990-01-01"}""",
            ),
        )
        assertThat(nameOnly.keys).doesNotContain("birthDate")
        assertThat(hasEmptyValue(nameOnly)).isFalse()
    }

    @Test
    fun `a name is needed, a vaccine needs its date, and no date lies ahead`() {
        assertThat(ManualRecordDraft(ManualRecordKind.CONDITION).isComplete(today)).isFalse()
        assertThat(ManualRecordDraft(ManualRecordKind.CONDITION, name = "Asthma").isComplete(today)).isTrue()
        assertThat(ManualRecordDraft(ManualRecordKind.VACCINE, name = "Tetanus").isComplete(today)).isFalse()
        assertThat(ManualRecordDraft(ManualRecordKind.MEDICATION, name = "X", date = today.plusDays(1)).isComplete(today)).isFalse()
    }

    @Test
    fun `only what is still ahead may be dated ahead`() {
        val tomorrow = today.plusDays(1)

        assertThat(ManualRecordDraft(ManualRecordKind.VISIT, name = "Dentist", status = "planned", date = tomorrow).isComplete(today)).isTrue()
        assertThat(ManualRecordDraft(ManualRecordKind.VISIT, name = "Dentist", date = tomorrow).isComplete(today)).isFalse()
        assertThat(ManualRecordDraft(ManualRecordKind.PROCEDURE, name = "Biopsy", status = "preparation", date = tomorrow).isComplete(today)).isTrue()
        assertThat(ManualRecordDraft(ManualRecordKind.MEDICATION, name = "Statin", status = "intended", date = tomorrow).isComplete(today)).isTrue()
    }

    @Test
    fun `a visit and a procedure need their day, and a lab result its result too`() {
        assertThat(ManualRecordDraft(ManualRecordKind.VISIT, name = "Check-up").isComplete(today)).isFalse()
        assertThat(ManualRecordDraft(ManualRecordKind.PROCEDURE, name = "Biopsy", date = today).isComplete(today)).isTrue()
        assertThat(ManualRecordDraft(ManualRecordKind.LAB_RESULT, name = "Glucose", date = today).isComplete(today)).isFalse()
        assertThat(ManualRecordDraft(ManualRecordKind.LAB_RESULT, name = "Glucose", date = today, result = "5.1").isComplete(today)).isTrue()
    }

    private companion object {
        val CommonElements = setOf("resourceType", "id", "meta")

        /** Every top-level element FHIR R4 defines on the types a manual entry writes, from hl7.org/fhir/R4. */
        val R4Elements: Map<String, Set<String>> = mapOf(
            "Immunization" to setOf(
                "identifier", "status", "statusReason", "vaccineCode", "patient", "encounter", "occurrenceDateTime",
                "occurrenceString", "recorded", "primarySource", "reportOrigin", "location", "manufacturer", "lotNumber",
                "expirationDate", "site", "route", "doseQuantity", "performer", "note", "reasonCode", "reasonReference",
                "isSubpotent", "subpotentReason", "education", "programEligibility", "fundingSource", "reaction", "protocolApplied",
            ),
            "AllergyIntolerance" to setOf(
                "identifier", "clinicalStatus", "verificationStatus", "type", "category", "criticality", "code", "patient",
                "encounter", "onsetDateTime", "onsetAge", "onsetPeriod", "onsetRange", "onsetString", "recordedDate",
                "recorder", "asserter", "lastOccurrence", "note", "reaction",
            ),
            "MedicationStatement" to setOf(
                "identifier", "basedOn", "partOf", "status", "statusReason", "category", "medicationCodeableConcept",
                "medicationReference", "subject", "context", "effectiveDateTime", "effectivePeriod", "dateAsserted",
                "informationSource", "derivedFrom", "reasonCode", "reasonReference", "note", "dosage",
            ),
            "Condition" to setOf(
                "identifier", "clinicalStatus", "verificationStatus", "category", "severity", "code", "bodySite", "subject",
                "encounter", "onsetDateTime", "onsetAge", "onsetPeriod", "onsetRange", "onsetString", "abatementDateTime",
                "abatementAge", "abatementPeriod", "abatementRange", "abatementString", "recordedDate", "recorder", "asserter",
                "stage", "evidence", "note",
            ),
            "Encounter" to setOf(
                "identifier", "status", "statusHistory", "class", "classHistory", "type", "serviceType", "priority", "subject",
                "episodeOfCare", "basedOn", "participant", "appointment", "period", "length", "reasonCode", "reasonReference",
                "diagnosis", "account", "hospitalization", "location", "serviceProvider", "partOf",
            ),
            "Procedure" to setOf(
                "identifier", "instantiatesCanonical", "instantiatesUri", "basedOn", "partOf", "status", "statusReason",
                "category", "code", "subject", "encounter", "performedDateTime", "performedPeriod", "performedString",
                "performedAge", "performedRange", "recorder", "asserter", "performer", "location", "reasonCode",
                "reasonReference", "bodySite", "outcome", "report", "complication", "complicationDetail", "followUp", "note",
                "focalDevice", "usedReference", "usedCode",
            ),
            "Observation" to setOf(
                "identifier", "basedOn", "partOf", "status", "category", "code", "subject", "focus", "encounter",
                "effectiveDateTime", "effectivePeriod", "effectiveTiming", "effectiveInstant", "issued", "performer",
                "valueQuantity", "valueCodeableConcept", "valueString", "valueBoolean", "valueInteger", "valueRange",
                "valueRatio", "valueSampledData", "valueTime", "valueDateTime", "valuePeriod", "dataAbsentReason",
                "interpretation", "note", "bodySite", "method", "specimen", "device", "referenceRange", "hasMember",
                "derivedFrom", "component",
            ),
            "Practitioner" to setOf(
                "identifier", "active", "name", "telecom", "address", "gender", "birthDate", "photo", "qualification", "communication",
            ),
            "Organization" to setOf(
                "identifier", "active", "type", "name", "alias", "telecom", "address", "partOf", "contact", "endpoint",
            ),
            "MedicationRequest" to setOf(
                "identifier", "status", "statusReason", "intent", "category", "priority", "doNotPerform", "reportedBoolean",
                "reportedReference", "medicationCodeableConcept", "medicationReference", "subject", "encounter",
                "supportingInformation", "authoredOn", "requester", "performer", "performerType", "recorder", "reasonCode",
                "reasonReference", "instantiatesCanonical", "instantiatesUri", "basedOn", "groupIdentifier",
                "courseOfTherapyType", "insurance", "note", "dosageInstruction", "dispenseRequest", "substitution",
                "priorPrescription", "detectedIssue", "eventHistory",
            ),
            "PractitionerRole" to setOf(
                "identifier", "active", "period", "practitioner", "organization", "code", "specialty", "location",
                "healthcareService", "telecom", "availableTime", "notAvailable", "availabilityExceptions", "endpoint",
            ),
            "Location" to setOf(
                "identifier", "status", "operationalStatus", "name", "alias", "description", "mode", "type", "telecom",
                "address", "physicalType", "position", "managingOrganization", "partOf", "hoursOfOperation",
                "availabilityExceptions", "endpoint",
            ),
            "Patient" to setOf(
                "identifier", "active", "name", "telecom", "gender", "birthDate", "deceasedBoolean", "deceasedDateTime",
                "address", "maritalStatus", "multipleBirthBoolean", "multipleBirthInteger", "photo", "contact",
                "communication", "generalPractitioner", "managingOrganization", "link",
            ),
        )

        /** The top-level elements FHIR R4 requires (cardinality 1..). */
        val R4Required: Map<String, Set<String>> = mapOf(
            "Immunization" to setOf("status", "vaccineCode", "patient"),
            "AllergyIntolerance" to setOf("patient"),
            "MedicationStatement" to setOf("status", "subject"),
            "Condition" to setOf("subject"),
            "Encounter" to setOf("status", "class"),
            "Procedure" to setOf("status", "subject"),
            "Observation" to setOf("status", "code"),
            "Practitioner" to emptySet(),
            "Organization" to emptySet(),
            "MedicationRequest" to setOf("status", "intent", "subject"),
            "PractitionerRole" to emptySet(),
            "Location" to emptySet(),
            "Patient" to emptySet(),
        )
    }
}
