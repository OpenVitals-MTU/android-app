package tech.mmarca.openvitals.features.imports.applehealth

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleHealthImportErrorFormatterTest {

    @Test
    fun `summary includes exception type when message is missing`() {
        val summary = AppleHealthImportErrorFormatter.summary(RuntimeException())

        assertEquals("java.lang.RuntimeException", summary)
    }

    @Test
    fun `details includes exception stack trace and cause`() {
        val details = AppleHealthImportErrorFormatter.details(
            IllegalStateException("Bad export zip", IllegalArgumentException("Missing export.xml")),
        )

        assertTrue(details.contains("java.lang.IllegalStateException: Bad export zip"))
        assertTrue(details.contains("Caused by: java.lang.IllegalArgumentException: Missing export.xml"))
    }

    @Test
    fun `isPermissionDenied is true for a direct SecurityException`() {
        assertTrue(AppleHealthImportErrorFormatter.isPermissionDenied(SecurityException("Permission Denial")))
    }

    @Test
    fun `isPermissionDenied is true when SecurityException is a wrapped cause`() {
        val error = RuntimeException("Import failed", SecurityException("Permission Denial"))

        assertTrue(AppleHealthImportErrorFormatter.isPermissionDenied(error))
    }

    @Test
    fun `isPermissionDenied is false for unrelated errors`() {
        assertFalse(AppleHealthImportErrorFormatter.isPermissionDenied(IllegalStateException("Bad export zip")))
    }

    @Test
    fun `report store round-trips the last report and failure via its file store`() {
        val filesDir = Files.createTempDirectory("apple-health-report-store").toFile()
        val context = mockk<Context>()
        every { context.filesDir } returns filesDir

        // Nothing was ever written, so both reads default to empty.
        assertEquals("", AppleHealthImportReportStore.read(AppleHealthImportReportStore.reportPath(context)))
        assertEquals("", AppleHealthImportReportStore.read(null))

        val reportPath = AppleHealthImportReportStore.write(context, "hello report")
        val failurePath = AppleHealthImportReportStore.writeFailure(context, "boom")

        assertEquals(AppleHealthImportReportStore.reportPath(context), reportPath)
        assertEquals("hello report", AppleHealthImportReportStore.read(reportPath))
        assertEquals("boom", AppleHealthImportReportStore.read(failurePath))
    }
}
