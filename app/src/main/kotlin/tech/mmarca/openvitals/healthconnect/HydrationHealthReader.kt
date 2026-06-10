package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.HydrationWriteRequest
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class HydrationHealthReader(
    private val support: HealthConnectReaderSupport,
    private val appPackageName: String,
) {
    suspend fun readHydrationLiters(date: LocalDate): Double? {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withNullableLogging("readHydrationLiters[$date][$start..$end]") {
            val aggregateLiters = support.client().aggregate(
                AggregateRequest(
                    metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[HydrationRecord.VOLUME_TOTAL]?.inLiters
            aggregateLiters?.takeIf { it > 0.0 }
                ?: support.client().readHydrationRecordsByDate(start, end, zone).values.sum().takeIf { it > 0.0 }
                ?: aggregateLiters
        }
    }

    suspend fun readTodayHydrationLiters(): Double? = readHydrationLiters(LocalDate.now())

    suspend fun readDailyHydration(startDate: LocalDate, endDate: LocalDate): List<DailyHydration> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withLogging("readDailyHydration[$start..$end]", emptyList()) {
            val aggregateBuckets = support.client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).map { bucket ->
                DailyHydration(
                    date = bucket.startTime.atZone(zone).toLocalDate(),
                    liters = bucket.result[HydrationRecord.VOLUME_TOTAL]?.inLiters ?: 0.0,
                )
            }
            val hydrationByDate = if (aggregateBuckets.any { it.liters > 0.0 }) {
                aggregateBuckets.associate { it.date to it.liters }
            } else {
                support.client().readHydrationRecordsByDate(start, end, zone)
            }
            dailyHydrationSeries(startDate, endDate, hydrationByDate)
        }
    }

    suspend fun readHydrationEntries(start: Instant, end: Instant): List<HydrationEntry> =
        support.withLogging("readHydrationEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = HydrationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 200,
            ).map { record ->
                HydrationEntry(
                    startTime = record.startTime,
                    endTime = record.endTime,
                    liters = record.volume.inLiters,
                    source = record.metadata.dataOrigin.packageName,
                    id = record.metadata.id,
                    isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
                )
            }
        }

    suspend fun readHydrationEntry(id: String): HydrationEntry? =
        support.withNullableLogging("readHydrationEntry[$id]") {
            support.client().readRecord(HydrationRecord::class, id).record.toHydrationEntry()
        }

    suspend fun writeHydrationEntry(request: HydrationWriteRequest): String = withContext(Dispatchers.IO) {
        require(request.volumeLiters > 0.0) { "Hydration volume must be greater than zero." }
        require(request.volumeLiters <= MaxHydrationRecordLiters) {
            "Hydration volume must not exceed ${MaxHydrationRecordLiters.toInt()} L."
        }

        val startTime = request.time
        val endTime = startTime.plusSeconds(1)
        val zone = ZoneId.systemDefault()
        val clientRecordId = "openvitals_hydration_${startTime.toEpochMilli()}_${UUID.randomUUID()}"
        val volumeMilliliters = request.volumeLiters * MillilitersPerLiter
        val record = HydrationRecord(
            startTime = startTime,
            startZoneOffset = zone.rules.getOffset(startTime),
            endTime = endTime,
            endZoneOffset = zone.rules.getOffset(endTime),
            volume = Volume.milliliters(volumeMilliliters),
            metadata = Metadata.manualEntry(
                device = Device(type = Device.TYPE_PHONE),
                clientRecordId = clientRecordId,
            ),
        )

        Log.d(TAG, "Writing hydration record ${support.diagnosticsSummary()}")
        support.client().insertRecords(listOf(record))
        clientRecordId
    }

    suspend fun updateHydrationEntry(id: String, request: HydrationWriteRequest) = withContext(Dispatchers.IO) {
        require(request.volumeLiters > 0.0) { "Hydration volume must be greater than zero." }
        require(request.volumeLiters <= MaxHydrationRecordLiters) {
            "Hydration volume must not exceed ${MaxHydrationRecordLiters.toInt()} L."
        }

        val existing = support.client().readRecord(HydrationRecord::class, id).record
        existing.requireOpenVitalsOrigin(appPackageName)

        val startTime = request.time
        val endTime = startTime.plusSeconds(1)
        val zone = ZoneId.systemDefault()
        val volumeMilliliters = request.volumeLiters * MillilitersPerLiter
        val record = HydrationRecord(
            startTime = startTime,
            startZoneOffset = zone.rules.getOffset(startTime),
            endTime = endTime,
            endZoneOffset = zone.rules.getOffset(endTime),
            volume = Volume.milliliters(volumeMilliliters),
            metadata = Metadata.manualEntryWithId(
                id = id,
                device = existing.metadata.device ?: Device(type = Device.TYPE_PHONE),
            ),
        )

        Log.d(TAG, "Updating hydration record ${support.diagnosticsSummary()}")
        support.client().updateRecords(listOf(record))
    }

    suspend fun deleteHydrationEntry(id: String) = withContext(Dispatchers.IO) {
        val existing = support.client().readRecord(HydrationRecord::class, id).record
        existing.requireOpenVitalsOrigin(appPackageName)

        Log.d(TAG, "Deleting hydration record ${support.diagnosticsSummary()}")
        support.client().deleteRecords(
            recordType = HydrationRecord::class,
            recordIdsList = listOf(existing.metadata.id),
            clientRecordIdsList = emptyList(),
        )
    }

    private fun HydrationRecord.toHydrationEntry(): HydrationEntry =
        HydrationEntry(
            startTime = startTime,
            endTime = endTime,
            liters = volume.inLiters,
            source = metadata.dataOrigin.packageName,
            id = metadata.id,
            isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
        )
}

private const val TAG = "HydrationHealthReader"
private const val MaxHydrationRecordLiters = 100.0
private const val MillilitersPerLiter = 1000.0

internal suspend fun HealthConnectClient.readHydrationRecordsByDate(
    start: Instant,
    end: Instant,
    zone: ZoneId,
): Map<LocalDate, Double> =
    readRecordsPaged(
        recordType = HydrationRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
    )
        .groupBy { record -> record.startTime.atZone(zone).toLocalDate() }
        .mapValues { (_, records) -> records.sumOf { it.volume.inLiters } }

internal fun dailyHydrationSeries(
    startDate: LocalDate,
    endDate: LocalDate,
    hydrationByDate: Map<LocalDate, Double>,
): List<DailyHydration> =
    generateSequence(startDate) { date ->
        date.plusDays(1).takeUnless { it.isAfter(endDate) }
    }.map { date ->
        DailyHydration(
            date = date,
            liters = hydrationByDate[date] ?: 0.0,
        )
    }.toList()
