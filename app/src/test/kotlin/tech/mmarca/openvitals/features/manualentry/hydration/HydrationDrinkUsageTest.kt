package tech.mmarca.openvitals.features.manualentry.hydration

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.NutritionEntry

class HydrationDrinkUsageTest {

    private val t0: Instant = Instant.parse("2026-01-01T08:00:00Z")
    private val t1: Instant = Instant.parse("2026-01-02T08:00:00Z")
    private val t2: Instant = Instant.parse("2026-01-03T08:00:00Z")

    private fun drink(id: String, name: String? = null) = CustomHydrationDrink(
        id = id,
        name = name ?: id,
        volumeMilliliters = 250.0,
    )

    /**
     * A hydration entry as the native writer emits it:
     * `openvitals_hydration_<epochMs>_drink_<id>_<uuid>`.
     */
    private fun hydration(drinkId: String, time: Instant, uuid: String = "u1") = HydrationEntry(
        startTime = time,
        endTime = time,
        liters = 0.25,
        source = "openvitals",
        isOpenVitalsEntry = true,
        clientRecordId = "openvitals_hydration_${time.toEpochMilli()}_drink_${drinkId}_$uuid",
    )

    private fun nutrition(
        time: Instant,
        clientRecordId: String? = null,
        name: String? = null,
        isOpenVitals: Boolean = true,
    ) = NutritionEntry(
        time = time,
        endTime = time,
        mealType = 0,
        name = name,
        energyKcal = null,
        proteinGrams = null,
        carbsGrams = null,
        fatGrams = null,
        fiberGrams = null,
        sugarGrams = null,
        source = "openvitals",
        clientRecordId = clientRecordId,
        isOpenVitalsEntry = isOpenVitals,
    )

    @Test
    fun `hydrationDrinkIdFromClientRecordId extracts the id between the marker and the next underscore`() {
        assertEquals(
            "abc-123",
            "openvitals_hydration_1700000000000_drink_abc-123_uuid".hydrationDrinkIdFromClientRecordId(),
        )
    }

    @Test
    fun `hydrationDrinkIdFromClientRecordId returns null without the prefix marker or a terminator`() {
        assertNull("other_1_drink_a_b".hydrationDrinkIdFromClientRecordId())
        assertNull("openvitals_hydration_1_uuid".hydrationDrinkIdFromClientRecordId())
        // No trailing '_' after the id.
        assertNull("openvitals_hydration_1_drink_abc".hydrationDrinkIdFromClientRecordId())
    }

    @Test
    fun `pairedHydrationClientRecordId unwraps a paired nutrition record id`() {
        assertEquals(
            "openvitals_hydration_1_drink_a_u",
            "openvitals_hydration_nutrition_openvitals_hydration_1_drink_a_u"
                .pairedHydrationClientRecordIdOrNull(),
        )
    }

    @Test
    fun `pairedHydrationClientRecordId returns null for a standalone nutrition record`() {
        assertNull("openvitals_nutrition_1_u".pairedHydrationClientRecordIdOrNull())
    }

    @Test
    fun `frequentHydrationDrinkOptions ranks by log count most frequent first`() {
        val result = frequentHydrationDrinkOptions(
            drinks = listOf(drink("a"), drink("b")),
            hydrationEntries = listOf(
                hydration("a", t0),
                hydration("b", t1, uuid = "u2"),
                hydration("b", t2, uuid = "u3"),
            ),
            nutritionEntries = emptyList(),
        )
        assertEquals(listOf("b", "a"), result.map { it.id })
    }

    @Test
    fun `frequentHydrationDrinkOptions breaks a count tie on the most recent log`() {
        val result = frequentHydrationDrinkOptions(
            drinks = listOf(drink("a"), drink("b")),
            hydrationEntries = listOf(hydration("a", t0), hydration("b", t2)),
            nutritionEntries = emptyList(),
        )
        assertEquals(listOf("b", "a"), result.map { it.id })
    }

