package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Generic page-streamed read of all records of a type in a range, for the
 * phone-to-phone sync feature. Unlike the metric readers this is type-agnostic:
 * sync moves every negotiated record type through one code path.
 *
 * Streamed, not accumulated: the caller sees one page at a time and the page
 * is dropped before the next is fetched. Accumulating the whole window in one
 * list — a data-dense year is easily a hundred thousand records — pinned small
 * heaps at their limit and GC-thrashed the session until its timeouts fired.
 *
 * A page read failure (including an exhausted rate-limit retry) ends the
 * type's stream with the pages already delivered, matching the other readers'
 * degrade-to-empty discipline. The [action] itself runs OUTSIDE the logging
 * envelope so a failure in the caller — the sync link dying mid-send —
 * propagates instead of being swallowed as a read failure.
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
            val response = support.withNullableLogging(
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
            } ?: return
            if (response.records.isNotEmpty()) action(response.records)
            pageToken = response.pageToken
        } while (pageToken != null)
    }

    private companion object {
        const val SYNC_READ_PAGE_SIZE = 1000
    }
}
