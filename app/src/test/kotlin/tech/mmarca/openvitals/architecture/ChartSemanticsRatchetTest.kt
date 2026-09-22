package tech.mmarca.openvitals.architecture

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * A Canvas says nothing to a screen reader. Every chart file that draws one
 * must also publish a description: `chartSemantics(...)`, one node per bucket
 * with `clearAndSetSemantics`, or a `contentDescription`. The files listed
 * here are exempt for the reason given. Nothing else may add a silent chart.
 */
class ChartSemanticsRatchetTest {

    @Test
    fun `every chart that draws a Canvas also describes itself`() {
        val silent = chartFiles()
            .filter { file -> file.readText().contains(CanvasCall) }
            .filterNot { file -> Described.containsMatchIn(file.readText()) }
            .map { it.name }
            .filterNot { it in Exempt }
            .toList()

        assertWithMessage(
            "chart files with a Canvas and no spoken summary. Add chartSemantics(...) on the " +
                "chart root, or one described node per bucket. See ChartSemantics.kt.",
        )
            .that(silent)
            .isEmpty()
    }

    @Test
    fun `the exemptions hold no stale names`() {
        val present = chartFiles()
            .filter { it.name in Exempt }
            .filter { file -> file.readText().contains(CanvasCall) }
            .filterNot { file -> Described.containsMatchIn(file.readText()) }
            .map { it.name }
            .toSet()

        assertWithMessage("remove a file from the exemptions once it describes itself")
            .that(present)
            .isEqualTo(Exempt)
    }

    private fun chartFiles(): Sequence<File> =
        File(SourceRoot).walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                file.path.contains("/ui/charts/") ||
                    ChartFileName.containsMatchIn(file.name) ||
                    file.name in AlsoCharts
            }

    private companion object {
        const val SourceRoot = "src/main/kotlin/tech/mmarca/openvitals"
        val CanvasCall = Regex("""\bCanvas\(""")
        // A `contentDescription = null` is an icon opting out, not a description.
        val Described = Regex("""chartSemantics\(|clearAndSetSemantics|contentDescription = (?!null)""")
        val ChartFileName = Regex("""Chart|Sparkline|Plot""")

        // Chart files whose name does not say so.
        val AlsoCharts = setOf("SleepStageComponents.kt")

        val Exempt = setOf(
            // Tick marks beside axis labels that are Text nodes already.
            "ChartAxis.kt",
            // Draws a bitmap for a home widget; Glance has no semantics tree.
            "BodyEnergyPlot.kt",
        )
    }
}
