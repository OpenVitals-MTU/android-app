package tech.mmarca.openvitals

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * The catalog-shaped half of the translation gate: what languages the picker may
 * offer, and what a plural has to look like before Android will resolve it.
 *
 * `scripts/verify-translations.py` covers stale keys, kind changes and plural
 * quantities that exist in English. What it cannot cover — because it only ever
 * reads `res/` — is the agreement between the resources and the picker that
 * shows them. `ShippedLanguagesTest` asserts that agreement, but it is an
 * instrumentation test: it needs a device, and `verifyCi` does not run one. The
 * checks below are the JVM half, so a language added without a translation (or
 * translated without being offered) fails a plain `:app:testCiUnitTest`.
 */
class TranslationCatalogTest {

    private val base = TranslationResources.read(TranslationResources.baseDirectory)

    /**
     * Android resolves an unmatched quantity by falling back to `other`; a
     * `<plurals>` without one throws `Resources.NotFoundException` for whatever
     * count the device's rules land on. The Python script only notices this in a
     * locale, and only because English happens to have `other` to compare
     * against — a base plural that lost `other` would go unmentioned.
     */
    @Test
    fun `every plurals resource offers an other branch in every locale`() {
        val problems = TranslationResources.localeDirectories().flatMap { dir ->
            TranslationResources.read(dir)
                .filterValues { TranslationResources.PLAIN !in it }
                .filterValues { "other" !in it }
                .keys
                .map { "${dir.name}/$it has quantities but no 'other'" }
        }

        assertThat(problems).isEmpty()
    }

    /**
     * Port of Flutter's `check 10: picker <-> ARB agreement`: an `AppLanguage`
     * constant with no catalog behind it is a picker entry that silently does
     * nothing — the user chooses it and the UI stays English.
     */
    @Test
    fun `every AppLanguage constant has a translation directory`() {
        val shipped = shippedLanguageTags()
        val present = TranslationResources.localeDirectories()
            .map { it.name }
            .map { it.removePrefix("values-") }
            .toSet() + "en"

        assertWithMessage("an AppLanguage constant with no values-XX/ is a picker entry that does nothing")
            .that(present)
            .containsAtLeastElementsIn(shipped)
    }

    /**
     * Port of Flutter's `a SHIPPED locale below the floor still FAILS` and its
     * twin `an in-progress locale that crosses the floor is reported, not
     * failed`.
     *
     * SHIPPED = has an `AppLanguage` constant. IN PROGRESS = a `values-XX/` with
     * no constant (Galician today) — Weblate fills those in one language at a
     * time and gating them would red the build on a translator's first commit,
     * so they are only reported. `generate-translation-coverage.py` keeps them
     * out of the picker on the same threshold.
     */
    @Test
    fun `every shipped language clears the translation floor`() {
        val translatable = base.keys - TranslationResources.nonTranslatableNames(
            TranslationResources.baseDirectory,
        )
        val shipped = shippedLanguageTags() - "en"

        val belowFloor = TranslationResources.localeDirectories()
            .filter { it != TranslationResources.baseDirectory }
            .associate { dir ->
                val tag = dir.name.removePrefix("values-")
                val translated = TranslationResources.read(dir).keys.count { it in translatable }
                tag to translated.toDouble() / translatable.size
            }
            .filterKeys { it in shipped }
            .filterValues { it <= MIN_COVERAGE }

        assertWithMessage("a language the picker offers must not be a mostly-English UI")
            .that(belowFloor)
            .isEmpty()
    }

    /**
     * Port of Flutter's `flags an AppLanguage constant with no autonym in the
     * dropdown` — "an autonym is the same in every locale, which is the accepted
     * i18n practice for a language selector so users can always recognise their
     * language" (`lib/ui/components/app_language_dropdown.dart`).
     *
     * The picker used to resolve each entry through a translatable
     * `settings_language_*` resource, so the labels were exonyms in whatever
     * language the UI was already in: an Estonian speaker who picks up a German
     * phone was offered "Estnisch", and a Spanish speaker on an Estonian phone
     * was offered "Hispaania". The one user who needs the picker most is the
     * one who cannot read it.
     *
     * It now derives every label from the language itself, so an autonym cannot
     * drift per locale and a new language needs no strings at all. This asserts
     * the mechanism rather than the output, because the output is the JDK's:
     * checking that `getDisplayLanguage` returns "Deutsch" would be testing
     * ICU. What is ours, and what regressed once, is WHICH locale the name is
     * asked for — and that no translatable resource is back in the path.
     *
     * `settings_language_system` is deliberately not covered: it names the
     * device's setting, not a language, and must stay translated.
     */
    @Test
    fun `the language picker names every language in that language`() {
        val source = File(
            "src/main/kotlin/tech/mmarca/openvitals/ui/components/AppLanguageDropdown.kt",
        ).readText()

        assertWithMessage(
            "a per-language string resource in the picker is an exonym: it renders in the " +
                "UI's current language, which is the one the user cannot read",
        )
            .that(RETIRED_LANGUAGE_LABEL_KEYS.filter { it in source })
            .isEmpty()

        assertWithMessage(
            "the label must be asked of the language's OWN locale; getDisplayName(displayLocale) " +
                "is what produced \"Estnisch\" on a German phone",
        )
            .that(source)
            .contains("getDisplayLanguage(locale)")
    }

    /** The retired exonym resources must not come back, in any locale. */
    @Test
    fun `no locale still ships the retired language-name strings`() {
        val problems = TranslationResources.localeDirectories().flatMap { dir ->
            val entries = TranslationResources.read(dir)
            RETIRED_LANGUAGE_LABEL_KEYS.filter { it in entries }.map { "${dir.name}/$it" }
        }

        assertWithMessage("these were replaced by autonyms derived from the language tag")
            .that(problems)
            .isEmpty()
    }

    /** `en`, `es`, `de`, `it`, `et` — read from the source, not restated here. */
    private fun shippedLanguageTags(): Set<String> {
        val source = File("src/main/kotlin/tech/mmarca/openvitals/domain/preferences/AppLanguage.kt")
            .readText()
        val tags = APP_LANGUAGE_CONSTANT.findAll(source).map { it.groupValues[1] }.toSet()
        assertWithMessage("AppLanguage.kt no longer declares its constants the way this test reads them")
            .that(tags)
            .isNotEmpty()
        return tags
    }

    private companion object {
        const val MIN_COVERAGE = 0.70

        /** `val ENGLISH = AppLanguage("en", "ENGLISH")` — the null-tag SYSTEM does not match. */
        val APP_LANGUAGE_CONSTANT = Regex("""AppLanguage\("([a-zA-Z-]+)",""")

        val RETIRED_LANGUAGE_LABEL_KEYS = listOf(
            "settings_language_english",
            "settings_language_spanish",
            "settings_language_german",
            "settings_language_italian",
            "settings_language_estonian",
        )
    }
}
