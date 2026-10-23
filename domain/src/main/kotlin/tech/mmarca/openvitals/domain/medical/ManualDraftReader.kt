package tech.mmarca.openvitals.domain.medical

import java.time.LocalDate
import kotlinx.serialization.json.JsonObject

/** Reads what [ManualFhirWriter] wrote back into the form that wrote it. */
internal object ManualDraftReader {
    private const val SystolicLoinc = "8480-6"
    private const val DiastolicLoinc = "8462-4"

    fun draftOf(kind: ManualRecordKind, json: JsonObject): ManualRecordDraft = when (kind) {
        ManualRecordKind.VACCINE,
        ManualRecordKind.ALLERGY,
        ManualRecordKind.MEDICATION,
        ManualRecordKind.CONDITION,
        -> clinicalDraftOf(kind, json)
        ManualRecordKind.PRESCRIPTION -> prescriptionDraftOf(json)
        ManualRecordKind.VISIT,
        ManualRecordKind.PROCEDURE,
        ManualRecordKind.LAB_RESULT,
        -> eventDraftOf(kind, json)
        ManualRecordKind.VITAL_SIGN,
        ManualRecordKind.SOCIAL_HISTORY,
        ManualRecordKind.PREGNANCY,
        -> measuredDraftOf(kind, json)
        ManualRecordKind.PRACTITIONER,
        ManualRecordKind.PRACTITIONER_ROLE,
        ManualRecordKind.ORGANIZATION,
        ManualRecordKind.LOCATION,
        ManualRecordKind.PATIENT,
        -> directoryDraftOf(kind, json)
    }

    private fun prescriptionDraftOf(json: JsonObject): ManualRecordDraft =
        baseDraft(ManualRecordKind.PRESCRIPTION, json, json.obj("medicationCodeableConcept"), json.string("status"), json.string("authoredOn"))
            .copy(
                detail = json.objects("dosageInstruction").firstOrNull()?.string("text").orEmpty(),
                doctor = json.obj("requester").toReference(ManualReference.Practitioner),
            )

    private fun clinicalDraftOf(kind: ManualRecordKind, json: JsonObject): ManualRecordDraft {
        val concept = when (kind) {
            ManualRecordKind.VACCINE -> json.obj("vaccineCode")
            ManualRecordKind.MEDICATION -> json.obj("medicationCodeableConcept")
            else -> json.obj("code")
        }
        val status = when (kind) {
            ManualRecordKind.VACCINE, ManualRecordKind.MEDICATION -> json.string("status")
            else -> json.obj("clinicalStatus")?.objects("coding")?.firstOrNull()?.string("code")
        }
        val dateText = when (kind) {
            ManualRecordKind.VACCINE -> json.string("occurrenceDateTime")
            ManualRecordKind.MEDICATION -> json.obj("effectivePeriod")?.string("start") ?: json.string("effectiveDateTime")
            else -> json.string("onsetDateTime")
        }
        val detail = when (kind) {
            ManualRecordKind.VACCINE -> json.string("lotNumber")
            ManualRecordKind.ALLERGY -> json.objects("reaction").firstOrNull()?.objects("manifestation")?.firstOrNull()?.codeableLabel()
            ManualRecordKind.MEDICATION -> json.objects("dosage").firstOrNull()?.string("text")
            else -> null
        }
        return baseDraft(kind, json, concept, status, dateText).copy(
            detail = detail.orEmpty(),
            criticality = json.string("criticality").takeIf { kind == ManualRecordKind.ALLERGY },
        )
    }

