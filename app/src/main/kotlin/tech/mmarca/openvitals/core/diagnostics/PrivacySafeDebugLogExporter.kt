package tech.mmarca.openvitals.core.diagnostics

import android.content.Context
import android.os.Process
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.BuildConfig

data class DebugLogExportResult(
    val writtenLines: Int,
    val droppedLines: Int,
)

private data class DebugLogExportPayload(
    val text: String,
    val result: DebugLogExportResult,
)

/**
 * Exports this process's logcat for diagnostics builds.
 *
 * The export is intentionally raw: no tag allow-list, keyword drops, or
 * redaction. Diagnostics builds are opt-in troubleshooting artifacts, and
 * Health Connect / activity troubleshooting needs the unfiltered trail.
 */
object PrivacySafeDebugLogExporter {
    /** Keep the most recent lines so a long session still fits a share intent. */
    internal const val MaxLines = 20_000

    suspend fun writeCurrentProcessLogcat(
        context: Context,
        outputStream: OutputStream,
    ): DebugLogExportResult = withContext(Dispatchers.IO) {
        val payload = currentProcessLogcatPayload(context)
        outputStream.writer(Charsets.UTF_8).use { writer ->
            writer.append(payload.text)
        }
        payload.result
    }

    suspend fun currentProcessLogcatText(context: Context): String = withContext(Dispatchers.IO) {
        currentProcessLogcatTextBlocking(context)
    }

    internal fun currentProcessLogcatTextBlocking(context: Context): String =
        currentProcessLogcatPayload(context).text

    private fun currentProcessLogcatPayload(context: Context): DebugLogExportPayload {
        check(BuildConfig.OPENVITALS_DIAGNOSTICS) {
            "Debug log export is only available in diagnostics builds."
        }

        val rawLines = runCatching { readCurrentProcessLogcat() }
            .getOrElse { throwable ->
                listOf("E/OpenVitalsDiagnostics: logcat capture failed type=${throwable::class.java.simpleName}")
            }
        val exported = exportLogcat(rawLines)
        val text = buildString {
            appendLine("OpenVitals diagnostics log export")
            appendLine("package=${context.packageName}")
            appendLine("version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("privacy=raw process logcat; no tag filtering or redaction")
            appendLine("writtenLines=${exported.writtenLines}")
            appendLine("droppedLines=${exported.droppedLines}")
            appendLine()
            exported.lines.forEach(::appendLine)
        }
        return DebugLogExportPayload(
            text = text,
            result = DebugLogExportResult(
                writtenLines = exported.writtenLines,
                droppedLines = exported.droppedLines,
            ),
        )
    }

    /**
     * Keeps raw log lines as-is, truncated to [MaxLines] most recent entries.
     * [droppedLines] counts only lines trimmed by that cap (never redacted).
     */
    internal fun exportLogcat(lines: List<String>): ExportedLogcat {
        if (lines.size <= MaxLines) {
            return ExportedLogcat(
                lines = lines,
                writtenLines = lines.size,
                droppedLines = 0,
            )
        }
        val kept = lines.takeLast(MaxLines)
        return ExportedLogcat(
            lines = kept,
            writtenLines = kept.size,
            droppedLines = lines.size - kept.size,
        )
    }

    /** @deprecated Prefer [exportLogcat]; kept for call-site compatibility during the raw-export switch. */
    internal fun sanitizeLogcat(lines: List<String>): ExportedLogcat = exportLogcat(lines)

    /** @deprecated No longer sanitizes; returns the line unchanged when non-blank. */
    internal fun sanitizeLogLine(line: String): String? = line.trim().takeIf { it.isNotEmpty() }

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

/** @deprecated Renamed to [ExportedLogcat]. */
typealias SanitizedLogcat = ExportedLogcat
