package tech.mmarca.openvitals.features.imports.applehealth

import java.io.FilterInputStream
import java.io.InputStream
import java.io.PushbackReader
import java.io.Reader
import tech.mmarca.openvitals.core.privacy.PrivacyRedactor
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.SAXParseException

/** Hardened SAX factory shared by the export and workout-route parsers (no external entities/DTDs). */
internal fun secureSaxParserFactory(): SAXParserFactory =
    SAXParserFactory.newInstance().apply {
        isNamespaceAware = false
        setFeatureIfSupported("http://xml.org/sax/features/external-general-entities", false)
        setFeatureIfSupported("http://xml.org/sax/features/external-parameter-entities", false)
        setFeatureIfSupported("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        setFeatureIfSupported("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
    }

private fun SAXParserFactory.setFeatureIfSupported(feature: String, enabled: Boolean) {
    runCatching { setFeature(feature, enabled) }
}

internal fun Attributes.value(name: String): String? = getValue(name)?.takeIf { it.isNotBlank() }

/** Prevents the SAX parser from closing the underlying ZipInputStream when streaming an entry. */
internal class NonClosingInputStream(delegate: InputStream) : FilterInputStream(delegate) {
    override fun close() = Unit
}

internal class CountingInputStream(delegate: InputStream) : FilterInputStream(delegate) {
    var bytesRead: Long = 0
        private set

    override fun read(): Int {
        val value = super.read()
        if (value != -1) bytesRead++
        return value
    }

    override fun read(
        b: ByteArray,
        off: Int,
        len: Int,
    ): Int {
        val count = super.read(b, off, len)
        if (count > 0) bytesRead += count
        return count
    }
}

internal class AppleHealthZipReadException(
    val entryName: String?,
    val decompressedBytesRead: Long?,
    cause: Throwable,
) : Exception(
    buildString {
        append("Apple Health export.zip ended unexpectedly")
        if (!entryName.isNullOrBlank()) {
            // By kind, not by name: a route file's name carries the workout's date.
            append(" while reading ")
            append(describeZipEntry(entryName))
        }
        if (decompressedBytesRead != null) {
            append(" after ")
            append(decompressedBytesRead)
            append(" decompressed byte(s)")
        }
        append(". The selected ZIP is likely incomplete, corrupt, not fully downloaded, or Android stopped providing the document stream. ")
        append("Re-copy or re-export the Apple Health ZIP, make sure it is stored locally on the phone, or extract export.xml and import that file directly.")
    },
    cause,
)

/** What a ZIP entry is, for error text that must not carry its name. */
internal fun describeZipEntry(entryName: String): String = when {
    entryName.isAppleHealthExportXml() -> "export.xml"
    entryName.isAppleWorkoutRouteFile() -> "a workout route file"
    "clinical-records/" in entryName -> "a clinical record file"
    else -> "a ZIP entry"
}

/**
 * Repairs the two common well-formedness breaks in free-text fields: raw
 * control characters and bare `&`. Tracks the position to explain the rest;
 * never the text, which is the user's records.
 */
internal class XmlCharacterSanitizingReader(
    source: Reader,
) : Reader() {
    private val source = PushbackReader(source, MaxEntityLookahead)
    private val pending = ArrayDeque<Char>()
    private var line = 1
    private var column = 0

    var strippedControlChars = 0
        private set
    var escapedAmpersands = 0
        private set

    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        var n = 0
        while (n < len) {
            val c = nextChar() ?: break
            cbuf[off + n] = c
            n++
        }
        return if (n == 0) -1 else n
    }

    fun currentPosition(): String = "line $line, column $column"

    override fun close() = source.close()

    private fun nextChar(): Char? {
        while (true) {
            if (pending.isNotEmpty()) return trackAndReturn(pending.removeFirst())
            val raw = source.read()
            if (raw == -1) return null
            val c = raw.toChar()
            when {
                c.isDisallowedXmlChar() -> strippedControlChars++
                c == '&' && !isEntityReferenceAhead() -> {
                    escapedAmpersands++
                    pending += "amp;".toList()
                    return trackAndReturn(c)
                }
                else -> return trackAndReturn(c)
            }
        }
    }

    private fun trackAndReturn(c: Char): Char {
        if (c == '\n') {
            line++
            column = 0
        } else {
            column++
        }
        return c
    }

    /** Peeks past a just-read `&` (without consuming) to check whether it starts a valid entity/character reference. */
    private fun isEntityReferenceAhead(): Boolean {
        val buffer = StringBuilder()
        var terminated = false
        while (buffer.length < MaxEntityLookahead) {
            val next = source.read()
            if (next == -1) break
            val ch = next.toChar()
            buffer.append(ch)
            if (ch == ';') {
                terminated = true
                break
            }
            if (ch != '#' && !ch.isLetterOrDigit()) break
        }
        if (buffer.isNotEmpty()) source.unread(buffer.toString().toCharArray())
        return terminated && buffer.toString().isValidXmlEntityBody()
    }

    private companion object {
        const val MaxEntityLookahead = 12
    }
}

private fun Char.isDisallowedXmlChar(): Boolean {
    val codePoint = code
    return codePoint in 0x00..0x08 ||
        codePoint == 0x0B ||
        codePoint == 0x0C ||
        codePoint in 0x0E..0x1F ||
        codePoint == 0xFFFE ||
        codePoint == 0xFFFF
}

private fun String.isValidXmlEntityBody(): Boolean {
    val body = removeSuffix(";")
    return when {
        body in NamedXmlEntities -> true
        body.startsWith("#x") || body.startsWith("#X") ->
            body.drop(2).let { it.isNotEmpty() && it.all { ch -> ch.isDigit() || ch in 'a'..'f' || ch in 'A'..'F' } }
        body.startsWith("#") -> body.drop(1).let { it.isNotEmpty() && it.all(Char::isDigit) }
        else -> false
    }
}

private val NamedXmlEntities = setOf("amp", "lt", "gt", "quot", "apos")

/** Thrown when export.xml still fails after sanitization: where it failed and the parser's reason, masked. */
internal class AppleHealthXmlParseException(
    cause: SAXParseException,
    sanitizer: XmlCharacterSanitizingReader,
) : Exception(
    "Apple Health export.xml is not well-formed at ${cause.lineNumber.takeIf { it >= 0 }?.let { "line $it" } ?: sanitizer.currentPosition()}" +
        (cause.columnNumber.takeIf { it >= 0 }?.let { ", column $it" } ?: "") +
        ": ${PrivacyRedactor.maskMessage(cause.message) ?: "no reason given"}" +
        (
            if (sanitizer.strippedControlChars > 0 || sanitizer.escapedAmpersands > 0) {
                " (already auto-repaired ${sanitizer.strippedControlChars} control character(s) and " +
                    "${sanitizer.escapedAmpersands} unescaped '&' earlier in the file)"
            } else {
                ""
            }
        ),
    cause,
)
