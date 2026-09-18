package tech.mmarca.openvitals.data.local.beverage

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.model.BeverageCategory
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink

@Singleton
class BeverageStore @Inject constructor(
    private val dao: BeverageDao,
    private val preferencesRepository: PreferencesRepository,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) {
    @Volatile
    private var initialized = false
    private val initMutex = Mutex()

    suspend fun beverages(): List<CustomHydrationDrink> = withDatabase {
        dao.activeBeverages().map(BeverageEntity::toDomain)
    }

    suspend fun save(drink: CustomHydrationDrink) = withDatabase {
        val existing = dao.beverageById(drink.id)
        val entity = BeverageEntity.fromDomain(
            drink = drink,
            sortOrder = existing?.sortOrder ?: dao.nextSortOrder(),
            isPreloaded = existing?.isPreloaded ?: drink.isPreloaded,
            category = drink.category ?: existing?.toDomain()?.category,
        )
        dao.upsert(entity.copy(isDeleted = false))
    }

    suspend fun delete(drinkId: String) = withDatabase {
        dao.softDelete(drinkId)
    }

    suspend fun moveToCategory(drinkId: String, category: BeverageCategory?) = withDatabase {
        dao.updateCategory(drinkId, category?.name)
    }

    suspend fun reorder(drinkIds: List<String>) = withDatabase {
        val current = dao.activeBeverages()
        val currentIds = current.map { it.id }.toSet()
        val orderedIds = drinkIds
            .filter { it in currentIds }
            .distinct()
        val orderedIdSet = orderedIds.toSet()
        dao.updateSortOrder(orderedIds + current.map { it.id }.filterNot { it in orderedIdSet })
    }

    // Runs on the IO context. The mutex makes concurrent first calls seed once.
    private suspend fun ensureInitialized() {
        if (initialized) return
        initMutex.withLock {
            if (initialized) return
            dao.insertDefaults(BeverageEntity.preloadedDefaults())
            if (!preferencesRepository.hasMigratedHydrationBeveragesToRoom()) {
                val nextSortOrder = dao.nextSortOrder()
                preferencesRepository.customHydrationDrinks().forEachIndexed { index, drink ->
                    dao.upsert(
                        BeverageEntity.fromDomain(
                            drink = drink,
                            sortOrder = nextSortOrder + index,
                            isPreloaded = false,
                            category = drink.category,
                        )
                    )
                }
                preferencesRepository.setMigratedHydrationBeveragesToRoom()
            }
            initialized = true
        }
    }

    private suspend fun <T> withDatabase(block: suspend () -> T): T =
        withContext(dispatchers.io) {
            ensureInitialized()
            block()
        }
}
