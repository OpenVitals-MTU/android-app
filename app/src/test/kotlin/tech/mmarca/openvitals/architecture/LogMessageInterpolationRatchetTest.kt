package tech.mmarca.openvitals.architecture

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * A log line that splices in an exception's message can carry a health value:
 * Health Connect quotes the value it refused. Pass the throwable as the last
 * argument instead, or log its class name; the diagnostics export masks a
 * throwable's message whole. The count here may only fall.
 */
class LogMessageInterpolationRatchetTest {

    @Test
    fun `log calls splicing in an exception message do not grow`() {
        val offenders = sourceFiles()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> logCalls(file.readText()).filter(SplicedMessage::containsMatchIn).map { "${file.name}: $it" } }
            .toList()

        assertWithMessage(
            "a log call splices in an exception message. Pass the throwable as the last argument " +
                "(Log.w(TAG, \"…\", e)) or log e::class.java.simpleName. Lower Ceiling when the count falls.\n" +
                offenders.joinToString("\n"),
        )
            .that(offenders.size)
            .isAtMost(Ceiling)
    }

    // Each Log.x( … ) call, across lines, up to its closing parenthesis.
    private fun logCalls(source: String): Sequence<String> = LogCall.findAll(source).map { match ->
        var depth = 1
        var end = match.range.last + 1
        while (end < source.length && depth > 0) {
            when (source[end]) {
                '(' -> depth++
                ')' -> depth--
            }
            end++
        }
        source.substring(match.range.first, end).replace(Regex("""\s+"""), " ")
    }

    private fun sourceFiles() =
        File(SourceRoot).walkTopDown() + File(DomainRoot).walkTopDown() + File(WearRoot).walkTopDown()

    private companion object {
        const val SourceRoot = "src/main/kotlin/tech/mmarca/openvitals"
        const val DomainRoot = "../domain/src/main/kotlin/tech/mmarca/openvitals"
        const val WearRoot = "../wear/src/main/kotlin/tech/mmarca/openvitals"
        val LogCall = Regex("""\bLog\.(?:v|d|i|w|e|wtf)\(""")
        val SplicedMessage = Regex("""\$\{[^}]*\.(?:message|localizedMessage)\b[^}]*}|\$(?:e|error|throwable|cause)\b""")

        // Bluetooth, pairing and foreground-service refusals: platform text, no record values.
        const val Ceiling = 30
    }
}
