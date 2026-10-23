package tech.mmarca.openvitals.core.privacy

// `logcat -v threadtime`: "10-23 14:22:01.123  1234  5678 W Tag: message". The prefix is kept as is.
private val threadtimeLine = Regex("""^(\d\d-\d\d \d\d:\d\d:\d\d\.\d{3}\s+\d+\s+\d+ [VDIWEAF] .*?: )(.*)$""")
private val stackFrame = Regex("""^\s*at [\w$.<>/-]+\((?:[\w$.\-]+(?::\d+)?|Native Method|Unknown Source(?::\d+)?)\)$""")
private val framesInCommon = Regex("""^\s*\.\.\. \d+ more$""")

// Any dotted name counts as an exception class, so names R8 shortened ("a.b: …") match too.
private val exceptionHeader = Regex(
    """^(\s*(?:Caused by|Suppressed): )?([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+)(?::\s?(.*))?$""",
)

/**
 * Masks one `logcat -v threadtime` line: the time, process, thread, level and
 * tag stay, frames stay, an exception header's message is masked like any
 * exception message, and the rest has its identifiers masked.
 */
fun PrivacyRedactor.maskLogLine(line: String): String = maskLogcat(listOf(line)).single()

/**
 * [maskLogLine] over a whole capture, line for line. A log call with a throwable
 * prints one entry per line under the same prefix, so a line after an exception
 * header that is not a frame is the rest of that exception's message, masked as one.
 */
fun PrivacyRedactor.maskLogcat(lines: List<String>): List<String> {
    var exceptionEntry: String? = null
    return lines.map { line ->
        val match = threadtimeLine.matchEntire(line)
        if (match == null) {
            exceptionEntry = null
            return@map maskIdentifiers(line)
        }
        val prefix = match.groupValues[1]
        val message = match.groupValues[2]
        val header = exceptionHeader.matchEntire(message)
        when {
            message.isBlank() || stackFrame.matches(message) || framesInCommon.matches(message) -> line
            header != null -> {
                exceptionEntry = prefix
                prefix + maskedHeader(header)
            }
            exceptionEntry == prefix -> prefix + (maskMessage(message) ?: "")
            else -> {
                exceptionEntry = null
                prefix + maskIdentifiers(message)
            }
        }
    }
}

private fun PrivacyRedactor.maskedHeader(header: MatchResult): String {
    val caption = header.groupValues[1]
    val className = header.groupValues[2]
    val message = maskMessage(header.groups[3]?.value)
    return if (message == null) "$caption$className" else "$caption$className: $message"
}
