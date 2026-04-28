package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.time.TimeRangeFilter
import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.BodyTempEntry
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.data.model.SpO2Entry
import tech.mmarca.openvitals.data.model.Vo2MaxEntry
import java.time.Instant
import java.time.LocalDate

internal class VitalsHealthReader(
    private val support: HealthConnectReaderSupport,
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
                )
            }
        }

    suspend fun readLatestBloodPressure(date: LocalDate): BloodPressureEntry? {
        val (start, end) = support.dayRange(date)
        return readBloodPressureEntries(start, end).maxByOrNull { it.time }
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
                )
            }
        }

    suspend fun readLatestSpO2(date: LocalDate): SpO2Entry? {
        val (start, end) = support.dayRange(date)
        return readSpO2Entries(start, end).maxByOrNull { it.time }
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
        return readVo2MaxEntries(start, end).maxByOrNull { it.time }
    }
}
