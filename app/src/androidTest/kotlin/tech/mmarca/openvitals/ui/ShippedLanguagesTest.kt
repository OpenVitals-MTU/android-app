package tech.mmarca.openvitals.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.AppLanguage

/**
 * Port of `the shell offers the SHIPPED locales, not every ARB present` from
 * Flutter's `test/widget_test.dart`.
 *
 * Flutter pins `MaterialApp.supportedLocales`. This app has no such list: the
 * languages a user may pick come from `R.array.translation_picker_language_tags`,
 * generated at build time by `scripts/generate-translation-coverage.py`, which
 * admits a `values-xx` translation only once it clears a coverage floor. So the
 * same guard reads as: the picker offers exactly the generated set, and every
 * entry in it cleared the floor — an in-progress translation kept in the tree
 * for Weblate never reaches a user.
 */
@RunWith(AndroidJUnit4::class)
class ShippedLanguagesTest {

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private val shippedTags: List<String>
        get() = resources.getStringArray(R.array.translation_picker_language_tags).toList()

    @Test
    fun thePickerOffersTheSystemDefaultAndEveryShippedLanguage() {
        val options = AppLanguage.pickerOptions(shippedTags)

        assertEquals(AppLanguage.SYSTEM, options.first())
        assertEquals(shippedTags, options.drop(1).map { language -> language.languageTag })
        assertEquals("no language may be offered twice", options.size, options.distinct().size)
    }

    @Test
    fun englishIsAlwaysOfferedAndLeadsTheList() {
        assertEquals(listOf("en"), shippedTags.take(1))
    }

    @Test
    fun everyOfferedLanguageClearedTheTranslationFloor() {
        val minimumPercent = resources.getInteger(R.integer.translation_picker_minimum_coverage_percent)
        val coverageByTag = resources
            .getStringArray(R.array.translation_picker_language_coverage)
            .associate { entry ->
                val tag = entry.substringBeforeLast(':')
                tag to entry.substringAfterLast(':').toInt()
            }

        assertEquals(
            "every offered language reports its coverage",
            shippedTags.toSet(),
            coverageByTag.keys,
        )
        shippedTags.forEach { tag ->
            val percent = checkNotNull(coverageByTag[tag])
            assertTrue(
                "$tag is offered at $percent%, below the $minimumPercent% floor",
                percent >= minimumPercent,
            )
        }
    }

    @Test
    fun everyOfferedLanguageIsAWellFormedLanguageTag() {
        shippedTags.forEach { tag ->
            assertTrue("'$tag' is not a language tag", Locale.forLanguageTag(tag).language.isNotEmpty())
        }
    }
}
