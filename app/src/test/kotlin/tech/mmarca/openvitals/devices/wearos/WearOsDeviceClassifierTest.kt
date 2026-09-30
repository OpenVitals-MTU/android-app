package tech.mmarca.openvitals.devices.wearos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

class WearOsDeviceClassifierTest {

    private val classifier = WearOsDeviceClassifier()
    private val appUuid = BluetoothWearOsNodePort.OPENVITALS_WEAR_APP_UUID.toString().lowercase()

    @Test
    fun `a bond listing the OpenVitals Wear OS app is a watch, whatever its name`() {
        val verdict = classifier.classify(
            discovered("sdk_gwear_x86_64", classicServiceUuids = setOf(appUuid)),
        )
        assertEquals(DeviceIntegration.WEAROS, verdict?.integration)
        assertEquals(BleDeviceKind.WATCH, verdict?.kind)
    }

    @Test
    fun `a bond whose class is a wrist watch is a watch, whatever its name`() {
        val verdict = classifier.classify(discovered("Mystery 42", isWristWatchClass = true))
        assertEquals(DeviceIntegration.WEAROS, verdict?.integration)
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
        isWristWatchClass: Boolean = false,
    ) = BleDiscoveredDevice(
        address = "AA:BB:CC:DD:EE:FF",
        name = name,
        rssi = null,
        suggestedCapabilities = emptySet(),
        classicServiceUuids = classicServiceUuids,
        isWristWatchClass = isWristWatchClass,
    )
}
