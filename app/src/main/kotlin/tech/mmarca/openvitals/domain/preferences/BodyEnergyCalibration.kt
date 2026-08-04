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
    // Personal gains: each scales one drain/charge component of the objective
    // model, clamped to [MinGain, MaxGain] so every adjustment stays one
    // legible number. 1.0 is the neutral default. The retired watch
    // integration used to fit these from Body Battery readings; gains it
    // learned before its removal are preserved and still applied.
    val sleepChargeGain: Double = 1.0,
    val activityDrainGain: Double = 1.0,
    val basalDrainGain: Double = 1.0,
    val stressDrainGain: Double = 1.0,
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
        )
    }

    /** Whether the gains differ from the neutral defaults. */
    val hasPersonalGains: Boolean
        get() = clampedSleepChargeGain != 1.0 ||
            clampedActivityDrainGain != 1.0 ||
            clampedBasalDrainGain != 1.0 ||
            clampedStressDrainGain != 1.0

    /**
     * The half a user sets: whether zones are manual, and what they are.
     *
     * Split from [gainSignature] because the two can change independently.
     * This one moves when someone edits a setting; the gains carry whatever
     * the retired watch fit learned. Anything that only needs to know "is
     * this still the same person's configuration" wants this half alone.
     */
    fun zoneSignature(): String {
        val normalized = normalized()
        return listOf(
            normalized.useManualZones,
            normalized.manualZoneThresholdsBpm?.toPreferenceString() ?: "auto",
        ).joinToString("|")
    }

    /** The personal-gain half, formatted to three decimals. */
    fun gainSignature(): String {
        val normalized = normalized()
        return listOf(
            formatGain(normalized.clampedSleepChargeGain),
            formatGain(normalized.clampedActivityDrainGain),
            formatGain(normalized.clampedBasalDrainGain),
            formatGain(normalized.clampedStressDrainGain),
        ).joinToString("|")
    }

    /**
     * Both halves, unchanged: a timeline really was computed with these gains, so
     * serving a cached one still requires all of it to match.
     */
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
 * The generation of the Body Energy SETUP requirements.
 *
 * Bumped when setup starts demanding something it did not before, so installs
 * that completed setup under the old rules are asked once for the missing piece
 * instead of running on a value the model can no longer derive.
 *
 * 1 — automatic zones need a birth year. The manual maximum heart rate was
 * removed, leaving Tanaka from age as the only estimate; without it the model
 * falls back to resting + 70, which for a resting 60 claims a maximum of 130 and
 * reads ordinary effort as zone 5.
 */
const val BodyEnergySetupEpoch = 1
