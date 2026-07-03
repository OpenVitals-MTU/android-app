package tech.mmarca.openvitals.domain.insights

import java.util.Locale
import tech.mmarca.openvitals.domain.model.CaffeineCatalogItem
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory
import tech.mmarca.openvitals.domain.model.NutritionNutrient

object BeverageNutritionDefaults {
    fun nutrientValuesFor(item: CaffeineCatalogItem): Map<NutritionNutrient, Double> {
        val name = item.name.lowercase(Locale.ROOT)
        val volumeMilliliters = item.defaultServingMilliliters ?: 240.0
        val base = when (item.category) {
            CaffeineSourceCategory.WATER -> emptyMap()
            CaffeineSourceCategory.COFFEE -> coffeeDefaults(name, volumeMilliliters)
            CaffeineSourceCategory.TEA -> teaDefaults(name, volumeMilliliters)
            CaffeineSourceCategory.ENERGY_DRINK -> energyDrinkDefaults(name, volumeMilliliters)
            CaffeineSourceCategory.SODA -> sodaDefaults(name, volumeMilliliters)
            CaffeineSourceCategory.CHOCOLATE -> chocolateDefaults(name, volumeMilliliters)
            CaffeineSourceCategory.SUPPLEMENT,
            CaffeineSourceCategory.OTHER,
            -> emptyMap()
        }
        return base.withCaffeine(item.typicalCaffeineMg)
    }

    private fun coffeeDefaults(name: String, volumeMilliliters: Double): Map<NutritionNutrient, Double> =
        when {
            name.containsAny("atkins", "protein shake") -> nutrients(
                energyKcal = 165.0,
                totalFatGrams = 9.0,
                saturatedFatGrams = 1.5,
                totalCarbohydrateGrams = 7.0,
                sugarGrams = 1.0,
                dietaryFiberGrams = 5.0,
                proteinGrams = 15.0,
                sodiumMilligrams = 240.0,
                calciumMilligrams = 350.0,
            )
            name.containsAny("mocha", "white coffee", "dunkaccino", "frappe") -> nutrients(
                energyKcal = scaled(290.0, volumeMilliliters, 354.0),
                totalFatGrams = scaled(12.0, volumeMilliliters, 354.0),
                saturatedFatGrams = scaled(7.0, volumeMilliliters, 354.0),
                totalCarbohydrateGrams = scaled(36.0, volumeMilliliters, 354.0),
                sugarGrams = scaled(28.0, volumeMilliliters, 354.0),
                proteinGrams = scaled(10.0, volumeMilliliters, 354.0),
                sodiumMilligrams = scaled(170.0, volumeMilliliters, 354.0),
                potassiumMilligrams = scaled(250.0, volumeMilliliters, 354.0),
                calciumMilligrams = scaled(300.0, volumeMilliliters, 354.0),
            )
            name.contains("cappuccino") -> nutrients(
                energyKcal = scaled(100.0, volumeMilliliters, 354.0),
                totalFatGrams = scaled(4.0, volumeMilliliters, 354.0),
                saturatedFatGrams = scaled(2.5, volumeMilliliters, 354.0),
                totalCarbohydrateGrams = scaled(10.0, volumeMilliliters, 354.0),
                sugarGrams = scaled(9.0, volumeMilliliters, 354.0),
                proteinGrams = scaled(7.0, volumeMilliliters, 354.0),
                sodiumMilligrams = scaled(100.0, volumeMilliliters, 354.0),
                potassiumMilligrams = scaled(300.0, volumeMilliliters, 354.0),
                calciumMilligrams = scaled(240.0, volumeMilliliters, 354.0),
            )
            name.containsAny("latte", "flat white", "macchiato", "cafe au lait", "caffe au lait") -> nutrients(
                energyKcal = scaled(150.0, volumeMilliliters, 354.0),
                totalFatGrams = scaled(6.0, volumeMilliliters, 354.0),
                saturatedFatGrams = scaled(3.5, volumeMilliliters, 354.0),
                totalCarbohydrateGrams = scaled(15.0, volumeMilliliters, 354.0),
                sugarGrams = scaled(13.0, volumeMilliliters, 354.0),
                proteinGrams = scaled(10.0, volumeMilliliters, 354.0),
                sodiumMilligrams = scaled(125.0, volumeMilliliters, 354.0),
                potassiumMilligrams = scaled(400.0, volumeMilliliters, 354.0),
                calciumMilligrams = scaled(300.0, volumeMilliliters, 354.0),
            )
            name.containsAny("espresso", "americano", "nespresso", "turkish") -> nutrients(
                energyKcal = scaled(5.0, volumeMilliliters, 60.0),
                totalCarbohydrateGrams = scaled(1.0, volumeMilliliters, 60.0),
                sodiumMilligrams = scaled(8.0, volumeMilliliters, 60.0),
                potassiumMilligrams = scaled(69.0, volumeMilliliters, 60.0),
                calciumMilligrams = scaled(1.0, volumeMilliliters, 60.0),
            )
            else -> nutrients(
                energyKcal = scaled(2.0, volumeMilliliters, 240.0),
                totalFatGrams = scaled(0.05, volumeMilliliters, 240.0),
                proteinGrams = scaled(0.3, volumeMilliliters, 240.0),
                sodiumMilligrams = scaled(5.0, volumeMilliliters, 240.0),
                potassiumMilligrams = scaled(116.0, volumeMilliliters, 240.0),
                calciumMilligrams = scaled(5.0, volumeMilliliters, 240.0),
            )
        }

