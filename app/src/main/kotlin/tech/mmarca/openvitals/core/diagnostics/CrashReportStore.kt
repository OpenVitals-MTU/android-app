package tech.mmarca.openvitals.core.diagnostics

import android.content.Context
import android.os.Build
import java.io.File
import java.time.Instant
import tech.mmarca.openvitals.BuildConfig

internal object CrashReportStore {
    private const val ReportDirectory = "crash_reports"
    private const val DiagnosticsExportMaxAgeMillis = 24 * 60 * 60 * 1000L

    fun writeLastCrashReport(
        context: Context,
        thread: Thread,
        throwable: Throwable,
    ) {
        val appContext = context.applicationContext
        val reportText = CrashReportEmail.buildStoredCrashReport(
            appInfo = appInfo(appContext),
            reportDetails = crashReportDetails(throwable, source = "OpenVitals uncaught exception handler"),
            threadName = thread.name,
            capturedAt = Instant.now().toString(),
        )
        crashFile(appContext).write(reportText)
    }

    fun readFreshCrashReport(context: Context): String =
        runCatching { crashFile(context).readFresh() }.getOrDefault("")

    /** Once the report has gone into an email, a copy or a file, the phone keeps none. */
    fun discardCrashReport(context: Context) {
        runCatching { crashFile(context).discard() }
    }

    /** At app start: a crash report past its age, and a diagnostics log export left over from a share. */
    fun pruneStale(context: Context) {
        runCatching { crashFile(context).pruneStale() }
        val cutoffMillis = System.currentTimeMillis() - DiagnosticsExportMaxAgeMillis
        File(context.cacheDir, DiagnosticsExportCacheDirectory).listFiles()
            ?.filter { it.isFile && it.lastModified() < cutoffMillis }
            ?.forEach { runCatching { it.delete() } }
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

    private fun crashFile(context: Context): CrashReportFile =
        CrashReportFile(File(context.applicationContext.filesDir, ReportDirectory))
}
