package tech.mmarca.openvitals.core.diagnostics

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import tech.mmarca.openvitals.core.privacy.PrivacySentinels

class CrashReportEmailTest {
    @Test
    fun `crash draft includes the reproduction template, the saved report and no log`() {
        val draft = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            savedCrashReport = "saved crash stack",
        )

        assertThat(draft.subject).isEqualTo("OpenVitals crash report")
        assertThat(draft.body).contains("Steps to reproduce:")
        assertThat(draft.body).contains("What happened:")
        assertThat(draft.body).contains("Last saved OpenVitals crash report:\nsaved crash stack")
        assertThat(draft.body).contains("Please don't add health data")
        assertThat(draft.body).doesNotContain("diagnostics log")
    }

    @Test
    fun `issue draft without crash uses issue subject and keeps template`() {
        val draft = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            savedCrashReport = "",
        )

        assertThat(draft.subject).isEqualTo("OpenVitals issue report")
        assertThat(draft.body).contains("No crash in the last seven days.")
        assertThat(draft.body).contains("Steps to reproduce:")
    }

    @Test
    fun `stored crash report includes app info, thread and the crash, and no log`() {
        val report = CrashReportEmail.buildStoredCrashReport(
            appInfo = testAppInfo(),
            reportDetails = crashReportDetails(IllegalStateException("boom"), source = "test"),
            threadName = "main",
            capturedAt = "2026-07-03T00:00:00Z",
        )

        assertThat(report).contains("capturedAt=2026-07-03T00:00:00Z")
        assertThat(report).contains("thread=main")
        assertThat(report).contains("Version: 1.7.4-nightly (107030328)")
        assertThat(report).contains("- Exception: java.lang.IllegalStateException")
        assertThat(report).doesNotContain("diagnostics log")
    }

    @Test
    fun `a crash carrying health data and identifiers is stored without any of them`() {
        val root = IllegalArgumentException("${PrivacySentinels.Sentence}\nand a second line by ${PrivacySentinels.Name}")
        val crash = RuntimeException("while saving ${PrivacySentinels.Date}", root)

        val report = CrashReportEmail.buildStoredCrashReport(
            appInfo = testAppInfo(),
            reportDetails = crashReportDetails(crash, source = "test"),
            threadName = "main",
            capturedAt = "now",
        )

        PrivacySentinels.assertNoneIn(report, except = setOf(PrivacySentinels.Name))
        assertThat(report).doesNotContain("second line")
        assertThat(report).contains("- Exception: java.lang.IllegalArgumentException")
        assertThat(report).contains("- Message: value #")
        assertThat(report).contains("- Throw site: tech.mmarca.openvitals.core.diagnostics.CrashReportEmailTest.")
        assertThat(report).contains("Caused by: java.lang.IllegalArgumentException")
        assertThat(report).contains("\tat tech.mmarca.openvitals.core.diagnostics.CrashReportEmailTest.")
    }

    @Test
    fun `crash details follow the root cause, as the crash dialog does`() {
        val details = crashReportDetails(
            RuntimeException("outer", IllegalStateException("inner 42")),
            source = "test",
        )

        assertThat(details.exceptionClassName).isEqualTo("java.lang.IllegalStateException")
        assertThat(details.exceptionMessage).isEqualTo("inner #")
        assertThat(details.throwMethodName).isNotEmpty()
    }

    @Test
    fun `draft carries the not-responding records, or says there are none`() {
        val withRecords = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            savedCrashReport = "",
            anrReports = "- At: 2026-09-18T19:51:02Z",
        )
        val without = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            savedCrashReport = "",
        )

        assertThat(withRecords.body).contains("- At: 2026-09-18T19:51:02Z")
        assertThat(without.body).contains("records (stack traces only):\nnone")
    }

    @Test
    fun `share text includes recipient subject and body`() {
        val draft = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            savedCrashReport = "",
        )

        val shareText = CrashReportEmail.buildShareText(draft)

        assertThat(shareText).startsWith("To: manuel@mmarca.tech\nSubject: OpenVitals issue report")
        assertThat(shareText).contains("Steps to reproduce:")
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
}
