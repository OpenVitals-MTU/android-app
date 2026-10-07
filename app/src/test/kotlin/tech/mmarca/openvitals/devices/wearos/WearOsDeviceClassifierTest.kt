package tech.mmarca.openvitals.devices.wearos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

class WearOsDeviceClassifierTest {

    private val classifier = WearOsDeviceClassifier()
    private val appUuid = WearLinkProtocol.SERVICE_UUID.toString().lowercase()

    @Test
    fun `a bond listing the OpenVitals Wear OS app is a watch, whatever its name`() {
        val verdict = classifier.classify(
            discovered("sdk_gwear_x86_64", classicServiceUuids = setOf(appUuid)),
        )
        assertEquals(DeviceIntegration.WEAROS, verdict?.integration)
        assertEquals(BleDeviceKind.WATCH, verdict?.kind)
    }

    @Test
    fun `an unknown name without the app is not claimed, so a Garmin or Fitbit stays out`() {
        assertNull(classifier.classify(discovered("Mystery 42")))
    }

    @Test
    fun `a known smartwatch name is still a watch without bond evidence`() {
        assertEquals(DeviceIntegration.WEAROS, classifier.classify(discovered("Pixel Watch 3"))?.integration)
    }

    @Test
    fun `an unknown name with unrelated services is not claimed`() {
        val verdict = classifier.classify(
            discovered(
                "sdk_gphone16k_x86_64",
                classicServiceUuids = setOf("0000110a-0000-1000-8000-00805f9b34fb"),
            ),
        )
        assertNull(verdict)
    }

    private fun discovered(
        name: String?,
        classicServiceUuids: Set<String> = emptySet(),
    ) = BleDiscoveredDevice(
        address = "AA:BB:CC:DD:EE:FF",
        name = name,
        rssi = null,
        suggestedCapabilities = emptySet(),
        classicServiceUuids = classicServiceUuids,
    )
}
