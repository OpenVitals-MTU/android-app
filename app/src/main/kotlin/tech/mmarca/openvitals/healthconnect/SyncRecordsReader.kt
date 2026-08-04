package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.Record
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Generic read-all-records-of-type-in-range for the phone-to-phone sync
 * feature. Unlike the metric readers this is type-agnostic: sync moves every
 * negotiated record type through one code path, so it reuses the shared paging
 * helper and the rate-limit/backoff discipline in [HealthConnectReaderSupport]
 * instead of adding ~40 typed read methods.
 *
 * Failures (including an exhausted rate-limit retry) degrade to an empty list,
 * matching the other readers — a type that cannot be read simply contributes
 * nothing to the outgoing batches.
 */
internal class SyncRecordsReader(private val support: HealthConnectReaderSupport) {

    @Suppress("UNCHECKED_CAST")
    suspend fun readAllRecords(
        recordType: KClass<out Record>,
        start: Instant,
        end: Instant,
    ): List<Record> =
        support.withLogging("readSyncRecords[${recordType.simpleName}]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = recordType as KClass<Record>,
                timeRangeFilter = TimeRangeFilter.between(start, end),
            )
        }
}
