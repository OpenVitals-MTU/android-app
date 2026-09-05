package tech.mmarca.openvitals.ui.theme

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * A ratchet on bare numbers for spacing, radius and alpha.
 *
 * The design system's rule is *"no bare numbers for spacing, radius, or alpha"*
 * in new code, with the existing ones migrated per screen and under golden
 * cover. That rule was prose, and prose does not stop the count going up.
 *
 * This is not a migration and does not try to be one. A blanket sweep would be
 * actively wrong: `16.dp` is `Spacing.lg` when it is padding and is nothing of
 * the sort when it is an icon's size, and a script cannot tell those apart.
 * What this does is hold the line — the number may fall, never rise — so the
 * backlog can be worked off a screen at a time without new debt arriving behind
 * it.
 *
 * **When this fails on a change that adds UI:** use the tokens (`Spacing`,
 * `Radii`, `Emphasis`, `LayoutMetrics`). If the value genuinely is not one of
 * them — an icon size, a stroke width, a chart geometry constant — give it a
 * named `private val` next to its use, which is also not a bare number.
 *
 * **When you migrate a screen:** lower the ceiling in the same commit. That is
 * the ratchet tightening, and it is the only edit to these numbers that should
 * ever happen.
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
        // The scales have to be literals SOMEWHERE, and that somewhere is the
        // token file. This guards the exclusion rather than leaving it implicit:
        // if DesignTokens.kt stopped containing bare dp values, the scale would
        // have been indirected into something that is no longer a single place
        // to read it.
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
                    // A named definition is the sanctioned form — "give it a
                    // named private val next to its use" — so it must not
                    // count against the ceiling, or following the rule would
                    // trip the guard that enforces it.
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

        // Ceilings, measured 2026-08-04 with named definitions excluded (they
        // are the sanctioned form, so counting them would punish following the
        // rule). These may only ever go DOWN.
        // 2026-09-05: DerivedMetricsResetCard migrated (1941 -> 1933).
        const val MaxBareDp = 1933
        const val MaxBareAlpha = 92
        const val MaxBareCorner = 16
    }
}
