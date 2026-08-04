package tech.mmarca.openvitals.healthconnect

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
import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.DailyBloodPressurePoint
import tech.mmarca.openvitals.domain.model.DailyVitalPoint
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementEntry
import tech.mmarca.openvitals.domain.model.VitalsMeasurementWriteRequest
import tech.mmarca.openvitals.domain.model.Vo2MaxEntry
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
            ).map { it.toEntry() }
        }

    suspend fun readLatestBloodPressure(date: LocalDate): BloodPressureEntry? {
        val (start, end) = support.dayRange(date)
        return readLatestBloodPressureInWindow(start, end)
    }

    suspend fun readLatestBloodPressureInWindow(start: Instant, end: Instant): BloodPressureEntry? =
        support.withNullableLogging("readLatestBloodPressure[$start..$end]") {
            latestRecord(BloodPressureRecord::class, start, end)?.toEntry()
        }

    suspend fun readSpO2Entries(start: Instant, end: Instant): List<SpO2Entry> =
        support.withLogging("readSpO2Entries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 200,
            ).map { it.toEntry() }
        }

    suspend fun readLatestSpO2(date: LocalDate): SpO2Entry? {
        val (start, end) = support.dayRange(date)
        return readLatestSpO2InWindow(start, end)
    }

    suspend fun readLatestSpO2InWindow(start: Instant, end: Instant): SpO2Entry? =
        support.withNullableLogging("readLatestSpO2[$start..$end]") {
            latestRecord(OxygenSaturationRecord::class, start, end)?.toEntry()
        }

    suspend fun readRespiratoryRateEntries(start: Instant, end: Instant): List<RespiratoryRateEntry> =
        support.withLogging("readRespiratoryRateEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = RespiratoryRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            ).map { it.toEntry() }
        }

    suspend fun readLatestRespiratoryRateInWindow(start: Instant, end: Instant): RespiratoryRateEntry? =
        support.withNullableLogging("readLatestRespiratoryRate[$start..$end]") {
            latestRecord(RespiratoryRateRecord::class, start, end)?.toEntry()
        }

    suspend fun readBodyTemperatureEntries(start: Instant, end: Instant): List<BodyTempEntry> =
        support.withLogging("readBodyTemperatureEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = BodyTemperatureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 200,
            ).map { it.toEntry() }
        }

    suspend fun readLatestBodyTemperatureInWindow(start: Instant, end: Instant): BodyTempEntry? =
        support.withNullableLogging("readLatestBodyTemperature[$start..$end]") {
            latestRecord(BodyTemperatureRecord::class, start, end)?.toEntry()
        }

    suspend fun readVo2MaxEntries(start: Instant, end: Instant): List<Vo2MaxEntry> =
        support.withLogging("readVo2MaxEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = Vo2MaxRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 200,
            ).map { it.toEntry() }
        }

    suspend fun readBloodGlucoseEntries(start: Instant, end: Instant): List<BloodGlucoseEntry> =
        support.withLogging("readBloodGlucoseEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = BloodGlucoseRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 200,
            ).map { it.toEntry() }
        }

    suspend fun readLatestBloodGlucoseInWindow(start: Instant, end: Instant): BloodGlucoseEntry? =
        support.withNullableLogging("readLatestBloodGlucose[$start..$end]") {
            latestRecord(BloodGlucoseRecord::class, start, end)?.toEntry()
        }

    suspend fun readSkinTemperatureEntries(start: Instant, end: Instant): List<SkinTemperatureEntry> =
        support.withLogging("readSkinTemperatureEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = SkinTemperatureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 200,
            ).map { it.toEntry() }
        }

    suspend fun readLatestSkinTemperatureInWindow(start: Instant, end: Instant): SkinTemperatureEntry? =
        support.withNullableLogging("readLatestSkinTemperature[$start..$end]") {
            latestRecord(SkinTemperatureRecord::class, start, end)?.toEntry()
        }

    suspend fun readLatestVo2Max(date: LocalDate): Vo2MaxEntry? {
        val (start, end) = support.dayRange(date)
        return readLatestVo2MaxInWindow(start, end)
    }

    suspend fun readLatestVo2MaxInWindow(start: Instant, end: Instant): Vo2MaxEntry? =
        support.withNullableLogging("readLatestVo2Max[$start..$end]") {
            latestRecord(Vo2MaxRecord::class, start, end)?.toEntry()
        }

    // ── Daily aggregates for long-range charts ─────────────────────────────────
    // Health Connect exposes no AVG aggregate metric for these record types, so —
    // like HeartHealthReader.readDailyHRV — the raw records are read once and
    // bucketed by local date here, producing one point per day (plus its reading
    // count) instead of a season of raw entries held in memory per metric.

    suspend fun readDailyBloodPressure(start: Instant, end: Instant): List<DailyBloodPressurePoint> =
        support.withLogging("readDailyBloodPressure[$start..$end]", emptyList()) {
            val zone = ZoneId.systemDefault()
            support.client().readRecordsPaged(
                recordType = BloodPressureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = DailyReadPageSize,
            ).groupBy { it.time.atZone(zone).toLocalDate() }
                .mapNotNull { (date, records) ->
                    if (records.isEmpty()) return@mapNotNull null
                    DailyBloodPressurePoint(
                        date = date,
                        systolic = records.map { it.systolic.inMillimetersOfMercury }.average(),
                        diastolic = records.map { it.diastolic.inMillimetersOfMercury }.average(),
                        count = records.size,
                    )
                }
                .sortedBy { it.date }
        }

    suspend fun readDailySpO2(start: Instant, end: Instant): List<DailyVitalPoint> =
        support.withLogging("readDailySpO2[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = DailyReadPageSize,
            ).dailyPoints({ it.time }, { it.percentage.value })
        }

    suspend fun readDailyRespiratoryRate(start: Instant, end: Instant): List<DailyVitalPoint> =
        support.withLogging("readDailyRespiratoryRate[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = RespiratoryRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = DailyReadPageSize,
            ).dailyPoints({ it.time }, { it.rate })
        }

    suspend fun readDailyBodyTemperature(start: Instant, end: Instant): List<DailyVitalPoint> =
        support.withLogging("readDailyBodyTemperature[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = BodyTemperatureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = DailyReadPageSize,
            ).dailyPoints({ it.time }, { it.temperature.inCelsius })
        }

    suspend fun readDailyVo2Max(start: Instant, end: Instant): List<DailyVitalPoint> =
        support.withLogging("readDailyVo2Max[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = Vo2MaxRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = DailyReadPageSize,
            ).dailyPoints({ it.time }, { it.vo2MillilitersPerMinuteKilogram })
        }

    suspend fun readDailyBloodGlucose(start: Instant, end: Instant): List<DailyVitalPoint> =
        support.withLogging("readDailyBloodGlucose[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = BloodGlucoseRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = DailyReadPageSize,
            ).dailyPoints({ it.time }, { it.level.inMillimolesPerLiter })
        }

    suspend fun readDailySkinTemperature(start: Instant, end: Instant): List<DailyVitalPoint> =
        support.withLogging("readDailySkinTemperature[$start..$end]", emptyList()) {
            // Match the chart, which plots (and the card reads) the per-record average
            // delta — records with no deltas carry no value and drop out of the day.
            support.client().readRecordsPaged(
                recordType = SkinTemperatureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
                pageSize = DailyReadPageSize,
            ).dailyPoints({ it.startTime }, { record -> record.deltas.map { it.delta.inCelsius }.averageOrNull() })
        }

    private suspend fun <R : Record> latestRecord(
        recordType: kotlin.reflect.KClass<R>,
        start: Instant,
        end: Instant,
    ): R? =
        support.client().readRecordsPaged(
            recordType = recordType,
            timeRangeFilter = TimeRangeFilter.between(start, end),
            ascendingOrder = false,
            pageSize = 1,
            maxRecords = 1,
        ).firstOrNull()

    /** Bucket raw records into one [DailyVitalPoint] per local date. */
    private fun <T> List<T>.dailyPoints(
        time: (T) -> Instant,
        value: (T) -> Double?,
    ): List<DailyVitalPoint> {
        val zone = ZoneId.systemDefault()
        return groupBy { time(it).atZone(zone).toLocalDate() }
            .mapNotNull { (date, records) ->
                val values = records.mapNotNull(value)
                if (values.isEmpty()) return@mapNotNull null
                DailyVitalPoint(
                    date = date,
                    value = values.average(),
                    count = values.size,
                )
            }
            .sortedBy { it.date }
    }

    private fun BloodPressureRecord.toEntry(): BloodPressureEntry =
        BloodPressureEntry(
            time = time,
            systolicMmHg = systolic.inMillimetersOfMercury.toInt(),
            diastolicMmHg = diastolic.inMillimetersOfMercury.toInt(),
            source = metadata.dataOrigin.packageName,
            id = metadata.id,
            isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
        )

    private fun OxygenSaturationRecord.toEntry(): SpO2Entry =
        SpO2Entry(
            time = time,
            percent = percentage.value,
            source = metadata.dataOrigin.packageName,
            id = metadata.id,
            isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
        )

    private fun RespiratoryRateRecord.toEntry(): RespiratoryRateEntry =
        RespiratoryRateEntry(
            time = time,
            breathsPerMinute = rate,
            source = metadata.dataOrigin.packageName,
            id = metadata.id,
            isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
        )

    private fun BodyTemperatureRecord.toEntry(): BodyTempEntry =
        BodyTempEntry(
            time = time,
            temperatureCelsius = temperature.inCelsius,
            source = metadata.dataOrigin.packageName,
            id = metadata.id,
            isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
        )

    private fun Vo2MaxRecord.toEntry(): Vo2MaxEntry =
        Vo2MaxEntry(
            time = time,
            vo2MaxMlPerKgPerMin = vo2MillilitersPerMinuteKilogram,
            source = metadata.dataOrigin.packageName,
        )

    private fun BloodGlucoseRecord.toEntry(): BloodGlucoseEntry =
        BloodGlucoseEntry(
            time = time,
            millimolesPerLiter = level.inMillimolesPerLiter,
            specimenSource = specimenSource,
            mealType = mealType,
            relationToMeal = relationToMeal,
            source = metadata.dataOrigin.packageName,
        )

    private fun SkinTemperatureRecord.toEntry(): SkinTemperatureEntry {
        val deltasCelsius = deltas.map { delta -> delta.delta.inCelsius }
        return SkinTemperatureEntry(
            startTime = startTime,
            endTime = endTime,
            baselineCelsius = baseline?.inCelsius,
            averageDeltaCelsius = deltasCelsius.averageOrNull(),
            minDeltaCelsius = deltasCelsius.minOrNull(),
            maxDeltaCelsius = deltasCelsius.maxOrNull(),
            measurementLocation = measurementLocation,
            source = metadata.dataOrigin.packageName,
        )
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

/**
 * Page size for the daily aggregate readers only. The platform maximum: the
 * default 1000 costs ~175 round-trips over a dense year of continuous SpO2 or
 * skin temperature; 5000 cuts that fivefold. Raw list readers keep their small
 * pages — they feed bounded windows.
 */
private const val DailyReadPageSize = 5000

private fun List<Double>.averageOrNull(): Double? =
    takeIf { it.isNotEmpty() }?.average()
