package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GPS ephemeris: reading the files the user supplies, and handing them to the
 * watch. Nothing here reaches the network — the file arrives by the user's own
 * hand, as it does in Gadgetbridge.
 */
class GarminAgpsTest {

    // ── building the three file shapes ──────────────────────────────────────

    private fun sonyCpe() = byteArrayOf(0x2a, 0x12, 0xa0.toByte(), 0x02) + ByteArray(64)

    /** A gzipped rxNetworks blob generated [ageSeconds] ago. */
    private fun rxNetworks(ageSeconds: Long, now: Long = NOW): ByteArray {
        val generatedAt = (now - ageSeconds).toInt()
        val plain = byteArrayOf(0x01, 0x00) + byteArrayOf(
            (generatedAt ushr 24).toByte(),
            (generatedAt ushr 16).toByte(),
            (generatedAt ushr 8).toByte(),
            generatedAt.toByte(),
        ) + ByteArray(32)
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(plain) }
        return out.toByteArray()
    }

    /** A tar carrying [names], each with a byte of content. */
    private fun tar(vararg names: String): ByteArray {
        val out = ByteArrayOutputStream()
        for (name in names) {
            val header = ByteArray(512)
            name.toByteArray(Charsets.US_ASCII).copyInto(header)
            // Octal size field at 124, then the ustar magic at 257.
            "00000000001".toByteArray(Charsets.US_ASCII).copyInto(header, 124)
            "ustar".toByteArray(Charsets.US_ASCII).copyInto(header, 257)
            out.write(header)
            out.write(ByteArray(512).also { it[0] = 0x42 })
        }
        out.write(ByteArray(1024)) // the two zeroed blocks that end an archive
        return out.toByteArray()
    }

    // ── reading them ────────────────────────────────────────────────────────

    @Test
    fun `each file shape is recognised by its contents, not its name`() {
        // The user downloads these from a third party; the filename tells us
        // nothing we can trust.
        assertEquals(GarminAgpsKind.SONY_CPE, GarminAgpsFile.classify(sonyCpe()))
        assertEquals(GarminAgpsKind.RX_NETWORKS, GarminAgpsFile.classify(rxNetworks(3_600)))
        assertEquals(
            GarminAgpsKind.CONSTELLATION_TAR,
            GarminAgpsFile.classify(tar("CPE_GPS.BIN", "CPE_GLO.BIN")),
        )
    }

    @Test
    fun `something that is not ephemeris at all is refused`() {
        assertNull(GarminAgpsFile.classify("not ephemeris".toByteArray()))
        assertNull(GarminAgpsFile.classify(ByteArray(0)))
    }

    @Test
    fun `a tar must carry every constellation the watch asked for`() {
        val file = tar("CPE_GPS.BIN", "CPE_GLO.BIN")

        assertTrue(
            GarminAgpsFile.isValid(file, GarminAgpsKind.CONSTELLATION_TAR, listOf("GPS", "GLONASS")),
        )
        // Half the constellations is not half a fix; the watch asked for
        // Galileo and would come up short.
        assertFalse(
            GarminAgpsFile.isValid(file, GarminAgpsKind.CONSTELLATION_TAR, listOf("GPS", "GALILEO")),
        )
        assertFalse(
            GarminAgpsFile.isValid(file, GarminAgpsKind.CONSTELLATION_TAR, listOf("PLUTO")),
        )
    }

    @Test
    fun `stale ephemeris is refused rather than sent`() {
        // Ephemeris predicts where the satellites WILL be. A week-old
        // prediction is not a smaller benefit, it is a wrong one, and the
        // watch would be better off with the almanac it already has.
        assertTrue(
            GarminAgpsFile.isValid(
                rxNetworks(ageSeconds = 2 * 86_400),
                GarminAgpsKind.RX_NETWORKS,
                nowEpochSeconds = NOW,
            ),
        )
        assertFalse(
            GarminAgpsFile.isValid(
                rxNetworks(ageSeconds = 8 * 86_400),
                GarminAgpsKind.RX_NETWORKS,
                nowEpochSeconds = NOW,
            ),
        )
        assertFalse(
            GarminAgpsFile.isValid(
                rxNetworks(ageSeconds = -86_400),
                GarminAgpsKind.RX_NETWORKS,
                nowEpochSeconds = NOW,
            ),
        )
    }

    // ── serving them ────────────────────────────────────────────────────────

    private class Recorder(private val held: Map<GarminAgpsKind, ByteArray> = emptyMap()) {
        val requested = mutableListOf<String>()
        val served = mutableListOf<GarminAgpsKind>()
        val rejected = mutableListOf<GarminAgpsKind>()

        val source = GarminAgpsSource(
            load = { held[it] },
            onRequested = { url, _ -> requested.add(url) },
            onServed = { served.add(it) },
            onRejected = { kind, _ -> rejected.add(kind) },
        )
    }

    private fun request(url: String, ifNoneMatch: String? = null): GarminHttpRequest {
        val path = url.substringAfter("api.gcs.garmin.com").substringBefore("?")
        val query = url.substringAfter("?", "")
            .split("&")
            .filter { it.isNotBlank() }
            .associate { it.substringBefore("=") to it.substringAfter("=") }
        return GarminHttpRequest(
            url = url,
            domain = "api.gcs.garmin.com",
            path = path,
            query = query,
            method = null,
            headers = ifNoneMatch?.let { mapOf("if-none-match" to it) }.orEmpty(),
            body = null,
            useDataTransfer = false,
        )
    }

    @Test
    fun `the watch gets the file that matches what it asked for`() {
        val sony = sonyCpe()
        val recorder = Recorder(mapOf(GarminAgpsKind.SONY_CPE to sony))
        val interceptor = GarminAgpsInterceptor(recorder.source)
        val ask = request("https://api.gcs.garmin.com/ephemeris/cpe/sony/v1")

        assertTrue(interceptor.supports(ask))
        val response = interceptor.handle(ask)!!

        assertEquals(200, response.status)
        assertTrue(sony.contentEquals(response.body))
        // Sent, not merely built: the store records delivery, not intent.
        assertTrue(recorder.served.isEmpty())
        response.onSent?.invoke()
        assertEquals(listOf(GarminAgpsKind.SONY_CPE), recorder.served)
    }

    @Test
    fun `a watch that already has the file is told so instead of resent it`() {
        val recorder = Recorder(mapOf(GarminAgpsKind.SONY_CPE to sonyCpe()))
        val interceptor = GarminAgpsInterceptor(recorder.source)
        val url = "https://api.gcs.garmin.com/ephemeris/cpe/sony/v1"
        val etag = interceptor.handle(request(url))!!.headers["etag"]!!

        val second = interceptor.handle(request(url, ifNoneMatch = etag))!!

        // The watch asks often, and re-sending 60 KB it already holds is pure
        // airtime over a link that is also carrying notifications.
        assertEquals(304, second.status)
        assertEquals(0, second.body.size)
    }

    @Test
    fun `nothing imported means the ask is refused, not answered with junk`() {
        val recorder = Recorder()
        val interceptor = GarminAgpsInterceptor(recorder.source)

        assertNull(interceptor.handle(request("https://api.gcs.garmin.com/ephemeris/cpe/sony/v1")))

        // Still recorded: the URL is how the user learns which file to fetch.
        assertEquals(
            listOf("https://api.gcs.garmin.com/ephemeris/cpe/sony/v1"),
            recorder.requested,
        )
    }

    @Test
    fun `a file of the wrong shape is refused and reported`() {
        // A tar was imported, but this watch's chipset wants Sony CPE.
        val recorder = Recorder(mapOf(GarminAgpsKind.CONSTELLATION_TAR to tar("CPE_GPS.BIN")))
        val interceptor = GarminAgpsInterceptor(recorder.source)

        val response = interceptor.handle(
            request("https://api.gcs.garmin.com/ephemeris/cpe/v1?constellations=GPS,GALILEO"),
        )

        assertNull(response)
        assertEquals(listOf(GarminAgpsKind.CONSTELLATION_TAR), recorder.rejected)
    }

    @Test
    fun `an ephemeris url this app does not understand is left alone`() {
        val recorder = Recorder(mapOf(GarminAgpsKind.SONY_CPE to sonyCpe()))
        val interceptor = GarminAgpsInterceptor(recorder.source)

        // Claimed, since it is ephemeris, but answered with nothing: guessing
        // a format would be worse than the watch falling back to its almanac.
        val ask = request("https://api.gcs.garmin.com/ephemeris/something/new")
        assertTrue(interceptor.supports(ask))
        assertNull(interceptor.handle(ask))
    }

    private companion object {
        /** 2026-08-12T10:00:00Z, so file ages are exact rather than wall-clock. */
        const val NOW = 1_786_608_000L
    }
}
