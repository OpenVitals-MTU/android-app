package tech.mmarca.openvitals.devices.wearos

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.wearos.WearOsDeviceNames.isSmartwatchName

class WearOsDeviceNamesTest {

    @Test
    fun `matches the Galaxy Watch, the test rig`() {
        assertTrue(isSmartwatchName("Galaxy Watch8 (89FZ)"))
        assertTrue(isSmartwatchName("Galaxy Watch4 Classic"))
    }

    @Test
    fun `matches other wrist smartwatch families`() {
        assertTrue(isSmartwatchName("Pixel Watch 2"))
        assertTrue(isSmartwatchName("TicWatch Pro 5"))
        assertTrue(isSmartwatchName("Amazfit GTR"))
    }

    @Test
    fun `a name that merely says watch is not an onboardable watch`() {
        // This list is the Watches screen's allow-list; a generic fragment would offer any device as a watch.
        assertFalse(isSmartwatchName("My Wear OS device"))
        assertFalse(isSmartwatchName("Watch Charger"))
        assertFalse(isSmartwatchName("Fancy Watch"))
    }

    @Test
    fun `does not match live sensors`() {
        assertFalse(isSmartwatchName("TICKR"))
        assertFalse(isSmartwatchName("Polar H10"))
        assertFalse(isSmartwatchName("Wahoo CADENCE"))
        assertFalse(isSmartwatchName("KICKR BIKE"))
    }

    @Test
    fun `is null- and blank-safe`() {
        assertFalse(isSmartwatchName(null))
        assertFalse(isSmartwatchName(""))
        assertFalse(isSmartwatchName("   "))
    }
}
