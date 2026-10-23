package tech.mmarca.openvitals.domain.medical

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The results a DiagnosticReport holds inside itself, as records of their own. Health Connect
 * stores no report, only its results, which are Observations. Most reports point at results kept
 * as records of their own, and those need nothing here. Some hold their results as contained
 * resources, which would otherwise be left out with the report.
 *
 * Every contained Observation is lifted out, with what it points at among the report's other
 * contained resources when Health Connect stores that type; a reference to one it does not store
 * keeps only a display text. A result takes from its report what a contained resource leaves to
 * its container: the patient, the visit, the time, the performer, and, when the report is a lab
 * report, the laboratory category. A field the result has is never replaced. A lifted id is
 * `<report id>-<local id>`, as [FhirIdAssigner] names a record lifted from a parent it keeps.
 */
object DiagnosticReportResults {
    const val ReportType = "DiagnosticReport"

    /** The results [report] holds, each with the note the import report shows. Empty for any other type. */
    fun of(report: FhirEntry, scope: String): List<Pair<FhirEntry, FhirIdNote>> {
        val json = report.resource
        if (report.type != ReportType) return emptyList()
        // Each result with its report's context first, so what that context points at is lifted too.
        val contained = json.objects("contained").map { if (it.resourceType == "Observation") withReportContext(it, json) else it }
        if (contained.none { it.resourceType == "Observation" }) return emptyList()
        val byLocal = contained.mapNotNull { resource -> resource.fhirId?.let { "#$it" to resource } }.toMap()
        val reportId = json.fhirId?.takeIf(::isValidFhirId) ?: FhirIdAssigner.hashId(scope, ReportType, json)

        val lifted = LinkedHashMap<JsonObject, String>()
        val pending = ArrayDeque(contained.filter { it.resourceType == "Observation" })
        while (pending.isNotEmpty()) {
            val resource = pending.removeFirst()
            val type = resource.resourceType
            if (resource in lifted || type !in FhirResourceTypes.supported) continue
            val joined = resource.fhirId?.let { "$reportId-$it" }
            lifted[resource] = if (isValidFhirId(joined)) joined!! else FhirIdAssigner.hashId("$scope/$reportId", type!!, resource)
            localReferences(resource).mapNotNullTo(pending) { byLocal[it] }
        }

        val targets = lifted.mapNotNull { (resource, id) -> resource.fhirId?.let { "#$it" to "${resource.resourceType}/$id" } }.toMap()
        val reportLabel = json.obj("code")?.codeableLabel() ?: ReportType
        return lifted.map { (resource, id) ->
            val type = resource.resourceType!!
            val rewritten = rewrite(resource, targets, byLocal, reportLabel) as JsonObject
            report.copy(resource = rewritten.with("id", JsonPrimitive(id)), fullUrl = null) to
                FhirIdNote.ContainedLifted(ReportType, json.fhirId ?: reportId, type, id)
        }
    }

    /** What the result leaves to its report. */
    private fun withReportContext(result: JsonObject, report: JsonObject): JsonObject {
        val fields = LinkedHashMap(result)
        fun inherit(key: String, value: JsonElement?) {
            if (value != null && key !in fields) fields[key] = value
        }
        inherit("subject", report["subject"])
        inherit("encounter", report["encounter"])
        if (fields.keys.none { it.startsWith("effective") }) {
            report.filterKeys { it.startsWith("effective") }.forEach { (key, value) -> fields[key] = value }
        }
        inherit("issued", report["issued"])
        // A DSTU2 report has one performer; an Observation has a list in both versions.
        inherit("performer", report["performer"]?.let { it as? JsonArray ?: JsonArray(listOf(it)) })
        if ("category" !in fields && isLabReport(report)) fields["category"] = LaboratoryCategory
        return JsonObject(fields)
    }

    /** A report in a lab section of HL7 table 0074, as DSTU2 and R4 lab reports name it. */
    private fun isLabReport(report: JsonObject): Boolean {
        val categories = (report["category"] as? JsonArray)?.filterIsInstance<JsonObject>() ?: listOfNotNull(report.obj("category"))
        return categories.flatMap { it.objects("coding") }.any { coding ->
            val code = coding.string("code")
            when (coding.string("system")) {
                null, Table0074, OldTable0074 -> code in LabSections
                ObservationCategories, OldObservationCategories -> code == "laboratory"
                else -> false
            }
        }
    }

    private fun localReferences(element: JsonElement): List<String> = when (element) {
        is JsonObject -> listOfNotNull(element.string("reference")?.takeIf { it.startsWith("#") && it != "#" }) + element.values.flatMap(::localReferences)
        is JsonArray -> element.flatMap(::localReferences)
        else -> emptyList()
    }

    /**
     * A local reference points at the lifted record. One to a resource left in the report, or to
     * the report itself, which Health Connect does not store, keeps only a display text.
     */
    private fun rewrite(element: JsonElement, targets: Map<String, String>, byLocal: Map<String, JsonObject>, reportLabel: String): JsonElement =
        when (element) {
            is JsonArray -> JsonArray(element.map { rewrite(it, targets, byLocal, reportLabel) })
            is JsonObject -> {
                val result = JsonObject(element.mapValues { (_, value) -> rewrite(value, targets, byLocal, reportLabel) })
                val reference = element.string("reference")?.takeIf { it.startsWith("#") }
                when {
                    reference == null -> result
                    reference in targets -> result.with("reference", JsonPrimitive(targets.getValue(reference)))
                    else -> {
                        val left = byLocal[reference]
                        // A specimen names itself by its type.
                        val display = if (left == null) reportLabel else (left.obj("code") ?: left.obj("type"))?.codeableLabel() ?: left.string("name") ?: left.resourceType
                        result.without("reference").let { if (it.string("display") != null || display == null) it else it.with("display", JsonPrimitive(display)) }
                    }
                }
            }
            else -> element
        }

    private const val Table0074 = "http://terminology.hl7.org/CodeSystem/v2-0074"
    private const val OldTable0074 = "http://hl7.org/fhir/v2/0074"
    private const val ObservationCategories = "http://terminology.hl7.org/CodeSystem/observation-category"
    private const val OldObservationCategories = "http://hl7.org/fhir/observation-category"

    /** The lab sections of HL7 table 0074: the laboratory as a whole, and its departments. */
    private val LabSections = setOf("LAB", "BG", "BLB", "CG", "CH", "CP", "GE", "HM", "IMM", "MB", "MCB", "MYC", "OSL", "SP", "SR", "TX", "URN", "VR")

    private val LaboratoryCategory = JsonArray(
        listOf(
            buildJsonObject {
                put("coding", JsonArray(listOf(buildJsonObject { put("system", ObservationCategories); put("code", "laboratory") })))
            },
        ),
    )
}
