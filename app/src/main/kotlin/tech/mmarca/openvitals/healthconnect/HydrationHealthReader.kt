package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.HydrationEntryChange
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
    /**
     * One row per day, zero-filled. A one-day range is the day value. Chunked
     * like readDailySteps, with one guard around all chunks.
     */
    suspend fun readDailyHydration(startDate: LocalDate, endDate: LocalDate): List<DailyHydration> {
        if (endDate.isBefore(startDate)) return emptyList()
        val zone = ZoneId.systemDefault()
        return support.withLogging("readDailyHydration[$startDate..$endDate]", emptyList()) {
            val hydrationByDate = dailyAggregateDateChunks(startDate, endDate)
                .flatMap { (chunkStart, chunkEnd) ->
                    support.client().aggregateGroupByDuration(
                        AggregateGroupByDurationRequest(
                            metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                            timeRangeFilter = TimeRangeFilter.between(
                                chunkStart.atStartOfDay(zone).toInstant(),
                                chunkEnd.plusDays(1).atStartOfDay(zone).toInstant(),
                            ),
                            timeRangeSlicer = Duration.ofDays(1),
                        )
                    ).byLocalDate(zone)
                }
                .associate { day -> day.date to day.total { it[HydrationRecord.VOLUME_TOTAL]?.inLiters } }
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
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    clientRecordId = record.metadata.clientRecordId,
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
        val drinkSegment = request.drinkId
            ?.toHydrationDrinkClientRecordSegment()
            ?.let { "_drink_$it" }
            .orEmpty()
        val clientRecordId = "openvitals_hydration_${startTime.toEpochMilli()}${drinkSegment}_${UUID.randomUUID()}"
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

    suspend fun updateHydrationEntry(
        id: String,
        request: HydrationWriteRequest,
    ): HydrationEntryChange = withContext(Dispatchers.IO) {
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
        val device = existing.metadata.device ?: Device(type = Device.TYPE_PHONE)
        val clientRecordId = existing.metadata.clientRecordId
        val record = HydrationRecord(
            startTime = startTime,
            startZoneOffset = zone.rules.getOffset(startTime),
            endTime = endTime,
            endZoneOffset = zone.rules.getOffset(endTime),
            volume = Volume.milliliters(volumeMilliliters),
            // The client id is the link to the drink's nutrition record. An update by record id
            // carries no client id, so the stored one would be lost. Writing the same client id
            // with a higher version replaces the record in place and keeps the link.
            metadata = if (clientRecordId != null) {
                Metadata.manualEntry(
                    device = device,
                    clientRecordId = clientRecordId,
                    clientRecordVersion = existing.metadata.clientRecordVersion + 1,
                )
            } else {
                Metadata.manualEntryWithId(id = id, device = device)
            },
        )

        Log.d(TAG, "Updating hydration record ${support.diagnosticsSummary()}")
        if (clientRecordId != null) {
            support.client().insertRecords(listOf(record))
        } else {
            support.client().updateRecords(listOf(record))
        }
        HydrationEntryChange(
            clientRecordId = clientRecordId,
            oldTime = existing.startTime,
            newTime = startTime,
            oldVolumeLiters = existing.volume.inLiters,
            newVolumeLiters = request.volumeLiters,
        )
    }

    suspend fun deleteHydrationEntry(id: String): String? = withContext(Dispatchers.IO) {
        val existing = support.client().readRecord(HydrationRecord::class, id).record
        existing.requireOpenVitalsOrigin(appPackageName)
        val clientRecordId = existing.metadata.clientRecordId

        Log.d(TAG, "Deleting hydration record ${support.diagnosticsSummary()}")
        support.client().deleteRecords(
            recordType = HydrationRecord::class,
            recordIdsList = listOf(existing.metadata.id),
            clientRecordIdsList = emptyList(),
        )
        clientRecordId
    }

    /** Deletes by clientRecordId, to roll back a record whose paired nutrition write failed. */
    suspend fun deleteHydrationEntryByClientRecordId(clientRecordId: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Deleting hydration record by clientRecordId ${support.diagnosticsSummary()}")
        support.client().deleteRecords(
            recordType = HydrationRecord::class,
            recordIdsList = emptyList(),
            clientRecordIdsList = listOf(clientRecordId),
        )
    }

    private fun HydrationRecord.toHydrationEntry(): HydrationEntry =
        HydrationEntry(
            startTime = startTime,
            endTime = endTime,
            liters = volume.inLiters,
            source = SyncedSourceOverlay.displaySource(metadata),
            id = metadata.id,
            clientRecordId = metadata.clientRecordId,
            isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
        )
}

private const val TAG = "HydrationHealthReader"
private const val MaxHydrationRecordLiters = 100.0
private const val MillilitersPerLiter = 1000.0

private fun String.toHydrationDrinkClientRecordSegment(): String? =
    trim()
        .filter { character -> character.isLetterOrDigit() || character == '-' }
        .takeIf { it.isNotBlank() }

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
