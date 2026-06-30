package tech.mmarca.openvitals.domain.preferences

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyEnergyCalibrationTest {

    @Test
    fun `manual zones round trip through preference string`() {
        val zones = HeartZoneThresholds(
            zone1LowerBpm = 95,
            zone2LowerBpm = 115,
            zone3LowerBpm = 135,
            zone4LowerBpm = 155,
            zone5LowerBpm = 175,
        )

        assertEquals(zones, HeartZoneThresholds.fromPreferenceString(zones.toPreferenceString()))
    }

    @Test
    fun `invalid manual zones are ignored and manual zone mode is disabled`() {
        val normalized = BodyEnergyCalibration(
            manualZoneThresholdsBpm = HeartZoneThresholds(
                zone1LowerBpm = 90,
                zone2LowerBpm = 120,
                zone3LowerBpm = 120,
                zone4LowerBpm = 160,
                zone5LowerBpm = 180,
            ),
            useManualZones = true,
        ).normalized(LocalDate.of(2026, 6, 30))

        assertNull(normalized.manualZoneThresholdsBpm)
        assertFalse(normalized.useManualZones)
    }

    @Test
    fun `normalization keeps optional values in safe ranges`() {
        val normalized = BodyEnergyCalibration(
            birthYear = 2030,
            manualMaxHeartRateBpm = 260,
            manualRestingHeartRateBpm = 20,
        ).normalized(LocalDate.of(2026, 6, 30))

        assertNull(normalized.birthYear)
        assertEquals(BodyEnergyCalibration.MaxMaxHeartRateBpm, normalized.manualMaxHeartRateBpm)
        assertEquals(BodyEnergyCalibration.MinRestingHeartRateBpm, normalized.manualRestingHeartRateBpm)
    }

    @Test
    fun `automatic calibration has no age and no manual zones`() {
        val automatic = BodyEnergyCalibration.Automatic

        assertNull(automatic.ageYears(LocalDate.of(2026, 6, 30)))
        assertFalse(automatic.useManualZones)
        assertTrue(automatic.signature(LocalDate.of(2026, 6, 30)).contains("auto"))
    }
}
