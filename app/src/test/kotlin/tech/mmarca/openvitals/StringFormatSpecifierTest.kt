package tech.mmarca.openvitals

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Every format string in the resources runs through `String.format`, and every translation
 * carries the same arguments as its English.
 *
 * `stress_factor_hrv_above` once read `HRV is %1$d% above your usual baseline.` The bare `%`
 * parsed as a `%a` conversion and crashed the stress screen. Every locale is checked because
 * Estonian and Italian inherited the bug; a translator's typo must not take the app down.
 *
 * `scripts/verify-translations.py` is the other half of this gate; these tests cover its blind spots.
 */
class StringFormatSpecifierTest {

    private val base = TranslationResources.read(TranslationResources.baseDirectory)
    private val formattedNames = TranslationResources.formattedBaseNames(base)

    /**
     * The base file decides which strings are checked. The old gate ("contains `%\d+$`")
     * skipped every non-positional format string, such as `%d activity`.
     */
    @Test
    fun `no formatted string in any locale has a malformed format specifier`() {
        val problems = TranslationResources.localeDirectories().flatMap { dir ->
            TranslationResources.read(dir).flatMap { (name, values) ->
                values.mapNotNull { (quantity, text) ->
                    // The base file decides, except a locale that introduced its own positional argument.
                    if (name !in formattedNames && !POSITIONAL.containsMatchIn(text)) {
                        return@mapNotNull null
                    }
                    TranslationResources.scan(text).problems
                        .firstOrNull()
                        ?.let { "${dir.name}/$name[$quantity]: $text  ($it)" }
                }
            }
        }

        assertWithMessage(
            "a bare % in a formatted string is read as a conversion; write %% for a literal one",
        )
            .that(problems)
            .isEmpty()
    }

    /**
     * A translation carries exactly the arguments its English does, precision included.
     * The script's regex cannot see `%1$.1f`, so a dropped or retyped specifier passed it.
     * Plural branches the script skips (`many` in `values-es`) are compared against `other`.
     */
    @Test
    fun `every translation keeps the format arguments of its base string`() {
        val problems = TranslationResources.localeDirectories()
            .filter { it != TranslationResources.baseDirectory }
            .flatMap { dir ->
                TranslationResources.read(dir)
                    .filterKeys { it in base }
                    .flatMap { (name, values) ->
                        val baseValues = base.getValue(name)
                        values.mapNotNull { (quantity, text) ->
                            val reference = baseValues[quantity]
                                ?: baseValues["other"]
                                ?: baseValues[TranslationResources.PLAIN]
                                ?: return@mapNotNull null
                            val expected = specifierCounts(reference)
                            val actual = specifierCounts(text)
                            if (expected == actual) {
                                null
                            } else {
                                "${dir.name}/$name[$quantity]: expected $expected, got $actual  ($text)"
                            }
                        }
                    }
            }

        assertWithMessage(
            "a translation must format the same arguments as its English source",
        )
            .that(problems)
            .isEmpty()
    }

    // Negative cases: a guard that cannot fail is not running.

    @Test
    fun `the scan catches a bare percent in a non-positional format string`() {
        val scan = TranslationResources.scan("%d% aktiivsust")

        assertThat(scan.specifiers).containsExactly("%d")
        assertThat(scan.problems).containsExactly("'% a' reads as a conversion")
    }

    @Test
    fun `the scan reads a precision specifier that verify-translations cannot see`() {
        val scan = TranslationResources.scan("%1\$.1f C · %2\$s")

        assertThat(scan.specifiers).containsExactly("%1\$.1f", "%2\$s").inOrder()
        assertThat(scan.problems).isEmpty()
    }

    @Test
    fun `dropping a precision argument is a mismatch`() {
        assertThat(specifierCounts("%2\$s"))
            .isNotEqualTo(specifierCounts("%1\$.1f C · %2\$s"))
    }

    @Test
    fun `retyping a precision argument is a mismatch`() {
        assertThat(specifierCounts("%1\$.1d C · %2\$s"))
            .isNotEqualTo(specifierCounts("%1\$.1f C · %2\$s"))
    }

    @Test
    fun `a lone percent in an unformatted string is not a problem worth reporting`() {
        // "Counts as hydration (%)" is never formatted and has no specifier, so the guard skips it.
        val base = mapOf("hydration_impact_percent_label" to mapOf("value" to "Counts as hydration (%)"))

        assertThat(TranslationResources.formattedBaseNames(base)).isEmpty()
    }

    /** Argument index, flags, width, precision and conversion, as a multiset. */
    private fun specifierCounts(text: String): Map<String, Int> =
        TranslationResources.scan(text).specifiers.groupingBy { it }.eachCount()

    private companion object {
        val POSITIONAL = Regex("""%\d+\$""")
    }
}
