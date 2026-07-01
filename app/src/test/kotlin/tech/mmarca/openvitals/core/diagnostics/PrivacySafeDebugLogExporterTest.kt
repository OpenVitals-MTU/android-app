package tech.mmarca.openvitals.core.diagnostics

import org.junit.Assert.assertEquals
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
}
