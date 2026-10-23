package tech.mmarca.openvitals.core.diagnostics

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Duration
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashReportFileTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val now = Instant.parse("2026-10-23T12:00:00Z")

    private fun reportFile(directory: File = folder.root) = CrashReportFile(directory) { now }

    private fun File.agedBy(age: Duration) = apply { setLastModified(now.minus(age).toEpochMilli()) }

    @Test
    fun `a report written today is read back`() {
        val crashes = reportFile(File(folder.root, "crash_reports"))

        crashes.write("crash")

        assertThat(crashes.readFresh()).isEqualTo("crash")
    }

    @Test
    fun `a report older than a week is not attached and is deleted`() {
        val crashes = reportFile()
        crashes.write("old crash")
        val file = File(folder.root, CrashReportFile.FileName).agedBy(Duration.ofDays(8))

        assertThat(crashes.readFresh()).isEmpty()
        assertThat(file.exists()).isFalse()
    }

    @Test
    fun `a report kept from before masking is never attached`() {
        val legacy = File(folder.root, CrashReportFile.LegacyFileName).apply { writeText("raw log") }

        assertThat(reportFile().readFresh()).isEmpty()
        assertThat(legacy.exists()).isFalse()
    }

    @Test
    fun `a report handed off is gone`() {
        val crashes = reportFile()
        crashes.write("crash")

        crashes.discard()

        assertThat(crashes.readFresh()).isEmpty()
        assertThat(folder.root.listFiles()).isEmpty()
    }

    @Test
    fun `pruning at start removes an old report and the legacy one, and keeps a recent one`() {
        val crashes = reportFile()
        val legacy = File(folder.root, CrashReportFile.LegacyFileName).apply { writeText("raw log") }
        crashes.write("recent")
        File(folder.root, CrashReportFile.FileName).agedBy(Duration.ofDays(2))

        crashes.pruneStale()

        assertThat(legacy.exists()).isFalse()
        assertThat(crashes.readFresh()).isEqualTo("recent")

        File(folder.root, CrashReportFile.FileName).agedBy(Duration.ofDays(30))
        crashes.pruneStale()

        assertThat(folder.root.listFiles()).isEmpty()
    }
}
