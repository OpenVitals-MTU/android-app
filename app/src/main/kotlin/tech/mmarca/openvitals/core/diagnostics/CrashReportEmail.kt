package tech.mmarca.openvitals.core.diagnostics

import java.util.Collections
import java.util.IdentityHashMap
import tech.mmarca.openvitals.core.privacy.PrivacyRedactor

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

/**
 * What a crash says about itself, as Android's crash dialog would put it, with
 * the message and the trace masked: the root cause's class and throw site, the
 * deepest non-empty message, the whole trace without any message's later lines.
 */
internal fun crashReportDetails(throwable: Throwable, source: String): CrashReportDetails {
    var root = throwable
    var message = throwable.message
    val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>()).apply { add(throwable) }
    var cause = throwable.cause
    while (cause != null && seen.add(cause)) {
        if (cause.stackTrace.isNotEmpty()) root = cause
        if (!cause.message.isNullOrEmpty()) message = cause.message
        cause = cause.cause
    }
    val site = root.stackTrace.firstOrNull()
    return CrashReportDetails(
        source = source,
        exceptionClassName = root.javaClass.name,
        exceptionMessage = PrivacyRedactor.maskMessage(message),
        throwClassName = site?.className,
        throwFileName = site?.fileName,
        throwMethodName = site?.methodName,
        throwLineNumber = site?.lineNumber,
        stackTrace = PrivacyRedactor.maskThrowable(throwable),
    )
}

internal data class CrashReportEmailDraft(
    val subject: String,
    val body: String,
)

internal object CrashReportEmail {
    private const val MaxEmailSectionChars = 80_000

    // The draft carries no log; the error text below it is masked.
    private const val PrivacyNote =
        "Please don't add health data (values, dates, names, files) to this email. The report below holds no log, " +
            "and numbers, dates, addresses and identifiers in its error text are masked."

    fun buildShareText(draft: CrashReportEmailDraft): String = buildString {
        appendLine("To: $CrashReportEmailRecipient")
        appendLine("Subject: ${draft.subject}")
        appendLine()
        append(draft.body)
    }

    fun buildDraft(
        appInfo: CrashReportAppInfo,
        savedCrashReport: String,
        anrReports: String = "",
    ): CrashReportEmailDraft {
        val hasCrash = savedCrashReport.isNotBlank()
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
                appendLine(PrivacyNote)
                appendLine()
                appendLine("App/device:")
                appendAppInfo(appInfo)
                appendLine()
                appendLine("Last saved OpenVitals crash report:")
                appendLine(savedCrashReport.ifBlank { "No crash in the last seven days." }.emailSection())
                appendLine()
                appendLine("Recent \"not responding\" records (stack traces only):")
                appendLine(anrReports.ifBlank { "none" }.emailSection())
            },
        )
    }

    fun buildStoredCrashReport(
        appInfo: CrashReportAppInfo,
        reportDetails: CrashReportDetails,
        threadName: String,
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
