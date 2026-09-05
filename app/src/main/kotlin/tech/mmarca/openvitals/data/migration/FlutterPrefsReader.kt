package tech.mmarca.openvitals.data.migration

import android.content.Context
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.io.ObjectStreamClass
import java.util.Base64

/**
 * Decodes the `shared_preferences_android` plugin's value encodings
 * (2.4.26). Bool, int and String survive as themselves; double is a String
 * with [DOUBLE_PREFIX]; a string list is [LIST_PREFIX] plus Base64 of a
 * Java-serialized ArrayList, or [JSON_LIST_PREFIX] plus a JSON array. Pure
 * JVM, so it is testable without Robolectric.
 */
object FlutterPrefsCodec {

    /** Base64 of "This is the prefix for Double." — the plugin's double marker. */
    const val DOUBLE_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBEb3VibGUu"

    /** Base64 of "This is the prefix for a list." — the plugin's list marker. */
    const val LIST_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBhIGxpc3Qu"

    /** The newer JSON list marker. `!` cannot appear in Base64. */
    const val JSON_LIST_PREFIX = "$LIST_PREFIX!"

    /** Decodes one raw value, or null when it cannot be decoded. */
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
     * Decodes the classic list form. Deserialization is restricted to the
     * plugin's own class allowlist. Null on any failure.
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

    /** Decodes the JSON list form. Hand-rolled: `org.json` is a stub on the test JVM. */
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

/** Reads the Flutter preferences file, stripping the `flutter.` prefix. Read-only. */
class FlutterPrefsReader(private val context: Context) {

    /** Every decoded entry. Undecodable values are skipped and logged. */
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
