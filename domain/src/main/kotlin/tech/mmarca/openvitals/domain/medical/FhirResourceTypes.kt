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

    /**
     * Health Connect's own Observation rules, from `MedicalResourceValidator` in the platform
     * (AOSP packages/modules/HealthFitness). Checked in this order: pregnancy by LOINC code
     * alone, social history and vital signs by LOINC code or category, lab results by category
     * alone. An Observation matching none is refused.
     */
    val PregnancyLoincCodes: Set<String> = setOf(
        "82810-3", "11636-8", "11637-6", "11638-4", "11639-2", "11640-0", "11612-9",
        "11613-7", "11614-5", "33065-4", "11778-8", "11779-6", "11780-4",
    )
    val SocialHistoryLoincCodes: Set<String> = setOf("74013-4", "72166-2")
    val VitalSignsLoincCodes: Set<String> = setOf(
        "85353-1", "9279-1", "8867-4", "2708-6", "8310-5", "8302-2", "9843-4",
        "29463-7", "39156-5", "85354-9", "8480-6", "8462-4",
    )
    const val ObservationCategorySystem = "http://terminology.hl7.org/CodeSystem/observation-category"

    /** The category Health Connect will assign. Null for a resource it would refuse to file. */
    fun likelyCategory(resource: JsonObject): MedicalCategory? = when (resource.resourceType) {
        "Immunization" -> MedicalCategory.VACCINES
        "AllergyIntolerance" -> MedicalCategory.ALLERGIES
        "Condition" -> MedicalCategory.CONDITIONS
        "Medication", "MedicationRequest", "MedicationStatement" -> MedicalCategory.MEDICATIONS
        "Procedure" -> MedicalCategory.PROCEDURES
        "Encounter", "Location", "Organization" -> MedicalCategory.VISITS
        "Patient" -> MedicalCategory.PERSONAL_DETAILS
        "Practitioner", "PractitionerRole" -> MedicalCategory.PRACTITIONER_DETAILS
        "Observation" -> observationCategory(resource)
        else -> null
    }

    private fun observationCategory(observation: JsonObject): MedicalCategory? {
        val loinc = observation.obj("code")?.codesOf(FhirPreflight.LoincSystem).orEmpty()
        val categories = observation.objects("category").flatMap { it.codesOf(ObservationCategorySystem) }
        return when {
            loinc.any { it in PregnancyLoincCodes } -> MedicalCategory.PREGNANCY
            loinc.any { it in SocialHistoryLoincCodes } || "social-history" in categories -> MedicalCategory.SOCIAL_HISTORY
            loinc.any { it in VitalSignsLoincCodes } || "vital-signs" in categories -> MedicalCategory.VITAL_SIGNS
            "laboratory" in categories -> MedicalCategory.LAB_RESULTS
            else -> null
        }
    }

    private fun JsonObject.codesOf(system: String): List<String> =
        objects("coding").filter { it.string("system") == system }.mapNotNull { it.string("code") }

    /** The category a resource type lands in. Null for Observation, which depends on its content. */
    fun categoryOfType(type: String): MedicalCategory? =
        likelyCategory(JsonObject(mapOf("resourceType" to JsonPrimitive(type))))
}
