package tech.mmarca.openvitals.devices.core.sync

import java.time.Instant
import kotlin.time.Duration
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

class CompositeDeviceSyncPortTest {

    private class PortFor(private val claims: (BleSensorDevice) -> Boolean, val name: String) : DeviceSyncPort {
        var syncs = 0
        override fun canSync(device: BleSensorDevice) = claims(device)
        override suspend fun sync(
            device: BleSensorDevice,
            listenAfter: Duration,
            onProgress: ((DeviceSyncProgress) -> Unit)?,
        ): DeviceSyncResult {
            syncs++
            return DeviceSyncResult.Failed(name)
        }
    }

    private val garmin = PortFor({ it.integration == DeviceIntegration.GARMIN }, "garmin")
    // A watch stored before the integration field existed: a legacy Garmin.
    private val legacy = PortFor({ it.isWatch && it.integration == null }, "legacy")
    private val composite = CompositeDeviceSyncPort(setOf(garmin, legacy))

    private fun watch(integration: DeviceIntegration?) = BleSensorDevice(
        id = "w",
        displayName = "w",
        address = "AA",
        bluetoothName = "w",
        capabilities = emptySet(),
        enabled = true,
        wheelCircumferenceMm = null,
        addedAt = Instant.parse("2026-01-01T00:00:00Z"),
        kind = BleDeviceKind.WATCH,
        integration = integration,
    )

    @Test
    fun `each device goes to the port that claims it`() = runTest {
        assertEquals(DeviceSyncResult.Failed("legacy"), composite.sync(watch(null), Duration.ZERO, null))
        assertEquals(DeviceSyncResult.Failed("garmin"), composite.sync(watch(DeviceIntegration.GARMIN), Duration.ZERO, null))
        assertEquals(1, garmin.syncs)
        assertEquals(1, legacy.syncs)
    }

    @Test
    fun `a device no port claims cannot sync`() = runTest {
        val sensor = watch(null).copy(kind = BleDeviceKind.SENSOR)

        assertFalse(composite.canSync(sensor))
        assertTrue(composite.sync(sensor, Duration.ZERO, null) is DeviceSyncResult.Failed)
        assertEquals(0, garmin.syncs + legacy.syncs)
    }
}
