package tech.mmarca.openvitals.features.settings

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences

/** What an out-of-range half-life is normalized to on save, and the bedtime the card seeds from. */
class CaffeinePreferencesCardTest {

    @Test
    fun `an out-of-range half-life is clamped on save`() {
        // The model is what refuses to store a half-life of six days.
        val typed = CaffeinePreferences(halfLifeMinutes = 9000)

        assertEquals(
            CaffeinePreferences.MaxHalfLifeMinutes,
            typed.normalized().halfLifeMinutes,
        )
        assertEquals(720, CaffeinePreferences.MaxHalfLifeMinutes)
    }

    @Test
    fun `an under-range half-life is clamped to the minimum`() {
        val typed = CaffeinePreferences(halfLifeMinutes = 0)

        assertEquals(
            CaffeinePreferences.MinHalfLifeMinutes,
            typed.normalized().halfLifeMinutes,
        )
    }

    @Test
    fun `an in-range half-life is stored unchanged`() {
        assertEquals(360, CaffeinePreferences(halfLifeMinutes = 360).normalized().halfLifeMinutes)
    }

    @Test
    fun `the bedtime default matches the model`() {
        assertEquals(LocalTime.of(22, 30), CaffeinePreferences().bedtime)
        assertEquals(LocalTime.of(22, 30), CaffeinePreferences.DefaultBedtime)
    }
}
