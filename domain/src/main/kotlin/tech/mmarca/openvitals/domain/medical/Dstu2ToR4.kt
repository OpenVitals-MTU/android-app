package tech.mmarca.openvitals.domain.medical

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Converts FHIR DSTU2 (1.0.2) resources to R4, for the providers still on it: older iPhones
 * export their records in it, and some labs serve it to the apps that fetch from them. Health
 * Connect stores R4 and R4B only.
 *
 * It is a field mapping, not a full converter: renamed fields, status codes that moved, single
 * values that became lists, and `MedicationOrder`, which became `MedicationRequest`. A field
 * R4 has no place for is dropped. The narrative is dropped too: it described the old shape.
 * Every converted record carries an OpenVitals tag, so it can always be told from one the
 * provider wrote as R4.
 *
 * A plain FHIR file states no version, so [looksDstu2] tells a DSTU2 record by its shape.
 */
object Dstu2ToR4 {
    const val Version = "1.0.2"

    /** Marks a record converted from DSTU2. */
    const val ConvertedTag = "converted-from-dstu2"

    private const val ConvertedDisplay = "Converted from FHIR DSTU2 by OpenVitals"

    /** The R4 form of [resource], or null for a type this does not convert. */
    fun convert(resource: JsonObject): JsonObject? = convertAny(resource)?.let { tagged(it) }

    fun isConverted(resource: JsonObject): Boolean =
        resource.obj("meta")?.objects("tag").orEmpty().any { it.string("system") == OpenVitalsFhir.TagSystem && it.string("code") == ConvertedTag }

    /**
     * Whether [resource] has DSTU2's shape: a field DSTU2 had and R4 dropped or renamed, a single
     * value R4 holds as a list, a type R4 renamed, or a profile that names DSTU2. A record with
     * none of those reads the same in both versions, so it is not one. A converted record never is.
     */
    fun looksDstu2(resource: JsonObject): Boolean {
        if (resource.obj("meta")?.strings("profile").orEmpty().any(::profileNamesDstu2)) return true
        val type = resource.resourceType
        if (Dstu2OnlyFields[type].orEmpty().any { it in resource }) return true
        return when (type) {
            "MedicationOrder" -> true
            "AllergyIntolerance" -> resource["category"] is JsonPrimitive || resource["note"] is JsonObject
            "Condition" -> resource["clinicalStatus"] is JsonPrimitive || resource["verificationStatus"] is JsonPrimitive || resource["category"] is JsonObject
            "Observation" -> resource["category"] is JsonObject || resource["interpretation"] is JsonObject
            "MedicationStatement" -> resource["note"] is JsonPrimitive
            "Procedure" -> resource["reasonReference"] is JsonObject
            "Patient" -> resource.objects("name").any { it["family"] is JsonArray }
            "Practitioner" -> resource["name"] is JsonObject
            "Organization", "Location" -> resource["type"] is JsonObject
            "Encounter" -> resource["class"] is JsonPrimitive
            // Not converted, as Health Connect stores no report, but the results it holds are DSTU2 too.
            "DiagnosticReport" -> resource["category"] is JsonObject || resource["performer"] is JsonObject
            else -> false
        }
    }

    /** Whether a `meta.profile` URL names DSTU2, as a Bundle's or a resource's may. */
    fun profileNamesDstu2(url: String): Boolean = Dstu2ProfileHints.any { url.contains(it, ignoreCase = true) }

    private fun convertAny(resource: JsonObject): JsonObject? {
        val fields = Fields(resource)
        when (resource.resourceType) {
            "Immunization" -> immunization(fields)
            "AllergyIntolerance" -> allergy(fields)
            "Condition" -> condition(fields)
            "MedicationOrder" -> medicationOrder(fields)
            "MedicationStatement" -> medicationStatement(fields)
            "Observation" -> observation(fields)
            "Procedure" -> procedure(fields)
            "Medication" -> medication(fields)
            "Patient" -> patient(fields)
            "Practitioner" -> practitioner(fields)
            "Organization", "Location" -> fields.wrap("type")
            "Encounter" -> encounter(fields)
            else -> return null
        }
        fields.remove("text")
        fields.obj("meta")?.let { meta -> fields.putOrRemove("meta", meta.without("profile")) }
        // A contained resource is converted like any other; one of a type with no mapping is dropped.
        fields.array("contained")?.let { contained ->
            fields.putOrRemove("contained", JsonArray(contained.filterIsInstance<JsonObject>().mapNotNull(::convertAny)))
        }
        return pruned(JsonObject(fields)) as? JsonObject
    }

