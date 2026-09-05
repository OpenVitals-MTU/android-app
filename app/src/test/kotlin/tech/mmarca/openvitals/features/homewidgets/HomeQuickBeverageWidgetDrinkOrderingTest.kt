package tech.mmarca.openvitals.features.homewidgets

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BeverageCategory
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink

class HomeQuickBeverageWidgetDrinkOrderingTest {
    @Test
    fun `orders frequent then custom then catalog drinks by category and name`() {
        val cola = drink(
            id = "cola",
            name = "Cola",
            category = BeverageCategory.SODA,
            isPreloaded = true,
        )
        val stillWater = drink(
            id = "still-water",
            name = "Still water",
            category = BeverageCategory.WATER,
            isPreloaded = true,
        )
        val sparklingWater = drink(
            id = "sparkling-water",
            name = "Sparkling water",
            category = BeverageCategory.WATER,
            isPreloaded = true,
        )
        val latte = drink(
            id = "latte",
            name = "Latte",
            category = BeverageCategory.COFFEE,
            isPreloaded = true,
        )
        val espresso = drink(
            id = "espresso",
            name = "Espresso",
            category = BeverageCategory.COFFEE,
            isPreloaded = true,
        )
        val customCoffee = drink(
            id = "custom-coffee",
            name = "Aeropress",
            category = BeverageCategory.COFFEE,
        )
        val customTea = drink(
            id = "custom-tea",
            name = "Assam tea",
            category = BeverageCategory.TEA,
        )
        val clubSoda = drink(
            id = "club-soda",
            name = "Club soda",
            category = BeverageCategory.SODA,
            isPreloaded = true,
        )
        val otherDrink = drink(
            id = "other",
            name = "Cider",
            category = BeverageCategory.OTHER,
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

    @Test
    fun `a frequent drink that is no longer in the catalog is dropped`() {
        val water = drink(id = "water", name = "water", category = BeverageCategory.WATER)

        val orderedDrinks = quickBeverageWidgetDrinkOptions(
            drinks = listOf(water),
            frequentDrinks = listOf(drink(id = "deleted", name = "deleted", category = null)),
        )

        assertEquals(listOf("water"), orderedDrinks.map(CustomHydrationDrink::id))
    }

    @Test
    fun `user drinks come before the preloaded catalog`() {
        val preloadedWater = drink(
            id = "preloaded_water",
            name = "preloaded_water",
            category = BeverageCategory.WATER,
            isPreloaded = true,
        )
        // The custom/preloaded split is the outer sort key.
        val userOther = drink(
            id = "user_other",
            name = "user_other",
            category = BeverageCategory.OTHER,
        )

        val orderedDrinks = quickBeverageWidgetDrinkOptions(
            drinks = listOf(preloadedWater, userOther),
            frequentDrinks = emptyList(),
        )

        assertEquals(
            listOf("user_other", "preloaded_water"),
            orderedDrinks.map(CustomHydrationDrink::id),
        )
    }

    @Test
    fun `sorts each group by category, then name, then id`() {
        val drinks = listOf(
            drink(id = "z_soda", name = "Soda", category = BeverageCategory.SODA),
            drink(id = "b_coffee", name = "beta", category = BeverageCategory.COFFEE),
            drink(id = "a_coffee", name = "Alpha", category = BeverageCategory.COFFEE),
            drink(id = "tea", name = "Tea", category = BeverageCategory.TEA),
            drink(id = "energy", name = "Energy", category = BeverageCategory.ENERGY_DRINK),
            drink(id = "water", name = "Water", category = BeverageCategory.WATER),
            drink(id = "choc", name = "Choc", category = BeverageCategory.CHOCOLATE),
            drink(id = "supp", name = "Supp", category = BeverageCategory.SUPPLEMENT),
            drink(id = "none", name = "None", category = null),
        )

        val orderedDrinks = quickBeverageWidgetDrinkOptions(
            drinks = drinks,
            frequentDrinks = emptyList(),
        )

        // Category order, then name lowercased inside a category.
        assertEquals(
            listOf(
                "water",
                "a_coffee",
                "b_coffee",
                "energy",
                "tea",
                "choc",
                "z_soda",
                "none",
                "supp",
            ),
            orderedDrinks.map(CustomHydrationDrink::id),
        )
    }

    @Test
    fun `breaks a same-name tie on the id`() {
        val orderedDrinks = quickBeverageWidgetDrinkOptions(
            drinks = listOf(
                drink(id = "b", name = "Coffee", category = BeverageCategory.COFFEE),
                drink(id = "a", name = "coffee", category = BeverageCategory.COFFEE),
            ),
            frequentDrinks = emptyList(),
        )

        assertEquals(listOf("a", "b"), orderedDrinks.map(CustomHydrationDrink::id))
    }

    @Test
    fun `an empty catalog yields no options`() {
        val orderedDrinks = quickBeverageWidgetDrinkOptions(
            drinks = emptyList(),
            frequentDrinks = listOf(drink(id = "water", name = "water", category = BeverageCategory.WATER)),
        )

        assertEquals(emptyList<CustomHydrationDrink>(), orderedDrinks)
    }

    @Test
    fun `the picker lists the drinks as name - amount, in catalog order`() {
        val espresso = CustomHydrationDrink(
            id = "espresso",
            name = "Espresso",
            volumeMilliliters = 30.0,
            category = BeverageCategory.COFFEE,
        )
        val bottle = CustomHydrationDrink(
            id = "bottle",
            name = "Water bottle",
            volumeMilliliters = 1_500.0,
            category = BeverageCategory.WATER,
        )

        val orderedDrinks = quickBeverageWidgetDrinkOptions(
            drinks = listOf(espresso, bottle),
            frequentDrinks = emptyList(),
        )

        assertEquals(
            listOf("Water bottle - 1.50 L", "Espresso - 30 ml"),
            quickBeverageWidgetPickerLabels(orderedDrinks, unitFormatter()),
        )
    }

    @Test
    fun `the picker has nothing to list for an empty catalog`() {
        assertEquals(
            emptyList<String>(),
            quickBeverageWidgetPickerLabels(emptyList(), unitFormatter()),
        )
    }

    private fun drink(
        id: String,
        name: String,
        category: BeverageCategory?,
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
