package tech.mmarca.openvitals.domain.medical

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import tech.mmarca.openvitals.domain.model.MedicalCategory

/** The FHIR resource types Health Connect accepts, and the category each lands in. */
object FhirResourceTypes {
    val supported: Set<String> = setOf(
        "Immunization", "AllergyIntolerance", "Condition", "Medication", "MedicationRequest",
        "MedicationStatement", "Observation", "Procedure", "Encounter", "Location", "Organization",
        "Patient", "Practitioner", "PractitionerRole",
    )

    /** The three Observation category codes Health Connect names. */
    private val observationCategories = mapOf(
        "laboratory" to MedicalCategory.LAB_RESULTS,
        "vital-signs" to MedicalCategory.VITAL_SIGNS,
        "social-history" to MedicalCategory.SOCIAL_HISTORY,
    )

    /**
     * The category Health Connect will most likely assign. Null for an Observation it
     * can only classify by LOINC code: the platform's LOINC lists are not published.
     */
    fun likelyCategory(resource: JsonObject): MedicalCategory? = when (resource.resourceType) {
        "Immunization" -> MedicalCategory.VACCINES
        "AllergyIntolerance" -> MedicalCategory.ALLERGIES
        "Condition" -> MedicalCategory.CONDITIONS
        "Medication", "MedicationRequest", "MedicationStatement" -> MedicalCategory.MEDICATIONS
        "Procedure" -> MedicalCategory.PROCEDURES
        "Encounter", "Location", "Organization" -> MedicalCategory.VISITS
        "Patient" -> MedicalCategory.PERSONAL_DETAILS
        "Practitioner", "PractitionerRole" -> MedicalCategory.PRACTITIONER_DETAILS
        "Observation" -> observationCategoryCodes(resource).firstNotNullOfOrNull { observationCategories[it] }
        else -> null
    }

    /** The category a resource type lands in. Null for Observation, which depends on its content. */
    fun categoryOfType(type: String): MedicalCategory? =
        likelyCategory(JsonObject(mapOf("resourceType" to JsonPrimitive(type))))

    internal fun observationCategoryCodes(resource: JsonObject): List<String> =
        resource.objects("category").flatMap { it.objects("coding") }.mapNotNull { it.string("code") }
}
