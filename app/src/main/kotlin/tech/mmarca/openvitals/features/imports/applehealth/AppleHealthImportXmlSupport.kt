package tech.mmarca.openvitals.features.imports.applehealth

import java.io.FilterInputStream
import java.io.InputStream
import java.io.PushbackReader
import java.io.Reader
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
            append(" while reading ")
            append(entryName)
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

/**
 * Repairs the two ways free-text fields (workout notes, device names, clinical titles/descriptions)
 * most often break XML well-formedness in the wild: raw control characters that XML 1.0 forbids as
 * literal characters, and bare `&` that the exporting app never escaped. Both trigger Expat's opaque
 * "not well-formed (invalid token)" failure with no indication of which character was at fault, so
 * this both fixes the common cases in place and keeps enough trailing context to explain the rest.
 */
internal class XmlCharacterSanitizingReader(
    source: Reader,
    private val maxContextChars: Int = 200,
) : Reader() {
    private val source = PushbackReader(source, MaxEntityLookahead)
    private val pending = ArrayDeque<Char>()
    private val context = ArrayDeque<Char>()
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

    /** Approximate text the parser last consumed before failing; useful when a `SAXParseException`'s own position is imprecise. */
    fun recentContext(): String = context.joinToString(separator = "") { it.toDisplayable() }

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
        context.addLast(c)
        if (context.size > maxContextChars) context.removeFirst()
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

private fun Char.toDisplayable(): String =
    if (code in 0x20..0x7E || this == '\n' || this == '\t') toString() else "\\u%04x".format(code)

/**
 * Thrown in place of a raw [SAXParseException] once export.xml still fails to parse after character
 * sanitization, carrying the trailing text the parser saw so the failure report names the actual
 * offending content instead of just a line/column pair.
 */
internal class AppleHealthXmlParseException(
    cause: SAXParseException,
    sanitizer: XmlCharacterSanitizingReader,
) : Exception(
    "Apple Health export.xml is not well-formed at ${cause.lineNumber.takeIf { it >= 0 }?.let { "line $it" } ?: sanitizer.currentPosition()}" +
        (cause.columnNumber.takeIf { it >= 0 }?.let { ", column $it" } ?: "") +
        ": ${cause.message}. Text leading up to the error: \"${sanitizer.recentContext()}\"" +
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
