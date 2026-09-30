package tech.mmarca.openvitals.data.repository.contract

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tech.mmarca.openvitals.domain.medical.FhirResourceTypes
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

/**
 * Medical records in memory, for the screens' tests. Knobs model the platform: a declined
 * category lists only this app's records, and with no access at all a read throws.
 */
class FakeMedicalRecordsRepository(
    var available: Boolean = true,
    var readable: Set<MedicalCategory> = MedicalCategory.entries.toSet(),
    var writable: Boolean = true,
    val sources: MutableList<MedicalRecordSource> = mutableListOf(),
    val records: MutableList<MedicalRecord> = mutableListOf(),
    private val ownPackage: String = "tech.mmarca.openvitals",
) : MedicalRecordsRepository {

    /** Categories whose count fails for a reason other than access. */
    var failingCategories: Set<MedicalCategory> = emptySet()

    fun addSource(
        id: String,
        name: String,
        packageName: String = ownPackage,
        baseUri: String = "https://$id.example/fhir",
        fhirVersion: String = "4.0.1",
    ) = MedicalRecordSource(id, packageName, baseUri, name, fhirVersion, null).also { sources += it }

    fun add(sourceId: String, type: String, id: String, json: String, category: MedicalCategory? = null) {
        val resolved = category ?: requireNotNull(FhirResourceTypes.categoryOfType(type)) { "Name the category for $type" }
        records += MedicalRecord(MedicalRecordRef(sourceId, type, id), resolved, "4.0.1", json)
    }

    override val permissions: Set<String> get() = if (available) setOf("medical") else emptySet()

    override val writePermissions: Set<String> get() = if (available) setOf("medical-write") else emptySet()

    override fun isAvailable(): Boolean = available

    override suspend fun grantedPermissions(): Set<String> = emptySet()

    override suspend fun readableCategories(): Set<MedicalCategory> = readable

    override suspend fun canWrite(): Boolean = writable

    override suspend fun count(category: MedicalCategory): Int {
        if (category in failingCategories) throw IllegalStateException("rate limited")
        return visible(category).size
    }

    override suspend fun readCategory(category: MedicalCategory, sourceIds: Set<String>): List<MedicalRecord> =
        visible(category).filter { sourceIds.isEmpty() || it.ref.dataSourceId in sourceIds }

    override suspend fun readRecords(refs: List<MedicalRecordRef>): List<MedicalRecord> =
        records.filter { it.ref in refs && canSee(it) }

    override suspend fun ownSources(): List<MedicalRecordSource> = sources.filter { it.packageName == ownPackage }

    override suspend fun allSources(): List<MedicalRecordSource> = sources.toList()

    override suspend fun createSource(fhirBaseUri: String, displayName: String, fhirVersion: String): MedicalRecordSource =
        MedicalRecordSource("s${sources.size + 1}", ownPackage, fhirBaseUri, displayName, fhirVersion, null).also { sources += it }

    /** Checks only the source's FHIR version, as Health Connect does. The screens' tests are not about validation. */
    override suspend fun upsert(sourceId: String, fhirVersion: String, jsons: List<String>): List<MedicalRecord> {
        val source = sources.firstOrNull { it.id == sourceId }
        require(source == null || source.fhirVersion == fhirVersion) {
            "Invalid fhir version: $fhirVersion. It did not match the data source's fhir version"
        }
        return jsons.map { json ->
            val resource = Json.parseToJsonElement(json).jsonObject
            val ref = MedicalRecordRef(sourceId, resource.getValue("resourceType").jsonPrimitive.content, resource.getValue("id").jsonPrimitive.content)
            val category = FhirResourceTypes.likelyCategory(resource) ?: MedicalCategory.VITAL_SIGNS
            MedicalRecord(ref, category, fhirVersion, json).also { record ->
                records.removeAll { it.ref == ref }
                records += record
            }
        }
    }

    /** Health Connect refuses to delete another app's records. */
    override suspend fun delete(refs: List<MedicalRecordRef>) {
        require(refs.all { isOwn(it.dataSourceId) }) { "Cannot delete another app's records" }
        records.removeAll { it.ref in refs }
    }

    override suspend fun deleteSource(sourceId: String) {
        sources.removeAll { it.id == sourceId }
        records.removeAll { it.ref.dataSourceId == sourceId }
    }

    private fun visible(category: MedicalCategory): List<MedicalRecord> {
        if (category !in readable && !writable) throw SecurityException("Caller doesn't have permission")
        return records.filter { it.category == category && canSee(it) }
    }

    private fun canSee(record: MedicalRecord): Boolean =
        record.category in readable || (writable && isOwn(record.ref.dataSourceId))

    private fun isOwn(sourceId: String) = sources.any { it.id == sourceId && it.packageName == ownPackage }
}
