package tech.mmarca.openvitals.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.core.performance.PerformanceTrace
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsRepository
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.MedicalRecordPage
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.model.MedicalRecordSource
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.MedicalCategoryMapping

@Singleton
class MedicalRecordsRepositoryImpl @Inject constructor(
    private val hc: HealthConnectManager,
) : MedicalRecordsRepository {

    override val permissions: Set<String> get() = hc.medicalRecordsPermissions

    override val writePermissions: Set<String>
        get() = hc.medicalRecordsPermissions.filterTo(mutableSetOf()) { it == MedicalCategoryMapping.WRITE_PERMISSION }

    override fun isAvailable(): Boolean = hc.isMedicalRecordsAvailable()

    override suspend fun grantedPermissions(): Set<String> = hc.grantedPermissions() intersect permissions

    override suspend fun readableCategories(): Set<MedicalCategory> {
        val granted = hc.grantedPermissions()
        return MedicalCategory.entries.filterTo(mutableSetOf()) { MedicalCategoryMapping.readPermission(it) in granted }
    }

    override suspend fun canWrite(): Boolean = MedicalCategoryMapping.WRITE_PERMISSION in hc.grantedPermissions()

    override suspend fun count(category: MedicalCategory): Int = hc.medicalRecordsReader.count(category)

    private suspend fun readPage(category: MedicalCategory, pageSize: Int, pageToken: String?, sourceIds: Set<String>): MedicalRecordPage =
        PerformanceTrace.timed("medicalRecordsPage", mapOf("category" to category, "pageSize" to pageSize)) {
            hc.medicalRecordsReader.readPage(category, pageSize, pageToken, sourceIds)
        }

    override suspend fun readCategory(category: MedicalCategory, sourceIds: Set<String>): List<MedicalRecord> {
        val records = mutableListOf<MedicalRecord>()
        var pageToken: String? = null
        do {
            val page = readPage(category, CategoryPageSize, pageToken, sourceIds)
            records += page.records
            pageToken = page.nextPageToken
        } while (!pageToken.isNullOrEmpty())
        return records
    }

    override suspend fun readRecords(refs: List<MedicalRecordRef>): List<MedicalRecord> =
        hc.medicalRecordsReader.readByIds(refs)

    override suspend fun ownSources(): List<MedicalRecordSource> = hc.medicalRecordsReader.ownDataSources()

    override suspend fun allSources(): List<MedicalRecordSource> = hc.medicalRecordsReader.dataSources(emptyList())

    override suspend fun createSource(
        fhirBaseUri: String,
        displayName: String,
        fhirVersion: String,
    ): MedicalRecordSource = hc.medicalRecordsWriter.createDataSource(fhirBaseUri, displayName, fhirVersion)

    override suspend fun upsert(sourceId: String, fhirVersion: String, jsons: List<String>): List<MedicalRecord> =
        PerformanceTrace.timed("medicalRecordsUpsert", mapOf("records" to jsons.size)) {
            hc.medicalRecordsWriter.upsert(sourceId, fhirVersion, jsons)
        }

    override suspend fun delete(refs: List<MedicalRecordRef>) = hc.medicalRecordsWriter.delete(refs)

    override suspend fun deleteSource(sourceId: String) = hc.medicalRecordsWriter.deleteDataSource(sourceId)

    private companion object {
        /** Measured in the spike: a page of 1000 takes about 0.6 s, and personal records rarely need a second. */
        const val CategoryPageSize = 1000
    }
}
