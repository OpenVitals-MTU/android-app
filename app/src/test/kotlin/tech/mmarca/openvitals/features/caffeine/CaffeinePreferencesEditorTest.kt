package tech.mmarca.openvitals.features.caffeine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.UnitSystem

class CaffeinePreferencesEditorTest {

    @Test
    fun `metric weight displays and stores kilograms`() {
        assertEquals(
            70.0,
            displayWeightForUnitSystem(70.0, UnitSystem.METRIC)!!,
            0.001,
        )
        assertEquals(
            70.0,
            storedWeightKgForUnitSystem(70.0, UnitSystem.METRIC)!!,
            0.001,
        )
    }

    @Test
    fun `imperial weight displays pounds and stores kilograms`() {
        assertEquals(
            154.323,
            displayWeightForUnitSystem(70.0, UnitSystem.IMPERIAL)!!,
            0.001,
        )
        assertEquals(
            70.0,
            storedWeightKgForUnitSystem(154.323583526, UnitSystem.IMPERIAL)!!,
            0.001,
        )
    }

    @Test
    fun `empty weight remains empty`() {
        assertNull(displayWeightForUnitSystem(null, UnitSystem.IMPERIAL))
        assertNull(storedWeightKgForUnitSystem(null, UnitSystem.IMPERIAL))
    }
}
