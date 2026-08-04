package tech.mmarca.openvitals.sensors.ble.parsers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BleParsersTest {
    @Test
    fun parseHeartRate_uint8() {
        assertEquals(74L, BleHeartRateParser.parseBytes(byteArrayOf(0x00, 0x4A)))
    }

    @Test
    fun parseHeartRate_uint16() {
        assertEquals(300L, BleHeartRateParser.parseBytes(byteArrayOf(0x01, 0x2C, 0x01)))
    }

    @Test
    fun parseCyclingPower_basic() {
        val payload = byteArrayOf(
            0x20,
            0x00,
            0x64, 0x00,
            0x05, 0x00,
            0x10, 0x00,
        )
        val parsed = BleCyclingPowerParser.parsePayload(payload)
        assertNotNull(parsed)
        assertEquals(100, parsed?.powerWatts)
        assertEquals(5L, parsed?.crank?.crankRevolutionsCount)
    }

    @Test
    fun parseCyclingSpeedCadence_wheelAndCrank() {
        val payload = byteArrayOf(
            0x03,
            0x10, 0x00, 0x00, 0x00,
            0x20, 0x00,
            0x05, 0x00,
            0x30, 0x00,
        )
        val parsed = BleCyclingSpeedCadenceParser.parsePayload(payload)
        assertNotNull(parsed)
        assertEquals(16L, parsed?.first?.wheelRevolutionsCount)
        assertEquals(5L, parsed?.second?.crankRevolutionsCount)
    }

    @Test
    fun parseRunningSpeedCadence() {
        val payload = byteArrayOf(
            0x00,
            0x00, 0x02,
            0x50,
        )
        val parsed = BleRunningSpeedCadenceParser.parsePayload(payload, sensorName = "Stryd")
        assertNotNull(parsed)
        assertEquals(2.0, parsed?.speedMetersPerSecond)
        assertEquals(80L, parsed?.cadenceRpm)
    }

    @Test
    fun parseRunningSpeedCadence_tickrXAdjustsCadence() {
        val payload = byteArrayOf(
            0x00,
            0x00, 0x02,
            0x64,
        )
        val parsed = BleRunningSpeedCadenceParser.parsePayload(payload, sensorName = "TICKR X 1234")
        assertEquals(50L, parsed?.cadenceRpm)
    }

    @Test
    fun parseHeartRate_zeroSignalPayload() {
        assertTrue(BleHeartRateParser.isZeroSignal(byteArrayOf(0x00, 0x00)))
        assertNull(BleHeartRateParser.parseBytes(byteArrayOf(0x00, 0x00)))
    }

    @Test
    fun parseHeartRate_singleByte() {
        assertEquals(74L, BleHeartRateParser.parseBytes(byteArrayOf(0x4A)))
    }

    @Test
    fun parseHeartRate_emptyPayloadReturnsNull() {
        assertNull(BleHeartRateParser.parseBytes(byteArrayOf()))
    }
}
