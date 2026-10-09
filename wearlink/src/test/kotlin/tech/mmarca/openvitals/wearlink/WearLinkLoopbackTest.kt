package tech.mmarca.openvitals.wearlink

import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Client against server over piped streams: the whole lifecycle of a connection, without Bluetooth. */
class WearLinkLoopbackTest {

    private val token = WearLinkToken.generate()
    private fun hello(version: Int = WearLinkProtocol.VERSION, token: String = this.token) =
        WearLinkProtocol.Hello(version, setOf("hr", "sm"), token, "Pixel 6 Pro")

    private fun trusted(): Loopback = Loopback().also { it.trust.tokens[it.phoneAddress] = token }

    @Test
    fun `a trusted phone says hello, pings, and the watch returns once the phone closes`() {
        val loopback = trusted()
        val link = loopback.connect()

        val session = (WearLinkClient.hello(link.clientConnection, hello()) as WearLinkOutcome.Ok).value
        assertEquals("Galaxy Watch8 (89FZ)", session.peer.name)
        assertEquals(setOf("hr", "sm"), session.peer.capabilities)
        assertEquals(WearLinkOutcome.Ok(Unit), session.ping())
        assertFalse(link.serverReturned())
        session.close()

        assertEquals(WearLinkServer.Outcome.SERVED, link.serverOutcome())
        assertEquals(0, loopback.server.activeClients)
    }

    @Test
    fun `an unknown phone is pending, allowed, then served`() {
        val loopback = Loopback()
        val first = loopback.connect()

        assertEquals(WearLinkOutcome.Pending, WearLinkClient.hello(first.clientConnection, hello()))
        first.clientConnection.close()
        assertEquals(WearLinkServer.Outcome.PENDING, first.serverOutcome())
        assertEquals(listOf(Triple(loopback.phoneAddress, token, "Pixel 6 Pro")), loopback.trust.pending)

        loopback.trust.tokens[loopback.phoneAddress] = token
        val second = loopback.connect()
        assertTrue(WearLinkClient.hello(second.clientConnection, hello()) is WearLinkOutcome.Ok)
        second.clientConnection.close()
        assertEquals(WearLinkServer.Outcome.SERVED, second.serverOutcome())
    }

    @Test
    fun `a different token for a known phone is a mismatch, a blocked phone is blocked`() {
        val loopback = trusted()
        val mismatch = loopback.connect()
        assertEquals(
            WearLinkOutcome.Unauthorized(WearLinkProtocol.UnauthorizedReason.MISMATCH),
            WearLinkClient.hello(mismatch.clientConnection, hello(token = WearLinkToken.generate())),
        )
        mismatch.clientConnection.close()
        assertEquals(WearLinkServer.Outcome.UNAUTHORIZED, mismatch.serverOutcome())
        assertEquals(listOf(loopback.phoneAddress to "Pixel 6 Pro"), loopback.trust.mismatches)

        loopback.trust.blocked += loopback.phoneAddress
        val blocked = loopback.connect()
        assertEquals(
            WearLinkOutcome.Unauthorized(WearLinkProtocol.UnauthorizedReason.BLOCKED),
            WearLinkClient.hello(blocked.clientConnection, hello()),
        )
        blocked.clientConnection.close()
    }

    @Test
    fun `an old phone learns the versions the watch speaks`() {
        val link = trusted().connect()

        assertEquals(WearLinkOutcome.VersionMismatch(4, 4), WearLinkClient.hello(link.clientConnection, hello(version = 3)))
        link.clientConnection.close()
        assertEquals(WearLinkServer.Outcome.VERSION_MISMATCH, link.serverOutcome())
    }

    @Test
    fun `a page of two thousand minutes arrives whole, and two requests share one connection`() {
        val loopback = trusted()
        loopback.handler.minutes = List(2500) { index ->
            WearLinkProtocol.SleepMinute(1_700_000_000_000L + index * 60_000L, WearLinkProtocol.MinuteKind.RAW, 7200, 16, 300, 0, 58, 12, 6, intArrayOf(0, 0, 1000), intArrayOf(3, 3, 3), 88, 90, 1)
        }
        loopback.handler.heartRate = List(10) { WearLinkProtocol.HeartRateSample(1_700_000_000_000L + it * 10_000L, 60 + it) }
        val link = loopback.connect()
        val session = (WearLinkClient.hello(link.clientConnection, hello()) as WearLinkOutcome.Ok).value

        val page = (session.pullSleepMinutes(0, 2000) as WearLinkOutcome.Ok).value
        assertEquals(2000, page.items.size)
        assertTrue(page.more)
        val rest = (session.pullSleepMinutes(page.items.last().epochMillis, 2000) as WearLinkOutcome.Ok).value
        assertEquals(500, rest.items.size)
        assertFalse(rest.more)
        val heart = (session.pullHeartRate(0, 2000) as WearLinkOutcome.Ok).value
        assertEquals(10, heart.items.size)
        assertFalse(link.serverReturned())
        session.close()
        assertEquals(WearLinkServer.Outcome.SERVED, link.serverOutcome())
    }

