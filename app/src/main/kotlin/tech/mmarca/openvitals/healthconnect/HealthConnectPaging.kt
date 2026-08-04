package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlin.reflect.KClass

internal suspend fun <T : Record> HealthConnectClient.readRecordsPaged(
    recordType: KClass<T>,
    timeRangeFilter: TimeRangeFilter,
    ascendingOrder: Boolean = true,
    pageSize: Int = 1000,
    maxRecords: Int? = null,
): List<T> {
    val startedAt = System.currentTimeMillis()
    val records = mutableListOf<T>()
    var pageToken: String? = null
    var pageCount = 0

    do {
        val remaining = maxRecords?.minus(records.size)
        if (remaining != null && remaining <= 0) break

        val response = readRecords(
            ReadRecordsRequest(
                recordType = recordType,
                timeRangeFilter = timeRangeFilter,
                ascendingOrder = ascendingOrder,
                pageSize = remaining?.let { minOf(pageSize, it) } ?: pageSize,
                pageToken = pageToken,
            )
        )

        records += response.records
        pageToken = response.pageToken
        pageCount++
    } while (pageToken != null && (maxRecords == null || records.size < maxRecords))

    val result = maxRecords?.let { records.take(it) } ?: records
    Log.d(
        "OpenVitalsPerf",
        "healthConnect.readRecordsPaged type=${recordType.simpleName} records=${result.size} " +
            "pages=$pageCount durationMs=${System.currentTimeMillis() - startedAt}",
    )
    return result
}
