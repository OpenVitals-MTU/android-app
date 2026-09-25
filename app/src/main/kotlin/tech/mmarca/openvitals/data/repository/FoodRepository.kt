package tech.mmarca.openvitals.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.local.food.FoodDao
import tech.mmarca.openvitals.data.local.food.FoodEntity
import tech.mmarca.openvitals.data.local.food.FoodNutrientEntity
import tech.mmarca.openvitals.data.local.food.FoodWithNutrients
import tech.mmarca.openvitals.data.repository.contract.FoodRepository
import tech.mmarca.openvitals.domain.model.CustomFood

/** Room holds the catalog only. There are no preloaded foods; the user makes every one. */
@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val dao: FoodDao,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) : FoodRepository {

    override suspend fun customFoods(): List<CustomFood> = withContext(dispatchers.io) {
        dao.activeFoods().map(FoodWithNutrients::toDomain)
    }

    override suspend fun saveCustomFood(food: CustomFood) = withContext(dispatchers.io) {
        // An edited food keeps its place; a new one goes last. A deleted id saved again comes back.
        val existing = dao.foodById(food.id)
        dao.save(
            food = FoodEntity.fromDomain(food, sortOrder = existing?.sortOrder ?: dao.nextSortOrder()),
            nutrients = FoodNutrientEntity.fromDomain(food),
        )
    }

    override suspend fun deleteCustomFood(foodId: String) = withContext(dispatchers.io) {
        dao.softDelete(foodId)
    }
}
