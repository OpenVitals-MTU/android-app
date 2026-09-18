package tech.mmarca.openvitals.features.imports.applehealth

import tech.mmarca.openvitals.core.presentation.isPermissionFailure
import java.io.PrintWriter
import java.io.StringWriter

internal object AppleHealthImportErrorFormatter {
    private const val FallbackMessage = "Apple Health import failed."

    fun summary(error: Throwable): String {
        val type = error::class.java.name.takeIf { it.isNotBlank() }
        val message = error.localizedMessage?.takeIf { it.isNotBlank() }
        return when {
            type != null && message != null -> "$type: $message"
            type != null -> type
            message != null -> message
            else -> FallbackMessage
        }
    }

    fun details(error: Throwable): String {
        val stackTrace = runCatching { error.stackTraceText().trim() }
            .getOrDefault("")
        return stackTrace.ifBlank { summary(error) }
    }

    /**
     * The start of an error, short enough to draw on a card. A failure
     * report holds every worker log line and a stack trace; one `Text` that
     * long froze the main thread. Copy and save keep the full text.
     */
    fun preview(text: String): ErrorPreview {
        val lines = text.lineSequence().take(MaxPreviewLines + 1).toList()
        val byLines = lines.take(MaxPreviewLines).joinToString("\n")
        val capped = byLines.take(MaxPreviewCharacters)
        val truncated = lines.size > MaxPreviewLines || byLines.length > MaxPreviewCharacters
        return ErrorPreview(text = capped.trimEnd(), truncated = truncated)
    }

    data class ErrorPreview(val text: String, val truncated: Boolean)

    const val MaxPreviewCharacters = 1_500
    const val MaxPreviewLines = 24

    /** Delegates to the app-wide rule so the import card and `toScreenError()` agree. */
    fun isPermissionDenied(error: Throwable): Boolean = error.isPermissionFailure()

    private fun Throwable.stackTraceText(): String {
        val writer = StringWriter()
        PrintWriter(writer).use { printWriter ->
            printStackTrace(printWriter)
        }
        return writer.toString()
    }
}
