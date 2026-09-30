@file:OptIn(ExperimentalPersonalHealthRecordApi::class)

package tech.mmarca.openvitals.healthconnect

import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.records.FhirVersion
import androidx.health.connect.client.records.MedicalDataSource
import androidx.health.connect.client.records.MedicalResource
import androidx.health.connect.client.records.MedicalResourceId
import androidx.health.connect.client.request.CreateMedicalDataSourceRequest
import androidx.health.connect.client.request.GetMedicalDataSourcesRequest
import androidx.health.connect.client.request.ReadMedicalResourcesInitialRequest
import androidx.health.connect.client.request.ReadMedicalResourcesPageRequest
import androidx.health.connect.client.request.UpsertMedicalResourceRequest
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.MedicalRecordPage
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

/**
 * The Health Connect medical records calls, in app types.
 *
 * The Jetpack medical classes build platform objects in their constructors, so a JVM
 * test cannot create them. The reader and writer depend on this interface, and tests
 * give them a fake. Only [HealthConnectMedicalRecordsClient] touches the Jetpack classes.
 */
internal interface MedicalRecordsClient {
    /** An empty [packageNames] asks for every app's sources. */
    suspend fun dataSources(packageNames: List<String>): List<MedicalRecordSource>

    suspend fun createDataSource(fhirBaseUri: String, displayName: String, fhirVersion: String): MedicalRecordSource

    /** A null [pageToken] starts the read. An empty [sourceIds] filters nothing. */
    suspend fun readPage(
        category: MedicalCategory,
        sourceIds: Set<String>,
        pageSize: Int,
        pageToken: String?,
    ): MedicalRecordPage

    suspend fun readByIds(refs: List<MedicalRecordRef>): List<MedicalRecord>

    /** One transaction: all of [jsons] are written, or none. */
    suspend fun upsert(dataSourceId: String, fhirVersion: String, jsons: List<String>): List<MedicalRecord>

    suspend fun delete(refs: List<MedicalRecordRef>)

    suspend fun deleteDataSource(id: String)
}

/** The real client. Thin on purpose: the device run is its only test. */
internal class HealthConnectMedicalRecordsClient(
    private val client: () -> HealthConnectClient,
) : MedicalRecordsClient {

    override suspend fun dataSources(packageNames: List<String>): List<MedicalRecordSource> =
        client().getMedicalDataSources(GetMedicalDataSourcesRequest(packageNames)).map { it.toSource() }

    override suspend fun createDataSource(
        fhirBaseUri: String,
        displayName: String,
        fhirVersion: String,
    ): MedicalRecordSource {
        val request = CreateMedicalDataSourceRequest(
            fhirBaseUri = fhirBaseUri.toUri(),
            displayName = displayName,
            fhirVersion = FhirVersion.parseFhirVersion(fhirVersion),
        )
        return client().createMedicalDataSource(request).toSource()
    }

    override suspend fun readPage(
        category: MedicalCategory,
        sourceIds: Set<String>,
        pageSize: Int,
        pageToken: String?,
    ): MedicalRecordPage {
        val request = if (pageToken == null) {
            ReadMedicalResourcesInitialRequest(MedicalCategoryMapping.resourceType(category), sourceIds, pageSize)
        } else {
            ReadMedicalResourcesPageRequest(pageToken, pageSize)
        }
        val response = client().readMedicalResources(request)
        return MedicalRecordPage(
            records = response.medicalResources.mapNotNull { it.toRecord() },
            nextPageToken = response.nextPageToken,
            remainingCount = response.remainingCount,
        )
    }

    override suspend fun readByIds(refs: List<MedicalRecordRef>): List<MedicalRecord> =
        client().readMedicalResources(refs.map { it.toResourceId() }).mapNotNull { it.toRecord() }

    override suspend fun upsert(dataSourceId: String, fhirVersion: String, jsons: List<String>): List<MedicalRecord> {
        val version = FhirVersion.parseFhirVersion(fhirVersion)
        return client()
            .upsertMedicalResources(jsons.map { UpsertMedicalResourceRequest(dataSourceId, version, it) })
            .mapNotNull { it.toRecord() }
    }

    override suspend fun delete(refs: List<MedicalRecordRef>) {
        client().deleteMedicalResources(refs.map { it.toResourceId() })
    }

    override suspend fun deleteDataSource(id: String) {
        client().deleteMedicalDataSourceWithData(id)
    }
}

private fun FhirVersion.text(): String = "$major.$minor.$patch"

private fun MedicalDataSource.toSource(): MedicalRecordSource = MedicalRecordSource(
    id = id,
    packageName = packageName,
    fhirBaseUri = fhirBaseUri.toString(),
    displayName = displayName,
    fhirVersion = fhirVersion.text(),
    lastDataUpdate = lastDataUpdateTime,
)

/** Null for a category or type this app does not know yet. */
private fun MedicalResource.toRecord(): MedicalRecord? {
    val category = MedicalCategoryMapping.category(type) ?: return null
    val typeName = MedicalCategoryMapping.fhirTypeName(id.fhirResourceType) ?: return null
    return MedicalRecord(
        ref = MedicalRecordRef(dataSourceId, typeName, id.fhirResourceId),
        category = category,
        fhirVersion = fhirVersion.text(),
        json = fhirResource.data,
    )
}

private fun MedicalRecordRef.toResourceId(): MedicalResourceId {
    val fhirType = requireNotNull(MedicalCategoryMapping.fhirTypeId(resourceType)) {
        "Health Connect has no FHIR type $resourceType"
    }
    return MedicalResourceId(dataSourceId, fhirType, resourceId)
}
