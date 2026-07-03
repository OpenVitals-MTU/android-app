package tech.mmarca.openvitals.data.local.beverage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeverageEntityTest {
    @Test
    fun `preloaded defaults include top level water`() {
        val water = BeverageEntity.preloadedDefaults().first().toDomain()

        assertEquals("openvitals-water", water.id)
        assertEquals("Water", water.name)
        assertEquals(100.0, water.volumeMilliliters, 0.001)
        assertEquals(1.0, water.hydrationMultiplier, 0.001)
        assertEquals(null, water.category)
        assertEquals(true, water.isPreloaded)
        assertTrue(water.nutrientValues.isEmpty())
    }
}
