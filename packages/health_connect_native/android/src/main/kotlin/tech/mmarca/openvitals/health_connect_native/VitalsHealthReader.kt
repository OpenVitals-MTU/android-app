package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.celsius
import androidx.health.connect.client.units.millimetersOfMercury
import androidx.health.connect.client.units.percent
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ported from the native OpenVitals app (`healthconnect/VitalsHealthReader.kt`).
 * Returns Pigeon `*Msg` types. BloodPressure/SpO2/RespiratoryRate/BodyTemperature
 * are writable; Vo2Max/BloodGlucose/SkinTemperature are read-only.
 */
internal class VitalsHealthReader(
  private val support: HealthConnectReaderSupport,
  private val appPackageName: String,
) {
  suspend fun readBloodPressureEntries(start: Instant, end: Instant): List<BloodPressureEntryMsg> =
    support.withLogging("readBloodPressureEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = BloodPressureRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 200,
      ).map { it.toMsg() }
    }

  suspend fun readLatestBloodPressure(start: Instant, end: Instant): BloodPressureEntryMsg? =
    support.withNullableLogging("readLatestBloodPressure[$start..$end]") {
      support.client().readRecordsPaged(
        recordType = BloodPressureRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 1,
        maxRecords = 1,
      ).firstOrNull()?.toMsg()
    }

  suspend fun readSpO2Entries(start: Instant, end: Instant): List<SpO2EntryMsg> =
    support.withLogging("readSpO2Entries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = OxygenSaturationRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 200,
      ).map { it.toMsg() }
    }

  suspend fun readLatestSpO2(start: Instant, end: Instant): SpO2EntryMsg? =
    support.withNullableLogging("readLatestSpO2[$start..$end]") {
      support.client().readRecordsPaged(
        recordType = OxygenSaturationRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 1,
        maxRecords = 1,
      ).firstOrNull()?.toMsg()
    }

  suspend fun readRespiratoryRateEntries(start: Instant, end: Instant): List<RespiratoryRateEntryMsg> =
    support.withLogging("readRespiratoryRateEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = RespiratoryRateRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
      ).map { it.toMsg() }
    }

  suspend fun readBodyTemperatureEntries(start: Instant, end: Instant): List<BodyTempEntryMsg> =
    support.withLogging("readBodyTemperatureEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = BodyTemperatureRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 200,
      ).map { it.toMsg() }
    }

  suspend fun readVo2MaxEntries(start: Instant, end: Instant): List<Vo2MaxEntryMsg> =
    support.withLogging("readVo2MaxEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = Vo2MaxRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 200,
      ).map { it.toMsg() }
    }

  suspend fun readLatestVo2Max(start: Instant, end: Instant): Vo2MaxEntryMsg? =
    support.withNullableLogging("readLatestVo2Max[$start..$end]") {
      support.client().readRecordsPaged(
        recordType = Vo2MaxRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 1,
        maxRecords = 1,
      ).firstOrNull()?.toMsg()
    }

  suspend fun readBloodGlucoseEntries(start: Instant, end: Instant): List<BloodGlucoseEntryMsg> =
    support.withLogging("readBloodGlucoseEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = BloodGlucoseRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 200,
      ).map { record ->
        BloodGlucoseEntryMsg(
          timeEpochMs = record.time.toEpochMilli(),
          millimolesPerLiter = record.level.inMillimolesPerLiter,
          specimenSource = record.specimenSource.toLong(),
          mealType = record.mealType.toLong(),
          relationToMeal = record.relationToMeal.toLong(),
          source = record.metadata.dataOrigin.packageName,
        )
      }
    }

  suspend fun readSkinTemperatureEntries(start: Instant, end: Instant): List<SkinTemperatureEntryMsg> =
    support.withLogging("readSkinTemperatureEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = SkinTemperatureRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 200,
      ).map { record ->
        val deltasCelsius = record.deltas.map { it.delta.inCelsius }
        SkinTemperatureEntryMsg(
          startEpochMs = record.startTime.toEpochMilli(),
          endEpochMs = record.endTime.toEpochMilli(),
          baselineCelsius = record.baseline?.inCelsius,
          averageDeltaCelsius = deltasCelsius.averageOrNull(),
          minDeltaCelsius = deltasCelsius.minOrNull(),
          maxDeltaCelsius = deltasCelsius.maxOrNull(),
          measurementLocation = record.measurementLocation.toLong(),
          source = record.metadata.dataOrigin.packageName,
        )
      }
    }

  suspend fun writeVitalsMeasurementEntry(request: VitalsMeasurementWriteRequestMsg): String =
    withContext(Dispatchers.IO) {
      support.requireSyncEnabled()
      validate(request)
      val time = Instant.ofEpochMilli(request.timeEpochMs)
      val clientRecordId =
        "openvitals_vitals_${request.type.name.lowercase()}_${time.toEpochMilli()}_${UUID.randomUUID()}"
      val metadata = Metadata.manualEntry(
        clientRecordId = clientRecordId,
        device = Device(type = Device.TYPE_PHONE),
      )
      support.client().insertRecords(listOf(buildRecord(request, time, metadata)))
      clientRecordId
    }

  suspend fun readVitalsMeasurementEntry(
    type: VitalsMeasurementTypeMsg,
    id: String,
  ): VitalsMeasurementEntryMsg? =
    support.withNullableLogging("readVitalsMeasurementEntry[$type][$id]") {
      when (type) {
        VitalsMeasurementTypeMsg.BLOOD_PRESSURE ->
          support.client().readRecord(BloodPressureRecord::class, id).record.toMeasurementMsg()
        VitalsMeasurementTypeMsg.SPO2 ->
          support.client().readRecord(OxygenSaturationRecord::class, id).record.toMeasurementMsg()
        VitalsMeasurementTypeMsg.RESPIRATORY_RATE ->
          support.client().readRecord(RespiratoryRateRecord::class, id).record.toMeasurementMsg()
        VitalsMeasurementTypeMsg.BODY_TEMPERATURE ->
          support.client().readRecord(BodyTemperatureRecord::class, id).record.toMeasurementMsg()
      }
    }

  suspend fun updateVitalsMeasurementEntry(id: String, request: VitalsMeasurementWriteRequestMsg) =
    withContext(Dispatchers.IO) {
      support.requireSyncEnabled()
      validate(request)
      val existing: Record = when (request.type) {
        VitalsMeasurementTypeMsg.BLOOD_PRESSURE -> support.client().readRecord(BloodPressureRecord::class, id).record
        VitalsMeasurementTypeMsg.SPO2 -> support.client().readRecord(OxygenSaturationRecord::class, id).record
        VitalsMeasurementTypeMsg.RESPIRATORY_RATE -> support.client().readRecord(RespiratoryRateRecord::class, id).record
        VitalsMeasurementTypeMsg.BODY_TEMPERATURE -> support.client().readRecord(BodyTemperatureRecord::class, id).record
      }
      existing.requireOpenVitalsOrigin(appPackageName)
      val time = Instant.ofEpochMilli(request.timeEpochMs)
      val metadata = Metadata.manualEntryWithId(
        id = id,
        device = existing.metadata.device ?: Device(type = Device.TYPE_PHONE),
      )
      support.client().updateRecords(listOf(buildRecord(request, time, metadata)))
    }

  suspend fun deleteVitalsMeasurementEntry(type: VitalsMeasurementTypeMsg, id: String) =
    withContext(Dispatchers.IO) {
      support.requireSyncEnabled()
      val existing: Record = when (type) {
        VitalsMeasurementTypeMsg.BLOOD_PRESSURE -> support.client().readRecord(BloodPressureRecord::class, id).record
        VitalsMeasurementTypeMsg.SPO2 -> support.client().readRecord(OxygenSaturationRecord::class, id).record
        VitalsMeasurementTypeMsg.RESPIRATORY_RATE -> support.client().readRecord(RespiratoryRateRecord::class, id).record
        VitalsMeasurementTypeMsg.BODY_TEMPERATURE -> support.client().readRecord(BodyTemperatureRecord::class, id).record
      }
      existing.requireOpenVitalsOrigin(appPackageName)
      val recordType = when (type) {
        VitalsMeasurementTypeMsg.BLOOD_PRESSURE -> BloodPressureRecord::class
        VitalsMeasurementTypeMsg.SPO2 -> OxygenSaturationRecord::class
        VitalsMeasurementTypeMsg.RESPIRATORY_RATE -> RespiratoryRateRecord::class
        VitalsMeasurementTypeMsg.BODY_TEMPERATURE -> BodyTemperatureRecord::class
      }
      support.client().deleteRecords(
        recordType = recordType,
        recordIdsList = listOf(existing.metadata.id),
        clientRecordIdsList = emptyList(),
      )
    }

  private fun buildRecord(
    request: VitalsMeasurementWriteRequestMsg,
    time: Instant,
    metadata: Metadata,
  ): Record {
    val offset = ZoneId.systemDefault().rules.getOffset(time)
    return when (request.type) {
      VitalsMeasurementTypeMsg.BLOOD_PRESSURE -> BloodPressureRecord(
        time = time,
        zoneOffset = offset,
        metadata = metadata,
        systolic = request.value.millimetersOfMercury,
        diastolic = requireNotNull(request.secondaryValue).millimetersOfMercury,
      )
      VitalsMeasurementTypeMsg.SPO2 -> OxygenSaturationRecord(
        time = time,
        zoneOffset = offset,
        percentage = request.value.percent,
        metadata = metadata,
      )
      VitalsMeasurementTypeMsg.RESPIRATORY_RATE -> RespiratoryRateRecord(
        time = time,
        zoneOffset = offset,
        rate = request.value,
        metadata = metadata,
      )
      VitalsMeasurementTypeMsg.BODY_TEMPERATURE -> BodyTemperatureRecord(
        time = time,
        zoneOffset = offset,
        metadata = metadata,
        temperature = request.value.celsius,
      )
    }
  }

  private fun validate(request: VitalsMeasurementWriteRequestMsg) {
    when (request.type) {
      VitalsMeasurementTypeMsg.BLOOD_PRESSURE -> {
        val diastolic = requireNotNull(request.secondaryValue) {
          "Blood pressure requires systolic and diastolic values."
        }
        require(request.value >= MinSystolicMmHg && request.value <= MaxSystolicMmHg) {
          "Systolic blood pressure must be between ${MinSystolicMmHg.toInt()} and ${MaxSystolicMmHg.toInt()} mmHg."
        }
        require(diastolic >= MinDiastolicMmHg && diastolic <= MaxDiastolicMmHg) {
          "Diastolic blood pressure must be between ${MinDiastolicMmHg.toInt()} and ${MaxDiastolicMmHg.toInt()} mmHg."
        }
        require(request.value > diastolic) {
          "Systolic blood pressure must be higher than diastolic blood pressure."
        }
      }
      VitalsMeasurementTypeMsg.SPO2 -> require(request.value > 0.0 && request.value <= MaxPercent) {
        "SpO2 must be greater than 0% and no more than ${MaxPercent.toInt()}%."
      }
      VitalsMeasurementTypeMsg.RESPIRATORY_RATE -> require(request.value > 0.0 && request.value <= MaxRespiratoryRate) {
        "Respiratory rate must be greater than 0 and no more than ${MaxRespiratoryRate.toInt()} breaths/min."
      }
      VitalsMeasurementTypeMsg.BODY_TEMPERATURE -> require(request.value > 0.0 && request.value <= MaxBodyTemperatureCelsius) {
        "Body temperature must be greater than 0 C and no more than ${MaxBodyTemperatureCelsius.toInt()} C."
      }
    }
  }

  private fun BloodPressureRecord.toMsg() = BloodPressureEntryMsg(
    timeEpochMs = time.toEpochMilli(),
    systolicMmHg = systolic.inMillimetersOfMercury.toLong(),
    diastolicMmHg = diastolic.inMillimetersOfMercury.toLong(),
    source = metadata.dataOrigin.packageName,
    id = metadata.id,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private fun OxygenSaturationRecord.toMsg() = SpO2EntryMsg(
    timeEpochMs = time.toEpochMilli(),
    percent = percentage.value,
    source = metadata.dataOrigin.packageName,
    id = metadata.id,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private fun RespiratoryRateRecord.toMsg() = RespiratoryRateEntryMsg(
    timeEpochMs = time.toEpochMilli(),
    breathsPerMinute = rate,
    source = metadata.dataOrigin.packageName,
    id = metadata.id,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private fun BodyTemperatureRecord.toMsg() = BodyTempEntryMsg(
    timeEpochMs = time.toEpochMilli(),
    temperatureCelsius = temperature.inCelsius,
    source = metadata.dataOrigin.packageName,
    id = metadata.id,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private fun Vo2MaxRecord.toMsg() = Vo2MaxEntryMsg(
    timeEpochMs = time.toEpochMilli(),
    vo2MaxMlPerKgPerMin = vo2MillilitersPerMinuteKilogram,
    source = metadata.dataOrigin.packageName,
  )

  private fun BloodPressureRecord.toMeasurementMsg() = VitalsMeasurementEntryMsg(
    id = metadata.id,
    type = VitalsMeasurementTypeMsg.BLOOD_PRESSURE,
    timeEpochMs = time.toEpochMilli(),
    value = systolic.inMillimetersOfMercury,
    secondaryValue = diastolic.inMillimetersOfMercury,
    source = metadata.dataOrigin.packageName,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private fun OxygenSaturationRecord.toMeasurementMsg() = VitalsMeasurementEntryMsg(
    id = metadata.id,
    type = VitalsMeasurementTypeMsg.SPO2,
    timeEpochMs = time.toEpochMilli(),
    value = percentage.value,
    secondaryValue = null,
    source = metadata.dataOrigin.packageName,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private fun RespiratoryRateRecord.toMeasurementMsg() = VitalsMeasurementEntryMsg(
    id = metadata.id,
    type = VitalsMeasurementTypeMsg.RESPIRATORY_RATE,
    timeEpochMs = time.toEpochMilli(),
    value = rate,
    secondaryValue = null,
    source = metadata.dataOrigin.packageName,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private fun BodyTemperatureRecord.toMeasurementMsg() = VitalsMeasurementEntryMsg(
    id = metadata.id,
    type = VitalsMeasurementTypeMsg.BODY_TEMPERATURE,
    timeEpochMs = time.toEpochMilli(),
    value = temperature.inCelsius,
    secondaryValue = null,
    source = metadata.dataOrigin.packageName,
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
  )

  private companion object {
    private const val MinSystolicMmHg = 20.0
    private const val MaxSystolicMmHg = 200.0
    private const val MinDiastolicMmHg = 10.0
    private const val MaxDiastolicMmHg = 180.0
    private const val MaxPercent = 100.0
    private const val MaxRespiratoryRate = 1000.0
    private const val MaxBodyTemperatureCelsius = 100.0
  }
}

private fun List<Double>.averageOrNull(): Double? = takeIf { it.isNotEmpty() }?.average()
