package tech.mmarca.openvitals.core.diagnostics

import android.content.Context
import android.os.Process
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.core.privacy.PrivacyRedactor
import tech.mmarca.openvitals.core.privacy.maskLogcat

data class DebugLogExportResult(
    val writtenLines: Int,
    val droppedLines: Int,
)

/**
 * Exports this process's logcat for diagnostics builds. Every line and tag is
 * kept, since Health Connect troubleshooting needs the whole trail; addresses,
 * identifiers, file locations and dates are masked, and an exception's message
 * is masked whole.
 */
object PrivacySafeDebugLogExporter {
    /** Keep the most recent lines so a long session still fits a share intent. */
    internal const val MaxLines = 20_000

    suspend fun writeCurrentProcessLogcat(
        context: Context,
        outputStream: OutputStream,
    ): DebugLogExportResult = withContext(Dispatchers.IO) {
        writeCurrentProcessLogcatBlocking(context, outputStream)
    }

    /** For callers already on a background thread. */
    internal fun writeCurrentProcessLogcatBlocking(context: Context, outputStream: OutputStream): DebugLogExportResult {
        check(BuildConfig.OPENVITALS_DIAGNOSTICS) {
            "Debug log export is only available in diagnostics builds."
        }
        val rawLines = runCatching { readCurrentProcessLogcat() }
            .getOrElse { throwable ->
                listOf("E/OpenVitalsDiagnostics: logcat capture failed type=${throwable::class.java.simpleName}")
            }
        val exported = exportLogcat(rawLines)
        outputStream.writer(Charsets.UTF_8).use { writer ->
            writer.appendLine("OpenVitals diagnostics log export")
            writer.appendLine("package=${context.packageName}")
            writer.appendLine("version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            writer.appendLine(PrivacyHeader)
            writer.appendLine("writtenLines=${exported.writtenLines}")
            writer.appendLine("droppedLines=${exported.droppedLines}")
            writer.appendLine()
            exported.lines.forEach(writer::appendLine)
            // This logcat is the live process only. A frozen, killed process left its trace with the system.
            writer.appendLine()
            writer.appendLine("Recent \"not responding\" records (stack traces only):")
            writer.appendLine(AnrExitInfo.recent(context).ifBlank { "none" })
        }
        return DebugLogExportResult(writtenLines = exported.writtenLines, droppedLines = exported.droppedLines)
    }

    /** The [MaxLines] most recent lines, masked. [droppedLines] counts the trimmed ones; masking drops none. */
    internal fun exportLogcat(lines: List<String>): ExportedLogcat {
        val kept = lines.takeLast(MaxLines)
        return ExportedLogcat(
            lines = PrivacyRedactor.maskLogcat(kept),
            writtenLines = kept.size,
            droppedLines = lines.size - kept.size,
        )
    }

    internal const val PrivacyHeader =
        "privacy=every line and tag kept; addresses, identifiers, file locations and dates masked; " +
            "exception messages masked whole"

    private fun readCurrentProcessLogcat(): List<String> {
        val process = ProcessBuilder(
            "logcat",
            "-d",
            "--pid",
            Process.myPid().toString(),
            "-v",
            "threadtime",
        )
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroy()
            return listOf("W/OpenVitalsDiagnostics: logcat capture timed out")
        }
        return process.inputStream.bufferedReader().useLines { it.toList() }
    }
}

data class ExportedLogcat(
    val lines: List<String>,
    val writtenLines: Int,
    val droppedLines: Int,
)
