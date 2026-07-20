package tech.mmarca.openvitals.bluetooth_sync_native

import java.util.UUID

/**
 * Shared RFCOMM constants for the OpenVitals phone-to-phone sync channel.
 */
internal object SyncBluetooth {
    /** SDP service name published by the server socket. */
    const val SERVICE_NAME: String = "OpenVitalsSync"

    /**
     * Fixed app RFCOMM UUID shared by both phones — the server advertises an SDP
     * record on it and the client connects on it. A private, app-specific UUID
     * (not the well-known SPP UUID `00001101-…`) so OpenVitals only ever pairs
     * with OpenVitals and never collides with a generic serial-port service.
     */
    val APP_UUID: UUID = UUID.fromString("a6f1e7c2-9b3d-4e58-8f21-7c9d4b2a1e60")

    const val TAG: String = "BluetoothSync"
}
