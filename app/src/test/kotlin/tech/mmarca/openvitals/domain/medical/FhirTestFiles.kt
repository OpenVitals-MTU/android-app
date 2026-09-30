package tech.mmarca.openvitals.domain.medical

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** Fixture files and small builders for the FHIR core tests. */
internal object FhirTestFiles {
    fun text(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("fhir/$name")) { "missing fixture fhir/$name" }.readText()

    fun parsed(name: String): FhirFile =
        (FhirFileParser.parse(text(name)) as FhirParseResult.Parsed).file

    fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    fun entry(text: String, fullUrl: String? = null, origin: FhirOrigin? = null) =
        FhirEntry(json(text), fullUrl = fullUrl, origin = origin)

    /** Every `reference` string anywhere in the resource. */
    fun references(element: JsonElement): List<String> = when (element) {
        is JsonObject -> listOfNotNull(element.string("reference")) + element.values.flatMap(::references)
        is JsonArray -> element.flatMap(::references)
        else -> emptyList()
    }
}
