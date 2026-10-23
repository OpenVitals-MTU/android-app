package tech.mmarca.openvitals.core.privacy

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import tech.mmarca.openvitals.core.privacy.PrivacyRedactor.Placeholder

class PrivacyRedactorTest {

    @Test
    fun `identifiers addresses locations and dates are masked while numbers stay`() {
        val masked = PrivacyRedactor.maskIdentifiers(PrivacySentinels.Sentence)

        PrivacySentinels.assertNoneIn(masked, except = setOf(PrivacySentinels.Value, PrivacySentinels.Name))
        assertThat(masked).contains("value 72.5")
    }

    @Test
    fun `each identifier kind is masked on its own`() {
        listOf(
            "uri=content://media/external/images/1",
            "mailto:jane@example.org",
            "staged /data/user/0/tech.mmarca.openvitals/files/export.zip",
            "Authorization: Bearer abc.def.ghi",
            "clientRecordId=apple-hk-1234",
            "deviceName=\"Jane's Watch\"",
            "peer 12-34-56-78-9A-BC",
            "at 2022-06-09 16:13:00 +0000",
            "on 14/03/2026",
            "since 1773473400000",
            "at 07:30",
            "key 0123456789abcdef0123",
            "token dGhpcyBpcyBhIHNlY3JldCB0b2tlbiAxMjM0NTY3",
        ).forEach { line ->
            assertThat(PrivacyRedactor.maskIdentifiers(line)).contains(Placeholder)
        }
    }

    @Test
    fun `type names packages permissions and bluetooth service uuids stay readable`() {
        val line = "HKQuantityTypeIdentifierHeartRateVariabilitySDNN MenstruationFlowRecord " +
            "android.permission.health.READ_HEART_RATE com.google.android.apps.healthdata " +
            "0000180d-0000-1000-8000-00805f9b34fb DefaultDispatcher-worker-1 status=133 took 250 ms"

        assertThat(PrivacyRedactor.maskIdentifiers(line)).isEqualTo(line)
    }

    @Test
    fun `masking twice changes nothing more`() {
        val once = PrivacyRedactor.maskIdentifiers(PrivacySentinels.Sentence + " uri=content://x path=\"/sdcard/a\"")

        assertThat(PrivacyRedactor.maskIdentifiers(once)).isEqualTo(once)
        assertThat(once).doesNotContain("]]")
    }

    @Test
    fun `a message also loses its numbers but keeps positions`() {
        val masked = PrivacyRedactor.maskMessage("weight must not be less than 0.0, currently 250.5 kg at line 3, column 15, row 7")

        assertThat(masked).isEqualTo("weight must not be less than #, currently # kg at line 3, column 15, row 7")
    }

    @Test
    fun `numbers inside names are not values`() {
        assertThat(PrivacyRedactor.maskMessage("HKQuantityTypeIdentifierVO2Max needs SHA256"))
            .isEqualTo("HKQuantityTypeIdentifierVO2Max needs SHA256")
    }

    @Test
    fun `a message keeps its first line only, capped, and a blank one is none`() {
        assertThat(PrivacyRedactor.maskMessage("first line\nJane Doe on the second")).isEqualTo("first line")
        assertThat(PrivacyRedactor.maskMessage("a".repeat(600))).hasLength(501)
        assertThat(PrivacyRedactor.maskMessage("  ")).isNull()
        assertThat(PrivacyRedactor.maskMessage(null)).isNull()
    }

    @Test
    fun `a message carrying every sentinel gives none back but the name`() {
        val masked = PrivacyRedactor.maskMessage(PrivacySentinels.Sentence).orEmpty()

        PrivacySentinels.assertNoneIn(masked, except = setOf(PrivacySentinels.Name))
    }

    @Test
    fun `describe names the class and masks the message`() {
        assertThat(PrivacyRedactor.describe(IllegalArgumentException("value 72.5 out of range")))
            .isEqualTo("java.lang.IllegalArgumentException: value # out of range")
        assertThat(PrivacyRedactor.describe(IllegalStateException())).isEqualTo("java.lang.IllegalStateException")
    }

    @Test
    fun `a masked stack trace keeps classes and frames and drops every message line after the first`() {
        val root = IllegalArgumentException("${PrivacySentinels.Sentence}\nsecond line by Jane Doe")
        val error = IllegalStateException("wrapped ${PrivacySentinels.Date}", root)
        error.addSuppressed(RuntimeException("closing ${PrivacySentinels.Mac}"))

        val trace = PrivacyRedactor.maskThrowable(error)

        PrivacySentinels.assertNoneIn(trace, except = setOf(PrivacySentinels.Name))
        assertThat(trace).doesNotContain("second line")
        assertThat(trace).startsWith("java.lang.IllegalStateException: wrapped $Placeholder")
        assertThat(trace).contains("\tSuppressed: java.lang.RuntimeException: closing $Placeholder")
        assertThat(trace).contains("Caused by: java.lang.IllegalArgumentException: value #")
        assertThat(trace).contains("\tat tech.mmarca.openvitals.core.privacy.PrivacyRedactorTest.")
        assertThat(trace).containsMatch("""\t\.\.\. \d+ more""")
    }

    @Test
    fun `a cause that points back at its wrapper ends the trace`() {
        val outer = IllegalStateException("outer")
        val inner = IllegalArgumentException("inner", outer)
        outer.initCause(inner)

        assertThat(PrivacyRedactor.maskThrowable(outer)).contains("[circular reference: java.lang.IllegalStateException]")
    }

    @Test
    fun `the shape of a cell keeps its layout and none of its content`() {
        assertThat(PrivacyRedactor.shapeOf(" 14/03/2026 07:30 ")).isEqualTo("99/99/9999 99:99")
        assertThat(PrivacyRedactor.shapeOf("n/a")).isEqualTo("a/a")
        assertThat(PrivacyRedactor.shapeOf("x".repeat(40))).isEqualTo("a".repeat(32) + "…")
    }
}
