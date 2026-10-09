package tech.mmarca.openvitals.wearlink

import java.io.IOException

/** What one exchange with the watch came to. */
sealed class WearLinkOutcome<out T> {
    data class Ok<T>(val value: T) : WearLinkOutcome<T>()
    data object Pending : WearLinkOutcome<Nothing>()
    data class Unauthorized(val reason: WearLinkProtocol.UnauthorizedReason) : WearLinkOutcome<Nothing>()
    data class VersionMismatch(val min: Int, val max: Int) : WearLinkOutcome<Nothing>()
    data class Refused(val code: WearLinkProtocol.ErrorCode) : WearLinkOutcome<Nothing>()

    /** The watch answered something the protocol does not allow here. */
    data class ProtocolError(val detail: String) : WearLinkOutcome<Nothing>()

    /** The connection ended before the answer: closed by the watch, the watchdog, or the link. */
    data object Closed : WearLinkOutcome<Nothing>()
}

/** An open connection whose hello the watch answered `OK`; requests go through it until [close]. */
class WearLinkSession internal constructor(
    val connection: WearLinkConnection,
    val peer: WearLinkProtocol.HelloReply.Ok,
    private val reader: LineReader,
    private val writer: LineWriter,
) {
    fun ping(): WearLinkOutcome<Unit> = exchange(WearLinkProtocol.Request.Ping) { line ->
        if (line.trim() == WearLinkProtocol.PONG) Reply.Done(Unit) else Reply.Unexpected
    }

    fun pullHeartRate(sinceEpochMillis: Long, limit: Int): WearLinkOutcome<Page<WearLinkProtocol.HeartRateSample>> {
        val items = ArrayList<WearLinkProtocol.HeartRateSample>()
        return exchange(WearLinkProtocol.Request.HeartRateSince(sinceEpochMillis, limit)) { line ->
            WearLinkProtocol.parseSample(line)?.let { items += it; return@exchange Reply.More }
            WearLinkProtocol.parseEnd(line)?.let { return@exchange Reply.Done(Page(items, it.more)) }
            Reply.Unexpected
        }
    }

    fun pullSleepMinutes(sinceEpochMillis: Long, limit: Int): WearLinkOutcome<Page<WearLinkProtocol.SleepMinute>> {
        val items = ArrayList<WearLinkProtocol.SleepMinute>()
        return exchange(WearLinkProtocol.Request.SleepMinutesSince(sinceEpochMillis, limit)) { line ->
            WearLinkProtocol.parseSleepMinute(line)?.let { items += it; return@exchange Reply.More }
            WearLinkProtocol.parseEnd(line)?.let { return@exchange Reply.Done(Page(items, it.more)) }
            Reply.Unexpected
        }
    }

    /** Closes the connection; the watch is waiting for exactly this. */
    fun close() = connection.close()

    private sealed class Reply<out T> {
        data object More : Reply<Nothing>()
        data class Done<T>(val value: T) : Reply<T>()
        data object Unexpected : Reply<Nothing>()
    }

    private fun <T> exchange(request: WearLinkProtocol.Request, onLine: (String) -> Reply<T>): WearLinkOutcome<T> =
        try {
            exchangeOrThrow(request, onLine)
        } catch (e: LineTooLongException) {
            WearLinkOutcome.ProtocolError(e.message ?: "line too long")
        } catch (_: IOException) {
            WearLinkOutcome.Closed
        }

    private fun <T> exchangeOrThrow(request: WearLinkProtocol.Request, onLine: (String) -> Reply<T>): WearLinkOutcome<T> {
        writer.line(WearLinkProtocol.formatRequest(request))
        writer.flush()
        while (true) {
            val line = reader.readLine() ?: return WearLinkOutcome.Closed
            val asReply = WearLinkProtocol.parseHelloReply(line)
            if (asReply is WearLinkProtocol.HelloReply.Error) return WearLinkOutcome.Refused(asReply.code)
            when (val reply = onLine(line)) {
                Reply.More -> continue
                is Reply.Done -> return WearLinkOutcome.Ok(reply.value)
                // An older or newer watch may say something extra; a line that is not ours is skipped.
                Reply.Unexpected -> continue
            }
        }
    }
}

/**
 * The phone's end: says hello over a fresh connection and, when the watch
 * answers `OK`, hands back a session for requests. Blocking; the caller
 * owns the socket's timeout (closing it unblocks every read here).
 */
object WearLinkClient {

    fun hello(connection: WearLinkConnection, hello: WearLinkProtocol.Hello): WearLinkOutcome<WearLinkSession> {
        val reader = LineReader(connection.input)
        val writer = LineWriter(connection.output)
        return try {
            writer.line(WearLinkProtocol.formatHello(hello))
            writer.flush()
            val line = reader.readLine() ?: return WearLinkOutcome.Closed
            when (val reply = WearLinkProtocol.parseHelloReply(line)) {
                null -> WearLinkOutcome.ProtocolError("unexpected reply to hello: ${line.take(40)}")
                is WearLinkProtocol.HelloReply.Ok -> WearLinkOutcome.Ok(WearLinkSession(connection, reply, reader, writer))
                WearLinkProtocol.HelloReply.Pending -> WearLinkOutcome.Pending
                is WearLinkProtocol.HelloReply.Unauthorized -> WearLinkOutcome.Unauthorized(reply.reason)
                is WearLinkProtocol.HelloReply.VersionMismatch -> WearLinkOutcome.VersionMismatch(reply.min, reply.max)
                is WearLinkProtocol.HelloReply.Error -> WearLinkOutcome.Refused(reply.code)
            }
        } catch (e: LineTooLongException) {
            WearLinkOutcome.ProtocolError(e.message ?: "line too long")
        } catch (_: IOException) {
            WearLinkOutcome.Closed
        }
    }
}
