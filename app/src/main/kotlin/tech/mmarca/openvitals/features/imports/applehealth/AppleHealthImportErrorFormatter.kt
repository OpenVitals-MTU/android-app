package tech.mmarca.openvitals.features.imports.applehealth

import tech.mmarca.openvitals.core.presentation.isPermissionFailure
import tech.mmarca.openvitals.core.privacy.PrivacyRedactor

internal object AppleHealthImportErrorFormatter {
    /** The error's class and its message, masked: the summary goes into reports the user shares. */
    fun summary(error: Throwable): String = PrivacyRedactor.describe(error)

    /** The masked stack trace, every message line after the first left out. */
    fun details(error: Throwable): String =
        runCatching { PrivacyRedactor.maskThrowable(error) }
            .getOrDefault("")
            .ifBlank { summary(error) }

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
}
