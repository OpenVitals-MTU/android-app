package tech.mmarca.openvitals.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacySafeDebugLogExporterTest {
    @Test
    fun `export keeps identifiers dates and unrelated tags`() {
        val line = "D/OkHttp: GET https://example.test/user@example.com date=2026-06-27 " +
            "peer AA:BB:CC:DD:EE:FF raw payload"

        assertEquals(line, PrivacySafeDebugLogExporter.sanitizeLogLine(line))
    }

    @Test
    fun `export keeps Apple Health importer lines unchanged`() {
        val error = "E/AppleHealthImporter: failed uri=content://example/export.xml email=user@example.com"

        assertEquals(error, PrivacySafeDebugLogExporter.sanitizeLogLine(error))
    }

    @Test
    fun `exportLogcat keeps every raw line`() {
        val result = PrivacySafeDebugLogExporter.exportLogcat(
            listOf(
                "D/ActivityRepository: Skipping loadDailySteps missingCount=1",
                "D/OkHttp: GET https://example.test/user@example.com",
                "I/RandomTag: location token secret raw payload",
                "garbage without level prefix",
            ),
        )

        assertEquals(4, result.writtenLines)
        assertEquals(0, result.droppedLines)
        assertEquals(4, result.lines.size)
        assertTrue(result.lines.any { it.contains("OkHttp") })
        assertTrue(result.lines.any { it.contains("garbage without level prefix") })
    }

    @Test
    fun `exportLogcat caps output at maxLines keeping the most recent lines`() {
        val maxLines = PrivacySafeDebugLogExporter.MaxLines
        val lines = (0 until maxLines + 100).map { "I/OpenVitalsX: line $it" }

        val result = PrivacySafeDebugLogExporter.exportLogcat(lines)

        assertEquals(maxLines, result.writtenLines)
        assertEquals(maxLines, result.lines.size)
        assertEquals(100, result.droppedLines)
        assertEquals("I/OpenVitalsX: line 100", result.lines.first())
        assertEquals("I/OpenVitalsX: line ${maxLines + 99}", result.lines.last())
    }

    @Test
    fun `sanitizeLogLine drops only blank lines`() {
        assertEquals(null, PrivacySafeDebugLogExporter.sanitizeLogLine("   "))
        assertEquals("I/OpenVitalsX: kept", PrivacySafeDebugLogExporter.sanitizeLogLine("I/OpenVitalsX: kept"))
    }
}
