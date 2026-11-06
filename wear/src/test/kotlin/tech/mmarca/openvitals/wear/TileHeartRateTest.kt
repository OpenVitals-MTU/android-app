package tech.mmarca.openvitals.wear

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.wearlink.WearLinkProtocol.HeartRateSample

class TileHeartRateTest {

    private val now = 1_793_700_000_000L
    private val minute = TileDay.MINUTE_MILLIS

    @Test
    fun `without the grant there is no heart rate, whatever the store holds`() {
        assertEquals(TileHeartRate.NotGranted, TileHeartRate.of(false, HeartRateSample(now, 70), now))
    }

    @Test
    fun `an empty store has none`() {
        assertEquals(TileHeartRate.None, TileHeartRate.of(true, null, now))
    }

    @Test
    fun `a recent sample carries its age in whole minutes`() {
        assertEquals(TileHeartRate.Recent(72, 0), TileHeartRate.of(true, HeartRateSample(now - 59_000, 72), now))
        assertEquals(TileHeartRate.Recent(72, 12), TileHeartRate.of(true, HeartRateSample(now - 12 * minute, 72), now))
    }

    @Test
    fun `a sample an hour old still shows, an older one does not`() {
        val hour = TileHeartRate.STALE_AFTER_MILLIS

        assertEquals(TileHeartRate.Recent(64, 60), TileHeartRate.of(true, HeartRateSample(now - hour, 64), now))
        assertEquals(TileHeartRate.None, TileHeartRate.of(true, HeartRateSample(now - hour - 1, 64), now))
    }

    @Test
    fun `a sample stamped after now counts as just taken`() {
        assertEquals(TileHeartRate.Recent(80, 0), TileHeartRate.of(true, HeartRateSample(now + 5_000, 80), now))
    }
}
