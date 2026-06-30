package tech.mmarca.openvitals.domain.preferences

import java.time.LocalDate

data class HeartZoneThresholds(
    val zone1LowerBpm: Int,
    val zone2LowerBpm: Int,
    val zone3LowerBpm: Int,
    val zone4LowerBpm: Int,
    val zone5LowerBpm: Int,
) {
    fun normalized(): HeartZoneThresholds? {
        val values = listOf(zone1LowerBpm, zone2LowerBpm, zone3LowerBpm, zone4LowerBpm, zone5LowerBpm)
        if (values.any { it !in MinZoneBpm..MaxZoneBpm }) return null
        if (values.zipWithNext().any { (first, second) -> second <= first }) return null
        return this
    }

    fun toPreferenceString(): String =
        listOf(zone1LowerBpm, zone2LowerBpm, zone3LowerBpm, zone4LowerBpm, zone5LowerBpm)
            .joinToString(PreferenceSeparator)

    companion object {
        const val MinZoneBpm = 40
        const val MaxZoneBpm = 240
        private const val PreferenceSeparator = ","

        fun fromPreferenceString(value: String?): HeartZoneThresholds? {
            val parts = value
                ?.split(PreferenceSeparator)
                ?.mapNotNull { it.toIntOrNull() }
                ?: return null
            if (parts.size != 5) return null
            return HeartZoneThresholds(
                zone1LowerBpm = parts[0],
                zone2LowerBpm = parts[1],
                zone3LowerBpm = parts[2],
                zone4LowerBpm = parts[3],
                zone5LowerBpm = parts[4],
            ).normalized()
        }
    }
}

data class BodyEnergyCalibration(
    val birthYear: Int? = null,
    val manualMaxHeartRateBpm: Int? = null,
    val manualRestingHeartRateBpm: Int? = null,
    val manualZoneThresholdsBpm: HeartZoneThresholds? = null,
    val useManualZones: Boolean = false,
) {
    fun normalized(today: LocalDate = LocalDate.now()): BodyEnergyCalibration {
        val currentYear = today.year
        val normalizedBirthYear = birthYear
            ?.takeIf { it in MinBirthYear..currentYear }
        val normalizedMaxHr = manualMaxHeartRateBpm
            ?.coerceIn(MinMaxHeartRateBpm, MaxMaxHeartRateBpm)
        val normalizedRestingHr = manualRestingHeartRateBpm
            ?.coerceIn(MinRestingHeartRateBpm, MaxRestingHeartRateBpm)
        val normalizedZones = manualZoneThresholdsBpm?.normalized()
        return BodyEnergyCalibration(
            birthYear = normalizedBirthYear,
            manualMaxHeartRateBpm = normalizedMaxHr,
            manualRestingHeartRateBpm = normalizedRestingHr,
            manualZoneThresholdsBpm = normalizedZones,
            useManualZones = useManualZones && normalizedZones != null,
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
            normalized.manualMaxHeartRateBpm ?: "auto",
            normalized.manualRestingHeartRateBpm ?: "auto",
            normalized.useManualZones,
            normalized.manualZoneThresholdsBpm?.toPreferenceString() ?: "auto",
        ).joinToString("|")
    }

    companion object {
        val Automatic = BodyEnergyCalibration()
        const val MinBirthYear = 1900
        const val MinAgeYears = 10
        const val MaxAgeYears = 110
        const val MinMaxHeartRateBpm = 80
        const val MaxMaxHeartRateBpm = 240
        const val MinRestingHeartRateBpm = 30
        const val MaxRestingHeartRateBpm = 120
    }
}
