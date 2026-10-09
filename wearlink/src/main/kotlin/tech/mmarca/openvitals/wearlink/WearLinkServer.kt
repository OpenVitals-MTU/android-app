package tech.mmarca.openvitals.wearlink

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * The watch's end of one connection, from the hello to the close, run on
 * the caller's thread (the watch keeps one thread per accepted client).
 *
 * The phone speaks first with a hello; the reply decides everything: only
 * `OK` opens the request loop. The loop ends when the phone closes, so a
 * big page is never cut short by the watch closing first (a Bluetooth
 * socket closed by its writer drops whatever the reader has not taken),
 * and several requests can share one connection. Watchdogs close the
 * socket when the phone never says hello, goes quiet mid-connection, or
 * stays too long; a cap on concurrent clients answers `ERROR busy`.
 */
class WearLinkServer(
    private val trust: WearLinkTrustStore,
    private val handler: WearLinkRequestHandler,
    private val options: Options = Options(),
) {

    data class Options(
        /** The phone must say hello within this. */
        val helloTimeoutMillis: Long = 5_000L,
        /** Between requests on an open connection. */
        val idleTimeoutMillis: Long = 15_000L,
        /** One connection may not live longer than this. */
        val maxLifetimeMillis: Long = 600_000L,
        /** Beyond this many connections at once, the next gets `ERROR busy`. */
        val maxClients: Int = 2,
        /** After a refusal, how long to wait for the phone to close before closing. */
        val refusalCloseWaitMillis: Long = 2_000L,
    )

    /** How a connection ended; [IDLE] is a served phone that went quiet past the idle timeout. */
    enum class Outcome { SERVED, IDLE, PENDING, UNAUTHORIZED, VERSION_MISMATCH, BAD_HELLO, BUSY, NO_HELLO, PROTOCOL_ERROR, INTERNAL_ERROR }

    private val active = AtomicInteger(0)

    /** Connections being served right now. */
    val activeClients: Int
        get() = active.get()

    fun serve(connection: WearLinkConnection): Outcome {
        if (active.incrementAndGet() > options.maxClients) {
            try {
                refuse(connection, WearLinkProtocol.formatError(WearLinkProtocol.ErrorCode.BUSY))
            } finally {
                active.decrementAndGet()
            }
            return Outcome.BUSY
        }
        val lifetime = WearLinkWatchdog(options.maxLifetimeMillis) { connection.close() }
        try {
            return serveInner(connection)
        } catch (_: LineTooLongException) {
            return Outcome.PROTOCOL_ERROR
        } catch (_: IOException) {
            return Outcome.PROTOCOL_ERROR
        } catch (_: RuntimeException) {
            runCatching { LineWriter(connection.output).apply { line(WearLinkProtocol.formatError(WearLinkProtocol.ErrorCode.INTERNAL)); flush() } }
            return Outcome.INTERNAL_ERROR
        } finally {
            lifetime.cancel()
            connection.close()
            active.decrementAndGet()
        }
    }

    private fun serveInner(connection: WearLinkConnection): Outcome {
        val reader = LineReader(connection.input)
        val writer = LineWriter(connection.output)
        val watchdog = WearLinkWatchdog(options.helloTimeoutMillis) { connection.close() }
        try {
            val line = reader.readLine() ?: return Outcome.NO_HELLO
            val hello = WearLinkProtocol.parseHello(line)
            if (hello == null) {
                watchdog.cancel()
                refuse(connection, WearLinkProtocol.formatError(WearLinkProtocol.ErrorCode.BAD_HELLO))
                return Outcome.BAD_HELLO
            }
            val reply = decide(connection.peerAddress, hello)
            if (reply !is WearLinkProtocol.HelloReply.Ok) {
                watchdog.cancel()
                refuse(connection, WearLinkProtocol.formatHelloReply(reply))
                return when (reply) {
                    WearLinkProtocol.HelloReply.Pending -> Outcome.PENDING
                    is WearLinkProtocol.HelloReply.Unauthorized -> Outcome.UNAUTHORIZED
                    is WearLinkProtocol.HelloReply.VersionMismatch -> Outcome.VERSION_MISMATCH
                    else -> Outcome.INTERNAL_ERROR
                }
            }
            writer.line(WearLinkProtocol.formatHelloReply(reply))
            writer.flush()
            watchdog.reset(options.idleTimeoutMillis)
            while (true) {
                val request = readRequest(reader) ?: return if (watchdog.hasFired) Outcome.IDLE else Outcome.SERVED
                watchdog.reset(options.idleTimeoutMillis)
                answer(writer, request)
                writer.flush()
            }
        } finally {
            watchdog.cancel()
        }
    }

    /**
     * The next request, or null once the connection is over. Android's
     * Bluetooth socket reports the phone's close as an exception rather than
     * an end of stream, so after the hello both mean the same: served.
     */
    private fun readRequest(reader: LineReader): String? =
        try {
            reader.readLine()
        } catch (e: LineTooLongException) {
            throw e
        } catch (_: IOException) {
            null
        }

    private fun decide(peerAddress: String, hello: WearLinkProtocol.Hello): WearLinkProtocol.HelloReply {
        if (hello.version < WearLinkProtocol.MIN_VERSION || hello.version > WearLinkProtocol.VERSION) {
            return WearLinkProtocol.HelloReply.VersionMismatch(WearLinkProtocol.MIN_VERSION, WearLinkProtocol.VERSION)
        }
        if (trust.isBlocked(peerAddress)) {
            return WearLinkProtocol.HelloReply.Unauthorized(WearLinkProtocol.UnauthorizedReason.BLOCKED)
        }
        val stored = trust.tokenFor(peerAddress)
        if (stored == null) {
            trust.notePending(peerAddress, hello.token, hello.name)
            return WearLinkProtocol.HelloReply.Pending
        }
        if (!WearLinkToken.matches(stored, hello.token)) {
            trust.noteMismatch(peerAddress, hello.name)
            return WearLinkProtocol.HelloReply.Unauthorized(WearLinkProtocol.UnauthorizedReason.MISMATCH)
        }
        return WearLinkProtocol.HelloReply.Ok(WearLinkProtocol.VERSION, handler.capabilities(), handler.localName())
    }

    private fun answer(writer: LineWriter, line: String) {
        when (val request = WearLinkProtocol.parseRequest(line)) {
            null -> writer.line(WearLinkProtocol.formatError(WearLinkProtocol.ErrorCode.BAD_REQUEST))
            WearLinkProtocol.Request.Ping -> writer.line(WearLinkProtocol.PONG)
            is WearLinkProtocol.Request.HeartRateSince -> {
                val page = handler.heartRateSince(request.sinceEpochMillis, request.limit)
                for (sample in page.items) writer.line(WearLinkProtocol.formatSample(sample.epochMillis, sample.bpm))
                writer.line(WearLinkProtocol.formatEnd(page.items.size, page.more))
            }
            is WearLinkProtocol.Request.SleepMinutesSince -> {
                val page = handler.sleepMinutesSince(request.sinceEpochMillis, request.limit)
                for (minute in page.items) writer.line(WearLinkProtocol.formatSleepMinute(minute))
                writer.line(WearLinkProtocol.formatEnd(page.items.size, page.more))
            }
        }
    }

    /** Sends one refusal line, then waits briefly for the phone to close so the line is not dropped with the socket. */
    private fun refuse(connection: WearLinkConnection, line: String) {
        val writer = LineWriter(connection.output)
        writer.line(line)
        writer.flush()
        val watchdog = WearLinkWatchdog(options.refusalCloseWaitMillis) { connection.close() }
        try {
            while (connection.input.read() >= 0) Unit
        } catch (_: IOException) {
            // The watchdog or the phone closed it.
        } finally {
            watchdog.cancel()
        }
    }
}
