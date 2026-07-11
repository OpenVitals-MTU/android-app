package tech.mmarca.openvitals.health_connect_native

import android.util.Log
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ported from the native OpenVitals app (`healthconnect/HydrationHealthReader.kt`).
 *
 * Returns Pigeon `*Msg` types. Daily-series gap-filling stays on the Dart side;
 * this reader returns the raw per-day aggregate buckets.
 */
internal class HydrationHealthReader(
  private val support: HealthConnectReaderSupport,
  private val appPackageName: String,
) {
  suspend fun readHydrationLiters(start: Instant, end: Instant): Double? =
    support.withNullableLogging("readHydrationLiters[$start..$end]") {
      support.client().aggregate(
        AggregateRequest(
          metrics = setOf(HydrationRecord.VOLUME_TOTAL),
          timeRangeFilter = TimeRangeFilter.between(start, end),
        ),
      )[HydrationRecord.VOLUME_TOTAL]?.inLiters
    }

  suspend fun readDailyHydration(start: Instant, end: Instant): List<DailyHydrationMsg> =
    support.withLogging("readDailyHydration[$start..$end]", emptyList()) {
      support.client().aggregateGroupByDuration(
        AggregateGroupByDurationRequest(
          metrics = setOf(HydrationRecord.VOLUME_TOTAL),
          timeRangeFilter = TimeRangeFilter.between(start, end),
          timeRangeSlicer = Duration.ofDays(1),
        ),
      ).map { bucket ->
        DailyHydrationMsg(
          dateEpochMs = bucket.startTime.toEpochMilli(),
          liters = bucket.result[HydrationRecord.VOLUME_TOTAL]?.inLiters ?: 0.0,
        )
      }
    }

  suspend fun readHydrationEntries(start: Instant, end: Instant): List<HydrationEntryMsg> =
    support.withLogging("readHydrationEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = HydrationRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 200,
      ).map { it.toMsg() }
    }

  suspend fun readHydrationEntry(id: String): HydrationEntryMsg? =
    support.withNullableLogging("readHydrationEntry[$id]") {
      support.client().readRecord(HydrationRecord::class, id).record.toMsg()
    }

  suspend fun writeHydrationEntry(request: HydrationWriteRequestMsg): String =
    withContext(Dispatchers.IO) {
      support.requireSyncEnabled()
      require(request.volumeLiters > 0.0) { "Hydration volume must be greater than zero." }
      require(request.volumeLiters <= MaxHydrationRecordLiters) {
        "Hydration volume must not exceed ${MaxHydrationRecordLiters.toInt()} L."
      }
      val startTime = Instant.ofEpochMilli(request.timeEpochMs)
      val drinkSegment = request.drinkId
        ?.toHydrationDrinkClientRecordSegment()
        ?.let { "_drink_$it" }
        .orEmpty()
      val clientRecordId =
        "openvitals_hydration_${startTime.toEpochMilli()}${drinkSegment}_${UUID.randomUUID()}"
      Log.d(TAG, "Writing hydration record ${support.diagnosticsSummary()}")
      support.client().insertRecords(
        listOf(
          buildRecord(
            startTime,
            request.volumeLiters,
            Metadata.manualEntry(
              clientRecordId = clientRecordId,
              device = Device(type = Device.TYPE_PHONE),
            ),
          ),
        ),
      )
      clientRecordId
    }

  suspend fun updateHydrationEntry(id: String, request: HydrationWriteRequestMsg) =
    withContext(Dispatchers.IO) {
      support.requireSyncEnabled()
      require(request.volumeLiters > 0.0) { "Hydration volume must be greater than zero." }
      require(request.volumeLiters <= MaxHydrationRecordLiters) {
        "Hydration volume must not exceed ${MaxHydrationRecordLiters.toInt()} L."
      }
      val existing = support.client().readRecord(HydrationRecord::class, id).record
      existing.requireOpenVitalsOrigin(appPackageName)
      val startTime = Instant.ofEpochMilli(request.timeEpochMs)
      Log.d(TAG, "Updating hydration record ${support.diagnosticsSummary()}")
      support.client().updateRecords(
        listOf(
          buildRecord(
            startTime,
            request.volumeLiters,
            Metadata.manualEntryWithId(
              id = id,
              device = existing.metadata.device ?: Device(type = Device.TYPE_PHONE),
            ),
          ),
        ),
      )
    }

  suspend fun deleteHydrationEntry(id: String): String? = withContext(Dispatchers.IO) {
    support.requireSyncEnabled()
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

  private fun buildRecord(startTime: Instant, volumeLiters: Double, metadata: Metadata): HydrationRecord {
    val endTime = startTime.plusSeconds(1)
    val zone = ZoneId.systemDefault()
    return HydrationRecord(
      startTime = startTime,
      startZoneOffset = zone.rules.getOffset(startTime),
      endTime = endTime,
      endZoneOffset = zone.rules.getOffset(endTime),
      volume = Volume.milliliters(volumeLiters * MillilitersPerLiter),
      metadata = metadata,
    )
  }

  private fun HydrationRecord.toMsg() = HydrationEntryMsg(
    startEpochMs = startTime.toEpochMilli(),
    endEpochMs = endTime.toEpochMilli(),
    liters = volume.inLiters,
    source = metadata.dataOrigin.packageName,
    id = metadata.id,
    clientRecordId = metadata.clientRecordId,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private companion object {
    private const val TAG = "HydrationHealthReader"
    private const val MaxHydrationRecordLiters = 100.0
    private const val MillilitersPerLiter = 1000.0
  }
}

private fun String.toHydrationDrinkClientRecordSegment(): String? =
  trim()
    .filter { character -> character.isLetterOrDigit() || character == '-' }
    .takeIf { it.isNotBlank() }
