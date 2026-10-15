package tech.mmarca.openvitals.domain.model

/** The client-record-id prefix of a nutrition record a logged food wrote. */
const val OpenVitalsFoodClientRecordPrefix = "openvitals_food_"

/** The sections of the food catalog. Stored by name, so rename with care. */
enum class FoodCategory {
    FRUIT,
    VEGETABLE,
    GRAIN,
    PROTEIN,
    DAIRY,
    SNACK,
    MEAL,
    DESSERT,
    OTHER,
}

/**
 * A food the user made. [nutrientValues] are for [amountGrams]; a logged
 * portion scales them. Energy is in kcal, every other nutrient in grams.
 */
data class CustomFood(
    val id: String,
    val name: String,
    val amountGrams: Double,
    val nutrientValues: Map<NutritionNutrient, Double> = emptyMap(),
    val category: FoodCategory? = null,
)
