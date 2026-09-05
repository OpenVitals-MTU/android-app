package tech.mmarca.openvitals.features.manualentry.hydration

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.local.beverage.BeverageDao
import tech.mmarca.openvitals.data.local.beverage.BeverageEntity
import tech.mmarca.openvitals.data.local.beverage.BeverageStore
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.domain.insights.CaffeineHealthDrinkCatalog
import tech.mmarca.openvitals.domain.model.BeverageCategory
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.util.MainDispatcherRule

/** In-memory stand-in for [BeverageDao]; each `@Query`'s SQL is reimplemented over a map. */
private class InMemoryBeverageDao : BeverageDao {
    private val rows = linkedMapOf<String, BeverageEntity>()

    override suspend fun activeBeverages(): List<BeverageEntity> =
        rows.values
            .filterNot { it.isDeleted }
            .sortedWith(compareBy({ it.sortOrder }, { it.name.lowercase() }))

    override suspend fun beverageById(id: String): BeverageEntity? = rows[id]

    override suspend fun nextSortOrder(): Int =
        (rows.values.maxOfOrNull { it.sortOrder } ?: -1) + 1

    override suspend fun insertDefaults(beverages: List<BeverageEntity>) {
        beverages.forEach { entity -> rows.putIfAbsent(entity.id, entity) }
    }

    override suspend fun upsert(beverage: BeverageEntity) {
        rows[beverage.id] = beverage
    }

    override suspend fun softDelete(id: String) {
        rows[id]?.let { rows[id] = it.copy(isDeleted = true) }
    }

    override suspend fun updateCategory(id: String, category: String?) {
        rows[id]?.let { rows[id] = it.copy(category = category) }
    }

    override suspend fun updateSortOrder(id: String, sortOrder: Int) {
        rows[id]?.let { rows[id] = it.copy(sortOrder = sortOrder) }
    }

    override suspend fun deleteAll() {
        rows.clear()
    }

    override suspend fun insertAll(beverages: List<BeverageEntity>) {
        beverages.forEach { rows[it.id] = it }
    }
}

/** Exercises the real [BeverageStore]: the seeded drinks only exist because it seeds them on first read. */
@OptIn(ExperimentalCoroutinesApi::class)
class HydrationSeededCatalogTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var store: BeverageStore

    @Before fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        val prefs = FakeSharedPreferences()
        val context = mockk<Context> {
            every {
                getSharedPreferences(PreferencesRepository.PREFS_FILE, Context.MODE_PRIVATE)
            } returns prefs as SharedPreferences
        }
        store = BeverageStore(InMemoryBeverageDao(), PreferencesRepository(context))
    }

    @After fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test fun `supplements and servingless items are excluded from the seed`() {
        val ids = store.beverages().map { it.id }.toSet()

        CaffeineHealthDrinkCatalog.items.forEach { item ->
            val seeded = ids.contains("caffeinehealth-${item.id}")
            val eligible = item.defaultServingMilliliters != null &&
                item.category != CaffeineSourceCategory.SUPPLEMENT
            assertEquals("${item.id} seeded=$seeded", eligible, seeded)
        }
    }

    @Test fun `a user drink is saved to the store and read back with the seed`() {
        store.save(
            CustomHydrationDrink(
                id = "mine",
                name = "My smoothie",
                volumeMilliliters = 400.0,
            ),
        )

        val drinks = store.beverages()
        val mine = drinks.single { it.id == "mine" }
        assertEquals("My smoothie", mine.name)
        assertFalse(mine.isPreloaded)
        // The seed is still there alongside it.
        assertTrue(drinks.any { it.name == "Drip coffee" })
    }

    @Test fun `deleting and recategorizing round-trip through the store`() {
        store.save(
            CustomHydrationDrink(
                id = "mine",
                name = "My smoothie",
                volumeMilliliters = 400.0,
            ),
        )

        store.moveToCategory("mine", BeverageCategory.OTHER)
        assertEquals(
            BeverageCategory.OTHER,
            store.beverages().single { it.id == "mine" }.category,
        )

        store.delete("mine")
        assertFalse(store.beverages().any { it.id == "mine" })
    }

    @Test fun `the entry view model surfaces the seeded catalog`() = runTest {
        val repository = mockk<HydrationRepository>(relaxed = true)
        every { repository.hydrationWritePermissions } returns setOf("write_hydration")
        every { repository.hydrationContainerVolumeMilliliters() } returns emptyMap()
        every { repository.lastCustomHydrationAmountMilliliters() } returns null
        every { repository.recentHydrationAmountsMilliliters() } returns emptyList()
        every { repository.hydrationDailyGoalLiters() } returns 2.0
        // The one call under test: the catalog comes off the real seeded store.
        every { repository.customHydrationDrinks() } answers { store.beverages() }

        val viewModel = HydrationEntryViewModel(repository)
        advanceUntilIdle()

        val options = viewModel.uiState.value.customDrinkOptions
        // The catalog must reach the screen.
        assertTrue(options.isNotEmpty())
        assertTrue(options.any { it.name == "Drip coffee" })
    }
}
