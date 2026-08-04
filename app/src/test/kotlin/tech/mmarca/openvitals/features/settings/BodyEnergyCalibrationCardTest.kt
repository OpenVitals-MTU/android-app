package tech.mmarca.openvitals.features.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.HeartZoneThresholds

/**
 * The rule the Flutter build's `body_settings_cards_test.dart` asserts through
 * the Body Energy calibration card: switching manual zones OFF stops them
 * applying but must not ERASE them, so a ladder typed once can be switched
 * back on rather than retyped.
 */
class BodyEnergyCalibrationCardTest {

    private val ladder = HeartZoneThresholds(
        zone1LowerBpm = 90,
        zone2LowerBpm = 110,
        zone3LowerBpm = 130,
        zone4LowerBpm = 150,
        zone5LowerBpm = 170,
    )

    @Test
    fun `switching manual zones off keeps the typed ladder`() {
        val saved = BodyEnergyCalibration(
            manualZoneThresholdsBpm = ladder,
            useManualZones = false,
            setupCompleted = true,
        ).normalized()

        assertFalse(saved.useManualZones)
        assertEquals(ladder, saved.manualZoneThresholdsBpm)
    }

    @Test
    fun `an invalid ladder is the one thing that does erase it`() {
        // Not a ladder at all — zone 3 sits below zone 2, so there is nothing
        // to switch back on.
        val saved = BodyEnergyCalibration(
            manualZoneThresholdsBpm = ladder.copy(zone3LowerBpm = 100),
            useManualZones = true,
        ).normalized()

        assertNull(saved.manualZoneThresholdsBpm)
        assertFalse(saved.useManualZones)
    }
}
