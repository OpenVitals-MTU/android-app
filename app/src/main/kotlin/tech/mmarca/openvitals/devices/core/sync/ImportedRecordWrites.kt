package tech.mmarca.openvitals.devices.core.sync

import androidx.health.connect.client.records.Record
import kotlinx.coroutines.CancellationException
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.features.imports.applehealth.isDuplicateClientRecordFailure

/**
 * Writes [records] in one batch and returns how many landed. Health Connect
 * refuses the whole batch if one `clientRecordId` exists at the same or a
 * higher version. Then each record is written alone, and a refused one
 * counts as already present.
 */
suspend fun AppleHealthImportRepository.insertSkippingPresent(records: List<Record>): Int {
    if (records.isEmpty()) return 0
    try {
        insertImportedRecords(records)
        return records.size
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        if (!error.isDuplicateClientRecordFailure()) throw error
    }
    var written = 0
    for (record in records) {
        try {
            insertImportedRecords(listOf(record))
            written += 1
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (!error.isDuplicateClientRecordFailure()) throw error
        }
    }
    return written
}
