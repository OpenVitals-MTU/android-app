package tech.mmarca.openvitals.devices.wearos

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bluetooth Classic RFCOMM implementation of [WearOsNodePort], on AOSP APIs
 * only. Finds the bonded watch and pings the OpenVitals Wear OS app listening
 * on [OPENVITALS_WEAR_APP_UUID]. The watch side is `wear/.../WearAppService`.
 *
 * RFCOMM is Classic, not BLE, so no radio lease is taken; phone-to-phone
 * sync works the same way.
 */
@Singleton
class BluetoothWearOsNodePort @Inject constructor(
    @ApplicationContext private val context: Context,
) : WearOsNodePort {

    override suspend fun checkStatus(
        targetAddress: String?,
        targetName: String?,
    ): WearOsCompanionStatus = withContext(Dispatchers.IO) {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || !adapter.isEnabled) return@withContext notPaired()

        // A missing BLUETOOTH_CONNECT propagates: the screen shows the grant affordance.
        val bonded = adapter.bondedDevices.orEmpty().map { BondedWatch(it.address, it.name) }
        val match = WearOsBondMatcher.pick(bonded, targetAddress, targetName)
            ?: return@withContext notPaired()

        val answered = ping(adapter, adapter.getRemoteDevice(match.address))
        WearOsCompanionStatus(
            isPaired = true,
            connectedNodeName = match.name ?: match.address,
            connectedNodeAddress = match.address,
            appStatus = if (answered) WearOsAppStatus.APP_RUNNING else WearOsAppStatus.NO_ANSWER,
            lastCheckedAt = Instant.now(),
        )
    }

    /**
     * One PING, one PONG. `connect()` and `readLine()` block and ignore
     * cancellation, so a watchdog closes the socket after [PING_TIMEOUT_MS]
     * or when the caller is cancelled, whichever comes first.
     */
    private suspend fun ping(adapter: BluetoothAdapter, device: BluetoothDevice): Boolean =
        coroutineScope {
            val socket = try {
                device.createRfcommSocketToServiceRecord(OPENVITALS_WEAR_APP_UUID)
            } catch (e: IOException) {
                Log.d(TAG, "No RFCOMM socket for ${device.address}: ${e.message}")
                return@coroutineScope false
            }
            val watchdog = launch {
                try {
                    delay(PING_TIMEOUT_MS)
                } finally {
                    runCatching { socket.close() }
                }
            }
            try {
                // A running discovery slows the connect down. Cancelling it needs
                // BLUETOOTH_SCAN, which the ping itself does not.
                runCatching { adapter.cancelDiscovery() }
                socket.connect()
                socket.outputStream.write("$PING\n".toByteArray(Charsets.UTF_8))
                socket.outputStream.flush()
                val reply = socket.inputStream.bufferedReader(Charsets.UTF_8).readLine()
                reply?.trim() == PONG
            } catch (e: IOException) {
                // Off, out of range, app not listening, or the watchdog closed the socket.
                Log.d(TAG, "No answer from ${device.address}: ${e.message}")
                false
            } finally {
                watchdog.cancel()
                runCatching { socket.close() }
            }
        }

    private fun notPaired() = WearOsCompanionStatus(
        isPaired = false,
        appStatus = WearOsAppStatus.NOT_PAIRED,
        lastCheckedAt = Instant.now(),
    )

    companion object {
        private const val TAG = "BluetoothWearOsNodePort"

        /** Connect (paging plus service lookup) and the exchange together. */
        private const val PING_TIMEOUT_MS = 8_000L

        /** Must match `WearAppService` on the watch; `WearOsLinkParityTest` holds them together. */
        val OPENVITALS_WEAR_APP_UUID: UUID = UUID.fromString("4838d728-6e5a-4b95-a29d-a60032338301")
        const val PING = "PING"
        const val PONG = "PONG"
    }
}
