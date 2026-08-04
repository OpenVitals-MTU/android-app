package tech.mmarca.openvitals

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Every format string in the source resources is one `String.format` can run,
 * and every translation of one carries the same arguments as its English.
 *
 * A malformed specifier is invisible until the line is actually rendered, and
 * some of these lines are rendered only in states a manual pass rarely reaches.
 * `stress_factor_hrv_above` read `HRV is %1$d% above your usual baseline.` —
 * the bare `%` before `above` parses as a `%a` hex-float conversion with a
 * space flag, so it threw `IllegalFormatConversionException` against the `Int`
 * it was handed, and the stress details screen crashed for anyone whose HRV was
 * above their baseline. Its neighbours rendered `%b` as "true" and `%o` as
 * octal instead.
 *
 * Every locale is checked, not only `values/`. The `values-XX/` files are
 * Weblate's and their wording is not ours to correct — but a bare `%` is not a
 * wording question. Estonian and Italian both inherited this bug from the
 * source string and crashed the stress screen with `%a`, `%e` and `%ü`
 * conversions against an `Int`, in locales nobody on the project reads. A
 * translator's typo must not be able to take the app down, so the guard covers
 * their files too and the fix goes upstream to Weblate as well.
 *
 * `scripts/verify-translations.py` is the other half of this gate and has two
 * blind spots these tests cover instead — see the KDoc on each test.
 */
class StringFormatSpecifierTest {

    private val base = TranslationResources.read(TranslationResources.baseDirectory)
    private val formattedNames = TranslationResources.formattedBaseNames(base)

    /**
     * Which strings are checked is decided by the BASE file, not by whether the
     * string in hand happens to use a positional argument.
     *
     * The earlier gate was "the text contains `%\d+$`", which quietly excluded
     * every non-positional format string in the tree. `activity_type_stats_activity_count`
     * is `%d activity` and is handed a count by
     * `ActivityTypeAggregateStatsCard.kt:107`; a translation reading `%d% aktiivsust`
     * would have crashed the activity screen exactly the way `stress_factor_hrv_above`
     * crashed the stress screen, and neither this test nor
     * `scripts/verify-translations.py` would have said a word.
     */
    @Test
    fun `no formatted string in any locale has a malformed format specifier`() {
        val problems = TranslationResources.localeDirectories().flatMap { dir ->
            TranslationResources.read(dir).flatMap { (name, values) ->
                values.mapNotNull { (quantity, text) ->
                    // The base file decides, except that a locale which has
                    // introduced a positional argument of its own is checked on
                    // its own evidence — the old gate, kept as a floor.
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
     * A translation carries exactly the arguments its English does — flags,
     * width, precision and conversion included.
     *
     * `scripts/verify-translations.py:16` matches placeholders with
     * `%(?:\d+\$)?[a-zA-Z]`, which cannot see a specifier that carries a
     * precision. `cycle_basal_temperature_value` is `%1$.1f C · %2$s`, and the
     * script reads it as having ONE placeholder, `%2$s`. A translator who drops
     * the temperature, or retypes it as `%1$.1d`, passes `verifyTranslations`
     * cleanly: the first silently renders the cycle card without its reading,
     * the second throws `IllegalFormatPrecisionException` when the card is
     * drawn.
     *
     * The same routine also covers the plural branches the script skips
     * entirely: `compare_placeholders` iterates the BASE quantities, so
     * `values-es`'s extra `many` item — which Spanish legitimately needs — is
     * never compared against anything. Here it is compared against `other`,
     * which is the branch Android itself falls back to.
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

    // --- negative cases: a guard that cannot fail is a guard that is not running.

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
        // "Counts as hydration (%)" is never handed an argument, so its % never
        // reaches String.format. It has no specifier, so it is not a formatted
        // name and the corpus guard skips it.
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
