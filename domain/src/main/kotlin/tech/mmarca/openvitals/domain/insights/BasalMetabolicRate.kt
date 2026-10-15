package tech.mmarca.openvitals.domain.insights

import java.time.LocalDate
import tech.mmarca.openvitals.domain.preferences.BiologicalSex
import tech.mmarca.openvitals.domain.preferences.BodyProfile

/** A body profile field the basal metabolic rate estimate cannot do without. */
enum class BmrInput { SEX, BIRTH_YEAR, WEIGHT, HEIGHT }

/**
 * Mifflin-St Jeor (1990): the calories a body at rest burns in a day, from
 * weight, height, age and sex. The formula most dietetic guidelines prefer
 * for adults; it needs all four inputs and gives no answer without them.
 */
object BasalMetabolicRate {
    fun mifflinStJeor(sex: BiologicalSex, weightKg: Double, heightCm: Double, ageYears: Int): Double {
        val sexTerm = when (sex) {
            BiologicalSex.MALE -> 5.0
            BiologicalSex.FEMALE -> -161.0
        }
        return 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears + sexTerm
    }
}

/** What the estimate still needs from this profile. Empty means it can be computed. */
fun BodyProfile.bmrMissingInputs(today: LocalDate = LocalDate.now()): Set<BmrInput> = buildSet {
    if (sex == null) add(BmrInput.SEX)
    if (ageYears(today) == null) add(BmrInput.BIRTH_YEAR)
    if (weightKg == null) add(BmrInput.WEIGHT)
    if (heightCm == null) add(BmrInput.HEIGHT)
}

/** The Mifflin-St Jeor estimate for [today], or null while an input is missing. */
fun BodyProfile.basalMetabolicRateKcal(today: LocalDate = LocalDate.now()): Double? {
    val age = ageYears(today) ?: return null
    return BasalMetabolicRate.mifflinStJeor(
        sex = sex ?: return null,
        weightKg = weightKg ?: return null,
        heightCm = heightCm ?: return null,
        ageYears = age,
    )
}