    @Test
    fun `a phone that never says hello is cut off, and one that goes quiet is too`() {
        // A closed pipe reads as end of stream where a closed socket throws; either way the watchdog ended it early.
        val quick = Loopback(options = WearLinkServer.Options(helloTimeoutMillis = 200, idleTimeoutMillis = 200))
        quick.trust.tokens[quick.phoneAddress] = token
        val started = System.currentTimeMillis()
        val silent = quick.connect()
        val silentOutcome = silent.serverOutcome(seconds = 3)
        assertTrue("$silentOutcome", silentOutcome == WearLinkServer.Outcome.NO_HELLO || silentOutcome == WearLinkServer.Outcome.PROTOCOL_ERROR)
        assertTrue(System.currentTimeMillis() - started < 2_000)

        val idle = quick.connect()
        val session = (WearLinkClient.hello(idle.clientConnection, hello()) as WearLinkOutcome.Ok).value
        val idleOutcome = idle.serverOutcome(seconds = 3)
        assertEquals(WearLinkServer.Outcome.IDLE, idleOutcome)
        assertEquals(WearLinkOutcome.Closed, session.ping())
    }

    @Test
    fun `a bad hello and a bad request are named, and the connection survives a bad request`() {
        val loopback = trusted()
        val bad = loopback.connect()
        LineWriter(bad.clientConnection.output).apply { line("PING"); flush() }
        assertEquals("ERROR bad_hello", LineReader(bad.clientConnection.input).readLine())
        bad.clientConnection.close()
        assertEquals(WearLinkServer.Outcome.BAD_HELLO, bad.serverOutcome())

        val link = loopback.connect()
        val session = (WearLinkClient.hello(link.clientConnection, hello()) as WearLinkOutcome.Ok).value
        LineWriter(link.clientConnection.output).apply { line("WHAT"); flush() }
        assertEquals("ERROR bad_request", LineReader(link.clientConnection.input).readLine())
        session.close()
        assertEquals(WearLinkServer.Outcome.SERVED, link.serverOutcome())
    }

    @Test
    fun `a line over the limit ends the connection`() {
        val link = trusted().connect()
        LineWriter(link.clientConnection.output).apply { line("HELLO " + "x".repeat(600)); flush() }

        assertEquals(WearLinkServer.Outcome.PROTOCOL_ERROR, link.serverOutcome())
    }

    @Test
    fun `a third phone at once is told the watch is busy`() {
        val loopback = Loopback(options = WearLinkServer.Options(maxClients = 2))
        loopback.trust.tokens[loopback.phoneAddress] = token
        val one = loopback.connect()
        val two = loopback.connect()
        val first = (WearLinkClient.hello(one.clientConnection, hello()) as WearLinkOutcome.Ok).value
        val second = (WearLinkClient.hello(two.clientConnection, hello()) as WearLinkOutcome.Ok).value
        val three = loopback.connect()

        assertEquals(WearLinkOutcome.Refused(WearLinkProtocol.ErrorCode.BUSY), WearLinkClient.hello(three.clientConnection, hello()))
        three.clientConnection.close()
        assertEquals(WearLinkServer.Outcome.BUSY, three.serverOutcome())
        first.close()
        second.close()
        assertEquals(WearLinkServer.Outcome.SERVED, one.serverOutcome())
        assertEquals(WearLinkServer.Outcome.SERVED, two.serverOutcome())
    }

    @Test
    fun `the watch closes a refusal only after the phone did, so the line is never dropped`() {
        val loopback = Loopback(options = WearLinkServer.Options(refusalCloseWaitMillis = 300))
        val link = loopback.connect()

        assertEquals(WearLinkOutcome.Pending, WearLinkClient.hello(link.clientConnection, hello()))
        Thread.sleep(100)
        assertFalse(link.serverReturned())
        link.clientConnection.close()
        assertEquals(WearLinkServer.Outcome.PENDING, link.serverOutcome())
    }

    @Test
    fun `hello and reply lines round-trip through the writer`() {
        val out = ByteArrayOutputStream()
        LineWriter(out).apply { line(WearLinkProtocol.formatHello(hello())); flush() }

        assertTrue(out.toString(Charsets.UTF_8).startsWith("HELLO 4 hr,sm $token Pixel 6 Pro"))
    }
}