    /** A visit, a procedure or a lab result: something that took place on a day, and who was there. */
    private fun eventDraftOf(kind: ManualRecordKind, json: JsonObject): ManualRecordDraft {
        val concept = if (kind == ManualRecordKind.VISIT) json.objects("type").firstOrNull() else json.obj("code")
        val dateText = when (kind) {
            ManualRecordKind.VISIT -> json.obj("period")?.string("start")
            ManualRecordKind.PROCEDURE -> json.string("performedDateTime") ?: json.obj("performedPeriod")?.string("start")
            else -> json.string("effectiveDateTime") ?: json.obj("effectivePeriod")?.string("start")
        }
        val draft = baseDraft(kind, json, concept, json.string("status"), dateText).copy(
            detail = json.objects("reasonCode").firstOrNull()?.codeableLabel().orEmpty(),
        )
        return when (kind) {
            ManualRecordKind.VISIT -> draft.copy(
                setting = json.obj("class")?.string("code")?.takeIf { it in VisitSettings } ?: VisitSettings.first(),
                doctor = json.objects("participant").firstOrNull()?.obj("individual").toReference(ManualReference.Practitioner),
                organization = json.obj("serviceProvider").toReference(ManualReference.Organization),
                place = json.objects("location").firstOrNull()?.obj("location").toReference(ManualReference.Location),
            )
            ManualRecordKind.PROCEDURE -> {
                val performer = json.objects("performer").firstOrNull()
                val actor = performer?.obj("actor").toReference(ManualReference.Practitioner)
                draft.copy(
                    doctor = actor?.takeIf { it.type == ManualReference.Practitioner },
                    organization = performer?.obj("onBehalfOf").toReference(ManualReference.Organization)
                        ?: actor?.takeIf { it.type == ManualReference.Organization },
                    place = json.obj("location").toReference(ManualReference.Location),
                )
            }
            else -> {
                val quantity = json.obj("valueQuantity")
                draft.copy(
                    result = quantity?.let { it.string("comparator").orEmpty() + it.primitive("value")?.content.orEmpty() }
                        ?: json.string("valueString").orEmpty(),
                    unit = quantity?.string("unit").orEmpty(),
                    interpretation = json.objects("interpretation").firstOrNull()?.objects("coding")?.firstOrNull()
                        ?.string("code")?.takeIf { it in LabInterpretations },
                    referenceRange = json.objects("referenceRange").firstOrNull()?.string("text").orEmpty(),
                    organization = json.objects("performer").firstOrNull().toReference(ManualReference.Organization),
                )
            }
        }
    }

    /** A vital sign, social history or pregnancy record: its measure, by its LOINC code, and that measure's value. */
    private fun measuredDraftOf(kind: ManualRecordKind, json: JsonObject): ManualRecordDraft {
        val concept = json.obj("code")
        val measure = ObservationMeasure.ofLoinc(kind, concept?.objects("coding")?.firstOrNull { it.string("system") == Loinc }?.string("code"))
        val draft = baseDraft(kind, json, concept, json.string("status"), json.string("effectiveDateTime")).copy(
            measure = measure,
            code = null,
        )
        fun JsonObject.quantityValue(): String = obj("valueQuantity")?.primitive("value")?.content.orEmpty()
        return when (measure?.value) {
            MeasureValue.QUANTITY -> draft.copy(
                result = json.quantityValue(),
                unit = json.obj("valueQuantity")?.string("code").orEmpty(),
            )
            MeasureValue.BLOOD_PRESSURE -> {
                fun component(loinc: String) = json.objects("component")
                    .firstOrNull { part -> part.obj("code")?.objects("coding").orEmpty().any { it.string("code") == loinc } }
                draft.copy(
                    result = component(SystolicLoinc)?.quantityValue().orEmpty(),
                    secondResult = component(DiastolicLoinc)?.quantityValue().orEmpty(),
                    unit = measure.units.first().ucum,
                )
            }
            MeasureValue.INTEGER -> draft.copy(result = json.primitive("valueInteger")?.content.orEmpty())
            MeasureValue.DATE -> draft.copy(
                valueDate = json.string("valueDateTime")?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            )
            MeasureValue.CODED -> {
                val value = json.obj("valueCodeableConcept")
                draft.copy(
                    answer = value?.objects("coding")?.firstOrNull { it.string("system") == Snomed }?.string("code"),
                    result = value?.string("text").orEmpty(),
                )
            }
            MeasureValue.TEXT -> draft.copy(result = json.string("valueString").orEmpty())
            null -> draft
        }
    }

