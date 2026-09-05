package tech.mmarca.openvitals.devices.garmin

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The live-streaming payloads, checked byte for byte. */
class GarminRealtimeTest {

    private fun b(vararg xs: Int) = ByteArray(xs.size) { xs[it].toByte() }

    private fun parse(service: GarminRealtimeService, vararg bytes: Int) =
        GarminRealtimeParser.parse(service, b(*bytes))

    @Test
    fun `heart rate carries the beat and the watch's resting figure`() {
        val reading = parse(GarminRealtimeService.HEART_RATE, 3, 72, 54, 0xFF, 0xFF)
        assertEquals(GarminRealtimeReading.HeartRate(bpm = 72, restingBpm = 54), reading)
    }

    @Test
    fun `a zero heart rate is the watch saying it has none`() {
        // Off the wrist, or still settling: not a heart that stopped.
        assertNull(parse(GarminRealtimeService.HEART_RATE, 3, 0, 0))
        // A resting figure of zero is simply absent, the beat still counts.
        assertEquals(
            GarminRealtimeReading.HeartRate(bpm = 61, restingBpm = null),
            parse(GarminRealtimeService.HEART_RATE, 3, 61, 0),
        )
    }

    @Test
    fun `steps carry the day's total and the goal`() {
        // 8000 = 0x1F40, 10000 = 0x2710, little-endian u32 each.
        val reading = parse(
            GarminRealtimeService.STEPS,
            0x40, 0x1F, 0x00, 0x00,
            0x10, 0x27, 0x00, 0x00,
        )
        assertEquals(GarminRealtimeReading.Steps(steps = 8000, goal = 10000), reading)
    }

    @Test
    fun `respiration and spo2 drop their unknown sentinels`() {
        assertEquals(
            GarminRealtimeReading.Respiration(14),
            parse(GarminRealtimeService.RESPIRATION, 14),
        )
        // The watch says "unknown" with a negative, usually -2.
        assertNull(parse(GarminRealtimeService.RESPIRATION, 0xFE))

        val spo2 = parse(GarminRealtimeService.SPO2, 97, 0x00, 0x00, 0x00, 0x00)
        assertEquals(97, (spo2 as GarminRealtimeReading.SpO2).percent)
        // -1 marks unknown, and then its timestamp is junk too.
        assertNull(parse(GarminRealtimeService.SPO2, 0xFF, 0x01, 0x02, 0x03, 0x04))
    }

    @Test
    fun `a truncated packet is dropped, never guessed at`() {
        // These arrive unsolicited on a link held for hours; one odd packet must not take it down.
        assertNull(parse(GarminRealtimeService.HEART_RATE, 3))
        assertNull(parse(GarminRealtimeService.STEPS, 0x40, 0x1F))
        assertNull(GarminRealtimeParser.parse(GarminRealtimeService.SPO2, ByteArray(0)))
    }

    @Test
    fun `service codes match the watch's table`() {
        assertEquals(GarminRealtimeService.HEART_RATE, GarminRealtimeParser.serviceFor(6))
        assertEquals(GarminRealtimeService.STEPS, GarminRealtimeParser.serviceFor(7))
        // A service this app never opens (accelerometer) has no parser.
        assertNull(GarminRealtimeParser.serviceFor(16))
    }

    // The store.

    @Test
    fun `the store keeps the latest of each reading`() {
        val store = GarminRealtimeStore()
        val now = Instant.parse("2026-08-12T10:00:00Z")

        store.record(GarminRealtimeReading.HeartRate(70, 55), now)
        store.record(GarminRealtimeReading.Steps(1200, 8000), now)
        store.record(GarminRealtimeReading.HeartRate(74, 55), now)

        assertEquals(74, store.readings.value.heartRateBpm)
        assertEquals(1200, store.readings.value.steps)
        assertEquals(74, store.readings.value.freshHeartRate(now))
    }

    @Test
    fun `a stale reading stops being live`() {
        val store = GarminRealtimeStore()
        val at = Instant.parse("2026-08-12T10:00:00Z")
        store.record(GarminRealtimeReading.HeartRate(70, null), at)

        // A quiet minute is normal; several minutes means the reading is stale.
        assertEquals(70, store.readings.value.freshHeartRate(at.plusSeconds(60)))
        assertNull(store.readings.value.freshHeartRate(at.plusSeconds(600)))
    }

    @Test
    fun `clearing drops everything when the link goes`() {
        val store = GarminRealtimeStore()
        store.record(GarminRealtimeReading.HeartRate(70, null))

        store.clear()

        // A live value that outlives its link is a lie.
        assertNull(store.readings.value.heartRateBpm)
    }
}
