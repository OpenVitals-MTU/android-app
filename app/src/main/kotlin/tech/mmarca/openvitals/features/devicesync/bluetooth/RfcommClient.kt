package tech.mmarca.openvitals.features.devicesync.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import java.io.IOException

/** RFCOMM client on [SyncBluetooth.APP_UUID]. Connecting triggers the pairing dialog on a new peer. */
internal class RfcommClient(private val adapter: BluetoothAdapter) {
    /** Blocks until connected. Throws [IOException] when the peer is not listening. */
    @Throws(IOException::class, SecurityException::class)
    fun connect(address: String): BluetoothSocket {
        val device = adapter.getRemoteDevice(address)
        // An active scan starves the RFCOMM connect.
        if (adapter.isDiscovering) adapter.cancelDiscovery()
        val socket = device.createRfcommSocketToServiceRecord(SyncBluetooth.APP_UUID)
        socket.connect()
        return socket
    }
}
