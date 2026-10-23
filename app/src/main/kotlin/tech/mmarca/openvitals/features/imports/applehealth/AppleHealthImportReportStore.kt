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

    /** The workouts the card lists. Kept beside the report, never in it. */
    fun writeWorkoutsWithoutRoute(context: Context, workouts: List<AppleWorkoutWithoutRoute>) {
        writeFile(context, WorkoutsWithoutRouteFileName, encodeWorkoutsWithoutRoute(workouts))
    }

    fun readWorkoutsWithoutRoute(context: Context): List<AppleWorkoutWithoutRoute> =
        decodeWorkoutsWithoutRoute(read(File(File(context.filesDir, ReportDirectory), WorkoutsWithoutRouteFileName).path))

    /**
     * Reports written before they were structure only carry values, times and file
     * locations. Their names are retired, so a finished import's path finds nothing.
     */
    fun deleteLegacyReports(context: Context) {
        val directory = File(context.filesDir, ReportDirectory)
        LegacyFileNames.forEach { name -> runCatching { File(directory, name).delete() } }
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
    private const val ReportFileName = "apple-health-report.txt"
    private const val ErrorFileName = "apple-health-error.txt"
    private const val WorkoutsWithoutRouteFileName = "apple-health-workouts-without-route.tsv"
    private val LegacyFileNames = listOf("apple-health-import-report.txt", "apple-health-import-error.txt")
}

/** One workout per line: type, start and end in epoch milliseconds, tab-separated. */
internal fun encodeWorkoutsWithoutRoute(workouts: List<AppleWorkoutWithoutRoute>): String =
    workouts.joinToString(separator = "\n") { "${it.appleType}\t${it.start.toEpochMilli()}\t${it.end.toEpochMilli()}" }

internal fun decodeWorkoutsWithoutRoute(text: String): List<AppleWorkoutWithoutRoute> =
    text.lineSequence().mapNotNull { line ->
        val fields = line.split('\t')
        if (fields.size != 3) return@mapNotNull null
        val start = fields[1].toLongOrNull() ?: return@mapNotNull null
        val end = fields[2].toLongOrNull() ?: return@mapNotNull null
        AppleWorkoutWithoutRoute(fields[0], Instant.ofEpochMilli(start), Instant.ofEpochMilli(end))
    }.toList()
