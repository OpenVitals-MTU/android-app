package tech.mmarca.openvitals.features.cycle

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import tech.mmarca.openvitals.domain.cycle.CycleFacts
import tech.mmarca.openvitals.domain.cycle.PhaseTips

/** Every tip and fact id in the catalog maps to a string; an unmapped id would draw an empty card. */
class CycleCopyTest {

    @Test
    fun `every phase tip id maps to its copy`() {
        val unmapped = PhaseTips.ALL.map { it.id }.filter { phaseTipTextRes(it) == null }
        assertWithMessage("tips without copy").that(unmapped).isEmpty()
    }

    @Test
    fun `every cycle fact id maps to its copy`() {
        val unmapped = CycleFacts.ALL.map { it.id }.filter { cycleFactTextRes(it) == null }
        assertWithMessage("facts without copy").that(unmapped).isEmpty()
    }

    @Test
    fun `an unknown id maps to nothing rather than a wrong string`() {
        assertWithMessage("tip").that(phaseTipTextRes("not_a_tip")).isNull()
        assertWithMessage("fact").that(cycleFactTextRes("not_a_fact")).isNull()
    }
}
