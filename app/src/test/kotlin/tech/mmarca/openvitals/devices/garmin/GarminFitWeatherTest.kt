package tech.mmarca.openvitals.devices.garmin

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.weather.WeatherSnapshot

/** The FIT weather encoding, byte for byte. The watch's glance is the only consumer, so the bytes are the contract. */
class GarminFitWeatherTest {

    private fun snapshot(
        hourly: List<WeatherSnapshot.HourlyForecast> = emptyList(),
        daily: List<WeatherSnapshot.DailyForecast> = emptyList(),
    ) = WeatherSnapshot(
        timestamp = Instant.parse("2026-08-12T10:00:00Z").epochSecond,
        location = "Valencia",
        currentTempKelvin = 303, // 30°C
        todayMinTempKelvin = 295, // 22°C
        todayMaxTempKelvin = 306, // 33°C
        currentConditionCode = 800, // clear
        currentHumidity = 45,
        windSpeedKmh = 10.0,
        windDirectionDegrees = 180,
        uvIndex = 7.5,
        precipProbability = 5,
        dewPointKelvin = 289,
        feelsLikeTempKelvin = 305,
        latitude = 39.47,
        longitude = -0.376,
        hourly = hourly,
        daily = daily,
    )

    // Definitions.

    @Test
    fun `definitions declare three layouts of global message 128`() {
        val payload = GarminFitWeather.definitionPayload()
        val reader = GarminByteReader(payload)

        for (local in 0..2) {
            val header = reader.readByte()
            assertEquals(0x40 or local, header) // definition flag + local slot
            assertEquals(0, reader.readByte()) // reserved
            assertEquals(0, reader.readByte()) // little-endian
            assertEquals(128, reader.readShort()) // weather global message
            val fieldCount = reader.readByte()
            assertTrue(fieldCount > 0)
            repeat(fieldCount) {
                reader.readByte() // number
                reader.readByte() // size
                reader.readByte() // base type
            }
        }
        assertEquals(0, reader.remaining)
    }

    @Test
    fun `the current layout matches the upstream field table`() {
        val reader = GarminByteReader(GarminFitWeather.definitionPayload())
        reader.readBytes(5) // header, reserved, arch, global
        val count = reader.readByte()
        val fields = (0 until count).map {
            Triple(reader.readByte(), reader.readByte(), reader.readByte())
        }
        // (number, size, baseType) — weather_report through timestamp.
        assertEquals(
            listOf(
                Triple(0, 1, 0x00), Triple(1, 1, 0x01), Triple(2, 1, 0x00),
                Triple(3, 2, 0x84), Triple(4, 2, 0x84), Triple(5, 1, 0x02),
                Triple(6, 1, 0x01), Triple(7, 1, 0x02), Triple(8, 15, 0x07),
                Triple(9, 4, 0x86), Triple(10, 4, 0x85), Triple(11, 4, 0x85),
                Triple(13, 1, 0x01), Triple(14, 1, 0x01), Triple(15, 1, 0x01),
                Triple(17, 1, 0x00), Triple(253, 4, 0x86),
            ),
            fields,
        )
    }

    // The current-conditions record.

    @Test
    fun `the current record carries the conditions in FIT units`() {
        val reader = GarminByteReader(GarminFitWeather.dataPayload(snapshot()))

        assertEquals(0, reader.readByte()) // local slot 0, no definition flag
        assertEquals(0, reader.readByte()) // report type: current
        assertEquals(30, reader.readByte()) // 303K → 30°C
        assertEquals(0, reader.readByte()) // OWM 800 → CLEAR
        assertEquals(180, reader.readShort())
        assertEquals(2980, reader.readShort()) // 10 km/h × 298
        assertEquals(5, reader.readByte())
        assertEquals(32, reader.readByte()) // feels like 305K → 32°C
        assertEquals(45, reader.readByte())
        val location = reader.readBytes(15)
        assertEquals("Valencia", String(location.takeWhile { it != 0.toByte() }.toByteArray()))
        val observedAt = reader.readInt()
        assertEquals(
            GarminTime.fromInstant(Instant.parse("2026-08-12T10:00:00Z")),
            observedAt,
        )
        // Semicircles round-trip back to degrees within a millionth.
        val lat = reader.readInt().toInt() / (2147483648.0 / 180.0)
        val lon = reader.readInt().toInt() / (2147483648.0 / 180.0)
        assertEquals(39.47, lat, 1e-6)
        assertEquals(-0.376, lon, 1e-6)
        assertEquals(33, reader.readByte()) // high
        assertEquals(22, reader.readByte()) // low
        assertEquals(16, reader.readByte()) // dew point 289K → 16°C
        assertEquals(0xFF, reader.readByte()) // air quality: invalid
        assertEquals(observedAt, reader.readInt())
    }

    @Test
    fun `a zero-Kelvin temperature encodes as the sint8 invalid value`() {
        val bare = snapshot().copy(dewPointKelvin = 0)
        val reader = GarminByteReader(GarminFitWeather.dataPayload(bare))
        reader.readBytes(2 + 1 + 1 + 2 + 2 + 1 + 1 + 1 + 15 + 4 + 4 + 4 + 1 + 1)
        assertEquals(0x7F, reader.readByte()) // dew point: absent, not -273°C
    }

