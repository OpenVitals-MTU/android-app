package tech.mmarca.openvitals.devices.weather

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import org.json.JSONArray
import org.json.JSONObject

/**
 * Decodes the generic-weather broadcast. Any app on the phone can send it, so every
 * size is capped before it is trusted: a few kilobytes of gzip can inflate to gigabytes,
 * and deep nesting overflows the JSON parser's stack.
 */
object GenericWeatherPayload {

    /** A real payload with 48 hours and 16 days is under 20 KB. */
    const val MAX_JSON_BYTES: Int = 512 * 1024
    const val MAX_NESTING: Int = 16
    const val MAX_HOURLY: Int = 72
    const val MAX_DAILY: Int = 16
    const val MAX_LOCATION_CHARS: Int = 120

    /** Thrown when a payload breaks a cap. */
    class TooLarge(message: String) : Exception(message)

    /** The primary location's weather, or null when the broadcast carries none. */
    fun decode(json: String?, gzipped: ByteArray?): WeatherSnapshot? {
        val text = json ?: gzipped?.let(::gunzip) ?: return null
        if (text.length > MAX_JSON_BYTES) throw TooLarge("weather JSON is ${text.length} chars")
        if (nestingDepth(text) > MAX_NESTING) throw TooLarge("weather JSON nests too deep")
        val trimmed = text.trimStart()
        // The gzipped form is an array of locations; only the primary matters.
        val primary = if (trimmed.startsWith("[")) {
            JSONArray(trimmed).optJSONObject(0) ?: return null
        } else {
            JSONObject(trimmed)
        }
        val snapshot = WeatherSnapshot.fromJson(primary)
        return snapshot.copy(
            location = snapshot.location.take(MAX_LOCATION_CHARS),
            hourly = snapshot.hourly.take(MAX_HOURLY),
            daily = snapshot.daily.take(MAX_DAILY),
        )
    }

    /** Inflates through a capped loop, so a gzip bomb never materializes. */
    private fun gunzip(compressed: ByteArray): String {
        val out = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(compressed)).use { stream ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                out.write(buffer, 0, read)
                if (out.size() > MAX_JSON_BYTES) throw TooLarge("weather gzip inflates past the cap")
            }
        }
        return out.toString(Charsets.UTF_8.name())
    }

    /** The deepest bracket nesting outside string literals. */
    internal fun nestingDepth(text: String): Int {
        var depth = 0
        var deepest = 0
        var inString = false
        var escaped = false
        for (char in text) {
            when {
                escaped -> escaped = false
                inString && char == '\\' -> escaped = true
                char == '"' -> inString = !inString
                inString -> Unit
                char == '[' || char == '{' -> {
                    depth += 1
                    if (depth > deepest) deepest = depth
                }
                char == ']' || char == '}' -> depth -= 1
            }
        }
        return deepest
    }
}
