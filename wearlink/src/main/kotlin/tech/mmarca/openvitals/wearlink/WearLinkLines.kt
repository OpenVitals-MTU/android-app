package tech.mmarca.openvitals.wearlink

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * The framing of the link, defined once for both ends: UTF-8 text, one
 * message per line, `\n` terminated, a trailing `\r` tolerated. A line
 * longer than [maxLineLength] bytes is a protocol violation and ends the
 * connection rather than being buffered without bound.
 */
class LineReader(
    private val input: InputStream,
    private val maxLineLength: Int = WearLinkProtocol.MAX_LINE_LENGTH,
) {
    private val buffer = ByteArray(maxLineLength)

    /** The next line without its terminator, or null at end of stream. */
    @Throws(IOException::class)
    fun readLine(): String? {
        var length = 0
        while (true) {
            val byte = input.read()
            if (byte < 0) return if (length == 0) null else String(buffer, 0, length, Charsets.UTF_8).trimEnd('\r')
            if (byte == '\n'.code) return String(buffer, 0, length, Charsets.UTF_8).trimEnd('\r')
            if (length == maxLineLength) throw LineTooLongException(maxLineLength)
            buffer[length++] = byte.toByte()
        }
    }
}

/** Writes lines; nothing reaches the peer until [flush]. */
class LineWriter(output: OutputStream) {
    private val out = output.bufferedWriter(Charsets.UTF_8)

    @Throws(IOException::class)
    fun line(text: String) {
        out.write(text)
        out.write("\n")
    }

    @Throws(IOException::class)
    fun flush() = out.flush()
}

class LineTooLongException(limit: Int) : IOException("line longer than $limit bytes")
