package tech.mmarca.openvitals.data.migration

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.Base64
import org.junit.Test

class FlutterPrefsCodecTest {

    // region Scalars

    @Test
    fun `booleans pass through`() {
        assertThat(FlutterPrefsCodec.decode(true)).isEqualTo(true)
        assertThat(FlutterPrefsCodec.decode(false)).isEqualTo(false)
    }

    @Test
    fun `ints and longs normalize to Long`() {
        assertThat(FlutterPrefsCodec.decode(7)).isEqualTo(7L)
        assertThat(FlutterPrefsCodec.decode(7L)).isEqualTo(7L)
        assertThat(FlutterPrefsCodec.decode(1_752_000_000_000L)).isEqualTo(1_752_000_000_000L)
    }

    @Test
    fun `plain strings pass through`() {
        assertThat(FlutterPrefsCodec.decode("metric")).isEqualTo("metric")
        assertThat(FlutterPrefsCodec.decode("")).isEqualTo("")
    }

    @Test
    fun `double marker decodes to Double`() {
        val raw = FlutterPrefsCodec.DOUBLE_PREFIX + "2.5"
        assertThat(FlutterPrefsCodec.decode(raw)).isEqualTo(2.5)
        assertThat(FlutterPrefsCodec.decode(FlutterPrefsCodec.DOUBLE_PREFIX + "-0.75"))
            .isEqualTo(-0.75)
    }

    @Test
    fun `corrupt double payload is skipped`() {
        assertThat(FlutterPrefsCodec.decode(FlutterPrefsCodec.DOUBLE_PREFIX + "not-a-number"))
            .isNull()
    }

    @Test
    fun `unsupported raw types are skipped`() {
        assertThat(FlutterPrefsCodec.decode(null)).isNull()
        assertThat(FlutterPrefsCodec.decode(Any())).isNull()
    }

    // endregion

    // region Java-serialized lists

    @Test
    fun `java serialized list round trips`() {
        val encoded = javaSerializedListPayload(listOf("water|250.0", "café", ""))
        assertThat(FlutterPrefsCodec.decode(encoded))
            .isEqualTo(listOf("water|250.0", "café", ""))
    }

    @Test
    fun `java serialized list with base64 line wraps decodes`() {
        // android.util.Base64.DEFAULT (what the plugin encodes with) wraps
        // lines every 76 chars; a long list forces at least one wrap.
        val values = (1..40).map { "element_number_$it" }
        val encoded = javaSerializedListPayload(values, wrapLines = true)
        assertThat(encoded).contains("\n")
        assertThat(FlutterPrefsCodec.decode(encoded)).isEqualTo(values)
    }

    @Test
    fun `empty java serialized list decodes`() {
        assertThat(FlutterPrefsCodec.decode(javaSerializedListPayload(emptyList())))
            .isEqualTo(emptyList<String>())
    }

    @Test
    fun `corrupt java serialized list is skipped`() {
        assertThat(FlutterPrefsCodec.decode(FlutterPrefsCodec.LIST_PREFIX + "%%%not-base64%%%"))
            .isNull()
        val truncated = javaSerializedListPayload(listOf("a", "b")).dropLast(8)
        assertThat(FlutterPrefsCodec.decode(truncated)).isNull()
    }

    @Test
    fun `java deserialization refuses arbitrary classes`() {
        val byteStream = ByteArrayOutputStream()
        ObjectOutputStream(byteStream).use { it.writeObject(java.util.Date(0)) }
        val payload = FlutterPrefsCodec.LIST_PREFIX +
            Base64.getEncoder().encodeToString(byteStream.toByteArray())
        assertThat(FlutterPrefsCodec.decode(payload)).isNull()
    }

    // endregion

    // region JSON lists

    @Test
    fun `json list variant decodes`() {
        val raw = FlutterPrefsCodec.JSON_LIST_PREFIX + """["a","b c","d,e"]"""
        assertThat(FlutterPrefsCodec.decode(raw)).isEqualTo(listOf("a", "b c", "d,e"))
    }

    @Test
    fun `json list decodes escapes and unicode`() {
        val raw = FlutterPrefsCodec.JSON_LIST_PREFIX +
            """[ "he said \"hi\"" , "tab\there", "café", "back\\slash" ]"""
        assertThat(FlutterPrefsCodec.decode(raw))
            .isEqualTo(listOf("he said \"hi\"", "tab\there", "café", "back\\slash"))
    }

    @Test
    fun `empty json list decodes`() {
        assertThat(FlutterPrefsCodec.decode(FlutterPrefsCodec.JSON_LIST_PREFIX + "[]"))
            .isEqualTo(emptyList<String>())
        assertThat(FlutterPrefsCodec.decode(FlutterPrefsCodec.JSON_LIST_PREFIX + " [ ] "))
            .isEqualTo(emptyList<String>())
    }

    @Test
    fun `malformed json list is skipped`() {
        val prefix = FlutterPrefsCodec.JSON_LIST_PREFIX
        assertThat(FlutterPrefsCodec.decode(prefix + "not json")).isNull()
        assertThat(FlutterPrefsCodec.decode(prefix + """["unterminated""")).isNull()
        assertThat(FlutterPrefsCodec.decode(prefix + """[1,2]""")).isNull()
        assertThat(FlutterPrefsCodec.decode(prefix + """["a",]""")).isNull()
    }

    // endregion

    private fun javaSerializedListPayload(
        values: List<String>,
        wrapLines: Boolean = false,
    ): String {
        val byteStream = ByteArrayOutputStream()
        ObjectOutputStream(byteStream).use { it.writeObject(ArrayList(values)) }
        val encoder =
            if (wrapLines) Base64.getMimeEncoder(76, "\n".toByteArray()) else Base64.getEncoder()
        return FlutterPrefsCodec.LIST_PREFIX + encoder.encodeToString(byteStream.toByteArray())
    }
}
