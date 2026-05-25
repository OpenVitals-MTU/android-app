package tech.mmarca.openvitals.data.model

import java.time.Instant
import java.time.LocalDate

data class DailyNutrition(
    val date: LocalDate,
    val hydrationLiters: Double,
    val caloriesBurnedKcal: Double,
)

data class DailyHydration(
    val date: LocalDate,
    val liters: Double,
)

data class HydrationEntry(
    val startTime: Instant,
    val endTime: Instant,
    val liters: Double,
    val source: String,
)

data class HydrationWriteRequest(
    val time: Instant,
    val volumeLiters: Double,
)

data class NutritionEntry(
    val time: Instant,
    val mealType: Int,
    val name: String?,
    val energyKcal: Double?,
    val proteinGrams: Double?,
    val carbsGrams: Double?,
    val fatGrams: Double?,
    val fiberGrams: Double?,
    val sugarGrams: Double?,
    val source: String,
)

data class DailyMacros(
    val date: LocalDate,
    val energyKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
)