    private fun immunization(f: Fields) {
        f.rename("date", "occurrenceDateTime")
        val notGiven = f.removeBoolean("wasNotGiven") == true
        f["status"] = JsonPrimitive(
            when {
                notGiven -> "not-done"
                f.string("status") == "entered-in-error" -> "entered-in-error"
                f.string("status") in setOf("on-hold", "stopped") -> "not-done"
                else -> "completed"
            },
        )
        f.removeBoolean("reported")?.let { f["primarySource"] = JsonPrimitive(!it) }
        f.remove("requester")
        (f.remove("performer") as? JsonObject)?.let { f["performer"] = JsonArray(listOf(buildJsonObject { put("actor", it) })) }
        (f.remove("explanation") as? JsonObject)?.let { explanation ->
            explanation["reason"]?.let { f["reasonCode"] = it }
            (explanation["reasonNotGiven"] as? JsonArray)?.firstOrNull()?.let { f["statusReason"] = it }
        }
        // R4 needs the dose number wherever a protocol is given.
        f.array("vaccinationProtocol")?.let { protocols ->
            val applied = protocols.filterIsInstance<JsonObject>().mapNotNull { protocol ->
                val dose = protocol.primitive("doseSequence") ?: return@mapNotNull null
                buildJsonObject {
                    protocol["series"]?.let { put("series", it) }
                    protocol["authority"]?.let { put("authority", it) }
                    protocol["targetDisease"]?.let { put("targetDisease", it) }
                    put("doseNumberPositiveInt", dose)
                    protocol.primitive("seriesDoses")?.let { put("seriesDosesPositiveInt", it) }
                }
            }
            f.remove("vaccinationProtocol")
            f.putOrRemove("protocolApplied", JsonArray(applied))
        }
    }

    private fun allergy(f: Fields) {
        f.rename("substance", "code")
        f.rename("onset", "onsetDateTime")
        f.rename("reporter", "asserter")
        f.rename("lastOccurence", "lastOccurrence")
        // One status became two: how active the allergy is, and how sure the record is.
        val (clinical, verification) = when (f.removeString("status")) {
            "unconfirmed" -> "active" to "unconfirmed"
            "confirmed" -> "active" to "confirmed"
            "inactive" -> "inactive" to null
            "resolved" -> "resolved" to null
            "refuted" -> "inactive" to "refuted"
            "entered-in-error" -> null to "entered-in-error"
            else -> "active" to null
        }
        clinical?.let { f["clinicalStatus"] = coded(AllergyClinical, it) }
        verification?.let { f["verificationStatus"] = coded(AllergyVerification, it) }
        f.removeString("category")?.takeIf { it in AllergyCategories }?.let { f["category"] = JsonArray(listOf(JsonPrimitive(it))) }
        f.removeString("criticality")?.let { Criticality[it] }?.let { f["criticality"] = JsonPrimitive(it) }
        f.wrap("note")
        f.array("reaction")?.let { reactions ->
            f["reaction"] = JsonArray(
                reactions.filterIsInstance<JsonObject>().map { reaction -> Fields(reaction).apply { remove("certainty"); wrap("note") }.let(::JsonObject) },
            )
        }
    }

    private fun condition(f: Fields) {
        f.rename("patient", "subject")
        f.rename("dateRecorded", "recordedDate")
        val verification = f.removeString("verificationStatus")?.takeIf { it != "unknown" }
        val clinical = f.removeString("clinicalStatus")?.let { if (it == "relapse") "recurrence" else it }
        verification?.let { f["verificationStatus"] = coded(ConditionVerification, it) }
        // R4 allows no clinical status on a record entered in error.
        clinical?.takeIf { verification != "entered-in-error" }?.let { f["clinicalStatus"] = coded(ConditionClinical, it) }
        f.wrap("category")
        f.noteFromString("notes")
    }

