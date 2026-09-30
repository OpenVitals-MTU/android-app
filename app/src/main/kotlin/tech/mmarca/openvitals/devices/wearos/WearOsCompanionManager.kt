package tech.mmarca.openvitals.devices.wearos

import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level manager to query whether a Wear OS watch is paired and whether
 * the OpenVitals Wear OS app on it answers.
 */
@Singleton
class WearOsCompanionManager @Inject constructor(
    private val nodePort: WearOsNodePort,
) {
    /** Throws [SecurityException] when the Bluetooth permission is missing. */
    suspend fun checkWearOsWatchStatus(
        targetAddress: String? = null,
        targetName: String? = null,
    ): WearOsCompanionStatus = nodePort.checkStatus(targetAddress, targetName)
}
