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

    @Test
    fun `a sleep minute round-trips, with and without a heart rate`() {
        val raw = WearLinkProtocol.SleepMinute(1_700_000_000_000L, WearLinkProtocol.MinuteKind.RAW, 4.2f, 58, 7200)
        val unworn = WearLinkProtocol.SleepMinute(1_700_000_060_000L, WearLinkProtocol.MinuteKind.UNMEASURABLE, 0f, null, -3600)

        assertEquals("SM 1700000000000 R 4.2 58 7200", WearLinkProtocol.formatSleepMinute(raw))
        assertEquals(raw, WearLinkProtocol.parseSleepMinute(WearLinkProtocol.formatSleepMinute(raw)))
        assertEquals("SM 1700000060000 U 0.0 - -3600", WearLinkProtocol.formatSleepMinute(unworn))
        assertEquals(unworn, WearLinkProtocol.parseSleepMinute(WearLinkProtocol.formatSleepMinute(unworn)))
    }

    @Test
    fun `a sleep minute with an implausible rate keeps the minute and drops the rate`() {
        val parsed = WearLinkProtocol.parseSleepMinute("SM 1700000000000 A 10.0 300 0")

        assertEquals(WearLinkProtocol.MinuteKind.AWAKE, parsed?.kind)
        assertNull(parsed?.bpm)
    }

    @Test
    fun `malformed sleep minutes parse to null`() {
        assertNull(WearLinkProtocol.parseSleepMinute("SM 1700000000000 R 4.2 58"))
        assertNull(WearLinkProtocol.parseSleepMinute("SM 1700000000000 X 4.2 58 0"))
        assertNull(WearLinkProtocol.parseSleepMinute("SM 1700000000000 R -1 58 0"))
        assertNull(WearLinkProtocol.parseSleepMinute("SM 1700000000000 R 4.2 58 99999"))
        assertNull(WearLinkProtocol.parseSleepMinute("HR 1700000000000 58"))
    }

    @Test
    fun `ping tolerates surrounding whitespace`() {
        assertTrue(WearLinkProtocol.isPing(" PING\r"))
    }
}
