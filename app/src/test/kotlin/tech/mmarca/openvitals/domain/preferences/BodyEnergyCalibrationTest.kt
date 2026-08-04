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

    @Test
    fun `gains default to neutral and normalize into their bounds`() {
        val automatic = BodyEnergyCalibration.Automatic

        assertEquals(1.0, automatic.sleepChargeGain, 0.0)
        assertFalse(automatic.hasPersonalGains)

        val normalized = BodyEnergyCalibration(
            sleepChargeGain = 9.0,
            activityDrainGain = 0.0,
        ).normalized()

        assertEquals(BodyEnergyCalibration.MaxGain, normalized.sleepChargeGain, 0.0)
        assertEquals(BodyEnergyCalibration.MinGain, normalized.activityDrainGain, 0.0)
    }

    @Test
    fun `the zone signature ignores the personal gains`() {
        // The chain anchor only wants the configuration half, not the gains.
        val neutral = BodyEnergyCalibration()
        val learned = BodyEnergyCalibration(activityDrainGain = 1.4)

        assertEquals(neutral.zoneSignature(), learned.zoneSignature())
        assertTrue(neutral.gainSignature() != learned.gainSignature())
        assertTrue(neutral.signature() != learned.signature())
        assertEquals("${learned.zoneSignature()}|${learned.gainSignature()}", learned.signature())
    }

    @Test
    fun `the gain signature is rounded to three decimals`() {
        val a = BodyEnergyCalibration(activityDrainGain = 1.2000001)
        val b = BodyEnergyCalibration(activityDrainGain = 1.2000009)

        assertEquals(a.gainSignature(), b.gainSignature())
        assertEquals("1.000|1.200|1.000|1.000", a.gainSignature())
    }
}
