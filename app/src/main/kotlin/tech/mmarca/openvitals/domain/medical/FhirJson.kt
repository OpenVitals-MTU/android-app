package tech.mmarca.openvitals.domain.medical

import java.security.MessageDigest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Small readers over the FHIR JSON tree. Every field is optional: a missing or odd value reads as null. */
internal fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

internal fun JsonObject.primitive(key: String): JsonPrimitive? =
    (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.objects(key: String): List<JsonObject> =
    (this[key] as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()

internal fun JsonObject.strings(key: String): List<String> =
    (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }.orEmpty()

internal val JsonObject.resourceType: String? get() = string("resourceType")

internal val JsonObject.fhirId: String? get() = string("id")

/** A CodeableConcept's label: its text, else the first coding's display, else its code. */
internal fun JsonObject.codeableLabel(): String? =
    string("text")
        ?: objects("coding").firstNotNullOfOrNull { it.string("display") }
        ?: objects("coding").firstNotNullOfOrNull { it.string("code") }

/** The FHIR id rule Health Connect enforces. */
internal val FhirIdRule = Regex("[A-Za-z0-9\\-.]{1,64}")

internal fun isValidFhirId(id: String?): Boolean = id != null && FhirIdRule.matches(id)

/** JSON with object keys sorted at every level, so equal content always prints the same. */
internal fun JsonElement.canonical(): String = when (this) {
    is JsonObject -> entries.sortedBy { it.key }
        .joinToString(prefix = "{", postfix = "}", separator = ",") { (key, value) ->
            JsonPrimitive(key).toString() + ":" + value.canonical()
        }
    is JsonArray -> joinToString(prefix = "[", postfix = "]", separator = ",") { it.canonical() }
    else -> toString()
}

internal fun sha256Hex(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

/** A copy of this object with [key] set to [value], keeping the key order. */
internal fun JsonObject.with(key: String, value: JsonElement): JsonObject =
    JsonObject(LinkedHashMap(this).apply { put(key, value) })

internal fun JsonObject.without(vararg keys: String): JsonObject =
    JsonObject(LinkedHashMap(this).apply { keys.forEach { remove(it) } })
