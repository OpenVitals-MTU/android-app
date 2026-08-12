package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayInputStream
import java.time.ZoneId
import java.util.zip.GZIPInputStream
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.weather.WeatherSnapshot

/**
 * The HTTP proxy and its interceptors, exercised with the protobuf messages a
 * vívoactive-era watch sends (`gdi_http_service` / `gdi_data_transfer`).
 */
class GarminHttpProxyTest {

    private val tallinn = WeatherSnapshot(
        timestamp = 1_786_600_800L,
        location = "Tallinn",
        currentTempKelvin = 290, // 17°C
        todayMinTempKelvin = 285,
        todayMaxTempKelvin = 293,
        currentConditionCode = 803,
        currentHumidity = 70,
        windSpeedKmh = 18.0,
        windDirectionDegrees = 250,
        uvIndex = 3.0,
        precipProbability = 30,
        dewPointKelvin = 284,
        feelsLikeTempKelvin = 288,
        latitude = 59.437,
        longitude = 24.7536,
        pressureMbar = 1013.0,
        cloudCover = 75,
        hourly = (0 until 6).map { hour ->
            WeatherSnapshot.HourlyForecast(
                timestamp = 1_786_600_800L + hour * 3600L,
                tempKelvin = 290 + hour,
                conditionCode = 500,
                humidity = 70,
                windSpeedKmh = 18.0,
                windDirectionDegrees = 250,
                uvIndex = 2.0,
                precipProbability = 40,
            )
        },
        daily = listOf(
            WeatherSnapshot.DailyForecast(284, 292, 500, 60, sunRise = 1_786_590_000L, sunSet = 1_786_650_000L),
            WeatherSnapshot.DailyForecast(283, 291, 800, 5),
        ),
    )

    private fun responder(
        weather: WeatherSnapshot? = tallinn,
        agps: GarminAgpsSource? = null,
    ) = GarminHttpProxy(
        buildList {
            add(
                GarminWeatherInterceptor(
                    weatherProvider = { weather },
                    zone = ZoneId.of("Europe/Tallinn"),
                ),
            )
            agps?.let { add(GarminAgpsInterceptor(it)) }
            add(GarminOauthInterceptor())
        },
    )

    /** A raw request the way the watch frames it. */
    private fun rawRequest(
        url: String,
        useDataXfer: Boolean = false,
        acceptGzip: Boolean = false,
        method: Int? = null,
        body: String? = null,
    ): ByteArray {
        val request = ProtobufWriter().string(1, url)
        if (method != null) request.varint(3, method)
        if (body != null) request.nested(7, body.toByteArray(Charsets.UTF_8))
        if (useDataXfer) request.varint(6, 1)
        if (acceptGzip) {
            request.nested(
                5,
                ProtobufWriter().string(1, "Accept-Encoding").string(2, "gzip").toBytes(),
            )
        }
        val service = ProtobufWriter().nested(5, request.toBytes()).toBytes()
        return ProtobufWriter().nested(GarminSmartService.HTTP, service).toBytes()
    }

    private fun rawResponse(reply: ByteArray): List<ProtobufField> {
        val http = protobufField(readProtobuf(reply), GarminSmartService.HTTP)!!.bytes!!
        return readProtobuf(protobufField(readProtobuf(http), 6)!!.bytes!!)
    }

    private fun responseBody(reply: ByteArray): ByteArray =
        protobufField(rawResponse(reply), 3)!!.bytes!!

    @Test
    fun `serves current conditions as the Garmin Connect API would`() {
        val reply = responder().handle(
            rawRequest("https://api.gcs.garmin.com/weather/v2/current?lat=59.4&lon=24.7&tempUnit=CELSIUS&speedUnit=METERS_PER_SECOND"),
        )!!

        val response = rawResponse(reply)
        assertEquals(100L, protobufField(response, 1)?.varint) // Status.OK
        assertEquals(200L, protobufField(response, 2)?.varint) // HTTP 200

        val json = JSONObject(String(responseBody(reply)))
        assertEquals(17, json.getJSONObject("temperature").getInt("value"))
        assertEquals("CELSIUS", json.getJSONObject("temperature").getString("units"))
        assertEquals("Tallinn", json.getString("locationName"))
        assertEquals(15, json.getInt("icon")) // 803 broken clouds
        assertEquals(70, json.getInt("relativeHumidity"))
        assertEquals(5.0, json.getJSONObject("wind").getJSONObject("speed").getDouble("value"), 1e-9)
        assertEquals("W", json.getJSONObject("wind").getString("directionString")) // 250°
    }

