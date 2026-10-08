package tech.mmarca.openvitals.devices.wearos

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bluetooth Classic RFCOMM implementation of [WearOsNodePort], on AOSP APIs
 * only. Finds the bonded watch and speaks [WearLinkProtocol] to the OpenVitals
 * Wear OS app listening on its service UUID, one request per connection. The
 * watch side is `wear/.../WearAppService`.
 *
 * RFCOMM is Classic, not BLE, so no radio lease is taken; phone-to-phone
 * sync works the same way.
 */
@Singleton
class BluetoothWearOsNodePort @Inject constructor(
    @ApplicationContext private val context: Context,
) : WearOsNodePort {

    @SuppressLint("MissingPermission")
    override suspend fun checkStatus(
        targetAddress: String?,
        targetName: String?,
    ): WearOsCompanionStatus = withContext(Dispatchers.IO) {
        val (adapter, match) = bondedWatch(targetAddress, targetName) ?: return@withContext notPaired()

        val answered = try {
            val reply = exchange(adapter, adapter.getRemoteDevice(match.address), WearLinkProtocol.PING, PING_TIMEOUT_MS) { line ->
                if (line.trim() == WearLinkProtocol.PONG) LineOutcome.DONE else LineOutcome.SKIP
            }
            reply.completed
        } catch (e: WearOsLinkException) {
            false
        }
        WearOsCompanionStatus(
            isPaired = true,
            connectedNodeName = match.name ?: match.address,
            connectedNodeAddress = match.address,
            appStatus = if (answered) WearOsAppStatus.APP_RUNNING else WearOsAppStatus.NO_ANSWER,
            lastCheckedAt = Instant.now(),
        )
    }

    @SuppressLint("MissingPermission")
    override suspend fun pullHeartRate(
        targetAddress: String?,
        targetName: String?,
        since: Instant,
    ): WearOsHeartRatePage? = withContext(Dispatchers.IO) {
        val (adapter, match) = bondedWatch(targetAddress, targetName) ?: return@withContext null

        val samples = ArrayList<WearOsHeartRateSample>()
        var end: WearLinkProtocol.End? = null
        val request = WearLinkProtocol.formatHeartRateRequest(
            since.toEpochMilli(),
            WearLinkProtocol.MAX_SAMPLES_PER_REQUEST,
        )
        val reply = exchange(adapter, adapter.getRemoteDevice(match.address), request, PULL_TIMEOUT_MS) { line ->
            WearLinkProtocol.parseSample(line)?.let {
                samples += WearOsHeartRateSample(Instant.ofEpochMilli(it.epochMillis), it.bpm)
                return@exchange LineOutcome.SKIP
            }
            end = WearLinkProtocol.parseEnd(line)
            if (end != null) LineOutcome.DONE else LineOutcome.SKIP
        }
        val terminator = end
        if (!reply.completed || terminator == null) {
            throw WearOsLinkException("The watch stopped answering before the end of the page.")
        }
        WearOsHeartRatePage(samples = samples, hasMore = terminator.more)
    }

    @SuppressLint("MissingPermission")
    override suspend fun pullSleepMinutes(
        targetAddress: String?,
        targetName: String?,
        since: Instant,
    ): WearOsSleepMinutePage? = withContext(Dispatchers.IO) {
        val (adapter, match) = bondedWatch(targetAddress, targetName) ?: return@withContext null

        val minutes = ArrayList<WearOsSleepMinute>()
        var end: WearLinkProtocol.End? = null
        val request = WearLinkProtocol.formatSleepMinutesRequest(
            since.toEpochMilli(),
            WearLinkProtocol.MAX_MINUTES_PER_REQUEST,
        )
        val reply = exchange(adapter, adapter.getRemoteDevice(match.address), request, PULL_TIMEOUT_MS) { line ->
            WearLinkProtocol.parseSleepMinute(line)?.let {
                minutes += WearOsSleepMinute(
                    time = Instant.ofEpochMilli(it.epochMillis),
                    kind = it.kind,
                    movement = it.movement,
                    heartRate = it.bpm?.toFloat(),
                    zoneOffset = ZoneOffset.ofTotalSeconds(it.offsetSeconds),
                )
                return@exchange LineOutcome.SKIP
            }
            end = WearLinkProtocol.parseEnd(line)
            if (end != null) LineOutcome.DONE else LineOutcome.SKIP
        }
        val terminator = end
        if (!reply.completed || terminator == null) {
            throw WearOsLinkException("The watch stopped answering before the end of the page.")
        }
        WearOsSleepMinutePage(minutes = minutes, hasMore = terminator.more)
    }

    /** The adapter and the bonded entry for the registered watch, or null when there is none. */
    @SuppressLint("MissingPermission")
    private fun bondedWatch(targetAddress: String?, targetName: String?): Pair<BluetoothAdapter, BondedWatch>? {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || !adapter.isEnabled) return null
        // A missing BLUETOOTH_CONNECT propagates: the screen shows the grant affordance.
        val bonded = adapter.bondedDevices.orEmpty().map { BondedWatch(it.address, it.name) }
        val match = WearOsBondMatcher.pick(bonded, targetAddress, targetName) ?: return null
        return adapter to match
    }

    private enum class LineOutcome { SKIP, DONE }

    private class Exchange(val completed: Boolean)

    /**
     * One request, one reply. Writes [request], then feeds every reply line to
     * [onLine] until it says [LineOutcome.DONE] or the watch closes the socket.
     * `connect()` and `readLine()` block and ignore cancellation, so a watchdog
     * closes the socket after [timeoutMs] or when the caller is cancelled,
     * whichever comes first. Throws [WearOsLinkException] when nothing came back.
     */
    @SuppressLint("MissingPermission")
    private suspend fun exchange(
        adapter: BluetoothAdapter,
        device: BluetoothDevice,
        request: String,
        timeoutMs: Long,
        onLine: (String) -> LineOutcome,
    ): Exchange = coroutineScope {
        val socket = try {
            device.createRfcommSocketToServiceRecord(WearLinkProtocol.SERVICE_UUID)
        } catch (e: IOException) {
            Log.d(TAG, "No RFCOMM socket for ${device.address}: ${e.message}")
            throw WearOsLinkException("The watch has no OpenVitals link.", e)
        }
        val watchdog = launch {
            try {
                delay(timeoutMs)
            } finally {
                runCatching { socket.close() }
            }
        }
        try {
            // A running discovery slows the connect down. Cancelling it needs
            // BLUETOOTH_SCAN, which the exchange itself does not.
            runCatching { adapter.cancelDiscovery() }
            socket.connect()
            socket.outputStream.write("$request\n".toByteArray(Charsets.UTF_8))
            socket.outputStream.flush()
            val reader = socket.inputStream.bufferedReader(Charsets.UTF_8)
            var completed = false
            while (true) {
                val line = reader.readLine() ?: break
                if (onLine(line) == LineOutcome.DONE) {
                    completed = true
                    break
                }
            }
            Exchange(completed)
        } catch (e: IOException) {
            // Off, out of range, app not listening, or the watchdog closed the socket.
            Log.d(TAG, "No answer from ${device.address}: ${e.message}")
            throw WearOsLinkException("The watch did not answer.", e)
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

        /** A full page over RFCOMM is well under a second; the connect dominates. */
        private const val PULL_TIMEOUT_MS = 30_000L
    }
}
