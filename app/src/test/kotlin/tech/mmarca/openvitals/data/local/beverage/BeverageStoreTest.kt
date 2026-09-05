package tech.mmarca.openvitals.data.local.beverage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.domain.model.BeverageCategory
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.model.NutritionNutrient

/**
 * In-memory stand-in for [BeverageDao]. There is no SQLite in the unit suite, so each
 * `@Query`'s SQL is reimplemented over a map. The `@Transaction` default methods run for real.
 */
private class FakeBeverageDao : BeverageDao {
    val rows = linkedMapOf<String, BeverageEntity>()

    override suspend fun activeBeverages(): List<BeverageEntity> =
        rows.values
            .filterNot { it.isDeleted }
            .sortedWith(compareBy({ it.sortOrder }, { it.name.lowercase() }))

    override suspend fun beverageById(id: String): BeverageEntity? = rows[id]

    override suspend fun nextSortOrder(): Int =
        (rows.values.maxOfOrNull { it.sortOrder } ?: -1) + 1

    override suspend fun insertDefaults(beverages: List<BeverageEntity>) {
        // OnConflictStrategy.IGNORE: an existing id keeps its stored row.
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

/** The store over [FakeBeverageDao] and a [FakeSharedPreferences]-backed [PreferencesRepository]. */
class BeverageStoreTest {

    private lateinit var dao: FakeBeverageDao
    private lateinit var prefs: FakeSharedPreferences
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var store: BeverageStore

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        dao = FakeBeverageDao()
        prefs = FakeSharedPreferences()
        val context = mockk<Context> {
            every {
                getSharedPreferences(PreferencesRepository.PREFS_FILE, Context.MODE_PRIVATE)
            } returns prefs as SharedPreferences
        }
        preferencesRepository = PreferencesRepository(context)
        store = BeverageStore(dao, preferencesRepository)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `beverages seeds preloaded defaults on first access`() {
        val beverages = store.beverages()

        assertEquals(BeverageEntity.preloadedDefaults().size, beverages.size)
        assertEquals("openvitals-still-water", beverages[0].id)
        assertEquals("openvitals-gasified-water", beverages[1].id)
        // The migration flag is flipped once the store initializes.
        assertTrue(preferencesRepository.hasMigratedHydrationBeveragesToRoom())
    }

    @Test
    fun `save inserts a new active drink with the next sort order`() {
        store.beverages() // force seeding
        val drink = CustomHydrationDrink(
            id = "my-smoothie",
            name = "Smoothie",
            volumeMilliliters = 350.0,
            hydrationMultiplier = 0.8,
            nutrientValues = mapOf(NutritionNutrient.ENERGY to 180.0),
            category = BeverageCategory.OTHER,
        )

        store.save(drink)
        val beverages = store.beverages()

        val saved = beverages.first { it.id == "my-smoothie" }
        assertEquals("Smoothie", saved.name)
        assertEquals(350.0, saved.volumeMilliliters, 0.001)
        assertEquals(0.8, saved.hydrationMultiplier, 0.001)
        assertEquals(BeverageCategory.OTHER, saved.category)
        assertEquals(180.0, saved.nutrientValues.getValue(NutritionNutrient.ENERGY), 0.001)
        // The next free sort order, i.e. after the whole preloaded catalog.
        assertEquals(BeverageEntity.preloadedDefaults().size, dao.rows.getValue("my-smoothie").sortOrder)
    }

    @Test
    fun `delete soft-deletes and hides the drink from active listing`() {
        store.beverages()
        store.delete("openvitals-still-water")

        val beverages = store.beverages()
        assertNull(beverages.firstOrNull { it.id == "openvitals-still-water" })
        // Soft delete keeps the row, just flips is_deleted.
        val row = dao.rows["openvitals-still-water"]
        assertNotNull(row)
        assertTrue(row!!.isDeleted)
    }

    @Test
    fun `moveToCategory updates the persisted category`() {
        store.beverages()
        store.moveToCategory("openvitals-still-water", BeverageCategory.TEA)

        assertEquals(BeverageCategory.TEA.name, dao.rows.getValue("openvitals-still-water").category)
    }

    @Test
    fun `reorder reindexes provided ids first, keeping the rest after`() {
        store.beverages()
        store.reorder(listOf("openvitals-gasified-water", "openvitals-still-water"))

        val beverages = store.beverages()
        assertEquals("openvitals-gasified-water", beverages[0].id)
        assertEquals("openvitals-still-water", beverages[1].id)
    }
}