    private fun medicationOrder(f: Fields) {
        f["resourceType"] = JsonPrimitive("MedicationRequest")
        f["intent"] = JsonPrimitive("order")
        f.rename("dateWritten", "authoredOn")
        f.rename("patient", "subject")
        f.rename("prescriber", "requester")
        f.rename("reasonEnded", "statusReason")
        f.remove("dateEnded")
        f.wrap("reasonCodeableConcept", "reasonCode")
        f.wrap("reasonReference")
        f.noteFromString("note")
        f.dosages("dosageInstruction")
        f.obj("dispenseRequest")?.let { f.putOrRemove("dispenseRequest", it.without("medicationCodeableConcept", "medicationReference")) }
        (f.remove("substitution") as? JsonObject)?.let { substitution ->
            val type = substitution["type"] ?: return@let
            f["substitution"] = buildJsonObject {
                put("allowedCodeableConcept", type)
                substitution["reason"]?.let { put("reason", it) }
            }
        }
    }

    private fun medicationStatement(f: Fields) {
        f.rename("patient", "subject")
        if (f.removeBoolean("wasNotTaken") == true) f["status"] = JsonPrimitive("not-taken")
        f.rename("reasonNotTaken", "statusReason")
        f.wrap("reasonForUseCodeableConcept", "reasonCode")
        f.wrap("reasonForUseReference", "reasonReference")
        f.rename("supportingInformation", "derivedFrom")
        f.noteFromString("note")
        f.dosages("dosage")
    }

    private fun observation(f: Fields) {
        // Health Connect sorts an Observation by its category, in R4's code system.
        val categories = f.array("category")?.filterIsInstance<JsonObject>() ?: listOfNotNull(f.obj("category"))
        f.putOrRemove("category", JsonArray(categories.map(::r4Category)))
        f.noteFromString("comments")
        f.wrap("interpretation")
        f.array("referenceRange")?.let { ranges ->
            f["referenceRange"] = JsonArray(ranges.filterIsInstance<JsonObject>().map { range -> Fields(range).apply { rename("meaning", "type") }.let(::JsonObject) })
        }
        (f.remove("related") as? JsonArray)?.filterIsInstance<JsonObject>()?.let { related ->
            val (members, others) = related.partition { it.string("type") == "has-member" }
            f.putOrRemove("hasMember", JsonArray(members.mapNotNull { it["target"] }))
            f.putOrRemove("derivedFrom", JsonArray(others.mapNotNull { it["target"] }))
        }
    }

    private fun procedure(f: Fields) {
        val notDone = f.removeBoolean("notPerformed") == true
        when {
            notDone -> f["status"] = JsonPrimitive("not-done")
            f.string("status") == "aborted" -> f["status"] = JsonPrimitive("stopped")
        }
        (f.remove("reasonNotPerformed") as? JsonArray)?.firstOrNull()?.let { f["statusReason"] = it }
        f.wrap("reasonCodeableConcept", "reasonCode")
        f.wrap("reasonReference")
        f.wrap("request", "basedOn")
        f.rename("notes", "note")
        f.rename("used", "usedReference")
        f.array("performer")?.let { performers ->
            f["performer"] = JsonArray(performers.filterIsInstance<JsonObject>().map { performer -> Fields(performer).apply { rename("role", "function") }.let(::JsonObject) })
        }
    }

    private fun r4Category(category: JsonObject): JsonObject {
        val codings = category.objects("coding").map { coding ->
            if (coding.string("system") == OldObservationCategories) coding.with("system", JsonPrimitive(ObservationCategories)) else coding
        }
        return if (codings.isEmpty()) category else category.with("coding", JsonArray(codings))
    }