    @Test
    fun `a long location truncates on a codepoint boundary`() {
        // "Vitoria-Gasteiz" is 15 bytes, one over budget; the cut must not split a codepoint.
        val reader = GarminByteReader(
            GarminFitWeather.dataPayload(snapshot().copy(location = "Vitoria-Gasteíz")),
        )
        reader.readBytes(2 + 1 + 1 + 2 + 2 + 1 + 1 + 1)
        val bytes = reader.readBytes(15)
        assertEquals(0, bytes[14].toInt()) // always NUL-terminated
        // Decodes cleanly — no replacement character from a split codepoint.
        val decoded = String(bytes.takeWhile { it != 0.toByte() }.toByteArray())
        assertTrue(decoded.startsWith("Vitoria-Gaste"))
        assertTrue('�' !in decoded)
    }

    // Forecasts.

    @Test
    fun `hourly and daily records follow under their own slots`() {
        val weather = snapshot(
            hourly = listOf(
                WeatherSnapshot.HourlyForecast(
                    timestamp = Instant.parse("2026-08-12T11:00:00Z").epochSecond,
                    tempKelvin = 304,
                    conditionCode = 801,
                    humidity = 40,
                    windSpeedKmh = 12.0,
                    windDirectionDegrees = 190,
                    uvIndex = 8.0,
                    precipProbability = 0,
                ),
            ),
            daily = listOf(
                WeatherSnapshot.DailyForecast(
                    minTempKelvin = 294,
                    maxTempKelvin = 307,
                    conditionCode = 500,
                    precipProbability = 60,
                ),
            ),
        )
        val payload = GarminFitWeather.dataPayload(
            weather,
            zone = ZoneId.of("Europe/Madrid"),
        )
        val reader = GarminByteReader(payload)

        // Skip the current record (header + 45 data bytes).
        reader.readBytes(1 + 45)

        // Hourly record, slot 1.
        assertEquals(1, reader.readByte())
        assertEquals(1, reader.readByte()) // report: hourly
        assertEquals(31, reader.readByte()) // 304K
        assertEquals(1, reader.readByte()) // OWM 801 → PARTLY_CLOUDY

        // Daily records, slot 2: today first, then tomorrow.
        reader.readBytes(2 + 2 + 1 + 1 + 1 + 1 + 4 + 1 + 4) // rest of hourly
        assertEquals(2, reader.readByte())
        assertEquals(2, reader.readByte()) // report: daily
        assertEquals(0, reader.readByte()) // today: clear
        assertEquals(5, reader.readByte())
        // 2026-08-12 is a Wednesday; FIT counts Sunday as 0 → 3.
        assertEquals(3, reader.readByte())

        reader.readBytes(1 + 1 + 1 + 4) // rest of today's daily
        assertEquals(2, reader.readByte())
        assertEquals(2, reader.readByte())
        assertEquals(16, reader.readByte()) // OWM 500 → LIGHT_RAIN
        assertEquals(60, reader.readByte())
        assertEquals(4, reader.readByte()) // Thursday
    }

    @Test
    fun `forecasts beyond the glance caps are dropped`() {
        val weather = snapshot(
            hourly = (0 until 24).map { hour ->
                WeatherSnapshot.HourlyForecast(
                    timestamp = 1_700_000_000L + hour * 3600L,
                    tempKelvin = 300,
                    conditionCode = 800,
                    humidity = 50,
                    windSpeedKmh = 5.0,
                    windDirectionDegrees = 0,
                    uvIndex = 1.0,
                    precipProbability = 0,
                )
            },
            daily = (0 until 8).map {
                WeatherSnapshot.DailyForecast(290, 300, 800, 0)
            },
        )
        val payload = GarminFitWeather.dataPayload(weather)
        var hourlyCount = 0
        var dailyCount = 0
        val reader = GarminByteReader(payload)
        reader.readBytes(1 + 45)
        while (reader.remaining > 0) {
            when (reader.readByte()) {
                1 -> { hourlyCount++; reader.readBytes(20) }
                2 -> { dailyCount++; reader.readBytes(11) }
                else -> break
            }
        }
        assertEquals(12, hourlyCount)
        assertEquals(5, dailyCount) // today + 4 forecast days
    }

    // Condition mapping.

    @Test
    fun `condition codes map like upstream`() {
        assertEquals(0, GarminFitWeather.fitCondition(800)) // clear
        assertEquals(6, GarminFitWeather.fitCondition(211)) // thunderstorm
        assertEquals(4, GarminFitWeather.fitCondition(601)) // snow
        assertEquals(8, GarminFitWeather.fitCondition(741)) // fog
        assertEquals(22, GarminFitWeather.fitCondition(804)) // overcast
        assertNull(GarminFitWeather.fitCondition(999)) // unknown → invalid
    }
}
