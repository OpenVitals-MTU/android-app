package tech.mmarca.openvitals

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Test
import org.w3c.dom.Element

/**
 * First-party copy is sentence case; third-party product names are not.
 *
 * The design system's copy standard: sentence case everywhere — titles,
 * buttons, list items. The app's own feature names are subject to it, so
 * "Body Energy" became "Body energy", and consistently so: a title that reads
 * one way while the paragraph under it reads another is worse than either
 * choice made throughout.
 *
 * The exemption is narrow and load-bearing. Garmin's *Body Battery*, *Sleep
 * Coach*, *Training Readiness*, *Intensity Minutes* and *Stress Level* are that
 * company's product names appearing in our UI, and lowercasing someone else's
 * trademark is not a house-style decision we get to make. This test guards the
 * exemption in both directions: the first-party names must not creep back to
 * title case, and the third-party ones must not be "corrected" into sentence
 * case by a well-meaning sweep.
 *
 * English only. `values-XX/` is Weblate's, and each language's capitalisation
 * rules are its translators' business — German capitalises every noun.
 */
class SentenceCaseTest {

    @Test
    fun `first-party feature names are sentence case`() {
        val text = sourceStrings()
        val offenders = FirstPartyTitleCase.filter { phrase -> phrase in text }

        assertWithMessage(
            "the copy standard is sentence case for the app's own names; " +
                "use the sentence-case spelling everywhere the name appears",
        )
            .that(offenders)
            .isEmpty()
    }

    @Test
    fun `third-party product names keep their trademark casing`() {
        val text = sourceStrings()
        val flattened = ThirdPartyProductNames.filterNot { phrase -> phrase in text }

        assertWithMessage(
            "these are Garmin's product names, not our copy — the sentence-case " +
                "rule does not reach them",
        )
            .that(flattened)
            .isEmpty()
    }

    /** Every `<string>` and plural item in the English catalogue, concatenated. */
    private fun sourceStrings(): String {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File("src/main/res/values/strings.xml"))
        val out = StringBuilder()
        val nodes = document.getElementsByTagName("*")
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as Element
            if (element.tagName == "string" || element.tagName == "item") {
                out.append(element.textContent).append('\n')
            }
        }
        return out.toString()
    }

    private companion object {
        /**
         * The exact title-case spellings that were corrected. Listed as the
         * WRONG form so the test names what must not come back.
         */
        val FirstPartyTitleCase = listOf(
            "Body Energy",
            "Daily Readiness",
            "Recovery Mode",
            "Data Importers",
            "Add Marker",
            "Heart & Vitals",
            "Stress Tracking",
            "HRV Status",
            "FIT Importer",
            "CSV Importer",
        )

        /** Garmin's names, in the casing Garmin uses. */
        val ThirdPartyProductNames = listOf(
            "Body Battery",
            "Sleep Coach",
            "Training Readiness",
            "Intensity Minutes",
            "Stress Level",
        )
    }
}
