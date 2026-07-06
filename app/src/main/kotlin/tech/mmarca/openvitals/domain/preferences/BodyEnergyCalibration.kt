package tech.mmarca.openvitals.domain.preferences

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
    val manualZoneThresholdsBpm: HeartZoneThresholds? = null,
    val useManualZones: Boolean = false,
    val setupCompleted: Boolean = false,
) {
    fun normalized(): BodyEnergyCalibration {
        val normalizedZones = manualZoneThresholdsBpm?.normalized()
        return BodyEnergyCalibration(
            manualZoneThresholdsBpm = normalizedZones,
            useManualZones = useManualZones && normalizedZones != null,
            setupCompleted = setupCompleted,
        )
    }

    fun signature(): String {
        val normalized = normalized()
        return listOf(
            normalized.useManualZones,
            normalized.manualZoneThresholdsBpm?.toPreferenceString() ?: "auto",
        ).joinToString("|")
    }

    companion object {
        val Automatic = BodyEnergyCalibration()
    }
}
