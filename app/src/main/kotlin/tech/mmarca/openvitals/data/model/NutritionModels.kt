package tech.mmarca.openvitals.data.model

import java.time.Instant
import java.time.LocalDate

data class DailyNutrition(
    val date: LocalDate,
    val hydrationLiters: Double,
    val caloriesBurnedKcal: Double,
    val caloriesBurnedSource: CaloriesBurnedSource = if (caloriesBurnedKcal > 0.0) {
        CaloriesBurnedSource.RECORDED_TOTAL
    } else {
        CaloriesBurnedSource.NO_DATA
    },
    val hasCaloriesBurnedData: Boolean = caloriesBurnedSource != CaloriesBurnedSource.NO_DATA,
)

enum class CaloriesBurnedSource {
    NO_DATA,
    RECORDED_TOTAL,
    ESTIMATED_ACTIVE_AND_BMR,
}

data class CaloriesBurnedValue(
    val kcal: Double,
    val source: CaloriesBurnedSource,
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
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
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
