package tech.mmarca.openvitals.data.local.food

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import tech.mmarca.openvitals.domain.model.CustomFood
import tech.mmarca.openvitals.domain.model.FoodCategory
import tech.mmarca.openvitals.domain.model.NutritionNutrient

/** A food the user made. Its nutrients sit in [FoodNutrientEntity], one row each. */
@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String?,
    @ColumnInfo(name = "amount_grams") val amountGrams: Double,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
) {
    companion object {
        fun fromDomain(food: CustomFood, sortOrder: Int): FoodEntity =
            FoodEntity(
                id = food.id,
                name = food.name,
                category = food.category?.name,
                amountGrams = food.amountGrams,
                sortOrder = sortOrder,
            )
    }
}

/** One nutrient of a food, for the food's amount. Energy in kcal, the rest in grams. */
@Entity(tableName = "food_nutrients", primaryKeys = ["food_id", "nutrient"])
data class FoodNutrientEntity(
    @ColumnInfo(name = "food_id") val foodId: String,
    val nutrient: String,
    val value: Double,
) {
    companion object {
        fun fromDomain(food: CustomFood): List<FoodNutrientEntity> =
            food.nutrientValues.map { (nutrient, value) ->
                FoodNutrientEntity(foodId = food.id, nutrient = nutrient.name, value = value)
            }
    }
}

data class FoodWithNutrients(
    @Embedded val food: FoodEntity,
    @Relation(parentColumn = "id", entityColumn = "food_id")
    val nutrients: List<FoodNutrientEntity>,
) {
    /** Unknown nutrient names and non-positive values are dropped, not surfaced. */
    fun toDomain(): CustomFood =
        CustomFood(
            id = food.id,
            name = food.name,
            amountGrams = food.amountGrams,
            nutrientValues = nutrients.mapNotNull { row ->
                val nutrient = runCatching { NutritionNutrient.valueOf(row.nutrient) }.getOrNull()
                    ?: return@mapNotNull null
                row.value.takeIf { it > 0.0 && it.isFinite() }?.let { nutrient to it }
            }.toMap(),
            category = food.category?.let { runCatching { FoodCategory.valueOf(it) }.getOrNull() },
        )
}
