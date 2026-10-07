package tech.mmarca.openvitals.devices.core.pairing

/**
 * Told when an associated companion device comes into range. Android
 * reports that to one service per app only, the primary one, so
 * [OpenVitalsCompanionDeviceService] takes it and passes it on to every
 * observer bound in `di/DevicesModule.kt`. A port: the service names no
 * integration.
 */
fun interface CompanionPresenceObserver {

    /** [address] is any associated device's, in either case. Ignore the ones that are not yours. */
    fun onCompanionDeviceAppeared(address: String)
}
