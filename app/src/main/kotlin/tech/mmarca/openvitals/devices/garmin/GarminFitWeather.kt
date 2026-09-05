package tech.mmarca.openvitals.devices.garmin

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import tech.mmarca.openvitals.devices.weather.WeatherSnapshot

/**
 * Encodes a [WeatherSnapshot] as the FIT weather messages the watch's glance
 * renders: a FIT_DEFINITION (5011) declaring three local layouts of global
 * message 128, then a FIT_DATA (5012) with the records. Mirrors
 * Gadgetbridge's `FitWeather` table, quirks included: temperature is
 * Kelvin - 273 (not 273.15), wind is km/h scaled by 298, coordinates are
 * semicircles, and day_of_week counts Sunday as 0.
 */
object GarminFitWeather {

    // Local message slots, echoed in every data record's header.
    private const val LOCAL_CURRENT = 0
    private const val LOCAL_HOURLY = 1
    private const val LOCAL_DAILY = 2

    private const val WEATHER_GLOBAL_MESSAGE = 128

    // FIT base type identifiers.
    private const val TYPE_ENUM = 0x00
    private const val TYPE_SINT8 = 0x01
    private const val TYPE_UINT8 = 0x02
    private const val TYPE_STRING = 0x07
    private const val TYPE_UINT16 = 0x84
    private const val TYPE_SINT32 = 0x85
    private const val TYPE_UINT32 = 0x86
    private const val TYPE_FLOAT32 = 0x88

    private const val LOCATION_BYTES = 15
    private const val KELVIN_OFFSET = 273
    private const val WIND_SPEED_SCALE = 298.0
    private const val SEMICIRCLES_PER_DEGREE = 2147483648.0 / 180.0

    /** The watch caps its glance at these; more is wasted air time. */
    private const val MAX_HOURLY = 12
    private const val MAX_DAILY = 4

    /** One field of a record layout: `(number, size, baseType)`. */
    private data class Field(val number: Int, val size: Int, val baseType: Int)

    // Field layouts per report type, as Gadgetbridge sends them, in field order.
    // Data records must write exactly these fields in this order.
    private val currentFields = listOf(
        Field(0, 1, TYPE_ENUM), // weather_report
        Field(1, 1, TYPE_SINT8), // temperature
        Field(2, 1, TYPE_ENUM), // condition
        Field(3, 2, TYPE_UINT16), // wind_direction
        Field(4, 2, TYPE_UINT16), // wind_speed
        Field(5, 1, TYPE_UINT8), // precipitation_probability
        Field(6, 1, TYPE_SINT8), // temperature_feels_like
        Field(7, 1, TYPE_UINT8), // relative_humidity
        Field(8, LOCATION_BYTES, TYPE_STRING), // location
        Field(9, 4, TYPE_UINT32), // observed_at_time
        Field(10, 4, TYPE_SINT32), // observed_location_lat
        Field(11, 4, TYPE_SINT32), // observed_location_long
        Field(13, 1, TYPE_SINT8), // high_temperature
        Field(14, 1, TYPE_SINT8), // low_temperature
        Field(15, 1, TYPE_SINT8), // dew_point
        Field(17, 1, TYPE_ENUM), // air_quality
        Field(253, 4, TYPE_UINT32), // timestamp
    )

    private val hourlyFields = listOf(
        Field(0, 1, TYPE_ENUM),
        Field(1, 1, TYPE_SINT8),
        Field(2, 1, TYPE_ENUM),
        Field(3, 2, TYPE_UINT16),
        Field(4, 2, TYPE_UINT16),
        Field(5, 1, TYPE_UINT8),
        Field(6, 1, TYPE_SINT8),
        Field(7, 1, TYPE_UINT8),
        Field(15, 1, TYPE_SINT8),
        Field(16, 4, TYPE_FLOAT32), // uv_index
        Field(17, 1, TYPE_ENUM),
        Field(253, 4, TYPE_UINT32),
    )

    private val dailyFields = listOf(
        Field(0, 1, TYPE_ENUM),
        Field(2, 1, TYPE_ENUM),
        Field(5, 1, TYPE_UINT8),
        Field(12, 1, TYPE_ENUM), // day_of_week
        Field(13, 1, TYPE_SINT8),
        Field(14, 1, TYPE_SINT8),
        Field(17, 1, TYPE_ENUM),
        Field(253, 4, TYPE_UINT32),
    )

