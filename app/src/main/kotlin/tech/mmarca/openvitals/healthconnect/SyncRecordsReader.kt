package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Page-streamed read of every record of a type in a range, for sync.
 * Streamed, not accumulated: a year pinned small heaps at their limit. A
 * page failure throws rather than truncating: a short stream once reported
 * a complete transfer. [action] runs outside the logging envelope.
 */
internal class SyncRecordsReader(private val support: HealthConnectReaderSupport) {

    @Suppress("UNCHECKED_CAST")
    suspend fun forEachRecordPage(
        recordType: KClass<out Record>,
        start: Instant,
        end: Instant,
        action: suspend (List<Record>) -> Unit,
    ) {
        val filter = TimeRangeFilter.between(start, end)
        var pageToken: String? = null
        do {
            val response = support.withLoggingOrThrow(
                "readSyncRecordPage[${recordType.simpleName}]",
            ) {
                support.client().readRecords(
                    ReadRecordsRequest(
                        recordType = recordType as KClass<Record>,
                        timeRangeFilter = filter,
                        pageSize = SYNC_READ_PAGE_SIZE,
                        pageToken = pageToken,
                    ),
                )
            }
            if (response.records.isNotEmpty()) action(response.records)
            pageToken = response.pageToken
        } while (pageToken != null)
    }

    private companion object {
        const val SYNC_READ_PAGE_SIZE = 1000
    }
}
