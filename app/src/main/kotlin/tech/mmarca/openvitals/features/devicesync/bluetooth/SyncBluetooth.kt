package tech.mmarca.openvitals.features.devicesync.bluetooth

import java.util.UUID

/**
 * Shared RFCOMM constants for the OpenVitals phone-to-phone sync channel.
 */
internal object SyncBluetooth {
    /** SDP service name published by the server socket. */
    const val SERVICE_NAME: String = "OpenVitalsSync"

    /** The app's own RFCOMM UUID, not the SPP one, so OpenVitals only pairs with OpenVitals. */
    val APP_UUID: UUID = UUID.fromString("a6f1e7c2-9b3d-4e58-8f21-7c9d4b2a1e60")

    const val TAG: String = "BluetoothSync"
}
