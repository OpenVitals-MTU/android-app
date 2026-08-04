package tech.mmarca.openvitals.features.imports.applehealth

import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Direct unit tests for [XmlCharacterSanitizingReader], mirroring Flutter's
 * `apple_health_import_xml_support_test.dart`. The reader is drained through a
 * deliberately tiny buffer so the pending-escape carry between read() calls is
 * exercised the same way Flutter's chunked stream transformer is.
 */
class AppleHealthImportXmlSupportTest {

    private fun sanitize(input: String, bufferSize: Int = 3): Pair<String, XmlCharacterSanitizingReader> {
        val sanitizer = XmlCharacterSanitizingReader(StringReader(input))
        val output = StringBuilder()
        val buffer = CharArray(bufferSize)
        while (true) {
            val read = sanitizer.read(buffer, 0, buffer.size)
            if (read == -1) break
            output.append(buffer, 0, read)
        }
        return output.toString() to sanitizer
    }

    @Test
    fun `escapes a bare ampersand mid-text`() {
        val (out, sanitizer) = sanitize("<a>AT&T</a>")

        assertEquals("<a>AT&amp;T</a>", out)
        assertEquals(1, sanitizer.escapedAmpersands)
        assertEquals(0, sanitizer.strippedControlChars)
    }

    @Test
    fun `does not re-escape a real entity`() {
        val (out, sanitizer) = sanitize("<a>x&amp;y</a>")

        assertEquals("<a>x&amp;y</a>", out)
        assertEquals(0, sanitizer.escapedAmpersands)
    }

    @Test
    fun `escapes a bare ampersand at the very end of the stream`() {
        val (out, sanitizer) = sanitize("<a>x&")

        assertEquals("<a>x&amp;", out)
        assertEquals(1, sanitizer.escapedAmpersands)
    }

    @Test
    fun `numeric and hex character references stay intact`() {
        val (out, sanitizer) = sanitize("<a>&#65;&#x41;</a>")

        assertEquals("<a>&#65;&#x41;</a>", out)
        assertEquals(0, sanitizer.escapedAmpersands)
    }

    @Test
    fun `strips a disallowed control character mid-text`() {
        val bell = '\u0007'
        val (out, sanitizer) = sanitize("<a>Notes${bell}App</a>")

        assertEquals("<a>NotesApp</a>", out)
        assertEquals(1, sanitizer.strippedControlChars)
        assertEquals(0, sanitizer.escapedAmpersands)
    }

    @Test
    fun `recentContext reports the trailing emitted text`() {
        val (_, sanitizer) = sanitize("<a>hello world</a>")

        assertTrue(sanitizer.recentContext().endsWith("world</a>"))
    }
}
