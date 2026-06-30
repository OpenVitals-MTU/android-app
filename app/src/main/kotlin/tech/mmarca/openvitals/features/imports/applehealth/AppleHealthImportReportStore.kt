package tech.mmarca.openvitals.features.imports.applehealth

import android.content.Context
import java.io.File

internal object AppleHealthImportReportStore {
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
