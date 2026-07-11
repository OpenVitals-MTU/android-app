package tech.mmarca.openvitals.health_connect_native

import android.util.Log
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.kilograms
import androidx.health.connect.client.units.meters
import androidx.health.connect.client.units.percent
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ported from the native OpenVitals app (`healthconnect/BodyHealthReader.kt`).
 *
 * Reads/writes body-measurement records and returns Pigeon `*Msg` types (which
 * the Dart boundary maps to the app's freezed domain models). Ownership is
 * tagged on read (`isOpenVitalsEntry`) and enforced on update/delete
 * ([requireOpenVitalsOrigin]).
 */
internal class BodyHealthReader(
  private val support: HealthConnectReaderSupport,
  private val appPackageName: String,
) {
  suspend fun readLatestWeight(): WeightEntryMsg? =
    support.withNullableLogging("readLatestWeight") {
      support.client().readRecordsPaged(
        recordType = WeightRecord::class,
        timeRangeFilter = TimeRangeFilter.before(Instant.now()),
        ascendingOrder = false,
        pageSize = 1,
        maxRecords = 1,
      ).firstOrNull()?.toMsg()
    }

  suspend fun readWeightEntries(start: Instant, end: Instant): List<WeightEntryMsg> =
    support.withLogging("readWeightEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = WeightRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
      ).map { it.toMsg() }
    }

  suspend fun readLatestHeightEntry(): HeightEntryMsg? =
    support.withNullableLogging("readLatestHeight") {
      support.client().readRecordsPaged(
        recordType = HeightRecord::class,
        timeRangeFilter = TimeRangeFilter.before(Instant.now()),
        ascendingOrder = false,
        pageSize = 1,
        maxRecords = 1,
      ).firstOrNull()?.toMsg()
    }

  suspend fun readHeightEntries(start: Instant, end: Instant): List<HeightEntryMsg> =
    support.withLogging("readHeightEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = HeightRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
      ).map { it.toMsg() }
    }

  suspend fun readLatestBodyFat(): BodyFatEntryMsg? =
    support.withNullableLogging("readLatestBodyFat") {
      support.client().readRecordsPaged(
        recordType = BodyFatRecord::class,
        timeRangeFilter = TimeRangeFilter.before(Instant.now()),
        ascendingOrder = false,
        pageSize = 1,
        maxRecords = 1,
      ).firstOrNull()?.toMsg()
    }

  suspend fun readBodyFatEntries(start: Instant, end: Instant): List<BodyFatEntryMsg> =
    support.withLogging("readBodyFatEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = BodyFatRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
      ).map { it.toMsg() }
    }

  suspend fun readLatestLeanBodyMass(): BodyMassEntryMsg? =
    support.withNullableLogging("readLatestLeanBodyMass") {
      support.client().readRecordsPaged(
        recordType = LeanBodyMassRecord::class,
        timeRangeFilter = TimeRangeFilter.before(Instant.now()),
        ascendingOrder = false,
        pageSize = 1,
        maxRecords = 1,
      ).firstOrNull()?.let { massMsg(it.time, it.mass.inKilograms, it.metadata.dataOrigin.packageName) }
    }

  suspend fun readLeanBodyMassEntries(start: Instant, end: Instant): List<BodyMassEntryMsg> =
    support.withLogging("readLeanBodyMassEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = LeanBodyMassRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
      ).map { massMsg(it.time, it.mass.inKilograms, it.metadata.dataOrigin.packageName) }
    }

  suspend fun readLatestBmr(): BmrEntryMsg? =
    support.withNullableLogging("readLatestBMR") {
      support.client().readRecordsPaged(
        recordType = BasalMetabolicRateRecord::class,
        timeRangeFilter = TimeRangeFilter.before(Instant.now()),
        ascendingOrder = false,
        pageSize = 1,
        maxRecords = 1,
      ).firstOrNull()?.let {
        BmrEntryMsg(
          timeEpochMs = it.time.toEpochMilli(),
          kcalPerDay = it.basalMetabolicRate.inKilocaloriesPerDay,
          source = it.metadata.dataOrigin.packageName,
        )
      }
    }

  suspend fun readBmrEntries(start: Instant, end: Instant): List<BmrEntryMsg> =
    support.withLogging("readBmrEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = BasalMetabolicRateRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
      ).map {
        BmrEntryMsg(
          timeEpochMs = it.time.toEpochMilli(),
          kcalPerDay = it.basalMetabolicRate.inKilocaloriesPerDay,
          source = it.metadata.dataOrigin.packageName,
        )
      }
    }

  suspend fun readLatestBoneMass(): BodyMassEntryMsg? =
    support.withNullableLogging("readLatestBoneMass") {
      support.client().readRecordsPaged(
        recordType = BoneMassRecord::class,
        timeRangeFilter = TimeRangeFilter.before(Instant.now()),
        ascendingOrder = false,
        pageSize = 1,
        maxRecords = 1,
      ).firstOrNull()?.let { massMsg(it.time, it.mass.inKilograms, it.metadata.dataOrigin.packageName) }
    }

  suspend fun readBoneMassEntries(start: Instant, end: Instant): List<BodyMassEntryMsg> =
    support.withLogging("readBoneMassEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = BoneMassRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
      ).map { massMsg(it.time, it.mass.inKilograms, it.metadata.dataOrigin.packageName) }
    }

  suspend fun readLatestBodyWaterMass(): BodyMassEntryMsg? =
    support.withNullableLogging("readLatestBodyWaterMass") {
      support.client().readRecordsPaged(
        recordType = BodyWaterMassRecord::class,
        timeRangeFilter = TimeRangeFilter.before(Instant.now()),
        ascendingOrder = false,
        pageSize = 1,
        maxRecords = 1,
      ).firstOrNull()?.let { massMsg(it.time, it.mass.inKilograms, it.metadata.dataOrigin.packageName) }
    }

  suspend fun readBodyWaterMassEntries(start: Instant, end: Instant): List<BodyMassEntryMsg> =
    support.withLogging("readBodyWaterMassEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = BodyWaterMassRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
      ).map { massMsg(it.time, it.mass.inKilograms, it.metadata.dataOrigin.packageName) }
    }

  suspend fun writeBodyMeasurementEntry(request: BodyMeasurementWriteRequestMsg): String =
    withContext(Dispatchers.IO) {
      support.requireSyncEnabled()
      validate(request)
      val time = Instant.ofEpochMilli(request.timeEpochMs)
      val clientRecordId =
        "openvitals_body_${request.type.name.lowercase()}_${time.toEpochMilli()}_${UUID.randomUUID()}"
      val metadata = Metadata.manualEntry(
        clientRecordId = clientRecordId,
        device = Device(type = Device.TYPE_PHONE),
      )
      Log.d(TAG, "Writing body record type=${request.type} ${support.diagnosticsSummary()}")
      support.client().insertRecords(listOf(buildRecord(request, time, metadata)))
      clientRecordId
    }

  suspend fun readBodyMeasurementEntry(
    type: BodyMeasurementTypeMsg,
    id: String,
  ): BodyMeasurementEntryMsg? =
    support.withNullableLogging("readBodyMeasurementEntry[$type][$id]") {
      when (type) {
        BodyMeasurementTypeMsg.WEIGHT ->
          support.client().readRecord(WeightRecord::class, id).record.toMeasurementMsg()
        BodyMeasurementTypeMsg.HEIGHT ->
          support.client().readRecord(HeightRecord::class, id).record.toMeasurementMsg()
        BodyMeasurementTypeMsg.BODY_FAT ->
          support.client().readRecord(BodyFatRecord::class, id).record.toMeasurementMsg()
      }
    }

  suspend fun updateBodyMeasurementEntry(id: String, request: BodyMeasurementWriteRequestMsg) =
    withContext(Dispatchers.IO) {
      support.requireSyncEnabled()
      validate(request)
      val existing: Record = when (request.type) {
        BodyMeasurementTypeMsg.WEIGHT -> support.client().readRecord(WeightRecord::class, id).record
        BodyMeasurementTypeMsg.HEIGHT -> support.client().readRecord(HeightRecord::class, id).record
        BodyMeasurementTypeMsg.BODY_FAT -> support.client().readRecord(BodyFatRecord::class, id).record
      }
      existing.requireOpenVitalsOrigin(appPackageName)
      val time = Instant.ofEpochMilli(request.timeEpochMs)
      val metadata = Metadata.manualEntryWithId(
        id = id,
        device = existing.metadata.device ?: Device(type = Device.TYPE_PHONE),
      )
      Log.d(TAG, "Updating body record type=${request.type} ${support.diagnosticsSummary()}")
      support.client().updateRecords(listOf(buildRecord(request, time, metadata)))
    }

  suspend fun deleteBodyMeasurementEntry(type: BodyMeasurementTypeMsg, id: String) =
    withContext(Dispatchers.IO) {
      support.requireSyncEnabled()
      val existing: Record = when (type) {
        BodyMeasurementTypeMsg.WEIGHT -> support.client().readRecord(WeightRecord::class, id).record
        BodyMeasurementTypeMsg.HEIGHT -> support.client().readRecord(HeightRecord::class, id).record
        BodyMeasurementTypeMsg.BODY_FAT -> support.client().readRecord(BodyFatRecord::class, id).record
      }
      existing.requireOpenVitalsOrigin(appPackageName)
      val recordType = when (type) {
        BodyMeasurementTypeMsg.WEIGHT -> WeightRecord::class
        BodyMeasurementTypeMsg.HEIGHT -> HeightRecord::class
        BodyMeasurementTypeMsg.BODY_FAT -> BodyFatRecord::class
      }
      support.client().deleteRecords(
        recordType = recordType,
        recordIdsList = listOf(existing.metadata.id),
        clientRecordIdsList = emptyList(),
      )
    }

  // ── mapping helpers ──

  private fun WeightRecord.toMsg() = WeightEntryMsg(
    timeEpochMs = time.toEpochMilli(),
    weightKg = weight.inKilograms,
    source = metadata.dataOrigin.packageName,
    id = metadata.id,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private fun HeightRecord.toMsg() = HeightEntryMsg(
    timeEpochMs = time.toEpochMilli(),
    heightCm = height.inMeters * CentimetersPerMeter,
    source = metadata.dataOrigin.packageName,
    id = metadata.id,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private fun BodyFatRecord.toMsg() = BodyFatEntryMsg(
    timeEpochMs = time.toEpochMilli(),
    percent = percentage.value,
    source = metadata.dataOrigin.packageName,
    id = metadata.id,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private fun massMsg(time: Instant, massKg: Double, source: String) =
    BodyMassEntryMsg(timeEpochMs = time.toEpochMilli(), massKg = massKg, source = source)

  private fun buildRecord(
    request: BodyMeasurementWriteRequestMsg,
    time: Instant,
    metadata: Metadata,
  ): Record {
    val zone = ZoneId.systemDefault()
    val offset = zone.rules.getOffset(time)
    return when (request.type) {
      BodyMeasurementTypeMsg.WEIGHT -> WeightRecord(
        time = time,
        zoneOffset = offset,
        weight = request.value.kilograms,
        metadata = metadata,
      )
      BodyMeasurementTypeMsg.HEIGHT -> HeightRecord(
        time = time,
        zoneOffset = offset,
        height = (request.value / CentimetersPerMeter).meters,
        metadata = metadata,
      )
      BodyMeasurementTypeMsg.BODY_FAT -> BodyFatRecord(
        time = time,
        zoneOffset = offset,
        percentage = request.value.percent,
        metadata = metadata,
      )
    }
  }

  private fun WeightRecord.toMeasurementMsg() = BodyMeasurementEntryMsg(
    id = metadata.id,
    type = BodyMeasurementTypeMsg.WEIGHT,
    timeEpochMs = time.toEpochMilli(),
    value = weight.inKilograms,
    source = metadata.dataOrigin.packageName,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private fun HeightRecord.toMeasurementMsg() = BodyMeasurementEntryMsg(
    id = metadata.id,
    type = BodyMeasurementTypeMsg.HEIGHT,
    timeEpochMs = time.toEpochMilli(),
    value = height.inMeters * CentimetersPerMeter,
    source = metadata.dataOrigin.packageName,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private fun BodyFatRecord.toMeasurementMsg() = BodyMeasurementEntryMsg(
    id = metadata.id,
    type = BodyMeasurementTypeMsg.BODY_FAT,
    timeEpochMs = time.toEpochMilli(),
    value = percentage.value,
    source = metadata.dataOrigin.packageName,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private fun validate(request: BodyMeasurementWriteRequestMsg) {
    when (request.type) {
      BodyMeasurementTypeMsg.WEIGHT -> require(request.value > 0.0 && request.value <= MaxWeightKg) {
        "Weight must be greater than 0 kg and no more than ${MaxWeightKg.toInt()} kg."
      }
      BodyMeasurementTypeMsg.HEIGHT -> require(request.value > 0.0 && request.value <= MaxHeightCm) {
        "Height must be greater than 0 cm and no more than ${MaxHeightCm.toInt()} cm."
      }
      BodyMeasurementTypeMsg.BODY_FAT -> require(request.value >= 0.0 && request.value <= MaxBodyFatPercent) {
        "Body fat must be between 0% and ${MaxBodyFatPercent.toInt()}%."
      }
    }
  }

  private companion object {
    private const val TAG = "BodyHealthReader"
    private const val CentimetersPerMeter = 100.0
    private const val MaxWeightKg = 1000.0
    private const val MaxHeightCm = 300.0
    private const val MaxBodyFatPercent = 100.0
  }
}