    @Test
    fun `serves the daily forecast with today first`() {
        val reply = responder().handle(
            rawRequest("https://api.gcs.garmin.com/weather/v2/forecast/day?duration=5&tempUnit=CELSIUS"),
        )!!

        val days = JSONArray(String(responseBody(reply)))
        assertEquals(3, days.length()) // today + two stored forecast days
        // The fixture timestamp is 2026-08-13 in Tallinn — a Thursday; v2
        // counts Monday as 1.
        assertEquals(4, days.getJSONObject(0).getInt("dayOfWeek"))
        assertEquals(20, days.getJSONObject(0).getJSONObject("high").getInt("value"))
        // The stored forecast's sunrise makes it through.
        assertEquals(1_786_590_000L, days.getJSONObject(1).getLong("epochSunrise"))
        assertTrue(!days.getJSONObject(2).has("epochSunrise"))
    }

    @Test
    fun `serves the hourly forecast capped at the asked duration`() {
        val reply = responder().handle(
            rawRequest("https://api.gcs.garmin.com/weather/v1/forecast/hour?duration=4"),
        )!!

        val hours = JSONArray(String(responseBody(reply)))
        assertEquals(4, hours.length())
        assertEquals(17, hours.getJSONObject(0).getJSONObject("temp").getInt("value"))
        assertEquals(17, hours.getJSONObject(0).getInt("icon")) // rain
    }

    @Test
    fun `gzips when the watch says it can take it`() {
        val reply = responder().handle(
            rawRequest("https://api.gcs.garmin.com/weather/v2/current", acceptGzip = true),
        )!!

        val response = rawResponse(reply)
        val headers = response.filter { it.field == 5 }.map { header ->
            val fields = readProtobuf(header.bytes!!)
            protobufField(fields, 1)!!.bytes!!.toString(Charsets.UTF_8) to
                protobufField(fields, 2)!!.bytes!!.toString(Charsets.UTF_8)
        }
        assertTrue(headers.contains("Content-Encoding" to "gzip"))
        val body = protobufField(response, 3)!!.bytes!!
        val json = GZIPInputStream(ByteArrayInputStream(body)).readBytes()
        assertEquals("Tallinn", JSONObject(String(json)).getString("locationName"))
    }

    @Test
    fun `a data-transfer response is chunked and finishes clean`() {
        val responder = responder()
        val reply = responder.handle(
            rawRequest("https://api.gcs.garmin.com/weather/v2/forecast/hour?duration=6", useDataXfer = true),
        )!!

        // No inline body — an id and a size instead.
        val response = rawResponse(reply)
        assertNull(protobufField(response, 3))
        val xfer = readProtobuf(protobufField(response, 4)!!.bytes!!)
        val id = protobufField(xfer, 1)!!.varint!!.toInt()
        val total = protobufField(xfer, 2)!!.varint!!.toInt()
        assertTrue(total > 0)

        // Pull it the way the watch does: 200 bytes at a time.
        val assembled = StringBuilder()
        var offset = 0
        while (offset < total) {
            val request = ProtobufWriter()
                .varint(1, id).varint(2, offset).varint(3, 200).toBytes()
            val service = ProtobufWriter().nested(1, request).toBytes()
            val smart = ProtobufWriter().nested(GarminSmartService.DATA_TRANSFER, service).toBytes()

            val chunkReply = responder.handle(smart)!!
            val transfer = protobufField(readProtobuf(chunkReply), GarminSmartService.DATA_TRANSFER)!!.bytes!!
            val download = readProtobuf(protobufField(readProtobuf(transfer), 2)!!.bytes!!)
            assertEquals(1L, protobufField(download, 1)?.varint) // SUCCESS
            assertEquals(offset.toLong(), protobufField(download, 3)?.varint)
            val payload = protobufField(download, 4)!!.bytes!!
            assembled.append(String(payload))
            offset += payload.size
        }
        assertEquals(6, JSONArray(assembled.toString()).length())

        // The id died with the final chunk.
        val again = ProtobufWriter().nested(
            GarminSmartService.DATA_TRANSFER,
            ProtobufWriter().nested(
                1,
                ProtobufWriter().varint(1, id).varint(2, 0).varint(3, 200).toBytes(),
            ).toBytes(),
        ).toBytes()
        val deadReply = responder.handle(again)!!
        val transfer = protobufField(readProtobuf(deadReply), GarminSmartService.DATA_TRANSFER)!!.bytes!!
        val download = readProtobuf(protobufField(readProtobuf(transfer), 2)!!.bytes!!)
        assertEquals(2L, protobufField(download, 1)?.varint) // INVALID_ID
    }

