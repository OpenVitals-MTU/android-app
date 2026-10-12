package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.HydrationEntryChange
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.domain.model.OpenVitalsFoodClientRecordPrefix
import tech.mmarca.openvitals.domain.model.OpenVitalsManualNutritionClientRecordPrefix
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class NutritionHealthReader(
    private val support: HealthConnectReaderSupport,
    private val appPackageName: String,
) {
    suspend fun readDailyNutrition(
        startDate: LocalDate,
        endDate: LocalDate,
        includeHydration: Boolean = true,
        includeCalories: Boolean = true,
        includeEstimatedCalories: Boolean = false,
    ): List<DailyNutrition> {
        if (endDate.isBefore(startDate)) return emptyList()
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withLogging("readDailyNutrition[$start..$end]", emptyList()) {
            val client = support.client()
            // One reading serves every chunk: the latest BMR does not change per chunk.
            val bmrKcalPerDay = if (includeCalories && includeEstimatedCalories) {
                client.readLatestBmrKcalPerDayBefore(end)
            } else {
                null
            }
            // Chunked like readDailySteps. One guard around all chunks, so a failure
            // anywhere fails the whole read: a half-zeroed year would lie.
            val aggregateRows = dailyAggregateDateChunks(startDate, endDate).flatMap { (chunkStart, chunkEnd) ->
                readDailyNutritionChunk(
                    startDate = chunkStart,
                    endDate = chunkEnd,
                    includeHydration = includeHydration,
                    includeCalories = includeCalories,
                    includeEstimatedCalories = includeEstimatedCalories,
                    bmrKcalPerDay = bmrKcalPerDay,
                )
            }
            if (aggregateRows.isNotEmpty()) aggregateRows else dailyNutritionSeries(startDate, endDate)
        }
    }

    private suspend fun readDailyNutritionChunk(
        startDate: LocalDate,
        endDate: LocalDate,
        includeHydration: Boolean,
        includeCalories: Boolean,
        includeEstimatedCalories: Boolean,
        bmrKcalPerDay: Double?,
    ): List<DailyNutrition> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        val metrics = buildSet {
            if (includeHydration) add(HydrationRecord.VOLUME_TOTAL)
            if (includeCalories) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
            if (includeCalories && includeEstimatedCalories) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
        }
        return support.client().aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = metrics,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                timeRangeSlicer = Duration.ofDays(1),
            )
        ).byLocalDate(zone).map { day ->
            val date = day.date
            // Health Connect synthesizes a basal baseline even with no records. A
            // bucket with empty dataOrigins is no data. Only sound with calorie
            // metrics alone, so it is skipped with hydration in the request.
            val hasCalorieRecords =
                includeHydration || day.any { it.dataOrigins.isNotEmpty() }
            val totalCaloriesKcal = if (includeCalories && hasCalorieRecords) {
                day.totalOrNull { it[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories }
            } else {
                null
            }
            val activeCaloriesKcal = if (includeCalories && includeEstimatedCalories && hasCalorieRecords) {
                day.totalOrNull { it[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories }
            } else {
                null
            }
            val caloriesBurned = totalCaloriesRecordedOrDailyEstimated(
                recordedTotalCaloriesKcal = totalCaloriesKcal,
                activeCaloriesKcal = activeCaloriesKcal,
                bmrKcalPerDay = bmrKcalPerDay,
            )
            DailyNutrition(
                date = date,
                hydrationLiters = if (includeHydration) {
                    day.total { it[HydrationRecord.VOLUME_TOTAL]?.inLiters }
                } else {
                    0.0
                },
                caloriesBurnedKcal = caloriesBurned?.kcal ?: 0.0,
                caloriesBurnedSource = caloriesBurned?.source ?: CaloriesBurnedSource.NO_DATA,
            )
        }
    }

    // Chunked like readDailyNutrition, with one guard around all chunks.
    suspend fun readDailyMacros(startDate: LocalDate, endDate: LocalDate): List<DailyMacros> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withLogging("readDailyMacros[$start..$end]", emptyList()) {
            dailyAggregateDateChunks(startDate, endDate).flatMap { (chunkStart, chunkEnd) ->
                readDailyMacrosChunk(chunkStart, chunkEnd)
            }
        }
    }

    private suspend fun readDailyMacrosChunk(startDate: LocalDate, endDate: LocalDate): List<DailyMacros> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return support.client().aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = nutritionAggregateMetrics,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                timeRangeSlicer = Duration.ofDays(1),
            )
        ).byLocalDate(zone).map { day ->
            DailyMacros(
                date = day.date,
                nutrientValues = day.buckets
                    .map { it.result.nutritionNutrientValues() }
                    .reduceOrNull { totals, bucket ->
                        totals.toMutableMap().apply {
                            bucket.forEach { (nutrient, value) -> merge(nutrient, value, Double::plus) }
                        }
                    }
                    .orEmpty(),
            )
        }
    }

    suspend fun readNutritionEntries(start: Instant, end: Instant): List<NutritionEntry> =
        support.withLogging("readNutritionEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = NutritionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 200,
            ).map { record -> record.toNutritionEntry() }
        }

    suspend fun readNutritionEntry(id: String): NutritionEntry? =
        support.withNullableLogging("readNutritionEntry[$id]") {
            support.client().readRecord(NutritionRecord::class, id).record.toNutritionEntry()
        }

    suspend fun writeNutritionEntry(request: NutritionWriteRequest): String = withContext(Dispatchers.IO) {
        val nutrientValues = request.nutrientValues
        require(nutrientValues.isNotEmpty()) {
            "At least one nutrient must be greater than zero."
        }
        nutrientValues.forEach { (nutrient, value) ->
            require(value > 0.0 && value.isFinite()) {
                "${nutrient.name} must be greater than zero."
            }
            require(value <= MaxNutritionNutrientValue) {
                "${nutrient.name} must not exceed ${MaxNutritionNutrientValue.toInt()}."
            }
        }

        val startTime = request.time
        // Health Connect needs a strictly positive interval.
        val endTime = request.endTime
            ?.takeIf { it.isAfter(startTime) }
            ?: startTime.plusSeconds(1)
        val zone = ZoneId.systemDefault()
        val clientRecordId = request.clientRecordId
            ?.takeIf { it.isNotBlank() }
            ?: request.associatedHydrationClientRecordId
                ?.takeIf { it.isNotBlank() }
                ?.let(::hydrationNutritionClientRecordId)
            ?: request.foodId
                ?.takeIf { it.isNotBlank() }
                ?.let { foodId -> foodNutritionClientRecordId(foodId, startTime) }
            ?: manualNutritionClientRecordId(startTime).takeIf { request.isManualNutritionEntry }
            ?: "openvitals_nutrition_${startTime.toEpochMilli()}_${UUID.randomUUID()}"
        val record = nutritionRecordOf(
            startTime = startTime,
            startZoneOffset = zone.rules.getOffset(startTime),
            endTime = endTime,
            endZoneOffset = zone.rules.getOffset(endTime),
            metadata = Metadata.manualEntry(
                device = Device(type = Device.TYPE_PHONE),
                clientRecordId = clientRecordId,
                clientRecordVersion = request.clientRecordVersion,
            ),
            nutrientValues = nutrientValues,
            name = request.name?.trim()?.takeIf { it.isNotBlank() } ?: "OpenVitals nutrition",
        )

        Log.d(TAG, "Writing nutrition record ${support.diagnosticsSummary()}")
        support.client().insertRecords(listOf(record))
        clientRecordId
    }

    /**
     * Replaces a record this app wrote with the edited values. The same client id with a higher
     * version replaces it in place: an update by record id would drop the client id, and the
     * prefix in it is what keeps a typed entry off the beverage screens. The name stays, so an
     * old carbs entry stays recognisable too.
     */
    suspend fun updateNutritionEntry(id: String, request: NutritionWriteRequest) = withContext(Dispatchers.IO) {
        val existing = support.client().readRecord(NutritionRecord::class, id).record
        existing.requireOpenVitalsOrigin(appPackageName)
        val clientRecordId = requireNotNull(existing.metadata.clientRecordId?.takeIf { it.isNotBlank() }) {
            "Only a nutrition record with a client id can be edited."
        }
        writeNutritionEntry(
            request.copy(
                name = existing.name,
                clientRecordId = clientRecordId,
                clientRecordVersion = existing.metadata.clientRecordVersion + 1,
            ),
        )
    }

    /**
     * Makes a drink's nutrition record follow an edit of its hydration record: the same shift
     * in time, and nutrients scaled with the volume. It used to stay behind, so an edited
     * coffee kept its caffeine at the old time and amount. Does nothing for plain water.
     */
    suspend fun updateHydrationNutritionEntry(change: HydrationEntryChange) = withContext(Dispatchers.IO) {
        val hydrationClientRecordId = change.clientRecordId?.takeIf { it.isNotBlank() } ?: return@withContext
        if (change.oldVolumeLiters <= 0.0) return@withContext
        val clientRecordId = hydrationNutritionClientRecordId(hydrationClientRecordId)
        // There is no read by client id. The record starts when the drink did.
        val existing = support.client().readRecordsPaged(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(change.oldTime, change.oldTime.plusSeconds(1)),
            ascendingOrder = true,
        ).firstOrNull { record ->
            record.metadata.dataOrigin.packageName == appPackageName &&
                record.metadata.clientRecordId == clientRecordId
        } ?: return@withContext

        val scale = change.newVolumeLiters / change.oldVolumeLiters
        val intake = Duration.between(existing.startTime, existing.endTime)
        writeNutritionEntry(
            NutritionWriteRequest(
                time = change.newTime,
                nutrientValues = existing.nutritionNutrientValues().mapValues { (_, value) -> value * scale },
                name = existing.name,
                associatedHydrationClientRecordId = hydrationClientRecordId,
                endTime = change.newTime.plus(intake),
                clientRecordVersion = existing.metadata.clientRecordVersion + 1,
            ),
        )
    }

    suspend fun deleteHydrationNutritionEntry(hydrationClientRecordId: String) = withContext(Dispatchers.IO) {
        if (hydrationClientRecordId.isBlank()) return@withContext

        Log.d(TAG, "Deleting paired hydration nutrition record ${support.diagnosticsSummary()}")
        support.client().deleteRecords(
            recordType = NutritionRecord::class,
            recordIdsList = emptyList(),
            clientRecordIdsList = listOf(hydrationNutritionClientRecordId(hydrationClientRecordId)),
        )
    }

    suspend fun deleteNutritionEntry(id: String): String? = withContext(Dispatchers.IO) {
        val existing = support.client().readRecord(NutritionRecord::class, id).record
        existing.requireOpenVitalsOrigin(appPackageName)
        val clientRecordId = existing.metadata.clientRecordId

        Log.d(TAG, "Deleting nutrition record ${support.diagnosticsSummary()}")
        support.client().deleteRecords(
            recordType = NutritionRecord::class,
            recordIdsList = listOf(existing.metadata.id),
            clientRecordIdsList = emptyList(),
        )
        clientRecordId
    }

    private fun NutritionRecord.toNutritionEntry(): NutritionEntry {
        val nutrientValues = nutritionNutrientValues()
        return NutritionEntry(
            time = startTime,
            endTime = endTime,
            mealType = mealType,
            name = name,
            energyKcal = nutrientValues[NutritionNutrient.ENERGY],
            proteinGrams = nutrientValues[NutritionNutrient.PROTEIN],
            carbsGrams = nutrientValues[NutritionNutrient.TOTAL_CARBOHYDRATE],
            fatGrams = nutrientValues[NutritionNutrient.TOTAL_FAT],
            fiberGrams = nutrientValues[NutritionNutrient.DIETARY_FIBER],
            sugarGrams = nutrientValues[NutritionNutrient.SUGAR],
            source = SyncedSourceOverlay.displaySource(metadata),
            nutrientValues = nutrientValues,
            id = metadata.id,
            clientRecordId = metadata.clientRecordId,
            isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
        )
    }
}

