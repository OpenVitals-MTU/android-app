package tech.mmarca.openvitals.wearlink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WearLinkProfileTest {

    private val profile = WearLinkProfile.Profile(720, 81_500, WearLinkProfile.Units.METRIC, 8_000, 5_000, 400)
    private val totals = WearLinkProfile.Totals(1_793_660_400_000L, 1_793_700_000_000L, 6_797, 4_890, null)

    @Test
    fun `a profile and today's totals survive the wire`() {
        assertEquals("PROFILE 720 81500 M 8000 5000 400", WearLinkProfile.formatProfile(profile))
        assertEquals(profile, WearLinkProfile.parseProfile(WearLinkProfile.formatProfile(profile)))
        assertEquals("TOTALS 1793660400000 1793700000000 6797 4890 -", WearLinkProfile.formatTotals(totals))
        assertEquals(totals, WearLinkProfile.parseTotals(WearLinkProfile.formatTotals(totals)))
    }

    @Test
    fun `a profile without a weight is still a profile`() {
        val noWeight = profile.copy(weightGrams = null, units = WearLinkProfile.Units.IMPERIAL)

        assertEquals(noWeight, WearLinkProfile.parseProfile("PROFILE 720 - I 8000 5000 400"))
    }

    @Test
    fun `values out of range refuse the whole line`() {
        assertNull(WearLinkProfile.parseProfile("PROFILE 50 81500 M 8000 5000 400"))
        assertNull(WearLinkProfile.parseProfile("PROFILE 720 900 M 8000 5000 400"))
        assertNull(WearLinkProfile.parseProfile("PROFILE 720 81500 X 8000 5000 400"))
        assertNull(WearLinkProfile.parseProfile("PROFILE 720 81500 M 0 5000 400"))
        assertNull(WearLinkProfile.parseProfile("PROFILE 720 81500 M 8000 5000"))
        assertNull(WearLinkProfile.parseTotals("TOTALS 1793700000000 1793660400000 1 2 3"))
        assertNull(WearLinkProfile.parseTotals("TOTALS 1793660400000 1793700000000 -1 2 3"))
        assertNull(WearLinkProfile.parseTotals("TOTALS 1793660400000 1793700000000 1 2"))
    }

    @Test
    fun `the requests parse as requests, so an older watch refuses them as unknown`() {
        assertEquals(WearLinkProtocol.Request.Profile(profile), WearLinkProtocol.parseRequest(WearLinkProfile.formatProfile(profile)))
        assertEquals(WearLinkProtocol.Request.Totals(totals), WearLinkProtocol.parseRequest(WearLinkProfile.formatTotals(totals)))
    }
}