    /** The class became a coding in v3 ActCode; a class with no code there is dropped. */
    private fun encounter(f: Fields) {
        f.rename("patient", "subject")
        f.rename("incomingReferral", "basedOn")
        f.rename("reason", "reasonCode")
        f.rename("indication", "reasonReference")
        f.wrap("appointment")
        (f["class"] as? JsonPrimitive)?.content?.let { code ->
            val mapped = EncounterClasses[code]
            if (mapped == null) f.remove("class") else f["class"] = buildJsonObject { put("system", ActCode); put("code", mapped) }
        }
        f.obj("hospitalization")?.let { f.putOrRemove("hospitalization", it.without("admittingDiagnosis", "dischargeDiagnosis")) }
    }

    private fun medication(f: Fields) {
        (f.remove("product") as? JsonObject)?.let { product ->
            product["form"]?.let { f["form"] = it }
            val ingredients = product.objects("ingredient").mapNotNull { ingredient ->
                val item = ingredient["item"] ?: return@mapNotNull null
                buildJsonObject {
                    put("itemReference", item)
                    ingredient["amount"]?.let { put("strength", it) }
                }
            }
            f.putOrRemove("ingredient", JsonArray(ingredients))
        }
        f.remove("isBrand")
        f.remove("package")
    }

    private fun patient(f: Fields) {
        f.rename("careProvider", "generalPractitioner")
        f.remove("animal")
        f.array("name")?.let { names -> f["name"] = JsonArray(names.filterIsInstance<JsonObject>().map(::humanName)) }
    }

    private fun practitioner(f: Fields) {
        f.obj("name")?.let { f["name"] = JsonArray(listOf(humanName(it))) }
        f.remove("practitionerRole")
    }