private const val TAG = "NutritionHealthReader"
private const val MaxNutritionNutrientValue = 10000.0
private const val HydrationNutritionClientRecordIdPrefix = "openvitals_hydration_nutrition_"

internal fun hydrationNutritionClientRecordId(hydrationClientRecordId: String): String =
    "$HydrationNutritionClientRecordIdPrefix$hydrationClientRecordId"

/** The beverage screens skip this prefix, so a typed nutrition entry never shows up as a drink. */
internal fun manualNutritionClientRecordId(time: Instant): String =
    "$OpenVitalsManualNutritionClientRecordPrefix${time.toEpochMilli()}_${UUID.randomUUID()}"

/** The beverage screens skip this prefix, so a food never shows up as a drink. */
internal fun foodNutritionClientRecordId(foodId: String, time: Instant): String =
    "$OpenVitalsFoodClientRecordPrefix${time.toEpochMilli()}_${foodId}_${UUID.randomUUID()}"

private data class NutritionEnergyAggregate(
    val nutrient: NutritionNutrient,
    val metric: AggregateMetric<Energy>,
)

private data class NutritionMassAggregate(
    val nutrient: NutritionNutrient,
    val metric: AggregateMetric<Mass>,
)

private val nutritionEnergyAggregates = listOf(
    NutritionEnergyAggregate(NutritionNutrient.ENERGY, NutritionRecord.ENERGY_TOTAL),
    NutritionEnergyAggregate(NutritionNutrient.ENERGY_FROM_FAT, NutritionRecord.ENERGY_FROM_FAT_TOTAL),
)

