package tech.mmarca.openvitals.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearLinkProtocolTest {

    @Test
    fun `a heart rate request round-trips and clamps its limit`() {
        val line = WearLinkProtocol.formatHeartRateRequest(1_700_000_000_000L, 99_999)

        assertEquals(
            WearLinkProtocol.HeartRateRequest(1_700_000_000_000L, WearLinkProtocol.MAX_SAMPLES_PER_REQUEST),
            WearLinkProtocol.parseHeartRateRequest(line),
        )
    }

    @Test
    fun `a sample and the end line round-trip`() {
        assertEquals(
            WearLinkProtocol.HeartRateSample(1_700_000_000_000L, 64),
            WearLinkProtocol.parseSample(WearLinkProtocol.formatSample(1_700_000_000_000L, 64)),
        )
        assertEquals(WearLinkProtocol.End(12, true), WearLinkProtocol.parseEnd(WearLinkProtocol.formatEnd(12, true)))
        assertEquals(WearLinkProtocol.End(0, false), WearLinkProtocol.parseEnd("END 0 0"))
    }

    @Test
    fun `malformed lines parse to null instead of throwing`() {
        assertNull(WearLinkProtocol.parseSample("HR 123"))
        assertNull(WearLinkProtocol.parseSample("HR abc 70"))
        assertNull(WearLinkProtocol.parseSample("HR 1700000000000 0"))
        assertNull(WearLinkProtocol.parseEnd("END 3 maybe"))
        assertNull(WearLinkProtocol.parseHeartRateRequest("HR_SINCE -1 10"))
        assertNull(WearLinkProtocol.parseHeartRateRequest("PING"))
    }

    @Test
    fun `a sleep minutes request round-trips and clamps its limit`() {
        val line = WearLinkProtocol.formatSleepMinutesRequest(1_700_000_000_000L, 99_999)

        assertEquals(
            WearLinkProtocol.SleepMinutesRequest(1_700_000_000_000L, WearLinkProtocol.MAX_MINUTES_PER_REQUEST),
            WearLinkProtocol.parseSleepMinutesRequest(line),
        )
        assertNull(WearLinkProtocol.parseHeartRateRequest(line))
    }

    private fun minute(bpm: Int?, kind: WearLinkProtocol.MinuteKind = WearLinkProtocol.MinuteKind.RAW) =
        WearLinkProtocol.SleepMinute(
            epochMillis = 1_700_000_000_000L,
            kind = kind,
            offsetSeconds = 7200,
            flags = 16,
            sampleCount = 300,
            movement10 = 42,
            bpm = bpm,
            heartRateSd10 = if (bpm == null) null else 15,
            heartRateSamples = if (bpm == null) 0 else 6,
            meanMilliG = intArrayOf(-12, 3, 998),
            sdMilliG = intArrayOf(3, 4, 5),
            zAngleMin = 86,
            zAngleMax = 90,
            zAngleDelta10 = 7,
        )

    @Test
    fun `a sleep minute round-trips, with and without a heart rate`() {
        val raw = minute(58)
        val unworn = minute(null, WearLinkProtocol.MinuteKind.UNMEASURABLE)

        assertEquals("SM 1700000000000 R 7200 16 300 42 58 15 6 -12 3 998 3 4 5 86 90 7", WearLinkProtocol.formatSleepMinute(raw))
        assertEquals(raw, WearLinkProtocol.parseSleepMinute(WearLinkProtocol.formatSleepMinute(raw)))
        assertEquals("SM 1700000000000 U 7200 16 300 42 - - 0 -12 3 998 3 4 5 86 90 7", WearLinkProtocol.formatSleepMinute(unworn))
        assertEquals(unworn, WearLinkProtocol.parseSleepMinute(WearLinkProtocol.formatSleepMinute(unworn)))
        assertEquals(4.2f, raw.movement)
        assertEquals(1.5f, raw.heartRateSd)
        assertEquals(0.7f, raw.zAngleDelta)
    }

    @Test
    fun `a sleep minute with an implausible rate keeps the minute and drops the rate`() {
        val parsed = WearLinkProtocol.parseSleepMinute("SM 1700000000000 A 7200 20 300 100 300 15 6 0 0 1000 3 3 3 - - -")

        assertEquals(WearLinkProtocol.MinuteKind.AWAKE, parsed?.kind)
        assertNull(parsed?.bpm)
        assertNull(parsed?.zAngleMin)
        assertTrue(parsed!!.hasFlag(WearLinkProtocol.FLAG_SCREEN_ON))
    }

    @Test
    fun `malformed sleep minutes parse to null`() {
        assertNull(WearLinkProtocol.parseSleepMinute("SM 1700000000000 R 7200 16 300 42 58 15 6 -12 3 998 3 4 5 86 90"))
        assertNull(WearLinkProtocol.parseSleepMinute("SM 1700000000000 X 7200 16 300 42 58 15 6 -12 3 998 3 4 5 86 90 7"))
        assertNull(WearLinkProtocol.parseSleepMinute("SM 1700000000000 R 7200 16 300 -1 58 15 6 -12 3 998 3 4 5 86 90 7"))
        assertNull(WearLinkProtocol.parseSleepMinute("SM 1700000000000 R 99999 16 300 42 58 15 6 -12 3 998 3 4 5 86 90 7"))
        assertNull(WearLinkProtocol.parseSleepMinute("SM 1700000000000 R 7200 16 300 42 58 1.5 6 -12 3 998 3 4 5 86 90 7"))
        assertNull(WearLinkProtocol.parseSleepMinute("HR 1700000000000 58"))
    }

    @Test
    fun `ping tolerates surrounding whitespace`() {
        assertTrue(WearLinkProtocol.isPing(" PING\r"))
    }
}
