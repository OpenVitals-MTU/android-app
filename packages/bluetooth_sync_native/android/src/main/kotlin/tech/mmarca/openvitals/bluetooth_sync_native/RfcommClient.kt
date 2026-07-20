package tech.mmarca.openvitals.bluetooth_sync_native

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import java.io.IOException

/**
 * RFCOMM client. Connects to a remote device on [SyncBluetooth.APP_UUID].
 * `createRfcommSocketToServiceRecord` + `connect()` triggers the OS pairing
 * dialog on a first-time (unbonded) peer — the bond is what encrypts the link.
 */
internal class RfcommClient(private val adapter: BluetoothAdapter) {
    /**
     * Blocks until connected; returns the open socket. Throws [IOException] if
     * the peer is not listening / out of range / pairing declined.
     */
    @Throws(IOException::class, SecurityException::class)
    fun connect(address: String): BluetoothSocket {
        val device = adapter.getRemoteDevice(address)
        // Discovery must be off before connecting — an active scan starves the
        // RFCOMM connect and can fail it.
        if (adapter.isDiscovering) adapter.cancelDiscovery()
        val socket = device.createRfcommSocketToServiceRecord(SyncBluetooth.APP_UUID)
        socket.connect()
        return socket
    }
}
