package tech.mmarca.openvitals.domain.medical

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.MedicalRecordRef

/**
 * Which records a category list shows as rows.
 *
 * Rows are events. Places, organisations and bare drug definitions show inside the
 * records that point at them, and become rows only when nothing in the list points at
 * them. References resolve inside a data source.
 */
object MedicalRecordRows {
    private val definitionTypes = setOf("Location", "Organization", "Medication")

    fun rows(records: List<MedicalRecord>): List<MedicalRecord> {
        val pointedAt = records.flatMapTo(mutableSetOf()) { record ->
            referencedRefs(record.ref.dataSourceId, parse(record.json))
        }
        return records.filter { it.ref.resourceType !in definitionTypes || it.ref !in pointedAt }
    }

    /** The `<type>/<id>` references in [resource], as refs in [dataSourceId]. Others are left out. */
    fun referencedRefs(dataSourceId: String, resource: JsonObject?): List<MedicalRecordRef> =
        resource?.let(::referenceStrings).orEmpty().mapNotNull { refOf(dataSourceId, it) }

    /** A relative `<type>/<id>` reference as a ref in [dataSourceId]. Null for any other shape. */
    fun refOf(dataSourceId: String, reference: String?): MedicalRecordRef? {
        val match = reference?.let(RelativeReference::matchEntire) ?: return null
        val (type, id) = match.destructured
        return MedicalRecordRef(dataSourceId, type, id)
    }

    private fun referenceStrings(element: JsonElement): List<String> = when (element) {
        is JsonObject -> listOfNotNull(element.string("reference")) + element.values.flatMap(::referenceStrings)
        is JsonArray -> element.flatMap(::referenceStrings)
        else -> emptyList()
    }

    private fun parse(json: String): JsonObject? = runCatching { Json.parseToJsonElement(json) as? JsonObject }.getOrNull()

    private val RelativeReference = Regex("^([A-Za-z]+)/([A-Za-z0-9\\-.]{1,64})$")
}
