package tech.mmarca.openvitals.core.privacy

import java.util.Collections
import java.util.IdentityHashMap

/**
 * Masks what must not leave the phone in text the user can copy, save or send:
 * crash drafts, the diagnostics log, and the error text inside import and sync
 * reports. Structure stays (class names, stack frames, record types, counts);
 * identifiers, addresses, file locations, dates and, in error messages, numbers go.
 *
 * A pattern cannot recognise a name or free text, so reports print structure
 * only and never hand this object a value to clean up.
 */
object PrivacyRedactor {
    const val Placeholder = "[redacted]"
    const val NumberPlaceholder = "#"

    private const val MaxMessageChars = 500
    private const val MaxShapeChars = 32
    private const val BluetoothBaseUuidSuffix = "-0000-1000-8000-00805f9b34fb"

    private val uri = Regex("""(?i)\b(?:content|file|https?|ftp)://\S+|\b(?:mailto|tel|sms|geo):[^\s,;]+""")
    private val userPath = Regex(
        """/(?:storage/(?:emulated/\d+|self/primary|[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4})|sdcard|mnt/(?:sdcard|media_rw|user)""" +
            """|data/(?:user(?:_de)?/\d+|data|media/\d+))(?:/\S*)?""",
    )
    private val email = Regex("""(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b""")
    private val bearer = Regex("""(?i)\bBearer\s+\S+""")
    private val keyValueId = Regex(
        """(?i)\b(clientRecordId|recordId|deviceId|widgetId|workId|uuid|uid|token|access[_-]?token|refresh[_-]?token""" +
            """|secret|password|passwd|api[_-]?key|authorization|serial(?:Number)?|address|mac|lat|lon|lng|latitude""" +
            """|longitude|deviceName|displayName|bluetoothName|sourceName|fileName|path|uri)(\s*[=:]\s*)""" +
            """(?!["']?\Q$Placeholder\E)("[^"]*"|'[^']*'|[^\s,;&)}\]]+)""",
    )
    private val mac = Regex("""\b[0-9A-Fa-f]{2}(?:[:-][0-9A-Fa-f]{2}){5}\b""")
    private val uuid = Regex("""\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b""")
    private val coordinates = Regex("""[-+]?\d{1,3}\.\d{3,}\s*[,;/ ]\s*[-+]?\d{1,3}\.\d{3,}""")
    private val dateTime = Regex(
        """\b\d{4}-\d{2}-\d{2}(?:[T ]\d{2}:\d{2}(?::\d{2}(?:[.,]\d+)?)?(?: ?(?:Z|[+-]\d{2}:?\d{2}))?(?:\[[\w/+\-]+])?)?""",
    )
    private val numericDate = Regex("""\b\d{1,2}/\d{1,2}/\d{2,4}\b|\b\d{1,2}\.\d{1,2}\.\d{4}\b""")
    private val epoch = Regex("""\b1\d{9}(?:\d{3})?\b""")
    private val timeOfDay = Regex("""\b\d{1,2}:\d{2}(?::\d{2}(?:\.\d+)?)?\b""")
    private val phone = Regex("""(?<![\w-])\+?\d[\d .()\-]{7,}\d(?![\w-])""")
    private val longHex = Regex("""\b[0-9a-fA-F]{16,}\b""")
    private val opaqueToken = Regex("""(?<![\w+/=-])[A-Za-z0-9+/_=-]{24,}(?![\w+/=-])""")

    // A position in a file or a run survives; any other number in a message is a value.
    private val number = Regex("""(?i)\b(?:line|column|col|row|offset|index|entry|position|attempt|batch)\s+\d+|(?<![A-Za-z_\d])\d+(?:[.,]\d+)*""")

