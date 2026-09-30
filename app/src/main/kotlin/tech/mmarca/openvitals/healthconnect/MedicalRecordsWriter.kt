package tech.mmarca.openvitals.healthconnect

import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

/**
 * Writes FHIR medical records. FHIR records have no `clientRecordId`: a record is
 * identified by its data source, type and id, so writing it again updates it.
 * Health Connect lets an app delete only the records and sources it wrote.
 */
internal class MedicalRecordsWriter(
    private val client: MedicalRecordsClient,
    private val requireSyncEnabled: () -> Unit,
    private val isAvailable: () -> Boolean,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) {
    /** Health Connect refuses a display name this app already uses. */
    suspend fun createDataSource(
        fhirBaseUri: String,
        displayName: String,
        fhirVersion: String,
    ): MedicalRecordSource = write { client.createDataSource(fhirBaseUri, displayName, fhirVersion) }

    /** Inserts or updates [jsons] in one transaction: all of them are written, or none. */
    suspend fun upsert(dataSourceId: String, fhirVersion: String, jsons: List<String>): List<MedicalRecord> {
        if (jsons.isEmpty()) return emptyList()
        return write { client.upsert(dataSourceId, fhirVersion, jsons) }
    }

    suspend fun delete(refs: List<MedicalRecordRef>) {
        if (refs.isEmpty()) return
        write { client.delete(refs) }
    }

    /** Deletes the source and every record in it. */
    suspend fun deleteDataSource(id: String) = write { client.deleteDataSource(id) }

    private suspend fun <T> write(block: suspend () -> T): T {
        requireSyncEnabled()
        requireMedicalRecordsAvailable(isAvailable)
        return withContext(dispatchers.io) { block() }
    }
}
