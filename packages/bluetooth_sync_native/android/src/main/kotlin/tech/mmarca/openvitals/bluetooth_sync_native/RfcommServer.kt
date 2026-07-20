package tech.mmarca.openvitals.bluetooth_sync_native

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.IOException

/**
 * RFCOMM server. [listen] opens the server socket (publishing an SDP record for
 * [SyncBluetooth.APP_UUID]) so a peer can find it; [accept] then blocks for ONE
 * inbound connection. Split so the plugin can report "listening" to Dart the
 * instant the socket is open, before the blocking accept.
 *
 * [cancel] closes the server socket, which unblocks a waiting [accept] (it
 * throws, and [accept] returns null).
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

    /**
     * Blocks until a peer connects (returns its socket) or the server is
     * cancelled/errors (returns null). Closes the listening socket once a peer
     * is accepted — one connection per session.
     */
    fun accept(): BluetoothSocket? {
        val server = serverSocket ?: return null
        return try {
            val socket = server.accept()
            try {
                server.close()
            } catch (_: IOException) {
                // best effort
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
            // best effort
        }
        serverSocket = null
    }
}
