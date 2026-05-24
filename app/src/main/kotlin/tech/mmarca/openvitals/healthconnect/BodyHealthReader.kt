package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.time.TimeRangeFilter
import tech.mmarca.openvitals.data.model.BodyFatEntry
import tech.mmarca.openvitals.data.model.BmrEntry
import tech.mmarca.openvitals.data.model.BoneMassEntry
import tech.mmarca.openvitals.data.model.HeightEntry
import tech.mmarca.openvitals.data.model.LeanBodyMassEntry
import tech.mmarca.openvitals.data.model.WeightEntry
import java.time.Instant
import java.time.LocalDate

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
}
