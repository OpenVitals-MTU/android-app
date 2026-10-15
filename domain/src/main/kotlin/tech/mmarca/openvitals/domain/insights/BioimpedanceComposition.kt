package tech.mmarca.openvitals.domain.insights

import java.time.LocalDate
import tech.mmarca.openvitals.domain.preferences.BiologicalSex
import tech.mmarca.openvitals.domain.preferences.BodyProfile

/** A body profile field the composition estimate cannot do without. */
enum class BodyCompositionInput { SEX, HEIGHT }

/** What a weight and a body impedance say about what the body is made of. An estimate, not a measurement. */
data class BodyCompositionEstimate(
    val fatFreeMassKg: Double,
    val bodyFatPercent: Double,
    val bodyWaterKg: Double,
)

/**
 * Sun et al. (2003, Am J Clin Nutr 77:331-340): fat-free mass and total body
 * water from weight, height, sex and the body's resistance at 50 kHz, fitted
 * against a multicomponent model in people aged 12 to 94.
 *
 * The equations were fitted on hand-to-foot resistance and a bathroom scale
 * measures foot to foot, so the result is a consistent trend more than an
 * absolute value. It will not match the figure a scale shows: manufacturers
 * do not publish theirs.
 */
object BioimpedanceComposition {

    private val ResistanceOhm = 200.0..1200.0
    private val HeightCm = 100.0..230.0
    private val WeightKg = 20.0..250.0
    private val AgeYears = 12..94

    // A result outside these shares of the body weight means the contact was bad, not that the body is odd.
    private val FatFreeShare = 0.30..0.97
    private val WaterShare = 0.25..0.80

    /** Null when an input is out of the range the equations were fitted on, or the result is not a body. */
    fun estimate(
        sex: BiologicalSex,
        weightKg: Double,
        heightCm: Double,
        resistanceOhm: Double,
        ageYears: Int? = null,
    ): BodyCompositionEstimate? {
        if (resistanceOhm !in ResistanceOhm || heightCm !in HeightCm || weightKg !in WeightKg) return null
        if (ageYears != null && ageYears !in AgeYears) return null

        val heightSquaredOverResistance = heightCm * heightCm / resistanceOhm
        val fatFreeMassKg = when (sex) {
            BiologicalSex.MALE ->
                -10.68 + 0.65 * heightSquaredOverResistance + 0.26 * weightKg + 0.02 * resistanceOhm

            BiologicalSex.FEMALE ->
                -9.53 + 0.69 * heightSquaredOverResistance + 0.17 * weightKg + 0.02 * resistanceOhm
        }
        // In litres. A litre of body water weighs a kilogram to within one percent.
        val bodyWaterKg = when (sex) {
            BiologicalSex.MALE -> 1.20 + 0.45 * heightSquaredOverResistance + 0.18 * weightKg
            BiologicalSex.FEMALE -> 3.75 + 0.45 * heightSquaredOverResistance + 0.11 * weightKg
        }
        if (fatFreeMassKg / weightKg !in FatFreeShare || bodyWaterKg / weightKg !in WaterShare) return null

        return BodyCompositionEstimate(
            fatFreeMassKg = fatFreeMassKg,
            bodyFatPercent = 100.0 * (1.0 - fatFreeMassKg / weightKg),
            bodyWaterKg = bodyWaterKg,
        )
    }
}

/** What the estimate still needs from this profile. Empty means it can be computed. */
fun BodyProfile.bodyCompositionMissingInputs(): Set<BodyCompositionInput> = buildSet {
    if (sex == null) add(BodyCompositionInput.SEX)
    if (heightCm == null) add(BodyCompositionInput.HEIGHT)
}

/** The estimate for a weigh-in of this person, or null while the profile or the reading does not allow one. */
fun BodyProfile.bodyComposition(
    weightKg: Double,
    resistanceOhm: Double,
    today: LocalDate = LocalDate.now(),
): BodyCompositionEstimate? =
    BioimpedanceComposition.estimate(
        sex = sex ?: return null,
        weightKg = weightKg,
        heightCm = heightCm ?: return null,
        resistanceOhm = resistanceOhm,
        ageYears = ageYears(today),
    )
