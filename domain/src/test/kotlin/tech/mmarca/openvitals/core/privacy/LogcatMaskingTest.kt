package tech.mmarca.openvitals.core.privacy

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import tech.mmarca.openvitals.core.privacy.PrivacyRedactor.Placeholder

class LogcatMaskingTest {

    private val prefix = "10-23 14:22:01.123  1234  5678 W CycleRepository: "

    @Test
    fun `the prefix and tag stay and identifiers in the message are masked`() {
        val masked = PrivacyRedactor.maskLogLine(prefix + "Deferred reconcile for [${PrivacySentinels.Date}] from ${PrivacySentinels.Mac}")

        assertThat(masked).isEqualTo(prefix + "Deferred reconcile for [$Placeholder] from $Placeholder")
    }

    @Test
    fun `plain numbers in a log message stay`() {
        val line = prefix + "Read 120 records in 350 ms, attempt 2"

        assertThat(PrivacyRedactor.maskLogLine(line)).isEqualTo(line)
    }

    @Test
    fun `an exception entry is masked whole and its frames stay`() {
        val entry = "10-23 14:22:01.124  1234  5678 W HealthConnectSyncStore: "
        val lines = listOf(
            entry + "Write failed",
            entry + "java.lang.IllegalArgumentException: value must not be less than 0.0, currently 72.5",
            entry + "second message line for Jane Doe 72.5",
            entry + "\tat tech.mmarca.openvitals.Foo.write(Foo.kt:42)",
            entry + "Caused by: a.b: 72.5 at ${PrivacySentinels.Instant}",
            entry + "\tat a.c.d(SourceFile:7)",
            entry + "\t... 12 more",
        )

        val masked = PrivacyRedactor.maskLogcat(lines)

        assertThat(masked).containsExactly(
            entry + "Write failed",
            entry + "java.lang.IllegalArgumentException: value must not be less than #, currently #",
            entry + "second message line for Jane Doe #",
            entry + "\tat tech.mmarca.openvitals.Foo.write(Foo.kt:42)",
            entry + "Caused by: a.b: # at $Placeholder",
            entry + "\tat a.c.d(SourceFile:7)",
            entry + "\t... 12 more",
        ).inOrder()
    }

    @Test
    fun `a new entry ends the exception it follows`() {
        val entry = "10-23 14:22:01.124  1234  5678 W Tag: "
        val next = "10-23 14:22:01.200  1234  5678 I Tag: "

        val masked = PrivacyRedactor.maskLogcat(
            listOf(entry + "java.lang.IllegalStateException: boom 3", next + "Read 120 records"),
        )

        assertThat(masked.last()).isEqualTo(next + "Read 120 records")
    }

    @Test
    fun `lines outside the threadtime format are masked and none is dropped`() {
        val lines = listOf("--------- beginning of main", "garbage ${PrivacySentinels.Email}", prefix)

        val masked = PrivacyRedactor.maskLogcat(lines)

        assertThat(masked).containsExactly("--------- beginning of main", "garbage $Placeholder", prefix).inOrder()
    }
}
