package tech.mmarca.openvitals.features.imports.applehealth

import org.junit.Assert.assertEquals
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
}
