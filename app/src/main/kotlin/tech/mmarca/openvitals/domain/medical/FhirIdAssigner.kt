package tech.mmarca.openvitals.domain.medical

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** A resource ready for pre-flight: it has a valid id, no contained resources, and rewritten references. */
data class PreparedResource(val type: String, val id: String, val json: JsonObject)

/** What the id assigner changed, for the import report. */
sealed interface FhirIdNote {
    data class IdAssigned(val type: String, val previousId: String?, val id: String) : FhirIdNote
    data class ContainedLifted(val parentType: String, val parentId: String, val type: String, val id: String) : FhirIdNote
    data class ContainedDropped(val parentType: String, val parentId: String, val type: String) : FhirIdNote

    /** Two different resources with one type and id: the later one is kept. */
    data class Duplicate(val type: String, val id: String) : FhirIdNote
}

data class FhirIdResult(val resources: List<PreparedResource>, val notes: List<FhirIdNote>)

/**
 * Gives every resource of one source group a valid id, lifts contained resources to the
 * top level, and rewrites references so each still points at the right record.
 *
 * - A valid id is kept, so re-imports stay idempotent.
 * - An entry with no id takes the one its fullUrl names: the UUID of a `urn:uuid:` URL,
 *   or the id at the end of `<base>/<type>/<id>`.
 * - Any other missing or invalid id becomes `ov-` and 40 hex digits of a SHA-256 over
 *   [scope], the type, and the resource with keys sorted and `id` and `meta` removed.
 *   The same file always yields the same ids.
 * - A contained resource becomes `<parent id>-<local id>`, or a hash when that breaks the
 *   id rule. One of a type Health Connect refuses is dropped, and references to it keep
 *   only a display text.
 *
 * It never leaves an empty object or array: Health Connect refuses both.
 */
object FhirIdAssigner {

    /** [entries] must all be of a supported type. [scope] is the group's base URI. */
    fun assign(entries: List<FhirEntry>, scope: String): FhirIdResult {
        val notes = mutableListOf<FhirIdNote>()
        val targets = mutableMapOf<String, String>()

        val tops = entries.map { entry ->
            val type = requireNotNull(entry.type)
            val previous = entry.resource.fhirId
            val id = when {
                isValidFhirId(previous) -> previous!!
                previous == null && entry.fullUrl != null && UrnUuid.matches(entry.fullUrl) ->
                    entry.fullUrl.substring(UrnUuidPrefix.length)
                previous == null && entry.fullUrl != null && FhirSourceGrouper.idOf(entry.fullUrl, type) != null ->
                    FhirSourceGrouper.idOf(entry.fullUrl, type)!!
                else -> hashId(scope, type, entry.resource)
            }
            if (id != previous) notes += FhirIdNote.IdAssigned(type, previous, id)
            entry.fullUrl?.let { targets[it] = "$type/$id" }
            if (previous != null && previous != id) targets["$type/$previous"] = "$type/$id"
            Top(type, id, entry.resource)
        }

        val prepared = tops.flatMap { top -> prepare(top, scope, targets, notes) }
        return FhirIdResult(dropDuplicates(prepared, notes), notes)
    }

    private data class Top(val type: String, val id: String, val resource: JsonObject)

    /** Where a local `#id` reference now points: a lifted resource, or nowhere, with a display to keep. */
    private sealed interface Local {
        data class Lifted(val reference: String) : Local
        data class Dropped(val display: String) : Local
    }

    private fun prepare(
        top: Top,
        scope: String,
        targets: Map<String, String>,
        notes: MutableList<FhirIdNote>,
    ): List<PreparedResource> {
        val locals = mutableMapOf<String, Local>()
        val lifted = mutableListOf<Top>()
        top.resource.objects("contained").forEach { contained ->
            val type = contained.resourceType ?: return@forEach
            val localId = contained.fhirId
            if (type in FhirResourceTypes.supported) {
                val joined = localId?.let { "${top.id}-$it" }
                val id = if (isValidFhirId(joined)) joined!! else hashId(scope, type, contained)
                lifted += Top(type, id, contained)
                notes += FhirIdNote.ContainedLifted(top.type, top.id, type, id)
                if (localId != null) locals["#$localId"] = Local.Lifted("$type/$id")
            } else {
                notes += FhirIdNote.ContainedDropped(top.type, top.id, type)
                val display = contained.obj("code")?.codeableLabel() ?: contained.string("name") ?: type
                if (localId != null) locals["#$localId"] = Local.Dropped(display)
            }
        }
        val container = "${top.type}/${top.id}"
        return (listOf(top) + lifted).map { resource ->
            val rewritten = rewrite(resource.resource.without("contained"), targets, locals, container) as JsonObject
            PreparedResource(resource.type, resource.id, rewritten.withId(resource.id))
        }
    }

    private fun rewrite(
        element: JsonElement,
        targets: Map<String, String>,
        locals: Map<String, Local>,
        container: String,
    ): JsonElement = when (element) {
        is JsonArray -> JsonArray(element.map { rewrite(it, targets, locals, container) })
        is JsonObject -> {
            var result = JsonObject(element.mapValues { (_, value) -> rewrite(value, targets, locals, container) })
            val reference = element.string("reference")
            if (reference != null) {
                val target: Local? = when {
                    reference == "#" -> Local.Lifted(container)
                    reference.startsWith("#") -> locals[reference]
                    else -> (targets[reference] ?: targets[reference.substringBefore("/_history/")])?.let(Local::Lifted)
                }
                result = when (target) {
                    is Local.Lifted -> result.with("reference", JsonPrimitive(target.reference))
                    is Local.Dropped -> result.without("reference").let { withoutReference ->
                        if (withoutReference.string("display") != null) {
                            withoutReference
                        } else {
                            withoutReference.with("display", JsonPrimitive(target.display))
                        }
                    }
                    null -> result
                }
            }
            result
        }
        else -> element
    }

    private fun dropDuplicates(prepared: List<PreparedResource>, notes: MutableList<FhirIdNote>): List<PreparedResource> {
        val kept = LinkedHashMap<Pair<String, String>, PreparedResource>()
        prepared.forEach { resource ->
            val key = resource.type to resource.id
            val earlier = kept.remove(key)
            if (earlier != null && earlier.json.canonical() != resource.json.canonical()) {
                notes += FhirIdNote.Duplicate(resource.type, resource.id)
            }
            kept[key] = resource
        }
        return kept.values.toList()
    }

    internal fun hashId(scope: String, type: String, resource: JsonObject): String =
        "ov-" + sha256Hex("$scope\n$type\n${resource.without("id", "meta").canonical()}").take(HashChars)

    /** `id` goes right after `resourceType`, where people look for it. */
    private fun JsonObject.withId(id: String): JsonObject {
        val ordered = LinkedHashMap<String, JsonElement>()
        this["resourceType"]?.let { ordered["resourceType"] = it }
        ordered["id"] = JsonPrimitive(id)
        forEach { (key, value) -> if (key != "resourceType" && key != "id") ordered[key] = value }
        return JsonObject(ordered)
    }

    private const val UrnUuidPrefix = "urn:uuid:"
    private val UrnUuid = Regex("^urn:uuid:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private const val HashChars = 40
}
