package tech.mmarca.openvitals.devices.weather

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The broadcast schema: what Breezy Weather (via Gadgetbridge's contract) sends. */
class WeatherSnapshotTest {

    @Test
    fun `parses the generic weather schema`() {
        val json = JSONObject(
            """
            {
              "timestamp": 1786600800,
              "location": "Valencia",
              "currentTemp": 303,
              "todayMinTemp": 295,
              "todayMaxTemp": 306,
              "currentConditionCode": 800,
              "currentHumidity": 45,
              "windSpeed": 10.5,
              "windDirection": 180,
              "uvIndex": 7.5,
              "precipProbability": 5,
              "dewPoint": 289,
              "feelsLikeTemp": 305,
              "latitude": 39.47,
              "longitude": -0.376,
              "hourly": [
                {"timestamp": 1786604400, "temp": 304, "conditionCode": 801,
                 "humidity": 40, "windSpeed": 12.0, "windDirection": 190,
                 "uvIndex": 8.0, "precipProbability": 0}
              ],
              "forecasts": [
                {"minTemp": 294, "maxTemp": 307, "conditionCode": 500,
                 "precipProbability": 60}
              ]
            }
            """.trimIndent(),
        )

        val snapshot = WeatherSnapshot.fromJson(json)

        assertEquals("Valencia", snapshot.location)
        assertEquals(303, snapshot.currentTempKelvin)
        assertEquals(800, snapshot.currentConditionCode)
        assertEquals(10.5, snapshot.windSpeedKmh, 1e-9)
        assertEquals(1, snapshot.hourly.size)
        assertEquals(804 - 3, snapshot.hourly.single().conditionCode)
        assertEquals(1, snapshot.daily.size)
        assertEquals(60, snapshot.daily.single().precipProbability)
    }

    @Test
    fun `a sparse payload defaults instead of failing`() {
        // Senders fill what they have; the schema grew over years.
        val snapshot = WeatherSnapshot.fromJson(JSONObject("""{"currentTemp": 290}"""))
        assertEquals(290, snapshot.currentTempKelvin)
        assertEquals("", snapshot.location)
        assertTrue(snapshot.hourly.isEmpty())
        assertTrue(snapshot.daily.isEmpty())
    }

    @Test
    fun `survives its own round trip`() {
        val original = WeatherSnapshot(
            timestamp = 1_786_600_800L,
            location = "Valencia",
            currentTempKelvin = 303,
            todayMinTempKelvin = 295,
            todayMaxTempKelvin = 306,
            currentConditionCode = 800,
            currentHumidity = 45,
            windSpeedKmh = 10.5,
            windDirectionDegrees = 180,
            uvIndex = 7.5,
            precipProbability = 5,
            dewPointKelvin = 289,
            feelsLikeTempKelvin = 305,
            latitude = 39.47,
            longitude = -0.376,
            hourly = listOf(
                WeatherSnapshot.HourlyForecast(1_786_604_400L, 304, 801, 40, 12.0, 190, 8.0, 0),
            ),
            daily = listOf(WeatherSnapshot.DailyForecast(294, 307, 500, 60)),
        )

        // The store persists via this exact round trip.
        assertEquals(original, WeatherSnapshot.fromJson(original.toJson()))
    }
}
