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

object PrivacySafeDebugLogExporter {
    private const val MaxLines = 2_000
    private const val Redacted = "[redacted]"
    private const val UnsanitizedAppleImporterTag = "AppleHealthImporter"

    private val logLinePattern = Regex("""^([VDIWEAF])/([A-Za-z0-9_.-]+)\s*:\s*(.*)$""")
    private val macAddressPattern = Regex("""\b[0-9A-Fa-f]{2}(?::[0-9A-Fa-f]{2}){5}\b""")
    private val uuidPattern = Regex(
        """\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b"""
    )
    private val emailPattern = Regex("""\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b""", RegexOption.IGNORE_CASE)
    private val phonePattern = Regex("""(?<!\w)\+?[0-9][0-9 .()\-]{7,}[0-9](?!\w)""")
    private val uriPattern = Regex("""\b(?:content|file|https?)://\S+""", RegexOption.IGNORE_CASE)
    private val isoInstantPattern = Regex("""\b\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?Z?\b""")
    private val isoDatePattern = Regex("""\b\d{4}-\d{2}-\d{2}\b""")
    private val userPathPattern = Regex("""/(?:storage/emulated/\d+|sdcard|data/user/\d+)/\S+""")
    private val keyValueIdPattern = Regex(
        """(?i)\b(clientRecordId|recordId|deviceId|widgetId|token|secret|password|api[_-]?key)=\S+"""
    )
    private val unsanitizedAppleImporterLevels = setOf("W", "E", "A", "F")

    private val dropKeywords = listOf(
        " latitude",
        " longitude",
        " lat=",
        " lon=",
        " lng=",
        " location",
        " polyline",
        " raw ",
        " payload",
        " content://",
        " file://",
        " /storage/",
        " /sdcard/",
        " displayname",
        " bluetoothname",
        " devicename",
        " token",
        " password",
        " secret",
        " api_key",
        " apikey",
    )

    private val explicitAllowedTags = setOf(
        "BleGattConnection",
        "BodyHealthReader",
        "HealthConnectManager",
        "HomeWidget",
        "HydrationHealthReader",
        "HydrationReminderAlarmManager",
        "HydrationReminderController",
        "MindfulnessReminderAlarmManager",
        "MindfulnessReminderController",
        "SettingsViewModel",
    )

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
        val sanitized = sanitizeLogcat(rawLines)
        val text = buildString {
            appendLine("OpenVitals diagnostics log export")
            appendLine("package=${context.packageName}")
            appendLine("version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine(
                "privacy=only app log tags are included; sensitive lines are dropped or redacted; " +
                    "AppleHealthImporter W/E/A/F lines are unsanitized",
            )
            appendLine("writtenLines=${sanitized.writtenLines}")
            appendLine("droppedLines=${sanitized.droppedLines}")
            appendLine()
            sanitized.lines.forEach(::appendLine)
        }
        return DebugLogExportPayload(
            text = text,
            result = DebugLogExportResult(
                writtenLines = sanitized.writtenLines,
                droppedLines = sanitized.droppedLines,
            ),
        )
    }

    internal fun sanitizeLogcat(lines: List<String>): SanitizedLogcat {
        var dropped = 0
        val kept = lines.asSequence()
            .mapNotNull { line ->
                val sanitized = sanitizeLogLine(line)
                if (sanitized == null) dropped += 1
                sanitized
            }
            .toList()
            .takeLast(MaxLines)
        return SanitizedLogcat(
            lines = kept,
            writtenLines = kept.size,
            droppedLines = dropped,
        )
    }

    internal fun sanitizeLogLine(line: String): String? {
        val match = logLinePattern.matchEntire(line.trim()) ?: return null
        val level = match.groupValues[1]
        val tag = match.groupValues[2]
        val message = match.groupValues[3]
        if (isUnsanitizedAppleImporterLine(level, tag)) return line.trim()
        if (!isAllowedTag(tag)) return null
        if (message.isBlank()) return null
        if (shouldDrop(message)) return null

        val redacted = message
            .replace(uriPattern, Redacted)
            .replace(userPathPattern, Redacted)
            .replace(emailPattern, Redacted)
            .replace(phonePattern, Redacted)
            .replace(macAddressPattern, Redacted)
            .replace(uuidPattern, Redacted)
            .replace(keyValueIdPattern) { result ->
                "${result.groupValues[1]}=$Redacted"
            }
            .replace(isoInstantPattern, Redacted)
            .replace(isoDatePattern, Redacted)
            .take(800)

        return "$level/$tag: $redacted"
    }

    private fun readCurrentProcessLogcat(): List<String> {
        val process = ProcessBuilder(
            "logcat",
            "-d",
            "--pid",
            Process.myPid().toString(),
            "-v",
            "tag",
        )
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroy()
            return listOf("W/OpenVitalsDiagnostics: logcat capture timed out")
        }
        return process.inputStream.bufferedReader().useLines { it.toList() }
    }

    private fun isAllowedTag(tag: String): Boolean =
        tag.startsWith("OpenVitals") ||
            tag.startsWith("HealthConnect") ||
            tag.endsWith("Repository") ||
            tag.endsWith("ViewModel") ||
            tag in explicitAllowedTags

    private fun isUnsanitizedAppleImporterLine(level: String, tag: String): Boolean =
        tag == UnsanitizedAppleImporterTag && level in unsanitizedAppleImporterLevels

    private fun shouldDrop(message: String): Boolean {
        val lower = " ${message.lowercase()} "
        return dropKeywords.any(lower::contains)
    }
}

data class SanitizedLogcat(
    val lines: List<String>,
    val writtenLines: Int,
    val droppedLines: Int,
)
