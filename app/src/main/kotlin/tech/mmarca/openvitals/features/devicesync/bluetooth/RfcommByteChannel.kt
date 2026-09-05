package tech.mmarca.openvitals.features.devicesync.bluetooth

import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread

/**
 * Wraps one open RFCOMM socket: a reader thread pumps bytes to [onBytes],
 * [write] blocks until flushed. [onClosed] fires once when the socket ends
 * on its own, not on [close].
 */
internal class RfcommByteChannel(
    private val socket: BluetoothSocket,
    private val onBytes: (ByteArray) -> Unit,
    private val onClosed: () -> Unit,
) {
    private val output: OutputStream = socket.outputStream
    private val input: InputStream = socket.inputStream

    @Volatile private var closed = false
    private var reader: Thread? = null

    /** Starts the reader thread. Call once, after construction. */
    fun start() {
        reader = thread(name = "rfcomm-reader", isDaemon = true) {
            val buffer = ByteArray(8 * 1024)
            try {
                while (!closed) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    if (n > 0) onBytes(buffer.copyOf(n))
                }
            } catch (_: IOException) {
                // Socket closed or link dropped.
            } finally {
                closeInternal(notify = true)
            }
        }
    }

    /** Writes all of [chunk], blocking until flushed. Throws on a dead socket. */
    @Throws(IOException::class)
    fun write(chunk: ByteArray) {
        if (closed) throw IOException("channel closed")
        output.write(chunk)
        output.flush()
    }

    /** Explicitly closes the socket. Does NOT fire [onClosed]. Idempotent. */
    fun close() = closeInternal(notify = false)

    private fun closeInternal(notify: Boolean) {
        synchronized(this) {
            if (closed) return
            closed = true
        }
        try {
            socket.close()
        } catch (_: IOException) {
            // Best effort.
        }
        if (notify) onClosed()
    }
}
