package tech.mmarca.openvitals.features.homewidgets

import java.util.Locale
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink

internal fun quickBeverageWidgetDrinkOptions(
    drinks: List<CustomHydrationDrink>,
    frequentDrinks: List<CustomHydrationDrink>,
): List<CustomHydrationDrink> {
    if (drinks.isEmpty()) return emptyList()
    val drinkById = drinks.associateBy { drink -> drink.id }
    val frequentOptions = frequentDrinks.mapNotNull { drink -> drinkById[drink.id] }
    val frequentIds = frequentOptions.mapTo(mutableSetOf()) { drink -> drink.id }
    val customOptions = drinks
        .filterNot { drink -> drink.isPreloaded || drink.id in frequentIds }
        .sortedWith(QuickBeverageDrinkComparator)
    val customIds = customOptions.mapTo(mutableSetOf()) { drink -> drink.id }
    val catalogOptions = drinks
        .filterNot { drink -> drink.id in frequentIds || drink.id in customIds }
        .sortedWith(QuickBeverageDrinkComparator)

    return frequentOptions + customOptions + catalogOptions
}

private val QuickBeverageDrinkComparator = compareBy<CustomHydrationDrink>(
    { drink -> drink.category.quickBeverageCategoryOrder() },
    { drink -> drink.name.lowercase(Locale.getDefault()) },
    { drink -> drink.id },
)

private fun CaffeineSourceCategory?.quickBeverageCategoryOrder(): Int =
    when (this) {
        CaffeineSourceCategory.WATER -> 0
        CaffeineSourceCategory.COFFEE -> 1
        CaffeineSourceCategory.ENERGY_DRINK -> 2
        CaffeineSourceCategory.TEA -> 3
        CaffeineSourceCategory.CHOCOLATE -> 4
        CaffeineSourceCategory.SODA -> 5
        CaffeineSourceCategory.SUPPLEMENT,
        CaffeineSourceCategory.OTHER,
        null,
        -> 6
    }
