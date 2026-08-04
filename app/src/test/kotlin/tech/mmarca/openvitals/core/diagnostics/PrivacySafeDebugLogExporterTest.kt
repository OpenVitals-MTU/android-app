package tech.mmarca.openvitals.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacySafeDebugLogExporterTest {
    @Test
    fun `sanitize redacts common identifiers`() {
        val sanitized = PrivacySafeDebugLogExporter.sanitizeLogLine(
            "D/SettingsViewModel: user=a@example.com phone=+1 555 123 4567 " +
                "recordId=123e4567-e89b-12d3-a456-426614174000 date=2026-06-27"
        )

        assertEquals(
            "D/SettingsViewModel: user=[redacted] phone=[redacted] " +
                "recordId=[redacted] date=[redacted]",
            sanitized,
        )
    }

    @Test
    fun `sanitize drops high risk lines`() {
        assertNull(
            PrivacySafeDebugLogExporter.sanitizeLogLine(
                "D/BleGattConnection: HR notify 4 bytes unparsed: raw payload",
            )
        )
        assertNull(
            PrivacySafeDebugLogExporter.sanitizeLogLine(
                "D/SettingsViewModel: imported content://com.android.providers.media.documents/document/1",
            )
        )
    }

    @Test
    fun `sanitize keeps OpenVitals operational lines`() {
        val sanitized = PrivacySafeDebugLogExporter.sanitizeLogLine(
            "W/ActivityRepository: Skipping loadDailySteps missingCount=1",
        )

        assertEquals(
            "W/ActivityRepository: Skipping loadDailySteps missingCount=1",
            sanitized,
        )
    }

    @Test
    fun `sanitize keeps Apple Health importer warnings and errors unsanitized`() {
        val error = "E/AppleHealthImporter: failed uri=content://example/export.xml email=user@example.com"
        val warning = "W/AppleHealthImporter: retrying file:///storage/emulated/0/export.zip"

        assertEquals(error, PrivacySafeDebugLogExporter.sanitizeLogLine(error))
        assertEquals(warning, PrivacySafeDebugLogExporter.sanitizeLogLine(warning))
    }

    @Test
    fun `sanitize excludes unrelated tags`() {
        val result = PrivacySafeDebugLogExporter.sanitizeLogcat(
            listOf(
                "D/ActivityRepository: Skipping loadDailySteps missingCount=1",
                "D/OkHttp: GET https://example.test/user@example.com",
            )
        )

        assertEquals(1, result.writtenLines)
        assertEquals(1, result.droppedLines)
        assertTrue(result.lines.single().contains("ActivityRepository"))
    }

    @Test
    fun `sanitize redacts MAC, email and UUID in an allowed-tag line`() {
        val line = "I/OpenVitalsBle: peer AA:BB:CC:DD:EE:FF user alice@example.com " +
            "session 12345678-1234-1234-1234-123456789abc done"

        val result = PrivacySafeDebugLogExporter.sanitizeLogLine(line)

        assertNotNull(result)
        assertTrue(result!!.contains("[redacted]"))
        assertFalse(result.contains("AA:BB:CC:DD:EE:FF"))
        assertFalse(result.contains("alice@example.com"))
        assertFalse(result.contains("12345678-1234-1234-1234-123456789abc"))
        // Level/tag prefix is preserved.
        assertTrue(result.startsWith("I/OpenVitalsBle: "))
    }

    @Test
    fun `sanitize redacts key=value identifiers keeping the key`() {
        // " token" IS a drop keyword, so the whole line goes rather than the value.
        assertNull(
            PrivacySafeDebugLogExporter.sanitizeLogLine(
                "D/SettingsViewModel: sync token=supersecretvalue ok",
            ),
        )

        assertEquals(
            "D/SettingsViewModel: sync deviceId=[redacted] ok",
            PrivacySafeDebugLogExporter.sanitizeLogLine(
                "D/SettingsViewModel: sync deviceId=abc123 ok",
            ),
        )
    }

    @Test
    fun `sanitize drops a line containing a location keyword`() {
        assertNull(
            PrivacySafeDebugLogExporter.sanitizeLogLine(
                "I/OpenVitalsX: current location update received",
            ),
        )
    }

    @Test
    fun `sanitize drops a line containing a token keyword`() {
        assertNull(
            PrivacySafeDebugLogExporter.sanitizeLogLine(
                "I/OpenVitalsX: refreshed token successfully",
            ),
        )
    }

    @Test
    fun `sanitize handles AppleHealthImporter non-W E A F lines normally`() {
        // Level I is not in {W,E,A,F}; AppleHealthImporter is not an allowed tag
        // by the general rules, so an I line is dropped.
        assertNull(
            PrivacySafeDebugLogExporter.sanitizeLogLine(
                "I/AppleHealthImporter: informational line",
            ),
        )
    }

    @Test
    fun `sanitize drops a non-log-format line`() {
        assertNull(PrivacySafeDebugLogExporter.sanitizeLogLine("not a logcat line"))
    }

    @Test
    fun `sanitize drops a blank message`() {
        assertNull(PrivacySafeDebugLogExporter.sanitizeLogLine("I/OpenVitalsX:   "))
    }

    @Test
    fun `sanitize redacts ISO instants and dates`() {
        val result = PrivacySafeDebugLogExporter.sanitizeLogLine(
            "I/OpenVitalsX: at 2024-01-02T03:04:05Z on 2024-01-02",
        )

        assertNotNull(result)
        assertFalse(result!!.contains("2024-01-02"))
        assertTrue(result.contains("[redacted]"))
    }

    @Test
    fun `sanitizeLogcat caps output at maxLines keeping the most recent lines`() {
        val maxLines = PrivacySafeDebugLogExporter.MaxLines
        val lines = (0 until maxLines + 100).map { "I/OpenVitalsX: line $it" }

        val result = PrivacySafeDebugLogExporter.sanitizeLogcat(lines)

        assertEquals(maxLines, result.writtenLines)
        assertEquals(maxLines, result.lines.size)
        assertEquals(0, result.droppedLines)
        // takeLast semantics: the first 100 lines are dropped from the front.
        assertEquals("I/OpenVitalsX: line 100", result.lines.first())
        assertEquals("I/OpenVitalsX: line ${maxLines + 99}", result.lines.last())
    }

    @Test
    fun `sanitizeLogcat counts dropped lines across the whole input`() {
        val result = PrivacySafeDebugLogExporter.sanitizeLogcat(
            listOf(
                "I/OpenVitalsX: kept one",
                "I/RandomTag: dropped tag",
                "garbage",
                "I/OpenVitalsX: location dropped keyword",
                "I/OpenVitalsX: kept two",
            ),
        )

        assertEquals(2, result.writtenLines)
        assertEquals(3, result.droppedLines)
        assertEquals(
            listOf("I/OpenVitalsX: kept one", "I/OpenVitalsX: kept two"),
            result.lines,
        )
    }
}
