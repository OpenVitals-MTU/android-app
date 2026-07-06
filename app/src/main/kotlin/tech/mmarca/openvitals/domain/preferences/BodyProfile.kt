package tech.mmarca.openvitals.domain.preferences

import java.time.LocalDate

data class BodyProfile(
    val birthYear: Int? = null,
    val weightKg: Double? = null,
    val restingHeartRateBpm: Int? = null,
    val maxHeartRateBpm: Int? = null,
) {
    fun normalized(today: LocalDate = LocalDate.now()): BodyProfile {
        val currentYear = today.year
        return BodyProfile(
            birthYear = birthYear?.takeIf { it in MinBirthYear..currentYear },
            weightKg = weightKg
                ?.takeIf { it.isFinite() }
                ?.coerceIn(MinWeightKg, MaxWeightKg),
            restingHeartRateBpm = restingHeartRateBpm
                ?.coerceIn(MinRestingHeartRateBpm, MaxRestingHeartRateBpm),
            maxHeartRateBpm = maxHeartRateBpm
                ?.coerceIn(MinMaxHeartRateBpm, MaxMaxHeartRateBpm),
        )
    }

    fun ageYears(today: LocalDate = LocalDate.now()): Int? =
        birthYear
            ?.let { today.year - it }
            ?.takeIf { it in MinAgeYears..MaxAgeYears }

    fun signature(today: LocalDate = LocalDate.now()): String {
        val normalized = normalized(today)
        return listOf(
            normalized.birthYear ?: "auto",
            normalized.weightKg ?: "auto",
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
        const val MinRestingHeartRateBpm = 30
        const val MaxRestingHeartRateBpm = 120
        const val MinMaxHeartRateBpm = 80
        const val MaxMaxHeartRateBpm = 240
    }
}
