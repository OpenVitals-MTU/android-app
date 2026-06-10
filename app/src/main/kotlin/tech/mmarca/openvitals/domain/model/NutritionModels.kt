package tech.mmarca.openvitals.domain.model

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
    val nutrientValues: Map<NutritionNutrient, Double> = emptyMap(),
)

data class DailyMacros(
    val date: LocalDate,
    val nutrientValues: Map<NutritionNutrient, Double> = emptyMap(),
    val energyKcal: Double = nutrientValues[NutritionNutrient.ENERGY] ?: 0.0,
    val proteinGrams: Double = nutrientValues[NutritionNutrient.PROTEIN] ?: 0.0,
    val carbsGrams: Double = nutrientValues[NutritionNutrient.TOTAL_CARBOHYDRATE] ?: 0.0,
    val fatGrams: Double = nutrientValues[NutritionNutrient.TOTAL_FAT] ?: 0.0,
)

enum class NutritionNutrient(val unit: NutritionNutrientUnit, val group: NutritionNutrientGroup) {
    ENERGY(NutritionNutrientUnit.ENERGY_KCAL, NutritionNutrientGroup.OVERVIEW),
    PROTEIN(NutritionNutrientUnit.MASS_GRAMS, NutritionNutrientGroup.OVERVIEW),
    TOTAL_CARBOHYDRATE(NutritionNutrientUnit.MASS_GRAMS, NutritionNutrientGroup.OVERVIEW),
    TOTAL_FAT(NutritionNutrientUnit.MASS_GRAMS, NutritionNutrientGroup.OVERVIEW),
    DIETARY_FIBER(NutritionNutrientUnit.MASS_GRAMS, NutritionNutrientGroup.CARBOHYDRATES),
    SUGAR(NutritionNutrientUnit.MASS_GRAMS, NutritionNutrientGroup.CARBOHYDRATES),
    ENERGY_FROM_FAT(NutritionNutrientUnit.ENERGY_KCAL, NutritionNutrientGroup.FATS),
    MONOUNSATURATED_FAT(NutritionNutrientUnit.MASS_GRAMS, NutritionNutrientGroup.FATS),
    POLYUNSATURATED_FAT(NutritionNutrientUnit.MASS_GRAMS, NutritionNutrientGroup.FATS),
    SATURATED_FAT(NutritionNutrientUnit.MASS_GRAMS, NutritionNutrientGroup.FATS),
    TRANS_FAT(NutritionNutrientUnit.MASS_GRAMS, NutritionNutrientGroup.FATS),
    UNSATURATED_FAT(NutritionNutrientUnit.MASS_GRAMS, NutritionNutrientGroup.FATS),
    CHOLESTEROL(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.FATS),
    BIOTIN(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.VITAMINS),
    FOLATE(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.VITAMINS),
    FOLIC_ACID(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.VITAMINS),
    NIACIN(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.VITAMINS),
    PANTOTHENIC_ACID(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.VITAMINS),
    RIBOFLAVIN(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.VITAMINS),
    THIAMIN(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.VITAMINS),
    VITAMIN_A(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.VITAMINS),
    VITAMIN_B12(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.VITAMINS),
    VITAMIN_B6(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.VITAMINS),
    VITAMIN_C(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.VITAMINS),
    VITAMIN_D(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.VITAMINS),
    VITAMIN_E(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.VITAMINS),
    VITAMIN_K(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.VITAMINS),
    CALCIUM(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.MINERALS),
    CHLORIDE(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.MINERALS),
    CHROMIUM(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.MINERALS),
    COPPER(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.MINERALS),
    IODINE(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.MINERALS),
    IRON(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.MINERALS),
    MAGNESIUM(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.MINERALS),
    MANGANESE(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.MINERALS),
    MOLYBDENUM(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.MINERALS),
    PHOSPHORUS(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.MINERALS),
    POTASSIUM(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.MINERALS),
    SELENIUM(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.MINERALS),
    SODIUM(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.MINERALS),
    ZINC(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.MINERALS),
    CAFFEINE(NutritionNutrientUnit.MASS_ADAPTIVE, NutritionNutrientGroup.OTHER),
}

enum class NutritionNutrientUnit {
    ENERGY_KCAL,
    MASS_GRAMS,
    MASS_ADAPTIVE,
}

enum class NutritionNutrientGroup {
    OVERVIEW,
    CARBOHYDRATES,
    FATS,
    VITAMINS,
    MINERALS,
    OTHER,
}

fun DailyMacros.valueFor(nutrient: NutritionNutrient): Double =
    nutrientValues[nutrient] ?: when (nutrient) {
        NutritionNutrient.ENERGY -> energyKcal
        NutritionNutrient.PROTEIN -> proteinGrams
        NutritionNutrient.TOTAL_CARBOHYDRATE -> carbsGrams
        NutritionNutrient.TOTAL_FAT -> fatGrams
        else -> 0.0
    }

fun NutritionEntry.valueFor(nutrient: NutritionNutrient): Double? =
    nutrientValues[nutrient] ?: when (nutrient) {
        NutritionNutrient.ENERGY -> energyKcal
        NutritionNutrient.PROTEIN -> proteinGrams
        NutritionNutrient.TOTAL_CARBOHYDRATE -> carbsGrams
        NutritionNutrient.TOTAL_FAT -> fatGrams
        NutritionNutrient.DIETARY_FIBER -> fiberGrams
        NutritionNutrient.SUGAR -> sugarGrams
        else -> null
    }
