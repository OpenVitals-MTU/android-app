package tech.mmarca.openvitals.features.homewidgets

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink

class HomeQuickBeverageWidgetDrinkOrderingTest {
    @Test
    fun `orders frequent then custom then catalog drinks by category and name`() {
        val cola = drink(
            id = "cola",
            name = "Cola",
            category = CaffeineSourceCategory.SODA,
            isPreloaded = true,
        )
        val stillWater = drink(
            id = "still-water",
            name = "Still water",
            category = CaffeineSourceCategory.WATER,
            isPreloaded = true,
        )
        val sparklingWater = drink(
            id = "sparkling-water",
            name = "Sparkling water",
            category = CaffeineSourceCategory.WATER,
            isPreloaded = true,
        )
        val latte = drink(
            id = "latte",
            name = "Latte",
            category = CaffeineSourceCategory.COFFEE,
            isPreloaded = true,
        )
        val espresso = drink(
            id = "espresso",
            name = "Espresso",
            category = CaffeineSourceCategory.COFFEE,
            isPreloaded = true,
        )
        val customCoffee = drink(
            id = "custom-coffee",
            name = "Aeropress",
            category = CaffeineSourceCategory.COFFEE,
        )
        val customTea = drink(
            id = "custom-tea",
            name = "Assam tea",
            category = CaffeineSourceCategory.TEA,
        )
        val clubSoda = drink(
            id = "club-soda",
            name = "Club soda",
            category = CaffeineSourceCategory.SODA,
            isPreloaded = true,
        )
        val otherDrink = drink(
            id = "other",
            name = "Cider",
            category = CaffeineSourceCategory.OTHER,
            isPreloaded = true,
        )

        val orderedDrinks = quickBeverageWidgetDrinkOptions(
            drinks = listOf(
                cola,
                stillWater,
                sparklingWater,
                latte,
                espresso,
                customCoffee,
                customTea,
                clubSoda,
                otherDrink,
            ),
            frequentDrinks = listOf(cola, stillWater),
        )

        assertEquals(
            listOf(
                "cola",
                "still-water",
                "custom-coffee",
                "custom-tea",
                "sparkling-water",
                "espresso",
                "latte",
                "club-soda",
                "other",
            ),
            orderedDrinks.map(CustomHydrationDrink::id),
        )
    }

    private fun drink(
        id: String,
        name: String,
        category: CaffeineSourceCategory,
        isPreloaded: Boolean = false,
    ): CustomHydrationDrink =
        CustomHydrationDrink(
            id = id,
            name = name,
            volumeMilliliters = 250.0,
            category = category,
            isPreloaded = isPreloaded,
        )
}