    /** The `FIT_DEFINITION` payload declaring all three record layouts. */
    fun definitionPayload(): ByteArray {
        val writer = GarminByteWriter()
        writeDefinition(writer, LOCAL_CURRENT, currentFields)
        writeDefinition(writer, LOCAL_HOURLY, hourlyFields)
        writeDefinition(writer, LOCAL_DAILY, dailyFields)
        return writer.toBytes()
    }

    /** The `FIT_DATA` payload: current conditions, then hourly, then daily. */
    fun dataPayload(
        weather: WeatherSnapshot,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ByteArray {
        val writer = GarminByteWriter()

        // Current conditions.
        writer.writeByte(LOCAL_CURRENT)
        writer.writeByte(REPORT_CURRENT)
        writeTemperature(writer, weather.currentTempKelvin)
        writeCondition(writer, weather.currentConditionCode)
        writer.writeShort(weather.windDirectionDegrees.coerceIn(0, 359))
        writeWindSpeed(writer, weather.windSpeedKmh)
        writer.writeByte(weather.precipProbability.coerceIn(0, 100))
        writeTemperature(writer, weather.feelsLikeTempKelvin)
        writer.writeByte(weather.currentHumidity.coerceIn(0, 100))
        writeLocation(writer, weather.location)
        writer.writeInt(GarminTime.fromInstant(Instant.ofEpochSecond(weather.timestamp)))
        writer.writeInt(semicircles(weather.latitude))
        writer.writeInt(semicircles(weather.longitude))
        writeTemperature(writer, weather.todayMaxTempKelvin)
        writeTemperature(writer, weather.todayMinTempKelvin)
        writeTemperature(writer, weather.dewPointKelvin)
        writer.writeByte(INVALID_ENUM) // air quality: not forwarded (yet)
        writer.writeInt(GarminTime.fromInstant(Instant.ofEpochSecond(weather.timestamp)))

        // Hourly forecast.
        for (hour in weather.hourly.take(MAX_HOURLY)) {
            writer.writeByte(LOCAL_HOURLY)
            writer.writeByte(REPORT_HOURLY)
            writeTemperature(writer, hour.tempKelvin)
            writeCondition(writer, hour.conditionCode)
            writer.writeShort(hour.windDirectionDegrees.coerceIn(0, 359))
            writeWindSpeed(writer, hour.windSpeedKmh)
            writer.writeByte(hour.precipProbability.coerceIn(0, 100))
            // The schema has no hourly feels-like; the plain temperature goes there.
            writeTemperature(writer, hour.tempKelvin)
            writer.writeByte(hour.humidity.coerceIn(0, 100))
            writer.writeByte(INVALID_SINT8) // dew point: not in the schema
            writeFloat(writer, hour.uvIndex.toFloat())
            writer.writeByte(INVALID_ENUM) // air quality
            writer.writeInt(GarminTime.fromInstant(Instant.ofEpochSecond(hour.timestamp)))
        }

        // Daily forecast: today, then up to four more days.
        val reportInstant = Instant.ofEpochSecond(weather.timestamp)
        writeDaily(
            writer = writer,
            reportTimestamp = reportInstant,
            day = dayOfWeekAt(reportInstant, zone),
            minKelvin = weather.todayMinTempKelvin,
            maxKelvin = weather.todayMaxTempKelvin,
            conditionCode = weather.currentConditionCode,
            precipProbability = weather.precipProbability,
        )
        weather.daily.take(MAX_DAILY).forEachIndexed { index, day ->
            val dayInstant = reportInstant.plusSeconds((index + 1) * 86_400L)
            writeDaily(
                writer = writer,
                reportTimestamp = reportInstant,
                day = dayOfWeekAt(dayInstant, zone),
                minKelvin = day.minTempKelvin,
                maxKelvin = day.maxTempKelvin,
                conditionCode = day.conditionCode,
                precipProbability = day.precipProbability,
            )
        }

        return writer.toBytes()
    }

    // Record pieces.

    private const val REPORT_CURRENT = 0
    private const val REPORT_HOURLY = 1
    private const val REPORT_DAILY = 2

    private const val DEFINITION_FLAG = 0x40
    private const val INVALID_ENUM = 0xFF
    private const val INVALID_SINT8 = 0x7F

    private fun writeDefinition(writer: GarminByteWriter, local: Int, fields: List<Field>) {
        writer.writeByte(DEFINITION_FLAG or local)
        writer.writeByte(0) // reserved
        writer.writeByte(0) // architecture: little-endian
        writer.writeShort(WEATHER_GLOBAL_MESSAGE)
        writer.writeByte(fields.size)
        for (field in fields) {
            writer.writeByte(field.number)
            writer.writeByte(field.size)
            writer.writeByte(field.baseType)
        }
    }

    private fun writeDaily(
        writer: GarminByteWriter,
        reportTimestamp: Instant,
        day: DayOfWeek,
        minKelvin: Int,
        maxKelvin: Int,
        conditionCode: Int,
        precipProbability: Int,
    ) {
        writer.writeByte(LOCAL_DAILY)
        writer.writeByte(REPORT_DAILY)
        writeCondition(writer, conditionCode)
        writer.writeByte(precipProbability.coerceIn(0, 100))
        // FIT's week starts on Sunday: java MONDAY=1..SUNDAY=7, mod 7.
        writer.writeByte(day.value % 7)
        writeTemperature(writer, maxKelvin)
        writeTemperature(writer, minKelvin)
        writer.writeByte(INVALID_ENUM) // air quality
        writer.writeInt(GarminTime.fromInstant(reportTimestamp))
    }

    private fun dayOfWeekAt(instant: Instant, zone: ZoneId): DayOfWeek =
        instant.atZone(zone).dayOfWeek

    /** Kelvin to the sint8 Celsius the watch expects, with upstream's integer -273. Zero marks absent. */
    private fun writeTemperature(writer: GarminByteWriter, kelvin: Int) {
        if (kelvin <= 0) {
            writer.writeByte(INVALID_SINT8)
            return
        }
        val celsius = (kelvin - KELVIN_OFFSET).coerceIn(-128, 126)
        writer.writeByte(celsius and 0xFF)
    }

    private fun writeCondition(writer: GarminByteWriter, openWeatherCode: Int) {
        writer.writeByte(fitCondition(openWeatherCode) ?: INVALID_ENUM)
    }

    private fun writeWindSpeed(writer: GarminByteWriter, kmh: Double) {
        val scaled = (kmh * WIND_SPEED_SCALE).roundToLong().coerceIn(0L, 0xFFFE)
        writer.writeShort(scaled.toInt())
    }

    private fun writeLocation(writer: GarminByteWriter, location: String) {
        val bytes = location.toByteArray(Charsets.UTF_8)
        // Truncate on a byte budget, never mid-codepoint.
        var length = minOf(bytes.size, LOCATION_BYTES - 1)
        if (length < bytes.size) {
            while (length > 0 && (bytes[length].toInt() and 0xC0) == 0x80) length--
        }
        for (i in 0 until LOCATION_BYTES) {
            writer.writeByte(if (i < length) bytes[i].toInt() and 0xFF else 0)
        }
    }

    private fun writeFloat(writer: GarminByteWriter, value: Float) {
        writer.writeInt(java.lang.Float.floatToIntBits(value).toLong() and 0xFFFFFFFFL)
    }

    private fun semicircles(degrees: Double): Long =
        (degrees * SEMICIRCLES_PER_DEGREE).roundToInt().toLong() and 0xFFFFFFFFL

    /** OpenWeatherMap condition code to FIT `weather_status`, Gadgetbridge's table. */
    fun fitCondition(code: Int): Int? = when (code) {
        200, 201, 202, 210, 211, 212, 230, 231, 232, 901 -> 6 // THUNDERSTORMS
        221 -> 14 // SCATTERED_THUNDERSTORMS
        300, 310, 313, 500, 520, 521 -> 16 // LIGHT_RAIN
        301, 311, 501, 531 -> 3 // RAIN
        302, 312, 314, 502, 503, 504, 522 -> 17 // HEAVY_RAIN
        321 -> 13 // SCATTERED_SHOWERS
        511 -> 15 // UNKNOWN_PRECIPITATION
        600 -> 18 // LIGHT_SNOW
        601, 620, 621 -> 4 // SNOW
        602, 622 -> 19 // HEAVY_SNOW
        611, 612, 613, 615, 616 -> 7 // WINTRY_MIX (sleet / rain and snow)
        701, 711, 721, 731, 751, 761, 762 -> 11 // HAZY
        741 -> 8 // FOG
        771, 781, 905 -> 5 // WINDY
        800 -> 0 // CLEAR
        801, 802 -> 1 // PARTLY_CLOUDY
        803 -> 2 // MOSTLY_CLOUDY
        804 -> 22 // CLOUDY
        906 -> 12 // HAIL
        else -> null
    }
}
