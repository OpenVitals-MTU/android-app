package tech.mmarca.openvitals.devices.garmin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.garmin.GarminDeviceNames.isGarminBikeComputerName
import tech.mmarca.openvitals.devices.garmin.GarminDeviceNames.isGarminSyncDeviceName
import tech.mmarca.openvitals.devices.garmin.GarminDeviceNames.isGarminWatchName

class GarminDeviceNamesTest {

    @Test
    fun `matches the accented names the watches actually advertise`() {
        // The exact string a vívoactive 5 advertises.
        assertTrue(isGarminSyncDeviceName("vívoactive 5"))
        assertTrue(isGarminSyncDeviceName("fēnix 7X Pro"))
    }

    @Test
    fun `matches the unaccented spellings some firmware uses`() {
        assertTrue(isGarminSyncDeviceName("vivoactive 5"))
        assertTrue(isGarminSyncDeviceName("fenix 6S Pro Solar"))
    }

    @Test
    fun `matches by family so an unreleased model still onboards`() {
        // The point of family matching over Gadgetbridge's exact-match table.
        assertTrue(isGarminSyncDeviceName("vívoactive 9"))
        assertTrue(isGarminSyncDeviceName("Forerunner 1055"))
    }

    @Test
    fun `strips the Garmin prefix some models advertise with`() {
        assertTrue(isGarminSyncDeviceName("Garmin Forerunner 265S"))
    }

    @Test
    fun `does NOT match HRM chest straps`() {
        // These expose the standard Heart Rate service; classifying one as a watch would break heart rate recording.
        assertFalse(isGarminSyncDeviceName("HRM 200"))
        assertFalse(isGarminSyncDeviceName("HRMPro+:123456"))
        assertFalse(isGarminSyncDeviceName("HRM600:998877"))
    }

    @Test
    fun `does not match other vendors, blanks or null`() {
        assertFalse(isGarminSyncDeviceName("Wahoo TICKR"))
        assertFalse(isGarminSyncDeviceName("Polar H10"))
        assertFalse(isGarminSyncDeviceName("LE_WH-1000XM4"))
        assertFalse(isGarminSyncDeviceName("   "))
        assertFalse(isGarminSyncDeviceName(null))
    }

    @Test
    fun `also matches Edge bike computers, they sync FIT files too`() {
        assertTrue(isGarminSyncDeviceName("Edge 840"))
        assertTrue(isGarminSyncDeviceName("Garmin Edge 1040"))
    }

    @Test
    fun `watch name matches the watch families, not the Edge`() {
        assertTrue(isGarminWatchName("vívoactive 5"))
        assertTrue(isGarminWatchName("fēnix 7X Pro"))
        assertTrue(isGarminWatchName("Garmin Forerunner 265S"))
        assertFalse(isGarminWatchName("Edge 840"))
    }

    @Test
    fun `watch name does not match straps, other vendors, blanks or null`() {
        assertFalse(isGarminWatchName("HRM 200"))
        assertFalse(isGarminWatchName("Wahoo TICKR"))
        assertFalse(isGarminWatchName("   "))
        assertFalse(isGarminWatchName(null))
    }

    @Test
    fun `bike computer name matches the Edge family, including sub-models and the prefix`() {
        assertTrue(isGarminBikeComputerName("Edge 840"))
        assertTrue(isGarminBikeComputerName("Edge Explore 2"))
        assertTrue(isGarminBikeComputerName("Edge MTB"))
        assertTrue(isGarminBikeComputerName("Garmin Edge 1040"))
    }

    @Test
    fun `bike computer name is disjoint from the watch families`() {
        assertFalse(isGarminBikeComputerName("vívoactive 5"))
        assertFalse(isGarminBikeComputerName("fēnix 7X Pro"))
        assertFalse(isGarminBikeComputerName("Forerunner 265S"))
    }

    @Test
    fun `bike computer name does not match straps, other vendors, blanks or null`() {
        assertFalse(isGarminBikeComputerName("HRM 200"))
        assertFalse(isGarminBikeComputerName("Wahoo TICKR"))
        assertFalse(isGarminBikeComputerName("   "))
        assertFalse(isGarminBikeComputerName(null))
    }
}
