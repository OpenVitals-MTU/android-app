package tech.mmarca.openvitals.devices.garmin

import java.time.Instant
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject
import tech.mmarca.openvitals.devices.weather.WeatherSnapshot

/**
 * Serves the watch's weather requests. Modern watches ask through the HTTP
 * proxy with Garmin Connect API calls; this answers from the companion-app
 * snapshot. Mirrors Gadgetbridge's `WeatherInterceptor`, quirks included.
 */
class GarminWeatherInterceptor(
    private val weatherProvider: () -> WeatherSnapshot?,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : GarminHttpInterceptor {

    override fun supports(request: GarminHttpRequest): Boolean =
        request.domain in WEATHER_DOMAINS && request.path.startsWith("/weather/")

    override fun handle(request: GarminHttpRequest): GarminHttpResponse? {
        val weather = weatherProvider()
        if (weather == null) {
            GarminLog.log("[GARMIN-HTTP] watch asked for ${request.path}; nothing fresh to serve")
            return null
        }
        val json = weatherJson(request.path, request.query, weather) ?: run {
            GarminLog.log("[GARMIN-HTTP] unknown weather path ${request.path}")
            return null
        }
        return GarminHttpResponse(
            body = json.toByteArray(Charsets.UTF_8),
            headers = mapOf("Content-Type" to "application/json"),
        )
    }

    // The weather endpoints, Garmin Connect API shaped.

    private fun weatherJson(
        path: String,
        query: Map<String, String>,
        weather: WeatherSnapshot,
    ): String? {
        val tempUnit = query["tempUnit"] ?: "CELSIUS"
        val speedUnit = query["speedUnit"] ?: "METERS_PER_SECOND"
        return when (path.removePrefix("/weather/v1").removePrefix("/weather/v2")) {
            "/current" -> currentJson(weather, tempUnit, speedUnit).toString()
            "/forecast/day" -> dayForecastJson(
                weather,
                version = if (path.startsWith("/weather/v2/")) 2 else 1,
                duration = query["duration"]?.toIntOrNull() ?: 5,
                tempUnit = tempUnit,
                speedUnit = query["speedUnit"] ?: "KILOMETERS_PER_HOUR",
            ).toString()
            "/forecast/hour" -> hourForecastJson(
                weather,
                duration = query["duration"]?.toIntOrNull() ?: 13,
                tempUnit = tempUnit,
                speedUnit = speedUnit,
            ).toString()
            else -> null
        }
    }

    private fun currentJson(weather: WeatherSnapshot, tempUnit: String, speedUnit: String) =
        JSONObject().apply {
            put("epochSeconds", weather.timestamp)
            put("temperature", temperature(weather.currentTempKelvin, tempUnit))
            put("description", conditionText(weather.currentConditionCode))
            put("icon", garminIcon(weather.currentConditionCode))
            put("feelsLikeTemperature", temperature(weather.feelsLikeTempKelvin, tempUnit))
            put("dewPoint", temperature(weather.dewPointKelvin, tempUnit))
            put("relativeHumidity", weather.currentHumidity)
            put("wind", wind(weather.windSpeedKmh, weather.windDirectionDegrees, speedUnit))
            put("locationName", weather.location)
            put("visibility", value(weather.visibilityMeters, "METER"))
            put("pressure", value(weather.pressureMbar * 0.02953, "INCHES_OF_MERCURY"))
            put("pressureChange", value(0.0, "INCHES_OF_MERCURY"))
            put("cloudCoverage", weather.cloudCover)
        }

    private fun dayForecastJson(
        weather: WeatherSnapshot,
        version: Int,
        duration: Int,
        tempUnit: String,
        speedUnit: String,
    ): JSONArray {
        val days = JSONArray()
        // Today first, from the top-level fields, then the stored forecasts.
        val today = WeatherSnapshot.DailyForecast(
            minTempKelvin = weather.todayMinTempKelvin,
            maxTempKelvin = weather.todayMaxTempKelvin,
            conditionCode = weather.currentConditionCode,
            precipProbability = weather.precipProbability,
        )
        val all = listOf(today) + weather.daily
        val reportInstant = Instant.ofEpochSecond(weather.timestamp)
        for ((index, day) in all.take(duration).withIndex()) {
            val date = reportInstant.plusSeconds(index * 86_400L).atZone(zone)
            days.put(
                JSONObject().apply {
                    // v2 counts Monday as 1; v1 uses Sunday-as-1.
                    put(
                        "dayOfWeek",
                        if (version == 2) date.dayOfWeek.value else (date.dayOfWeek.value % 7) + 1,
                    )
                    put("description", conditionText(day.conditionCode))
                    put("summary", conditionText(day.conditionCode))
                    put("high", temperature(day.maxTempKelvin, tempUnit))
                    put("low", temperature(day.minTempKelvin, tempUnit))
                    put("precipProb", day.precipProbability)
                    put("icon", garminIcon(day.conditionCode))
                    if (day.sunRise > 0) put("epochSunrise", day.sunRise)
                    if (day.sunSet > 0) put("epochSunset", day.sunSet)
                    put("wind", wind(weather.windSpeedKmh, weather.windDirectionDegrees, speedUnit))
                    put("humidity", weather.currentHumidity)
                },
            )
        }
        return days
    }

    private fun hourForecastJson(
        weather: WeatherSnapshot,
        duration: Int,
        tempUnit: String,
        speedUnit: String,
    ): JSONArray {
        val hours = JSONArray()
        for (hour in weather.hourly.take(duration)) {
            hours.put(
                JSONObject().apply {
                    put("epochSeconds", hour.timestamp)
                    put("description", conditionText(hour.conditionCode))
                    put("temp", temperature(hour.tempKelvin, tempUnit))
                    put("precipProb", hour.precipProbability)
                    put("wind", wind(hour.windSpeedKmh, hour.windDirectionDegrees, speedUnit))
                    put("icon", garminIcon(hour.conditionCode))
                    put("uvIndex", hour.uvIndex)
                    put("relativeHumidity", hour.humidity)
                },
            )
        }
        return hours
    }

    // Pieces.

    private fun value(value: Number, units: String) = JSONObject().apply {
        put("value", value)
        put("units", units)
    }

    /** Kelvin → the asked unit, with upstream's integer -273 for Celsius. */
    private fun temperature(kelvin: Int, unit: String) = when (unit) {
        "FAHRENHEIT" -> value((kelvin - 273.15) * 9.0 / 5.0 + 32.0, "FAHRENHEIT")
        "KELVIN" -> value(kelvin, "KELVIN")
        else -> value(kelvin - 273, "CELSIUS")
    }

    private fun wind(kmh: Double, direction: Int, speedUnit: String) = JSONObject().apply {
        put(
            "speed",
            when (speedUnit) {
                "METERS_PER_SECOND" -> value(kmh / 3.6, "METERS_PER_SECOND")
                else -> value(kmh, "KILOMETERS_PER_HOUR")
            },
        )
        put("directionString", windDirectionText(direction))
        put("direction", direction)
    }

    private fun windDirectionText(degrees: Int): String {
        val normalized = ((degrees % 360) + 360) % 360
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return directions[(Math.round(normalized / 45.0) % 8).toInt()]
    }

    /** OWM condition code to the watch's icon index, mapped by upstream from a Venu 3. */
    fun garminIcon(code: Int): Int = when (code) {
        in 200..232 -> 27 // thunderstorms
        771, 781, 900, 901, 902, 905, in 951..962 -> 46 // wind and storm
        // Before the rain range: 511 is a freezing mix.
        511, 615, 616, 906 -> 40 // freezing rain, sleety mixes, hail
        in 300..321, in 500..531 -> 17 // drizzle and rain
        611, 612, in 600..602, in 620..622 -> 38 // snow
        in 701..762 -> 47 // fog and haze
        800, 904 -> 5 // clear
        801, 802 -> 8 // partly cloudy
        803, 804 -> 15 // clouds
        else -> 35 // upstream's default (a snowflake, oddly — theirs to own)
    }

    /** A short English description, since the schema carries only the code. */
    private fun conditionText(code: Int): String = when (code) {
        in 200..232 -> "Thunderstorm"
        in 300..321 -> "Drizzle"
        in 500..504 -> "Rain"
        511 -> "Freezing rain"
        in 520..531 -> "Showers"
        in 600..602 -> "Snow"
        in 611..616 -> "Sleet"
        in 620..622 -> "Snow showers"
        701, 741 -> "Fog"
        711, 721, 731, 751, 761, 762 -> "Haze"
        771, 781 -> "Storm"
        800 -> "Clear"
        801 -> "Partly cloudy"
        802 -> "Scattered clouds"
        803 -> "Broken clouds"
        804 -> "Overcast"
        else -> "Unknown"
    }

    private companion object {
        val WEATHER_DOMAINS = setOf("api.gcs.garmin.com", "cache.dciwx.com")
    }
}
