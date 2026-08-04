package tech.mmarca.openvitals.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportEmailTest {
    @Test
    fun `crash draft includes reproduction template crash details and logs`() {
        val draft = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            reportDetails = testCrashDetails(),
            savedCrashReport = "saved crash stack",
            diagnosticsLog = "D/OpenVitals: sanitized log",
        )

        assertEquals("OpenVitals crash report", draft.subject)
        assertTrue(draft.body.contains("Steps to reproduce:"))
        assertTrue(draft.body.contains("What happened:"))
        assertTrue(draft.body.contains("java.lang.IllegalStateException"))
        assertTrue(draft.body.contains("MainActivity.kt:42"))
        assertTrue(draft.body.contains("saved crash stack"))
        assertTrue(draft.body.contains("D/OpenVitals: sanitized log"))
    }

    @Test
    fun `issue draft without crash uses issue subject and keeps template`() {
        val draft = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            reportDetails = null,
            savedCrashReport = "",
            diagnosticsLog = "Diagnostics log unavailable in this build.",
        )

        assertEquals("OpenVitals issue report", draft.subject)
        assertTrue(draft.body.contains("No Android crash report was included"))
        assertTrue(draft.body.contains("Steps to reproduce:"))
        assertTrue(draft.body.contains("Diagnostics log unavailable in this build."))
    }

    @Test
    fun `stored crash report includes app info thread and diagnostics`() {
        val report = CrashReportEmail.buildStoredCrashReport(
            appInfo = testAppInfo(),
            reportDetails = testCrashDetails(),
            threadName = "main",
            diagnosticsLog = "W/OpenVitals: saved before death",
            capturedAt = "2026-07-03T00:00:00Z",
        )

        assertTrue(report.contains("capturedAt=2026-07-03T00:00:00Z"))
        assertTrue(report.contains("thread=main"))
        assertTrue(report.contains("Version: 1.7.4-nightly (107030328)"))
        assertTrue(report.contains("W/OpenVitals: saved before death"))
    }

    @Test
    fun `share text includes recipient subject and body`() {
        val draft = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            reportDetails = null,
            savedCrashReport = "",
            diagnosticsLog = "log",
        )

        val shareText = CrashReportEmail.buildShareText(draft)

        assertTrue(shareText.startsWith("To: manuel@mmarca.tech\nSubject: OpenVitals issue report"))
        assertTrue(shareText.contains("Steps to reproduce:"))
    }

    private fun testAppInfo(): CrashReportAppInfo =
        CrashReportAppInfo(
            packageName = "tech.mmarca.openvitals",
            versionName = "1.7.4-nightly",
            versionCode = 107030328,
            buildType = "nightly",
            androidRelease = "16",
            sdkInt = 36,
            device = "Example Device",
        )

    private fun testCrashDetails(): CrashReportDetails =
        CrashReportDetails(
            source = "Android crash dialog",
            exceptionClassName = "java.lang.IllegalStateException",
            exceptionMessage = "boom",
            throwClassName = "tech.mmarca.openvitals.MainActivity",
            throwFileName = "MainActivity.kt",
            throwMethodName = "onCreate",
            throwLineNumber = 42,
            stackTrace = "java.lang.IllegalStateException: boom",
        )
}
