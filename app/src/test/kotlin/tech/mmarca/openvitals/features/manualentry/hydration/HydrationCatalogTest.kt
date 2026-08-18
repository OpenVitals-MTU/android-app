package tech.mmarca.openvitals.features.manualentry.hydration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BeverageCategory
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink

class HydrationCatalogTest {

    @Test
    fun `uncategorized saved drinks land in the unassigned group`() {
        val grouped = group(savedDrinks = listOf(drink("a"), drink("b")))

        assertEquals(listOf("a", "b"), rowIds(grouped.unassignedSavedRows))
        assertFalse(grouped.isEmpty())
    }

    @Test
    fun `a drink is filed under its category section`() {
        val grouped = group(
            savedDrinks = listOf(
                drink("c", category = BeverageCategory.COFFEE),
                drink("s", category = BeverageCategory.SODA),
            ),
        )

        assertEquals(listOf("c"), rowIds(grouped.section(HydrationCatalogSectionKey.COFFEE)))
        assertEquals(
            listOf("s"),
            rowIds(grouped.section(HydrationCatalogSectionKey.CARBONATED_SOFT_DRINK)),
        )
        assertTrue(grouped.unassignedSavedRows.isEmpty())
    }

    @Test
    fun `supplement collapses into the other section`() {
        val grouped = group(
            savedDrinks = listOf(drink("x", category = BeverageCategory.SUPPLEMENT)),
        )

        assertEquals(listOf("x"), rowIds(grouped.section(HydrationCatalogSectionKey.OTHER)))
        // …but dropping a drink into "other" writes back the `OTHER` category.
        assertEquals(
            BeverageCategory.OTHER,
            sectionCategory(HydrationCatalogSectionKey.OTHER),
        )
    }

    @Test
    fun `a session category override beats the drink's persisted category`() {
        val grouped = group(
            savedDrinks = listOf(drink("c", category = BeverageCategory.COFFEE)),
            savedDrinkCategories = mapOf("c" to HydrationCatalogSectionKey.TEA),
        )

        assertTrue(rowIds(grouped.section(HydrationCatalogSectionKey.COFFEE)).isEmpty())
        assertEquals(listOf("c"), rowIds(grouped.section(HydrationCatalogSectionKey.TEA)))
    }

    @Test
    fun `a frequent drink is not repeated in its section`() {
        val coffee = drink("c", category = BeverageCategory.COFFEE)
        val grouped = group(
            savedDrinks = listOf(coffee, drink("t", category = BeverageCategory.TEA)),
            frequentDrinks = listOf(coffee),
        )

        assertEquals(listOf("c"), rowIds(grouped.frequentRows))
        assertTrue(rowIds(grouped.section(HydrationCatalogSectionKey.COFFEE)).isEmpty())
        assertEquals(listOf("t"), rowIds(grouped.section(HydrationCatalogSectionKey.TEA)))
    }

    @Test
    fun `a frequent drink that is no longer saved is dropped`() {
        val grouped = group(
            savedDrinks = listOf(drink("a")),
            frequentDrinks = listOf(drink("gone")),
        )

        assertTrue(grouped.frequentRows.isEmpty())
    }

    @Test
    fun `the search query filters by name, case-insensitively`() {
        val grouped = group(
            savedDrinks = listOf(drink("a", name = "Cold Brew"), drink("b", name = "Tea")),
            normalizedQuery = "brew",
        )

        assertEquals(listOf("a"), rowIds(grouped.unassignedSavedRows))
    }

    @Test
    fun `a query matching nothing leaves the grouping empty`() {
        val grouped = group(
            savedDrinks = listOf(drink("a", name = "Cola")),
            normalizedQuery = "zzz",
        )

        assertTrue(grouped.isEmpty())
    }

    @Test
    fun `a session row order reorders a section, unknown keys keep their place`() {
        val grouped = group(
            savedDrinks = listOf(drink("a"), drink("b"), drink("c")),
            unassignedSavedOrder = listOf("c".toSavedCatalogRowKey(), "a".toSavedCatalogRowKey()),
        )

        // Ordered rows first, then whatever the order did not mention.
        assertEquals(listOf("c", "a", "b"), rowIds(grouped.unassignedSavedRows))
    }

    @Test
    fun `row keys round-trip a saved drink id`() {
        val key = "abc".toSavedCatalogRowKey()

        assertEquals("abc", key.savedDrinkIdOrNull())
        assertEquals("abc", key.catalogDrinkIdOrNull())
    }

    @Test
    fun `row keys a preset key is not a saved key but still yields its id`() {
        val key = "abc".toPresetCatalogRowKey()

        assertNull(key.savedDrinkIdOrNull())
        assertEquals("abc", key.catalogDrinkIdOrNull())
    }

    @Test
    fun `row keys an unprefixed key yields nothing`() {
        assertNull("abc".catalogDrinkIdOrNull())
    }

    @Test
    fun `every category maps to a section and back`() {
        BeverageCategory.entries.forEach { category ->
            val key = category.toHydrationCatalogSectionKey()
            // SUPPLEMENT is the one lossy case, collapsing into `OTHER`.
            if (category == BeverageCategory.SUPPLEMENT) {
                assertEquals(BeverageCategory.OTHER, sectionCategory(key))
            } else {
                assertEquals(category, sectionCategory(key))
            }
        }
    }

    private fun drink(
        id: String,
        name: String? = null,
        category: BeverageCategory? = null,
    ): CustomHydrationDrink =
        CustomHydrationDrink(
            id = id,
            name = name ?: id,
            volumeMilliliters = 250.0,
            category = category,
        )

    private fun group(
        savedDrinks: List<CustomHydrationDrink>,
        frequentDrinks: List<CustomHydrationDrink> = emptyList(),
        catalogDrinks: List<CustomHydrationDrink> = emptyList(),
        savedDrinkCategories: Map<String, HydrationCatalogSectionKey> = emptyMap(),
        unassignedSavedOrder: List<String> = emptyList(),
        sectionOrders: Map<HydrationCatalogSectionKey, List<String>> = emptyMap(),
        normalizedQuery: String = "",
    ): HydrationCatalogGroupedDrinks =
        hydrationCatalogGroupedDrinks(
            catalogDrinks = catalogDrinks,
            savedDrinks = savedDrinks,
            frequentDrinks = frequentDrinks,
            savedDrinkCategories = savedDrinkCategories,
            unassignedSavedOrder = unassignedSavedOrder,
            sectionOrders = sectionOrders,
            normalizedQuery = normalizedQuery,
        )

    private fun rowIds(rows: List<HydrationCatalogRowItem>): List<String> =
        rows.map { row -> row.drink.id }

    private fun HydrationCatalogGroupedDrinks.section(
        key: HydrationCatalogSectionKey,
    ): List<HydrationCatalogRowItem> = sections.first { it.spec.key == key }.rows

    private fun HydrationCatalogGroupedDrinks.isEmpty(): Boolean =
        frequentRows.isEmpty() &&
            unassignedSavedRows.isEmpty() &&
            sections.all { it.rows.isEmpty() }

    private fun sectionCategory(key: HydrationCatalogSectionKey): BeverageCategory? =
        HydrationCatalogSections.first { it.key == key }.category
}