    @Test
    fun `frequentHydrationDrinkOptions breaks a count and recency tie on the saved order`() {
        val result = frequentHydrationDrinkOptions(
            drinks = listOf(drink("a"), drink("b")),
            hydrationEntries = listOf(
                hydration("b", t0, uuid = "u1"),
                hydration("a", t0, uuid = "u2"),
            ),
            nutritionEntries = emptyList(),
        )
        assertEquals(listOf("a", "b"), result.map { it.id })
    }

    @Test
    fun `frequentHydrationDrinkOptions ignores entries for unknown or deleted drinks`() {
        val result = frequentHydrationDrinkOptions(
            drinks = listOf(drink("a")),
            hydrationEntries = listOf(hydration("a", t0), hydration("gone", t1)),
            nutritionEntries = emptyList(),
        )
        assertEquals(listOf("a"), result.map { it.id })
    }

    @Test
    fun `frequentHydrationDrinkOptions does not double-count a hydration record and its paired nutrition`() {
        val hydrationEntry = hydration("a", t0)
        val result = frequentHydrationDrinkOptions(
            drinks = listOf(drink("a"), drink("b")),
            hydrationEntries = listOf(hydrationEntry, hydration("b", t1, uuid = "u2")),
            nutritionEntries = listOf(
                nutrition(
                    time = t0,
                    clientRecordId = "openvitals_hydration_nutrition_${hydrationEntry.clientRecordId}",
                ),
            ),
        )
        // Both logged once; the tie breaks on recency, so 'b' leads.
        assertEquals(listOf("b", "a"), result.map { it.id })
    }

    @Test
    fun `frequentHydrationDrinkOptions counts a paired nutrition record whose hydration half never wrote`() {
        // A zero-hydration drink writes nutrition only, so nothing counted the
        // hydration client record id.
        val orphanHydrationId = "openvitals_hydration_5_drink_a_u9"
        val result = frequentHydrationDrinkOptions(
            drinks = listOf(drink("a")),
            hydrationEntries = emptyList(),
            nutritionEntries = listOf(
                nutrition(
                    time = t0,
                    clientRecordId = "openvitals_hydration_nutrition_$orphanHydrationId",
                ),
            ),
        )
        assertEquals(listOf("a"), result.map { it.id })
    }

    @Test
    fun `frequentHydrationDrinkOptions falls back to matching a standalone nutrition entry by drink name`() {
        val result = frequentHydrationDrinkOptions(
            drinks = listOf(drink("a", name = "Cold Brew")),
            hydrationEntries = emptyList(),
            nutritionEntries = listOf(
                nutrition(
                    time = t0,
                    clientRecordId = "openvitals_nutrition_1_u",
                    name = "  cold brew ",
                ),
            ),
        )
        assertEquals(listOf("a"), result.map { it.id })
    }

    @Test
    fun `frequentHydrationDrinkOptions ignores entries from other apps`() {
        val result = frequentHydrationDrinkOptions(
            drinks = listOf(drink("a", name = "Cola")),
            hydrationEntries = emptyList(),
            nutritionEntries = listOf(
                nutrition(
                    time = t0,
                    clientRecordId = "someone_else",
                    name = "Cola",
                    isOpenVitals = false,
                ),
            ),
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `frequentHydrationDrinkOptions caps the list at the frequent-drink limit`() {
        val drinks = (0 until FrequentHydrationDrinkLimit + 3).map { drink("d$it") }
        val result = frequentHydrationDrinkOptions(
            drinks = drinks,
            hydrationEntries = drinks.mapIndexed { index, saved ->
                hydration(
                    drinkId = saved.id,
                    time = t0.plus(Duration.ofMinutes(index.toLong())),
                    uuid = "u$index",
                )
            },
            nutritionEntries = emptyList(),
        )
        assertEquals(FrequentHydrationDrinkLimit, result.size)
    }

    @Test
    fun `frequentHydrationDrinkOptions is empty when there are no saved drinks`() {
        val result = frequentHydrationDrinkOptions(
            drinks = emptyList(),
            hydrationEntries = listOf(hydration("a", t0)),
            nutritionEntries = emptyList(),
        )
        assertTrue(result.isEmpty())
    }
}
