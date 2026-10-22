package tech.mmarca.openvitals.healthconnect

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
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.kilograms
import androidx.health.connect.client.units.meters
import androidx.health.connect.client.units.percent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.domain.model.BodyFatEntry
import tech.mmarca.openvitals.domain.model.BodyMeasurementEntry
import tech.mmarca.openvitals.domain.model.BodyWaterMassEntry
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.BoneMassEntry
import tech.mmarca.openvitals.domain.model.HeightEntry
import tech.mmarca.openvitals.domain.model.LeanBodyMassEntry
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.domain.model.accepts
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.reflect.KClass

internal class BodyHealthReader(
    private val support: HealthConnectReaderSupport,
    private val appPackageName: String,
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
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
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
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
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
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
                )
            }
        }

    suspend fun readLatestHeight(): Double? =
        readLatestHeightEntry()?.heightCm

    suspend fun readLatestHeightEntry(): HeightEntry? =
        support.withNullableLogging("readLatestHeight") {
            support.client().readRecordsPaged(
                recordType = HeightRecord::class,
                timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.let { record ->
                HeightEntry(
                    time = record.time,
                    heightCm = record.height.inMeters * 100.0,
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
                )
            }
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
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
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
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
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
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = isEditableBodyRecord(record.metadata),
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
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = isEditableBodyRecord(record.metadata),
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
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = isEditableBodyRecord(record.metadata),
                )
            }
        }

    suspend fun readLatestBodyWaterMass(): Double? =
        support.withNullableLogging("readLatestBodyWaterMass") {
            support.client().readRecordsPaged(
                recordType = BodyWaterMassRecord::class,
                timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                ascendingOrder = false,
                pageSize = 1,
                maxRecords = 1,
            ).firstOrNull()?.mass?.inKilograms
        }

    suspend fun readBodyWaterMassEntries(start: Instant, end: Instant): List<BodyWaterMassEntry> =
        support.withLogging("readBodyWaterMassEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = BodyWaterMassRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            ).map { record ->
                BodyWaterMassEntry(
                    time = record.time,
                    massKg = record.mass.inKilograms,
                    source = SyncedSourceOverlay.displaySource(record.metadata),
                    id = record.metadata.id,
                    isOpenVitalsEntry = isEditableBodyRecord(record.metadata),
                )
            }
        }

    suspend fun writeBodyMeasurementEntry(request: BodyMeasurementWriteRequest): String =
        writeBodyMeasurementEntries(listOf(request)).single()

    /** One insert for every value, so a weigh-in is saved whole or not at all. */
    suspend fun writeBodyMeasurementEntries(requests: List<BodyMeasurementWriteRequest>): List<String> =
        withContext(Dispatchers.IO) {
            requests.forEach(::validateBodyMeasurement)

            val clientRecordIds = requests.map { request ->
                "openvitals_body_${request.type.name.lowercase()}_${request.time.toEpochMilli()}_${UUID.randomUUID()}"
            }
            val records = requests.zip(clientRecordIds) { request, clientRecordId ->
                request.toRecord(
                    Metadata.manualEntry(
                        device = Device(type = Device.TYPE_PHONE),
                        clientRecordId = clientRecordId,
                    ),
                )
            }

            Log.d(TAG, "Writing body records types=${requests.map { it.type }} ${support.diagnosticsSummary()}")
            support.client().insertRecords(records)
            clientRecordIds
        }

    suspend fun readBodyMeasurementEntry(type: BodyMeasurementType, id: String): BodyMeasurementEntry? =
        support.withNullableLogging("readBodyMeasurementEntry[$type][$id]") {
            support.client().readRecord(type.recordType, id).record.toBodyMeasurementEntry(type)
        }

    suspend fun updateBodyMeasurementEntry(id: String, request: BodyMeasurementWriteRequest) =
        withContext(Dispatchers.IO) {
            validateBodyMeasurement(request)

            val existing = support.client().readRecord(request.type.recordType, id).record
            existing.requireOpenVitalsOrigin(appPackageName)

            val record = request.toRecord(
                Metadata.manualEntryWithId(
                    id = id,
                    device = existing.metadata.device ?: Device(type = Device.TYPE_PHONE),
                ),
            )

            Log.d(TAG, "Updating body record type=${request.type} ${support.diagnosticsSummary()}")
            support.client().updateRecords(listOf(record))
        }

    suspend fun deleteBodyMeasurementEntry(type: BodyMeasurementType, id: String) = withContext(Dispatchers.IO) {
        val existing = support.client().readRecord(type.recordType, id).record
        existing.requireOpenVitalsOrigin(appPackageName)
        support.client().deleteRecords(
            recordType = type.recordType,
            recordIdsList = listOf(existing.metadata.id),
            clientRecordIdsList = emptyList(),
        )
    }

    private fun validateBodyMeasurement(request: BodyMeasurementWriteRequest) {
        require(request.type.accepts(request.value)) {
            "${request.type} value ${request.value} is outside what Health Connect accepts."
        }
    }

    private fun BodyMeasurementWriteRequest.toRecord(metadata: Metadata): Record {
        val zoneOffset = ZoneId.systemDefault().rules.getOffset(time)
        return when (type) {
            BodyMeasurementType.WEIGHT -> WeightRecord(time, zoneOffset, value.kilograms, metadata)
            BodyMeasurementType.HEIGHT -> HeightRecord(time, zoneOffset, (value / CentimetersPerMeter).meters, metadata)
            BodyMeasurementType.BODY_FAT -> BodyFatRecord(time, zoneOffset, value.percent, metadata)
            BodyMeasurementType.LEAN_MASS -> LeanBodyMassRecord(time, zoneOffset, value.kilograms, metadata)
            BodyMeasurementType.BODY_WATER_MASS -> BodyWaterMassRecord(time, zoneOffset, value.kilograms, metadata)
            BodyMeasurementType.BONE_MASS -> BoneMassRecord(time, zoneOffset, value.kilograms, metadata)
            BodyMeasurementType.BMR ->
                BasalMetabolicRateRecord(time, zoneOffset, Power.kilocaloriesPerDay(value), metadata)
        }
    }

    private fun Record.toBodyMeasurementEntry(type: BodyMeasurementType): BodyMeasurementEntry {
        val (time, value) = when (this) {
            is WeightRecord -> time to weight.inKilograms
            is HeightRecord -> time to height.inMeters * CentimetersPerMeter
            is BodyFatRecord -> time to percentage.value
            is LeanBodyMassRecord -> time to mass.inKilograms
            is BodyWaterMassRecord -> time to mass.inKilograms
            is BoneMassRecord -> time to mass.inKilograms
            is BasalMetabolicRateRecord -> time to basalMetabolicRate.inKilocaloriesPerDay
            else -> error("Not a body record: ${this::class.simpleName}")
        }
        return BodyMeasurementEntry(
            id = metadata.id,
            type = type,
            time = time,
            value = value,
            source = SyncedSourceOverlay.displaySource(metadata),
            isOpenVitalsEntry = isEditableBodyRecord(metadata),
        )
    }

    /**
     * Written here, and not the daily BMR estimate: that one is rewritten by
     * its own pass, so an edit would not stay.
     */
    private fun isEditableBodyRecord(metadata: Metadata): Boolean =
        isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName) &&
            metadata.clientRecordId?.startsWith(BmrEstimateClientRecordIdPrefix) != true
}

/** The Health Connect record each body value is saved as. */
internal val BodyMeasurementType.recordType: KClass<out Record>
    get() = when (this) {
        BodyMeasurementType.WEIGHT -> WeightRecord::class
        BodyMeasurementType.HEIGHT -> HeightRecord::class
        BodyMeasurementType.BODY_FAT -> BodyFatRecord::class
        BodyMeasurementType.LEAN_MASS -> LeanBodyMassRecord::class
        BodyMeasurementType.BODY_WATER_MASS -> BodyWaterMassRecord::class
        BodyMeasurementType.BONE_MASS -> BoneMassRecord::class
        BodyMeasurementType.BMR -> BasalMetabolicRateRecord::class
    }

private const val TAG = "BodyHealthReader"
private const val CentimetersPerMeter = 100.0
