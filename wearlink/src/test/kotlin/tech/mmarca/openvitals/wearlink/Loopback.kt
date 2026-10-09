package tech.mmarca.openvitals.wearlink

import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Client and server in one JVM over piped streams: the server runs on its
 * own thread, the test drives the client. Buffers are large enough for a
 * full page; closing either end's connection closes both pipes.
 */
class Loopback(
    val trust: MemoryTrustStore = MemoryTrustStore(),
    val handler: MemoryHandler = MemoryHandler(),
    options: WearLinkServer.Options = WearLinkServer.Options(),
    val phoneAddress: String = "AA:BB:CC:DD:EE:01",
) {
    val server = WearLinkServer(trust, handler, options)

    /** One connection: a server thread serving it, and the client's end. */
    inner class Link {
        private val toServer = PipedOutputStream()
        private val fromServer = PipedOutputStream()
        private val serverInput = PipedInputStream(toServer, BUFFER)
        private val clientInput = PipedInputStream(fromServer, BUFFER)
        private val outcome = AtomicReference<WearLinkServer.Outcome?>()
        private val served = CountDownLatch(1)

        /**
         * Closing either end closes all four pipe ends: a closed Bluetooth
         * socket unblocks both sides' reads, and a piped reader only wakes
         * when its writer closes.
         */
        private fun closeAll() {
            runCatching { toServer.close() }
            runCatching { fromServer.close() }
            runCatching { clientInput.close() }
            runCatching { serverInput.close() }
        }

        val clientConnection = WearLinkConnection(clientInput, toServer, "watch") { closeAll() }
        private val serverConnection = WearLinkConnection(serverInput, fromServer, phoneAddress) { closeAll() }

        val serverThread = Thread({
            outcome.set(server.serve(serverConnection))
            served.countDown()
        }, "Loopback-server").apply { start() }

        /** The server's outcome once it returned, within [seconds]. */
        fun serverOutcome(seconds: Long = 5): WearLinkServer.Outcome? {
            served.await(seconds, TimeUnit.SECONDS)
            return outcome.get()
        }

        fun serverReturned(): Boolean = served.count == 0L
    }

    fun connect(): Link = Link()

    private companion object {
        const val BUFFER = 256 * 1024
    }
}

class MemoryTrustStore : WearLinkTrustStore {
    val tokens = HashMap<String, String>()
    val blocked = HashSet<String>()
    val pending = ArrayList<Triple<String, String, String>>()
    val mismatches = ArrayList<Pair<String, String>>()

    override fun tokenFor(peerAddress: String): String? = tokens[peerAddress]
    override fun isBlocked(peerAddress: String): Boolean = peerAddress in blocked
    override fun notePending(peerAddress: String, token: String, peerName: String) {
        pending += Triple(peerAddress, token, peerName)
    }
    override fun noteMismatch(peerAddress: String, peerName: String) {
        mismatches += peerAddress to peerName
    }
}

class MemoryHandler(
    var heartRate: List<WearLinkProtocol.HeartRateSample> = emptyList(),
    var minutes: List<WearLinkProtocol.SleepMinute> = emptyList(),
    var name: String = "Galaxy Watch8 (89FZ)",
) : WearLinkRequestHandler {
    override fun localName(): String = name
    override fun capabilities(): Set<String> = setOf(WearLinkProtocol.CAP_HEART_RATE, WearLinkProtocol.CAP_SLEEP_MINUTES)
    override fun heartRateSince(sinceEpochMillis: Long, limit: Int): Page<WearLinkProtocol.HeartRateSample> {
        val newer = heartRate.filter { it.epochMillis > sinceEpochMillis }
        return Page(newer.take(limit), newer.size > limit)
    }
    override fun sleepMinutesSince(sinceEpochMillis: Long, limit: Int): Page<WearLinkProtocol.SleepMinute> {
        val newer = minutes.filter { it.epochMillis > sinceEpochMillis }
        return Page(newer.take(limit), newer.size > limit)
    }
}
