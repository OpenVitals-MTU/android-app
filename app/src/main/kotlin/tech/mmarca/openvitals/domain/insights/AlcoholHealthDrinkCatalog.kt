package tech.mmarca.openvitals.domain.insights

import tech.mmarca.openvitals.domain.model.BeverageCategory
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.model.NutritionNutrient

/**
 * OpenVitals' own catalog of beers, alcoholic and alcohol-free. Unlike
 * [CaffeineHealthDrinkCatalog] the nutrients live on each item, and alcoholic
 * drinks carry a reduced hydration multiplier so a serving never counts as a
 * full serving of water.
 */
object AlcoholHealthDrinkCatalog : HydrationDrinkCatalog {
    private const val BeveragePresetIdPrefix = "alcoholhealth-"
    private const val BeerHydrationMultiplier = 0.6

    data class AlcoholCatalogItem(
        val id: String,
        val name: String,
        val defaultServingMilliliters: Double,
        val hydrationMultiplier: Double,
        val nutrientValues: Map<NutritionNutrient, Double>,
    )

    val items: List<AlcoholCatalogItem> = listOf(
        AlcoholCatalogItem(
            id = "beer",
            name = "Beer",
            defaultServingMilliliters = 330.0,
            hydrationMultiplier = BeerHydrationMultiplier,
            nutrientValues = nutrients(
                energyKcal = 142.0,
                totalCarbohydrateGrams = 10.6,
                proteinGrams = 1.5,
                sodiumMilligrams = 13.0,
                potassiumMilligrams = 90.0,
                calciumMilligrams = 13.0,
            ),
        ),
        AlcoholCatalogItem(
            id = "lager",
            name = "Lager",
            defaultServingMilliliters = 330.0,
            hydrationMultiplier = BeerHydrationMultiplier,
            nutrientValues = nutrients(
                energyKcal = 135.0,
                totalCarbohydrateGrams = 10.2,
                proteinGrams = 1.2,
                sodiumMilligrams = 13.0,
                potassiumMilligrams = 90.0,
                calciumMilligrams = 13.0,
            ),
        ),
        AlcoholCatalogItem(
            id = "pilsner",
            name = "Pilsner",
            defaultServingMilliliters = 330.0,
            hydrationMultiplier = BeerHydrationMultiplier,
            nutrientValues = nutrients(
                energyKcal = 132.0,
                totalCarbohydrateGrams = 9.2,
                proteinGrams = 1.5,
                sodiumMilligrams = 13.0,
                potassiumMilligrams = 90.0,
                calciumMilligrams = 13.0,
            ),
        ),
        AlcoholCatalogItem(
            id = "stout",
            name = "Stout",
            defaultServingMilliliters = 440.0,
            hydrationMultiplier = BeerHydrationMultiplier,
            nutrientValues = nutrients(
                energyKcal = 154.0,
                totalCarbohydrateGrams = 13.2,
                proteinGrams = 1.9,
                sodiumMilligrams = 18.0,
                potassiumMilligrams = 120.0,
                calciumMilligrams = 18.0,
            ),
        ),
        AlcoholCatalogItem(
            id = "alcohol-free-beer",
            name = "Alcohol-free beer",
            defaultServingMilliliters = 330.0,
            hydrationMultiplier = 1.0,
            nutrientValues = nutrients(
                energyKcal = 80.0,
                totalCarbohydrateGrams = 17.5,
                sugarGrams = 3.5,
                proteinGrams = 1.3,
                sodiumMilligrams = 13.0,
                potassiumMilligrams = 100.0,
                calciumMilligrams = 16.0,
            ),
        ),
    )

    override fun beveragePresets(): List<CustomHydrationDrink> =
        items.map { item ->
            CustomHydrationDrink(
                id = "$BeveragePresetIdPrefix${item.id}",
                name = item.name,
                volumeMilliliters = item.defaultServingMilliliters,
                hydrationMultiplier = item.hydrationMultiplier,
                nutrientValues = item.nutrientValues,
                category = BeverageCategory.BEER,
                isPreloaded = true,
            )
        }

    private fun nutrients(
        energyKcal: Double? = null,
        totalCarbohydrateGrams: Double? = null,
        sugarGrams: Double? = null,
        proteinGrams: Double? = null,
        sodiumMilligrams: Double? = null,
        potassiumMilligrams: Double? = null,
        calciumMilligrams: Double? = null,
    ): Map<NutritionNutrient, Double> = buildMap {
        putPositive(NutritionNutrient.ENERGY, energyKcal)
        putPositive(NutritionNutrient.TOTAL_CARBOHYDRATE, totalCarbohydrateGrams)
        putPositive(NutritionNutrient.SUGAR, sugarGrams)
        putPositive(NutritionNutrient.PROTEIN, proteinGrams)
        putPositive(NutritionNutrient.SODIUM, sodiumMilligrams?.div(MilligramsPerGram))
        putPositive(NutritionNutrient.POTASSIUM, potassiumMilligrams?.div(MilligramsPerGram))
        putPositive(NutritionNutrient.CALCIUM, calciumMilligrams?.div(MilligramsPerGram))
    }

    private fun MutableMap<NutritionNutrient, Double>.putPositive(
        nutrient: NutritionNutrient,
        value: Double?,
    ) {
        if (value != null && value > 0.0 && value.isFinite()) {
            put(nutrient, value)
        }
    }

    private const val MilligramsPerGram = 1000.0
}
