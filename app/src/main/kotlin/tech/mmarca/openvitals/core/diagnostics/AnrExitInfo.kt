package tech.mmarca.openvitals.core.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.Reader
import java.time.Instant

/**
 * The "not responding" records Android keeps for this app. A frozen process
 * cannot log its own freeze; the system's record survives it. The trace
 * holds thread names and stack frames only, no health values.
 */
internal object AnrExitInfo {
    private const val MaxRecords = 2
    private const val RecordsToScan = 16
    private const val MaxTraceReadChars = 2_000_000

    /** Report text for the newest records. Blank before Android 11 or when there are none. */
    fun recent(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return ""
        return runCatching { recentApi30(context) }
            .getOrElse { throwable -> "Not-responding records unavailable: ${throwable::class.java.simpleName}" }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun recentApi30(context: Context): String {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return ""
        return manager.getHistoricalProcessExitReasons(null, 0, RecordsToScan)
            .filter { it.reason == ApplicationExitInfo.REASON_ANR }
            .take(MaxRecords)
            .joinToString(separator = "\n") { info ->
                val trace = runCatching {
                    info.traceInputStream?.bufferedReader()?.use { it.readCapped(MaxTraceReadChars) }
                }.getOrNull()
                formatAnrRecord(
                    at = Instant.ofEpochMilli(info.timestamp).toString(),
                    processName = info.processName,
                    description = info.description,
                    importance = info.importance,
                    trace = trace,
                )
            }
    }
}

private fun Reader.readCapped(maxChars: Int): String {
    val out = StringBuilder()
    val buffer = CharArray(8_192)
    while (out.length < maxChars) {
        val read = read(buffer)
        if (read < 0) break
        out.append(buffer, 0, read)
    }
    return out.toString()
}

internal fun formatAnrRecord(
    at: String,
    processName: String?,
    description: String?,
    importance: Int,
    trace: String?,
): String = buildString {
    appendLine("- At: $at")
    appendLine("- Process: ${processName.orEmpty().ifBlank { "missing" }}")
    appendLine("- Reason: ${description.orEmpty().ifBlank { "missing" }}")
    appendLine("- Importance: $importance")
    appendLine(trace?.let(::trimAnrTrace)?.ifBlank { null } ?: "No trace was kept.")
}

private const val MaxTrimmedTraceChars = 12_000

/**
 * Keeps the threads that explain a freeze: main, and the coroutine workers
 * it may wait on. Blocks are separated by blank lines and start with the
 * quoted thread name.
 */
internal fun trimAnrTrace(trace: String, maxChars: Int = MaxTrimmedTraceChars): String {
    val blocks = trace.replace("\r\n", "\n").split(Regex("\n\\s*\n"))
    val kept = blocks.filter { block ->
        val first = block.trimStart()
        first.startsWith("\"main\"") || first.startsWith("\"DefaultDispatcher")
    }
    // "main" first: it is the thread that stopped answering.
    val ordered = kept.sortedByDescending { it.trimStart().startsWith("\"main\"") }
    val text = ordered.joinToString(separator = "\n\n") { it.trim('\n') }
    return if (text.length <= maxChars) text else text.take(maxChars) + "\n[trace truncated]"
}
