package tech.mmarca.openvitals.core.diagnostics

import java.io.File
import java.time.Duration
import java.time.Instant

/**
 * The one crash report kept on the phone, for "Report an issue" to attach. It
 * holds masked error text only, and goes once it has been handed to an email,
 * a copy or a file, or after [MaxAge] either way.
 */
internal class CrashReportFile(
    private val directory: File,
    private val now: () -> Instant = Instant::now,
) {
    fun write(text: String) {
        directory.mkdirs()
        File(directory, FileName).writeText(text)
    }

    /** The report, or "" when there is none or it is too old to be about the issue at hand. */
    fun readFresh(): String {
        deleteLegacy()
        val file = File(directory, FileName)
        if (!file.isFile) return ""
        if (file.isStale()) {
            file.delete()
            return ""
        }
        return runCatching { file.readText() }.getOrDefault("")
    }

    fun discard() {
        File(directory, FileName).delete()
        deleteLegacy()
    }

    fun pruneStale() {
        deleteLegacy()
        File(directory, FileName).takeIf { it.isFile && it.isStale() }?.delete()
    }

    private fun File.isStale(): Boolean = Duration.between(Instant.ofEpochMilli(lastModified()), now()) > MaxAge

    private fun deleteLegacy() {
        File(directory, LegacyFileName).delete()
    }

    companion object {
        val MaxAge: Duration = Duration.ofDays(7)
        const val FileName = "last-crash.txt"

        // Written before reports were masked: it holds the raw log and the error text as thrown.
        const val LegacyFileName = "last-crash-report.txt"
    }
}
