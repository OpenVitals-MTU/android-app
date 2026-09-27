package tech.mmarca.openvitals.domain.cycle

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * Every tip, fact and symptom id has its copy. A card whose id has no string
 * would show nothing, and nothing else would notice.
 */
class CycleCopyCatalogTest {

    private val strings = File("src/main/res/values/strings.xml").readText()

    @Test
    fun `every phase tip has a string`() {
        assertMissing("tips", PhaseTips.ALL.map { "cycle_tip_${it.id}" })
    }

    @Test
    fun `every cycle fact has a string`() {
        assertMissing("facts", CycleFacts.ALL.map { "cycle_fact_${it.id}" })
    }

    @Test
    fun `every symptom has a string`() {
        assertMissing("symptoms", CycleSymptom.entries.map { "cycle_symptom_${it.id}" })
    }

    private fun assertMissing(what: String, names: List<String>) {
        val missing = names.filterNot { "name=\"$it\"" in strings }
        assertWithMessage("$what without a string in values/strings.xml").that(missing).isEmpty()
    }
}
