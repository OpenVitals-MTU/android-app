package tech.mmarca.openvitals

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * The catalog half of the translation gate: which languages the picker offers, and
 * what a plural needs before Android resolves it. `ShippedLanguagesTest` needs a device;
 * these checks run on a plain `:app:testCiUnitTest`.
 */
class TranslationCatalogTest {

    private val base = TranslationResources.read(TranslationResources.baseDirectory)

    /** A `<plurals>` without `other` throws `Resources.NotFoundException`. The script only notices this in a locale. */
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

    /** An `AppLanguage` constant with no catalog behind it is a picker entry that does nothing. */
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
     * SHIPPED = has an `AppLanguage` constant and must clear the floor.
     * IN PROGRESS = a `values-XX/` with no constant; only reported, so Weblate's first commit does not red the build.
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
     * Every picker label is an autonym derived from the language itself. Translatable
     * `settings_language_*` resources gave exonyms, so an Estonian speaker on a German phone
     * saw "Estnisch". This asserts which locale the name is asked for, not the JDK's output.
     * `settings_language_system` names the device setting and stays translated.
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

    /** Read from the source, not restated here. */
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

        /** Matches `AppLanguage("en", ...)`; the null-tag SYSTEM does not match. */
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
