package tech.mmarca.openvitals.data.repository

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice

class BleDeviceRepositoryTest {
    @Test
    fun resolveAssignments_usesFirstEnabledDevicePerCapability() {
        val devices = listOf(
            BleSensorDevice(
                id = "hr",
                displayName = "HR",
                address = "AA:BB:CC:DD:EE:01",
                bluetoothName = null,
                capabilities = setOf(BleSensorCapability.HEART_RATE),
                enabled = true,
                wheelCircumferenceMm = null,
                addedAt = Instant.EPOCH,
            ),
            BleSensorDevice(
                id = "power",
                displayName = "Power",
                address = "AA:BB:CC:DD:EE:02",
                bluetoothName = null,
                capabilities = setOf(BleSensorCapability.CYCLING_POWER),
                enabled = true,
                wheelCircumferenceMm = null,
                addedAt = Instant.EPOCH,
            ),
        )
        val assignments = resolveCapabilityAssignmentsForTest(devices)
        assertEquals("hr", assignments[BleSensorCapability.HEART_RATE]?.id)
        assertEquals("power", assignments[BleSensorCapability.CYCLING_POWER]?.id)
    }

    @Test
    fun disabledDevicesAreExcludedFromAssignments() {
        val devices = listOf(
            BleSensorDevice(
                id = "hr",
                displayName = "HR",
                address = "AA:BB:CC:DD:EE:01",
                bluetoothName = null,
                capabilities = setOf(BleSensorCapability.HEART_RATE),
                enabled = false,
                wheelCircumferenceMm = null,
                addedAt = Instant.EPOCH,
            ),
        )
        assertEquals(0, resolveCapabilityAssignmentsForTest(devices).size)
    }
}

private fun resolveCapabilityAssignmentsForTest(
    devices: List<BleSensorDevice>,
): Map<BleSensorCapability, BleSensorDevice> {
    val assignments = linkedMapOf<BleSensorCapability, BleSensorDevice>()
    devices.filter { it.enabled }.forEach { device ->
        device.capabilities.forEach { capability ->
            assignments.putIfAbsent(capability, device)
        }
    }
    return assignments
}
