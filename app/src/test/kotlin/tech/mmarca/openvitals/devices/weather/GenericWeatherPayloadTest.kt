package tech.mmarca.openvitals.devices.weather

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Any app on the phone can send this broadcast. A hostile one must cost nothing. */
class GenericWeatherPayloadTest {

    @Test
    fun `a plain JSON payload decodes`() {
        val snapshot = GenericWeatherPayload.decode("""{"location":"Valencia","currentTemp":303}""", null)

        assertEquals("Valencia", snapshot?.location)
        assertEquals(303, snapshot?.currentTempKelvin)
    }

    @Test
    fun `a gzipped list of locations decodes to the first one`() {
        val payload = gzip("""[{"location":"Valencia"},{"location":"Madrid"}]""")

        assertEquals("Valencia", GenericWeatherPayload.decode(null, payload)?.location)
    }

    @Test
    fun `a broadcast with no payload decodes to nothing`() {
        assertNull(GenericWeatherPayload.decode(null, null))
        assertNull(GenericWeatherPayload.decode("[]", null))
    }

    @Test
    fun `a gzip bomb is refused before it fills the heap`() {
        // 64 MB of spaces is about 64 KB of gzip, well under the Binder limit.
        val bomb = gzip(" ".repeat(64 * 1024 * 1024))
        assertTrue(bomb.size < 128 * 1024)

        assertThrows(GenericWeatherPayload.TooLarge::class.java) { GenericWeatherPayload.decode(null, bomb) }
    }

    @Test
    fun `an oversized plain payload is refused`() {
        val huge = """{"location":"${"x".repeat(GenericWeatherPayload.MAX_JSON_BYTES)}"}"""

        assertThrows(GenericWeatherPayload.TooLarge::class.java) { GenericWeatherPayload.decode(huge, null) }
    }

    @Test
    fun `deep nesting is refused instead of overflowing the parser's stack`() {
        val nested = "[".repeat(100_000)

        assertThrows(GenericWeatherPayload.TooLarge::class.java) { GenericWeatherPayload.decode(nested, null) }
    }

    @Test
    fun `brackets inside a string are not nesting`() {
        val json = """{"location":"${"[".repeat(40)} \" {{{"}"""

        assertEquals(1, GenericWeatherPayload.nestingDepth(json))
        assertTrue(GenericWeatherPayload.decode(json, null)!!.location.startsWith("[[["))
    }

    @Test
    fun `long lists and names are cut to what a watch can use`() {
        val hours = (1..500).joinToString(",") { """{"timestamp":$it}""" }
        val days = (1..100).joinToString(",") { """{"minTemp":$it}""" }
        val json = """{"location":"${"y".repeat(5_000)}","hourly":[$hours],"forecasts":[$days]}"""

        val snapshot = GenericWeatherPayload.decode(json, null)!!

        assertEquals(GenericWeatherPayload.MAX_HOURLY, snapshot.hourly.size)
        assertEquals(GenericWeatherPayload.MAX_DAILY, snapshot.daily.size)
        assertEquals(GenericWeatherPayload.MAX_LOCATION_CHARS, snapshot.location.length)
    }

    private fun gzip(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(text.toByteArray(Charsets.UTF_8)) }
        return out.toByteArray()
    }
}
