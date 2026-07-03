package tech.mmarca.openvitals.data.local.beverage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory

class BeverageEntityTest {
    @Test
    fun `preloaded defaults include water category drinks`() {
        val waters = BeverageEntity.preloadedDefaults()
            .take(2)
            .map(BeverageEntity::toDomain)

        assertEquals(listOf("openvitals-still-water", "openvitals-gasified-water"), waters.map { it.id })
        assertEquals(listOf("Still water", "Gasified water"), waters.map { it.name })
        waters.forEach { water ->
            assertEquals(100.0, water.volumeMilliliters, 0.001)
            assertEquals(1.0, water.hydrationMultiplier, 0.001)
            assertEquals(CaffeineSourceCategory.WATER, water.category)
            assertEquals(true, water.isPreloaded)
            assertTrue(water.nutrientValues.isEmpty())
        }
    }
}
