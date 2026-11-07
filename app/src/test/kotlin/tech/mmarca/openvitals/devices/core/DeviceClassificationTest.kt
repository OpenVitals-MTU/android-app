package tech.mmarca.openvitals.devices.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.devices.garmin.GarminDeviceClassifier
import tech.mmarca.openvitals.devices.wearos.WearOsDeviceClassifier
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

class DeviceClassificationTest {

    private val classifiers = listOf(GarminDeviceClassifier(), WearOsDeviceClassifier())

    @Test
    fun `a Garmin watch family classifies as a Garmin watch`() {
        val verdict = classifyDevice(discovered("vívoactive 5"), classifiers)
        assertEquals(DeviceIntegration.GARMIN, verdict.integration)
        assertEquals(BleDeviceKind.WATCH, verdict.kind)
    }

    @Test
    fun `an Edge classifies as a Garmin bike computer, not a watch`() {
        val verdict = classifyDevice(discovered("Edge 840"), classifiers)
        assertEquals(DeviceIntegration.GARMIN, verdict.integration)
        assertEquals(BleDeviceKind.BIKE_COMPUTER, verdict.kind)
    }

    @Test
    fun `a prefixed Edge name → (garmin, bikeComputer)`() {
        val verdict = classifyDevice(discovered("Garmin Edge 1040"), classifiers)
        assertEquals(DeviceIntegration.GARMIN, verdict.integration)
        assertEquals(BleDeviceKind.BIKE_COMPUTER, verdict.kind)
    }

    @Test
    fun `the member service alone does NOT make an unknown name a watch`() {
        // 0xFE1F surfaces a device in the scan, but the name decides the kind.
        val verdict = classifyDevice(
            discovered(name = "anything", advertisesSyncService = true),
            classifiers,
        )
        assertNull(verdict.integration)
        assertEquals(BleDeviceKind.SENSOR, verdict.kind)
    }

    @Test
    fun `a member-service-only advert (no distinguishing name) → sensor`() {
        // Without a recognised name it falls through to a plain live sensor.
        val verdict = classifyDevice(discovered(advertisesSyncService = true), classifiers)
        assertNull(verdict.integration)
        assertEquals(BleDeviceKind.SENSOR, verdict.kind)
    }

    @Test
    fun `the NAME decides, so a WearOS name is WearOS even with 0xFE1F`() {
        // A device advertising the Garmin member service but named like a WearOS watch is WearOS.
        val verdict = classifyDevice(
            discovered(name = "Galaxy Watch", advertisesSyncService = true),
            classifiers,
        )
        assertEquals(DeviceIntegration.WEAROS, verdict.integration)
    }

    @Test
    fun `an unnamed, unremarkable device → sensor`() {
        assertNull(classifyDevice(discovered(), classifiers).integration)
    }

    @Test
    fun `a wearos-style smartwatch classifies as a wearos watch`() {
        val verdict = classifyDevice(discovered("Galaxy Watch8 (89FZ)"), classifiers)
        assertEquals(DeviceIntegration.WEAROS, verdict.integration)
        assertEquals(BleDeviceKind.WATCH, verdict.kind)
    }

    @Test
    fun `anything unclaimed falls through to a plain sensor`() {
        val verdict = classifyDevice(discovered("Polar H10"), classifiers)
        assertEquals(DeviceClassification.SENSOR, verdict)
        assertNull(verdict.integration)
        assertEquals(BleDeviceKind.SENSOR, verdict.kind)
    }

    @Test
    fun `an HRM strap stays a sensor even though it is Garmin`() {
        val verdict = classifyDevice(discovered("HRM 200"), classifiers)
        assertEquals(DeviceClassification.SENSOR, verdict)
    }

    @Test
    fun `order matters, Garmin's verdict beats the generic watch name match`() {
        // A name that could match both classifiers: the first in the list wins.
        val verdict = classifyDevice(discovered("Garmin Forerunner 265S"), classifiers)
        assertEquals(DeviceIntegration.GARMIN, verdict.integration)
    }

    private fun discovered(
        name: String? = null,
        advertisesSyncService: Boolean = false,
    ): BleDiscoveredDevice = BleDiscoveredDevice(
        address = "AA:BB:CC:DD:EE:01",
        name = name,
        rssi = -60,
        suggestedCapabilities = emptySet(),
        advertisesSyncService = advertisesSyncService,
    )
}
