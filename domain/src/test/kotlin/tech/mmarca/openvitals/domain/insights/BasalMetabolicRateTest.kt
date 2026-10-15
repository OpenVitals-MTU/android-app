package tech.mmarca.openvitals.domain.insights

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.BiologicalSex
import tech.mmarca.openvitals.domain.preferences.BodyProfile

/** Mifflin-St Jeor, and the profile gaps that stop it. */
class BasalMetabolicRateTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25)

    private val profile = BodyProfile(
        birthYear = 1996,
        weightKg = 70.0,
        heightCm = 175.0,
        sex = BiologicalSex.MALE,
    )

    @Test
    fun `the published worked example holds for both sexes`() {
        // 10*70 + 6.25*175 - 5*30 = 1643.75, then +5 or -161.
        assertEquals(1648.75, BasalMetabolicRate.mifflinStJeor(BiologicalSex.MALE, 70.0, 175.0, 30), 1e-9)
        assertEquals(1482.75, BasalMetabolicRate.mifflinStJeor(BiologicalSex.FEMALE, 70.0, 175.0, 30), 1e-9)
    }

    @Test
    fun `a complete profile gives today's estimate from the age on that day`() {
        assertEquals(1648.75, profile.basalMetabolicRateKcal(today)!!, 1e-9)
        // A year later the same profile is a year older: five kcal less.
        assertEquals(1643.75, profile.basalMetabolicRateKcal(today.plusYears(1))!!, 1e-9)
        assertEquals(emptySet<BmrInput>(), profile.bmrMissingInputs(today))
    }

    @Test
    fun `each missing input is named and stops the estimate`() {
        assertEquals(setOf(BmrInput.SEX), profile.copy(sex = null).bmrMissingInputs(today))
        assertEquals(setOf(BmrInput.BIRTH_YEAR), profile.copy(birthYear = null).bmrMissingInputs(today))
        assertEquals(setOf(BmrInput.WEIGHT), profile.copy(weightKg = null).bmrMissingInputs(today))
        assertEquals(setOf(BmrInput.HEIGHT), profile.copy(heightCm = null).bmrMissingInputs(today))
        assertNull(profile.copy(sex = null).basalMetabolicRateKcal(today))
        assertNull(profile.copy(heightCm = null).basalMetabolicRateKcal(today))
    }

    @Test
    fun `an age outside the profile bounds counts as no birth year`() {
        val infant = profile.copy(birthYear = today.year - 5)
        assertEquals(setOf(BmrInput.BIRTH_YEAR), infant.bmrMissingInputs(today))
        assertNull(infant.basalMetabolicRateKcal(today))
    }

    @Test
    fun `an empty profile is missing everything`() {
        assertEquals(BmrInput.entries.toSet(), BodyProfile().bmrMissingInputs(today))
    }
}
