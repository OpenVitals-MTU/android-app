package tech.mmarca.openvitals.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.local.food.FoodDao
import tech.mmarca.openvitals.data.local.food.FoodEntity
import tech.mmarca.openvitals.data.local.food.FoodNutrientEntity
import tech.mmarca.openvitals.data.local.food.FoodWithNutrients
import tech.mmarca.openvitals.domain.model.CustomFood
import tech.mmarca.openvitals.domain.model.FoodCategory
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.util.MainDispatcherRule

/**
 * In-memory stand-in for [FoodDao]. There is no SQLite in the unit suite, so each
 * `@Query`'s SQL is reimplemented over maps. The `@Transaction` default method runs for real.
 */
private class FakeFoodDao : FoodDao {
    val foods = linkedMapOf<String, FoodEntity>()
    val nutrients = mutableListOf<FoodNutrientEntity>()

    override suspend fun activeFoods(): List<FoodWithNutrients> =
        foods.values
            .filterNot { it.isDeleted }
            .sortedWith(compareBy({ it.sortOrder }, { it.name.lowercase() }))
            .map { food -> FoodWithNutrients(food, nutrients.filter { it.foodId == food.id }) }

    override suspend fun foodById(id: String): FoodEntity? = foods[id]

    override suspend fun nextSortOrder(): Int =
        (foods.values.maxOfOrNull { it.sortOrder } ?: -1) + 1

    override suspend fun upsertFood(food: FoodEntity) {
        foods[food.id] = food
    }

    override suspend fun deleteNutrients(foodId: String) {
        nutrients.removeAll { it.foodId == foodId }
    }

    override suspend fun insertNutrients(nutrients: List<FoodNutrientEntity>) {
        nutrients.forEach { row ->
            this.nutrients.removeAll { it.foodId == row.foodId && it.nutrient == row.nutrient }
            this.nutrients += row
        }
    }

    override suspend fun softDelete(id: String) {
        foods[id]?.let { foods[id] = it.copy(isDeleted = true) }
    }
}

class FoodRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = FakeFoodDao()
    private val repository = FoodRepositoryImpl(dao, mainDispatcherRule.dispatcherProvider)

    private fun banana(id: String = "banana") = CustomFood(
        id = id,
        name = "Banana",
        amountGrams = 120.0,
        nutrientValues = mapOf(
            NutritionNutrient.ENERGY to 105.0,
            NutritionNutrient.TOTAL_CARBOHYDRATE to 27.0,
        ),
        category = FoodCategory.FRUIT,
    )

    @Test
    fun `the catalog starts empty, there are no preloaded foods`() = runTest {
        assertTrue(repository.customFoods().isEmpty())
    }

    @Test
    fun `save stores the food and its nutrients, in creation order`() = runTest {
        repository.saveCustomFood(banana())
        repository.saveCustomFood(banana(id = "apple").copy(name = "Apple"))

        val foods = repository.customFoods()

        assertEquals(listOf("banana", "apple"), foods.map { it.id })
        assertEquals(banana(), foods.first())
        assertEquals(0, dao.foods.getValue("banana").sortOrder)
        assertEquals(1, dao.foods.getValue("apple").sortOrder)
    }

    @Test
    fun `saving an edit replaces the nutrients and keeps the place in the list`() = runTest {
        repository.saveCustomFood(banana())
        repository.saveCustomFood(banana(id = "apple").copy(name = "Apple"))

        repository.saveCustomFood(
            banana().copy(nutrientValues = mapOf(NutritionNutrient.ENERGY to 90.0)),
        )

        val edited = repository.customFoods().first { it.id == "banana" }
        assertEquals(mapOf(NutritionNutrient.ENERGY to 90.0), edited.nutrientValues)
        assertEquals(listOf("banana", "apple"), repository.customFoods().map { it.id })
        // The old carbohydrate row is gone, not orphaned.
        assertTrue(dao.nutrients.none { it.foodId == "banana" && it.nutrient == "TOTAL_CARBOHYDRATE" })
    }

    @Test
    fun `delete hides the food and keeps the row`() = runTest {
        repository.saveCustomFood(banana())

        repository.deleteCustomFood("banana")

        assertNull(repository.customFoods().firstOrNull { it.id == "banana" })
        assertTrue(dao.foods.getValue("banana").isDeleted)
    }

    @Test
    fun `saving a deleted id brings the food back`() = runTest {
        repository.saveCustomFood(banana())
        repository.deleteCustomFood("banana")

        repository.saveCustomFood(banana())

        assertEquals(listOf("banana"), repository.customFoods().map { it.id })
    }

    @Test
    fun `an unknown nutrient name or category in the table is dropped, not surfaced`() = runTest {
        dao.foods["x"] = FoodEntity(id = "x", name = "X", category = "NOT_A_CATEGORY", amountGrams = 10.0, sortOrder = 0)
        dao.nutrients += FoodNutrientEntity("x", "NOT_A_NUTRIENT", 1.0)
        dao.nutrients += FoodNutrientEntity("x", "ENERGY", 5.0)

        val food = repository.customFoods().single()

        assertNull(food.category)
        assertEquals(mapOf(NutritionNutrient.ENERGY to 5.0), food.nutrientValues)
    }
}
