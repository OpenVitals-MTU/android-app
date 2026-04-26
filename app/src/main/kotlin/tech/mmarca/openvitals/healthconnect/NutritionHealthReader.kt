package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import tech.mmarca.openvitals.data.model.DailyMacros
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.NutritionEntry
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal class NutritionHealthReader(
    private val support: HealthConnectReaderSupport,
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
    ): List<DailyNutrition> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withLogging("readDailyNutrition[$start..$end]", emptyList()) {
            val metrics = buildSet {
                if (includeHydration) add(HydrationRecord.VOLUME_TOTAL)
                if (includeCalories) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
            }
            val aggregateRows = support.client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).map { bucket ->
                DailyNutrition(
                    date = bucket.startTime.atZone(zone).toLocalDate(),
                    hydrationLiters = if (includeHydration) {
                        bucket.result[HydrationRecord.VOLUME_TOTAL]?.inLiters ?: 0.0
                    } else {
                        0.0
                    },
                    caloriesBurnedKcal = if (includeCalories) {
                        bucket.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
                    } else {
                        0.0
                    },
                )
            }
            if (!includeHydration || aggregateRows.any { it.hydrationLiters > 0.0 }) {
                aggregateRows
            } else {
                val hydrationByDate = support.client().readHydrationRecordsByDate(start, end, zone)
                if (aggregateRows.isEmpty() && hydrationByDate.isNotEmpty()) {
                    dailyNutritionSeries(startDate, endDate, hydrationByDate)
                } else {
                    aggregateRows.map { row ->
                        row.copy(hydrationLiters = hydrationByDate[row.date] ?: row.hydrationLiters)
                    }
                }
            }
        }
    }

    suspend fun readDailyMacros(startDate: LocalDate, endDate: LocalDate): List<DailyMacros> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return support.withLogging("readDailyMacros[$start..$end]", emptyList()) {
            support.client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(
                        NutritionRecord.ENERGY_TOTAL,
                        NutritionRecord.PROTEIN_TOTAL,
                        NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL,
                        NutritionRecord.TOTAL_FAT_TOTAL,
                    ),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofDays(1),
                )
            ).map { bucket ->
                DailyMacros(
                    date = bucket.startTime.atZone(zone).toLocalDate(),
                    energyKcal = bucket.result[NutritionRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0,
                    proteinGrams = bucket.result[NutritionRecord.PROTEIN_TOTAL]?.inGrams ?: 0.0,
                    carbsGrams = bucket.result[NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL]?.inGrams ?: 0.0,
                    fatGrams = bucket.result[NutritionRecord.TOTAL_FAT_TOTAL]?.inGrams ?: 0.0,
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
                NutritionEntry(
                    time = record.startTime,
                    mealType = record.mealType,
                    name = record.name,
                    energyKcal = record.energy?.inKilocalories,
                    proteinGrams = record.protein?.inGrams,
                    carbsGrams = record.totalCarbohydrate?.inGrams,
                    fatGrams = record.totalFat?.inGrams,
                    fiberGrams = record.dietaryFiber?.inGrams,
                    sugarGrams = record.sugar?.inGrams,
                    source = record.metadata.dataOrigin.packageName,
                )
            }
        }
}

private fun dailyNutritionSeries(
    startDate: LocalDate,
    endDate: LocalDate,
    hydrationByDate: Map<LocalDate, Double>,
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