    /** A doctor, a role, a clinic, a place or the owner, with its first phone number or email address. */
    private fun directoryDraftOf(kind: ManualRecordKind, json: JsonObject): ManualRecordDraft {
        val draft = ManualRecordDraft(kind = kind, contact = json.objects("telecom").firstOrNull()?.string("value").orEmpty())
        return when (kind) {
            ManualRecordKind.PRACTITIONER -> draft.copy(
                name = practitionerName(json).orEmpty(),
                detail = json.objects("qualification").firstOrNull()?.obj("code")?.codeableLabel().orEmpty(),
            )
            ManualRecordKind.PRACTITIONER_ROLE -> draft.copy(
                name = json.objects("code").firstOrNull()?.codeableLabel().orEmpty(),
                detail = json.objects("specialty").firstOrNull()?.codeableLabel().orEmpty(),
                doctor = json.obj("practitioner").toReference(ManualReference.Practitioner),
                organization = json.obj("organization").toReference(ManualReference.Organization),
                place = json.objects("location").firstOrNull().toReference(ManualReference.Location),
            )
            ManualRecordKind.LOCATION -> draft.copy(
                name = json.string("name").orEmpty(),
                detail = json.obj("address")?.string("text").orEmpty(),
            )
            ManualRecordKind.PATIENT -> {
                val name = json.objects("name").firstOrNull { it.string("use") == "usual" } ?: json.objects("name").firstOrNull()
                draft.copy(
                    name = name?.strings("given").orEmpty().joinToString(" "),
                    familyName = name?.string("family").orEmpty(),
                    date = json.string("birthDate")?.takeIf { it.length >= 10 }?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() },
                    gender = json.string("gender")?.takeIf { it in Genders },
                    detail = json.objects("address").firstOrNull()?.string("text").orEmpty(),
                    doctor = json.objects("generalPractitioner").firstOrNull().toReference(ManualReference.Practitioner),
                )
            }
            else -> draft.copy(
                name = json.string("name").orEmpty(),
                detail = json.objects("address").firstOrNull()?.string("text").orEmpty(),
            )
        }
    }

    /** The name as written, or its given and family parts joined. */
    fun practitionerName(json: JsonObject): String? {
        val name = json.objects("name").firstOrNull() ?: return null
        return name.string("text") ?: (name.strings("given") + listOfNotNull(name.string("family"))).joinToString(" ").takeIf { it.isNotBlank() }
    }

    private fun baseDraft(
        kind: ManualRecordKind,
        json: JsonObject,
        concept: JsonObject?,
        status: String?,
        dateText: String?,
    ): ManualRecordDraft {
        val coding = concept?.objects("coding")?.firstOrNull()
        return ManualRecordDraft(
            kind = kind,
            name = concept?.codeableLabel().orEmpty(),
            status = status?.takeIf { it in kind.statuses } ?: kind.statuses.firstOrNull().orEmpty(),
            date = dateText?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            code = coding?.string("code")?.let { ManualCode(coding.string("system"), it) },
            note = json.objects("note").firstOrNull()?.string("text").orEmpty(),
        )
    }

    /** A reference read back. The type comes from `Type/id` when there is one, else [fallbackType]. */
    private fun JsonObject?.toReference(fallbackType: String): ManualReference? {
        val target = this?.string("reference")?.split('/')?.takeIf { it.size == 2 }
        val display = this?.string("display").orEmpty()
        if (target == null && display.isBlank()) return null
        return ManualReference(type = target?.first() ?: fallbackType, display = display, id = target?.last())
    }
}
