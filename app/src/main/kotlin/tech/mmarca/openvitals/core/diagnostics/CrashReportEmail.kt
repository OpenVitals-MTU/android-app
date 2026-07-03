package tech.mmarca.openvitals.core.diagnostics

internal const val CrashReportEmailRecipient = "manuel@mmarca.tech"

internal data class CrashReportAppInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val buildType: String,
    val androidRelease: String,
    val sdkInt: Int,
    val device: String,
)

internal data class CrashReportDetails(
    val source: String,
    val exceptionClassName: String? = null,
    val exceptionMessage: String? = null,
    val throwClassName: String? = null,
    val throwFileName: String? = null,
    val throwMethodName: String? = null,
    val throwLineNumber: Int? = null,
    val stackTrace: String? = null,
)

internal data class CrashReportEmailDraft(
    val subject: String,
    val body: String,
)

internal object CrashReportEmail {
    private const val MaxEmailSectionChars = 80_000

    fun buildShareText(draft: CrashReportEmailDraft): String = buildString {
        appendLine("To: $CrashReportEmailRecipient")
        appendLine("Subject: ${draft.subject}")
        appendLine()
        append(draft.body)
    }

    fun buildDraft(
        appInfo: CrashReportAppInfo,
        reportDetails: CrashReportDetails?,
        savedCrashReport: String,
        diagnosticsLog: String,
    ): CrashReportEmailDraft {
        val hasCrash = reportDetails != null || savedCrashReport.isNotBlank()
        val subject = if (hasCrash) {
            "OpenVitals crash report"
        } else {
            "OpenVitals issue report"
        }
        return CrashReportEmailDraft(
            subject = subject,
            body = buildString {
                appendLine("OpenVitals report")
                appendLine()
                appendLine("Steps to reproduce:")
                appendLine("1. ")
                appendLine("2. ")
                appendLine("3. ")
                appendLine()
                appendLine("What happened:")
                appendLine()
                appendLine("What you expected:")
                appendLine()
                appendLine("Can Manuel follow up with questions? yes/no")
                appendLine()
                appendLine("App/device:")
                appendAppInfo(appInfo)
                appendLine()
                appendLine("Crash details:")
                if (reportDetails != null) {
                    appendCrashDetails(reportDetails)
                } else {
                    appendLine("No Android crash report was included with this email draft.")
                }
                if (savedCrashReport.isNotBlank()) {
                    appendLine()
                    appendLine("Last saved OpenVitals crash report:")
                    appendLine(savedCrashReport.emailSection())
                }
                appendLine()
                appendLine("Current sanitized diagnostics log:")
                appendLine(diagnosticsLog.ifBlank { "No diagnostics log available." }.emailSection())
            },
        )
    }

    fun buildStoredCrashReport(
        appInfo: CrashReportAppInfo,
        reportDetails: CrashReportDetails,
        threadName: String,
        diagnosticsLog: String,
        capturedAt: String,
    ): String = buildString {
        appendLine("OpenVitals saved crash report")
        appendLine("capturedAt=$capturedAt")
        appendLine("thread=$threadName")
        appendLine()
        appendLine("App/device:")
        appendAppInfo(appInfo)
        appendLine()
        appendLine("Crash details:")
        appendCrashDetails(reportDetails)
        appendLine()
        appendLine("Sanitized diagnostics log:")
        appendLine(diagnosticsLog.ifBlank { "No diagnostics log available." }.emailSection())
    }

    private fun StringBuilder.appendAppInfo(appInfo: CrashReportAppInfo) {
        appendLine("- Package: ${appInfo.packageName}")
        appendLine("- Version: ${appInfo.versionName} (${appInfo.versionCode})")
        appendLine("- Build type: ${appInfo.buildType}")
        appendLine("- Android: ${appInfo.androidRelease} (SDK ${appInfo.sdkInt})")
        appendLine("- Device: ${appInfo.device}")
    }

    private fun StringBuilder.appendCrashDetails(details: CrashReportDetails) {
        appendLine("- Source: ${details.source}")
        appendLine("- Exception: ${details.exceptionClassName.orMissing()}")
        appendLine("- Message: ${details.exceptionMessage.orMissing()}")
        appendLine("- Throw site: ${details.throwSite()}")
        appendLine()
        appendLine("Stack trace:")
        appendLine(details.stackTrace.orMissing().emailSection())
    }

    private fun CrashReportDetails.throwSite(): String {
        val className = throwClassName.orEmpty()
        val methodName = throwMethodName.orEmpty()
        val fileName = throwFileName.orEmpty()
        val line = throwLineNumber?.takeIf { it > 0 }?.toString().orEmpty()
        val location = when {
            fileName.isNotBlank() && line.isNotBlank() -> "$fileName:$line"
            fileName.isNotBlank() -> fileName
            else -> ""
        }
        val member = listOf(className, methodName)
            .filter { it.isNotBlank() }
            .joinToString(".")
        return listOf(member, location)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "missing" }
    }

    private fun String?.orMissing(): String = this?.takeIf { it.isNotBlank() } ?: "missing"

    private fun String.emailSection(): String =
        if (length <= MaxEmailSectionChars) {
            this
        } else {
            take(MaxEmailSectionChars) + "\n\n[truncated for email size]"
        }
}