    /** DSTU2 held several family names; R4 holds one string. */
    private fun humanName(name: JsonObject): JsonObject {
        val family = (name["family"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }?.joinToString(" ") ?: return name
        return if (family.isBlank()) name.without("family") else name.with("family", JsonPrimitive(family))
    }

    /** A dosage's dose and rate moved into `doseAndRate`, and a few fields were renamed. */
    private fun Fields.dosages(key: String) {
        val dosages = array(key) ?: return
        this[key] = JsonArray(
            dosages.filterIsInstance<JsonObject>().map { dosage ->
                val d = Fields(dosage)
                d.wrap("additionalInstructions", "additionalInstruction")
                d.rename("siteCodeableConcept", "site")
                d.remove("siteReference")
                d.rename("quantityQuantity", "doseQuantity")
                d.rename("quantityRange", "doseRange")
                val doseAndRate = buildJsonObject {
                    for (part in DoseAndRateParts) d.remove(part)?.let { put(part, it) }
                }
                if (doseAndRate.isNotEmpty()) d["doseAndRate"] = JsonArray(listOf(doseAndRate))
                d.obj("timing")?.obj("repeat")?.let { repeat ->
                    val r = Fields(repeat)
                    r.rename("periodUnits", "periodUnit")
                    r.rename("durationUnits", "durationUnit")
                    r.rename("boundsQuantity", "boundsDuration")
                    (r["when"] as? JsonPrimitive)?.let { r["when"] = JsonArray(listOf(it)) }
                    d["timing"] = d.obj("timing")!!.with("repeat", JsonObject(r))
                }
                JsonObject(d)
            },
        )
    }

    private fun tagged(resource: JsonObject): JsonObject {
        val meta = resource.obj("meta") ?: JsonObject(emptyMap())
        val tag = buildJsonObject {
            put("system", OpenVitalsFhir.TagSystem)
            put("code", ConvertedTag)
            put("display", ConvertedDisplay)
        }
        return resource.with("meta", meta.with("tag", JsonArray(meta.objects("tag") + tag)))
    }

    /** The element without empty objects, empty arrays and nulls: Health Connect refuses them. */
    private fun pruned(element: JsonElement): JsonElement? = when (element) {
        is JsonNull -> null
        is JsonObject -> element.mapNotNull { (key, value) -> pruned(value)?.let { key to it } }.takeIf { it.isNotEmpty() }?.let { JsonObject(it.toMap()) }
        is JsonArray -> element.mapNotNull(::pruned).takeIf { it.isNotEmpty() }?.let(::JsonArray)
        else -> element
    }

    private fun coded(system: String, code: String): JsonObject = buildJsonObject {
        put("coding", JsonArray(listOf(buildJsonObject { put("system", system); put("code", code) })))
    }

    /** A resource's fields, open for change. */
    private class Fields(source: JsonObject) : LinkedHashMap<String, JsonElement>(source) {
        fun string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
        fun obj(key: String): JsonObject? = this[key] as? JsonObject
        fun array(key: String): JsonArray? = this[key] as? JsonArray
        fun removeString(key: String): String? = (remove(key) as? JsonPrimitive)?.takeIf { it.isString }?.content
        fun removeBoolean(key: String): Boolean? = (remove(key) as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

        fun rename(from: String, to: String) {
            remove(from)?.let { put(to, it) }
        }

        /** A single value that R4 holds as a list. */
        fun wrap(from: String, to: String = from) {
            val value = remove(from) ?: return
            put(to, value as? JsonArray ?: JsonArray(listOf(value)))
        }

        /** A plain text note as R4's list of annotations. */
        fun noteFromString(from: String) {
            val text = removeString(from)?.takeIf { it.isNotBlank() } ?: return
            put("note", JsonArray(listOf(buildJsonObject { put("text", text) })))
        }

        fun putOrRemove(key: String, value: JsonElement) {
            val empty = (value as? JsonArray)?.isEmpty() == true || (value as? JsonObject)?.isEmpty() == true
            if (empty) remove(key) else put(key, value)
        }
    }

    private const val AllergyClinical = "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical"
    private const val AllergyVerification = "http://terminology.hl7.org/CodeSystem/allergyintolerance-verification"
    private const val ConditionClinical = "http://terminology.hl7.org/CodeSystem/condition-clinical"
    private const val ConditionVerification = "http://terminology.hl7.org/CodeSystem/condition-ver-status"
    private const val ObservationCategories = "http://terminology.hl7.org/CodeSystem/observation-category"
    private const val OldObservationCategories = "http://hl7.org/fhir/observation-category"
    private const val ActCode = "http://terminology.hl7.org/CodeSystem/v3-ActCode"
    private val EncounterClasses = mapOf(
        "inpatient" to "IMP", "outpatient" to "AMB", "ambulatory" to "AMB", "emergency" to "EMER",
        "home" to "HH", "field" to "FLD", "daytime" to "SS", "virtual" to "VR",
    )
    private val Dstu2ProfileHints = listOf("/DSTU2/", "/1.0.2/", "fhir.dstu2")

    /** Per type, the fields DSTU2 had that R4 dropped or renamed. */
    private val Dstu2OnlyFields = mapOf(
        "Immunization" to setOf("date", "wasNotGiven", "reported", "explanation", "vaccinationProtocol"),
        "AllergyIntolerance" to setOf("substance", "status", "onset", "reporter", "lastOccurence"),
        "Condition" to setOf("patient", "dateRecorded", "notes"),
        "MedicationStatement" to setOf("patient", "wasNotTaken", "reasonNotTaken", "reasonForUseCodeableConcept", "reasonForUseReference", "supportingInformation"),
        "Observation" to setOf("comments", "related"),
        "Procedure" to setOf("notPerformed", "reasonNotPerformed", "reasonCodeableConcept", "request", "notes", "used"),
        "Medication" to setOf("product", "isBrand", "package"),
        "Patient" to setOf("careProvider", "animal"),
        "Practitioner" to setOf("practitionerRole"),
        "Encounter" to setOf("patient", "indication", "incomingReferral"),
        "DiagnosticReport" to setOf("request", "codedDiagnosis"),
    )
    private val AllergyCategories = setOf("food", "medication", "environment")
    private val Criticality = mapOf("CRITL" to "low", "CRITH" to "high", "CRITU" to "unable-to-assess")
    private val DoseAndRateParts = listOf("doseQuantity", "doseRange", "rateRatio", "rateRange", "rateQuantity")
}
