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
    // Personal gains, one per component. 1.0 is neutral; the watch fit nudges within bounds.
    val sleepChargeGain: Double = 1.0,
    val activityDrainGain: Double = 1.0,
    val basalDrainGain: Double = 1.0,
    val stressDrainGain: Double = 1.0,
    // How many watch readings have informed the gains, for display.
    val watchObservationCount: Int = 0,
) {
    private val clampedSleepChargeGain: Double get() = sleepChargeGain.coerceIn(MinGain, MaxGain)
    private val clampedActivityDrainGain: Double get() = activityDrainGain.coerceIn(MinGain, MaxGain)
    private val clampedBasalDrainGain: Double get() = basalDrainGain.coerceIn(MinGain, MaxGain)
    private val clampedStressDrainGain: Double get() = stressDrainGain.coerceIn(MinGain, MaxGain)

    fun normalized(): BodyEnergyCalibration {
        val normalizedZones = manualZoneThresholdsBpm?.normalized()
        return BodyEnergyCalibration(
            manualZoneThresholdsBpm = normalizedZones,
            useManualZones = useManualZones && normalizedZones != null,
            setupCompleted = setupCompleted,
            sleepChargeGain = clampedSleepChargeGain,
            activityDrainGain = clampedActivityDrainGain,
            basalDrainGain = clampedBasalDrainGain,
            stressDrainGain = clampedStressDrainGain,
            watchObservationCount = watchObservationCount.coerceAtLeast(0),
        )
    }

    /** Whether a watch has contributed to the gains, for the calibration copy. */
    val hasWatchObservations: Boolean get() = watchObservationCount > 0

    /** Whether the gains differ from the neutral defaults. */
    val hasPersonalGains: Boolean
        get() = clampedSleepChargeGain != 1.0 ||
            clampedActivityDrainGain != 1.0 ||
            clampedBasalDrainGain != 1.0 ||
            clampedStressDrainGain != 1.0

    /**
     * The half a user sets. Split from [gainSignature]: the gains move on
     * their own every sync, and would invalidate a cache keyed on both.
     */
    fun zoneSignature(): String {
        val normalized = normalized()
        return listOf(
            normalized.useManualZones,
            normalized.manualZoneThresholdsBpm?.toPreferenceString() ?: "auto",
        ).joinToString("|")
    }

    /** The half the watch fit moves, in steps far smaller than three decimals. */
    fun gainSignature(): String {
        val normalized = normalized()
        return listOf(
            formatGain(normalized.clampedSleepChargeGain),
            formatGain(normalized.clampedActivityDrainGain),
            formatGain(normalized.clampedBasalDrainGain),
            formatGain(normalized.clampedStressDrainGain),
        ).joinToString("|")
    }

    /** Both halves: serving a cached timeline requires all of it to match. */
    fun signature(): String = "${zoneSignature()}|${gainSignature()}"

    companion object {
        const val MinGain = 0.5
        const val MaxGain = 2.0

        val Automatic = BodyEnergyCalibration()

        private fun formatGain(value: Double): String =
            String.format(java.util.Locale.US, "%.3f", value)
    }
}

/**
 * The generation of the setup requirements. Bump when setup demands
 * something new. 1: automatic zones need a birth year.
 */
const val BodyEnergySetupEpoch = 1
