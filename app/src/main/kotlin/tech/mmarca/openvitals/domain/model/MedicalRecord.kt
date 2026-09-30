package tech.mmarca.openvitals.domain.model

import java.time.Instant

/** Points at one record: its data source, its FHIR resource type name, and its FHIR id. */
data class MedicalRecordRef(
    val dataSourceId: String,
    val resourceType: String,
    val resourceId: String,
)

/** One FHIR resource as Health Connect holds it. [json] is the resource exactly as stored. */
data class MedicalRecord(
    val ref: MedicalRecordRef,
    val category: MedicalCategory,
    val fhirVersion: String,
    val json: String,
)

/** Where records come from. The app that wrote it created it, and nobody can edit it. */
data class MedicalRecordSource(
    val id: String,
    val packageName: String,
    val fhirBaseUri: String,
    val displayName: String,
    val fhirVersion: String,
    val lastDataUpdate: Instant?,
)

/** One page of a category read. [remainingCount] is how many records follow this page. */
data class MedicalRecordPage(
    val records: List<MedicalRecord>,
    val nextPageToken: String?,
    val remainingCount: Int,
)
