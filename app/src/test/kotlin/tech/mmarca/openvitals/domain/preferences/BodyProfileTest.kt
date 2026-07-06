package tech.mmarca.openvitals.domain.preferences

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyProfileTest {

    @Test
    fun `normalization keeps optional values in safe ranges`() {
        val normalized = BodyProfile(
            birthYear = 2030,
            weightKg = 5.0,
            maxHeartRateBpm = 260,
            restingHeartRateBpm = 20,
        ).normalized(LocalDate.of(2026, 6, 30))

        assertNull(normalized.birthYear)
        assertEquals(BodyProfile.MinWeightKg, normalized.weightKg)
        assertEquals(BodyProfile.MaxMaxHeartRateBpm, normalized.maxHeartRateBpm)
        assertEquals(BodyProfile.MinRestingHeartRateBpm, normalized.restingHeartRateBpm)
    }

    @Test
    fun `age is derived from birth year`() {
        val profile = BodyProfile(birthYear = 1990)

        assertEquals(36, profile.ageYears(LocalDate.of(2026, 6, 30)))
    }

    @Test
    fun `empty profile has no age and automatic signature`() {
        val profile = BodyProfile()

        assertNull(profile.ageYears(LocalDate.of(2026, 6, 30)))
        assertTrue(profile.signature(LocalDate.of(2026, 6, 30)).contains("auto"))
    }
}
