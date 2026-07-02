package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionNutrientUnit
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
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
    suspend fun readCaloriesInKcal(date: LocalDate): Double? {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withNullableLogging("readCaloriesInKcal[$date][$start..$end]") {
            support.client().aggregate(
                AggregateRequest(
                    metrics = setOf(NutritionRecord.ENERGY_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )[NutritionRecord.ENERGY_TOTAL]?.inKilocalories
        }
    }

    suspend fun readDailyNutrition(
        startDate: LocalDate,
        endDate: LocalDate,
        includeHydration: Boolean = true,
        includeCalories: Boolean = true,
        includeEstimatedCalories: Boolean = false,
    ): List<DailyNutrition> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withLogging("readDailyNutrition[$start..$end]", emptyList()) {
            val client = support.client()
            val bmrKcalPerDay = if (includeCalories && includeEstimatedCalories) {
                client.readLatestBmrKcalPerDayBefore(end)
            } else {
                null
            }
            val metrics = buildSet {
                if (includeHydration) add(HydrationRecord.VOLUME_TOTAL)
                if (includeCalories) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                if (includeCalories && includeEstimatedCalories) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            }
            val aggregateRows = client.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).map { bucket ->
                val date = bucket.startTime.atZone(zone).toLocalDate()
                val totalCaloriesKcal = if (includeCalories) {
                    bucket.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
                } else {
                    null
                }
                val activeCaloriesKcal = if (includeCalories && includeEstimatedCalories) {
                    bucket.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
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
                        bucket.result[HydrationRecord.VOLUME_TOTAL]?.inLiters ?: 0.0
                    } else {
                        0.0
                    },
                    caloriesBurnedKcal = caloriesBurned?.kcal ?: 0.0,
                    caloriesBurnedSource = caloriesBurned?.source ?: CaloriesBurnedSource.NO_DATA,
                )
            }
            if (aggregateRows.isNotEmpty()) aggregateRows else dailyNutritionSeries(startDate, endDate)
        }
    }

    suspend fun readDailyMacros(startDate: LocalDate, endDate: LocalDate): List<DailyMacros> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withLogging("readDailyMacros[$start..$end]", emptyList()) {
            support.client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = nutritionAggregateMetrics,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).map { bucket ->
                DailyMacros(
                    date = bucket.startTime.atZone(zone).toLocalDate(),
                    nutrientValues = bucket.result.nutritionNutrientValues(),
                )
            }
        }
    }

    suspend fun readNutritionEntries(start: Instant, end: Instant): List<NutritionEntry> =
        support.withLogging("readNutritionEntries[$start..$end]", emptyList()) {
            support.client().readRecordsPaged(
                recordType = NutritionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 200,
            ).map { record ->
                val nutrientValues = record.nutritionNutrientValues()
                NutritionEntry(
                    time = record.startTime,
                    mealType = record.mealType,
                    name = record.name,
                    energyKcal = nutrientValues[NutritionNutrient.ENERGY],
                    proteinGrams = nutrientValues[NutritionNutrient.PROTEIN],
                    carbsGrams = nutrientValues[NutritionNutrient.TOTAL_CARBOHYDRATE],
                    fatGrams = nutrientValues[NutritionNutrient.TOTAL_FAT],
                    fiberGrams = nutrientValues[NutritionNutrient.DIETARY_FIBER],
                    sugarGrams = nutrientValues[NutritionNutrient.SUGAR],
                    source = record.metadata.dataOrigin.packageName,
                    nutrientValues = nutrientValues,
                    id = record.metadata.id,
                    clientRecordId = record.metadata.clientRecordId,
                    isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
                )
            }
        }

    suspend fun writeCarbsEntry(request: NutritionWriteRequest): String =
        writeNutritionEntry(request)

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
        val endTime = startTime.plusSeconds(1)
        val zone = ZoneId.systemDefault()
        val clientRecordId = request.associatedHydrationClientRecordId
            ?.takeIf { it.isNotBlank() }
            ?.let(::hydrationNutritionClientRecordId)
            ?: "openvitals_nutrition_${startTime.toEpochMilli()}_${UUID.randomUUID()}"
        val record = NutritionRecord(
            startTime = startTime,
            startZoneOffset = zone.rules.getOffset(startTime),
            endTime = endTime,
            endZoneOffset = zone.rules.getOffset(endTime),
            metadata = Metadata.manualEntry(
                device = Device(type = Device.TYPE_PHONE),
                clientRecordId = clientRecordId,
            ),
            energy = nutrientValues.energy(NutritionNutrient.ENERGY),
            energyFromFat = nutrientValues.energy(NutritionNutrient.ENERGY_FROM_FAT),
            biotin = nutrientValues.mass(NutritionNutrient.BIOTIN),
            caffeine = nutrientValues.mass(NutritionNutrient.CAFFEINE),
            calcium = nutrientValues.mass(NutritionNutrient.CALCIUM),
            chloride = nutrientValues.mass(NutritionNutrient.CHLORIDE),
            cholesterol = nutrientValues.mass(NutritionNutrient.CHOLESTEROL),
            chromium = nutrientValues.mass(NutritionNutrient.CHROMIUM),
            copper = nutrientValues.mass(NutritionNutrient.COPPER),
            dietaryFiber = nutrientValues.mass(NutritionNutrient.DIETARY_FIBER),
            folate = nutrientValues.mass(NutritionNutrient.FOLATE),
            folicAcid = nutrientValues.mass(NutritionNutrient.FOLIC_ACID),
            iodine = nutrientValues.mass(NutritionNutrient.IODINE),
            iron = nutrientValues.mass(NutritionNutrient.IRON),
            magnesium = nutrientValues.mass(NutritionNutrient.MAGNESIUM),
            manganese = nutrientValues.mass(NutritionNutrient.MANGANESE),
            molybdenum = nutrientValues.mass(NutritionNutrient.MOLYBDENUM),
            monounsaturatedFat = nutrientValues.mass(NutritionNutrient.MONOUNSATURATED_FAT),
            niacin = nutrientValues.mass(NutritionNutrient.NIACIN),
            pantothenicAcid = nutrientValues.mass(NutritionNutrient.PANTOTHENIC_ACID),
            phosphorus = nutrientValues.mass(NutritionNutrient.PHOSPHORUS),
            polyunsaturatedFat = nutrientValues.mass(NutritionNutrient.POLYUNSATURATED_FAT),
            potassium = nutrientValues.mass(NutritionNutrient.POTASSIUM),
            protein = nutrientValues.mass(NutritionNutrient.PROTEIN),
            riboflavin = nutrientValues.mass(NutritionNutrient.RIBOFLAVIN),
            saturatedFat = nutrientValues.mass(NutritionNutrient.SATURATED_FAT),
            selenium = nutrientValues.mass(NutritionNutrient.SELENIUM),
            sodium = nutrientValues.mass(NutritionNutrient.SODIUM),
            sugar = nutrientValues.mass(NutritionNutrient.SUGAR),
            thiamin = nutrientValues.mass(NutritionNutrient.THIAMIN),
            totalCarbohydrate = nutrientValues.mass(NutritionNutrient.TOTAL_CARBOHYDRATE),
            totalFat = nutrientValues.mass(NutritionNutrient.TOTAL_FAT),
            transFat = nutrientValues.mass(NutritionNutrient.TRANS_FAT),
            unsaturatedFat = nutrientValues.mass(NutritionNutrient.UNSATURATED_FAT),
            vitaminA = nutrientValues.mass(NutritionNutrient.VITAMIN_A),
            vitaminB12 = nutrientValues.mass(NutritionNutrient.VITAMIN_B12),
            vitaminB6 = nutrientValues.mass(NutritionNutrient.VITAMIN_B6),
            vitaminC = nutrientValues.mass(NutritionNutrient.VITAMIN_C),
            vitaminD = nutrientValues.mass(NutritionNutrient.VITAMIN_D),
            vitaminE = nutrientValues.mass(NutritionNutrient.VITAMIN_E),
            vitaminK = nutrientValues.mass(NutritionNutrient.VITAMIN_K),
            zinc = nutrientValues.mass(NutritionNutrient.ZINC),
            name = request.name?.trim()?.takeIf { it.isNotBlank() } ?: "OpenVitals nutrition",
            mealType = MealType.MEAL_TYPE_UNKNOWN,
        )

        Log.d(TAG, "Writing nutrition record ${support.diagnosticsSummary()}")
        support.client().insertRecords(listOf(record))
        clientRecordId
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
}

