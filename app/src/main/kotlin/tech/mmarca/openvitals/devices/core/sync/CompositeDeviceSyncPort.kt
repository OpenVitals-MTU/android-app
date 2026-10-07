package tech.mmarca.openvitals.devices.core.sync

import kotlin.time.Duration
import tech.mmarca.openvitals.domain.model.BleSensorDevice

/**
 * The one [DeviceSyncPort] the sync controller sees, over every
 * integration's own. A device belongs to the first port that claims it; a
 * device no port claims cannot sync. Order is the binding order, which is
 * irrelevant as long as no two integrations claim the same device.
 */
class CompositeDeviceSyncPort(private val ports: Set<DeviceSyncPort>) : DeviceSyncPort {

    override fun canSync(device: BleSensorDevice): Boolean = owner(device) != null

    override suspend fun sync(
        device: BleSensorDevice,
        listenAfter: Duration,
        onProgress: ((DeviceSyncProgress) -> Unit)?,
    ): DeviceSyncResult {
        val owner = owner(device)
            ?: return DeviceSyncResult.Failed("No integration syncs this device.")
        return owner.sync(device, listenAfter, onProgress)
    }

    private fun owner(device: BleSensorDevice): DeviceSyncPort? = ports.firstOrNull { it.canSync(device) }
}
