package tech.mmarca.openvitals.data.repository.contract

import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

/**
 * FHIR medical records in Health Connect. Health Connect is the only copy: nothing
 * here caches records. Every call throws on failure, so a missing permission or an
 * unavailable feature reaches the screen instead of looking like "no records".
 */
interface MedicalRecordsRepository {
    /** The twelve category reads and the one write. Empty where the feature is unavailable. */
    val permissions: Set<String>

    /** The write permission alone, which an import needs before it reads anything. Empty where the feature is unavailable. */
    val writePermissions: Set<String>

    fun isAvailable(): Boolean

    /** The medical permissions the user has granted. */
    suspend fun grantedPermissions(): Set<String>

    /** Categories whose read permission is granted. The others still list this app's own records. */
    suspend fun readableCategories(): Set<MedicalCategory>

    /** Whether this app may write medical records, which also lets it read its own in every category. */
    suspend fun canWrite(): Boolean

    suspend fun count(category: MedicalCategory): Int

    /** Every record of [category] this app may read, page by page. A non-empty [sourceIds] reads only those sources. */
    suspend fun readCategory(category: MedicalCategory, sourceIds: Set<String> = emptySet()): List<MedicalRecord>

    /** The records that exist and can be read, in no set order. */
    suspend fun readRecords(refs: List<MedicalRecordRef>): List<MedicalRecord>

    /** Sources this app wrote. */
    suspend fun ownSources(): List<MedicalRecordSource>

    /** Sources from every app. */
    suspend fun allSources(): List<MedicalRecordSource>

    suspend fun createSource(fhirBaseUri: String, displayName: String, fhirVersion: String): MedicalRecordSource

    /** One transaction: a single bad record fails the whole batch. */
    suspend fun upsert(sourceId: String, fhirVersion: String, jsons: List<String>): List<MedicalRecord>

    /** Deletes records this app wrote. Health Connect refuses another app's. */
    suspend fun delete(refs: List<MedicalRecordRef>)

    /** Deletes one of this app's sources and every record in it. */
    suspend fun deleteSource(sourceId: String)
}
