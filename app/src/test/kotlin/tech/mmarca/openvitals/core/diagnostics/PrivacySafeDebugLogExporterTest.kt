package tech.mmarca.openvitals.core.diagnostics

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import tech.mmarca.openvitals.core.privacy.PrivacySentinels

class PrivacySafeDebugLogExporterTest {
    private val prefix = "10-23 14:22:01.123  1234  5678 D OkHttp: "

    @Test
    fun `export masks identifiers, addresses and dates and keeps the tag and the numbers`() {
        val result = PrivacySafeDebugLogExporter.exportLogcat(
            listOf(prefix + "GET https://example.test/${PrivacySentinels.Email} ${PrivacySentinels.Sentence} took 250 ms"),
        )

        val line = result.lines.single()
        PrivacySentinels.assertNoneIn(line, except = setOf(PrivacySentinels.Value, PrivacySentinels.Name))
        assertThat(line).startsWith(prefix + "GET [redacted]")
        assertThat(line).endsWith("took 250 ms")
    }

    @Test
    fun `an exception's message is masked whole and its frames stay`() {
        val entry = "10-23 14:22:01.124  1234  5678 E AppleHealthImporter: "

        val result = PrivacySafeDebugLogExporter.exportLogcat(
            listOf(
                entry + "Apple Health import failed",
                entry + "java.lang.IllegalArgumentException: value ${PrivacySentinels.Value} out of range",
                entry + "\tat tech.mmarca.openvitals.Importer.write(Importer.kt:12)",
            ),
        )

        assertThat(result.lines).containsExactly(
            entry + "Apple Health import failed",
            entry + "java.lang.IllegalArgumentException: value # out of range",
            entry + "\tat tech.mmarca.openvitals.Importer.write(Importer.kt:12)",
        ).inOrder()
    }

    @Test
    fun `exportLogcat keeps every line and tag`() {
        val result = PrivacySafeDebugLogExporter.exportLogcat(
            listOf(
                "10-23 14:22:01.123  1234  5678 D ActivityRepository: Skipping loadDailySteps missingCount=1",
                "10-23 14:22:01.123  1234  5678 I RandomTag: location token secret raw payload",
                "garbage without level prefix",
            ),
        )

        assertThat(result.writtenLines).isEqualTo(3)
        assertThat(result.droppedLines).isEqualTo(0)
        assertThat(result.lines).hasSize(3)
        assertThat(result.lines[1]).contains("RandomTag: location token secret raw payload")
        assertThat(result.lines[2]).isEqualTo("garbage without level prefix")
    }

    @Test
    fun `exportLogcat caps output at maxLines keeping the most recent lines`() {
        val maxLines = PrivacySafeDebugLogExporter.MaxLines
        val lines = (0 until maxLines + 100).map { "I/OpenVitalsX: line $it" }

        val result = PrivacySafeDebugLogExporter.exportLogcat(lines)

        assertThat(result.writtenLines).isEqualTo(maxLines)
        assertThat(result.lines).hasSize(maxLines)
        assertThat(result.droppedLines).isEqualTo(100)
        assertThat(result.lines.first()).isEqualTo("I/OpenVitalsX: line 100")
        assertThat(result.lines.last()).isEqualTo("I/OpenVitalsX: line ${maxLines + 99}")
    }

    @Test
    fun `the export header says what is masked`() {
        assertThat(PrivacySafeDebugLogExporter.PrivacyHeader).contains("dates masked")
        assertThat(PrivacySafeDebugLogExporter.PrivacyHeader).doesNotContain("no tag filtering or redaction")
    }
}
