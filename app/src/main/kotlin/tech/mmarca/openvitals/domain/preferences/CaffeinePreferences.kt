package tech.mmarca.openvitals.domain.preferences

import java.time.LocalTime
import kotlin.math.roundToInt

data class CaffeinePreferences(
    val profileCompleted: Boolean = false,
    val halfLifeMinutes: Int = DefaultHalfLifeMinutes,
    val absorptionMinutes: Int = DefaultAbsorptionMinutes,
    val sleepThresholdMg: Int = DefaultSleepThresholdMg,
    val bedtime: LocalTime = DefaultBedtime,
    val ageYears: Int? = null,
    val weightKg: Double? = null,
    val sleepSensitivity: CaffeineSleepSensitivity = CaffeineSleepSensitivity.NORMAL,
    val smoker: Boolean = false,
    val alcoholUse: CaffeineAlcoholUse = CaffeineAlcoholUse.NONE,
    val caffeineHabituation: CaffeineHabituation = CaffeineHabituation.MODERATE,
    val liverImpairment: Boolean = false,
    val medicationInteraction: Boolean = false,
    val cyp1a2Genotype: CaffeineGenotype = CaffeineGenotype.UNKNOWN,
    val ahrGenotype: CaffeineGenotype = CaffeineGenotype.UNKNOWN,
    val hormonalStatus: CaffeineHormonalStatus = CaffeineHormonalStatus.NONE,
) {
    fun normalized(): CaffeinePreferences =
        copy(
            halfLifeMinutes = halfLifeMinutes.coerceIn(MinHalfLifeMinutes, MaxHalfLifeMinutes),
            absorptionMinutes = absorptionMinutes.coerceIn(MinAbsorptionMinutes, MaxAbsorptionMinutes),
            sleepThresholdMg = sleepThresholdMg.coerceIn(MinSleepThresholdMg, MaxSleepThresholdMg),
            ageYears = ageYears?.coerceIn(MinAgeYears, MaxAgeYears),
            weightKg = weightKg
                ?.takeIf { it.isFinite() }
                ?.coerceIn(MinWeightKg, MaxWeightKg),
        )

    val effectiveHalfLifeMinutes: Int
        get() {
            val base = halfLifeMinutes.toDouble()
            val multiplier = listOf(
                sleepSensitivity.halfLifeMultiplier,
                alcoholUse.halfLifeMultiplier,
                caffeineHabituation.halfLifeMultiplier,
                cyp1a2Genotype.halfLifeMultiplier,
                ahrGenotype.halfLifeMultiplier,
                hormonalStatus.halfLifeMultiplier,
                if (smoker) 0.7 else 1.0,
                if (liverImpairment) 1.8 else 1.0,
                if (medicationInteraction) 1.4 else 1.0,
                ageMultiplier,
                weightMultiplier,
            ).fold(1.0) { product, factor -> product * factor }
            return (base * multiplier)
                .roundToInt()
                .coerceIn(MinHalfLifeMinutes, MaxEffectiveHalfLifeMinutes)
        }

    private val ageMultiplier: Double
        get() = when (ageYears) {
            null -> 1.0
            in 0..17 -> 1.1
            in 18..44 -> 1.0
            in 45..64 -> 1.1
            else -> 1.2
        }

    private val weightMultiplier: Double
        get() = when {
            weightKg == null -> 1.0
            weightKg < 55.0 -> 1.1
            weightKg > 95.0 -> 0.92
            else -> 1.0
        }

    companion object {
        const val DefaultHalfLifeMinutes = 300
        const val DefaultAbsorptionMinutes = 45
        const val DefaultSleepThresholdMg = 60
        val DefaultBedtime: LocalTime = LocalTime.of(22, 30)
        const val DefaultConsumptionDurationMinutes = 10
        const val MinHalfLifeMinutes = 90
        const val MaxHalfLifeMinutes = 720
        const val MaxEffectiveHalfLifeMinutes = 1_080
        const val MinAbsorptionMinutes = 10
        const val MaxAbsorptionMinutes = 180
        const val MinSleepThresholdMg = 5
        const val MaxSleepThresholdMg = 300
        const val MinAgeYears = 10
        const val MaxAgeYears = 110
        const val MinWeightKg = 30.0
        const val MaxWeightKg = 250.0
    }
}

enum class CaffeineSleepSensitivity(val halfLifeMultiplier: Double) {
    LOW(0.9),
    NORMAL(1.0),
    HIGH(1.2),
    INSOMNIA(1.35),
}

enum class CaffeineAlcoholUse(val halfLifeMultiplier: Double) {
    NONE(1.0),
    OCCASIONAL(1.05),
    REGULAR(1.15),
}

enum class CaffeineHabituation(val halfLifeMultiplier: Double) {
    LOW(1.1),
    MODERATE(1.0),
    HIGH(0.95),
}

enum class CaffeineGenotype(val halfLifeMultiplier: Double) {
    UNKNOWN(1.0),
    FAST(0.85),
    NORMAL(1.0),
    SLOW(1.25),
}

enum class CaffeineHormonalStatus(val halfLifeMultiplier: Double) {
    NONE(1.0),
    ORAL_CONTRACEPTIVE(1.4),
    PREGNANT(1.7),
}
