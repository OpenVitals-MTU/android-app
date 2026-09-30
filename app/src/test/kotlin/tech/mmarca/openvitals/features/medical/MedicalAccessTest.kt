package tech.mmarca.openvitals.features.medical

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Health Connect closes any request that holds a permission refused twice, so the re-ask leaves those out. */
class MedicalAccessTest {

    private val refusedTwice = setOf("read-pregnancy", "read-social")

    private fun action(missing: Set<String>, firstRequestDone: Boolean = true) =
        medicalAccessAction(missing, firstRequestDone) { it !in refusedTwice }

    @Test
    fun `nothing missing offers nothing`() {
        assertThat(action(emptySet())).isEqualTo(MedicalAccessAction.None)
    }

    @Test
    fun `before the first request everything missing is asked`() {
        // Android's check is false for a permission never asked, so it cannot be trusted yet.
        assertThat(action(refusedTwice + "write", firstRequestDone = false))
            .isEqualTo(MedicalAccessAction.Ask(refusedTwice + "write"))
    }

    @Test
    fun `a re-ask leaves out what was refused twice`() {
        assertThat(action(refusedTwice + "read-vaccines")).isEqualTo(MedicalAccessAction.Ask(setOf("read-vaccines")))
    }

    @Test
    fun `with only refused permissions left, the way on is Health Connect's settings`() {
        assertThat(action(refusedTwice)).isEqualTo(MedicalAccessAction.OpenSettings)
    }
}
