package tech.mmarca.openvitals.data.local.beverage

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink

@Singleton
class BeverageStore @Inject constructor(
    private val dao: BeverageDao,
    private val preferencesRepository: PreferencesRepository,
) {
    @Volatile
    private var initialized = false

    fun beverages(): List<CustomHydrationDrink> = withDatabase {
        dao.activeBeverages().map(BeverageEntity::toDomain)
    }

    fun save(drink: CustomHydrationDrink) = withDatabase {
        val existing = dao.beverageById(drink.id)
        val entity = BeverageEntity.fromDomain(
            drink = drink,
            sortOrder = existing?.sortOrder ?: dao.nextSortOrder(),
            isPreloaded = existing?.isPreloaded ?: drink.isPreloaded,
            category = drink.category ?: existing?.toDomain()?.category,
        )
        dao.upsert(entity.copy(isDeleted = false))
    }

    fun delete(drinkId: String) = withDatabase {
        dao.softDelete(drinkId)
    }

    fun moveToCategory(drinkId: String, category: CaffeineSourceCategory?) = withDatabase {
        dao.updateCategory(drinkId, category?.name)
    }

    fun reorder(drinkIds: List<String>) = withDatabase {
        val current = dao.activeBeverages()
        val currentIds = current.map { it.id }.toSet()
        val orderedIds = drinkIds
            .filter { it in currentIds }
            .distinct()
        val orderedIdSet = orderedIds.toSet()
        dao.updateSortOrder(orderedIds + current.map { it.id }.filterNot { it in orderedIdSet })
    }

    private fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            runBlocking(Dispatchers.IO) {
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
            }
            initialized = true
        }
    }

    private fun <T> withDatabase(block: suspend () -> T): T {
        ensureInitialized()
        return runBlocking(Dispatchers.IO) { block() }
    }
}