private const val TAG = "NutritionHealthReader"
private const val MaxNutritionNutrientValue = 10000.0
private const val HydrationNutritionClientRecordIdPrefix = "openvitals_hydration_nutrition_"

internal fun hydrationNutritionClientRecordId(hydrationClientRecordId: String): String =
    "$HydrationNutritionClientRecordIdPrefix$hydrationClientRecordId"

private fun Map<NutritionNutrient, Double>.energy(nutrient: NutritionNutrient): Energy? {
    require(nutrient.unit == NutritionNutrientUnit.ENERGY_KCAL) {
        "${nutrient.name} is not a Health Connect energy nutrient."
    }
    return this[nutrient]?.let(Energy::kilocalories)
}

private fun Map<NutritionNutrient, Double>.mass(nutrient: NutritionNutrient): Mass? {
    require(nutrient.unit != NutritionNutrientUnit.ENERGY_KCAL) {
        "${nutrient.name} is not a Health Connect mass nutrient."
    }
    return this[nutrient]?.let(Mass::grams)
}

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

private fun NutritionRecord.nutritionNutrientValues(): Map<NutritionNutrient, Double> = buildMap {
    putIfPositive(NutritionNutrient.ENERGY, energy?.inKilocalories)
    putIfPositive(NutritionNutrient.ENERGY_FROM_FAT, energyFromFat?.inKilocalories)
    putIfPositive(NutritionNutrient.BIOTIN, biotin?.inGrams)
    putIfPositive(NutritionNutrient.CAFFEINE, caffeine?.inGrams)
    putIfPositive(NutritionNutrient.CALCIUM, calcium?.inGrams)
    putIfPositive(NutritionNutrient.CHLORIDE, chloride?.inGrams)
    putIfPositive(NutritionNutrient.CHOLESTEROL, cholesterol?.inGrams)
    putIfPositive(NutritionNutrient.CHROMIUM, chromium?.inGrams)
    putIfPositive(NutritionNutrient.COPPER, copper?.inGrams)
    putIfPositive(NutritionNutrient.DIETARY_FIBER, dietaryFiber?.inGrams)
    putIfPositive(NutritionNutrient.FOLATE, folate?.inGrams)
    putIfPositive(NutritionNutrient.FOLIC_ACID, folicAcid?.inGrams)
    putIfPositive(NutritionNutrient.IODINE, iodine?.inGrams)
    putIfPositive(NutritionNutrient.IRON, iron?.inGrams)
    putIfPositive(NutritionNutrient.MAGNESIUM, magnesium?.inGrams)
    putIfPositive(NutritionNutrient.MANGANESE, manganese?.inGrams)
    putIfPositive(NutritionNutrient.MOLYBDENUM, molybdenum?.inGrams)
    putIfPositive(NutritionNutrient.MONOUNSATURATED_FAT, monounsaturatedFat?.inGrams)
    putIfPositive(NutritionNutrient.NIACIN, niacin?.inGrams)
    putIfPositive(NutritionNutrient.PANTOTHENIC_ACID, pantothenicAcid?.inGrams)
    putIfPositive(NutritionNutrient.PHOSPHORUS, phosphorus?.inGrams)
    putIfPositive(NutritionNutrient.POLYUNSATURATED_FAT, polyunsaturatedFat?.inGrams)
    putIfPositive(NutritionNutrient.POTASSIUM, potassium?.inGrams)
    putIfPositive(NutritionNutrient.PROTEIN, protein?.inGrams)
    putIfPositive(NutritionNutrient.RIBOFLAVIN, riboflavin?.inGrams)
    putIfPositive(NutritionNutrient.SATURATED_FAT, saturatedFat?.inGrams)
    putIfPositive(NutritionNutrient.SELENIUM, selenium?.inGrams)
    putIfPositive(NutritionNutrient.SODIUM, sodium?.inGrams)
    putIfPositive(NutritionNutrient.SUGAR, sugar?.inGrams)
    putIfPositive(NutritionNutrient.THIAMIN, thiamin?.inGrams)
    putIfPositive(NutritionNutrient.TOTAL_CARBOHYDRATE, totalCarbohydrate?.inGrams)
    putIfPositive(NutritionNutrient.TOTAL_FAT, totalFat?.inGrams)
    putIfPositive(NutritionNutrient.TRANS_FAT, transFat?.inGrams)
    putIfPositive(NutritionNutrient.UNSATURATED_FAT, unsaturatedFat?.inGrams)
    putIfPositive(NutritionNutrient.VITAMIN_A, vitaminA?.inGrams)
    putIfPositive(NutritionNutrient.VITAMIN_B12, vitaminB12?.inGrams)
    putIfPositive(NutritionNutrient.VITAMIN_B6, vitaminB6?.inGrams)
    putIfPositive(NutritionNutrient.VITAMIN_C, vitaminC?.inGrams)
    putIfPositive(NutritionNutrient.VITAMIN_D, vitaminD?.inGrams)
    putIfPositive(NutritionNutrient.VITAMIN_E, vitaminE?.inGrams)
    putIfPositive(NutritionNutrient.VITAMIN_K, vitaminK?.inGrams)
    putIfPositive(NutritionNutrient.ZINC, zinc?.inGrams)
}

private fun MutableMap<NutritionNutrient, Double>.putIfPositive(
    nutrient: NutritionNutrient,
    value: Double?,
) {
    if (value != null && value > 0.0) {
        put(nutrient, value)
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
