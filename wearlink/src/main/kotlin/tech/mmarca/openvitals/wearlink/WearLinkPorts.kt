package tech.mmarca.openvitals.wearlink

import java.io.InputStream
import java.io.OutputStream

/** One accepted or opened connection: the streams, who the peer is, and how to close it. */
class WearLinkConnection(
    val input: InputStream,
    val output: OutputStream,
    /** The peer's Bluetooth address, the key of its trust. */
    val peerAddress: String,
    private val closer: () -> Unit,
) {
    @Volatile
    var isClosed: Boolean = false
        private set

    fun close() {
        isClosed = true
        runCatching { closer() }
    }
}

/**
 * What the watch knows about phones. A token is stored only once the wearer
 * has allowed the phone; until then the phone is pending.
 */
interface WearLinkTrustStore {
    /** The token stored for [peerAddress], or null when the phone is unknown. */
    fun tokenFor(peerAddress: String): String?

    fun isBlocked(peerAddress: String): Boolean

    /** An unknown phone presented [token]: remember it for the wearer to allow or block. */
    fun notePending(peerAddress: String, token: String, peerName: String)

    /** A known phone presented a different token: tell the wearer. */
    fun noteMismatch(peerAddress: String, peerName: String)
}

/** One page of a pull. [more] means the limit cut it short. */
data class Page<T>(val items: List<T>, val more: Boolean)

/** What the watch serves once a phone is trusted. */
interface WearLinkRequestHandler {
    /** The watch's name, sent in the `OK` reply. */
    fun localName(): String

    fun capabilities(): Set<String>

    fun heartRateSince(sinceEpochMillis: Long, limit: Int): Page<WearLinkProtocol.HeartRateSample>

    fun sleepMinutesSince(sinceEpochMillis: Long, limit: Int): Page<WearLinkProtocol.SleepMinute>
}