    private fun teaDefaults(name: String, volumeMilliliters: Double): Map<NutritionNutrient, Double> =
        when {
            name.containsAny("guayak", "yerba") -> nutrients(
                energyKcal = scaled(120.0, volumeMilliliters, 458.0),
                totalCarbohydrateGrams = scaled(31.0, volumeMilliliters, 458.0),
                sugarGrams = scaled(28.0, volumeMilliliters, 458.0),
                proteinGrams = scaled(1.0, volumeMilliliters, 458.0),
                sodiumMilligrams = scaled(15.0, volumeMilliliters, 458.0),
            )
            name.containsAny("iced", "lipton", "fuze", "nestea") -> nutrients(
                energyKcal = scaled(100.0, volumeMilliliters, 500.0),
                totalCarbohydrateGrams = scaled(25.0, volumeMilliliters, 500.0),
                sugarGrams = scaled(25.0, volumeMilliliters, 500.0),
                sodiumMilligrams = scaled(90.0, volumeMilliliters, 500.0),
            )
            name.containsAny("green", "jasmine", "matcha") -> nutrients(
                energyKcal = scaled(2.0, volumeMilliliters, 240.0),
                totalCarbohydrateGrams = scaled(0.5, volumeMilliliters, 240.0),
                sodiumMilligrams = scaled(5.0, volumeMilliliters, 240.0),
                potassiumMilligrams = scaled(70.0, volumeMilliliters, 240.0),
                calciumMilligrams = scaled(2.0, volumeMilliliters, 240.0),
            )
            name.contains("herbal") -> nutrients(
                energyKcal = scaled(1.0, volumeMilliliters, 240.0),
                potassiumMilligrams = scaled(20.0, volumeMilliliters, 240.0),
                calciumMilligrams = scaled(2.0, volumeMilliliters, 240.0),
            )
            else -> nutrients(
                energyKcal = scaled(2.0, volumeMilliliters, 240.0),
                totalCarbohydrateGrams = scaled(0.7, volumeMilliliters, 240.0),
                sodiumMilligrams = scaled(7.0, volumeMilliliters, 240.0),
                potassiumMilligrams = scaled(70.0, volumeMilliliters, 240.0),
            )
        }

    private fun sodaDefaults(name: String, volumeMilliliters: Double): Map<NutritionNutrient, Double> =
        if (name.containsAny("diet", "zero", "max", "crystal light")) {
            nutrients(sodiumMilligrams = scaled(35.0, volumeMilliliters, 355.0))
        } else {
            nutrients(
                energyKcal = scaled(150.0, volumeMilliliters, 355.0),
                totalCarbohydrateGrams = scaled(39.0, volumeMilliliters, 355.0),
                sugarGrams = scaled(39.0, volumeMilliliters, 355.0),
                sodiumMilligrams = scaled(85.0, volumeMilliliters, 355.0),
                potassiumMilligrams = scaled(10.0, volumeMilliliters, 355.0),
                calciumMilligrams = scaled(5.0, volumeMilliliters, 355.0),
            )
        }

