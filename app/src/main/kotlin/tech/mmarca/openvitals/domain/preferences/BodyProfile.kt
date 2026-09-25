package tech.mmarca.openvitals.domain.preferences

import java.time.LocalDate

data class BodyProfile(
    val birthYear: Int? = null,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val restingHeartRateBpm: Int? = null,
    val maxHeartRateBpm: Int? = null,
    /** Only the basal metabolic rate formula reads it. */
    val sex: BiologicalSex? = null,
) {
    fun normalized(today: LocalDate = LocalDate.now()): BodyProfile {
        val currentYear = today.year
        return BodyProfile(
            birthYear = birthYear?.takeIf { it in MinBirthYear..currentYear },
            weightKg = weightKg
                ?.takeIf { it.isFinite() }
                ?.coerceIn(MinWeightKg, MaxWeightKg),
            heightCm = heightCm
                ?.takeIf { it.isFinite() }
                ?.coerceIn(MinHeightCm, MaxHeightCm),
            restingHeartRateBpm = restingHeartRateBpm
                ?.coerceIn(MinRestingHeartRateBpm, MaxRestingHeartRateBpm),
            maxHeartRateBpm = maxHeartRateBpm
                ?.coerceIn(MinMaxHeartRateBpm, MaxMaxHeartRateBpm),
            sex = sex,
        )
    }

    fun ageYears(today: LocalDate = LocalDate.now()): Int? =
        birthYear
            ?.let { today.year - it }
            ?.takeIf { it in MinAgeYears..MaxAgeYears }

    /** The Body Energy inputs. Sex is not one, so a sex change does not rebuild the chain. */
    fun signature(today: LocalDate = LocalDate.now()): String {
        val normalized = normalized(today)
        return listOf(
            normalized.birthYear ?: "auto",
            normalized.weightKg ?: "auto",
            normalized.heightCm ?: "auto",
            normalized.restingHeartRateBpm ?: "auto",
            normalized.maxHeartRateBpm ?: "auto",
        ).joinToString("|")
    }

    companion object {
        const val MinBirthYear = 1900
        const val MinAgeYears = 10
        const val MaxAgeYears = 110
        const val MinWeightKg = 30.0
        const val MaxWeightKg = 250.0
        const val MinHeightCm = 50.0
        const val MaxHeightCm = 260.0
        const val MinRestingHeartRateBpm = 30
        const val MaxRestingHeartRateBpm = 120
        const val MinMaxHeartRateBpm = 80
        const val MaxMaxHeartRateBpm = 240
    }
}

enum class BiologicalSex { FEMALE, MALE }
