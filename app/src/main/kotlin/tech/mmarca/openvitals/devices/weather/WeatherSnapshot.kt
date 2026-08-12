package tech.mmarca.openvitals.devices.weather

import org.json.JSONObject

/**
 * One weather report as a companion weather app broadcast it, in Gadgetbridge's
 * generic-weather JSON schema — the de-facto standard Breezy Weather (and
 * every other Gadgetbridge-aware weather app) already speaks. Implementing
 * the same schema is what lets those apps feed OpenVitals with zero changes
 * on their side: the user just adds this app's package name as a broadcast
 * target.
 *
 * Units follow that schema, quirks included: temperatures in KELVIN as
 * integers, wind in km/h, timestamps in unix SECONDS. Conversion to what the
 * watch wants happens in the FIT encoder, not here — this type stores what
 * was received, verbatim, so a stored snapshot can be re-encoded differently
 * later without loss.
 */
data class WeatherSnapshot(
    /** When the provider says this report was observed, unix seconds. */
    val timestamp: Long,
    val location: String,
    val currentTempKelvin: Int,
    val todayMinTempKelvin: Int,
    val todayMaxTempKelvin: Int,
    /** OpenWeatherMap condition code — the schema's condition vocabulary. */
    val currentConditionCode: Int,
    val currentHumidity: Int,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Int,
    val uvIndex: Double,
    val precipProbability: Int,
    val dewPointKelvin: Int,
    val feelsLikeTempKelvin: Int,
    val latitude: Double,
    val longitude: Double,
    val pressureMbar: Double = 0.0,
    val cloudCover: Int = 0,
    val visibilityMeters: Double = 0.0,
    val hourly: List<HourlyForecast> = emptyList(),
    val daily: List<DailyForecast> = emptyList(),
) {
    data class HourlyForecast(
        val timestamp: Long,
        val tempKelvin: Int,
        val conditionCode: Int,
        val humidity: Int,
        val windSpeedKmh: Double,
        val windDirectionDegrees: Int,
        val uvIndex: Double,
        val precipProbability: Int,
    )

    data class DailyForecast(
        val minTempKelvin: Int,
        val maxTempKelvin: Int,
        val conditionCode: Int,
        val precipProbability: Int,
        val sunRise: Long = 0,
        val sunSet: Long = 0,
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("timestamp", timestamp)
        put("location", location)
        put("currentTemp", currentTempKelvin)
        put("todayMinTemp", todayMinTempKelvin)
        put("todayMaxTemp", todayMaxTempKelvin)
        put("currentConditionCode", currentConditionCode)
        put("currentHumidity", currentHumidity)
        put("windSpeed", windSpeedKmh)
        put("windDirection", windDirectionDegrees)
        put("uvIndex", uvIndex)
        put("precipProbability", precipProbability)
        put("dewPoint", dewPointKelvin)
        put("feelsLikeTemp", feelsLikeTempKelvin)
        put("latitude", latitude)
        put("longitude", longitude)
        put("pressure", pressureMbar)
        put("cloudCover", cloudCover)
        put("visibility", visibilityMeters)
        put(
            "hourly",
            org.json.JSONArray().also { array ->
                hourly.forEach { hour ->
                    array.put(
                        JSONObject().apply {
                            put("timestamp", hour.timestamp)
                            put("temp", hour.tempKelvin)
                            put("conditionCode", hour.conditionCode)
                            put("humidity", hour.humidity)
                            put("windSpeed", hour.windSpeedKmh)
                            put("windDirection", hour.windDirectionDegrees)
                            put("uvIndex", hour.uvIndex)
                            put("precipProbability", hour.precipProbability)
                        },
                    )
                }
            },
        )
        put(
            "forecasts",
            org.json.JSONArray().also { array ->
                daily.forEach { day ->
                    array.put(
                        JSONObject().apply {
                            put("minTemp", day.minTempKelvin)
                            put("maxTemp", day.maxTempKelvin)
                            put("conditionCode", day.conditionCode)
                            put("precipProbability", day.precipProbability)
                            put("sunRise", day.sunRise)
                            put("sunSet", day.sunSet)
                        },
                    )
                }
            },
        )
    }

    companion object {
        /**
         * Parses the broadcast schema. Absent fields default rather than
         * fail — the schema grew over years and senders fill what they have,
         * so a strict parser would reject real-world payloads.
         */
        fun fromJson(json: JSONObject): WeatherSnapshot = WeatherSnapshot(
            timestamp = json.optLong("timestamp", System.currentTimeMillis() / 1000),
            location = json.optString("location", ""),
            currentTempKelvin = json.optInt("currentTemp", 0),
            todayMinTempKelvin = json.optInt("todayMinTemp", 0),
            todayMaxTempKelvin = json.optInt("todayMaxTemp", 0),
            currentConditionCode = json.optInt("currentConditionCode", 0),
            currentHumidity = json.optInt("currentHumidity", 0),
            windSpeedKmh = json.optDouble("windSpeed", 0.0),
            windDirectionDegrees = json.optInt("windDirection", 0),
            uvIndex = json.optDouble("uvIndex", 0.0),
            precipProbability = json.optInt("precipProbability", 0),
            dewPointKelvin = json.optInt("dewPoint", 0),
            feelsLikeTempKelvin = json.optInt("feelsLikeTemp", 0),
            latitude = json.optDouble("latitude", 0.0),
            longitude = json.optDouble("longitude", 0.0),
            pressureMbar = json.optDouble("pressure", 0.0),
            cloudCover = json.optInt("cloudCover", 0),
            visibilityMeters = json.optDouble("visibility", 0.0),
            hourly = json.optJSONArray("hourly")?.let { array ->
                (0 until array.length()).map { i ->
                    val hour = array.getJSONObject(i)
                    HourlyForecast(
                        timestamp = hour.optLong("timestamp", 0),
                        tempKelvin = hour.optInt("temp", 0),
                        conditionCode = hour.optInt("conditionCode", 0),
                        humidity = hour.optInt("humidity", 0),
                        windSpeedKmh = hour.optDouble("windSpeed", 0.0),
                        windDirectionDegrees = hour.optInt("windDirection", 0),
                        uvIndex = hour.optDouble("uvIndex", 0.0),
                        precipProbability = hour.optInt("precipProbability", 0),
                    )
                }
            }.orEmpty(),
            daily = json.optJSONArray("forecasts")?.let { array ->
                (0 until array.length()).map { i ->
                    val day = array.getJSONObject(i)
                    DailyForecast(
                        minTempKelvin = day.optInt("minTemp", 0),
                        maxTempKelvin = day.optInt("maxTemp", 0),
                        conditionCode = day.optInt("conditionCode", 0),
                        precipProbability = day.optInt("precipProbability", 0),
                        sunRise = day.optLong("sunRise", 0),
                        sunSet = day.optLong("sunSet", 0),
                    )
                }
            }.orEmpty(),
        )
    }
}