    /** Masks identifiers, addresses, file locations, dates and times. Plain numbers stay. For log messages. */
    fun maskIdentifiers(text: String): String {
        var masked = text
        // Keyed values first, so a quoted value goes whole.
        masked = masked.replace(keyValueId) { "${it.groupValues[1]}${it.groupValues[2]}$Placeholder" }
        masked = masked.replace(uri, Placeholder)
        masked = masked.replace(userPath, Placeholder)
        masked = masked.replace(email, Placeholder)
        masked = masked.replace(bearer, "Bearer $Placeholder")
        masked = masked.replace(mac, Placeholder)
        masked = masked.replace(uuid) { if (it.value.isBluetoothServiceUuid()) it.value else Placeholder }
        masked = masked.replace(coordinates, Placeholder)
        masked = masked.replace(dateTime, Placeholder)
        masked = masked.replace(numericDate, Placeholder)
        masked = masked.replace(epoch, Placeholder)
        masked = masked.replace(timeOfDay, Placeholder)
        masked = masked.replace(phone, Placeholder)
        masked = masked.replace(longHex) { if (it.value.any(Char::isDigit)) Placeholder else it.value }
        masked = masked.replace(opaqueToken) { if (it.value.looksLikeToken()) Placeholder else it.value }
        return masked
    }

    /**
     * An exception message: what [maskIdentifiers] masks, and every number but a
     * position ("line 3", "column 15", "row 7"). Only the first line, capped.
     * Null for a missing or blank message.
     */
    fun maskMessage(message: String?): String? {
        val firstLine = message?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim() ?: return null
        val masked = maskIdentifiers(firstLine)
            .replace(number) { if (it.value.first().isLetter()) it.value else NumberPlaceholder }
        return if (masked.length <= MaxMessageChars) masked else masked.take(MaxMessageChars) + "…"
    }

    /** "fully.qualified.Class: masked message", or the class alone. */
    fun describe(throwable: Throwable): String {
        val message = maskMessage(throwable.localizedMessage)
        return if (message == null) throwable.javaClass.name else "${throwable.javaClass.name}: $message"
    }

    /**
     * The stack trace as `printStackTrace` prints it, built from the throwable
     * itself so a message's later lines never appear: each header through
     * [describe], frames as they are.
     */
    fun maskThrowable(throwable: Throwable): String = buildString {
        val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        appendThrowable(throwable, caption = "", prefix = "", enclosingTrace = emptyArray(), seen = seen)
    }.trimEnd()

    /** The shape of a cell: digits as 9, letters as a, the rest kept. Says what a cell looked like, never what it said. */
    fun shapeOf(text: String): String {
        val shape = text.trim().map { char ->
            when {
                char.isDigit() -> '9'
                char.isLetter() -> 'a'
                else -> char
            }
        }.joinToString("")
        return if (shape.length <= MaxShapeChars) shape else shape.take(MaxShapeChars) + "…"
    }

    private fun StringBuilder.appendThrowable(
        throwable: Throwable,
        caption: String,
        prefix: String,
        enclosingTrace: Array<StackTraceElement>,
        seen: MutableSet<Throwable>,
    ) {
        if (!seen.add(throwable)) {
            appendLine("$prefix$caption[circular reference: ${throwable.javaClass.name}]")
            return
        }
        appendLine("$prefix$caption${describe(throwable)}")
        val trace = throwable.stackTrace
        var last = trace.size - 1
        var enclosing = enclosingTrace.size - 1
        while (last >= 0 && enclosing >= 0 && trace[last] == enclosingTrace[enclosing]) {
            last--
            enclosing--
        }
        for (index in 0..last) appendLine("$prefix\tat ${trace[index]}")
        val inCommon = trace.size - 1 - last
        if (inCommon != 0) appendLine("$prefix\t... $inCommon more")
        throwable.suppressed.forEach { appendThrowable(it, "Suppressed: ", "$prefix\t", trace, seen) }
        throwable.cause?.let { appendThrowable(it, "Caused by: ", prefix, trace, seen) }
    }

    // Long runs of letters alone are type names (HKQuantityTypeIdentifier…); a token mixes in digits.
    private fun String.looksLikeToken(): Boolean =
        count(Char::isDigit) >= 3 && any(Char::isLetter) && !isBluetoothServiceUuid()

    // Bluetooth assigned numbers (heart rate, battery) name a service, not a device.
    private fun String.isBluetoothServiceUuid(): Boolean = endsWith(BluetoothBaseUuidSuffix, ignoreCase = true)
}
