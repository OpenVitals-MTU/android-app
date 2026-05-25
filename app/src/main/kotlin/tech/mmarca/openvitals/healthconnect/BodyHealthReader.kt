package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.kilograms
import androidx.health.connect.client.units.meters
import androidx.health.connect.client.units.percent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.data.model.BodyMeasurementType
import tech.mmarca.openvitals.data.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.data.model.BodyFatEntry
import tech.mmarca.openvitals.data.model.BmrEntry
import tech.mmarca.openvitals.data.model.BoneMassEntry
import tech.mmarca.openvitals.data.model.HeightEntry
import tech.mmarca.openvitals.data.model.LeanBodyMassEntry
import tech.mmarca.openvitals.data.model.WeightEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

internal class BodyHealthReader(
    private val support: HealthConnectReaderSupport,
) {
    suspend fun readLatestWeight(date: LocalDate): WeightEntry? {
        val (start, end) = support.dayRange(date)
        return support.withNullableLogging("readLatestWeight[$date][$start..$end]") {
            support.client().readRecordsPaged(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.let { record ->
                WeightEntry(
                    time = record.time,
                    weightKg = record.weight.inKilograms,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }
    }

    suspend fun readLatestWeight(): WeightEntry? =
        support.withNullableLogging("readLatestWeight") {
            support.client().readRecordsPaged(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.let { record ->
                WeightEntry(
                    time = record.time,
                    weightKg = record.weight.inKilograms,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readWeightEntries(start: Instant, end: Instant): List<WeightEntry> =
        support.withLogging("readWeightEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            ).map { record ->
                WeightEntry(
                    time = record.time,
                    weightKg = record.weight.inKilograms,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readLatestHeight(): Double? =
        support.withNullableLogging("readLatestHeight") {
            support.client().readRecordsPaged(
                recordType = HeightRecord::class,
                timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.height?.inMeters?.times(100.0)
        }

    suspend fun readHeightEntries(start: Instant, end: Instant): List<HeightEntry> =
        support.withLogging("readHeightEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = HeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            ).map { record ->
                HeightEntry(
                    time = record.time,
                    heightCm = record.height.inMeters * 100.0,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readLatestBodyFat(): Double? =
        support.withNullableLogging("readLatestBodyFat") {
            support.client().readRecordsPaged(
                recordType = BodyFatRecord::class,
                timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.percentage?.value
        }

    suspend fun readBodyFatEntries(start: Instant, end: Instant): List<BodyFatEntry> =
        support.withLogging("readBodyFatEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = BodyFatRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            ).map { record ->
                BodyFatEntry(
                    time = record.time,
                    percent = record.percentage.value,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readLatestLeanBodyMass(): Double? =
        support.withNullableLogging("readLatestLeanBodyMass") {
            support.client().readRecordsPaged(
                recordType = LeanBodyMassRecord::class,
                timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.mass?.inKilograms
        }

    suspend fun readLeanBodyMassEntries(start: Instant, end: Instant): List<LeanBodyMassEntry> =
        support.withLogging("readLeanBodyMassEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = LeanBodyMassRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            ).map { record ->
                LeanBodyMassEntry(
                    time = record.time,
                    massKg = record.mass.inKilograms,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readLatestBMR(): Double? =
        support.withNullableLogging("readLatestBMR") {
            support.client().readRecordsPaged(
                recordType = BasalMetabolicRateRecord::class,
                timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.basalMetabolicRate?.inKilocaloriesPerDay
        }

    suspend fun readBmrEntries(start: Instant, end: Instant): List<BmrEntry> =
        support.withLogging("readBmrEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = BasalMetabolicRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            ).map { record ->
                BmrEntry(
                    time = record.time,
                    kcalPerDay = record.basalMetabolicRate.inKilocaloriesPerDay,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun readLatestBoneMass(): Double? =
        support.withNullableLogging("readLatestBoneMass") {
            support.client().readRecordsPaged(
                recordType = BoneMassRecord::class,
                timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.mass?.inKilograms
        }

    suspend fun readBoneMassEntries(start: Instant, end: Instant): List<BoneMassEntry> =
        support.withLogging("readBoneMassEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = BoneMassRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            ).map { record ->
                BoneMassEntry(
                    time = record.time,
                    massKg = record.mass.inKilograms,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }

    suspend fun writeBodyMeasurementEntry(request: BodyMeasurementWriteRequest): String = withContext(Dispatchers.IO) {
        validateBodyMeasurement(request)

        val time = request.time
        val zone = ZoneId.systemDefault()
        val clientRecordId = "openvitals_body_${request.type.name.lowercase()}_${time.toEpochMilli()}_${UUID.randomUUID()}"
        val metadata = Metadata.manualEntry(
            device = Device(type = Device.TYPE_PHONE),
            clientRecordId = clientRecordId,
        )
        val record = when (request.type) {
            BodyMeasurementType.WEIGHT -> WeightRecord(
                time = time,
                zoneOffset = zone.rules.getOffset(time),
                weight = request.value.kilograms,
                metadata = metadata,
            )
            BodyMeasurementType.HEIGHT -> HeightRecord(
                time = time,
                zoneOffset = zone.rules.getOffset(time),
                height = (request.value / CentimetersPerMeter).meters,
                metadata = metadata,
            )
            BodyMeasurementType.BODY_FAT -> BodyFatRecord(
                time = time,
                zoneOffset = zone.rules.getOffset(time),
                percentage = request.value.percent,
                metadata = metadata,
            )
        }

        Log.d(TAG, "Writing body record type=${request.type} value=${request.value} ${support.diagnosticsSummary()}")
        support.client().insertRecords(listOf(record))
        clientRecordId
    }

    private fun validateBodyMeasurement(request: BodyMeasurementWriteRequest) {
        when (request.type) {
            BodyMeasurementType.WEIGHT -> require(request.value > 0.0 && request.value <= MaxWeightKg) {
                "Weight must be greater than 0 kg and no more than ${MaxWeightKg.toInt()} kg."
            }
            BodyMeasurementType.HEIGHT -> require(request.value > 0.0 && request.value <= MaxHeightCm) {
                "Height must be greater than 0 cm and no more than ${MaxHeightCm.toInt()} cm."
            }
            BodyMeasurementType.BODY_FAT -> require(request.value >= 0.0 && request.value <= MaxBodyFatPercent) {
                "Body fat must be between 0% and ${MaxBodyFatPercent.toInt()}%."
            }
        }
    }
}

private const val TAG = "BodyHealthReader"
private const val CentimetersPerMeter = 100.0
private const val MaxWeightKg = 1000.0
private const val MaxHeightCm = 300.0
private const val MaxBodyFatPercent = 100.0
