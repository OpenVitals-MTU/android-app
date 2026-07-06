package tech.mmarca.openvitals.domain.preferences

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
        ).normalized()

        assertNull(normalized.manualZoneThresholdsBpm)
        assertFalse(normalized.useManualZones)
    }

    @Test
    fun `automatic calibration has no manual zones`() {
        val automatic = BodyEnergyCalibration.Automatic

        assertFalse(automatic.useManualZones)
        assertTrue(automatic.signature().contains("auto"))
    }

    @Test
    fun `automatic calibration defaults to setup not completed`() {
        assertFalse(BodyEnergyCalibration.Automatic.setupCompleted)
    }

    @Test
    fun `normalization preserves setupCompleted flag`() {
        val normalized = BodyEnergyCalibration(setupCompleted = true).normalized()

        assertTrue(normalized.setupCompleted)
    }
}
