package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.celsius
import androidx.health.connect.client.units.millimetersOfMercury
import androidx.health.connect.client.units.percent
import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.BodyTempEntry
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.data.model.SpO2Entry
import tech.mmarca.openvitals.data.model.VitalsMeasurementType
import tech.mmarca.openvitals.data.model.VitalsMeasurementEntry
import tech.mmarca.openvitals.data.model.VitalsMeasurementWriteRequest
import tech.mmarca.openvitals.data.model.Vo2MaxEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class VitalsHealthReader(
    private val support: HealthConnectReaderSupport,
    private val appPackageName: String,
) {
    suspend fun readBloodPressureEntries(start: Instant, end: Instant): List<BloodPressureEntry> =
        support.withLogging("readBloodPressureEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = BloodPressureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 200,
            ).map { record ->
                BloodPressureEntry(
                    time = record.time,
                    systolicMmHg = record.systolic.inMillimetersOfMercury.toInt(),
                    diastolicMmHg = record.diastolic.inMillimetersOfMercury.toInt(),
                    source = record.metadata.dataOrigin.packageName,
                    id = record.metadata.id,
                    isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
                )
            }
        }

    suspend fun readLatestBloodPressure(date: LocalDate): BloodPressureEntry? {
        val (start, end) = support.dayRange(date)
        return support.withNullableLogging("readLatestBloodPressure[$date][$start..$end]") {
            support.client().readRecordsPaged(
                recordType = BloodPressureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.let { record ->
                BloodPressureEntry(
                    time = record.time,
                    systolicMmHg = record.systolic.inMillimetersOfMercury.toInt(),
                    diastolicMmHg = record.diastolic.inMillimetersOfMercury.toInt(),
                    source = record.metadata.dataOrigin.packageName,
                    id = record.metadata.id,
                    isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
                )
            }
        }
    }

    suspend fun readSpO2Entries(start: Instant, end: Instant): List<SpO2Entry> =
        support.withLogging("readSpO2Entries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 200,
            ).map { record ->
                SpO2Entry(
                    time = record.time,
                    percent = record.percentage.value,
                    source = record.metadata.dataOrigin.packageName,
                    id = record.metadata.id,
                    isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
                )
            }
        }

    suspend fun readLatestSpO2(date: LocalDate): SpO2Entry? {
        val (start, end) = support.dayRange(date)
        return support.withNullableLogging("readLatestSpO2[$date][$start..$end]") {
            support.client().readRecordsPaged(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.let { record ->
                SpO2Entry(
                    time = record.time,
                    percent = record.percentage.value,
                    source = record.metadata.dataOrigin.packageName,
                    id = record.metadata.id,
                    isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
                )
            }
        }
    }

    suspend fun readRespiratoryRateEntries(start: Instant, end: Instant): List<RespiratoryRateEntry> =
        support.withLogging("readRespiratoryRateEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = RespiratoryRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            ).map { record ->
                RespiratoryRateEntry(
                    time = record.time,
                    breathsPerMinute = record.rate,
                    source = record.metadata.dataOrigin.packageName,
                    id = record.metadata.id,
                    isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
                )
            }
        }

    suspend fun readBodyTemperatureEntries(start: Instant, end: Instant): List<BodyTempEntry> =
        support.withLogging("readBodyTemperatureEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = BodyTemperatureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 200,
            ).map { record ->
                BodyTempEntry(
                    time = record.time,
                    temperatureCelsius = record.temperature.inCelsius,
                    source = record.metadata.dataOrigin.packageName,
                    id = record.metadata.id,
                    isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
                )
            }
        }

    suspend fun readVo2MaxEntries(start: Instant, end: Instant): List<Vo2MaxEntry> =
        support.withLogging("readVo2MaxEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = Vo2MaxRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 200,
            ).map { record ->
                Vo2MaxEntry(
                    time = record.time,
                    vo2MaxMlPerKgPerMin = record.vo2MillilitersPerMinuteKilogram,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readLatestVo2Max(date: LocalDate): Vo2MaxEntry? {
        val (start, end) = support.dayRange(date)
        return support.withNullableLogging("readLatestVo2Max[$date][$start..$end]") {
            support.client().readRecordsPaged(
                recordType = Vo2MaxRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.let { record ->
                Vo2MaxEntry(
                    time = record.time,
                    vo2MaxMlPerKgPerMin = record.vo2MillilitersPerMinuteKilogram,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }
    }

    suspend fun writeVitalsMeasurementEntry(request: VitalsMeasurementWriteRequest): String = withContext(Dispatchers.IO) {
        validateVitalsMeasurement(request)

        val time = request.time
        val zone = ZoneId.systemDefault()
        val clientRecordId = "openvitals_vitals_${request.type.name.lowercase()}_${time.toEpochMilli()}_${UUID.randomUUID()}"
        val metadata = Metadata.manualEntry(
            device = Device(type = Device.TYPE_PHONE),
            clientRecordId = clientRecordId,
        )
        val record = when (request.type) {
            VitalsMeasurementType.BLOOD_PRESSURE -> BloodPressureRecord(
                time = time,
                zoneOffset = zone.rules.getOffset(time),
                metadata = metadata,
                systolic = request.value.millimetersOfMercury,
                diastolic = requireNotNull(request.secondaryValue).millimetersOfMercury,
            )
            VitalsMeasurementType.SPO2 -> OxygenSaturationRecord(
                time = time,
                zoneOffset = zone.rules.getOffset(time),
                percentage = request.value.percent,
                metadata = metadata,
            )
            VitalsMeasurementType.RESPIRATORY_RATE -> RespiratoryRateRecord(
                time = time,
                zoneOffset = zone.rules.getOffset(time),
                rate = request.value,
                metadata = metadata,
            )
            VitalsMeasurementType.BODY_TEMPERATURE -> BodyTemperatureRecord(
                time = time,
                zoneOffset = zone.rules.getOffset(time),
                metadata = metadata,
                temperature = request.value.celsius,
            )
        }

        support.client().insertRecords(listOf(record))
        clientRecordId
    }

    suspend fun readVitalsMeasurementEntry(type: VitalsMeasurementType, id: String): VitalsMeasurementEntry? =
        support.withNullableLogging("readVitalsMeasurementEntry[$type][$id]") {
            when (type) {
                VitalsMeasurementType.BLOOD_PRESSURE ->
                    support.client().readRecord(BloodPressureRecord::class, id).record.toVitalsMeasurementEntry()
                VitalsMeasurementType.SPO2 ->
                    support.client().readRecord(OxygenSaturationRecord::class, id).record.toVitalsMeasurementEntry()
                VitalsMeasurementType.RESPIRATORY_RATE ->
                    support.client().readRecord(RespiratoryRateRecord::class, id).record.toVitalsMeasurementEntry()
                VitalsMeasurementType.BODY_TEMPERATURE ->
                    support.client().readRecord(BodyTemperatureRecord::class, id).record.toVitalsMeasurementEntry()
            }
        }

    suspend fun updateVitalsMeasurementEntry(id: String, request: VitalsMeasurementWriteRequest) =
        withContext(Dispatchers.IO) {
            validateVitalsMeasurement(request)

            val existing: Record = when (request.type) {
                VitalsMeasurementType.BLOOD_PRESSURE -> support.client().readRecord(BloodPressureRecord::class, id).record
                VitalsMeasurementType.SPO2 -> support.client().readRecord(OxygenSaturationRecord::class, id).record
                VitalsMeasurementType.RESPIRATORY_RATE -> support.client().readRecord(RespiratoryRateRecord::class, id).record
                VitalsMeasurementType.BODY_TEMPERATURE -> support.client().readRecord(BodyTemperatureRecord::class, id).record
            }
            existing.requireOpenVitalsOrigin(appPackageName)

            val time = request.time
            val zone = ZoneId.systemDefault()
            val metadata = Metadata.manualEntryWithId(
                id = id,
                device = existing.metadata.device ?: Device(type = Device.TYPE_PHONE),
            )
            val record = when (request.type) {
                VitalsMeasurementType.BLOOD_PRESSURE -> BloodPressureRecord(
                    time = time,
                    zoneOffset = zone.rules.getOffset(time),
                    metadata = metadata,
                    systolic = request.value.millimetersOfMercury,
                    diastolic = requireNotNull(request.secondaryValue).millimetersOfMercury,
                )
                VitalsMeasurementType.SPO2 -> OxygenSaturationRecord(
                    time = time,
                    zoneOffset = zone.rules.getOffset(time),
                    percentage = request.value.percent,
                    metadata = metadata,
                )
                VitalsMeasurementType.RESPIRATORY_RATE -> RespiratoryRateRecord(
                    time = time,
                    zoneOffset = zone.rules.getOffset(time),
                    rate = request.value,
                    metadata = metadata,
                )
                VitalsMeasurementType.BODY_TEMPERATURE -> BodyTemperatureRecord(
                    time = time,
                    zoneOffset = zone.rules.getOffset(time),
                    metadata = metadata,
                    temperature = request.value.celsius,
                )
            }

            support.client().updateRecords(listOf(record))
        }

    suspend fun deleteVitalsMeasurementEntry(type: VitalsMeasurementType, id: String) = withContext(Dispatchers.IO) {
        when (type) {
            VitalsMeasurementType.BLOOD_PRESSURE -> {
                val existing = support.client().readRecord(BloodPressureRecord::class, id).record
                existing.requireOpenVitalsOrigin(appPackageName)
                support.client().deleteRecords(
                    recordType = BloodPressureRecord::class,
                    recordIdsList = listOf(existing.metadata.id),
                    clientRecordIdsList = emptyList(),
                )
            }
            VitalsMeasurementType.SPO2 -> {
                val existing = support.client().readRecord(OxygenSaturationRecord::class, id).record
                existing.requireOpenVitalsOrigin(appPackageName)
                support.client().deleteRecords(
                    recordType = OxygenSaturationRecord::class,
                    recordIdsList = listOf(existing.metadata.id),
                    clientRecordIdsList = emptyList(),
                )
            }
            VitalsMeasurementType.RESPIRATORY_RATE -> {
                val existing = support.client().readRecord(RespiratoryRateRecord::class, id).record
                existing.requireOpenVitalsOrigin(appPackageName)
                support.client().deleteRecords(
                    recordType = RespiratoryRateRecord::class,
                    recordIdsList = listOf(existing.metadata.id),
                    clientRecordIdsList = emptyList(),
                )
            }
            VitalsMeasurementType.BODY_TEMPERATURE -> {
                val existing = support.client().readRecord(BodyTemperatureRecord::class, id).record
                existing.requireOpenVitalsOrigin(appPackageName)
                support.client().deleteRecords(
                    recordType = BodyTemperatureRecord::class,
                    recordIdsList = listOf(existing.metadata.id),
                    clientRecordIdsList = emptyList(),
                )
            }
        }
    }

    private fun validateVitalsMeasurement(request: VitalsMeasurementWriteRequest) {
        when (request.type) {
            VitalsMeasurementType.BLOOD_PRESSURE -> {
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
            VitalsMeasurementType.SPO2 -> require(request.value > 0.0 && request.value <= MaxPercent) {
                "SpO2 must be greater than 0% and no more than ${MaxPercent.toInt()}%."
            }
            VitalsMeasurementType.RESPIRATORY_RATE -> require(request.value > 0.0 && request.value <= MaxRespiratoryRate) {
                "Respiratory rate must be greater than 0 and no more than ${MaxRespiratoryRate.toInt()} breaths/min."
            }
            VitalsMeasurementType.BODY_TEMPERATURE -> require(
                request.value > 0.0 && request.value <= MaxBodyTemperatureCelsius
            ) {
                "Body temperature must be greater than 0 C and no more than ${MaxBodyTemperatureCelsius.toInt()} C."
            }
        }
    }

    private fun BloodPressureRecord.toVitalsMeasurementEntry(): VitalsMeasurementEntry =
        VitalsMeasurementEntry(
            id = metadata.id,
            type = VitalsMeasurementType.BLOOD_PRESSURE,
            time = time,
            value = systolic.inMillimetersOfMercury,
            secondaryValue = diastolic.inMillimetersOfMercury,
            source = metadata.dataOrigin.packageName,
            isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
        )

    private fun OxygenSaturationRecord.toVitalsMeasurementEntry(): VitalsMeasurementEntry =
        VitalsMeasurementEntry(
            id = metadata.id,
            type = VitalsMeasurementType.SPO2,
            time = time,
            value = percentage.value,
            source = metadata.dataOrigin.packageName,
            isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
        )

    private fun RespiratoryRateRecord.toVitalsMeasurementEntry(): VitalsMeasurementEntry =
        VitalsMeasurementEntry(
            id = metadata.id,
            type = VitalsMeasurementType.RESPIRATORY_RATE,
            time = time,
            value = rate,
            source = metadata.dataOrigin.packageName,
            isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
        )

    private fun BodyTemperatureRecord.toVitalsMeasurementEntry(): VitalsMeasurementEntry =
        VitalsMeasurementEntry(
            id = metadata.id,
            type = VitalsMeasurementType.BODY_TEMPERATURE,
            time = time,
            value = temperature.inCelsius,
            source = metadata.dataOrigin.packageName,
            isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
        )
}

private const val MinSystolicMmHg = 20.0
private const val MaxSystolicMmHg = 200.0
private const val MinDiastolicMmHg = 10.0
private const val MaxDiastolicMmHg = 180.0
private const val MaxPercent = 100.0
private const val MaxRespiratoryRate = 1000.0
private const val MaxBodyTemperatureCelsius = 100.0
