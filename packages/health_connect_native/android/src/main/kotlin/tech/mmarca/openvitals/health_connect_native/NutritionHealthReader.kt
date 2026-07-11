package tech.mmarca.openvitals.health_connect_native

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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ported from the native OpenVitals app (`healthconnect/NutritionHealthReader.kt`).
 *
 * Nutrient maps cross the bridge keyed by `NutritionNutrient.storageName`
 * (e.g. "ENERGY", "TOTAL_CARBOHYDRATE"); energy nutrients are kcal, mass
 * nutrients grams.
 */
internal class NutritionHealthReader(
  private val support: HealthConnectReaderSupport,
  private val appPackageName: String,
) {
  suspend fun readCaloriesInKcal(start: Instant, end: Instant): Double? =
    support.withNullableLogging("readCaloriesInKcal[$start..$end]") {
      support.client().aggregate(
        AggregateRequest(
          metrics = setOf(NutritionRecord.ENERGY_TOTAL),
          timeRangeFilter = TimeRangeFilter.between(start, end),
        ),
      )[NutritionRecord.ENERGY_TOTAL]?.inKilocalories
    }

  suspend fun readDailyNutrition(
    start: Instant,
    end: Instant,
    includeHydration: Boolean,
    includeCalories: Boolean,
    includeEstimatedCalories: Boolean,
  ): List<DailyNutritionMsg> =
    support.withLogging("readDailyNutrition[$start..$end]", emptyList()) {
      val zone = ZoneId.systemDefault()
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
      if (metrics.isEmpty()) return@withLogging dailyNutritionSeries(start, end, zone)
      val rows = client.aggregateGroupByDuration(
        AggregateGroupByDurationRequest(
          metrics = metrics,
          timeRangeFilter = TimeRangeFilter.between(start, end),
          timeRangeSlicer = Duration.ofDays(1),
        ),
      ).map { bucket ->
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
        DailyNutritionMsg(
          dateEpochMs = bucket.startTime.toEpochMilli(),
          hydrationLiters = if (includeHydration) {
            bucket.result[HydrationRecord.VOLUME_TOTAL]?.inLiters ?: 0.0
          } else {
            0.0
          },
          caloriesBurnedKcal = caloriesBurned?.kcal ?: 0.0,
          caloriesBurnedSource = caloriesBurned?.source ?: CaloriesBurnedSourceMsg.NO_DATA,
        )
      }
      if (rows.isNotEmpty()) rows else dailyNutritionSeries(start, end, zone)
    }

  suspend fun readDailyMacros(start: Instant, end: Instant): List<DailyMacrosMsg> =
    support.withLogging("readDailyMacros[$start..$end]", emptyList()) {
      val zone = ZoneId.systemDefault()
      support.client().aggregateGroupByDuration(
        AggregateGroupByDurationRequest(
          metrics = nutritionAggregateMetrics,
          timeRangeFilter = TimeRangeFilter.between(start, end),
          timeRangeSlicer = Duration.ofDays(1),
        ),
      ).map { bucket ->
        DailyMacrosMsg(
          dateEpochMs = bucket.startTime.toEpochMilli(),
          nutrientValues = bucket.result.nutrientValues(),
        )
      }
    }

  suspend fun readNutritionEntries(start: Instant, end: Instant): List<NutritionEntryMsg> =
    support.withLogging("readNutritionEntries[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = NutritionRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 200,
      ).map { record ->
        NutritionEntryMsg(
          startEpochMs = record.startTime.toEpochMilli(),
          endEpochMs = record.endTime.toEpochMilli(),
          mealType = record.mealType.toLong(),
          name = record.name,
          source = record.metadata.dataOrigin.packageName,
          id = record.metadata.id,
          clientRecordId = record.metadata.clientRecordId,
          isOpenVitalsEntry = isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName),
          nutrientValues = record.nutrientValues(),
        )
      }
    }

  suspend fun writeNutritionEntry(request: NutritionWriteRequestMsg): String =
    withContext(Dispatchers.IO) {
      support.requireSyncEnabled()
      val nv = request.nutrientValues
      require(nv.isNotEmpty()) { "At least one nutrient must be greater than zero." }
      nv.forEach { (nutrient, value) ->
        require(value > 0.0 && value.isFinite()) { "$nutrient must be greater than zero." }
        require(value <= MaxNutritionNutrientValue) {
          "$nutrient must not exceed ${MaxNutritionNutrientValue.toInt()}."
        }
      }
      val startTime = Instant.ofEpochMilli(request.timeEpochMs)
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
          clientRecordId = clientRecordId,
          device = Device(type = Device.TYPE_PHONE),
        ),
        energy = nv.energy("ENERGY"),
        energyFromFat = nv.energy("ENERGY_FROM_FAT"),
        biotin = nv.mass("BIOTIN"),
        caffeine = nv.mass("CAFFEINE"),
        calcium = nv.mass("CALCIUM"),
        chloride = nv.mass("CHLORIDE"),
        cholesterol = nv.mass("CHOLESTEROL"),
        chromium = nv.mass("CHROMIUM"),
        copper = nv.mass("COPPER"),
        dietaryFiber = nv.mass("DIETARY_FIBER"),
        folate = nv.mass("FOLATE"),
        folicAcid = nv.mass("FOLIC_ACID"),
        iodine = nv.mass("IODINE"),
        iron = nv.mass("IRON"),
        magnesium = nv.mass("MAGNESIUM"),
        manganese = nv.mass("MANGANESE"),
        molybdenum = nv.mass("MOLYBDENUM"),
        monounsaturatedFat = nv.mass("MONOUNSATURATED_FAT"),
        niacin = nv.mass("NIACIN"),
        pantothenicAcid = nv.mass("PANTOTHENIC_ACID"),
        phosphorus = nv.mass("PHOSPHORUS"),
        polyunsaturatedFat = nv.mass("POLYUNSATURATED_FAT"),
        potassium = nv.mass("POTASSIUM"),
        protein = nv.mass("PROTEIN"),
        riboflavin = nv.mass("RIBOFLAVIN"),
        saturatedFat = nv.mass("SATURATED_FAT"),
        selenium = nv.mass("SELENIUM"),
        sodium = nv.mass("SODIUM"),
        sugar = nv.mass("SUGAR"),
        thiamin = nv.mass("THIAMIN"),
        totalCarbohydrate = nv.mass("TOTAL_CARBOHYDRATE"),
        totalFat = nv.mass("TOTAL_FAT"),
        transFat = nv.mass("TRANS_FAT"),
        unsaturatedFat = nv.mass("UNSATURATED_FAT"),
        vitaminA = nv.mass("VITAMIN_A"),
        vitaminB12 = nv.mass("VITAMIN_B12"),
        vitaminB6 = nv.mass("VITAMIN_B6"),
        vitaminC = nv.mass("VITAMIN_C"),
        vitaminD = nv.mass("VITAMIN_D"),
        vitaminE = nv.mass("VITAMIN_E"),
        vitaminK = nv.mass("VITAMIN_K"),
        zinc = nv.mass("ZINC"),
        name = request.name?.trim()?.takeIf { it.isNotBlank() } ?: "OpenVitals nutrition",
        mealType = MealType.MEAL_TYPE_UNKNOWN,
      )
      Log.d(TAG, "Writing nutrition record ${support.diagnosticsSummary()}")
      support.client().insertRecords(listOf(record))
      clientRecordId
    }

  suspend fun deleteNutritionEntry(id: String): String? = withContext(Dispatchers.IO) {
    support.requireSyncEnabled()
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

  suspend fun deleteHydrationNutritionEntry(hydrationClientRecordId: String) = withContext(Dispatchers.IO) {
    if (hydrationClientRecordId.isBlank()) return@withContext
    support.requireSyncEnabled()
    Log.d(TAG, "Deleting paired hydration nutrition record ${support.diagnosticsSummary()}")
    support.client().deleteRecords(
      recordType = NutritionRecord::class,
      recordIdsList = emptyList(),
      clientRecordIdsList = listOf(hydrationNutritionClientRecordId(hydrationClientRecordId)),
    )
  }

  private fun dailyNutritionSeries(start: Instant, end: Instant, zone: ZoneId): List<DailyNutritionMsg> {
    val startDate = start.atZone(zone).toLocalDate()
    val endExclusive = end.atZone(zone).toLocalDate()
    val out = mutableListOf<DailyNutritionMsg>()
    var date = startDate
    while (date.isBefore(endExclusive)) {
      out += DailyNutritionMsg(
        dateEpochMs = date.atStartOfDay(zone).toInstant().toEpochMilli(),
        hydrationLiters = 0.0,
        caloriesBurnedKcal = 0.0,
        caloriesBurnedSource = CaloriesBurnedSourceMsg.NO_DATA,
      )
      date = date.plusDays(1)
    }
    return out
  }

  private fun NutritionRecord.nutrientValues(): Map<String, Double> = buildMap {
    putIfPositive("ENERGY", energy?.inKilocalories)
    putIfPositive("ENERGY_FROM_FAT", energyFromFat?.inKilocalories)
    putIfPositive("BIOTIN", biotin?.inGrams)
    putIfPositive("CAFFEINE", caffeine?.inGrams)
    putIfPositive("CALCIUM", calcium?.inGrams)
    putIfPositive("CHLORIDE", chloride?.inGrams)
    putIfPositive("CHOLESTEROL", cholesterol?.inGrams)
    putIfPositive("CHROMIUM", chromium?.inGrams)
    putIfPositive("COPPER", copper?.inGrams)
    putIfPositive("DIETARY_FIBER", dietaryFiber?.inGrams)
    putIfPositive("FOLATE", folate?.inGrams)
    putIfPositive("FOLIC_ACID", folicAcid?.inGrams)
    putIfPositive("IODINE", iodine?.inGrams)
    putIfPositive("IRON", iron?.inGrams)
    putIfPositive("MAGNESIUM", magnesium?.inGrams)
    putIfPositive("MANGANESE", manganese?.inGrams)
    putIfPositive("MOLYBDENUM", molybdenum?.inGrams)
    putIfPositive("MONOUNSATURATED_FAT", monounsaturatedFat?.inGrams)
    putIfPositive("NIACIN", niacin?.inGrams)
    putIfPositive("PANTOTHENIC_ACID", pantothenicAcid?.inGrams)
    putIfPositive("PHOSPHORUS", phosphorus?.inGrams)
    putIfPositive("POLYUNSATURATED_FAT", polyunsaturatedFat?.inGrams)
    putIfPositive("POTASSIUM", potassium?.inGrams)
    putIfPositive("PROTEIN", protein?.inGrams)
    putIfPositive("RIBOFLAVIN", riboflavin?.inGrams)
    putIfPositive("SATURATED_FAT", saturatedFat?.inGrams)
    putIfPositive("SELENIUM", selenium?.inGrams)
    putIfPositive("SODIUM", sodium?.inGrams)
    putIfPositive("SUGAR", sugar?.inGrams)
    putIfPositive("THIAMIN", thiamin?.inGrams)
    putIfPositive("TOTAL_CARBOHYDRATE", totalCarbohydrate?.inGrams)
    putIfPositive("TOTAL_FAT", totalFat?.inGrams)
    putIfPositive("TRANS_FAT", transFat?.inGrams)
    putIfPositive("UNSATURATED_FAT", unsaturatedFat?.inGrams)
    putIfPositive("VITAMIN_A", vitaminA?.inGrams)
    putIfPositive("VITAMIN_B12", vitaminB12?.inGrams)
    putIfPositive("VITAMIN_B6", vitaminB6?.inGrams)
    putIfPositive("VITAMIN_C", vitaminC?.inGrams)
    putIfPositive("VITAMIN_D", vitaminD?.inGrams)
    putIfPositive("VITAMIN_E", vitaminE?.inGrams)
    putIfPositive("VITAMIN_K", vitaminK?.inGrams)
    putIfPositive("ZINC", zinc?.inGrams)
  }

  private fun AggregationResult.nutrientValues(): Map<String, Double> = buildMap {
    energyAggregates.forEach { (key, metric) ->
      this@nutrientValues[metric]?.inKilocalories?.takeIf { it > 0.0 }?.let { put(key, it) }
    }
    massAggregates.forEach { (key, metric) ->
      this@nutrientValues[metric]?.inGrams?.takeIf { it > 0.0 }?.let { put(key, it) }
    }
  }

  private fun Map<String, Double>.energy(key: String): Energy? = this[key]?.let(Energy::kilocalories)
  private fun Map<String, Double>.mass(key: String): Mass? = this[key]?.let(Mass::grams)

  private companion object {
    private const val TAG = "NutritionHealthReader"
    private const val MaxNutritionNutrientValue = 10000.0

    private val energyAggregates: List<Pair<String, AggregateMetric<Energy>>> = listOf(
      "ENERGY" to NutritionRecord.ENERGY_TOTAL,
      "ENERGY_FROM_FAT" to NutritionRecord.ENERGY_FROM_FAT_TOTAL,
    )

    private val massAggregates: List<Pair<String, AggregateMetric<Mass>>> = listOf(
      "BIOTIN" to NutritionRecord.BIOTIN_TOTAL,
      "CAFFEINE" to NutritionRecord.CAFFEINE_TOTAL,
      "CALCIUM" to NutritionRecord.CALCIUM_TOTAL,
      "CHLORIDE" to NutritionRecord.CHLORIDE_TOTAL,
      "CHOLESTEROL" to NutritionRecord.CHOLESTEROL_TOTAL,
      "CHROMIUM" to NutritionRecord.CHROMIUM_TOTAL,
      "COPPER" to NutritionRecord.COPPER_TOTAL,
      "DIETARY_FIBER" to NutritionRecord.DIETARY_FIBER_TOTAL,
      "FOLATE" to NutritionRecord.FOLATE_TOTAL,
      "FOLIC_ACID" to NutritionRecord.FOLIC_ACID_TOTAL,
      "IODINE" to NutritionRecord.IODINE_TOTAL,
      "IRON" to NutritionRecord.IRON_TOTAL,
      "MAGNESIUM" to NutritionRecord.MAGNESIUM_TOTAL,
      "MANGANESE" to NutritionRecord.MANGANESE_TOTAL,
      "MOLYBDENUM" to NutritionRecord.MOLYBDENUM_TOTAL,
      "MONOUNSATURATED_FAT" to NutritionRecord.MONOUNSATURATED_FAT_TOTAL,
      "NIACIN" to NutritionRecord.NIACIN_TOTAL,
      "PANTOTHENIC_ACID" to NutritionRecord.PANTOTHENIC_ACID_TOTAL,
      "PHOSPHORUS" to NutritionRecord.PHOSPHORUS_TOTAL,
      "POLYUNSATURATED_FAT" to NutritionRecord.POLYUNSATURATED_FAT_TOTAL,
      "POTASSIUM" to NutritionRecord.POTASSIUM_TOTAL,
      "PROTEIN" to NutritionRecord.PROTEIN_TOTAL,
      "RIBOFLAVIN" to NutritionRecord.RIBOFLAVIN_TOTAL,
      "SATURATED_FAT" to NutritionRecord.SATURATED_FAT_TOTAL,
      "SELENIUM" to NutritionRecord.SELENIUM_TOTAL,
      "SODIUM" to NutritionRecord.SODIUM_TOTAL,
      "SUGAR" to NutritionRecord.SUGAR_TOTAL,
      "THIAMIN" to NutritionRecord.THIAMIN_TOTAL,
      "TOTAL_CARBOHYDRATE" to NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL,
      "TOTAL_FAT" to NutritionRecord.TOTAL_FAT_TOTAL,
      "TRANS_FAT" to NutritionRecord.TRANS_FAT_TOTAL,
      "UNSATURATED_FAT" to NutritionRecord.UNSATURATED_FAT_TOTAL,
      "VITAMIN_A" to NutritionRecord.VITAMIN_A_TOTAL,
      "VITAMIN_B12" to NutritionRecord.VITAMIN_B12_TOTAL,
      "VITAMIN_B6" to NutritionRecord.VITAMIN_B6_TOTAL,
      "VITAMIN_C" to NutritionRecord.VITAMIN_C_TOTAL,
      "VITAMIN_D" to NutritionRecord.VITAMIN_D_TOTAL,
      "VITAMIN_E" to NutritionRecord.VITAMIN_E_TOTAL,
      "VITAMIN_K" to NutritionRecord.VITAMIN_K_TOTAL,
      "ZINC" to NutritionRecord.ZINC_TOTAL,
    )

    private val nutritionAggregateMetrics: Set<AggregateMetric<*>> =
      (energyAggregates.map { it.second } + massAggregates.map { it.second }).toSet()
  }
}

private fun MutableMap<String, Double>.putIfPositive(key: String, value: Double?) {
  if (value != null && value > 0.0) put(key, value)
}

private const val HydrationNutritionClientRecordIdPrefix = "openvitals_hydration_nutrition_"

internal fun hydrationNutritionClientRecordId(hydrationClientRecordId: String): String =
  "$HydrationNutritionClientRecordIdPrefix$hydrationClientRecordId"
