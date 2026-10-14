package tech.mmarca.openvitals.data.repository

import androidx.health.connect.client.records.Record
import kotlinx.coroutines.CancellationException

/** What became of one record in [insertWithOutcomes]. */
enum class RecordWrite { WRITTEN, PRESENT, FAILED }

/**
 * Writes [records] in one batch. Health Connect refuses the whole batch when
 * one `clientRecordId` is already there at the same or a higher version;
 * then each record is written alone, so the new ones still land.
 *
 * Returns what became of each record, in order. Any other failure of the
 * batch is thrown. Any other failure of a single record goes to [onFailure],
 * which throws it unless the caller says otherwise.
 */
suspend fun AppleHealthImportRepository.insertWithOutcomes(
    records: List<Record>,
    onFailure: (Record, Exception) -> Unit = { _, error -> throw error },
): List<RecordWrite> {
    if (records.isEmpty()) return emptyList()
    try {
        insertImportedRecords(records)
        return List(records.size) { RecordWrite.WRITTEN }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        if (!error.isDuplicateClientRecordFailure()) throw error
        // One record: the refusal already says it is present.
        if (records.size == 1) return listOf(RecordWrite.PRESENT)
    }
    return records.map { record ->
        try {
            insertImportedRecords(listOf(record))
            RecordWrite.WRITTEN
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (error.isDuplicateClientRecordFailure()) {
                RecordWrite.PRESENT
            } else {
                onFailure(record, error)
                RecordWrite.FAILED
            }
        }
    }
}

/** [insertWithOutcomes], counting the records that landed. A record already present does not count. */
suspend fun AppleHealthImportRepository.insertSkippingPresent(records: List<Record>): Int =
    insertWithOutcomes(records).count { it == RecordWrite.WRITTEN }

/** True when Health Connect refused a write because the `clientRecordId` is already there. */
internal fun Throwable.isDuplicateClientRecordFailure(): Boolean {
    val text = generateSequence(this) { it.cause }
        .joinToString(" ") { listOfNotNull(it.message, it::class.simpleName).joinToString(" ") }
        .lowercase()
    return "clientrecordid" in text ||
        "client record id" in text ||
        (("duplicate" in text || "already exist" in text || "already exists" in text) && "record" in text)
}
