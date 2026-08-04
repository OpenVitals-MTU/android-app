package tech.mmarca.openvitals.features.imports.applehealth

import android.content.Context
import java.io.File
import java.time.Instant
import tech.mmarca.openvitals.BuildConfig

internal fun StringBuilder.appendAppleHealthReportHeader() {
    appendLine("OpenVitals Apple Health Import Report")
    appendLine("Generated: ${Instant.now()}")
    appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    appendLine("Health Connect client: androidx.health.connect:connect-client (runtime version unavailable)")
}

internal object AppleHealthImportReportStore {
    fun reportPath(context: Context): String =
        File(File(context.filesDir, ReportDirectory), ReportFileName).absolutePath

    fun write(context: Context, reportText: String): String {
        return writeFile(context, ReportFileName, reportText)
    }

    fun writeFailure(context: Context, reportText: String): String {
        return writeFile(context, ErrorFileName, reportText)
    }

    private fun writeFile(context: Context, fileName: String, reportText: String): String {
        val directory = File(context.filesDir, ReportDirectory).apply { mkdirs() }
        val report = File(directory, fileName)
        report.writeText(reportText)
        return report.absolutePath
    }

    fun read(reportPath: String?): String =
        reportPath
            ?.let(::File)
            ?.takeIf { it.exists() && it.isFile }
            ?.runCatching { readText() }
            ?.getOrNull()
            .orEmpty()

    private const val ReportDirectory = "import_reports"
    private const val ReportFileName = "apple-health-import-report.txt"
    private const val ErrorFileName = "apple-health-import-error.txt"
}
