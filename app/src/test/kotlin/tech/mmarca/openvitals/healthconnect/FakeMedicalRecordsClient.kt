package tech.mmarca.openvitals.healthconnect

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.MedicalRecordPage
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

/**
 * Health Connect's medical records in memory.
 *
 * It enforces the documented write checks only: JSON, a supported type, the id rule,
 * no `contained`, a classifiable Observation, and one transaction per upsert. The
 * deeper checks a real phone runs are not modelled, so a pass here says nothing about them.
 */
internal class FakeMedicalRecordsClient(
    private val packageName: String = OWN_PACKAGE,
) : MedicalRecordsClient {

    /** Categories whose reads throw, as a missing read permission would. */
    var deniedCategories: Set<MedicalCategory> = emptySet()

    /** Thrown by every write, as a revoked permission would be. */
    var failWrites: Throwable? = null

    /** Every call, by name, in order. */
    val calls = mutableListOf<String>()

    private val sources = linkedMapOf<String, MedicalRecordSource>()
    private val records = linkedMapOf<MedicalRecordRef, MedicalRecord>()
    private val pages = mutableMapOf<String, List<MedicalRecord>>()
    private var nextId = 1

    /** A source another app wrote. */
    fun addForeignSource(packageName: String, fhirBaseUri: String, displayName: String): MedicalRecordSource =
        MedicalRecordSource("source-${nextId++}", packageName, fhirBaseUri, displayName, "4.0.1", null)
            .also { sources[it.id] = it }

    override suspend fun dataSources(packageNames: List<String>): List<MedicalRecordSource> {
        calls += "dataSources"
        return sources.values.filter { packageNames.isEmpty() || it.packageName in packageNames }
    }

    override suspend fun createDataSource(
        fhirBaseUri: String,
        displayName: String,
        fhirVersion: String,
    ): MedicalRecordSource {
        calls += "createDataSource"
        require(own().none { it.displayName == displayName }) { "Display name already in use" }
        return MedicalRecordSource("source-${nextId++}", packageName, fhirBaseUri, displayName, fhirVersion, null)
            .also { sources[it.id] = it }
    }

    override suspend fun readPage(
        category: MedicalCategory,
        sourceIds: Set<String>,
        pageSize: Int,
        pageToken: String?,
    ): MedicalRecordPage {
        calls += "readPage"
        require(pageSize in 1..MAX_PAGE_SIZE) { "Page size out of range" }
        if (category in deniedCategories) throw SecurityException("Caller doesn't have permission")
        val matched = if (pageToken == null) {
            records.values.filter { it.category == category && (sourceIds.isEmpty() || it.ref.dataSourceId in sourceIds) }
        } else {
            requireNotNull(pages[pageToken]) { "Unknown page token" }
        }
        val offset = pageToken?.substringAfterLast(':')?.toInt() ?: 0
        val page = matched.drop(offset).take(pageSize)
        val nextOffset = offset + page.size
        val nextToken = if (nextOffset < matched.size) "page:${nextId++}:$nextOffset".also { pages[it] = matched } else null
        return MedicalRecordPage(page, nextToken, matched.size - nextOffset)
    }

    override suspend fun readByIds(refs: List<MedicalRecordRef>): List<MedicalRecord> {
        calls += "readByIds"
        return refs.mapNotNull { records[it] }
    }

    override suspend fun upsert(dataSourceId: String, fhirVersion: String, jsons: List<String>): List<MedicalRecord> {
        calls += "upsert"
        failWrites?.let { throw it }
        // Validate everything first: one bad record writes nothing.
        val built = jsons.map { build(dataSourceId, fhirVersion, it) }
        require(built.map { it.ref }.toSet().size == built.size) { "Duplicate resource in one request" }
        built.forEach { records[it.ref] = it }
        return built
    }

    override suspend fun delete(refs: List<MedicalRecordRef>) {
        calls += "delete"
        refs.filter { sources[it.dataSourceId]?.packageName == packageName }.forEach { records.remove(it) }
    }

    override suspend fun deleteDataSource(id: String) {
        calls += "deleteDataSource"
        val source = requireNotNull(sources[id]) { "No data source $id" }
        if (source.packageName != packageName) throw SecurityException("Not this app's data source")
        sources.remove(id)
        records.values.removeAll { it.ref.dataSourceId == id }
    }

    private fun own() = sources.values.filter { it.packageName == packageName }

    private fun build(dataSourceId: String, fhirVersion: String, data: String): MedicalRecord {
        val source = requireNotNull(sources[dataSourceId]) { "No data source $dataSourceId" }
        require(source.packageName == packageName) { "Not this app's data source" }
        val json = runCatching { Json.parseToJsonElement(data).jsonObject }
            .getOrElse { throw IllegalArgumentException("Invalid JSON") }
        val type = json["resourceType"]?.jsonPrimitive?.content
        require(type in SUPPORTED_TYPES) { "Unsupported resource type $type" }
        val id = json["id"]?.jsonPrimitive?.content
        require(id != null && FHIR_ID.matches(id)) { "Invalid resource id $id" }
        require("contained" !in json) { "Contained resources are not supported" }
        val category = requireNotNull(category(type!!, json)) { "Cannot classify $type/$id" }
        return MedicalRecord(MedicalRecordRef(dataSourceId, type, id), category, fhirVersion, data)
    }

    private fun category(type: String, json: JsonObject): MedicalCategory? = when (type) {
        "Immunization" -> MedicalCategory.VACCINES
        "AllergyIntolerance" -> MedicalCategory.ALLERGIES
        "Condition" -> MedicalCategory.CONDITIONS
        "Medication", "MedicationRequest", "MedicationStatement" -> MedicalCategory.MEDICATIONS
        "Procedure" -> MedicalCategory.PROCEDURES
        "Encounter", "Location", "Organization" -> MedicalCategory.VISITS
        "Patient" -> MedicalCategory.PERSONAL_DETAILS
        "Practitioner", "PractitionerRole" -> MedicalCategory.PRACTITIONER_DETAILS
        "Observation" -> observationCategory(json)
        else -> null
    }

    private fun observationCategory(json: JsonObject): MedicalCategory? {
        val codes = json["category"]?.jsonArray.orEmpty()
            .flatMap { it.jsonObject["coding"]?.jsonArray.orEmpty() }
            .mapNotNull { it.jsonObject["code"]?.jsonPrimitive?.content }
        return when {
            "laboratory" in codes -> MedicalCategory.LAB_RESULTS
            "vital-signs" in codes -> MedicalCategory.VITAL_SIGNS
            "social-history" in codes -> MedicalCategory.SOCIAL_HISTORY
            else -> null
        }
    }

    companion object {
        const val OWN_PACKAGE = "tech.mmarca.openvitals"
        const val MAX_PAGE_SIZE = 5000

        private val FHIR_ID = Regex("[A-Za-z0-9\\-.]{1,64}")

        private val SUPPORTED_TYPES = setOf(
            "Immunization", "AllergyIntolerance", "Observation", "Condition", "Procedure", "Medication",
            "MedicationRequest", "MedicationStatement", "Patient", "Practitioner", "PractitionerRole",
            "Encounter", "Location", "Organization",
        )
    }
}
