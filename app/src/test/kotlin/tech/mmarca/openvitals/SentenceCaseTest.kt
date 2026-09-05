package tech.mmarca.openvitals

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Test
import org.w3c.dom.Element

/**
 * First-party copy is sentence case; third-party product names are not.
 * "Body energy" must not creep back to title case, and Garmin's *Body Battery* must not
 * be "corrected" into sentence case. English only: `values-XX/` is Weblate's.
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
        /** The title-case spellings that were corrected, listed as the wrong form. */
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
