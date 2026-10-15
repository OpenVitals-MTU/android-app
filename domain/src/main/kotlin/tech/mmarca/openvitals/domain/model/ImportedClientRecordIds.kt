package tech.mmarca.openvitals.domain.model

/**
 * The client-id prefixes of records OpenVitals writes for data logged somewhere else: an Apple
 * Health export, a CSV file, a record synced from another phone. Health Connect names OpenVitals
 * as their origin, but nobody logged them in OpenVitals.
 */
private val ImportedClientRecordPrefixes = listOf("apple_health_", "csv_", "sync_")

/** Whether [clientRecordId] marks a record OpenVitals imported or synced rather than one logged in it. */
fun isImportedClientRecordId(clientRecordId: String?): Boolean =
    clientRecordId != null && ImportedClientRecordPrefixes.any { clientRecordId.startsWith(it) }
