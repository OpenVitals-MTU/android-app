package tech.mmarca.openvitals.domain.medical

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Why pre-flight holds a record back. Health Connect would refuse it for the same reason. */
enum class FhirRejection {
    UNSUPPORTED_TYPE,
    INVALID_ID,
    CONTAINED,

    /** An Observation with none of the three category codes and no LOINC code to classify it by. */
    UNCLASSIFIABLE_OBSERVATION,

    /** An empty object or array. [FhirPreflightProblem.field] names where. */
    EMPTY_VALUE,
}

data class FhirPreflightProblem(val reason: FhirRejection, val field: String? = null)

/**
 * The checks worth making before a write, because Health Connect's own reason would come
 * too late to show in the analyze step. The platform checks much more: primitives, complex
 * types, extensions and narratives. Its reason for those reaches the import report instead.
 */
object FhirPreflight {
    const val LoincSystem = "http://loinc.org"

    fun check(resource: PreparedResource): FhirPreflightProblem? {
        val json = resource.json
        return when {
            resource.type !in FhirResourceTypes.supported -> FhirPreflightProblem(FhirRejection.UNSUPPORTED_TYPE)
            !isValidFhirId(json.fhirId) -> FhirPreflightProblem(FhirRejection.INVALID_ID)
            "contained" in json -> FhirPreflightProblem(FhirRejection.CONTAINED)
            resource.type == "Observation" && !isClassifiable(json) ->
                FhirPreflightProblem(FhirRejection.UNCLASSIFIABLE_OBSERVATION)
            else -> firstEmpty(json, path = "")?.let { FhirPreflightProblem(FhirRejection.EMPTY_VALUE, it) }
        }
    }

    private fun isClassifiable(observation: JsonObject): Boolean =
        FhirResourceTypes.likelyCategory(observation) != null ||
            observation.obj("code")?.objects("coding").orEmpty().any { it.string("system") == LoincSystem }

    /** The path of the first empty object or array, such as `reaction.manifestation`. */
    private fun firstEmpty(element: JsonElement, path: String): String? = when (element) {
        is JsonObject -> if (element.isEmpty() && path.isNotEmpty()) {
            path
        } else {
            element.entries.firstNotNullOfOrNull { (key, value) -> firstEmpty(value, join(path, key)) }
        }
        is JsonArray -> if (element.isEmpty()) path else element.firstNotNullOfOrNull { firstEmpty(it, path) }
        else -> null
    }

    private fun join(path: String, key: String) = if (path.isEmpty()) key else "$path.$key"
}
