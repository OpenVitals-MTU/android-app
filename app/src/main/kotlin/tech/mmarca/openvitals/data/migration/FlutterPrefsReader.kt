package tech.mmarca.openvitals.data.migration

import android.content.Context
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.io.ObjectStreamClass
import java.util.Base64

/**
 * Decodes the value encodings of the `shared_preferences_android` plugin
 * (verified against version 2.4.26, the one the Flutter build shipped with).
 *
 * The plugin stores every Dart value in a regular [android.content.SharedPreferences]
 * file, but only four native types survive the trip untouched:
 *
 * * Dart `bool`   -> `Boolean`
 * * Dart `int`    -> `Long`
 * * Dart `String` -> `String`
 * * Dart `double` -> `String` prefixed with [DOUBLE_PREFIX]
 * * Dart `List<String>` -> `String` prefixed with [LIST_PREFIX] followed by
 *   Base64(Java-serialized `ArrayList<String>`), or — newer plugin versions —
 *   [JSON_LIST_PREFIX] followed by a JSON array of strings. Both forms are
 *   decoded here because both can be on disk at once.
 *
 * Everything in this object is pure JVM code (no Android classes) so the
 * decoders are unit-testable without Robolectric.
 */
object FlutterPrefsCodec {

    /** Base64 of "This is the prefix for Double." — the plugin's double marker. */
    const val DOUBLE_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBEb3VibGUu"

    /** Base64 of "This is the prefix for a list." — the plugin's list marker. */
    const val LIST_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBhIGxpc3Qu"

    /**
     * The newer JSON list marker. `!` cannot appear in Base64 output, so the
     * plugin uses it to tell the two list encodings apart.
     */
    const val JSON_LIST_PREFIX = "$LIST_PREFIX!"

    /**
     * Decodes one raw preference value into `Boolean | Long | Double | String |
     * List<String>`, or null when the value cannot be decoded (the caller
     * should skip and log the key).
     */
    fun decode(raw: Any?): Any? =
        when (raw) {
            is Boolean -> raw
            is Int -> raw.toLong()
            is Long -> raw
            is Float -> raw.toDouble()
            is String -> decodeString(raw)
            else -> null
        }

    /** Decodes a stored string, resolving the plugin's prefix markers. */
    fun decodeString(raw: String): Any? =
        when {
            raw.startsWith(JSON_LIST_PREFIX) -> decodeJsonStringList(raw.substring(JSON_LIST_PREFIX.length))
            raw.startsWith(LIST_PREFIX) -> decodeJavaSerializedStringList(raw.substring(LIST_PREFIX.length))
            raw.startsWith(DOUBLE_PREFIX) -> raw.substring(DOUBLE_PREFIX.length).toDoubleOrNull()
            else -> raw
        }

    /**
     * Decodes the plugin's classic list form: Base64 (with possible line
     * breaks — the plugin encodes with `Base64.DEFAULT`) of a Java-serialized
     * `ArrayList<String>`. Deserialization is restricted to the same class
     * allowlist the plugin itself uses, so a forged payload cannot instantiate
     * arbitrary classes. Returns null on any decode failure.
     */
    fun decodeJavaSerializedStringList(base64: String): List<String>? =
        try {
            val bytes = Base64.getMimeDecoder().decode(base64)
            val stream = object : ObjectInputStream(ByteArrayInputStream(bytes)) {
                override fun resolveClass(desc: ObjectStreamClass): Class<*> {
                    val allowed = setOf(
                        "java.util.ArrayList",
                        "java.util.Arrays\$ArrayList",
                        "java.lang.String",
                        "[Ljava.lang.String;",
                    )
                    if (desc.name !in allowed) throw ClassNotFoundException(desc.name)
                    return super.resolveClass(desc)
                }
            }
            (stream.readObject() as List<*>).filterIsInstance<String>()
        } catch (_: Exception) {
            null
        }

    /**
     * Decodes the plugin's newer list form: a JSON array of strings.
     *
     * Hand-rolled instead of `org.json` because `org.json` is only a stub on
     * the local-test JVM and this must stay unit-testable without Robolectric.
     * Handles the standard JSON string escapes; anything else (non-string
     * elements, malformed input) yields null.
     */
    fun decodeJsonStringList(json: String): List<String>? {
        var index = 0

        fun skipWhitespace() {
            while (index < json.length && json[index].isWhitespace()) index++
        }

        fun parseString(): String? {
            if (index >= json.length || json[index] != '"') return null
            index++
            val builder = StringBuilder()
            while (index < json.length) {
                when (val current = json[index]) {
                    '"' -> {
                        index++
                        return builder.toString()
                    }
                    '\\' -> {
                        index++
                        if (index >= json.length) return null
                        when (val escaped = json[index]) {
                            '"' -> builder.append('"')
                            '\\' -> builder.append('\\')
                            '/' -> builder.append('/')
                            'b' -> builder.append('\b')
                            'f' -> builder.append('\u000C')
                            'n' -> builder.append('\n')
                            'r' -> builder.append('\r')
                            't' -> builder.append('\t')
                            'u' -> {
                                if (index + 4 >= json.length) return null
                                val hex = json.substring(index + 1, index + 5)
                                val code = hex.toIntOrNull(16) ?: return null
                                builder.append(code.toChar())
                                index += 4
                            }
                            else -> return null
                        }
                        index++
                    }
                    else -> {
                        builder.append(current)
                        index++
                    }
                }
            }
            return null
        }

        skipWhitespace()
        if (index >= json.length || json[index] != '[') return null
        index++
        skipWhitespace()
        if (index < json.length && json[index] == ']') return emptyList()

        val result = mutableListOf<String>()
        while (true) {
            skipWhitespace()
            val element = parseString() ?: return null
            result.add(element)
            skipWhitespace()
            when {
                index < json.length && json[index] == ',' -> index++
                index < json.length && json[index] == ']' -> return result
                else -> return null
            }
        }
    }
}

/**
 * Reads the Flutter build's `FlutterSharedPreferences` file, stripping the
 * plugin's `flutter.` key prefix and decoding the plugin's value encodings via
 * [FlutterPrefsCodec].
 *
 * Read-only: nothing in the migration ever writes (or deletes) a Flutter-era
 * file.
 */
class FlutterPrefsReader(private val context: Context) {

    /**
     * Every decoded entry of the Flutter preferences file. Undecodable values
     * are skipped and logged. Value types: `Boolean | Long | Double | String |
     * List<String>`.
     */
    fun readAll(): Map<String, Any> {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val result = LinkedHashMap<String, Any>()
        for ((rawKey, rawValue) in prefs.all) {
            if (rawKey == null) continue
            val key = rawKey.removePrefix(KEY_PREFIX)
            val decoded = FlutterPrefsCodec.decode(rawValue)
            if (decoded == null) {
                Log.w(TAG, "Skipping undecodable Flutter preference \"$rawKey\".")
                continue
            }
            result[key] = decoded
        }
        return result
    }

    companion object {
        const val PREFS_FILE = "FlutterSharedPreferences"
        private const val KEY_PREFIX = "flutter."
        private const val TAG = "FlutterPrefsReader"
    }
}
