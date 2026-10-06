package tech.mmarca.openvitals.domain.insights

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.BiologicalSex
import tech.mmarca.openvitals.domain.preferences.BodyProfile

/** Sun et al. (2003), and the inputs it gives no answer for. */
class BioimpedanceCompositionTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 6)

    private val profile = BodyProfile(birthYear = 1991, heightCm = 175.0, sex = BiologicalSex.MALE)

    @Test
    fun `the published equations hold for both sexes`() {
        // 175 cm, 75 kg, 500 ohm: height squared over resistance is 61.25.
        val man = BioimpedanceComposition.estimate(BiologicalSex.MALE, 75.0, 175.0, 500.0)!!
        val woman = BioimpedanceComposition.estimate(BiologicalSex.FEMALE, 75.0, 175.0, 500.0)!!

        // -10.68 + 0.65*61.25 + 0.26*75 + 0.02*500
        assertEquals(58.6325, man.fatFreeMassKg, 1e-9)
        assertEquals(100.0 * (1.0 - 58.6325 / 75.0), man.bodyFatPercent, 1e-9)
        // 1.20 + 0.45*61.25 + 0.18*75
        assertEquals(42.2625, man.bodyWaterKg, 1e-9)
        // -9.53 + 0.69*61.25 + 0.17*75 + 0.02*500
        assertEquals(55.4825, woman.fatFreeMassKg, 1e-9)
        // 3.75 + 0.45*61.25 + 0.11*75
        assertEquals(39.5625, woman.bodyWaterKg, 1e-9)
    }

    @Test
    fun `inputs outside what the equations were fitted on give no estimate`() {
        fun estimate(
            weightKg: Double = 75.0,
            heightCm: Double = 175.0,
            resistanceOhm: Double = 500.0,
            ageYears: Int? = 35,
        ) = BioimpedanceComposition.estimate(BiologicalSex.MALE, weightKg, heightCm, resistanceOhm, ageYears)

        val refused = mapOf(
            "a resistance no body has" to estimate(resistanceOhm = 150.0),
            "a broken contact" to estimate(resistanceOhm = 1500.0),
            "a child" to estimate(ageYears = 9),
            "a height that is a typo" to estimate(heightCm = 17.5),
            "a weight below the scale's range" to estimate(weightKg = 10.0),
            "a result that is not a body" to estimate(weightKg = 200.0, heightCm = 150.0, resistanceOhm = 1100.0),
        )

        assertEquals(refused.mapValues { null }, refused)
    }

    @Test
    fun `the profile supplies sex, height and age, and each gap is named`() {
        assertEquals(58.6325, profile.bodyComposition(75.0, 500.0, today)!!.fatFreeMassKg, 1e-9)
        // An age the profile does not know does not stop the estimate: the equations have no age term.
        assertEquals(58.6325, profile.copy(birthYear = null).bodyComposition(75.0, 500.0, today)!!.fatFreeMassKg, 1e-9)

        assertNull(profile.copy(sex = null).bodyComposition(75.0, 500.0, today))
        assertNull(profile.copy(heightCm = null).bodyComposition(75.0, 500.0, today))
        assertEquals(
            setOf(BodyCompositionInput.SEX, BodyCompositionInput.HEIGHT),
            BodyProfile().bodyCompositionMissingInputs(),
        )
        assertEquals(emptySet<BodyCompositionInput>(), profile.bodyCompositionMissingInputs())
    }
}
