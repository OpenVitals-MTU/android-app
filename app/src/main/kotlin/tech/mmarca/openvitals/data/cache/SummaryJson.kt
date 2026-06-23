package tech.mmarca.openvitals.data.cache

import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal val SummaryJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal fun encodeObject(block: JsonObjectBuilder.() -> Unit): String =
    SummaryJson.encodeToString(JsonObject.serializer(), buildJsonObject(block))

internal fun decodeObject(payloadJson: String): JsonObject =
    SummaryJson.parseToJsonElement(payloadJson).jsonObject

internal fun <T> Iterable<T>.toJsonArray(transform: (T) -> JsonElement): JsonArray =
    buildJsonArray {
        this@toJsonArray.forEach { add(transform(it)) }
    }

internal fun Iterable<String>.toStringJsonArray(): JsonArray =
    sorted().toJsonArray { JsonPrimitive(it) }

internal fun JsonObject.string(name: String): String =
    getValue(name).jsonPrimitive.content

internal fun JsonObject.stringOrNull(name: String): String? =
    get(name)?.jsonPrimitive?.contentOrNull

internal fun JsonObject.long(name: String): Long =
    getValue(name).jsonPrimitive.content.toLong()

internal fun JsonObject.longOrNull(name: String): Long? =
    get(name)?.jsonPrimitive?.longOrNull

internal fun JsonObject.int(name: String): Int =
    getValue(name).jsonPrimitive.content.toInt()

internal fun JsonObject.intOrNull(name: String): Int? =
    get(name)?.jsonPrimitive?.intOrNull

internal fun JsonObject.double(name: String): Double =
    getValue(name).jsonPrimitive.content.toDouble()

internal fun JsonObject.doubleOrNull(name: String): Double? =
    get(name)?.jsonPrimitive?.doubleOrNull

internal fun JsonObject.boolean(name: String): Boolean =
    getValue(name).jsonPrimitive.booleanOrNull ?: false

internal fun JsonObject.instant(name: String): Instant =
    Instant.parse(string(name))

internal fun JsonObject.instantOrNull(name: String): Instant? =
    stringOrNull(name)?.let(Instant::parse)

internal fun JsonObject.localDate(name: String): LocalDate =
    LocalDate.parse(string(name))

internal fun JsonObject.array(name: String): JsonArray =
    get(name)?.jsonArray ?: JsonArray(emptyList())

internal fun JsonObject.obj(name: String): JsonObject =
    getValue(name).jsonObject

internal fun JsonObject.objOrNull(name: String): JsonObject? =
    get(name)?.takeUnless { it is JsonNull }?.jsonObject

internal fun JsonObject.stringSet(name: String): Set<String> =
    array(name).map { it.jsonPrimitive.content }.toSet()

internal fun JsonObjectBuilder.putNullable(name: String, value: String?) {
    if (value == null) put(name, JsonNull) else put(name, value)
}

internal fun JsonObjectBuilder.putNullable(name: String, value: Double?) {
    if (value == null) put(name, JsonNull) else put(name, value)
}

internal fun JsonObjectBuilder.putNullable(name: String, value: Long?) {
    if (value == null) put(name, JsonNull) else put(name, value)
}

internal fun JsonObjectBuilder.putNullable(name: String, value: Int?) {
    if (value == null) put(name, JsonNull) else put(name, value)
}

internal fun JsonObjectBuilder.putNullable(name: String, value: Boolean?) {
    if (value == null) put(name, JsonNull) else put(name, value)
}

internal fun JsonObjectBuilder.putNullable(name: String, value: JsonElement?) {
    put(name, value ?: JsonNull)
}
