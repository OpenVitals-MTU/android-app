package tech.mmarca.openvitals.core.diagnostics

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import tech.mmarca.openvitals.core.privacy.PrivacySentinels

class CrashReportEmailTest {
    @Test
    fun `crash draft includes the reproduction template, the saved report and the log`() {
        val draft = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            savedCrashReport = "saved crash stack",
            diagnosticsLog = "masked log lines",
        )

        assertThat(draft.subject).isEqualTo("OpenVitals crash report")
        assertThat(draft.body).contains("Steps to reproduce:")
        assertThat(draft.body).contains("What happened:")
        assertThat(draft.body).contains("Last saved OpenVitals crash report:\nsaved crash stack")
        assertThat(draft.body).contains("Please don't add health data")
        assertThat(draft.body).contains("Current diagnostics log (masked):\nmasked log lines")
    }

    @Test
    fun `issue draft without crash uses issue subject and keeps template`() {
        val draft = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            savedCrashReport = "",
            diagnosticsLog = "",
        )

        assertThat(draft.subject).isEqualTo("OpenVitals issue report")
        assertThat(draft.body).contains("No crash in the last seven days.")
        assertThat(draft.body).contains("No diagnostics log available.")
        assertThat(draft.body).contains("Steps to reproduce:")
    }

    @Test
    fun `the log in a draft is the masked export, with its sentinels gone`() {
        val exported = PrivacySafeDebugLogExporter.exportLogcat(
            listOf(
                "10-23 14:22:01.123  1234  5678 W HealthConnectManager: refused ${PrivacySentinels.Email} on ${PrivacySentinels.Date}",
                "10-23 14:22:01.124  1234  5678 D ActivityRepository: Skipping loadDailySteps missingCount=1",
            ),
        )

        val draft = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            savedCrashReport = "",
            diagnosticsLog = PrivacySafeDebugLogExporter.exportText("tech.mmarca.openvitals", exported),
        )

        PrivacySentinels.assertNoneIn(draft.body, except = setOf(PrivacySentinels.Value, PrivacySentinels.Name))
        assertThat(draft.body).contains("W HealthConnectManager: refused")
        assertThat(draft.body).contains("ActivityRepository: Skipping loadDailySteps missingCount=1")
        assertThat(draft.body).contains("writtenLines=2")
    }

    @Test
    fun `stored crash report includes app info, thread, the crash and the log`() {
        val report = CrashReportEmail.buildStoredCrashReport(
            appInfo = testAppInfo(),
            reportDetails = crashReportDetails(IllegalStateException("boom"), source = "test"),
            threadName = "main",
            diagnosticsLog = "masked log lines",
            capturedAt = "2026-07-03T00:00:00Z",
        )

        assertThat(report).contains("capturedAt=2026-07-03T00:00:00Z")
        assertThat(report).contains("thread=main")
        assertThat(report).contains("Version: 1.7.4-nightly (107030328)")
        assertThat(report).contains("- Exception: java.lang.IllegalStateException")
        assertThat(report).contains("Diagnostics log at the crash (masked):\nmasked log lines")
    }

    @Test
    fun `a crash carrying health data and identifiers is stored without any of them`() {
        val root = IllegalArgumentException("${PrivacySentinels.Sentence}\nand a second line by ${PrivacySentinels.Name}")
        val crash = RuntimeException("while saving ${PrivacySentinels.Date}", root)

        val report = CrashReportEmail.buildStoredCrashReport(
            appInfo = testAppInfo(),
            reportDetails = crashReportDetails(crash, source = "test"),
            threadName = "main",
            diagnosticsLog = "",
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
            diagnosticsLog = "",
            anrReports = "- At: 2026-09-18T19:51:02Z",
        )
        val without = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            savedCrashReport = "",
            diagnosticsLog = "",
        )

        assertThat(withRecords.body).contains("- At: 2026-09-18T19:51:02Z")
        assertThat(without.body).contains("records (stack traces only):\nnone")
    }

    @Test
    fun `draft carries the Health Connect call counts, or says there are none`() {
        val withCounts = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            savedCrashReport = "",
            diagnosticsLog = "",
            healthConnectCalls = "total: started=84 ok=61 failed=0 rateLimited=2 skipped=21",
        )
        val without = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            savedCrashReport = "",
            diagnosticsLog = "",
        )

        assertThat(withCounts.body).contains("Health Connect calls this process (counts only):\ntotal: started=84")
        assertThat(without.body).contains("(counts only):\nnone")
    }

    @Test
    fun `share text includes recipient subject and body`() {
        val draft = CrashReportEmail.buildDraft(
            appInfo = testAppInfo(),
            savedCrashReport = "",
            diagnosticsLog = "",
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
