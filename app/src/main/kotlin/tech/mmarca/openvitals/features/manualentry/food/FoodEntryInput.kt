package tech.mmarca.openvitals.features.manualentry.food

import java.util.Locale
import kotlin.math.round
import tech.mmarca.openvitals.domain.model.CustomFood
import tech.mmarca.openvitals.domain.model.FoodCategory
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.manualentry.nutrition.isValidNutrientInputValue

internal const val MinFoodAmountGrams = 1.0
internal const val MaxFoodAmountGrams = 10000.0
internal const val GramsPerOunce = 28.349523125

/** What the food form hands back. [toCustomFood] validates it. */
data class CustomFoodInput(
    val name: String,
    val amountGrams: Double,
    val category: FoodCategory? = null,
    val nutrientValues: Map<NutritionNutrient, Double> = emptyMap(),
)

internal fun isValidFoodAmountGrams(grams: Double): Boolean =
    grams >= MinFoodAmountGrams &&
        grams <= MaxFoodAmountGrams &&
        grams.isFinite()

/**
 * Null for a blank name, an amount out of range, no nutrient, or a nutrient out of range.
 * A food needs one nutrient: a portion with none has nothing to write.
 */
internal fun CustomFoodInput.toCustomFood(id: String): CustomFood? {
    val normalizedName = name.trim()
    if (normalizedName.isBlank()) return null
    if (!isValidFoodAmountGrams(amountGrams)) return null
    if (nutrientValues.isEmpty()) return null
    if (!nutrientValues.values.all(::isValidNutrientInputValue)) return null
    return CustomFood(
        id = id,
        name = normalizedName,
        amountGrams = amountGrams,
        nutrientValues = nutrientValues.toSortedMap(compareBy { it.name }),
        category = category,
    )
}

internal fun CustomFood.isValidCustomFood(): Boolean =
    id.isNotBlank() &&
        name.isNotBlank() &&
        isValidFoodAmountGrams(amountGrams) &&
        nutrientValues.isNotEmpty() &&
        nutrientValues.values.all(::isValidNutrientInputValue)

/** The nutrients of a portion of [amountGrams], scaled from the food's own amount. */
internal fun CustomFood.nutrientValuesFor(amountGrams: Double): Map<NutritionNutrient, Double> {
    val portion = amountGrams / this.amountGrams
    return nutrientValues.mapValues { (_, value) -> value * portion }
}

internal fun foodInputGrams(input: String, unitSystem: UnitSystem): Double? {
    val value = input.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return when (unitSystem) {
        UnitSystem.METRIC -> value
        UnitSystem.IMPERIAL -> value * GramsPerOunce
    }
}

/** Input text, not display text: no grouping and a '.' decimal, so [foodInputGrams] reads it back. */
internal fun foodInputAmountText(grams: Double?, unitSystem: UnitSystem): String {
    if (grams == null) return ""
    return when (unitSystem) {
        UnitSystem.METRIC -> if (grams == round(grams)) grams.toLong().toString() else "%.1f".format(Locale.US, grams)
        UnitSystem.IMPERIAL -> "%.1f".format(Locale.US, grams / GramsPerOunce)
    }
}

internal fun foodInputUnitLabel(unitSystem: UnitSystem): String =
    when (unitSystem) {
        UnitSystem.METRIC -> "g"
        UnitSystem.IMPERIAL -> "oz"
    }

/** One section of the catalog: a category, or the foods without one when [category] is null. */
internal data class FoodCatalogSection(
    val category: FoodCategory?,
    val foods: List<CustomFood>,
)

/** Foods without a category first, then each category in its declared order. Empty sections are left out. */
internal fun groupFoodCatalog(foods: List<CustomFood>, query: String = ""): List<FoodCatalogSection> {
    val normalizedQuery = query.trim()
    val matching = if (normalizedQuery.isBlank()) {
        foods
    } else {
        foods.filter { it.name.contains(normalizedQuery, ignoreCase = true) }
    }
    val byCategory = matching.groupBy { it.category }
    return (listOf(null) + FoodCategory.entries).mapNotNull { category ->
        byCategory[category]?.let { FoodCatalogSection(category, it) }
    }
}
