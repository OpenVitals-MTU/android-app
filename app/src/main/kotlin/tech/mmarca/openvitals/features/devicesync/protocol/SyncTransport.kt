package tech.mmarca.openvitals.features.devicesync.protocol

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * The byte-transport seam between the protocol and the carrier. The
 * protocol depends only on this, so tests drive it over an in-memory pipe.
 */
interface SyncByteTransport {
    /** Sends [bytes]. Suspends until handed to the carrier. Concurrent sends are not safe. */
    suspend fun send(bytes: ByteArray)

    /** Inbound chunks in order, re-chunked arbitrarily. Closes when the link ends. */
    val inbound: ReceiveChannel<ByteArray>

    /** Closes the channel. Idempotent. */
    fun close()
}

/** An in-memory transport pair for tests. Use [SyncPipe.create]. */
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
