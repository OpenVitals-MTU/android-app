package tech.mmarca.openvitals.devices.wearos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The registry holds the BLE scan address; the bond sits under the Classic one. */
class WearOsBondMatcherTest {

    private val galaxy = BondedWatch("A8:D1:62:BE:3A:3B", "Galaxy Watch8 (3A3B)")
    private val pixel = BondedWatch("44:55:66:77:88:99", "Pixel Watch 3")
    private val headphones = BondedWatch("00:11:22:33:44:55", "WH-1000XM5")

    @Test
    fun `an exact address wins, whatever the case`() {
        val picked = WearOsBondMatcher.pick(
            bonded = listOf(pixel, galaxy),
            targetAddress = "a8:d1:62:be:3a:3b",
            targetName = "Pixel Watch 3",
        )

        assertEquals(galaxy, picked)
    }

    @Test
    fun `a private BLE address falls back to the name, without the LE suffix`() {
        val picked = WearOsBondMatcher.pick(
            bonded = listOf(pixel, galaxy, headphones),
            targetAddress = "7F:12:34:56:78:9A",
            targetName = "Galaxy Watch8 (3A3B) LE",
        )

        assertEquals(galaxy, picked)
    }

    @Test
    fun `with no match the only bonded smartwatch is taken`() {
        val picked = WearOsBondMatcher.pick(
            bonded = listOf(headphones, pixel),
            targetAddress = "7F:12:34:56:78:9A",
            targetName = "Something else",
        )

        assertEquals(pixel, picked)
    }

    @Test
    fun `several smartwatches and no match is no guess`() {
        val picked = WearOsBondMatcher.pick(
            bonded = listOf(galaxy, pixel),
            targetAddress = "7F:12:34:56:78:9A",
            targetName = null,
        )

        assertNull(picked)
    }

    @Test
    fun `no bonded smartwatch is no match`() {
        assertNull(WearOsBondMatcher.pick(listOf(headphones), "7F:12:34:56:78:9A", "Galaxy Watch8"))
    }
}
