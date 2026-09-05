package tech.mmarca.openvitals.features.homewidgets

import java.util.Locale
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BeverageCategory
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.features.manualentry.hydration.hydrationAmountLabel

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

/** The picker's labels: "name - amount", using the hydration entry screen's own amount label. */
internal fun quickBeverageWidgetPickerLabels(
    drinks: List<CustomHydrationDrink>,
    unitFormatter: UnitFormatter,
): List<String> =
    drinks.map { drink -> "${drink.name} - ${hydrationAmountLabel(drink.volumeLiters, unitFormatter)}" }

private val QuickBeverageDrinkComparator = compareBy<CustomHydrationDrink>(
    { drink -> drink.category.quickBeverageCategoryOrder() },
    { drink -> drink.name.lowercase(Locale.getDefault()) },
    { drink -> drink.id },
)

private fun BeverageCategory?.quickBeverageCategoryOrder(): Int =
    when (this) {
        BeverageCategory.WATER -> 0
        BeverageCategory.COFFEE -> 1
        BeverageCategory.ENERGY_DRINK -> 2
        BeverageCategory.TEA -> 3
        BeverageCategory.CHOCOLATE -> 4
        BeverageCategory.SODA -> 5
        BeverageCategory.BEER -> 6
        BeverageCategory.SUPPLEMENT,
        BeverageCategory.OTHER,
        null,
        -> 7
    }
