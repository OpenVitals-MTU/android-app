package tech.mmarca.openvitals.features.devicesync.protocol

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * The byte-transport seam between the sync protocol (pure Kotlin) and the actual
 * carrier (Bluetooth RFCOMM via `features/devicesync/bluetooth`).
 *
 * The protocol layer ([SyncSession]) depends ONLY on [SyncByteTransport], so it
 * can be driven over an in-memory pipe in tests with no Bluetooth at all. The
 * production implementation lives in `BluetoothSyncManager` and wraps the
 * RFCOMM byte channel.
 */
interface SyncByteTransport {
    /**
     * Sends [bytes] to the peer. Suspends until the bytes have been handed to
     * the carrier (which applies backpressure over a slow link). Callers must
     * not assume concurrent sends are safe — [SyncSession] serializes its own.
     */
    suspend fun send(bytes: ByteArray)

    /**
     * Raw inbound byte chunks from the peer, in order; the carrier may re-chunk
     * them arbitrarily (framing handles that). The channel closes when the link
     * ends.
     */
    val inbound: ReceiveChannel<ByteArray>

    /** Closes the channel. Idempotent. */
    fun close()
}

/**
 * An in-memory [SyncByteTransport] pair for tests: two endpoints wired together
 * so what one [send]s appears on the other's [inbound]. Models the real carrier
 * closely enough to exercise the whole protocol without Bluetooth.
 *
 * Use [SyncPipe.create] to get the connected `(a, b)` pair.
 */
class SyncPipe private constructor() : SyncByteTransport {

    private lateinit var peer: SyncPipe
    private val inboundChannel = Channel<ByteArray>(Channel.UNLIMITED)

    @Volatile private var closed = false

    override val inbound: ReceiveChannel<ByteArray> get() = inboundChannel

    override suspend fun send(bytes: ByteArray) {
        check(!closed) { "pipe is closed" }
        // Buffered channel: delivery never re-enters the peer synchronously.
        peer.inboundChannel.trySend(bytes.copyOf())
    }

    override fun close() {
        if (closed) return
        closed = true
        inboundChannel.close()
        // Closing one end ends the peer's inbound too.
        if (!peer.closed) {
            peer.closed = true
            peer.inboundChannel.close()
        }
    }

    companion object {
        /** Creates two endpoints wired to each other. */
        fun create(): Pair<SyncPipe, SyncPipe> {
            val a = SyncPipe()
            val b = SyncPipe()
            a.peer = b
            b.peer = a
            return a to b
        }
    }
}
