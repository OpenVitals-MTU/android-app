package tech.mmarca.openvitals.healthconnect

import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.MedicalRecordPage
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

/**
 * Reads FHIR medical records. Every read throws on failure: an empty list here would
 * claim there are no records, and a missing permission must reach the screen.
 */
internal class MedicalRecordsHealthReader(
    private val support: HealthConnectReaderSupport,
    private val client: MedicalRecordsClient,
    private val appPackageName: String,
    private val isAvailable: () -> Boolean,
) {
    /** This app's own data sources. They need no read permission. */
    suspend fun ownDataSources(): List<MedicalRecordSource> = dataSources(listOf(appPackageName))

    /** The data sources [packageNames] wrote. An empty list asks for every app's. */
    suspend fun dataSources(packageNames: List<String>): List<MedicalRecordSource> =
        read("getMedicalDataSources") { client.dataSources(packageNames) }

    /** One page of [category]. A null [pageToken] starts the read. An empty [sourceIds] filters nothing. */
    suspend fun readPage(
        category: MedicalCategory,
        pageSize: Int,
        pageToken: String? = null,
        sourceIds: Set<String> = emptySet(),
    ): MedicalRecordPage = read("readMedicalResources[${category.name} $pageSize]") {
        client.readPage(category, sourceIds, pageSize, pageToken)
    }

    /** How many records of [category] this app may read. One record is read to learn it. */
    suspend fun count(category: MedicalCategory): Int =
        readPage(category, pageSize = 1).let { it.records.size + it.remainingCount }

    suspend fun readByIds(refs: List<MedicalRecordRef>): List<MedicalRecord> {
        if (refs.isEmpty()) return emptyList()
        return read("readMedicalResourcesByIds") { client.readByIds(refs) }
    }

    private suspend fun <T> read(operation: String, block: suspend () -> T): T {
        requireMedicalRecordsAvailable(isAvailable)
        return support.withLoggingOrThrow(operation, block)
    }
}
