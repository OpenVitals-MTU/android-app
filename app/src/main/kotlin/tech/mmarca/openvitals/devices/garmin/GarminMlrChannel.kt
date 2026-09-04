package tech.mmarca.openvitals.devices.garmin

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * Stop-and-wait implementation of Garmin's Multi-Link Reliable sub-protocol.
 *
 * Gadgetbridge pipelines up to 32 fragments; file sync does not need that
 * throughput to be correct. Keeping one fragment outstanding makes ordering,
 * wraparound and retransmission deterministic while retaining the wire format
 * and cumulative acknowledgements used by the watch.
 */
internal class GarminMlrChannel(
    private val handle: Int,
    private var maxPacketSize: Int,
    private val scope: CoroutineScope,
    private val write: suspend (ByteArray) -> Unit,
    private val onData: (ByteArray) -> Unit,
    private val onLog: ((String) -> Unit)? = null,
) {
    private val sendMutex = Mutex()
    private var nextSendSequence = 0
    private var nextReceiveSequence = 0
    private var pendingAck: CompletableDeferred<Unit>? = null
    private var expectedAck = 0
    private var closed = false

    fun setMaxPacketSize(size: Int) {
        maxPacketSize = size
    }

    suspend fun sendMessage(message: ByteArray) = sendMutex.withLock {
        check(!closed) { "MLR channel is closed" }
        val chunkSize = (maxPacketSize - 2).coerceAtLeast(1)
        var offset = 0
        while (offset < message.size) {
            val end = (offset + chunkSize).coerceAtMost(message.size)
            val data = message.copyOfRange(offset, end)
            val sequence = nextSendSequence
            expectedAck = (sequence + 1) and SEQUENCE_MASK
            var timeout = 1.seconds
            var delivered = false
            for (attempt in 0 until MAX_ATTEMPTS) {
                val waiter = CompletableDeferred<Unit>()
                pendingAck = waiter
                write(packet(nextReceiveSequence, sequence, data))
                try {
                    withTimeout(timeout) { waiter.await() }
                    delivered = true
                    break
                } catch (_: TimeoutCancellationException) {
                    onLog?.invoke(
                        "[GARMIN-MLR] retransmitting handle=$handle seq=$sequence attempt=${attempt + 2}",
                    )
                    timeout *= 2
                } finally {
                    if (pendingAck === waiter) pendingAck = null
                }
            }
            check(delivered) { "MLR fragment $sequence was not acknowledged" }
            nextSendSequence = expectedAck
            offset = end
        }
    }

    fun handlePacket(packet: ByteArray) {
        if (closed || packet.size < 2) return
        val first = packet[0].toInt() and 0xFF
        val second = packet[1].toInt() and 0xFF
        val packetHandle = (first and HANDLE_MASK) ushr HANDLE_SHIFT
        if (packetHandle != (handle and 0x07)) return
        val requestNumber = ((first and REQUEST_HIGH_MASK) shl 2) or (second ushr 6)
        val sequence = second and SEQUENCE_MASK

        if (requestNumber == expectedAck) pendingAck?.complete(Unit)

        if (packet.size > 2) {
            if (sequence == nextReceiveSequence) {
                onData(packet.copyOfRange(2, packet.size))
                nextReceiveSequence = (nextReceiveSequence + 1) and SEQUENCE_MASK
            }
            // ACK promptly. Delayed/batched acknowledgements save packets but
            // make Android process scheduling part of transfer correctness.
            scope.launchSafely {
                write(packet(nextReceiveSequence, 0, ByteArray(0)))
            }
        }
    }

    fun close() {
        closed = true
        pendingAck?.completeExceptionally(IllegalStateException("MLR channel closed"))
        pendingAck = null
    }

    private fun packet(requestNumber: Int, sequence: Int, data: ByteArray): ByteArray {
        val out = ByteArray(data.size + 2)
        out[0] = (MLR_FLAG or
            ((handle and 0x07) shl HANDLE_SHIFT) or
            ((requestNumber ushr 2) and REQUEST_HIGH_MASK)).toByte()
        out[1] = (((requestNumber and 0x03) shl 6) or (sequence and SEQUENCE_MASK)).toByte()
        data.copyInto(out, 2)
        return out
    }

    private fun CoroutineScope.launchSafely(block: suspend () -> Unit) {
        launch {
            runCatching { block() }
                .onFailure { onLog?.invoke("[GARMIN-MLR] ACK write failed: $it") }
        }
    }

    private companion object {
        const val MLR_FLAG = 0x80
        const val HANDLE_MASK = 0x70
        const val HANDLE_SHIFT = 4
        const val REQUEST_HIGH_MASK = 0x0F
        const val SEQUENCE_MASK = 0x3F
        const val MAX_ATTEMPTS = 5
    }
}
