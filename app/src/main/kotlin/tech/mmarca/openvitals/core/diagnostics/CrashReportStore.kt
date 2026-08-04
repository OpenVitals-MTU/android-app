package tech.mmarca.openvitals.core.diagnostics

import android.app.ApplicationErrorReport
import android.content.Context
import android.os.Build
import java.io.File
import java.time.Instant
import tech.mmarca.openvitals.BuildConfig

internal object CrashReportStore {
    private const val ReportDirectory = "crash_reports"
    private const val LastCrashFileName = "last-crash-report.txt"

    fun writeLastCrashReport(
        context: Context,
        thread: Thread,
        throwable: Throwable,
    ) {
        val appContext = context.applicationContext
        val details = detailsFromThrowable(throwable)
        val reportText = CrashReportEmail.buildStoredCrashReport(
            appInfo = appInfo(appContext),
            reportDetails = details,
            threadName = thread.name,
            diagnosticsLog = captureDiagnosticsLog(appContext),
            capturedAt = Instant.now().toString(),
        )
        lastCrashFile(appContext).apply {
            parentFile?.mkdirs()
            writeText(reportText)
        }
    }

    fun readLastCrashReport(context: Context): String =
        runCatching {
            lastCrashFile(context.applicationContext)
                .takeIf { it.isFile }
                ?.readText()
                .orEmpty()
        }.getOrDefault("")

    fun detailsFromApplicationErrorReport(report: ApplicationErrorReport?): CrashReportDetails? {
        val crashInfo = report?.crashInfo ?: return null
        return detailsFromCrashInfo(
            crashInfo = crashInfo,
            source = "Android crash dialog",
        )
    }

    fun appInfo(context: Context): CrashReportAppInfo =
        CrashReportAppInfo(
            packageName = context.packageName,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            buildType = BuildConfig.BUILD_TYPE,
            androidRelease = Build.VERSION.RELEASE ?: "unknown",
            sdkInt = Build.VERSION.SDK_INT,
            device = listOf(Build.MANUFACTURER, Build.MODEL)
                .filter { !it.isNullOrBlank() }
                .joinToString(" ")
                .ifBlank { "unknown" },
        )

    fun captureDiagnosticsLog(context: Context): String {
        if (!BuildConfig.OPENVITALS_DIAGNOSTICS) {
            return "Diagnostics log unavailable in this build."
        }
        return runCatching {
            PrivacySafeDebugLogExporter.currentProcessLogcatTextBlocking(context)
        }.getOrElse { throwable ->
            "Diagnostics log unavailable: ${throwable::class.java.simpleName}"
        }
    }

    private fun detailsFromThrowable(throwable: Throwable): CrashReportDetails =
        detailsFromCrashInfo(
            crashInfo = ApplicationErrorReport.CrashInfo(throwable),
            source = "OpenVitals uncaught exception handler",
        )

    private fun detailsFromCrashInfo(
        crashInfo: ApplicationErrorReport.CrashInfo,
        source: String,
    ): CrashReportDetails =
        CrashReportDetails(
            source = source,
            exceptionClassName = crashInfo.exceptionClassName,
            exceptionMessage = crashInfo.exceptionMessage,
            throwClassName = crashInfo.throwClassName,
            throwFileName = crashInfo.throwFileName,
            throwMethodName = crashInfo.throwMethodName,
            throwLineNumber = crashInfo.throwLineNumber,
            stackTrace = crashInfo.stackTrace,
        )

    private fun lastCrashFile(context: Context): File =
        File(File(context.filesDir, ReportDirectory), LastCrashFileName)
}
