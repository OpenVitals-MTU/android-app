package tech.mmarca.openvitals.wearlink

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class WearLinkLinesTest {

    @Test
    fun `lines round-trip, a trailing carriage return is dropped, the end is null`() {
        val out = ByteArrayOutputStream()
        LineWriter(out).apply {
            line("PING")
            line("SM 1 R 0 16 300 0 - - 0 0 0 1000 3 3 3 90 90 0")
            flush()
        }
        val reader = LineReader(ByteArrayInputStream(out.toByteArray() + "last\r\n".toByteArray()))

        assertEquals("PING", reader.readLine())
        assertEquals("SM 1 R 0 16 300 0 - - 0 0 0 1000 3 3 3 90 90 0", reader.readLine())
        assertEquals("last", reader.readLine())
        assertNull(reader.readLine())
    }

    @Test
    fun `an unterminated final line is still returned`() {
        val reader = LineReader(ByteArrayInputStream("PONG".toByteArray()))

        assertEquals("PONG", reader.readLine())
        assertNull(reader.readLine())
    }

    @Test
    fun `a line over the limit throws instead of growing`() {
        val reader = LineReader(ByteArrayInputStream(("x".repeat(600) + "\n").toByteArray()), maxLineLength = 512)

        assertThrows(LineTooLongException::class.java) { reader.readLine() }
    }

    @Test
    fun `multi-byte characters survive`() {
        val out = ByteArrayOutputStream()
        LineWriter(out).apply { line("OK 4 hr,sm Manu’s Watch8 ✓"); flush() }

        assertEquals("OK 4 hr,sm Manu’s Watch8 ✓", LineReader(ByteArrayInputStream(out.toByteArray())).readLine())
    }
}
