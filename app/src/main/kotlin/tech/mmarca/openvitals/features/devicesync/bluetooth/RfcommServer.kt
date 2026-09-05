package tech.mmarca.openvitals.features.devicesync.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.IOException

/**
 * RFCOMM server. [listen] opens the socket, [accept] blocks for one
 * connection, [cancel] closes the socket and unblocks [accept].
 */
internal class RfcommServer(private val adapter: BluetoothAdapter) {
    private var serverSocket: BluetoothServerSocket? = null

    /** Opens the listening server socket. Throws if Bluetooth is off/denied. */
    @Throws(IOException::class, SecurityException::class)
    fun listen() {
        serverSocket =
            adapter.listenUsingRfcommWithServiceRecord(
                SyncBluetooth.SERVICE_NAME,
                SyncBluetooth.APP_UUID,
            )
    }

    /** Blocks until a peer connects, or null when cancelled. One connection per session. */
    fun accept(): BluetoothSocket? {
        val server = serverSocket ?: return null
        return try {
            val socket = server.accept()
            try {
                server.close()
            } catch (_: IOException) {
                // Best effort.
            }
            socket
        } catch (_: IOException) {
            // Cancelled via close(), or accept failed.
            null
        } finally {
            serverSocket = null
        }
    }

    /** Closes the server socket if still listening. Idempotent. */
    fun cancel() {
        try {
            serverSocket?.close()
        } catch (_: IOException) {
            // Best effort.
        }
        serverSocket = null
    }
}
