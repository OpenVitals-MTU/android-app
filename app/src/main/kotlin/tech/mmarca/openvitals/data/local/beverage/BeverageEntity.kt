package tech.mmarca.openvitals.data.local.beverage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import tech.mmarca.openvitals.domain.insights.BeverageNutritionDefaults
import tech.mmarca.openvitals.domain.insights.CaffeineHealthDrinkCatalog
import tech.mmarca.openvitals.domain.model.CaffeineCatalogItem
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.model.NutritionNutrient

@Entity(tableName = "beverages")
data class BeverageEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String?,
    @ColumnInfo(name = "volume_milliliters") val volumeMilliliters: Double,
    @ColumnInfo(name = "hydration_multiplier") val hydrationMultiplier: Double,
    @ColumnInfo(name = "is_preloaded") val isPreloaded: Boolean,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "energy_kcal") val energyKcal: Double? = null,
    @ColumnInfo(name = "protein_grams") val proteinGrams: Double? = null,
    @ColumnInfo(name = "total_carbohydrate_grams") val totalCarbohydrateGrams: Double? = null,
    @ColumnInfo(name = "total_fat_grams") val totalFatGrams: Double? = null,
    @ColumnInfo(name = "dietary_fiber_grams") val dietaryFiberGrams: Double? = null,
    @ColumnInfo(name = "sugar_grams") val sugarGrams: Double? = null,
    @ColumnInfo(name = "saturated_fat_grams") val saturatedFatGrams: Double? = null,
    @ColumnInfo(name = "sodium_grams") val sodiumGrams: Double? = null,
    @ColumnInfo(name = "potassium_grams") val potassiumGrams: Double? = null,
    @ColumnInfo(name = "calcium_grams") val calciumGrams: Double? = null,
    @ColumnInfo(name = "caffeine_grams") val caffeineGrams: Double? = null,
) {
    fun toDomain(): CustomHydrationDrink =
        CustomHydrationDrink(
            id = id,
            name = name,
            volumeMilliliters = volumeMilliliters,
            hydrationMultiplier = hydrationMultiplier,
            nutrientValues = nutrientValues(),
            category = category?.let { runCatching { CaffeineSourceCategory.valueOf(it) }.getOrNull() },
            isPreloaded = isPreloaded,
        )

    private fun nutrientValues(): Map<NutritionNutrient, Double> = buildMap {
        putPositive(NutritionNutrient.ENERGY, energyKcal)
        putPositive(NutritionNutrient.PROTEIN, proteinGrams)
        putPositive(NutritionNutrient.TOTAL_CARBOHYDRATE, totalCarbohydrateGrams)
        putPositive(NutritionNutrient.TOTAL_FAT, totalFatGrams)
        putPositive(NutritionNutrient.DIETARY_FIBER, dietaryFiberGrams)
        putPositive(NutritionNutrient.SUGAR, sugarGrams)
        putPositive(NutritionNutrient.SATURATED_FAT, saturatedFatGrams)
        putPositive(NutritionNutrient.SODIUM, sodiumGrams)
        putPositive(NutritionNutrient.POTASSIUM, potassiumGrams)
        putPositive(NutritionNutrient.CALCIUM, calciumGrams)
        putPositive(NutritionNutrient.CAFFEINE, caffeineGrams)
    }

    private fun MutableMap<NutritionNutrient, Double>.putPositive(
        nutrient: NutritionNutrient,
        value: Double?,
    ) {
        if (value != null && value > 0.0 && value.isFinite()) {
            put(nutrient, value)
        }
    }

    companion object {
        fun preloadedDefaults(): List<BeverageEntity> =
            listOf(waterDefault()) + CaffeineHealthDrinkCatalog.items
                .asSequence()
                .filter { item -> item.defaultServingMilliliters != null }
                .filterNot { item -> item.category == CaffeineSourceCategory.SUPPLEMENT }
                .mapIndexed { index, item -> item.toPreloadedEntity(index + 1) }
                .toList()

        private fun waterDefault(): BeverageEntity =
            fromDomain(
                drink = CustomHydrationDrink(
                    id = "openvitals-water",
                    name = "Water",
                    volumeMilliliters = 100.0,
                    hydrationMultiplier = 1.0,
                    nutrientValues = emptyMap(),
                    category = null,
                    isPreloaded = true,
                ),
                sortOrder = 0,
                isPreloaded = true,
                category = null,
            )

        fun fromDomain(
            drink: CustomHydrationDrink,
            sortOrder: Int,
            isPreloaded: Boolean = drink.isPreloaded,
            category: CaffeineSourceCategory? = drink.category,
        ): BeverageEntity =
            BeverageEntity(
                id = drink.id,
                name = drink.name,
                category = category?.name,
                volumeMilliliters = drink.volumeMilliliters,
                hydrationMultiplier = drink.hydrationMultiplier,
                isPreloaded = isPreloaded,
                sortOrder = sortOrder,
                energyKcal = drink.nutrientValues[NutritionNutrient.ENERGY],
                proteinGrams = drink.nutrientValues[NutritionNutrient.PROTEIN],
                totalCarbohydrateGrams = drink.nutrientValues[NutritionNutrient.TOTAL_CARBOHYDRATE],
                totalFatGrams = drink.nutrientValues[NutritionNutrient.TOTAL_FAT],
                dietaryFiberGrams = drink.nutrientValues[NutritionNutrient.DIETARY_FIBER],
                sugarGrams = drink.nutrientValues[NutritionNutrient.SUGAR],
                saturatedFatGrams = drink.nutrientValues[NutritionNutrient.SATURATED_FAT],
                sodiumGrams = drink.nutrientValues[NutritionNutrient.SODIUM],
                potassiumGrams = drink.nutrientValues[NutritionNutrient.POTASSIUM],
                calciumGrams = drink.nutrientValues[NutritionNutrient.CALCIUM],
                caffeineGrams = drink.nutrientValues[NutritionNutrient.CAFFEINE],
            )

        private fun CaffeineCatalogItem.toPreloadedEntity(sortOrder: Int): BeverageEntity =
            fromDomain(
                drink = CustomHydrationDrink(
                    id = "$BeveragePresetIdPrefix$id",
                    name = name,
                    volumeMilliliters = defaultServingMilliliters ?: 240.0,
                    hydrationMultiplier = 1.0,
                    nutrientValues = BeverageNutritionDefaults.nutrientValuesFor(this),
                    category = category,
                    isPreloaded = true,
                ),
                sortOrder = sortOrder,
                isPreloaded = true,
                category = category,
            )

        private const val BeveragePresetIdPrefix = "caffeinehealth-"
    }
}
