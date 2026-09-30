@file:OptIn(ExperimentalPersonalHealthRecordApi::class)

package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.FhirResource
import androidx.health.connect.client.records.MedicalResource
import tech.mmarca.openvitals.core.presentation.FeatureUnavailableException
import tech.mmarca.openvitals.domain.model.MedicalCategory

/** Health Connect's ids for medical categories, FHIR types and permissions. Nothing above this layer sees them. */
internal object MedicalCategoryMapping {
    const val FEATURE: Int = HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD

    const val WRITE_PERMISSION: String = HealthPermission.PERMISSION_WRITE_MEDICAL_DATA

    /** The twelve category reads and the one write. */
    val allPermissions: Set<String> =
        MedicalCategory.entries.mapTo(linkedSetOf(), ::readPermission) + WRITE_PERMISSION

    fun readPermission(category: MedicalCategory): String = when (category) {
        MedicalCategory.VACCINES -> HealthPermission.PERMISSION_READ_MEDICAL_DATA_VACCINES
        MedicalCategory.ALLERGIES -> HealthPermission.PERMISSION_READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES
        MedicalCategory.CONDITIONS -> HealthPermission.PERMISSION_READ_MEDICAL_DATA_CONDITIONS
        MedicalCategory.MEDICATIONS -> HealthPermission.PERMISSION_READ_MEDICAL_DATA_MEDICATIONS
        MedicalCategory.LAB_RESULTS -> HealthPermission.PERMISSION_READ_MEDICAL_DATA_LABORATORY_RESULTS
        MedicalCategory.PROCEDURES -> HealthPermission.PERMISSION_READ_MEDICAL_DATA_PROCEDURES
        MedicalCategory.VISITS -> HealthPermission.PERMISSION_READ_MEDICAL_DATA_VISITS
        MedicalCategory.VITAL_SIGNS -> HealthPermission.PERMISSION_READ_MEDICAL_DATA_VITAL_SIGNS
        MedicalCategory.PREGNANCY -> HealthPermission.PERMISSION_READ_MEDICAL_DATA_PREGNANCY
        MedicalCategory.SOCIAL_HISTORY -> HealthPermission.PERMISSION_READ_MEDICAL_DATA_SOCIAL_HISTORY
        MedicalCategory.PERSONAL_DETAILS -> HealthPermission.PERMISSION_READ_MEDICAL_DATA_PERSONAL_DETAILS
        MedicalCategory.PRACTITIONER_DETAILS -> HealthPermission.PERMISSION_READ_MEDICAL_DATA_PRACTITIONER_DETAILS
    }

    fun resourceType(category: MedicalCategory): Int = when (category) {
        MedicalCategory.VACCINES -> MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES
        MedicalCategory.ALLERGIES -> MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES
        MedicalCategory.CONDITIONS -> MedicalResource.MEDICAL_RESOURCE_TYPE_CONDITIONS
        MedicalCategory.MEDICATIONS -> MedicalResource.MEDICAL_RESOURCE_TYPE_MEDICATIONS
        MedicalCategory.LAB_RESULTS -> MedicalResource.MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS
        MedicalCategory.PROCEDURES -> MedicalResource.MEDICAL_RESOURCE_TYPE_PROCEDURES
        MedicalCategory.VISITS -> MedicalResource.MEDICAL_RESOURCE_TYPE_VISITS
        MedicalCategory.VITAL_SIGNS -> MedicalResource.MEDICAL_RESOURCE_TYPE_VITAL_SIGNS
        MedicalCategory.PREGNANCY -> MedicalResource.MEDICAL_RESOURCE_TYPE_PREGNANCY
        MedicalCategory.SOCIAL_HISTORY -> MedicalResource.MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY
        MedicalCategory.PERSONAL_DETAILS -> MedicalResource.MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS
        MedicalCategory.PRACTITIONER_DETAILS -> MedicalResource.MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS
    }

    /** Null for a category a newer Health Connect adds. */
    fun category(resourceType: Int): MedicalCategory? =
        MedicalCategory.entries.firstOrNull { resourceType(it) == resourceType }

    private val fhirTypes: Map<String, Int> = mapOf(
        "Immunization" to FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION,
        "AllergyIntolerance" to FhirResource.FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE,
        "Observation" to FhirResource.FHIR_RESOURCE_TYPE_OBSERVATION,
        "Condition" to FhirResource.FHIR_RESOURCE_TYPE_CONDITION,
        "Procedure" to FhirResource.FHIR_RESOURCE_TYPE_PROCEDURE,
        "Medication" to FhirResource.FHIR_RESOURCE_TYPE_MEDICATION,
        "MedicationRequest" to FhirResource.FHIR_RESOURCE_TYPE_MEDICATION_REQUEST,
        "MedicationStatement" to FhirResource.FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT,
        "Patient" to FhirResource.FHIR_RESOURCE_TYPE_PATIENT,
        "Practitioner" to FhirResource.FHIR_RESOURCE_TYPE_PRACTITIONER,
        "PractitionerRole" to FhirResource.FHIR_RESOURCE_TYPE_PRACTITIONER_ROLE,
        "Encounter" to FhirResource.FHIR_RESOURCE_TYPE_ENCOUNTER,
        "Location" to FhirResource.FHIR_RESOURCE_TYPE_LOCATION,
        "Organization" to FhirResource.FHIR_RESOURCE_TYPE_ORGANIZATION,
    )

    /** The FHIR name, such as "Immunization". Null for a type a newer Health Connect adds. */
    fun fhirTypeName(fhirType: Int): String? = fhirTypes.entries.firstOrNull { it.value == fhirType }?.key

    fun fhirTypeId(name: String): Int? = fhirTypes[name]
}

/** Throws what the platform throws, before any call is made. */
internal fun requireMedicalRecordsAvailable(isAvailable: () -> Boolean) {
    if (!isAvailable()) throw FeatureUnavailableException("Medical records are not available on this device")
}
