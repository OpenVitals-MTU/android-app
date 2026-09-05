package tech.mmarca.openvitals.ui.theme

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * A ratchet on bare numbers for spacing, radius and alpha: the count may fall, never rise.
 *
 * A blanket sweep would be wrong, because `16.dp` is `Spacing.lg` as padding and not as an icon size.
 * When this fails on new UI, use the tokens or a named `private val` next to the use.
 * When you migrate a screen, lower the ceiling in the same commit.
 */
class TokenDisciplineTest {

    @Test
    fun `bare dp literals do not increase`() {
        val count = countMatches(BareDp)

        assertWithMessage(
            "bare dp literals went UP. Use Spacing/Radii/LayoutMetrics, or a named " +
                "private val for a genuine one-off. If you migrated a screen, lower " +
                "the ceiling in this test to the new count.",
        )
            .that(count)
            .isAtMost(MaxBareDp)
    }

    @Test
    fun `hand-written alphas do not increase`() {
        val count = countMatches(BareAlpha)

        assertWithMessage(
            "hand-written alpha literals went UP. The tint ladder is Emphasis " +
                "(wash/subtle/disabled/fill/strong); Material's own state layers are " +
                "applied by the components and are not yours to hand-build.",
        )
            .that(count)
            .isAtMost(MaxBareAlpha)
    }

    @Test
    fun `hand-built corner shapes do not increase`() {
        val count = countMatches(BareCorner)

        assertWithMessage(
            "hand-built RoundedCornerShape literals went UP. Prefer " +
                "MaterialTheme.shapes, or Radii for a shape built by hand.",
        )
            .that(count)
            .isAtMost(MaxBareCorner)
    }

    @Test
    fun `the theme itself is allowed to hold the numbers`() {
        // The scales must be literals somewhere, and that is the token file. This guards the exclusion.
        val tokens = File("$SourceRoot/ui/theme/DesignTokens.kt").readText()

        assertWithMessage("the token file is the one place a bare dp belongs")
            .that(BareDp.findAll(tokens).count())
            .isGreaterThan(20)
    }

    private fun countMatches(pattern: Regex): Int =
        File(SourceRoot).walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.path.contains(ExcludedThemeDir) }
            .sumOf { file ->
                file.readLines()
                    // A named definition is the sanctioned form, so it must not count against the ceiling.
                    .filterNot { line -> NamedDefinition.matches(line.trim()) }
                    .sumOf { line -> pattern.findAll(line).count() }
            }

    private companion object {
        const val SourceRoot = "src/main/kotlin/tech/mmarca/openvitals"

        /** The scales live here; they are definitions, not call sites. */
        const val ExcludedThemeDir = "/ui/theme/"

        /** `16.dp`, but not `Spacing.lg` and not a decimal like `3.5.dp`. */
        val BareDp = Regex("""(?<![\w.])\d+\.dp""")

        /** `private val Foo = 16.dp` — a definition, not a call-site literal. */
        val NamedDefinition =
            Regex("""(private\s+|internal\s+)?(const\s+)?val\s+\w+(\s*:\s*\w+)?\s*=\s*[\d.]+\.?(dp|f)?,?\s*(//.*)?""")

        /** `alpha = 0.12f` written out instead of `Emphasis.wash`. */
        val BareAlpha = Regex("""alpha\s*=\s*0\.\d+f""")

        /** `RoundedCornerShape(12.dp)` instead of a theme shape. */
        val BareCorner = Regex("""RoundedCornerShape\(\s*\d""")

        // Ceilings, measured 2026-08-04 with named definitions excluded. These may only go down.
        // 2026-09-05: DerivedMetricsResetCard migrated (1941 -> 1933).
        // 2026-09-05: FitImportCard migrated (1933 -> 1925).
        const val MaxBareDp = 1925
        const val MaxBareAlpha = 92
        const val MaxBareCorner = 16
    }
}