private val nutritionMassAggregates = listOf(
    NutritionMassAggregate(NutritionNutrient.BIOTIN, NutritionRecord.BIOTIN_TOTAL),
    NutritionMassAggregate(NutritionNutrient.CAFFEINE, NutritionRecord.CAFFEINE_TOTAL),
    NutritionMassAggregate(NutritionNutrient.CALCIUM, NutritionRecord.CALCIUM_TOTAL),
    NutritionMassAggregate(NutritionNutrient.CHLORIDE, NutritionRecord.CHLORIDE_TOTAL),
    NutritionMassAggregate(NutritionNutrient.CHOLESTEROL, NutritionRecord.CHOLESTEROL_TOTAL),
    NutritionMassAggregate(NutritionNutrient.CHROMIUM, NutritionRecord.CHROMIUM_TOTAL),
    NutritionMassAggregate(NutritionNutrient.COPPER, NutritionRecord.COPPER_TOTAL),
    NutritionMassAggregate(NutritionNutrient.DIETARY_FIBER, NutritionRecord.DIETARY_FIBER_TOTAL),
    NutritionMassAggregate(NutritionNutrient.FOLATE, NutritionRecord.FOLATE_TOTAL),
    NutritionMassAggregate(NutritionNutrient.FOLIC_ACID, NutritionRecord.FOLIC_ACID_TOTAL),
    NutritionMassAggregate(NutritionNutrient.IODINE, NutritionRecord.IODINE_TOTAL),
    NutritionMassAggregate(NutritionNutrient.IRON, NutritionRecord.IRON_TOTAL),
    NutritionMassAggregate(NutritionNutrient.MAGNESIUM, NutritionRecord.MAGNESIUM_TOTAL),
    NutritionMassAggregate(NutritionNutrient.MANGANESE, NutritionRecord.MANGANESE_TOTAL),
    NutritionMassAggregate(NutritionNutrient.MOLYBDENUM, NutritionRecord.MOLYBDENUM_TOTAL),
    NutritionMassAggregate(NutritionNutrient.MONOUNSATURATED_FAT, NutritionRecord.MONOUNSATURATED_FAT_TOTAL),
    NutritionMassAggregate(NutritionNutrient.NIACIN, NutritionRecord.NIACIN_TOTAL),
    NutritionMassAggregate(NutritionNutrient.PANTOTHENIC_ACID, NutritionRecord.PANTOTHENIC_ACID_TOTAL),
    NutritionMassAggregate(NutritionNutrient.PHOSPHORUS, NutritionRecord.PHOSPHORUS_TOTAL),
    NutritionMassAggregate(NutritionNutrient.POLYUNSATURATED_FAT, NutritionRecord.POLYUNSATURATED_FAT_TOTAL),
    NutritionMassAggregate(NutritionNutrient.POTASSIUM, NutritionRecord.POTASSIUM_TOTAL),
    NutritionMassAggregate(NutritionNutrient.PROTEIN, NutritionRecord.PROTEIN_TOTAL),
    NutritionMassAggregate(NutritionNutrient.RIBOFLAVIN, NutritionRecord.RIBOFLAVIN_TOTAL),
    NutritionMassAggregate(NutritionNutrient.SATURATED_FAT, NutritionRecord.SATURATED_FAT_TOTAL),
    NutritionMassAggregate(NutritionNutrient.SELENIUM, NutritionRecord.SELENIUM_TOTAL),
    NutritionMassAggregate(NutritionNutrient.SODIUM, NutritionRecord.SODIUM_TOTAL),
    NutritionMassAggregate(NutritionNutrient.SUGAR, NutritionRecord.SUGAR_TOTAL),
    NutritionMassAggregate(NutritionNutrient.THIAMIN, NutritionRecord.THIAMIN_TOTAL),
    NutritionMassAggregate(NutritionNutrient.TOTAL_CARBOHYDRATE, NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL),
    NutritionMassAggregate(NutritionNutrient.TOTAL_FAT, NutritionRecord.TOTAL_FAT_TOTAL),
    NutritionMassAggregate(NutritionNutrient.TRANS_FAT, NutritionRecord.TRANS_FAT_TOTAL),
    NutritionMassAggregate(NutritionNutrient.UNSATURATED_FAT, NutritionRecord.UNSATURATED_FAT_TOTAL),
    NutritionMassAggregate(NutritionNutrient.VITAMIN_A, NutritionRecord.VITAMIN_A_TOTAL),
    NutritionMassAggregate(NutritionNutrient.VITAMIN_B12, NutritionRecord.VITAMIN_B12_TOTAL),
    NutritionMassAggregate(NutritionNutrient.VITAMIN_B6, NutritionRecord.VITAMIN_B6_TOTAL),
    NutritionMassAggregate(NutritionNutrient.VITAMIN_C, NutritionRecord.VITAMIN_C_TOTAL),
    NutritionMassAggregate(NutritionNutrient.VITAMIN_D, NutritionRecord.VITAMIN_D_TOTAL),
    NutritionMassAggregate(NutritionNutrient.VITAMIN_E, NutritionRecord.VITAMIN_E_TOTAL),
    NutritionMassAggregate(NutritionNutrient.VITAMIN_K, NutritionRecord.VITAMIN_K_TOTAL),
    NutritionMassAggregate(NutritionNutrient.ZINC, NutritionRecord.ZINC_TOTAL),
)