    @Test
    fun `a chunked body counts as delivered only once the last chunk goes`() {
        val served = mutableListOf<GarminAgpsKind>()
        // A real Sony CPE blob's header, since the interceptor checks the
        // shape before it will serve anything.
        val ephemeris =
            byteArrayOf(0x2a, 0x12, 0xa0.toByte(), 0x02) + ByteArray(1_196) { (it % 251).toByte() }
        val responder = responder(
            agps = GarminAgpsSource(
                load = { ephemeris },
                onServed = { served.add(it) },
            ),
        )

        val reply = responder.handle(
            rawRequest("https://api.gcs.garmin.com/ephemeris/cpe/sony/v1", useDataXfer = true),
        )!!
        val xfer = readProtobuf(protobufField(rawResponse(reply), 4)!!.bytes!!)
        val id = protobufField(xfer, 1)!!.varint!!.toInt()
        val total = protobufField(xfer, 2)!!.varint!!.toInt()
        assertEquals(ephemeris.size, total)

        var offset = 0
        while (offset < total) {
            // Recorded as delivered before the watch has the bytes would be a
            // lie the settings screen keeps repeating.
            assertTrue(served.isEmpty())
            val smart = ProtobufWriter().nested(
                GarminSmartService.DATA_TRANSFER,
                ProtobufWriter().nested(
                    1,
                    ProtobufWriter().varint(1, id).varint(2, offset).varint(3, 500).toBytes(),
                ).toBytes(),
            ).toBytes()
            val transfer = protobufField(
                readProtobuf(responder.handle(smart)!!),
                GarminSmartService.DATA_TRANSFER,
            )!!.bytes!!
            val download = readProtobuf(protobufField(readProtobuf(transfer), 2)!!.bytes!!)
            offset += protobufField(download, 4)!!.bytes!!.size
        }

        assertEquals(listOf(GarminAgpsKind.SONY_CPE), served)
    }

    @Test
    fun `an unrelated domain gets an explicit unknown, never silence`() {
        val reply = responder().handle(
            rawRequest("https://connectapi.garmin.com/device-gateway/usercontact/contacts"),
        )!!
        // Unanswered would leave the watch retrying; UNKNOWN tells it to stop.
        assertEquals(0L, protobufField(rawResponse(reply), 1)?.varint)
    }

    // ── the credential exchange the connected tier waits behind ─────────────

    @Test
    fun `connectToIT is answered with fabricated bearer tokens`() {
        val reply = responder().handle(
            rawRequest(
                "https://services.garmin.com/oauthTokenExchangeService/connectToIT",
                method = 3, // POST
            ),
        )!!

        val response = rawResponse(reply)
        assertEquals(100L, protobufField(response, 1)?.varint)
        assertEquals(200L, protobufField(response, 2)?.varint)
        val json = JSONObject(String(responseBody(reply)))
        assertEquals("Bearer", json.getString("tokenType"))
        assertEquals(7_776_000, json.getInt("expiresIn"))
        assertTrue(json.getString("scope").contains("GCS_CIQ_APPSTORE_MOBILE_READ"))
        assertTrue(json.getString("accessToken").isNotBlank())
        assertTrue(json.getString("customerId").isNotBlank())
    }

    @Test
    fun `a token refresh keeps the watch's refresh token`() {
        val reply = responder().handle(
            rawRequest(
                "https://services.garmin.com/oauth/refresh_token/token",
                method = 3,
                body = "grant_type=refresh_token&refresh_token=keep-me&client_id=x",
            ),
        )!!

        val json = JSONObject(String(responseBody(reply)))
        assertEquals("keep-me", json.getString("refresh_token"))
        assertEquals("Bearer", json.getString("token_type"))
    }

    @Test
    fun `a non-POST oauth ask is refused like upstream`() {
        val reply = responder().handle(
            rawRequest(
                "https://services.garmin.com/oauthTokenExchangeService/connectToIT",
                method = 1, // GET
            ),
        )!!
        assertEquals(0L, protobufField(rawResponse(reply), 1)?.varint)
    }

    @Test
    fun `no fresh weather answers unknown rather than stale air`() {
        val reply = responder(weather = null).handle(
            rawRequest("https://api.gcs.garmin.com/weather/v2/current"),
        )!!
        assertEquals(0L, protobufField(rawResponse(reply), 1)?.varint)
    }

    @Test
    fun `messages for other services are left alone`() {
        // A settings-service message must fall through to its own handler.
        val smart = ProtobufWriter()
            .nested(GarminSmartService.SETTINGS, ByteArray(0))
            .toBytes()
        assertNull(responder().handle(smart))
    }
}
