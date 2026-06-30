package tech.mmarca.openvitals.features.imports.applehealth

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

    private fun Throwable.stackTraceText(): String {
        val writer = StringWriter()
        PrintWriter(writer).use { printWriter ->
            printStackTrace(printWriter)
        }
        return writer.toString()
    }
}
