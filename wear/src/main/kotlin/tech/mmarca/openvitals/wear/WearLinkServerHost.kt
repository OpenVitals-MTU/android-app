package tech.mmarca.openvitals.wear

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import tech.mmarca.openvitals.wearlink.Page
import tech.mmarca.openvitals.wearlink.WearLinkConnection
import tech.mmarca.openvitals.wearlink.WearLinkProtocol
import tech.mmarca.openvitals.wearlink.WearLinkRequestHandler
import tech.mmarca.openvitals.wearlink.WearLinkServer

/**
 * Hosts the RFCOMM listener and runs `WearLinkServer` for every phone that
 * connects, one thread per connection. All the protocol lives in
 * `:wearlink`; this class is the Bluetooth around it and the hands the
 * store to its request handler.
 */
class WearLinkServerHost(
    private val adapter: () -> BluetoothAdapter?,
    trust: WearTrustStore,
    store: MetricStore,
    private val localName: () -> String,
    private val state: WearLinkState = WearLinkState,
) {
    private val server = WearLinkServer(trust, Handler(store, localName))

    @Volatile
    private var serverSocket: BluetoothServerSocket? = null
    private val isListening = AtomicBoolean(false)

    /** Bumped per listener start, so a thread that is shutting down cannot stop its successor. */
    private val generation = AtomicInteger(0)

    fun start() {
        if (isListening.getAndSet(true)) return
        val mine = generation.incrementAndGet()
        Thread({ listen(mine) }, "WearLinkServerHost").start()
    }

    fun stop() {
        isListening.set(false)
        // Unblocks accept(); the listener thread then exits.
        runCatching { serverSocket?.close() }
        state.update { it.copy(listening = false) }
    }

    private fun listen(mine: Int) {
        var socket: BluetoothServerSocket? = null
        try {
            val adapter = adapter()
            if (adapter == null || !adapter.isEnabled) {
                // The state receiver starts it again once Bluetooth is on.
                Log.w(TAG, "Bluetooth unavailable or disabled")
                return
            }
            val server = try {
                adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, WearLinkProtocol.SERVICE_UUID)
            } catch (e: SecurityException) {
                Log.e(TAG, "Missing Bluetooth permission for RFCOMM server", e)
                return
            }
            socket = server
            serverSocket = server
            state.update { it.copy(listening = true) }
            Log.i(TAG, "Listening on ${WearLinkProtocol.SERVICE_UUID}")
            while (isListening.get() && generation.get() == mine) {
                val client = try {
                    server.accept()
                } catch (e: IOException) {
                    if (isListening.get()) Log.e(TAG, "Accept failed", e)
                    break
                }
                serveOnThread(client)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Cannot start the listener", e)
        } finally {
            runCatching { socket?.close() }
            if (serverSocket === socket) serverSocket = null
            if (generation.get() == mine) {
                isListening.set(false)
                state.update { it.copy(listening = false) }
            }
        }
    }

    private fun serveOnThread(socket: BluetoothSocket) {
        Thread({
            val address = runCatching { socket.remoteDevice.address }.getOrNull() ?: "unknown"
            val connection = try {
                WearLinkConnection(socket.inputStream, socket.outputStream, address) { socket.close() }
            } catch (e: IOException) {
                Log.w(TAG, "No streams for $address: ${e.message}")
                runCatching { socket.close() }
                return@Thread
            }
            state.update { it.copy(activeClients = server.activeClients + 1) }
            val outcome = server.serve(connection)
            state.update { it.copy(activeClients = server.activeClients) }
            Log.i(TAG, "Served $address: $outcome")
        }, "WearLinkServerHost-client").start()
    }

    private class Handler(
        private val store: MetricStore,
        private val localName: () -> String,
    ) : WearLinkRequestHandler {
        override fun localName(): String = localName.invoke()

        override fun capabilities(): Set<String> = WearMetrics.ALL.mapTo(LinkedHashSet()) { it.key }

        override fun heartRateSince(sinceEpochMillis: Long, limit: Int): Page<WearLinkProtocol.HeartRateSample> =
            page(WearMetrics.HEART_RATE, sinceEpochMillis, limit)

        override fun sleepMinutesSince(sinceEpochMillis: Long, limit: Int): Page<WearLinkProtocol.SleepMinute> =
            page(WearMetrics.SLEEP_MINUTES, sinceEpochMillis, limit)

        private fun <T> page(metric: WearMetric<T>, sinceEpochMillis: Long, limit: Int): Page<T> {
            // One more than asked tells whether the limit cut the reply.
            val rows = store.since(metric, sinceEpochMillis, limit + 1)
            return Page(rows.take(limit), rows.size > limit)
        }
    }

    private companion object {
        const val TAG = "WearLinkServerHost"
        const val SERVICE_NAME = "OpenVitalsWearApp"
    }
}