private val nutritionAggregateMetrics: Set<AggregateMetric<*>> =
    (nutritionEnergyAggregates.map { it.metric } + nutritionMassAggregates.map { it.metric }).toSet()

private fun AggregationResult.nutritionNutrientValues(): Map<NutritionNutrient, Double> = buildMap {
    nutritionEnergyAggregates.forEach { aggregate ->
        this@nutritionNutrientValues[aggregate.metric]
            ?.inKilocalories
            ?.takeIf { it > 0.0 }
            ?.let { put(aggregate.nutrient, it) }
    }
    nutritionMassAggregates.forEach { aggregate ->
        this@nutritionNutrientValues[aggregate.metric]
            ?.inGrams
            ?.takeIf { it > 0.0 }
            ?.let { put(aggregate.nutrient, it) }
    }
}

private fun dailyNutritionSeries(
    startDate: LocalDate,
    endDate: LocalDate,
    hydrationByDate: Map<LocalDate, Double> = emptyMap(),
): List<DailyNutrition> =
    generateSequence(startDate) { date ->
        date.plusDays(1).takeUnless { it.isAfter(endDate) }
    }.map { date ->
        DailyNutrition(
            date = date,
            hydrationLiters = hydrationByDate[date] ?: 0.0,
            caloriesBurnedKcal = 0.0,
        )
    }.toList()
