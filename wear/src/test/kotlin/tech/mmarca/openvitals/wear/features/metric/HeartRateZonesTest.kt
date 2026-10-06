package tech.mmarca.openvitals.wear.features.metric

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeartRateZonesTest {

    @Test
    fun zone_followsShareOfMaximum() {
        assertNull(heartRateZone(90.0, 200))
        assertEquals(0, heartRateZone(100.0, 200))
        assertEquals(2, heartRateZone(150.0, 200))
        assertEquals(4, heartRateZone(199.0, 200))
        assertEquals(4, heartRateZone(230.0, 200))
    }

    @Test
    fun shares_countSamplesPerZone_andIgnoreRestingOnes() {
        val shares = heartRateZoneShares(listOf(60.0, 105.0, 125.0, 125.0), 200)
        assertEquals(listOf(0.25f, 0.5f, 0f, 0f, 0f), shares)
    }

    @Test
    fun progress_runsFromZoneOneToMaximum() {
        assertEquals(0f, heartRateZoneProgress(80.0, 200), 0f)
        assertEquals(0.5f, heartRateZoneProgress(150.0, 200), 1e-6f)
        assertEquals(1f, heartRateZoneProgress(220.0, 200), 0f)
    }
}