    private fun energyDrinkDefaults(name: String, volumeMilliliters: Double): Map<NutritionNutrient, Double> =
        when {
            name.containsAny("5 hour", "eternal energy") -> nutrients(
                energyKcal = 4.0,
                sodiumMilligrams = 15.0,
            )
            name.containsAny("v8", "juice", "rehab", "kickstart") -> nutrients(
                energyKcal = scaled(50.0, volumeMilliliters, 237.0),
                totalCarbohydrateGrams = scaled(12.0, volumeMilliliters, 237.0),
                sugarGrams = scaled(10.0, volumeMilliliters, 237.0),
                sodiumMilligrams = scaled(60.0, volumeMilliliters, 237.0),
            )
            name.containsAny(
                "zero",
                "sugarfree",
                "sugar free",
                "diet",
                "ultra",
                "bang",
                "reign",
                "c4",
                "celsius",
                "nocco",
                "ghost",
                "gorilla",
                "jocko",
                "gorgie",
                "true north",
            ) -> nutrients(sodiumMilligrams = scaled(60.0, volumeMilliliters, 473.0))
            else -> nutrients(
                energyKcal = scaled(110.0, volumeMilliliters, 250.0),
                totalCarbohydrateGrams = scaled(27.0, volumeMilliliters, 250.0),
                sugarGrams = scaled(27.0, volumeMilliliters, 250.0),
                sodiumMilligrams = scaled(105.0, volumeMilliliters, 250.0),
            )
        }

    private fun chocolateDefaults(name: String, volumeMilliliters: Double): Map<NutritionNutrient, Double> =
        if (name.contains("dunkin")) {
            nutrients(
                energyKcal = scaled(330.0, volumeMilliliters, 414.0),
                totalFatGrams = scaled(11.0, volumeMilliliters, 414.0),
                saturatedFatGrams = scaled(9.0, volumeMilliliters, 414.0),
                totalCarbohydrateGrams = scaled(59.0, volumeMilliliters, 414.0),
                sugarGrams = scaled(46.0, volumeMilliliters, 414.0),
                proteinGrams = scaled(3.0, volumeMilliliters, 414.0),
                sodiumMilligrams = scaled(320.0, volumeMilliliters, 414.0),
                potassiumMilligrams = scaled(250.0, volumeMilliliters, 414.0),
                calciumMilligrams = scaled(40.0, volumeMilliliters, 414.0),
            )
        } else {
            nutrients(
                energyKcal = scaled(90.0, volumeMilliliters, 240.0),
                totalFatGrams = scaled(2.0, volumeMilliliters, 240.0),
                saturatedFatGrams = scaled(1.5, volumeMilliliters, 240.0),
                totalCarbohydrateGrams = scaled(16.0, volumeMilliliters, 240.0),
                sugarGrams = scaled(14.0, volumeMilliliters, 240.0),
                dietaryFiberGrams = scaled(1.0, volumeMilliliters, 240.0),
                proteinGrams = scaled(1.0, volumeMilliliters, 240.0),
                sodiumMilligrams = scaled(170.0, volumeMilliliters, 240.0),
                calciumMilligrams = scaled(40.0, volumeMilliliters, 240.0),
            )
        }

    private fun Map<NutritionNutrient, Double>.withCaffeine(caffeineMilligrams: Double): Map<NutritionNutrient, Double> =
        if (caffeineMilligrams > 0.0 && caffeineMilligrams.isFinite()) {
            this + (NutritionNutrient.CAFFEINE to caffeineMilligrams / MilligramsPerGram)
        } else {
            this
        }

    private fun nutrients(
        energyKcal: Double? = null,
        totalFatGrams: Double? = null,
        saturatedFatGrams: Double? = null,
        totalCarbohydrateGrams: Double? = null,
        sugarGrams: Double? = null,
        dietaryFiberGrams: Double? = null,
        proteinGrams: Double? = null,
        sodiumMilligrams: Double? = null,
        potassiumMilligrams: Double? = null,
        calciumMilligrams: Double? = null,
    ): Map<NutritionNutrient, Double> = buildMap {
        putPositive(NutritionNutrient.ENERGY, energyKcal)
        putPositive(NutritionNutrient.TOTAL_FAT, totalFatGrams)
        putPositive(NutritionNutrient.SATURATED_FAT, saturatedFatGrams)
        putPositive(NutritionNutrient.TOTAL_CARBOHYDRATE, totalCarbohydrateGrams)
        putPositive(NutritionNutrient.SUGAR, sugarGrams)
        putPositive(NutritionNutrient.DIETARY_FIBER, dietaryFiberGrams)
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

    private fun scaled(value: Double, volumeMilliliters: Double, sourceMilliliters: Double): Double =
        value * volumeMilliliters / sourceMilliliters

    private fun String.containsAny(vararg needles: String): Boolean =
        needles.any(::contains)

    private const val MilligramsPerGram = 1000.0
}
